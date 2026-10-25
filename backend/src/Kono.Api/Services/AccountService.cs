using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class AccountService
{
    private readonly KonoDbContext _db;
    public AccountService(KonoDbContext db) => _db = db;

    // ── addresses ───────────────────────────────────────────────────────────
    public async Task<List<AddressDto>> AddressesAsync(int userId) =>
        (await _db.Addresses.Where(a => a.UserId == userId)
            .OrderByDescending(a => a.IsDefault).ThenBy(a => a.Id).ToListAsync())
        .Select(AddressDto.From).ToList();

    public async Task<AddressDto> AddAddressAsync(int userId, AddressUpsertRequest r)
    {
        var first = !await _db.Addresses.AnyAsync(a => a.UserId == userId);
        var a = new Address
        {
            UserId = userId, LabelAr = r.LabelAr.Trim(), LineAr = r.LineAr.Trim(),
            CityAr = r.CityAr, Icon = r.Icon, Latitude = r.Latitude, Longitude = r.Longitude,
            IsDefault = first || r.MakeDefault
        };
        if (a.IsDefault) await ClearDefaultAsync(userId);
        _db.Addresses.Add(a);
        await _db.SaveChangesAsync();
        return AddressDto.From(a);
    }

    public async Task<AddressDto> UpdateAddressAsync(int userId, int id, AddressUpsertRequest r)
    {
        var a = await Owned(userId, id);
        a.LabelAr = r.LabelAr.Trim(); a.LineAr = r.LineAr.Trim(); a.CityAr = r.CityAr;
        a.Icon = r.Icon; a.Latitude = r.Latitude; a.Longitude = r.Longitude;
        if (r.MakeDefault && !a.IsDefault) { await ClearDefaultAsync(userId); a.IsDefault = true; }
        await _db.SaveChangesAsync();
        return AddressDto.From(a);
    }

    public async Task SetDefaultAddressAsync(int userId, int id)
    {
        var a = await Owned(userId, id);
        await ClearDefaultAsync(userId);
        a.IsDefault = true;
        await _db.SaveChangesAsync();
    }

    public async Task DeleteAddressAsync(int userId, int id)
    {
        var a = await Owned(userId, id);
        var wasDefault = a.IsDefault;
        _db.Addresses.Remove(a);
        await _db.SaveChangesAsync();
        if (wasDefault)
        {
            var next = await _db.Addresses.Where(x => x.UserId == userId).OrderBy(x => x.Id).FirstOrDefaultAsync();
            if (next is not null) { next.IsDefault = true; await _db.SaveChangesAsync(); }
        }
    }

    private async Task<Address> Owned(int userId, int id) =>
        await _db.Addresses.FirstOrDefaultAsync(a => a.Id == id && a.UserId == userId)
        ?? throw new KeyNotFoundException("العنوان غير موجود.");

    private async Task ClearDefaultAsync(int userId)
    {
        foreach (var x in await _db.Addresses.Where(a => a.UserId == userId && a.IsDefault).ToListAsync())
            x.IsDefault = false;
    }

    // ── payment methods ─────────────────────────────────────────────────────
    public async Task<List<PaymentMethodDto>> PaymentsAsync(int userId) =>
        (await _db.PaymentMethods.Where(p => p.UserId == userId)
            .OrderByDescending(p => p.IsDefault).ThenBy(p => p.Id).ToListAsync())
        .Select(PaymentMethodDto.From).ToList();

    public async Task<PaymentMethodDto> AddPaymentAsync(int userId, PaymentMethodUpsertRequest r)
    {
        var first = !await _db.PaymentMethods.AnyAsync(p => p.UserId == userId);
        var p = new PaymentMethod
        {
            UserId = userId, LabelAr = r.LabelAr.Trim(), Icon = r.Icon,
            DetailAr = r.DetailAr, IsDefault = first || r.MakeDefault
        };
        if (p.IsDefault)
            foreach (var x in await _db.PaymentMethods.Where(m => m.UserId == userId && m.IsDefault).ToListAsync())
                x.IsDefault = false;
        _db.PaymentMethods.Add(p);
        await _db.SaveChangesAsync();
        return PaymentMethodDto.From(p);
    }

    public async Task DeletePaymentAsync(int userId, int id)
    {
        var p = await _db.PaymentMethods.FirstOrDefaultAsync(x => x.Id == id && x.UserId == userId)
            ?? throw new KeyNotFoundException();
        _db.PaymentMethods.Remove(p);
        await _db.SaveChangesAsync();
    }

    // ── profile ─────────────────────────────────────────────────────────────
    public async Task<UserSummary> UpdateProfileAsync(int userId, UpdateProfileRequest r)
    {
        var u = await _db.Users.FindAsync(userId) ?? throw new KeyNotFoundException();
        if (!string.IsNullOrWhiteSpace(r.Name)) u.NameAr = r.Name.Trim();
        if (r.Email is not null) u.Email = string.IsNullOrWhiteSpace(r.Email) ? null : r.Email.Trim();
        await _db.SaveChangesAsync();
        return UserSummary.From(u);
    }

    public async Task<UserSummary> ToggleKonoPlusAsync(int userId)
    {
        var u = await _db.Users.FindAsync(userId) ?? throw new KeyNotFoundException();
        u.KonoPlus = !u.KonoPlus;
        await _db.SaveChangesAsync();
        return UserSummary.From(u);
    }
}
