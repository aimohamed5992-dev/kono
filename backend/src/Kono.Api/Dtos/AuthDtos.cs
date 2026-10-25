using System.ComponentModel.DataAnnotations;
using Kono.Api.Domain;

namespace Kono.Api.Dtos;

public record RegisterRequest(
    [Required, RegularExpression(@"^\+?[0-9]{9,15}$")] string Phone,
    [Required, MinLength(2)] string Name,
    [Required, MinLength(6)] string Password,
    string? Email = null,
    UserRole Role = UserRole.Customer);

public record LoginRequest(
    [Required] string Phone,
    [Required] string Password);

public record RefreshRequest([Required] string RefreshToken);

public record PhoneLoginRequest([Required] string Phone, string? Name = null, string? Role = null);

/// <summary>Demo helper — sign in as the merchant that owns the given store.</summary>
public record DemoStoreMerchantRequest([Required] int StoreId);

public record AuthResponse(
    string AccessToken,
    DateTime AccessTokenExpiresAt,
    string RefreshToken,
    DateTime RefreshTokenExpiresAt,
    UserSummary User);

public record UserSummary(
    int Id, string Phone, string Name, string? Email, string Role,
    bool KonoPlus, decimal WalletBalance, int Points, string OnboardingStatus)
{
    public static UserSummary From(User u) =>
        new(u.Id, u.Phone, u.NameAr, u.Email, u.Role.ToString(), u.KonoPlus,
            u.WalletBalance, u.Points, u.OnboardingStatus);
}
