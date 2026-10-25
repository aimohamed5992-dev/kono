import Foundation

/// One required/optional document in an onboarding checklist. `provided` flips
/// once the applicant attaches something (the prototype records name + type only).
struct OnboardingDoc: Codable, Identifiable, Equatable {
    var id: String { type }
    let type: String
    let labelAr: String
    let required: Bool
    var provided: Bool = false
    var fileName: String = ""
}

// MARK: - Courier (مندوب توصيل) — KSA delivery-rider application

struct CourierOnboarding: Encodable {
    // 1. personal
    var fullNameAr = ""
    var nationalId = ""
    var dateOfBirth = ""            // yyyy-MM-dd
    var nationality = "سعودي"
    var gender = "ذكر"
    var email = ""
    var cityAr = "الرياض"
    var nationalAddress = ""
    // 2. vehicle
    var vehicleTypeAr = "دباب"
    var vehicleMake = ""
    var vehicleModel = ""
    var vehicleYear = ""
    var vehicleColorAr = ""
    var plateAr = ""
    var plateType = "دراجة نارية"
    // 3. permits
    var workPermitNumber = ""
    var licenseNumber = ""
    var licenseExpiry = ""
    // 4. bank
    var iban = "SA"
    var bankNameAr = ""
    var accountHolderAr = ""
    // 5. location + docs + consent
    var latitude: Double? = nil
    var longitude: Double? = nil
    var documents: [OnboardingDoc] = OnboardingChecklists.courier
    var agreeTerms = false

    static let vehicleTypes = ["دباب", "سيارة", "دراجة هوائية", "سيراً على الأقدام"]
    static let plateTypes = ["دراجة نارية", "خصوصي", "نقل عام"]
    static let nationalities = ["سعودي", "مصري", "سوري", "يمني", "باكستاني", "هندي", "بنغلاديشي", "أخرى"]
    static let banks = ["الأهلي", "الراجحي", "الرياض", "البلاد", "الإنماء", "سامبا", "الجزيرة", "ساب", "الفرنسي", "العربي الوطني"]

    var personalDone: Bool {
        !fullNameAr.isBlank && nationalId.filter(\.isNumber).count == 10
            && !dateOfBirth.isBlank && !nationality.isBlank && !cityAr.isBlank
    }
    var vehicleDone: Bool {
        vehicleTypeAr == "سيراً على الأقدام"
            || (!vehicleMake.isBlank && !vehicleModel.isBlank && !plateAr.isBlank)
    }
    var docsDone: Bool { documents.filter(\.required).allSatisfy(\.provided) }
    var bankDone: Bool {
        iban.filter(\.isNumber).count >= 20 && !bankNameAr.isBlank && !accountHolderAr.isBlank
    }
    var canSubmit: Bool { personalDone && vehicleDone && docsDone && bankDone && agreeTerms }
}

// MARK: - Merchant (متجر / مطعم) — KSA store application

struct MerchantOnboarding: Encodable {
    // 1. business
    var tradeNameAr = ""
    var activityKind = "Restaurants"
    var cuisineAr = ""
    var crNumber = ""
    var crExpiry = ""
    var vatNumber = ""
    var descriptionAr = ""
    // 2. branch + location
    var cityAr = "الرياض"
    var districtAr = ""
    var streetAr = ""
    var latitude: Double? = nil
    var longitude: Double? = nil
    var nationalAddress = ""
    var openingHoursAr = "١٠:٠٠ ص – ١:٠٠ ص"
    var branchPhone = ""
    // 3. bank
    var iban = "SA"
    var bankNameAr = ""
    var accountHolderAr = ""
    // 4. contact
    var contactNameAr = ""
    var contactPhone = ""
    var contactEmail = ""
    // 5. branding + ops
    var logoDataUri: String? = nil
    var prepMinutes = 20
    var minOrder: Double = 0
    var deliveryBy = "كونو"
    // 6. docs + consent
    var documents: [OnboardingDoc] = OnboardingChecklists.merchant
    var agreeTerms = false

    static let activities: [(key: String, ar: String)] = [
        ("Restaurants", "مطعم"), ("Restaurants", "كافيه"), ("Sweets", "حلويات ومخبوزات"),
        ("Groceries", "بقالة / سوبرماركت"), ("Pharmacy", "صيدلية"),
        ("Flowers", "ورود وهدايا"), ("Water", "مياه"),
    ]
    static let deliveryOptions = ["كونو", "المتجر"]
    static let banks = CourierOnboarding.banks

    var businessDone: Bool {
        !tradeNameAr.isBlank && crNumber.filter(\.isNumber).count == 10
            && !crExpiry.isBlank && !activityKind.isBlank
    }
    var locationDone: Bool {
        !cityAr.isBlank && !districtAr.isBlank && !streetAr.isBlank
            && latitude != nil && !branchPhone.isBlank && !openingHoursAr.isBlank
    }
    var bankDone: Bool {
        iban.filter(\.isNumber).count >= 20 && !bankNameAr.isBlank && !accountHolderAr.isBlank
    }
    var contactDone: Bool { !contactNameAr.isBlank && !contactPhone.isBlank }
    var docsDone: Bool {
        documents.filter { $0.required && !($0.type == "sfda_license" && activityKind != "Pharmacy") }
            .allSatisfy(\.provided)
    }
    var canSubmit: Bool { businessDone && locationDone && bankDone && contactDone && docsDone && agreeTerms }
}

// MARK: - Document checklists (mirror of the server's `OnboardingDocs`)

enum OnboardingChecklists {
    static let courier: [OnboardingDoc] = [
        .init(type: "national_id",       labelAr: "صورة الهوية / الإقامة (الوجهان)", required: true),
        .init(type: "driving_license",   labelAr: "رخصة القيادة سارية المفعول", required: true),
        .init(type: "delivery_permit",   labelAr: "تصريح توصيل الطلبات (الهيئة العامة للنقل)", required: true),
        .init(type: "vehicle_reg",       labelAr: "استمارة المركبة (رخصة السير)", required: true),
        .init(type: "vehicle_insurance", labelAr: "وثيقة تأمين المركبة", required: true),
        .init(type: "personal_photo",    labelAr: "صورة شخصية حديثة وواضحة", required: true),
        .init(type: "criminal_record",   labelAr: "شهادة عدم محكومية / السجل الجنائي", required: false),
        .init(type: "iban_certificate",  labelAr: "شهادة الآيبان", required: true),
    ]
    static let merchant: [OnboardingDoc] = [
        .init(type: "commercial_reg",    labelAr: "السجل التجاري", required: true),
        .init(type: "municipal_license", labelAr: "الرخصة البلدية (رخصة المحل)", required: true),
        .init(type: "civil_defense",     labelAr: "شهادة السلامة — الدفاع المدني", required: true),
        .init(type: "health_certs",      labelAr: "الشهادات الصحية للعاملين", required: true),
        .init(type: "sfda_license",      labelAr: "ترخيص هيئة الغذاء والدواء (للصيدليات)", required: true),
        .init(type: "lease_contract",    labelAr: "عقد الإيجار / صك الموقع", required: false),
        .init(type: "owner_id",          labelAr: "هوية المالك / المدير المفوّض", required: true),
        .init(type: "authorization",     labelAr: "تفويض بالتسجيل (إن لزم)", required: false),
        .init(type: "iban_certificate",  labelAr: "شهادة الآيبان", required: true),
        .init(type: "store_logo",        labelAr: "شعار المتجر (عالي الدقة)", required: true),
    ]
}

extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}
