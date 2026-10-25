import Foundation

struct CourierJob: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var shortCode: String
    var storeNameAr: String
    var storeAddressAr: String
    var dropoffAddressAr: String
    var customerNameAr: String
    var distanceKm: Double
    var estMinutes: Int
    var payout: Double
    var itemCount: Int
    var isBatched: Bool
    var stage: JobStage
    var storeLat: Double
    var storeLng: Double
    var dropLat: Double
    var dropLng: Double
    var customerPhone: String = ""
}

enum JobStage: Int, CaseIterable {
    case offered
    case headingToStore
    case atStore
    case pickedUp
    case headingToCustomer
    case arrived
    case delivered

    var titleAr: String {
        switch self {
        case .offered:            return "عرض رحلة جديدة"
        case .headingToStore:     return "متجه إلى المتجر"
        case .atStore:            return "وصلت المتجر"
        case .pickedUp:           return "استلمت الطلب"
        case .headingToCustomer:  return "متجه إلى العميل"
        case .arrived:            return "وصلت موقع العميل"
        case .delivered:          return "تم التوصيل"
        }
    }

    var ctaAr: String {
        switch self {
        case .offered:           return "قبول الرحلة"
        case .headingToStore:    return "وصلت المتجر"
        case .atStore:           return "استلمت الطلب"
        case .pickedUp:          return "بدء التوصيل"
        case .headingToCustomer: return "وصلت للعميل"
        case .arrived:           return "تأكيد التسليم"
        case .delivered:         return "إنهاء"
        }
    }

    var next: JobStage { JobStage(rawValue: rawValue + 1) ?? .delivered }
}

struct EarningsDay: Identifiable, Hashable {
    let id = UUID()
    var weekdayAr: String
    var amount: Double
    var trips: Int
}
