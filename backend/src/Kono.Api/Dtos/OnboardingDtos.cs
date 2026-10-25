using System.ComponentModel.DataAnnotations;

namespace Kono.Api.Dtos;

/// <summary>A document the applicant attached (name/type only — the prototype does
/// not persist file blobs).</summary>
public record OnboardingDocDto(string Type, string FileName, bool Provided);

// ── Courier (مندوب توصيل) — KSA delivery-rider onboarding ────────────────────
public record CourierOnboardingRequest(
    // 1. personal
    [Required] string FullNameAr,
    [Required] string NationalId,
    string? DateOfBirth,
    [Required] string Nationality,
    string? Gender,
    string? Email,
    [Required] string CityAr,
    string? NationalAddress,
    // 2. vehicle
    [Required] string VehicleTypeAr,      // دباب | سيارة | دراجة هوائية | مشياً
    string? VehicleMake,
    string? VehicleModel,
    int? VehicleYear,
    string? VehicleColorAr,
    string? PlateAr,
    string? PlateType,                    // خصوصي | نقل عام | دراجة نارية
    // 3. work permit
    string? WorkPermitNumber,             // تصريح توصيل الطلبات — الهيئة العامة للنقل
    string? LicenseNumber,
    string? LicenseExpiry,
    // 4. bank
    [Required] string Iban,
    [Required] string BankNameAr,
    [Required] string AccountHolderAr,
    // 5. docs + consent
    double? Latitude,
    double? Longitude,
    List<OnboardingDocDto>? Documents,
    bool AgreeTerms = false);

// ── Merchant (متجر / مطعم) — KSA store onboarding ───────────────────────────
public record MerchantOnboardingRequest(
    // 1. business
    [Required] string TradeNameAr,
    [Required] string ActivityKind,       // Restaurants | Groceries | Pharmacy | Sweets | Flowers | Water
    string? CuisineAr,
    [Required] string CrNumber,           // السجل التجاري
    string? CrExpiry,
    string? VatNumber,                    // الرقم الضريبي
    string? DescriptionAr,
    // 2. branch + location
    [Required] string CityAr,
    [Required] string DistrictAr,
    [Required] string StreetAr,
    double? Latitude,
    double? Longitude,
    string? NationalAddress,
    [Required] string OpeningHoursAr,
    [Required] string BranchPhone,
    // 3. bank
    [Required] string Iban,
    [Required] string BankNameAr,
    [Required] string AccountHolderAr,
    // 4. contact
    [Required] string ContactNameAr,
    [Required] string ContactPhone,
    // 5. optional
    string? ContactEmail = null,
    string? LogoDataUri = null,           // base64 data: URI for the logo
    int PrepMinutes = 20,
    decimal MinOrder = 0,
    string? DeliveryBy = null,            // كونو | المتجر
    List<OnboardingDocDto>? Documents = null,
    bool AgreeTerms = false);

public record OnboardingStateDto(string Status, string PayloadJson);

// Required-document checklists the client renders (source of truth = server).
public static class OnboardingDocs
{
    public static readonly (string type, string labelAr, bool required)[] Courier =
    {
        ("national_id",      "صورة الهوية / الإقامة (الوجهان)",              true),
        ("driving_license",  "رخصة القيادة سارية المفعول",                   true),
        ("delivery_permit",  "تصريح توصيل الطلبات (الهيئة العامة للنقل)",     true),
        ("vehicle_reg",      "استمارة المركبة (رخصة السير)",                  true),
        ("vehicle_insurance","وثيقة تأمين المركبة",                          true),
        ("personal_photo",   "صورة شخصية حديثة وواضحة",                       true),
        ("criminal_record",  "شهادة عدم محكومية / السجل الجنائي",            false),
        ("iban_certificate", "شهادة الآيبان",                                true),
    };

    public static readonly (string type, string labelAr, bool required)[] Merchant =
    {
        ("commercial_reg",   "السجل التجاري",                                true),
        ("municipal_license","الرخصة البلدية (رخصة المحل)",                  true),
        ("civil_defense",    "شهادة السلامة — الدفاع المدني",                true),
        ("health_certs",     "الشهادات الصحية للعاملين",                     true),
        ("sfda_license",     "ترخيص هيئة الغذاء والدواء (للصيدليات)",         false),
        ("lease_contract",   "عقد الإيجار / صك الموقع",                       false),
        ("owner_id",         "هوية المالك / المدير المفوّض",                 true),
        ("authorization",    "تفويض بالتسجيل (إن لزم)",                       false),
        ("iban_certificate", "شهادة الآيبان",                                true),
        ("store_logo",       "شعار المتجر (عالي الدقة)",                     true),
    };
}
