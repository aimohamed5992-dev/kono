using System.ComponentModel.DataAnnotations;
using Kono.Api.Domain;

namespace Kono.Api.Dtos;

public record AddressDto(int Id, string LabelAr, string LineAr, string CityAr,
    string Icon, double Latitude, double Longitude, bool IsDefault)
{
    public static AddressDto From(Address a) =>
        new(a.Id, a.LabelAr, a.LineAr, a.CityAr, a.Icon, a.Latitude, a.Longitude, a.IsDefault);
}

public record AddressUpsertRequest(
    [Required] string LabelAr,
    [Required] string LineAr,
    string CityAr = "الرياض",
    string Icon = "house.fill",
    double Latitude = 24.7136,
    double Longitude = 46.6753,
    bool MakeDefault = false);

public record PaymentMethodDto(int Id, string LabelAr, string Icon, string? DetailAr, bool IsDefault)
{
    public static PaymentMethodDto From(PaymentMethod p) =>
        new(p.Id, p.LabelAr, p.Icon, p.DetailAr, p.IsDefault);
}

public record PaymentMethodUpsertRequest(
    [Required] string LabelAr, string Icon = "creditcard.fill",
    string? DetailAr = null, bool MakeDefault = false);

public record UpdateProfileRequest(string? Name, string? Email);
