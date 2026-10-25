using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public enum StoreSort { Recommended, Fastest, Rating, Nearest }

public readonly record struct GeoPoint(double Lat, double Lng)
{
    public double DistanceKm(double lat, double lng)
    {
        if (Lat == 0 || lat == 0) return 3;
        const double r = 6371;
        double dLat = (lat - Lat) * Math.PI / 180, dLon = (lng - Lng) * Math.PI / 180;
        double a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                   Math.Cos(Lat * Math.PI / 180) * Math.Cos(lat * Math.PI / 180) *
                   Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        return Math.Round(r * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a)), 1);
    }
}

public record StoreFilter(
    VerticalKind? Kind = null,
    string? Search = null,
    StoreSort Sort = StoreSort.Recommended,
    bool FreeDeliveryOnly = false,
    bool OpenNowOnly = false,
    bool PromoOnly = false,
    GeoPoint? Origin = null,
    double? RadiusKm = null);

public class CatalogService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;

    public CatalogService(KonoDbContext db, ImageUrlBuilder img)
    {
        _db = db;
        _img = img.Func;
    }

    public async Task<List<VerticalDto>> GetVerticalsAsync() =>
        (await _db.Verticals.AsNoTracking().Where(v => v.IsActive).OrderBy(v => v.SortOrder).ToListAsync())
        .Select(v => v.ToDto(_img)).ToList();

    public async Task<List<PromotionDto>> GetPromotionsAsync() =>
        (await _db.Promotions.AsNoTracking().Where(p => p.IsActive).OrderBy(p => p.SortOrder).ToListAsync())
        .Select(p => p.ToDto(_img)).ToList();

    /// <summary>Applies distance from `origin` and orders/filters accordingly.
    /// All ordering happens in memory because it depends on the caller's location.</summary>
    private async Task<List<Store>> QueryStoresAsync(StoreFilter f)
    {
        var q = _db.Stores.AsNoTracking().Include(s => s.Tags).Where(s => s.IsActive);

        if (f.Kind is { } k) q = q.Where(s => s.Kind == k);
        if (f.FreeDeliveryOnly) q = q.Where(s => s.DeliveryFee == 0);
        if (f.OpenNowOnly) q = q.Where(s => !s.IsClosed);
        if (f.PromoOnly) q = q.Where(s => s.PromoTagAr != null);
        if (!string.IsNullOrWhiteSpace(f.Search))
        {
            var term = f.Search.Trim();
            q = q.Where(s => s.Name.Contains(term) || s.CuisineAr.Contains(term)
                             || s.Tags.Any(t => t.Text.Contains(term)));
        }

        var list = await q.ToListAsync();

        if (f.Origin is { } o)
        {
            foreach (var s in list) s.DistanceKm = o.DistanceKm(s.Latitude, s.Longitude);
            if (f.RadiusKm is { } radius)
                list = list.Where(s => s.DistanceKm <= radius).ToList();
        }

        list = f.Sort switch
        {
            StoreSort.Fastest => list.OrderBy(s => s.EtaMinutes).ToList(),
            StoreSort.Rating => list.OrderByDescending(s => s.Rating).ThenByDescending(s => s.RatingCount).ToList(),
            StoreSort.Nearest => list.OrderBy(s => s.DistanceKm).ToList(),
            _ when f.Origin is not null =>
                list.OrderBy(s => s.DistanceKm).ThenByDescending(s => s.Rating).ToList(),
            _ => list.OrderByDescending(s => s.Rating * (s.RatingCount + 50)).ToList(),
        };
        return list;
    }

    public async Task<PagedResult<StoreListItemDto>> GetStoresAsync(StoreFilter f, PageQuery page)
    {
        var list = await QueryStoresAsync(f);
        var pageItems = list.Skip(page.Skip).Take(page.PageSize).Select(s => s.ToListItem(_img)).ToList();
        return new PagedResult<StoreListItemDto>(pageItems, page.Page, page.PageSize, list.Count);
    }

    public async Task<StoreDetailDto?> GetStoreAsync(int id, GeoPoint? origin = null)
    {
        var store = await _db.Stores.AsNoTracking()
            .Include(s => s.Tags)
            .Include(s => s.Categories).ThenInclude(c => c.Products).ThenInclude(p => p.OptionGroups).ThenInclude(g => g.Choices)
            .FirstOrDefaultAsync(s => s.Id == id && s.IsActive);
        if (store is null) return null;
        if (origin is { } o) store.DistanceKm = o.DistanceKm(store.Latitude, store.Longitude);
        return store.ToDetail(_img);
    }

    public async Task<SearchResultsDto> SearchAsync(string q, string scope, GeoPoint? origin)
    {
        q = (q ?? "").Trim();
        if (q.Length < 1) return new SearchResultsDto([], []);

        var storeList = scope is "items"
            ? new List<Store>()
            : await _db.Stores.AsNoTracking().Include(s => s.Tags)
                .Where(s => s.IsActive && (s.Name.Contains(q) || s.CuisineAr.Contains(q)
                                           || s.Tags.Any(t => t.Text.Contains(q))))
                .Take(20).ToListAsync();
        if (origin is { } o1)
            foreach (var s in storeList) s.DistanceKm = o1.DistanceKm(s.Latitude, s.Longitude);
        var stores = storeList.OrderBy(s => origin is null ? 0 : s.DistanceKm)
            .Select(s => s.ToListItem(_img)).ToList();

        var items = scope is "stores"
            ? new List<ItemHitDto>()
            : (await _db.Products.AsNoTracking().Include(p => p.Store)
                    .Where(p => p.InStock && p.NameAr.Contains(q))
                    .Take(30).ToListAsync())
                .Select(p => new ItemHitDto(p.Id, p.StoreId, p.Store.Name, p.NameAr, p.Price, _img(p.ImageUrl)))
                .ToList();

        return new SearchResultsDto(stores, items);
    }

    /// <summary>The composite payload behind the multi-rail home screen — ordered
    /// by proximity to the given location when supplied.</summary>
    public async Task<HomeFeedDto> GetHomeAsync(GeoPoint? origin)
    {
        var verticals = await GetVerticalsAsync();
        var promos = await GetPromotionsAsync();

        var all = await _db.Stores.AsNoTracking().Include(s => s.Tags).Where(s => s.IsActive).ToListAsync();
        if (origin is { } o)
        {
            foreach (var s in all) s.DistanceKm = o.DistanceKm(s.Latitude, s.Longitude);
            // Address-scoped: prefer stores within ~8 km of the chosen address,
            // but never show an empty home — widen to the 10 closest if too few.
            const double radiusKm = 8;
            var within = all.Where(s => s.DistanceKm <= radiusKm).ToList();
            all = within.Count >= 6
                ? within
                : all.OrderBy(s => s.DistanceKm).Take(Math.Max(10, within.Count)).ToList();
        }

        IEnumerable<Store> near(IEnumerable<Store> src) =>
            origin is null ? src : src.OrderBy(s => s.DistanceKm);

        List<StoreListItemDto> Pick(Func<Store, bool> where,
            Func<IEnumerable<Store>, IEnumerable<Store>> order, int take) =>
            order(near(all.Where(where))).Take(take).Select(s => s.ToListItem(_img)).ToList();

        var rails = new List<HomeRailDto>
        {
            new("nearby", "الأقرب إليك", "خدمات حولك الآن", "compact",
                Pick(_ => true, o2 => o2, 8)),
            new("featured", "مطاعم مميزة", "مختارة لك بعناية", "featured",
                Pick(s => s.Kind == VerticalKind.Restaurants, o2 => o2.OrderByDescending(s => s.Rating), 6)),
            new("deals", "عروض قوية اليوم", "خصومات لوقت محدود", "deal",
                Pick(s => s.PromoTagAr != null, o2 => o2, 8)),
            new("popular", "الأكثر طلباً حولك", null, "compact",
                Pick(s => s.Kind == VerticalKind.Restaurants, o2 => o2.OrderByDescending(s => s.RatingCount), 6)),
            new("groceries", "سوبرماركت ومقاضي", "من بندة ولولو والتميمي", "circle",
                Pick(s => s.Kind == VerticalKind.Groceries, o2 => o2, 8)),
            new("pharmacy", "صيدليات توصلك بسرعة", "دواءك خلال دقائق", "quick",
                Pick(s => s.Kind == VerticalKind.Pharmacy, o2 => o2.OrderBy(s => s.EtaMinutes), 8)),
            new("sweets", "حلا ومناسبات", "كيك وحلويات شرقية", "poster",
                Pick(s => s.Kind == VerticalKind.Sweets, o2 => o2, 6)),
        };
        rails = rails.Where(r => r.Stores.Count > 0).ToList();

        return new HomeFeedDto(verticals, promos, rails);
    }
}
