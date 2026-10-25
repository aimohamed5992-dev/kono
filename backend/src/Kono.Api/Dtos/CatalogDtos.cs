using Kono.Api.Domain;

namespace Kono.Api.Dtos;

public record VerticalDto(int Id, string Kind, string TitleAr, string SubtitleAr,
    string Icon, string ImageUrl, int SortOrder);

public record PromotionDto(int Id, string TitleAr, string SubtitleAr, string ImageUrl,
    string BackgroundHex, int SortOrder);

public record StoreListItemDto(
    int Id, string Name, string Kind, string CuisineAr,
    double Rating, int RatingCount, int EtaMinutes,
    decimal DeliveryFee, decimal MinOrder, double DistanceKm,
    bool IsBusy, bool IsClosed, string? PromoTagAr,
    string HeroImageUrl, string LogoImageUrl, int PriceLevel,
    IReadOnlyList<string> Tags);

public record StoreDetailDto(
    int Id, string Name, string Kind, string CuisineAr,
    double Rating, int RatingCount, int EtaMinutes,
    decimal DeliveryFee, decimal MinOrder, double DistanceKm,
    bool IsBusy, bool IsClosed, string? PromoTagAr,
    string HeroImageUrl, string LogoImageUrl, int PriceLevel, int PrepMinutes,
    double Latitude, double Longitude, string AddressAr, string CityAr,
    IReadOnlyList<string> Tags,
    IReadOnlyList<MenuCategoryDto> Menu);

public record MenuCategoryDto(int Id, string NameAr, int SortOrder, IReadOnlyList<ProductDto> Items);

public record ProductDto(
    int Id, int StoreId, int MenuCategoryId, string NameAr, string DescAr,
    decimal Price, decimal? OldPrice, string ImageUrl, bool IsPopular,
    int? Calories, string? UnitAr, bool InStock,
    IReadOnlyList<ProductOptionGroupDto> OptionGroups)
{
    public bool HasDiscount => OldPrice is > 0 && OldPrice > Price;
}

public record ProductOptionGroupDto(int Id, string TitleAr, bool Required, bool MultiSelect,
    IReadOnlyList<ProductOptionDto> Choices);

public record ProductOptionDto(int Id, string NameAr, decimal PriceDelta);

public record ItemHitDto(int ProductId, int StoreId, string StoreName, string NameAr,
    decimal Price, string ImageUrl);

public record SearchResultsDto(IReadOnlyList<StoreListItemDto> Stores, IReadOnlyList<ItemHitDto> Items);

public record HomeFeedDto(
    IReadOnlyList<VerticalDto> Verticals,
    IReadOnlyList<PromotionDto> Promotions,
    IReadOnlyList<HomeRailDto> Rails);

public record HomeRailDto(string Key, string TitleAr, string? SubtitleAr,
    string Style, IReadOnlyList<StoreListItemDto> Stores);

// ── mapping helpers ─────────────────────────────────────────────────────────
public static class CatalogMap
{
    public static VerticalDto ToDto(this Vertical v, Func<string, string> img) =>
        new(v.Id, v.Kind.ToString(), v.TitleAr, v.SubtitleAr, v.Icon, img(v.ImageUrl), v.SortOrder);

    public static PromotionDto ToDto(this Promotion p, Func<string, string> img) =>
        new(p.Id, p.TitleAr, p.SubtitleAr, img(p.ImageUrl), p.BackgroundHex, p.SortOrder);

    public static StoreListItemDto ToListItem(this Store s, Func<string, string> img) =>
        new(s.Id, s.Name, s.Kind.ToString(), s.CuisineAr, s.Rating, s.RatingCount, s.EtaMinutes,
            s.DeliveryFee, s.MinOrder, s.DistanceKm, s.IsBusy, s.IsClosed, s.PromoTagAr,
            img(s.HeroImageUrl), img(s.LogoImageUrl), s.PriceLevel,
            s.Tags.Select(t => t.Text).ToList());

    public static StoreDetailDto ToDetail(this Store s, Func<string, string> img) =>
        new(s.Id, s.Name, s.Kind.ToString(), s.CuisineAr, s.Rating, s.RatingCount, s.EtaMinutes,
            s.DeliveryFee, s.MinOrder, s.DistanceKm, s.IsBusy, s.IsClosed, s.PromoTagAr,
            img(s.HeroImageUrl), img(s.LogoImageUrl), s.PriceLevel, s.PrepMinutes,
            s.Latitude, s.Longitude, s.AddressAr, s.CityAr,
            s.Tags.Select(t => t.Text).ToList(),
            s.Categories.OrderBy(c => c.SortOrder).Select(c => c.ToDto(img)).ToList());

    public static MenuCategoryDto ToDto(this MenuCategory c, Func<string, string> img) =>
        new(c.Id, c.NameAr, c.SortOrder,
            c.Products.OrderBy(p => p.SortOrder).Select(p => p.ToDto(img)).ToList());

    public static ProductDto ToDto(this Product p, Func<string, string> img) =>
        new(p.Id, p.StoreId, p.MenuCategoryId, p.NameAr, p.DescAr, p.Price, p.OldPrice,
            img(p.ImageUrl), p.IsPopular, p.Calories, p.UnitAr, p.InStock,
            p.OptionGroups.OrderBy(g => g.SortOrder).Select(g => new ProductOptionGroupDto(
                g.Id, g.TitleAr, g.Required, g.MultiSelect,
                g.Choices.Select(ch => new ProductOptionDto(ch.Id, ch.NameAr, ch.PriceDelta)).ToList()))
                .ToList());
}
