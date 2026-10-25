import SwiftUI

/// The service lines Kono offers, HungerStation-style: food plus q-commerce.
enum VerticalKind: String, CaseIterable, Identifiable, Codable {
    case restaurants
    case groceries
    case pharmacy
    case sweets
    case flowers
    case water

    var id: String { rawValue }

    var titleAr: String {
        switch self {
        case .restaurants: return "مطاعم"
        case .groceries:   return "بقالة"
        case .pharmacy:    return "صيدليات"
        case .sweets:      return "حلويات"
        case .flowers:     return "ورود"
        case .water:       return "مياه"
        }
    }

    var subtitleAr: String {
        switch self {
        case .restaurants: return "وجبتك المفضلة توصلك سريع"
        case .groceries:   return "مقاضي البيت من بندة ولولو والتميمي"
        case .pharmacy:    return "دواءك يوصلك خلال دقائق"
        case .sweets:      return "حلا ومناسبات"
        case .flowers:     return "بوكيهات وهدايا"
        case .water:       return "قوارير ومياه معدنية"
        }
    }

    var systemIcon: String {
        switch self {
        case .restaurants: return "fork.knife"
        case .groceries:   return "basket.fill"
        case .pharmacy:    return "cross.case.fill"
        case .sweets:      return "birthday.cake.fill"
        case .flowers:     return "leaf.fill"
        case .water:       return "drop.fill"
        }
    }

    var accent: Color { KonoColor.vertical(self) }

    /// Whether items are browsed as a menu (food) or a product catalog (retail).
    var isCatalog: Bool { self != .restaurants }
}

struct Vertical: Identifiable, Hashable {
    let id = UUID()
    let kind: VerticalKind
    var etaMinutes: Int
    var storeCount: Int
}
