import Foundation

/// An order as seen from the merchant side of Kono.
struct MerchantTicket: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var shortCode: String
    var customerNameAr: String
    var lines: [CartLine]
    var placedAt: Date
    var state: TicketState
    var isDelivery: Bool
    var courierNameAr: String?
    var noteAr: String?
    var acceptDeadline: Date          // countdown for new tickets

    var itemCount: Int { lines.reduce(0) { $0 + $1.quantity } }
    var total: Double { lines.reduce(0) { $0 + $1.lineTotal } }
}

enum TicketState: String, CaseIterable {
    case incoming
    case preparing
    case ready
    case handedOver
    case completed

    var titleAr: String {
        switch self {
        case .incoming:   return "جديد"
        case .preparing:  return "قيد التجهيز"
        case .ready:      return "جاهز"
        case .handedOver: return "تم التسليم"
        case .completed:  return "مكتمل"
        }
    }
}

struct MenuItemStatus: Identifiable, Hashable {
    let id = UUID()
    var product: Product
    var available: Bool
    var soldToday: Int
}
