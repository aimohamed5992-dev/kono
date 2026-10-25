using Kono.Api.Domain;
using Kono.Api.Dtos;

namespace Kono.Api.Services;

public static class OrderMapping
{
    public static OrderItemDto ToDto(this OrderItem i, Func<string, string> img) =>
        new(i.Id, i.ProductId, i.NameAr, img(i.ImageUrl), i.UnitPrice, i.Quantity,
            i.SelectedOptionsAr, i.NoteAr, i.LineTotal);

    public static OrderDto ToDto(this Order o, Func<string, string> img) =>
        new(o.Id, o.ShortCode, o.Stage.ToString(), OrderDto.Display(o.Stage),
            o.StoreId, o.Store.Name, img(o.Store.HeroImageUrl), o.Store.EtaMinutes,
            o.PlacedAt, o.DeliveredAt, o.IsDelivery, o.PaymentLabelAr, o.ScheduledForAr, o.NoteAr,
            o.AddressLabelAr, o.AddressLineAr, o.AddressLat, o.AddressLng,
            o.Subtotal, o.DeliveryFee, o.ServiceFee, o.Discount, o.Tip, o.Total, o.ItemCount,
            o.Items.Select(x => x.ToDto(img)).ToList(),
            o.Events.OrderBy(e => e.At).Select(e => new OrderEventDto(e.Stage.ToString(), e.At, e.Note)).ToList(),
            o.Courier is null ? null : new CourierMiniDto(
                o.Courier.Id, o.Courier.Name, o.Courier.Rating, o.Courier.Trips,
                o.Courier.VehicleAr, o.Courier.PlateAr, img(o.Courier.PhotoUrl),
                o.Courier.Latitude, o.Courier.Longitude));

    public static string StageArabic(OrderStage s) => s switch
    {
        OrderStage.Placed => "بانتظار التأكيد",
        OrderStage.Confirmed => "مؤكَّد",
        OrderStage.Preparing => "قيد التجهيز",
        OrderStage.ReadyForPickup => "جاهز للاستلام",
        OrderStage.CourierAssigned => "أُسند لمندوب",
        OrderStage.PickedUp => "استلمه المندوب",
        OrderStage.OnTheWay => "في الطريق للعميل",
        OrderStage.Arrived => "وصل للعميل",
        OrderStage.Delivered => "تم التوصيل",
        OrderStage.Cancelled => "ملغى",
        _ => s.ToString(),
    };

    public static OrderDetailDto ToDetail(this Order o, Func<string, string> img)
    {
        var dist = Haversine(o.Store.Latitude, o.Store.Longitude, o.AddressLat, o.AddressLng);
        return new OrderDetailDto(
            o.Id, o.ShortCode, o.Stage.ToString(), StageArabic(o.Stage), OrderDto.Display(o.Stage),
            o.PlacedAt, o.DeliveredAt, o.IsDelivery, o.PaymentLabelAr, o.ScheduledForAr, o.NoteAr,
            o.Customer.NameAr, o.Customer.Phone,
            o.AddressLabelAr, o.AddressLineAr, o.AddressLat, o.AddressLng,
            o.StoreId, o.Store.Name, o.Store.CuisineAr, o.Store.AddressAr,
            o.Store.Owner?.Phone ?? "", o.Store.Latitude, o.Store.Longitude,
            img(o.Store.HeroImageUrl), o.Store.EtaMinutes,
            o.Courier is null ? null : new CourierMiniDto(
                o.Courier.Id, o.Courier.Name, o.Courier.Rating, o.Courier.Trips,
                o.Courier.VehicleAr, o.Courier.PlateAr, img(o.Courier.PhotoUrl),
                o.Courier.Latitude, o.Courier.Longitude),
            o.CourierPayout, Math.Round(dist, 1),
            o.Subtotal, o.DeliveryFee, o.ServiceFee, o.Discount, o.Tip, o.Total, o.ItemCount,
            o.Items.Select(x => x.ToDto(img)).ToList(),
            o.Events.OrderBy(e => e.At).Select(e => new OrderEventDto(e.Stage.ToString(), e.At, e.Note)).ToList());
    }

    public static MerchantTicketDto ToTicket(this Order o, Func<string, string> img)
    {
        var state = o.Stage switch
        {
            OrderStage.Placed => "incoming",
            OrderStage.Confirmed or OrderStage.Preparing => "preparing",
            OrderStage.ReadyForPickup => "ready",
            OrderStage.CourierAssigned or OrderStage.PickedUp or OrderStage.OnTheWay
                or OrderStage.Arrived => "handedOver",
            OrderStage.Delivered => "completed",
            _ => "cancelled"
        };
        return new MerchantTicketDto(o.Id, o.ShortCode, o.Customer.NameAr, state,
            o.PlacedAt, o.PlacedAt.AddSeconds(60), o.IsDelivery,
            o.Courier?.Name, o.NoteAr, o.ItemCount, o.Total,
            o.Items.Select(x => x.ToDto(img)).ToList());
    }

    public static CourierJobDto ToJob(this Order o)
    {
        var (stage, cta) = o.Stage switch
        {
            OrderStage.ReadyForPickup => ("offered", "قبول الرحلة"),
            OrderStage.CourierAssigned => ("headingToStore", "وصلت المتجر"),
            OrderStage.PickedUp => ("pickedUp", "بدء التوصيل"),
            OrderStage.OnTheWay => ("headingToCustomer", "وصلت للعميل"),
            OrderStage.Arrived => ("arrived", "تأكيد التسليم"),
            OrderStage.Delivered => ("delivered", "إنهاء"),
            _ => ("offered", "قبول الرحلة")
        };
        var dist = Haversine(o.Store.Latitude, o.Store.Longitude, o.AddressLat, o.AddressLng);
        return new CourierJobDto(o.Id, o.ShortCode, stage, cta,
            o.Store.Name, o.Store.AddressAr, o.Store.Latitude, o.Store.Longitude,
            o.Customer.NameAr, o.AddressLineAr, o.AddressLat, o.AddressLng,
            Math.Round(dist, 1), Math.Max(8, (int)(dist * 3.5)), o.CourierPayout, o.ItemCount,
            o.Customer?.Phone ?? "");
    }

    public static double Haversine(double lat1, double lon1, double lat2, double lon2)
    {
        if (lat1 == 0 || lat2 == 0) return 3.4;
        const double r = 6371;
        double dLat = (lat2 - lat1) * Math.PI / 180, dLon = (lon2 - lon1) * Math.PI / 180;
        double a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                   Math.Cos(lat1 * Math.PI / 180) * Math.Cos(lat2 * Math.PI / 180) *
                   Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
        return r * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
    }
}
