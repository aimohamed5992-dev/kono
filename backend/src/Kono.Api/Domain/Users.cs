namespace Kono.Api.Domain;

public class User : BaseEntity
{
    public string Phone { get; set; } = "";
    public string? Email { get; set; }
    public string NameAr { get; set; } = "";
    public string PasswordHash { get; set; } = "";
    public UserRole Role { get; set; } = UserRole.Customer;
    public bool IsActive { get; set; } = true;

    // customer extras
    public bool KonoPlus { get; set; }
    public decimal WalletBalance { get; set; }
    public int Points { get; set; }

    // merchant / courier onboarding — the full application form + review state
    public string OnboardingJson { get; set; } = "";
    public string OnboardingStatus { get; set; } = "";   // "" | under_review | approved | rejected

    public List<Address> Addresses { get; set; } = new();
    public List<PaymentMethod> PaymentMethods { get; set; } = new();
    public List<RefreshToken> RefreshTokens { get; set; } = new();
    public Courier? CourierProfile { get; set; }
}

public class RefreshToken : BaseEntity
{
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public string Token { get; set; } = "";
    public DateTime ExpiresAt { get; set; }
    public DateTime? RevokedAt { get; set; }
    public bool IsActive => RevokedAt is null && DateTime.UtcNow < ExpiresAt;
}

public class Address : BaseEntity
{
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public string LabelAr { get; set; } = "";
    public string LineAr { get; set; } = "";
    public string CityAr { get; set; } = "الرياض";
    public string Icon { get; set; } = "house.fill";
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public bool IsDefault { get; set; }
}

public class PaymentMethod : BaseEntity
{
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public string LabelAr { get; set; } = "";
    public string Icon { get; set; } = "creditcard.fill";
    public string? DetailAr { get; set; }
    public bool IsDefault { get; set; }
}

public class Courier : BaseEntity
{
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public string Name { get; set; } = "";
    public double Rating { get; set; } = 5.0;
    public int Trips { get; set; }
    public string VehicleAr { get; set; } = "دباب";
    public string PlateAr { get; set; } = "";
    public string PhotoUrl { get; set; } = "";
    public bool IsOnline { get; set; }
    public int AcceptanceRate { get; set; } = 100;
    public double Latitude { get; set; }
    public double Longitude { get; set; }

    // rolling daily figures (reset by a job in real life)
    public int TodayTrips { get; set; }
    public decimal TodayEarnings { get; set; }
    public double TodayDistanceKm { get; set; }
}
