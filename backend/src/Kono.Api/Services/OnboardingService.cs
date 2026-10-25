using System.Text.Json;
using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

/// <summary>Stores the merchant / courier onboarding application and mirrors the
/// key fields onto the live Store / Courier row so the app shows real data while
/// the application is "under review".</summary>
public class OnboardingService
{
    private readonly KonoDbContext _db;
    private static readonly JsonSerializerOptions J = new() { WriteIndented = false };

    public OnboardingService(KonoDbContext db) => _db = db;

    public async Task<OnboardingStateDto> GetAsync(int userId)
    {
        var u = await _db.Users.AsNoTracking().FirstOrDefaultAsync(x => x.Id == userId)
            ?? throw new KeyNotFoundException();
        return new OnboardingStateDto(u.OnboardingStatus, u.OnboardingJson);
    }

    public async Task<OnboardingStateDto> SubmitCourierAsync(int userId, CourierOnboardingRequest req)
    {
        var user = await _db.Users.FirstOrDefaultAsync(x => x.Id == userId) ?? throw new KeyNotFoundException();
        var courier = await _db.Couriers.FirstOrDefaultAsync(c => c.UserId == userId);
        if (courier is null)
        {
            courier = new Courier { UserId = userId };
            _db.Couriers.Add(courier);
        }

        courier.Name = req.FullNameAr.Trim();
        courier.VehicleAr = string.IsNullOrWhiteSpace(req.VehicleTypeAr) ? courier.VehicleAr : req.VehicleTypeAr.Trim();
        if (!string.IsNullOrWhiteSpace(req.PlateAr)) courier.PlateAr = req.PlateAr.Trim();
        if (req.Latitude is double la && req.Longitude is double lo) { courier.Latitude = la; courier.Longitude = lo; }
        if (!string.IsNullOrWhiteSpace(req.FullNameAr)) user.NameAr = req.FullNameAr.Trim();
        if (!string.IsNullOrWhiteSpace(req.Email)) user.Email = req.Email.Trim();

        user.OnboardingJson = JsonSerializer.Serialize(req, J);
        user.OnboardingStatus = "under_review";
        await _db.SaveChangesAsync();
        return new OnboardingStateDto(user.OnboardingStatus, user.OnboardingJson);
    }

    public async Task<OnboardingStateDto> SubmitMerchantAsync(int userId, MerchantOnboardingRequest req)
    {
        var user = await _db.Users.FirstOrDefaultAsync(x => x.Id == userId) ?? throw new KeyNotFoundException();

        var store = await _db.Stores.FirstOrDefaultAsync(s => s.OwnerUserId == userId);
        if (store is null)
        {
            store = new Store { OwnerUserId = userId, HeroImageUrl = "/img/store_burger.jpg" };
            DbSeeder.BuildMenu(store);
            _db.Stores.Add(store);
        }

        store.Name = req.TradeNameAr.Trim();
        if (Enum.TryParse<VerticalKind>(req.ActivityKind, true, out var kind)) store.Kind = kind;
        if (!string.IsNullOrWhiteSpace(req.CuisineAr)) store.CuisineAr = req.CuisineAr!.Trim();
        store.AddressAr = $"{req.DistrictAr.Trim()}، {req.StreetAr.Trim()}";
        store.CityAr = req.CityAr.Trim();
        store.OpeningHoursAr = req.OpeningHoursAr.Trim();
        store.PrepMinutes = Math.Clamp(req.PrepMinutes, 5, 120);
        store.MinOrder = Math.Max(0, req.MinOrder);
        store.IbanLast4 = new string(req.Iban.Where(char.IsDigit).TakeLast(4).ToArray());
        if (req.Latitude is double la && req.Longitude is double lo) { store.Latitude = la; store.Longitude = lo; }
        if (!string.IsNullOrWhiteSpace(req.LogoDataUri)) store.LogoImageUrl = req.LogoDataUri!;
        if (!string.IsNullOrWhiteSpace(req.TradeNameAr)) user.NameAr = req.TradeNameAr.Trim();
        if (!string.IsNullOrWhiteSpace(req.ContactEmail)) user.Email = req.ContactEmail!.Trim();

        user.OnboardingJson = JsonSerializer.Serialize(req, J);
        user.OnboardingStatus = "under_review";
        await _db.SaveChangesAsync();
        return new OnboardingStateDto(user.OnboardingStatus, user.OnboardingJson);
    }
}
