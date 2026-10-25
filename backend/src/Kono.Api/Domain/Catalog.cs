namespace Kono.Api.Domain;

public abstract class BaseEntity
{
    public int Id { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

/// <summary>A service line (مطاعم، بقالة، صيدليات ...). Editable metadata for the
/// home-screen category strip.</summary>
public class Vertical : BaseEntity
{
    public VerticalKind Kind { get; set; }
    public string TitleAr { get; set; } = "";
    public string SubtitleAr { get; set; } = "";
    public string Icon { get; set; } = "";          // SF Symbol name
    public string ImageUrl { get; set; } = "";       // category tile photo
    public int SortOrder { get; set; }
    public bool IsActive { get; set; } = true;
}

public class Store : BaseEntity
{
    public string Name { get; set; } = "";
    public VerticalKind Kind { get; set; }
    public string CuisineAr { get; set; } = "";
    public double Rating { get; set; }
    public int RatingCount { get; set; }
    public int EtaMinutes { get; set; }
    public decimal DeliveryFee { get; set; }
    public decimal MinOrder { get; set; }
    public double DistanceKm { get; set; }
    public bool IsBusy { get; set; }
    public bool IsClosed { get; set; }
    public string? PromoTagAr { get; set; }
    public string HeroImageUrl { get; set; } = "";
    public string LogoImageUrl { get; set; } = "";
    public int PriceLevel { get; set; } = 2;         // 1..3
    public int PrepMinutes { get; set; } = 15;
    public bool IsActive { get; set; } = true;

    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string AddressAr { get; set; } = "";
    public string CityAr { get; set; } = "الرياض";

    public string OpeningHoursAr { get; set; } = "٧:٠٠ ص – ٢:٠٠ ص";
    public string IbanLast4 { get; set; } = "";
    public double DeliveryRadiusKm { get; set; } = 8;

    public int? OwnerUserId { get; set; }
    public User? Owner { get; set; }

    public List<StoreTag> Tags { get; set; } = new();
    public List<MenuCategory> Categories { get; set; } = new();
    public List<Product> Products { get; set; } = new();
}

public class StoreTag : BaseEntity
{
    public int StoreId { get; set; }
    public Store Store { get; set; } = null!;
    public string Text { get; set; } = "";
}

public class MenuCategory : BaseEntity
{
    public int StoreId { get; set; }
    public Store Store { get; set; } = null!;
    public string NameAr { get; set; } = "";
    public int SortOrder { get; set; }
    public List<Product> Products { get; set; } = new();
}

public class Product : BaseEntity
{
    public int StoreId { get; set; }
    public Store Store { get; set; } = null!;
    public int MenuCategoryId { get; set; }
    public MenuCategory MenuCategory { get; set; } = null!;

    public string NameAr { get; set; } = "";
    public string DescAr { get; set; } = "";
    public decimal Price { get; set; }
    public decimal? OldPrice { get; set; }
    public string ImageUrl { get; set; } = "";
    public bool IsPopular { get; set; }
    public int? Calories { get; set; }
    public string? UnitAr { get; set; }
    public bool InStock { get; set; } = true;
    public int SortOrder { get; set; }
    public int SoldCount { get; set; }

    public List<ProductOptionGroup> OptionGroups { get; set; } = new();
}

public class ProductOptionGroup : BaseEntity
{
    public int ProductId { get; set; }
    public Product Product { get; set; } = null!;
    public string TitleAr { get; set; } = "";
    public bool Required { get; set; }
    public bool MultiSelect { get; set; }
    public int SortOrder { get; set; }
    public List<ProductOption> Choices { get; set; } = new();
}

public class ProductOption : BaseEntity
{
    public int ProductOptionGroupId { get; set; }
    public ProductOptionGroup Group { get; set; } = null!;
    public string NameAr { get; set; } = "";
    public decimal PriceDelta { get; set; }
}

/// <summary>Home-screen promo carousel slide.</summary>
public class Promotion : BaseEntity
{
    public string TitleAr { get; set; } = "";
    public string SubtitleAr { get; set; } = "";
    public string ImageUrl { get; set; } = "";
    public string BackgroundHex { get; set; } = "#FFE79A";
    public int SortOrder { get; set; }
    public bool IsActive { get; set; } = true;
}
