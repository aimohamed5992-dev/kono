import Foundation

/// Wire models — decoded straight from the API, then mapped to the app's
/// domain types in `AppData`.
enum AuthDTO {
    struct Response: Decodable {
        let accessToken: String
        let refreshToken: String
        let user: User
    }
    struct User: Decodable {
        let id: Int
        let phone: String
        let name: String
        let email: String?
        let role: String
        let konoPlus: Bool
        let walletBalance: Double
        let points: Int
        var onboardingStatus: String = ""
    }
}

struct OnboardingStateDTO: Decodable {
    let status: String
    let payloadJson: String
}

struct VerticalDTO: Decodable {
    let id: Int
    let kind: String
    let titleAr: String
    let subtitleAr: String
    let icon: String
    let imageUrl: String
    let sortOrder: Int
}

struct PromotionDTO: Decodable {
    let id: Int
    let titleAr: String
    let subtitleAr: String
    let imageUrl: String
    let backgroundHex: String
    let sortOrder: Int
}

struct StoreDTO: Decodable {
    let id: Int
    let name: String
    let kind: String
    let cuisineAr: String
    let rating: Double
    let ratingCount: Int
    let etaMinutes: Int
    let deliveryFee: Double
    let minOrder: Double
    let distanceKm: Double
    let isBusy: Bool
    let isClosed: Bool
    let promoTagAr: String?
    let heroImageUrl: String
    let logoImageUrl: String
    let priceLevel: Int
    let tags: [String]
}

struct StoreDetailDTO: Decodable {
    let id: Int
    let name: String
    let kind: String
    let cuisineAr: String
    let rating: Double
    let ratingCount: Int
    let etaMinutes: Int
    let deliveryFee: Double
    let minOrder: Double
    let distanceKm: Double
    let isBusy: Bool
    let isClosed: Bool
    let promoTagAr: String?
    let heroImageUrl: String
    let logoImageUrl: String
    let priceLevel: Int
    let prepMinutes: Int
    let latitude: Double
    let longitude: Double
    let addressAr: String
    let cityAr: String
    let tags: [String]
    let menu: [MenuCategoryDTO]
}

struct MenuCategoryDTO: Decodable {
    let id: Int
    let nameAr: String
    let sortOrder: Int
    let items: [ProductDTO]
}

struct ProductDTO: Decodable {
    let id: Int
    let storeId: Int
    let menuCategoryId: Int
    let nameAr: String
    let descAr: String
    let price: Double
    let oldPrice: Double?
    let imageUrl: String
    let isPopular: Bool
    let calories: Int?
    let unitAr: String?
    let inStock: Bool
    let optionGroups: [ProductOptionGroupDTO]
}

struct ProductOptionGroupDTO: Decodable {
    let id: Int
    let titleAr: String
    let required: Bool
    let multiSelect: Bool
    let choices: [ProductOptionDTO]
}

struct ProductOptionDTO: Decodable {
    let id: Int
    let nameAr: String
    let priceDelta: Double
}

struct HomeFeedDTO: Decodable {
    let verticals: [VerticalDTO]
    let promotions: [PromotionDTO]
    let rails: [HomeRailDTO]
}

struct HomeRailDTO: Decodable {
    let key: String
    let titleAr: String
    let subtitleAr: String?
    let style: String
    let stores: [StoreDTO]
}

struct SearchResultsDTO: Decodable {
    let stores: [StoreDTO]
    let items: [ItemHitDTO]
}

struct ItemHitDTO: Decodable {
    let productId: Int
    let storeId: Int
    let storeName: String
    let nameAr: String
    let price: Double
    let imageUrl: String
}

struct AddressDTO: Decodable {
    let id: Int
    let labelAr: String
    let lineAr: String
    let cityAr: String
    let icon: String
    let latitude: Double
    let longitude: Double
    let isDefault: Bool
}

struct PaymentMethodDTO: Decodable {
    let id: Int
    let labelAr: String
    let icon: String
    let detailAr: String?
    let isDefault: Bool
}

struct PagedDTO<T: Decodable>: Decodable {
    let items: [T]
    let page: Int
    let pageSize: Int
    let total: Int
    let totalPages: Int
    let hasMore: Bool
}

// ── orders ──────────────────────────────────────────────────────────────────
struct OrderDTO: Decodable {
    let id: Int
    let shortCode: String
    let stage: String
    let displayStage: String
    let storeId: Int
    let storeName: String
    let storeImageUrl: String
    let storeEtaMinutes: Int
    let placedAt: Date
    let deliveredAt: Date?
    let isDelivery: Bool
    let paymentLabelAr: String
    let scheduledForAr: String?
    let noteAr: String?
    let addressLabelAr: String
    let addressLineAr: String
    let addressLat: Double
    let addressLng: Double
    let subtotal: Double
    let deliveryFee: Double
    let serviceFee: Double
    let discount: Double
    let tip: Double
    let total: Double
    let itemCount: Int
    let items: [OrderItemDTO]
    let events: [OrderEventDTO]
    let courier: CourierMiniDTO?
}

struct OrderItemDTO: Decodable {
    let id: Int
    let productId: Int?
    let nameAr: String
    let imageUrl: String
    let unitPrice: Double
    let quantity: Int
    let selectedOptionsAr: String
    let noteAr: String
    let lineTotal: Double
}

struct OrderEventDTO: Decodable {
    let stage: String
    let at: Date
    let note: String?
}

struct CourierMiniDTO: Decodable {
    let id: Int
    let name: String
    let rating: Double
    let trips: Int
    let vehicleAr: String
    let plateAr: String
    let photoUrl: String
    let latitude: Double
    let longitude: Double
}

struct OrderDetailDTO: Decodable {
    let id: Int
    let shortCode: String
    let stage: String
    let stageAr: String
    let displayStage: String
    let placedAt: Date
    let deliveredAt: Date?
    let isDelivery: Bool
    let paymentLabelAr: String
    let scheduledForAr: String?
    let noteAr: String?
    let customerNameAr: String
    let customerPhone: String
    let addressLabelAr: String
    let addressLineAr: String
    let addressLat: Double
    let addressLng: Double
    let storeId: Int
    let storeNameAr: String
    let storeCuisineAr: String
    let storeAddressAr: String
    let storePhone: String
    let storeLat: Double
    let storeLng: Double
    let storeImageUrl: String
    let storeEtaMinutes: Int
    let courier: CourierMiniDTO?
    let courierPayout: Double
    let distanceKm: Double
    let subtotal: Double
    let deliveryFee: Double
    let serviceFee: Double
    let discount: Double
    let tip: Double
    let total: Double
    let itemCount: Int
    let items: [OrderItemDTO]
    let events: [OrderEventDTO]
}

struct MerchantTicketDTO: Decodable {
    let id: Int
    let shortCode: String
    let customerNameAr: String
    let state: String
    let placedAt: Date
    let acceptDeadline: Date
    let isDelivery: Bool
    let courierNameAr: String?
    let noteAr: String?
    let itemCount: Int
    let total: Double
    let items: [OrderItemDTO]
}

struct CourierJobDTO: Decodable {
    let orderId: Int
    let shortCode: String
    let stage: String
    let cta: String
    let storeNameAr: String
    let storeAddressAr: String
    let storeLat: Double
    let storeLng: Double
    let customerNameAr: String
    let dropoffAddressAr: String
    let dropLat: Double
    let dropLng: Double
    let distanceKm: Double
    let estMinutes: Int
    let payout: Double
    let itemCount: Int
    var customerPhone: String = ""
}

struct MerchantMenuItemDTO: Decodable {
    let productId: Int
    let nameAr: String
    let descAr: String
    let price: Double
    let imageUrl: String
    let inStock: Bool
    let soldCount: Int
    let categoryAr: String
}

struct CourierSummaryDTO: Decodable {
    let todayEarnings: Double
    let todayTrips: Int
    let todayDistanceKm: Double
    let deliveryPart: Double
    let tipsPart: Double
    let bonusPart: Double
    let week: [EarningsDayDTO]
    let recentTrips: [CourierTripDTO]
}

struct EarningsDayDTO: Decodable {
    let weekdayAr: String
    let amount: Double
    let trips: Int
}

struct MerchantStoreDTO: Decodable {
    let id: Int
    let name: String
    let kind: String
    let cuisineAr: String
    let rating: Double
    let ratingCount: Int
    let prepMinutes: Int
    let isBusy: Bool
    let isClosed: Bool
    let heroImageUrl: String
    let addressAr: String
    let cityAr: String
    let openingHoursAr: String
    let ibanLast4: String
    let deliveryRadiusKm: Double
    let menuItemCount: Int
    let ownerNameAr: String
    let ownerPhone: String
    var onboardingStatus: String = ""
    var logoUrl: String = ""
}

struct MerchantStoreUpdateBody: Encodable {
    var name: String? = nil
    var cuisineAr: String? = nil
    var addressAr: String? = nil
    var openingHoursAr: String? = nil
    var ibanLast4: String? = nil
}

struct CourierProfileDTO: Decodable {
    let id: Int
    let name: String
    let rating: Double
    let ratingCount: Int
    let trips: Int
    let vehicleAr: String
    let plateAr: String
    let photoUrl: String
    let isOnline: Bool
    let acceptanceRate: Int
    let completionRate: Int
    let latitude: Double
    let longitude: Double
    let todayEarnings: Double
    let todayTrips: Int
    let todayDistanceKm: Double
    var onboardingStatus: String = ""
}

struct MerchantReportsDTO: Decodable {
    let totalSales: Double
    let orderCount: Int
    let avgOrderValue: Double
    let rating: Double
    let prepMinutes: Int
    let days: [EarningsDayDTO]
    let topItems: [MerchantTopItemDTO]
}

struct MerchantTopItemDTO: Decodable {
    let nameAr: String
    let count: Int
    let revenue: Double
}

struct CourierTripDTO: Decodable {
    let shortCode: String
    let whenAr: String
    let amount: Double
    let rating: Double
}

// ── request bodies ──────────────────────────────────────────────────────────
struct PlaceOrderBody: Encodable {
    let storeId: Int
    let items: [CartItemBody]
    let addressLabelAr: String
    let addressLineAr: String
    let addressLat: Double
    let addressLng: Double
    let isDelivery: Bool
    let paymentLabelAr: String
    let tip: Double
    let scheduledForAr: String?
    let promoCode: String?
    let noteAr: String?
}

struct CartItemBody: Encodable {
    let productId: Int
    let quantity: Int
    let optionIds: [Int]
    let noteAr: String?
}

struct CourierStatusBody: Encodable {
    let isOnline: Bool
    let latitude: Double
    let longitude: Double
}

struct MerchantMenuUpsertBody: Encodable {
    let nameAr: String
    let descAr: String
    let price: Double
    let inStock: Bool
    let imageUrl: String?
    let categoryAr: String
}

struct MerchantStatusBody: Encodable {
    let isOnline: Bool?
    let isBusy: Bool?
    let prepMinutes: Int?
}
