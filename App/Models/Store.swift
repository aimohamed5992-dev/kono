import Foundation

struct Store: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0               // backend id; 0 = local/mock only
    var name: String
    var kind: VerticalKind
    var cuisineAr: String            // e.g. "برجر · أمريكي" or "سوبرماركت"
    var rating: Double
    var ratingCount: Int
    var etaMinutes: Int
    var deliveryFee: Double
    var minOrder: Double
    var distanceKm: Double
    var isFavorite: Bool = false
    var isBusy: Bool = false
    var isClosed: Bool = false
    var promoTagAr: String?          // e.g. "خصم ٢٥٪" / "توصيل مجاني"
    var heroAsset: String            // asset name for hero image
    var logoAsset: String            // asset name for round logo
    var tagsAr: [String] = []
    var priceLevel: Int = 2          // 1...3

    var deliveryFeeText: String {
        deliveryFee == 0 ? "توصيل مجاني" : "\(deliveryFee.clean) ر.س توصيل"
    }
}

struct MenuCategory: Identifiable, Hashable {
    let id = UUID()
    var nameAr: String
    var items: [Product]
}

struct Product: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var nameAr: String
    var descAr: String
    var price: Double
    var oldPrice: Double?
    var imageAsset: String
    var isPopular: Bool = false
    var calories: Int?
    var options: [ProductOptionGroup] = []
    var unitAr: String?              // retail: "١ كجم" / "٦ حبات"
    var inStock: Bool = true

    var hasDiscount: Bool { (oldPrice ?? 0) > price }
}

struct ProductOptionGroup: Identifiable, Hashable {
    let id = UUID()
    var titleAr: String
    var required: Bool
    var multiSelect: Bool
    var choices: [ProductOption]
}

struct ProductOption: Identifiable, Hashable {
    let id = UUID()
    var apiId: Int = 0
    var nameAr: String
    var priceDelta: Double = 0
}

extension Double {
    /// Drops a trailing `.0` — `12.0` -> `12`, `12.5` -> `12.5`.
    var clean: String {
        truncatingRemainder(dividingBy: 1) == 0
            ? String(format: "%.0f", self)
            : String(format: "%.2f", self)
    }
}
