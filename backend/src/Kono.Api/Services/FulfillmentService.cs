using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

/// <summary>Merchant + courier sides of the order lifecycle.</summary>
public class FulfillmentService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;

    public FulfillmentService(KonoDbContext db, ImageUrlBuilder img)
    {
        _db = db;
        _img = img.Func;
    }

    // ── merchant ────────────────────────────────────────────────────────────
    public async Task<List<MerchantTicketDto>> TicketsAsync(int merchantUserId, string? state)
    {
        var storeIds = await _db.Stores.Where(s => s.OwnerUserId == merchantUserId).Select(s => s.Id).ToListAsync();
        if (storeIds.Count == 0) storeIds = await _db.Stores.Select(s => s.Id).ToListAsync(); // demo: no owner set → all

        var q = TicketQuery().Where(o => storeIds.Contains(o.StoreId) && o.Stage != OrderStage.Cancelled);
        var list = (await q.OrderByDescending(o => o.PlacedAt).ToListAsync())
            .Select(o => o.ToTicket(_img)).ToList();
        if (!string.IsNullOrWhiteSpace(state))
            list = list.Where(t => t.State == state).ToList();
        return list;
    }

    public async Task<MerchantTicketDto> MerchantAdvanceAsync(int merchantUserId, int orderId, string action)
    {
        var order = await TicketQuery().FirstOrDefaultAsync(o => o.Id == orderId)
            ?? throw new KeyNotFoundException();

        order.Stage = action switch
        {
            "accept" when order.Stage == OrderStage.Placed => OrderStage.Confirmed,
            "prepare" when order.Stage == OrderStage.Confirmed => OrderStage.Preparing,
            "ready" when order.Stage == OrderStage.Preparing => OrderStage.ReadyForPickup,
            "advance" => Next(order.Stage),
            "reject" => OrderStage.Cancelled,
            "handover" when order.Stage == OrderStage.ReadyForPickup && !order.IsDelivery => OrderStage.Delivered,
            _ => throw new InvalidOperationException($"لا يمكن تنفيذ '{action}' على طلب في حالة {order.Stage}.")
        };
        if (order.Stage == OrderStage.Delivered) order.DeliveredAt = DateTime.UtcNow;
        order.Events.Add(new OrderEvent { Stage = order.Stage, Note = action == "reject" ? "رفضه المتجر" : null });
        await _db.SaveChangesAsync();
        return order.ToTicket(_img);
    }

    // ── merchant: menu + status ─────────────────────────────────────────────
    private async Task<List<int>> OwnedStoreIdsAsync(int merchantUserId)
    {
        var ids = await _db.Stores.Where(s => s.OwnerUserId == merchantUserId).Select(s => s.Id).ToListAsync();
        if (ids.Count == 0) ids = await _db.Stores.Select(s => s.Id).Take(1).ToListAsync(); // demo fallback
        return ids;
    }

    public async Task<MerchantStoreDto> StoreAsync(int merchantUserId)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var store = await _db.Stores.Include(s => s.Owner).AsNoTracking()
            .FirstOrDefaultAsync(s => storeIds.Contains(s.Id))
            ?? throw new InvalidOperationException("لا يوجد متجر لهذا الحساب.");
        var itemCount = await _db.Products.CountAsync(p => p.StoreId == store.Id);
        var me = await _db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == merchantUserId);
        return new MerchantStoreDto(
            store.Id, store.Name, store.Kind.ToString(), store.CuisineAr,
            store.Rating, store.RatingCount, store.PrepMinutes,
            store.IsBusy, store.IsClosed,
            _img(store.HeroImageUrl), store.AddressAr, store.CityAr,
            store.OpeningHoursAr, store.IbanLast4, store.DeliveryRadiusKm,
            itemCount,
            store.Owner?.NameAr ?? me?.NameAr ?? "—",
            me?.Phone ?? store.Owner?.Phone ?? "",
            me?.OnboardingStatus ?? "",
            _img(store.LogoImageUrl));
    }

    public async Task<MerchantStoreDto> UpdateStoreAsync(int merchantUserId, MerchantStoreUpdateRequest req)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var store = await _db.Stores.FirstOrDefaultAsync(s => storeIds.Contains(s.Id))
            ?? throw new InvalidOperationException("لا يوجد متجر لهذا الحساب.");

        if (!string.IsNullOrWhiteSpace(req.Name)) store.Name = req.Name.Trim();
        if (!string.IsNullOrWhiteSpace(req.CuisineAr)) store.CuisineAr = req.CuisineAr.Trim();
        if (!string.IsNullOrWhiteSpace(req.AddressAr)) store.AddressAr = req.AddressAr.Trim();
        if (!string.IsNullOrWhiteSpace(req.OpeningHoursAr)) store.OpeningHoursAr = req.OpeningHoursAr.Trim();
        if (req.IbanLast4 is not null)
            store.IbanLast4 = new string(req.IbanLast4.Where(char.IsDigit).ToArray());
        await _db.SaveChangesAsync();
        return await StoreAsync(merchantUserId);
    }

    public async Task<List<MerchantMenuItemDto>> MenuAsync(int merchantUserId)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var products = await _db.Products
            .Include(p => p.MenuCategory)
            .Where(p => storeIds.Contains(p.StoreId))
            .OrderBy(p => p.MenuCategoryId).ThenBy(p => p.SortOrder)
            .ToListAsync();
        return products.Select(p => new MerchantMenuItemDto(
            p.Id, p.NameAr, p.DescAr, p.Price, _img(p.ImageUrl),
            p.InStock, p.SoldCount, p.MenuCategory?.NameAr ?? "أصناف")).ToList();
    }

    public async Task<MerchantMenuItemDto> UpsertMenuItemAsync(int merchantUserId, int? productId, MerchantMenuUpsertRequest req)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var storeId = storeIds.First();
        Product product;
        if (productId is int pid)
        {
            product = await _db.Products.FirstOrDefaultAsync(p => p.Id == pid && storeIds.Contains(p.StoreId))
                ?? throw new KeyNotFoundException();
        }
        else
        {
            var category = await _db.MenuCategories
                .FirstOrDefaultAsync(c => c.StoreId == storeId && c.NameAr == req.CategoryAr);
            if (category is null)
            {
                category = new MenuCategory { StoreId = storeId, NameAr = req.CategoryAr, SortOrder = 99 };
                _db.MenuCategories.Add(category);
                await _db.SaveChangesAsync();
            }
            product = new Product { StoreId = storeId, MenuCategoryId = category.Id, SortOrder = 99 };
            _db.Products.Add(product);
        }

        product.NameAr = req.NameAr;
        product.DescAr = req.DescAr;
        product.Price = req.Price;
        product.InStock = req.InStock;
        if (!string.IsNullOrWhiteSpace(req.ImageUrl)) product.ImageUrl = req.ImageUrl!;
        await _db.SaveChangesAsync();

        await _db.Entry(product).Reference(p => p.MenuCategory).LoadAsync();
        return new MerchantMenuItemDto(product.Id, product.NameAr, product.DescAr, product.Price,
            _img(product.ImageUrl), product.InStock, product.SoldCount, product.MenuCategory?.NameAr ?? "أصناف");
    }

    public async Task SetMerchantStatusAsync(int merchantUserId, MerchantStatusRequest req)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var stores = await _db.Stores.Where(s => storeIds.Contains(s.Id)).ToListAsync();
        foreach (var s in stores)
        {
            if (req.IsOnline is bool on) s.IsClosed = !on;
            if (req.IsBusy is bool busy) s.IsBusy = busy;
            if (req.PrepMinutes is int m) s.PrepMinutes = Math.Clamp(m, 5, 90);
        }
        await _db.SaveChangesAsync();
    }

    public async Task<MerchantReportsDto> ReportsAsync(int merchantUserId)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var since = DateTime.UtcNow.AddDays(-7);
        var orders = await _db.Orders
            .Include(o => o.Items)
            .Where(o => storeIds.Contains(o.StoreId) && o.Stage != OrderStage.Cancelled && o.PlacedAt >= since)
            .ToListAsync();

        string[] wd = { "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت" };
        var days = Enumerable.Range(0, 7).Select(i =>
        {
            var day = DateTime.UtcNow.Date.AddDays(-6 + i);
            var forDay = orders.Where(o => o.PlacedAt.Date == day).ToList();
            return new EarningsDayDto(wd[(int)day.DayOfWeek], forDay.Sum(o => o.Total), forDay.Count);
        }).ToList();

        var total = orders.Sum(o => o.Total);
        var top = orders.SelectMany(o => o.Items)
            .GroupBy(i => i.NameAr)
            .Select(g => new MerchantTopItemDto(g.Key, g.Sum(i => i.Quantity), g.Sum(i => i.LineTotal)))
            .OrderByDescending(t => t.Count).Take(5).ToList();

        var store = await _db.Stores.FirstOrDefaultAsync(s => storeIds.Contains(s.Id));
        return new MerchantReportsDto(
            total, orders.Count,
            orders.Count == 0 ? 0 : Math.Round(total / orders.Count, 2),
            store?.Rating ?? 0, store?.PrepMinutes ?? 15, days, top);
    }

    // ── courier ─────────────────────────────────────────────────────────────
    public async Task<CourierStatusRequest> SetStatusAsync(int courierUserId, CourierStatusRequest req)
    {
        var courier = await GetCourierAsync(courierUserId);
        courier.IsOnline = req.IsOnline;
        courier.Latitude = req.Latitude;
        courier.Longitude = req.Longitude;
        await _db.SaveChangesAsync();
        return req;
    }

    public async Task<List<CourierJobDto>> OffersAsync(int courierUserId)
    {
        var courier = await GetCourierAsync(courierUserId);
        if (!courier.IsOnline) return new();
        var orders = await JobQuery()
            .Where(o => o.IsDelivery && o.CourierId == null && o.Stage == OrderStage.ReadyForPickup)
            .OrderBy(o => o.PlacedAt)
            .Take(5)
            .ToListAsync();
        return orders.Select(o => o.ToJob()).ToList();
    }

    public async Task<CourierJobDto?> ActiveJobAsync(int courierUserId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var order = await JobQuery().FirstOrDefaultAsync(o =>
            o.CourierId == courier.Id &&
            o.Stage >= OrderStage.CourierAssigned && o.Stage < OrderStage.Delivered);
        return order?.ToJob();
    }

    public async Task<CourierJobDto> AcceptAsync(int courierUserId, int orderId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var order = await JobQuery().FirstOrDefaultAsync(o => o.Id == orderId)
            ?? throw new KeyNotFoundException();
        if (order.CourierId != null) throw new InvalidOperationException("الرحلة أُسندت لمندوب آخر.");
        if (order.Stage != OrderStage.ReadyForPickup) throw new InvalidOperationException("الطلب غير جاهز للاستلام.");

        order.CourierId = courier.Id;
        order.Stage = OrderStage.CourierAssigned;
        order.Events.Add(new OrderEvent { Stage = OrderStage.CourierAssigned, Note = $"المندوب {courier.Name}" });
        await _db.SaveChangesAsync();
        return order.ToJob();
    }

    public async Task<CourierJobDto> CourierAdvanceAsync(int courierUserId, int orderId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var order = await JobQuery().FirstOrDefaultAsync(o => o.Id == orderId && o.CourierId == courier.Id)
            ?? throw new KeyNotFoundException();
        if (order.Stage is < OrderStage.CourierAssigned or >= OrderStage.Delivered)
            throw new InvalidOperationException("لا توجد مرحلة تالية.");

        order.Stage = Next(order.Stage);
        order.Events.Add(new OrderEvent { Stage = order.Stage });

        if (order.Stage == OrderStage.Delivered)
        {
            order.DeliveredAt = DateTime.UtcNow;
            courier.Trips++;
            courier.TodayTrips++;
            courier.TodayEarnings += order.CourierPayout;
        }
        await _db.SaveChangesAsync();
        return order.ToJob();
    }

    public async Task<CourierSummaryDto> SummaryAsync(int courierUserId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var since = DateTime.UtcNow.AddDays(-7);
        var delivered = await _db.Orders
            .Include(o => o.Store)
            .Where(o => o.CourierId == courier.Id && o.Stage == OrderStage.Delivered
                        && o.DeliveredAt != null && o.DeliveredAt >= since)
            .OrderByDescending(o => o.DeliveredAt)
            .ToListAsync();

        string[] wd = { "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت" };
        var week = Enumerable.Range(0, 7).Select(i =>
        {
            var day = DateTime.UtcNow.Date.AddDays(-6 + i);
            var forDay = delivered.Where(o => o.DeliveredAt!.Value.Date == day).ToList();
            return new EarningsDayDto(wd[(int)day.DayOfWeek], forDay.Sum(o => o.CourierPayout), forDay.Count);
        }).ToList();

        var total = courier.TodayEarnings;
        var today = DateTime.UtcNow.Date;
        var tips = delivered.Where(o => o.DeliveredAt!.Value.Date == today).Sum(o => o.Tip);
        var bonus = Math.Round(total * 0.06m, 2);
        var delivery = Math.Max(0, total - tips - bonus);
        var trips = delivered.Take(10).Select(o => new CourierTripDto(
            o.ShortCode, RelativeAr(o.DeliveredAt!.Value), o.CourierPayout + o.Tip, courier.Rating)).ToList();

        return new CourierSummaryDto(
            total, courier.TodayTrips, courier.TodayDistanceKm,
            delivery, tips, bonus, week, trips);
    }

    private static string RelativeAr(DateTime utc)
    {
        var local = utc.AddHours(3); // KSA
        var day = (DateTime.UtcNow.AddHours(3).Date - local.Date).Days;
        var t = local.ToString("h:mm tt");
        return day switch { 0 => $"اليوم {t}", 1 => $"أمس {t}", _ => local.ToString("dd/MM h:mm tt") };
    }

    public async Task<CourierProfileDto> MeAsync(int courierUserId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var status = await _db.Users.Where(u => u.Id == courierUserId)
            .Select(u => u.OnboardingStatus).FirstOrDefaultAsync() ?? "";

        var assigned = await _db.Orders
            .Where(o => o.CourierId == courier.Id)
            .Select(o => o.Stage)
            .ToListAsync();
        var finished = assigned.Count(s => s is OrderStage.Delivered or OrderStage.Cancelled);
        var delivered = assigned.Count(s => s == OrderStage.Delivered);
        var completion = finished == 0 ? 100 : (int)Math.Round(100.0 * delivered / finished);

        return new CourierProfileDto(
            courier.Id, courier.Name, courier.Rating, courier.Trips, courier.Trips,
            courier.VehicleAr, courier.PlateAr, courier.PhotoUrl,
            courier.IsOnline, courier.AcceptanceRate, completion,
            courier.Latitude, courier.Longitude,
            courier.TodayEarnings, courier.TodayTrips, courier.TodayDistanceKm,
            status);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private static OrderStage Next(OrderStage s) =>
        s >= OrderStage.Delivered ? OrderStage.Delivered : s + 1;

    private IQueryable<Order> TicketQuery() => _db.Orders
        .Include(o => o.Store).ThenInclude(s => s.Owner)
        .Include(o => o.Customer).Include(o => o.Courier)
        .Include(o => o.Items).Include(o => o.Events);

    private IQueryable<Order> JobQuery() => _db.Orders
        .Include(o => o.Store).ThenInclude(s => s.Owner)
        .Include(o => o.Customer).Include(o => o.Courier)
        .Include(o => o.Items).Include(o => o.Events);

    // ── full order detail (merchant + courier) ──────────────────────────────
    public async Task<OrderDetailDto> MerchantOrderDetailAsync(int merchantUserId, int orderId)
    {
        var storeIds = await OwnedStoreIdsAsync(merchantUserId);
        var order = await TicketQuery().AsNoTracking()
            .FirstOrDefaultAsync(o => o.Id == orderId)
            ?? throw new KeyNotFoundException("الطلب غير موجود.");
        if (!storeIds.Contains(order.StoreId))
            throw new UnauthorizedAccessException("هذا الطلب ليس من متجرك.");
        return order.ToDetail(_img);
    }

    public async Task<OrderDetailDto> CourierOrderDetailAsync(int courierUserId, int orderId)
    {
        var courier = await GetCourierAsync(courierUserId);
        var order = await JobQuery().AsNoTracking()
            .FirstOrDefaultAsync(o => o.Id == orderId)
            ?? throw new KeyNotFoundException("الطلب غير موجود.");
        // assigned to me, or still an open offer any online courier may inspect
        if (order.CourierId != courier.Id && order.Stage != OrderStage.ReadyForPickup)
            throw new UnauthorizedAccessException("هذه الرحلة ليست لك.");
        return order.ToDetail(_img);
    }

    private async Task<Courier> GetCourierAsync(int userId) =>
        await _db.Couriers.FirstOrDefaultAsync(c => c.UserId == userId)
        ?? throw new InvalidOperationException("لا يوجد ملف مندوب لهذا الحساب.");
}
