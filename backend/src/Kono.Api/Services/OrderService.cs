using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class OrderService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;

    public OrderService(KonoDbContext db, ImageUrlBuilder img)
    {
        _db = db;
        _img = img.Func;
    }

    public async Task<OrderDto> PlaceAsync(int customerId, PlaceOrderRequest req)
    {
        var store = await _db.Stores.FirstOrDefaultAsync(s => s.Id == req.StoreId && s.IsActive)
            ?? throw new InvalidOperationException("المتجر غير متاح.");
        if (store.IsClosed) throw new InvalidOperationException("المتجر مغلق حالياً.");

        var productIds = req.Items.Select(i => i.ProductId).Distinct().ToList();
        var products = await _db.Products
            .Include(p => p.OptionGroups).ThenInclude(g => g.Choices)
            .Where(p => productIds.Contains(p.Id) && p.StoreId == store.Id)
            .ToDictionaryAsync(p => p.Id);

        if (products.Count != productIds.Count)
            throw new InvalidOperationException("أحد الأصناف غير موجود في هذا المتجر.");

        var order = new Order
        {
            ShortCode = await NextShortCodeAsync(),
            CustomerUserId = customerId,
            StoreId = store.Id,
            Stage = OrderStage.Placed,
            PlacedAt = DateTime.UtcNow,
            IsDelivery = req.IsDelivery,
            PaymentLabelAr = req.PaymentLabelAr,
            ScheduledForAr = req.ScheduledForAr,
            NoteAr = req.NoteAr,
            Tip = Math.Max(0, req.Tip),
        };

        if (req.AddressId is { } addrId)
        {
            var addr = await _db.Addresses.FirstOrDefaultAsync(a => a.Id == addrId && a.UserId == customerId)
                ?? throw new InvalidOperationException("العنوان غير موجود.");
            order.AddressLabelAr = addr.LabelAr;
            order.AddressLineAr = addr.LineAr;
            order.AddressLat = addr.Latitude;
            order.AddressLng = addr.Longitude;
        }
        else
        {
            order.AddressLabelAr = req.AddressLabelAr ?? "التوصيل";
            order.AddressLineAr = req.AddressLineAr ?? "";
            order.AddressLat = req.AddressLat;
            order.AddressLng = req.AddressLng;
        }

        foreach (var line in req.Items)
        {
            var p = products[line.ProductId];
            var chosen = (line.OptionIds ?? new())
                .SelectMany(id => p.OptionGroups.SelectMany(g => g.Choices).Where(c => c.Id == id))
                .ToList();
            var unit = p.Price + chosen.Sum(c => c.PriceDelta);
            var qty = Math.Clamp(line.Quantity, 1, 99);
            order.Items.Add(new OrderItem
            {
                ProductId = p.Id,
                NameAr = p.NameAr,
                ImageUrl = p.ImageUrl,
                UnitPrice = unit,
                Quantity = qty,
                SelectedOptionsAr = string.Join(" · ", chosen.Select(c => c.NameAr)),
                NoteAr = line.NoteAr ?? "",
                LineTotal = unit * qty
            });
            p.SoldCount += qty;
        }

        ApplyPricing(order, store, req.PromoCode);
        order.Events.Add(new OrderEvent { Stage = OrderStage.Placed });

        _db.Orders.Add(order);
        await _db.SaveChangesAsync();

        return await GetAsync(customerId, order.Id) ?? throw new InvalidOperationException();
    }

    public async Task<List<OrderDto>> ListForCustomerAsync(int customerId, string status)
    {
        var q = BaseQuery().Where(o => o.CustomerUserId == customerId);
        q = status switch
        {
            "active" => q.Where(o => o.Stage != OrderStage.Delivered && o.Stage != OrderStage.Cancelled),
            "past" => q.Where(o => o.Stage == OrderStage.Delivered || o.Stage == OrderStage.Cancelled),
            _ => q
        };
        return (await q.OrderByDescending(o => o.PlacedAt).ToListAsync())
            .Select(o => o.ToDto(_img)).ToList();
    }

    public async Task<OrderDto?> GetAsync(int customerId, int id)
    {
        var order = await BaseQuery().FirstOrDefaultAsync(o => o.Id == id && o.CustomerUserId == customerId);
        return order?.ToDto(_img);
    }

    public async Task<OrderDto> CancelAsync(int customerId, int id)
    {
        var order = await BaseQuery().FirstOrDefaultAsync(o => o.Id == id && o.CustomerUserId == customerId)
            ?? throw new KeyNotFoundException();
        if (order.Stage is > OrderStage.Preparing)
            throw new InvalidOperationException("لا يمكن إلغاء الطلب بعد بدء التجهيز.");
        order.Stage = OrderStage.Cancelled;
        order.Events.Add(new OrderEvent { Stage = OrderStage.Cancelled, Note = "ألغاه العميل" });
        await _db.SaveChangesAsync();
        return order.ToDto(_img);
    }

    // ── internals ───────────────────────────────────────────────────────────
    private IQueryable<Order> BaseQuery() => _db.Orders
        .Include(o => o.Store)
        .Include(o => o.Customer)
        .Include(o => o.Courier)
        .Include(o => o.Items)
        .Include(o => o.Events);

    private static void ApplyPricing(Order o, Store store, string? promoCode)
    {
        o.Subtotal = o.Items.Sum(i => i.LineTotal);
        o.DeliveryFee = o.IsDelivery ? store.DeliveryFee : 0;
        o.ServiceFee = o.Items.Count == 0 ? 0 : 2;

        decimal discount = store.PromoTagAr != null ? 10 : 0;
        if (string.Equals(promoCode?.Trim(), "KONO20", StringComparison.OrdinalIgnoreCase))
            discount += Math.Min(o.Subtotal * 0.20m, 25);
        o.Discount = Math.Round(discount, 2);

        o.Total = Math.Max(0, o.Subtotal + o.DeliveryFee + o.ServiceFee - o.Discount + o.Tip);
        o.CourierPayout = Math.Round(Math.Max(o.DeliveryFee, 8) + 10 + o.Tip, 2);
    }

    private async Task<string> NextShortCodeAsync()
    {
        for (var i = 0; i < 10; i++)
        {
            var code = $"KON-{Random.Shared.Next(4000, 9999)}";
            if (!await _db.Orders.AnyAsync(o => o.ShortCode == code)) return code;
        }
        return $"KON-{DateTime.UtcNow:HHmmss}";
    }
}
