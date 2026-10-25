import SwiftUI

struct CustomerTabView: View {
    @State private var selection: Int
    @State private var cart: CartStore

    init() {
        let s = RootFlow.launch.screen
        _selection = State(initialValue: {
            switch s {
            case "offers", "deals": return 1
            case "orders": return 2
            case "account", "chat", "chats": return 3
            default: return 0
            }
        }())
        // Seed a cart so tracking / orders / cart deep-links have content.
        let seedScreens: Set<String?> = ["cart", "checkout", "tracking", "orders"]
        _cart = State(initialValue: seedScreens.contains(s) ? .seeded : CartStore())
    }

    private let items = [
        KonoTabItem(title: "الرئيسية", icon: "house", selectedIcon: "house.fill"),
        KonoTabItem(title: "العروض", icon: "tag", selectedIcon: "tag.fill"),
        KonoTabItem(title: "الطلبات", icon: "bag", selectedIcon: "bag.fill", badge: 1),
        KonoTabItem(title: "حسابي", icon: "person", selectedIcon: "person.fill"),
    ]

    var body: some View {
        KonoTabScaffold(items: items, selection: $selection) { index in
            switch index {
            case 0:
                CustomerHomeView()
            case 1:
                OffersView()
            case 2:
                OrdersListView()
            default:
                AccountView()
            }
        }
        .environment(\.cart, cart)
    }
}
