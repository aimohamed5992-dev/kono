import SwiftUI

/// Navigation destinations shared across the customer stacks.
enum CustomerRoute: Hashable {
    case store(Store)
    case vertical(VerticalKind)
    case search(prefill: String? = nil)
    case cart
    case checkout
    case tracking(Order)
    case addresses
    case addAddress
    case editAddress(Address)
    case payments
    case notifications
    case deals(title: String)
    case nearby
    case konoPlus
    case help
    case about
    case chats
}

extension View {
    @ViewBuilder
    func customerDestinations() -> some View {
        navigationDestination(for: CustomerRoute.self) { route in
            switch route {
            case .store(let s):        StoreDetailView(store: s)
            case .vertical(let k):     VerticalListView(kind: k)
            case .search(let prefill): SearchView(prefill: prefill)
            case .cart:                CartView()
            case .checkout:            CheckoutView()
            case .tracking(let o):     OrderTrackingView(order: o)
            case .addresses:           AddressesView()
            case .addAddress:          AddAddressFlow()
            case .editAddress(let a):  AddAddressFlow(editing: a)
            case .payments:            PaymentMethodsView()
            case .notifications:       NotificationsView()
            case .deals(let title):    DealsListView(title: title)
            case .nearby:              NearbyView()
            case .konoPlus:            KonoPlusView()
            case .help:                SimpleInfoView(title: "المساعدة والدعم",
                                        body: "فريق دعم كونو متاح ٢٤ ساعة. راسلنا عبر الدردشة أو اتصل على ٩٢٠٠٠٠٠٠٠.")
            case .about:               SimpleInfoView(title: "عن كونو",
                                        body: "كونو — منصة توصيل سعودية للأكل والمقاضي والصيدليات. الإصدار ١.٠")
            case .chats:               ChatListView()
            }
        }
    }
}
