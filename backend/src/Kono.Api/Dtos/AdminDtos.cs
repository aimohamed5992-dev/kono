using System.ComponentModel.DataAnnotations;
using Kono.Api.Domain;

namespace Kono.Api.Dtos;

// ── dashboard ───────────────────────────────────────────────────────────────
public record AdminStatsDto(
    decimal RevenueToday, decimal Revenue7d, decimal Revenue30d,
    int OrdersToday, int OrdersActive, int OrdersTotal,
    int StoresActive, int CouriersOnline, int CouriersTotal, int CustomersTotal,
    decimal AvgOrderValue,
    IReadOnlyList<CountByLabel> OrdersByStage,
    IReadOnlyList<RevenuePoint> RevenueByDay,
    IReadOnlyList<VerticalSalesRow> SalesByVertical,
    IReadOnlyList<TopProductRow> TopProducts,
    IReadOnlyList<TopStoreRow> TopStores);

public record CountByLabel(string Label, int Count);
public record RevenuePoint(string Day, decimal Revenue, int Orders);
public record VerticalSalesRow(string Kind, int Orders, decimal Revenue);

// ── live operations map ─────────────────────────────────────────────────────
public record LiveOrderDto(
    int Id, string ShortCode, string Stage, string StageAr, decimal Total, int ItemCount,
    DateTime PlacedAt, string CustomerNameAr, string? CourierNameAr,
    string StoreNameAr, double StoreLat, double StoreLng,
    string DropoffAddressAr, double DropoffLat, double DropoffLng,
    double? CourierLat, double? CourierLng);

public record LiveBoardDto(IReadOnlyList<LiveOrderDto> Orders, double CenterLat, double CenterLng,
    int ActiveCount, int UnassignedCount);
public record TopProductRow(int Id, string NameAr, string StoreName, int Sold, decimal Revenue, string ImageUrl);
public record TopStoreRow(int Id, string Name, string Kind, int Orders, decimal Revenue, double Rating, string ImageUrl);

// ── stores CRUD ─────────────────────────────────────────────────────────────
public record StoreUpsertRequest(
    [Required] string Name, [Required] string Kind, string CuisineAr = "",
    double Rating = 4.5, int RatingCount = 0, int EtaMinutes = 30,
    decimal DeliveryFee = 0, decimal MinOrder = 0, double DistanceKm = 2,
    bool IsBusy = false, bool IsClosed = false, string? PromoTagAr = null,
    string HeroImageUrl = "", string LogoImageUrl = "", int PriceLevel = 2, int PrepMinutes = 15,
    bool IsActive = true, double Latitude = 24.71, double Longitude = 46.67,
    string AddressAr = "", string CityAr = "الرياض", int? OwnerUserId = null,
    List<string>? Tags = null);

public record StoreAdminRow(int Id, string Name, string Kind, string CuisineAr, double Rating,
    int EtaMinutes, decimal DeliveryFee, bool IsActive, bool IsClosed, int ProductCount, string HeroImageUrl);

// ── products CRUD ───────────────────────────────────────────────────────────
public record ProductUpsertRequest(
    [Required] string NameAr, string DescAr = "", [Range(0, 100000)] decimal Price = 0,
    decimal? OldPrice = null, string ImageUrl = "", bool IsPopular = false,
    int? Calories = null, string? UnitAr = null, bool InStock = true,
    int MenuCategoryId = 0, string? NewCategoryNameAr = null, int SortOrder = 0);

public record MenuCategoryUpsertRequest([Required] string NameAr, int SortOrder = 0);

// ── couriers ────────────────────────────────────────────────────────────────
public record CourierAdminRow(int Id, int UserId, string Name, string Phone, double Rating,
    int Trips, string VehicleAr, string PlateAr, bool IsOnline, int AcceptanceRate,
    decimal TodayEarnings, int TodayTrips);

public record CourierUpdateRequest(string Name, string VehicleAr, string PlateAr,
    int AcceptanceRate, bool IsOnline);

public record CourierCreateRequest([Required] string Name, [Required] string Phone,
    string VehicleAr = "دباب", string PlateAr = "", int AcceptanceRate = 100, bool IsOnline = false);

// ── customers ───────────────────────────────────────────────────────────────
public record CustomerAdminRow(int Id, string NameAr, string Phone, string? Email,
    bool KonoPlus, decimal WalletBalance, int Points, bool IsActive, int OrderCount,
    decimal LifetimeSpend, DateTime CreatedAt);

// ── verticals & promotions ──────────────────────────────────────────────────
public record VerticalUpdateRequest(string TitleAr, string SubtitleAr, string Icon,
    string ImageUrl, int SortOrder, bool IsActive);

public record PromotionUpsertRequest([Required] string TitleAr, string SubtitleAr = "",
    string ImageUrl = "", string BackgroundHex = "#FFE79A", int SortOrder = 0, bool IsActive = true);

// ── admin order row ─────────────────────────────────────────────────────────
public record AdminOrderRow(int Id, string ShortCode, string CustomerNameAr, string StoreName,
    string Stage, string? CourierNameAr, decimal Total, int ItemCount, DateTime PlacedAt);
