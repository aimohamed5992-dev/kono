using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class StoreAdminService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;

    public StoreAdminService(KonoDbContext db, ImageUrlBuilder img)
    {
        _db = db;
        _img = img.Func;
    }

    // ── stores ──────────────────────────────────────────────────────────────
    public async Task<PagedResult<StoreAdminRow>> ListAsync(string? search, string? kind, PageQuery page)
    {
        var q = _db.Stores.AsNoTracking().Include(s => s.Products).AsQueryable();
        if (Enum.TryParse<VerticalKind>(kind, true, out var k)) q = q.Where(s => s.Kind == k);
        if (!string.IsNullOrWhiteSpace(search))
            q = q.Where(s => s.Name.Contains(search.Trim()) || s.CuisineAr.Contains(search.Trim()));

        var total = await q.CountAsync();
        var rows = await q.OrderBy(s => s.Name).Skip(page.Skip).Take(page.PageSize)
            .Select(s => new StoreAdminRow(s.Id, s.Name, s.Kind.ToString(), s.CuisineAr, s.Rating,
                s.EtaMinutes, s.DeliveryFee, s.IsActive, s.IsClosed, s.Products.Count, s.HeroImageUrl))
            .ToListAsync();
        foreach (var r in rows) { } // rows already projected; resolve images below
        var resolved = rows.Select(r => r with { HeroImageUrl = _img(r.HeroImageUrl) }).ToList();
        return new PagedResult<StoreAdminRow>(resolved, page.Page, page.PageSize, total);
    }

    public async Task<StoreDetailDto?> GetAsync(int id)
    {
        var s = await _db.Stores
            .Include(x => x.Tags)
            .Include(x => x.Categories).ThenInclude(c => c.Products).ThenInclude(p => p.OptionGroups).ThenInclude(g => g.Choices)
            .FirstOrDefaultAsync(x => x.Id == id);
        return s?.ToDetail(_img);
    }

    public async Task<StoreDetailDto> CreateAsync(StoreUpsertRequest req)
    {
        var s = new Store();
        Apply(s, req);
        _db.Stores.Add(s);
        await _db.SaveChangesAsync();
        return (await GetAsync(s.Id))!;
    }

    public async Task<StoreDetailDto> UpdateAsync(int id, StoreUpsertRequest req)
    {
        var s = await _db.Stores.Include(x => x.Tags).FirstOrDefaultAsync(x => x.Id == id)
            ?? throw new KeyNotFoundException();
        Apply(s, req);
        _db.StoreTags.RemoveRange(s.Tags);
        s.Tags = (req.Tags ?? new()).Select(t => new StoreTag { Text = t }).ToList();
        await _db.SaveChangesAsync();
        return (await GetAsync(s.Id))!;
    }

    public async Task SetStatusAsync(int id, bool isActive, bool? isClosed)
    {
        var s = await _db.Stores.FindAsync(id) ?? throw new KeyNotFoundException();
        s.IsActive = isActive;
        if (isClosed is { } c) s.IsClosed = c;
        await _db.SaveChangesAsync();
    }

    public async Task DeleteAsync(int id)
    {
        var s = await _db.Stores.FindAsync(id) ?? throw new KeyNotFoundException();
        s.IsActive = false;              // soft delete
        await _db.SaveChangesAsync();
    }

    private static void Apply(Store s, StoreUpsertRequest r)
    {
        s.Name = r.Name;
        s.Kind = Enum.TryParse<VerticalKind>(r.Kind, true, out var k) ? k : VerticalKind.Restaurants;
        s.CuisineAr = r.CuisineAr; s.Rating = r.Rating; s.RatingCount = r.RatingCount;
        s.EtaMinutes = r.EtaMinutes; s.DeliveryFee = r.DeliveryFee; s.MinOrder = r.MinOrder;
        s.DistanceKm = r.DistanceKm; s.IsBusy = r.IsBusy; s.IsClosed = r.IsClosed;
        s.PromoTagAr = string.IsNullOrWhiteSpace(r.PromoTagAr) ? null : r.PromoTagAr;
        s.HeroImageUrl = r.HeroImageUrl; s.LogoImageUrl = r.LogoImageUrl;
        s.PriceLevel = r.PriceLevel; s.PrepMinutes = r.PrepMinutes; s.IsActive = r.IsActive;
        s.Latitude = r.Latitude; s.Longitude = r.Longitude;
        s.AddressAr = r.AddressAr; s.CityAr = r.CityAr; s.OwnerUserId = r.OwnerUserId;
        if (r.Tags is not null && s.Id == 0)
            s.Tags = r.Tags.Select(t => new StoreTag { Text = t }).ToList();
    }

    // ── products ────────────────────────────────────────────────────────────
    public async Task<List<MenuCategoryDto>> MenuAsync(int storeId)
    {
        var cats = await _db.MenuCategories
            .Include(c => c.Products).ThenInclude(p => p.OptionGroups).ThenInclude(g => g.Choices)
            .Where(c => c.StoreId == storeId).OrderBy(c => c.SortOrder).ToListAsync();
        return cats.Select(c => c.ToDto(_img)).ToList();
    }

    public async Task<ProductDto> CreateProductAsync(int storeId, ProductUpsertRequest req)
    {
        var store = await _db.Stores.FindAsync(storeId) ?? throw new KeyNotFoundException();
        int catId = req.MenuCategoryId;
        if (catId == 0)
        {
            var cat = new MenuCategory
            {
                StoreId = storeId,
                NameAr = string.IsNullOrWhiteSpace(req.NewCategoryNameAr) ? "أصناف" : req.NewCategoryNameAr,
                SortOrder = await _db.MenuCategories.CountAsync(c => c.StoreId == storeId)
            };
            _db.MenuCategories.Add(cat);
            await _db.SaveChangesAsync();
            catId = cat.Id;
        }
        var p = new Product { StoreId = storeId, MenuCategoryId = catId };
        ApplyProduct(p, req);
        _db.Products.Add(p);
        await _db.SaveChangesAsync();
        return (await LoadProduct(p.Id))!;
    }

    public async Task<ProductDto> UpdateProductAsync(int id, ProductUpsertRequest req)
    {
        var p = await _db.Products.FindAsync(id) ?? throw new KeyNotFoundException();
        ApplyProduct(p, req);
        if (req.MenuCategoryId > 0) p.MenuCategoryId = req.MenuCategoryId;
        await _db.SaveChangesAsync();
        return (await LoadProduct(id))!;
    }

    public async Task SetStockAsync(int id, bool inStock)
    {
        var p = await _db.Products.FindAsync(id) ?? throw new KeyNotFoundException();
        p.InStock = inStock;
        await _db.SaveChangesAsync();
    }

    public async Task DeleteProductAsync(int id)
    {
        var p = await _db.Products.FindAsync(id) ?? throw new KeyNotFoundException();
        _db.Products.Remove(p);
        await _db.SaveChangesAsync();
    }

    private static void ApplyProduct(Product p, ProductUpsertRequest r)
    {
        p.NameAr = r.NameAr; p.DescAr = r.DescAr; p.Price = r.Price; p.OldPrice = r.OldPrice;
        p.ImageUrl = r.ImageUrl; p.IsPopular = r.IsPopular; p.Calories = r.Calories;
        p.UnitAr = r.UnitAr; p.InStock = r.InStock; p.SortOrder = r.SortOrder;
    }

    private async Task<ProductDto?> LoadProduct(int id)
    {
        var p = await _db.Products.Include(x => x.OptionGroups).ThenInclude(g => g.Choices)
            .FirstOrDefaultAsync(x => x.Id == id);
        return p?.ToDto(_img);
    }
}
