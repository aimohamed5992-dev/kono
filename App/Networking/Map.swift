import Foundation
import SwiftUI

/// DTO → app-model mapping.
enum Wire {
    static func vertical(_ d: VerticalDTO) -> Vertical {
        Vertical(kind: VerticalKind(apiName: d.kind),
                 etaMinutes: 25, storeCount: 0)
    }

    static func promo(_ d: PromotionDTO) -> PromoSlide {
        let cta: String
        if d.titleAr.contains("كونو بلس") || d.titleAr.contains("اشترك") { cta = "اشترك الآن" }
        else if d.titleAr.contains("مقاضي") || d.titleAr.contains("تسوّق") || d.titleAr.contains("سوبرماركت") { cta = "تسوّق الآن" }
        else { cta = "اطلب الآن" }
        return PromoSlide(apiId: d.id, titleAr: d.titleAr, subtitleAr: d.subtitleAr,
                          ctaAr: cta, illustration: d.imageUrl, bg: Color(hex: hex(d.backgroundHex)))
    }

    static func address(_ d: AddressDTO) -> Address {
        Address(apiId: d.id, labelAr: d.labelAr, lineAr: d.lineAr, cityAr: d.cityAr,
                icon: d.icon, latitude: d.latitude, longitude: d.longitude, isDefault: d.isDefault)
    }

    static func payment(_ d: PaymentMethodDTO) -> PaymentMethod {
        PaymentMethod(apiId: d.id, labelAr: d.labelAr, icon: d.icon,
                      detailAr: d.detailAr, isDefault: d.isDefault)
    }

    static func store(_ d: StoreDTO) -> Store {
        Store(apiId: d.id, name: d.name, kind: VerticalKind(apiName: d.kind), cuisineAr: d.cuisineAr,
              rating: d.rating, ratingCount: d.ratingCount, etaMinutes: d.etaMinutes,
              deliveryFee: d.deliveryFee, minOrder: d.minOrder, distanceKm: d.distanceKm,
              isBusy: d.isBusy, isClosed: d.isClosed, promoTagAr: d.promoTagAr,
              heroAsset: d.heroImageUrl, logoAsset: d.logoImageUrl,
              tagsAr: d.tags, priceLevel: d.priceLevel)
    }

    static func store(detail d: StoreDetailDTO) -> Store {
        Store(apiId: d.id, name: d.name, kind: VerticalKind(apiName: d.kind), cuisineAr: d.cuisineAr,
              rating: d.rating, ratingCount: d.ratingCount, etaMinutes: d.etaMinutes,
              deliveryFee: d.deliveryFee, minOrder: d.minOrder, distanceKm: d.distanceKm,
              isBusy: d.isBusy, isClosed: d.isClosed, promoTagAr: d.promoTagAr,
              heroAsset: d.heroImageUrl, logoAsset: d.logoImageUrl,
              tagsAr: d.tags, priceLevel: d.priceLevel)
    }

    static func menuCategory(_ d: MenuCategoryDTO) -> MenuCategory {
        MenuCategory(nameAr: d.nameAr, items: d.items.map(product))
    }

    static func product(_ d: ProductDTO) -> Product {
        Product(apiId: d.id, nameAr: d.nameAr, descAr: d.descAr, price: d.price, oldPrice: d.oldPrice,
                imageAsset: d.imageUrl, isPopular: d.isPopular, calories: d.calories,
                options: d.optionGroups.map { g in
                    ProductOptionGroup(titleAr: g.titleAr, required: g.required, multiSelect: g.multiSelect,
                        choices: g.choices.map { ProductOption(apiId: $0.id, nameAr: $0.nameAr, priceDelta: $0.priceDelta) })
                },
                unitAr: d.unitAr, inStock: d.inStock)
    }

    static func order(_ d: OrderDTO) -> Order {
        let store = Store(apiId: d.storeId, name: d.storeName, kind: .restaurants, cuisineAr: "",
                          rating: 0, ratingCount: 0, etaMinutes: d.storeEtaMinutes, deliveryFee: d.deliveryFee,
                          minOrder: 0, distanceKm: 0, promoTagAr: nil,
                          heroAsset: d.storeImageUrl, logoAsset: "")
        let lines = d.items.map { i in
            CartLine(product: Product(apiId: i.productId ?? 0, nameAr: i.nameAr, descAr: "",
                                      price: i.unitPrice, imageAsset: i.imageUrl),
                     quantity: i.quantity,
                     selectedOptionsAr: i.selectedOptionsAr.isEmpty ? [] :
                        i.selectedOptionsAr.components(separatedBy: " · "),
                     note: i.noteAr)
        }
        let addr = Address(labelAr: d.addressLabelAr, lineAr: d.addressLineAr, cityAr: "الرياض",
                           icon: "house.fill", latitude: d.addressLat, longitude: d.addressLng)
        var courier: Courier?
        if let c = d.courier {
            courier = Courier(name: c.name, rating: c.rating, trips: c.trips, vehicleAr: c.vehicleAr,
                              plateAr: c.plateAr, photoAsset: c.photoUrl,
                              latitude: c.latitude, longitude: c.longitude)
        }
        var order = Order(apiId: d.id, shortCode: d.shortCode, store: store, lines: lines,
                          stage: OrderStage(display: d.displayStage), placedAt: d.placedAt,
                          address: addr, courier: courier, paymentLabelAr: d.paymentLabelAr,
                          scheduledForAr: d.scheduledForAr)
        order.serverSubtotal = d.subtotal
        order.serverDeliveryFee = d.deliveryFee
        order.serverServiceFee = d.serviceFee
        order.serverDiscount = d.discount
        order.serverTip = d.tip
        order.serverTotal = d.total
        order.events = d.events.map { OrderStageEvent(stage: OrderStage(display: displayFor($0.stage)), at: $0.at) }
        return order
    }

    static func ticket(_ d: MerchantTicketDTO) -> MerchantTicket {
        let lines = d.items.map { i in
            CartLine(product: Product(apiId: i.productId ?? 0, nameAr: i.nameAr, descAr: "",
                                      price: i.unitPrice, imageAsset: i.imageUrl),
                     quantity: i.quantity,
                     selectedOptionsAr: i.selectedOptionsAr.isEmpty ? [] :
                        i.selectedOptionsAr.components(separatedBy: " · "),
                     note: i.noteAr)
        }
        return MerchantTicket(apiId: d.id, shortCode: d.shortCode, customerNameAr: d.customerNameAr,
                              lines: lines, placedAt: d.placedAt, state: ticketState(d.state),
                              isDelivery: d.isDelivery, courierNameAr: d.courierNameAr, noteAr: d.noteAr,
                              acceptDeadline: d.acceptDeadline)
    }

    static func job(_ d: CourierJobDTO) -> CourierJob {
        CourierJob(apiId: d.orderId, shortCode: d.shortCode, storeNameAr: d.storeNameAr,
                   storeAddressAr: d.storeAddressAr, dropoffAddressAr: d.dropoffAddressAr,
                   customerNameAr: d.customerNameAr, distanceKm: d.distanceKm, estMinutes: d.estMinutes,
                   payout: d.payout, itemCount: d.itemCount, isBatched: false,
                   stage: jobStage(d.stage),
                   storeLat: d.storeLat, storeLng: d.storeLng, dropLat: d.dropLat, dropLng: d.dropLng,
                   customerPhone: d.customerPhone)
    }

    static func merchantStore(_ d: MerchantStoreDTO) -> MerchantProfile {
        MerchantProfile(
            apiId: d.id, storeName: d.name, kind: VerticalKind(apiName: d.kind),
            cuisineAr: d.cuisineAr,
            isOnline: !d.isClosed, isBusy: d.isBusy,
            rating: d.rating, ratingCount: d.ratingCount,
            todayOrders: 0, todayRevenue: 0, prepMinutes: d.prepMinutes,
            heroAsset: d.heroImageUrl, addressAr: d.addressAr,
            openingHoursAr: d.openingHoursAr, ibanLast4: d.ibanLast4,
            deliveryRadiusKm: d.deliveryRadiusKm, menuItemCount: d.menuItemCount,
            ownerNameAr: d.ownerNameAr, ownerPhone: d.ownerPhone,
            onboardingStatus: d.onboardingStatus,
            logoAsset: d.logoUrl.isEmpty ? d.heroImageUrl : d.logoUrl)
    }

    static func courierProfile(_ d: CourierProfileDTO) -> CourierProfile {
        CourierProfile(
            name: d.name, rating: d.rating, ratingCount: d.ratingCount,
            isOnline: d.isOnline, vehicleAr: d.vehicleAr, plateAr: d.plateAr,
            todayTrips: d.todayTrips, todayEarnings: d.todayEarnings,
            todayDistanceKm: d.todayDistanceKm, acceptanceRate: d.acceptanceRate,
            completionRate: d.completionRate,
            photoAsset: d.photoUrl.isEmpty ? "courier_1" : d.photoUrl,
            latitude: d.latitude, longitude: d.longitude,
            onboardingStatus: d.onboardingStatus)
    }

    static func menuItem(_ d: MerchantMenuItemDTO) -> MenuItemStatus {
        MenuItemStatus(
            product: Product(apiId: d.productId, nameAr: d.nameAr, descAr: d.descAr,
                             price: d.price, imageAsset: d.imageUrl),
            available: d.inStock, soldToday: d.soldCount)
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    static func hex(_ s: String) -> UInt32 {
        var t = s.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        if t.count == 3 { t = t.map { "\($0)\($0)" }.joined() }
        return UInt32(t, radix: 16) ?? 0xFFE79A
    }

    static func ticketState(_ s: String) -> TicketState {
        switch s {
        case "preparing": return .preparing
        case "ready": return .ready
        case "handedOver": return .handedOver
        case "completed": return .completed
        default: return .incoming
        }
    }

    static func jobStage(_ s: String) -> JobStage {
        switch s {
        case "headingToStore": return .headingToStore
        case "atStore": return .atStore
        case "pickedUp": return .pickedUp
        case "headingToCustomer": return .headingToCustomer
        case "arrived": return .arrived
        case "delivered": return .delivered
        default: return .offered
        }
    }

    /// API OrderEvent.stage is the raw enum ("Confirmed"…); reduce to displayStage.
    static func displayFor(_ raw: String) -> String {
        switch raw {
        case "Confirmed": return "confirmed"
        case "Preparing": return "preparing"
        case "ReadyForPickup", "CourierAssigned", "PickedUp": return "readyForPickup"
        case "OnTheWay", "Arrived": return "onTheWay"
        case "Delivered": return "delivered"
        case "Cancelled": return "cancelled"
        default: return "placed"
        }
    }
}

