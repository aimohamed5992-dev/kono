using System.ComponentModel.DataAnnotations;
using Kono.Api.Domain;

namespace Kono.Api.Dtos;

// ── request ─────────────────────────────────────────────────────────────────
public record PlaceOrderRequest(
    [Required] int StoreId,
    [Required, MinLength(1)] List<CartItemInput> Items,
    int? AddressId,
    string? AddressLabelAr,
    string? AddressLineAr,
    double AddressLat,
    double AddressLng,
    bool IsDelivery = true,
    string PaymentLabelAr = "Apple Pay",
    decimal Tip = 0,
    string? ScheduledForAr = null,
    string? PromoCode = null,
    string? NoteAr = null);

public record CartItemInput(
    [Required] int ProductId,
    [Range(1, 99)] int Quantity,
    List<int>? OptionIds = null,
    string? NoteAr = null);

public record CourierStatusRequest(bool IsOnline, double Latitude, double Longitude);

// ── response ────────────────────────────────────────────────────────────────
public record OrderItemDto(int Id, int? ProductId, string NameAr, string ImageUrl,
    decimal UnitPrice, int Quantity, string SelectedOptionsAr, string NoteAr, decimal LineTotal);

public record OrderEventDto(string Stage, DateTime At, string? Note);

public record CourierMiniDto(int Id, string Name, double Rating, int Trips,
    string VehicleAr, string PlateAr, string PhotoUrl, double Latitude, double Longitude);

public record OrderDto(
    int Id, string ShortCode, string Stage, string DisplayStage,
    int StoreId, string StoreName, string StoreImageUrl, int StoreEtaMinutes,
    DateTime PlacedAt, DateTime? DeliveredAt, bool IsDelivery,
    string PaymentLabelAr, string? ScheduledForAr, string? NoteAr,
    string AddressLabelAr, string AddressLineAr, double AddressLat, double AddressLng,
    decimal Subtotal, decimal DeliveryFee, decimal ServiceFee, decimal Discount, decimal Tip, decimal Total,
    int ItemCount,
    IReadOnlyList<OrderItemDto> Items,
    IReadOnlyList<OrderEventDto> Events,
    CourierMiniDto? Courier)
{
    public static string Display(OrderStage s) => s switch
    {
        OrderStage.Placed => "placed",
        OrderStage.Confirmed => "confirmed",
        OrderStage.Preparing => "preparing",
        OrderStage.ReadyForPickup => "readyForPickup",
        OrderStage.CourierAssigned or OrderStage.PickedUp => "readyForPickup",
        OrderStage.OnTheWay or OrderStage.Arrived => "onTheWay",
        OrderStage.Delivered => "delivered",
        _ => "cancelled"
    };
}

/// Everything about one order — customer, store, courier, items, money, timeline.
/// Shared by the merchant ticket screen, the courier job screen and the admin drawer.
public record OrderDetailDto(
    int Id, string ShortCode, string Stage, string StageAr, string DisplayStage,
    DateTime PlacedAt, DateTime? DeliveredAt, bool IsDelivery,
    string PaymentLabelAr, string? ScheduledForAr, string? NoteAr,
    string CustomerNameAr, string CustomerPhone,
    string AddressLabelAr, string AddressLineAr, double AddressLat, double AddressLng,
    int StoreId, string StoreNameAr, string StoreCuisineAr, string StoreAddressAr,
    string StorePhone, double StoreLat, double StoreLng, string StoreImageUrl, int StoreEtaMinutes,
    CourierMiniDto? Courier, decimal CourierPayout, double DistanceKm,
    decimal Subtotal, decimal DeliveryFee, decimal ServiceFee, decimal Discount, decimal Tip, decimal Total,
    int ItemCount,
    IReadOnlyList<OrderItemDto> Items,
    IReadOnlyList<OrderEventDto> Events);

public record MerchantTicketDto(
    int Id, string ShortCode, string CustomerNameAr, string State,
    DateTime PlacedAt, DateTime AcceptDeadline, bool IsDelivery,
    string? CourierNameAr, string? NoteAr,
    int ItemCount, decimal Total,
    IReadOnlyList<OrderItemDto> Items);

public record CourierJobDto(
    int OrderId, string ShortCode, string Stage, string Cta,
    string StoreNameAr, string StoreAddressAr, double StoreLat, double StoreLng,
    string CustomerNameAr, string DropoffAddressAr, double DropLat, double DropLng,
    double DistanceKm, int EstMinutes, decimal Payout, int ItemCount,
    string CustomerPhone = "");

// ── merchant menu ───────────────────────────────────────────────────────────
public record MerchantMenuItemDto(
    int ProductId, string NameAr, string DescAr, decimal Price, string ImageUrl,
    bool InStock, int SoldCount, string CategoryAr);

public record MerchantMenuUpsertRequest(
    string NameAr, string DescAr, decimal Price, bool InStock,
    string? ImageUrl = null, string CategoryAr = "أصناف");

public record MerchantStatusRequest(bool? IsOnline, bool? IsBusy, int? PrepMinutes);

public record MerchantStoreDto(
    int Id, string Name, string Kind, string CuisineAr,
    double Rating, int RatingCount, int PrepMinutes,
    bool IsBusy, bool IsClosed,
    string HeroImageUrl, string AddressAr, string CityAr,
    string OpeningHoursAr, string IbanLast4, double DeliveryRadiusKm,
    int MenuItemCount, string OwnerNameAr, string OwnerPhone,
    string OnboardingStatus, string LogoUrl);

public record MerchantStoreUpdateRequest(
    string? Name = null, string? CuisineAr = null,
    string? AddressAr = null, string? OpeningHoursAr = null, string? IbanLast4 = null);

public record MerchantReportsDto(
    decimal TotalSales, int OrderCount, decimal AvgOrderValue, double Rating,
    int PrepMinutes, IReadOnlyList<EarningsDayDto> Days,
    IReadOnlyList<MerchantTopItemDto> TopItems);

public record MerchantTopItemDto(string NameAr, int Count, decimal Revenue);

public record CourierSummaryDto(
    decimal TodayEarnings, int TodayTrips, double TodayDistanceKm,
    decimal DeliveryPart, decimal TipsPart, decimal BonusPart,
    IReadOnlyList<EarningsDayDto> Week,
    IReadOnlyList<CourierTripDto> RecentTrips);

public record CourierProfileDto(
    int Id, string Name, double Rating, int RatingCount, int Trips,
    string VehicleAr, string PlateAr, string PhotoUrl,
    bool IsOnline, int AcceptanceRate, int CompletionRate,
    double Latitude, double Longitude,
    decimal TodayEarnings, int TodayTrips, double TodayDistanceKm,
    string OnboardingStatus);

public record EarningsDayDto(string WeekdayAr, decimal Amount, int Trips);

public record CourierTripDto(string ShortCode, string WhenAr, decimal Amount, double Rating);
