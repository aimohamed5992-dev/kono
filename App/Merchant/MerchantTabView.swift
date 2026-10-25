import SwiftUI

struct MerchantTabView: View {
    @State private var selection: Int
    @State private var session = MerchantSession()

    init() {
        let s = RootFlow.launch.screen
        _selection = State(initialValue: {
            switch s {
            case "menu": return 1
            case "reports": return 2
            case "store": return 3
            default: return 0
            }
        }())
    }

    private var items: [KonoTabItem] {
        [
            KonoTabItem(title: "الطلبات", icon: "tray.full", selectedIcon: "tray.full.fill",
                        badge: session.newCount),
            KonoTabItem(title: "القائمة", icon: "menucard", selectedIcon: "menucard.fill"),
            KonoTabItem(title: "التقارير", icon: "chart.bar", selectedIcon: "chart.bar.fill"),
            KonoTabItem(title: "المتجر", icon: "storefront", selectedIcon: "storefront.fill"),
        ]
    }

    var body: some View {
        KonoTabScaffold(items: items, selection: $selection) { index in
            switch index {
            case 0: MerchantDashboardView()
            case 1: MerchantMenuView()
            case 2: MerchantReportsView()
            default: MerchantStoreView()
            }
        }
        .environment(\.merchant, session)
    }
}
