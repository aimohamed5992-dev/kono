import SwiftUI

/// Kono ships as three experiences from one design system.
enum AppRole: String, CaseIterable, Identifiable {
    case customer
    case merchant
    case courier

    var id: String { rawValue }

    /// API role string, e.g. "Customer".
    var apiName: String { rawValue.prefix(1).uppercased() + rawValue.dropFirst() }

    /// Maps the API's role string ("Customer"/"Merchant"/"Courier"/"Admin") to an experience.
    init?(apiRole: String?) {
        switch apiRole?.lowercased() {
        case "customer": self = .customer
        case "merchant": self = .merchant
        case "courier":  self = .courier
        default: return nil
        }
    }

    var titleAr: String {
        switch self {
        case .customer: return "العميل"
        case .merchant: return "المتجر / المطعم"
        case .courier:  return "المندوب"
        }
    }

    var taglineAr: String {
        switch self {
        case .customer: return "اطلب أكل ومقاضي ودواء بسرعة"
        case .merchant: return "استقبل الطلبات وادِر متجرك"
        case .courier:  return "استلم رحلات ووصّل واكسب"
        }
    }

    var icon: String {
        switch self {
        case .customer: return "bag.fill"
        case .merchant: return "storefront.fill"
        case .courier:  return "bicycle"
        }
    }

    var accent: Color {
        switch self {
        case .customer: return KonoColor.brown
        case .merchant: return KonoColor.caramel
        case .courier:  return KonoColor.info
        }
    }
}

struct CustomerProfile {
    var nameAr: String
    var phone: String
    var email: String
    var konoPlus: Bool
    var walletBalance: Double
    var points: Int
    var addresses: [Address]
    var payments: [PaymentMethod]
}

struct MerchantProfile {
    var apiId: Int = 0
    var storeName: String
    var kind: VerticalKind
    var cuisineAr: String = ""
    var isOnline: Bool
    var isBusy: Bool = false
    var rating: Double
    var ratingCount: Int = 0
    var todayOrders: Int
    var todayRevenue: Double
    var prepMinutes: Int
    var heroAsset: String = "store_burger"
    var addressAr: String = "الرياض"
    var openingHoursAr: String = "٧:٠٠ ص – ٢:٠٠ ص"
    var ibanLast4: String = ""
    var deliveryRadiusKm: Double = 8
    var menuItemCount: Int = 0
    var ownerNameAr: String = ""
    var ownerPhone: String = ""
    var onboardingStatus: String = ""
    var logoAsset: String = ""
}

struct CourierProfile {
    var name: String
    var rating: Double
    var ratingCount: Int = 0
    var isOnline: Bool
    var vehicleAr: String
    var plateAr: String = "—"
    var todayTrips: Int
    var todayEarnings: Double
    var todayDistanceKm: Double
    var acceptanceRate: Int
    var completionRate: Int = 98
    var photoAsset: String
    var latitude: Double = 24.774
    var longitude: Double = 46.738
    var onboardingStatus: String = ""
}
