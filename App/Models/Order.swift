import Foundation

struct CartLine: Identifiable, Hashable {
    let id = UUID()
    var product: Product
    var quantity: Int
    var selectedOptionsAr: [String] = []
    var selectedOptionIds: [Int] = []
    var optionsDelta: Double = 0
    var note: String = ""

    var unitPrice: Double { product.price + optionsDelta }
    var lineTotal: Double { unitPrice * Double(quantity) }
}

enum OrderStage: Int, CaseIterable, Codable {
    case placed
    case confirmed
    case preparing
    case readyForPickup
    case onTheWay
    case delivered

    /// Map the API's `displayStage` string onto the app's 6-step ladder.
    init(display: String) {
        switch display {
        case "confirmed": self = .confirmed
        case "preparing": self = .preparing
        case "readyForPickup": self = .readyForPickup
        case "onTheWay": self = .onTheWay
        case "delivered": self = .delivered
        default: self = .placed
        }
    }

    var titleAr: String {
        switch self {
        case .placed:         return "تم استلام الطلب"
        case .confirmed:      return "أكد المطعم الطلب"
        case .preparing:      return "يتم تجهيز طلبك"
        case .readyForPickup: return "الطلب جاهز للاستلام"
        case .onTheWay:       return "المندوب في الطريق إليك"
        case .delivered:      return "تم التوصيل"
        }
    }

    var icon: String {
        switch self {
        case .placed:         return "checkmark.circle.fill"
        case .confirmed:      return "checkmark.seal.fill"
        case .preparing:      return "flame.fill"
        case .readyForPickup: return "bag.fill"
        case .onTheWay:       return "figure.outdoor.cycle"
        case .delivered:      return "house.fill"
        }
    }
}

struct Order: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var shortCode: String            // "KON-4821"
    var store: Store
    var lines: [CartLine]
    var stage: OrderStage
    var placedAt: Date
    var address: Address
    var courier: Courier?
    var paymentLabelAr: String       // "Apple Pay" / "مدى ****4421" / "نقداً"
    var scheduledForAr: String?      // nil = ASAP

    // server-authoritative figures (nil = compute locally from the cart)
    var serverSubtotal: Double? = nil
    var serverDeliveryFee: Double? = nil
    var serverServiceFee: Double? = nil
    var serverDiscount: Double? = nil
    var serverTip: Double = 0
    var serverTotal: Double? = nil
    var events: [OrderStageEvent] = []

    var itemCount: Int { lines.reduce(0) { $0 + $1.quantity } }
    var subtotal: Double { serverSubtotal ?? lines.reduce(0) { $0 + $1.lineTotal } }
    var deliveryFee: Double { serverDeliveryFee ?? store.deliveryFee }
    var serviceFee: Double { serverServiceFee ?? 2.00 }
    var discount: Double { serverDiscount ?? (store.promoTagAr != nil ? 10 : 0) }
    var tip: Double { serverTip }
    var total: Double { serverTotal ?? max(0, subtotal + deliveryFee + serviceFee - discount + tip) }
}

struct OrderStageEvent: Hashable {
    var stage: OrderStage
    var at: Date
}

struct Address: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var labelAr: String              // "المنزل" / "العمل"
    var lineAr: String               // "حي الياسمين، شارع الأمير سلطان"
    var cityAr: String
    var icon: String                 // SF Symbol
    var latitude: Double
    var longitude: Double
    var isDefault: Bool = false
}

struct Courier: Identifiable, Hashable {
    let id = UUID()
    var name: String
    var rating: Double
    var trips: Int
    var vehicleAr: String            // "دباب" / "سيارة"
    var plateAr: String
    var photoAsset: String
    var phone: String = "+9665XXXXXXXX"
    var latitude: Double
    var longitude: Double
}

struct PaymentMethod: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var labelAr: String
    var icon: String
    var detailAr: String?
    var isDefault: Bool = false
}
