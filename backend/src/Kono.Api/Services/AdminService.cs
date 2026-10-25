using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class AdminService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;

    public AdminService(KonoDbContext db, ImageUrlBuilder img)
    {
        _db = db;
        _img = img.Func;
    }

    // ── dashboard ───────────────────────────────────────────────────────────
    public async Task<AdminStatsDto> StatsAsync()
    {
        var now = DateTime.UtcNow;
        var today = now.Date;
        var d7 = today.AddDays(-6);
        var d30 = today.AddDays(-29);

        var paidStages = new[] { OrderStage.Delivered };
        var orders = await _db.Orders.AsNoTracking().ToListAsync();

        decimal Rev(DateTime from) => orders
            .Where(o => o.PlacedAt >= from && o.Stage != OrderStage.Cancelled)
            .Sum(o => o.Total);

        var byStage = orders.GroupBy(o => o.Stage)
            .Select(g => new CountByLabel(g.Key.ToString(), g.Count()))
            .OrderBy(x => x.Label).ToList();

        var revByDay = Enumerable.Range(0, 7).Select(i =>
        {
            var day = d7.AddDays(i);
            var dayOrders = orders.Where(o => o.PlacedAt.Date == day && o.Stage != OrderStage.Cancelled).ToList();
            return new RevenuePoint(day.ToString("ddd"), dayOrders.Sum(o => o.Total), dayOrders.Count);
        }).ToList();

        var items = await _db.OrderItems.AsNoTracking()
            .Include(i => i.Order).ThenInclude(o => o.Store)
            .Where(i => i.ProductId != null)
            .ToListAsync();
        var topProducts = items
            .GroupBy(i => new { i.ProductId, i.NameAr, i.ImageUrl, Store = i.Order.Store.Name })
            .Select(g => new TopProductRow(g.Key.ProductId!.Value, g.Key.NameAr, g.Key.Store,
                g.Sum(x => x.Quantity), g.Sum(x => x.LineTotal), _img(g.Key.ImageUrl)))
            .OrderByDescending(x => x.Sold).Take(6).ToList();

        var storeAgg = orders.Where(o => o.Stage != OrderStage.Cancelled)
            .GroupBy(o => o.StoreId)
            .ToDictionary(g => g.Key, g => (Orders: g.Count(), Revenue: g.Sum(o => o.Total)));
        var stores = await _db.Stores.AsNoTracking().ToListAsync();
        var topStores = stores
            .Select(s => new TopStoreRow(s.Id, s.Name, s.Kind.ToString(),
                storeAgg.TryGetValue(s.Id, out var a) ? a.Orders : 0,
                storeAgg.TryGetValue(s.Id, out var b) ? b.Revenue : 0, s.Rating, _img(s.HeroImageUrl)))
            .OrderByDescending(x => x.Revenue).Take(6).ToList();

        var completed = orders.Where(o => o.Stage == OrderStage.Delivered).ToList();

        var storeKind = stores.ToDictionary(s => s.Id, s => s.Kind);
        var salesByVertical = orders.Where(o => o.Stage != OrderStage.Cancelled)
            .Where(o => storeKind.ContainsKey(o.StoreId))
            .GroupBy(o => storeKind[o.StoreId])
            .Select(g => new VerticalSalesRow(g.Key.ToString(), g.Count(), g.Sum(o => o.Total)))
            .OrderByDescending(x => x.Revenue).ToList();

        return new AdminStatsDto(
            RevenueToday: Rev(today),
            Revenue7d: Rev(d7),
            Revenue30d: Rev(d30),
            OrdersToday: orders.Count(o => o.PlacedAt >= today),
            OrdersActive: orders.Count(o => o.Stage != OrderStage.Delivered && o.Stage != OrderStage.Cancelled),
            OrdersTotal: orders.Count,
            StoresActive: stores.Count(s => s.IsActive),
            CouriersOnline: await _db.Couriers.CountAsync(c => c.IsOnline),
            CouriersTotal: await _db.Couriers.CountAsync(),
            CustomersTotal: await _db.Users.CountAsync(u => u.Role == UserRole.Customer),
            AvgOrderValue: completed.Count == 0 ? 0 : Math.Round(completed.Average(o => o.Total), 2),
            OrdersByStage: byStage,
            RevenueByDay: revByDay,
            SalesByVertical: salesByVertical,
            TopProducts: topProducts,
            TopStores: topStores);
    }

    // ── orders ──────────────────────────────────────────────────────────────
    public async Task<PagedResult<AdminOrderRow>> OrdersAsync(string? status, int? storeId, PageQuery page)
    {
        var q = _db.Orders.AsNoTracking().Include(o => o.Customer).Include(o => o.Store).Include(o => o.Courier)
            .Include(o => o.Items).AsQueryable();
        if (storeId is { } sid) q = q.Where(o => o.StoreId == sid);
        q = status switch
        {
            "active" => q.Where(o => o.Stage != OrderStage.Delivered && o.Stage != OrderStage.Cancelled),
            "completed" => q.Where(o => o.Stage == OrderStage.Delivered),
            "cancelled" => q.Where(o => o.Stage == OrderStage.Cancelled),
            _ => q
        };
        var total = await q.CountAsync();
        var rows = await q.OrderByDescending(o => o.PlacedAt).Skip(page.Skip).Take(page.PageSize)
            .Select(o => new AdminOrderRow(o.Id, o.ShortCode, o.Customer.NameAr, o.Store.Name,
                o.Stage.ToString(), o.Courier != null ? o.Courier.Name : null,
                o.Total, o.Items.Sum(i => i.Quantity), o.PlacedAt))
            .ToListAsync();
        return new PagedResult<AdminOrderRow>(rows, page.Page, page.PageSize, total);
    }

    public async Task<OrderDetailDto?> OrderAsync(int id)
    {
        var o = await _db.Orders
            .Include(x => x.Store).ThenInclude(s => s.Owner)
            .Include(x => x.Customer).Include(x => x.Courier)
            .Include(x => x.Items).Include(x => x.Events)
            .FirstOrDefaultAsync(x => x.Id == id);
        return o?.ToDetail(_img);
    }

    public async Task<OrderDto> SetStageAsync(int id, OrderStage stage)
    {
        var o = await _db.Orders.Include(x => x.Events).Include(x => x.Store).Include(x => x.Customer)
            .Include(x => x.Courier).Include(x => x.Items)
            .FirstOrDefaultAsync(x => x.Id == id) ?? throw new KeyNotFoundException();
        o.Stage = stage;
        if (stage == OrderStage.Delivered) o.DeliveredAt = DateTime.UtcNow;
        o.Events.Add(new OrderEvent { Stage = stage, Note = "تعديل من الإدارة" });
        await _db.SaveChangesAsync();
        return o.ToDto(_img);
    }

    // ── live operations map ─────────────────────────────────────────────────
    public async Task<LiveBoardDto> LiveBoardAsync()
    {
        var active = new List<OrderStage>
        {
            OrderStage.Placed, OrderStage.Confirmed, OrderStage.Preparing, OrderStage.ReadyForPickup,
            OrderStage.CourierAssigned, OrderStage.PickedUp, OrderStage.OnTheWay, OrderStage.Arrived,
        };

        var orders = await _db.Orders.AsNoTracking()
            .Include(o => o.Store)
            .Include(o => o.Customer)
            .Include(o => o.Courier)
            .Include(o => o.Items)
            .Where(o => active.Contains(o.Stage))
            .OrderByDescending(o => o.PlacedAt)
            .ToListAsync();

        var list = orders.Select(o => new LiveOrderDto(
            o.Id, o.ShortCode, o.Stage.ToString(), StageArabic(o.Stage), o.Total, o.ItemCount,
            o.PlacedAt, o.Customer.NameAr, o.Courier?.Name,
            o.Store.Name, o.Store.Latitude, o.Store.Longitude,
            string.IsNullOrWhiteSpace(o.AddressLineAr) ? o.AddressLabelAr : o.AddressLineAr,
            o.AddressLat, o.AddressLng,
            o.Courier?.Latitude, o.Courier?.Longitude)).ToList();

        double clat = 24.7136, clng = 46.6753;
        if (list.Count > 0)
        {
            clat = list.Average(x => x.StoreLat);
            clng = list.Average(x => x.StoreLng);
        }

        return new LiveBoardDto(list, clat, clng, list.Count,
            list.Count(x => x.CourierNameAr is null));
    }

    private static string StageArabic(OrderStage s) => s switch
    {
        OrderStage.Placed => "بانتظار التأكيد",
        OrderStage.Confirmed => "مؤكَّد",
        OrderStage.Preparing => "قيد التجهيز",
        OrderStage.ReadyForPickup => "جاهز للاستلام",
        OrderStage.CourierAssigned => "أُسند لمندوب",
        OrderStage.PickedUp => "استلمه المندوب",
        OrderStage.OnTheWay => "في الطريق",
        OrderStage.Arrived => "وصل للعميل",
        OrderStage.Delivered => "تم التوصيل",
        OrderStage.Cancelled => "ملغى",
        _ => s.ToString(),
    };

    // ── couriers ────────────────────────────────────────────────────────────
    public async Task<List<CourierAdminRow>> CouriersAsync() =>
        await _db.Couriers.AsNoTracking().Include(c => c.User)
            .Select(c => new CourierAdminRow(c.Id, c.UserId, c.Name, c.User.Phone, c.Rating, c.Trips,
                c.VehicleAr, c.PlateAr, c.IsOnline, c.AcceptanceRate, c.TodayEarnings, c.TodayTrips))
            .ToListAsync();

    public async Task<CourierAdminRow> CreateCourierAsync(CourierCreateRequest req)
    {
        var phone = NormalizePhone(req.Phone);
        var user = await _db.Users.Include(u => u.CourierProfile)
            .FirstOrDefaultAsync(u => u.Phone == phone);
        if (user is not null)
        {
            if (user.Role != UserRole.Courier)
                throw new InvalidOperationException("رقم الجوال مستخدم لحساب آخر.");
            if (user.CourierProfile is not null)
                throw new InvalidOperationException("يوجد مندوب مسجّل بهذا الرقم بالفعل.");
        }
        else
        {
            user = new User
            {
                Phone = phone,
                NameAr = req.Name.Trim(),
                Role = UserRole.Courier,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(Guid.NewGuid().ToString()),
            };
            _db.Users.Add(user);
        }

        var c = new Courier
        {
            User = user,
            Name = req.Name.Trim(),
            VehicleAr = string.IsNullOrWhiteSpace(req.VehicleAr) ? "دباب" : req.VehicleAr.Trim(),
            PlateAr = (req.PlateAr ?? "").Trim(),
            AcceptanceRate = Math.Clamp(req.AcceptanceRate, 0, 100),
            IsOnline = req.IsOnline,
            Rating = 5.0,
            Latitude = 24.774, Longitude = 46.738,
        };
        _db.Couriers.Add(c);
        await _db.SaveChangesAsync();

        return new CourierAdminRow(c.Id, user.Id, c.Name, user.Phone, c.Rating, c.Trips,
            c.VehicleAr, c.PlateAr, c.IsOnline, c.AcceptanceRate, c.TodayEarnings, c.TodayTrips);
    }

    private static string NormalizePhone(string phone)
    {
        var digits = new string((phone ?? "").Where(char.IsDigit).ToArray());
        if (digits.StartsWith("00966")) digits = digits[5..];
        if (digits.StartsWith("966")) digits = digits[3..];
        if (digits.StartsWith('0')) digits = digits[1..];
        return "+966" + digits;
    }

    public async Task<CourierAdminRow> UpdateCourierAsync(int id, CourierUpdateRequest req)
    {
        var c = await _db.Couriers.Include(x => x.User).FirstOrDefaultAsync(x => x.Id == id)
            ?? throw new KeyNotFoundException();
        c.Name = req.Name; c.VehicleAr = req.VehicleAr; c.PlateAr = req.PlateAr;
        c.AcceptanceRate = Math.Clamp(req.AcceptanceRate, 0, 100); c.IsOnline = req.IsOnline;
        await _db.SaveChangesAsync();
        return new CourierAdminRow(c.Id, c.UserId, c.Name, c.User.Phone, c.Rating, c.Trips,
            c.VehicleAr, c.PlateAr, c.IsOnline, c.AcceptanceRate, c.TodayEarnings, c.TodayTrips);
    }

    // ── customers ───────────────────────────────────────────────────────────
    public async Task<PagedResult<CustomerAdminRow>> CustomersAsync(string? search, PageQuery page)
    {
        var q = _db.Users.AsNoTracking().Where(u => u.Role == UserRole.Customer);
        if (!string.IsNullOrWhiteSpace(search))
        {
            var t = search.Trim();
            q = q.Where(u => u.NameAr.Contains(t) || u.Phone.Contains(t) || (u.Email != null && u.Email.Contains(t)));
        }
        var total = await q.CountAsync();
        var users = await q.OrderByDescending(u => u.CreatedAt).Skip(page.Skip).Take(page.PageSize).ToListAsync();
        var ids = users.Select(u => u.Id).ToList();
        var agg = await _db.Orders.Where(o => ids.Contains(o.CustomerUserId) && o.Stage != OrderStage.Cancelled)
            .GroupBy(o => o.CustomerUserId)
            .Select(g => new { g.Key, Count = g.Count(), Spend = g.Sum(o => o.Total) })
            .ToDictionaryAsync(x => x.Key, x => (x.Count, x.Spend));
        var rows = users.Select(u => new CustomerAdminRow(u.Id, u.NameAr, u.Phone, u.Email,
            u.KonoPlus, u.WalletBalance, u.Points, u.IsActive,
            agg.TryGetValue(u.Id, out var a) ? a.Count : 0,
            agg.TryGetValue(u.Id, out var b) ? b.Spend : 0, u.CreatedAt)).ToList();
        return new PagedResult<CustomerAdminRow>(rows, page.Page, page.PageSize, total);
    }

    public async Task SetUserActiveAsync(int id, bool active)
    {
        var u = await _db.Users.FindAsync(id) ?? throw new KeyNotFoundException();
        u.IsActive = active;
        await _db.SaveChangesAsync();
    }

    // ── verticals ───────────────────────────────────────────────────────────
    public async Task<List<VerticalDto>> VerticalsAsync() =>
        (await _db.Verticals.OrderBy(v => v.SortOrder).ToListAsync()).Select(v => v.ToDto(_img)).ToList();

    public async Task<VerticalDto> UpdateVerticalAsync(int id, VerticalUpdateRequest req)
    {
        var v = await _db.Verticals.FindAsync(id) ?? throw new KeyNotFoundException();
        v.TitleAr = req.TitleAr; v.SubtitleAr = req.SubtitleAr; v.Icon = req.Icon;
        v.ImageUrl = req.ImageUrl; v.SortOrder = req.SortOrder; v.IsActive = req.IsActive;
        await _db.SaveChangesAsync();
        return v.ToDto(_img);
    }

    // ── promotions ──────────────────────────────────────────────────────────
    public async Task<List<PromotionDto>> PromotionsAsync() =>
        (await _db.Promotions.OrderBy(p => p.SortOrder).ToListAsync()).Select(p => p.ToDto(_img)).ToList();

    public async Task<PromotionDto> CreatePromotionAsync(PromotionUpsertRequest req)
    {
        var p = new Promotion { TitleAr = req.TitleAr, SubtitleAr = req.SubtitleAr, ImageUrl = req.ImageUrl,
            BackgroundHex = req.BackgroundHex, SortOrder = req.SortOrder, IsActive = req.IsActive };
        _db.Promotions.Add(p);
        await _db.SaveChangesAsync();
        return p.ToDto(_img);
    }

    public async Task<PromotionDto> UpdatePromotionAsync(int id, PromotionUpsertRequest req)
    {
        var p = await _db.Promotions.FindAsync(id) ?? throw new KeyNotFoundException();
        p.TitleAr = req.TitleAr; p.SubtitleAr = req.SubtitleAr; p.ImageUrl = req.ImageUrl;
        p.BackgroundHex = req.BackgroundHex; p.SortOrder = req.SortOrder; p.IsActive = req.IsActive;
        await _db.SaveChangesAsync();
        return p.ToDto(_img);
    }

    public async Task DeletePromotionAsync(int id)
    {
        var p = await _db.Promotions.FindAsync(id) ?? throw new KeyNotFoundException();
        _db.Promotions.Remove(p);
        await _db.SaveChangesAsync();
    }
}
