using Kono.Api.Auth;
using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class AuthException : Exception
{
    public int StatusCode { get; }
    public AuthException(string message, int statusCode = 400) : base(message) => StatusCode = statusCode;
}

public class AuthService
{
    private readonly KonoDbContext _db;
    private readonly ITokenService _tokens;

    public AuthService(KonoDbContext db, ITokenService tokens)
    {
        _db = db;
        _tokens = tokens;
    }

    public async Task<AuthResponse> RegisterAsync(RegisterRequest req)
    {
        var phone = Normalize(req.Phone);
        if (await _db.Users.AnyAsync(u => u.Phone == phone))
            throw new AuthException("رقم الجوال مسجّل مسبقاً.", 409);
        if (!string.IsNullOrWhiteSpace(req.Email) &&
            await _db.Users.AnyAsync(u => u.Email == req.Email))
            throw new AuthException("البريد الإلكتروني مستخدم.", 409);

        var user = new User
        {
            Phone = phone,
            NameAr = req.Name.Trim(),
            Email = string.IsNullOrWhiteSpace(req.Email) ? null : req.Email.Trim(),
            Role = req.Role,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(req.Password),
        };
        _db.Users.Add(user);

        if (req.Role == UserRole.Courier)
            _db.Couriers.Add(new Courier { User = user, Name = user.NameAr, PlateAr = "—" });

        await _db.SaveChangesAsync();
        return await IssueAsync(user);
    }

    /// <summary>Passwordless sign-in (demo stands in for OTP): signs in an existing
    /// user by phone, or registers a new account (customer / merchant / courier)
    /// on the spot. A new merchant also gets a starter store + menu; a new courier
    /// gets a courier profile.</summary>
    public async Task<AuthResponse> PhoneLoginAsync(string phone, string? name, string? role = null)
    {
        var norm = Normalize(phone);
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Phone == norm);
        if (user is null)
        {
            var wanted = ParseRole(role);
            var displayName = string.IsNullOrWhiteSpace(name)
                ? wanted switch
                {
                    UserRole.Merchant => "متجر كونو",
                    UserRole.Courier => "مندوب كونو",
                    _ => "ضيف كونو",
                }
                : name.Trim();

            user = new User
            {
                Phone = norm,
                NameAr = displayName,
                Role = wanted,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(Guid.NewGuid().ToString()),
            };
            _db.Users.Add(user);

            if (wanted == UserRole.Courier)
            {
                _db.Couriers.Add(new Courier
                {
                    User = user, Name = displayName, PlateAr = "غير مسجّلة",
                    VehicleAr = "دباب", Rating = 5.0, AcceptanceRate = 100,
                    Latitude = 24.774, Longitude = 46.738,
                });
            }
            else if (wanted == UserRole.Merchant)
            {
                var store = new Store
                {
                    Name = displayName,
                    Kind = VerticalKind.Restaurants,
                    CuisineAr = "مطعم · جديد",
                    Rating = 5.0, RatingCount = 0, EtaMinutes = 25,
                    DeliveryFee = 0, MinOrder = 20, DistanceKm = 2.0,
                    PrepMinutes = 15, PriceLevel = 2,
                    HeroImageUrl = "/img/store_burger.jpg", LogoImageUrl = "",
                    Latitude = 24.7136, Longitude = 46.6753,
                    AddressAr = "الرياض", CityAr = "الرياض",
                    Owner = user,
                };
                DbSeeder.BuildMenu(store);
                _db.Stores.Add(store);
            }

            await _db.SaveChangesAsync();
        }
        else if (!string.IsNullOrWhiteSpace(name) &&
                 (string.IsNullOrWhiteSpace(user.NameAr) || user.NameAr is "ضيف كونو" or "متجر كونو" or "مندوب كونو"))
        {
            user.NameAr = name.Trim();
            await _db.SaveChangesAsync();
        }

        if (!user.IsActive) throw new AuthException("الحساب موقوف.", 403);
        return await IssueAsync(user);
    }

    private static UserRole ParseRole(string? role) => role?.Trim().ToLowerInvariant() switch
    {
        "merchant" or "store" => UserRole.Merchant,
        "courier" or "driver" => UserRole.Courier,
        _ => UserRole.Customer,
    };

    /// <summary>Demo: issue tokens for the merchant that owns <paramref name="storeId"/>,
    /// so "order from store → switch to merchant" lands on the right store.</summary>
    public async Task<AuthResponse> DemoStoreMerchantAsync(int storeId)
    {
        var ownerId = await _db.Stores.Where(s => s.Id == storeId)
            .Select(s => s.OwnerUserId).FirstOrDefaultAsync();
        if (ownerId is null)
            throw new AuthException("لا يوجد متجر بهذا المعرّف أو لم يُسنَد لمالك.", 404);
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == ownerId)
            ?? throw new AuthException("حساب المتجر غير موجود.", 404);
        return await IssueAsync(user);
    }

    public async Task<AuthResponse> LoginAsync(LoginRequest req)
    {
        var phone = Normalize(req.Phone);
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Phone == phone);
        if (user is null || !BCrypt.Net.BCrypt.Verify(req.Password, user.PasswordHash))
            throw new AuthException("رقم الجوال أو كلمة المرور غير صحيحة.", 401);
        if (!user.IsActive)
            throw new AuthException("الحساب موقوف.", 403);
        return await IssueAsync(user);
    }

    public async Task<AuthResponse> RefreshAsync(string refreshToken)
    {
        var token = await _db.RefreshTokens
            .Include(t => t.User)
            .FirstOrDefaultAsync(t => t.Token == refreshToken);
        if (token is null || !token.IsActive)
            throw new AuthException("جلسة منتهية، سجّل الدخول من جديد.", 401);

        token.RevokedAt = DateTime.UtcNow;         // rotate
        await _db.SaveChangesAsync();
        return await IssueAsync(token.User);
    }

    public async Task LogoutAsync(string refreshToken)
    {
        var token = await _db.RefreshTokens.FirstOrDefaultAsync(t => t.Token == refreshToken);
        if (token is { RevokedAt: null })
        {
            token.RevokedAt = DateTime.UtcNow;
            await _db.SaveChangesAsync();
        }
    }

    public async Task<User?> GetAsync(int id) =>
        await _db.Users.FirstOrDefaultAsync(u => u.Id == id);

    private async Task<AuthResponse> IssueAsync(User user)
    {
        var (access, accessExp) = _tokens.CreateAccessToken(user);
        var (refresh, refreshExp) = _tokens.CreateRefreshToken();

        _db.RefreshTokens.Add(new RefreshToken
        {
            UserId = user.Id,
            Token = refresh,
            ExpiresAt = refreshExp
        });
        await _db.SaveChangesAsync();

        return new AuthResponse(access, accessExp, refresh, refreshExp, UserSummary.From(user));
    }

    private static string Normalize(string phone)
    {
        var digits = new string(phone.Where(char.IsDigit).ToArray());
        if (digits.StartsWith("00966")) digits = digits[5..];
        if (digits.StartsWith("966")) digits = digits[3..];
        if (digits.StartsWith('0')) digits = digits[1..];
        return "+966" + digits;
    }
}
