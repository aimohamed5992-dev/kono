import SwiftUI

struct CourierTabView: View {
    @State private var selection: Int
    @State private var session = CourierSession()

    init() {
        let s = RootFlow.launch.screen
        _selection = State(initialValue: {
            switch s {
            case "earnings": return 1
            case "history": return 2
            case "profile": return 3
            default: return 0
            }
        }())
        let sess = CourierSession()
        switch s {
        case "online": sess.isOnline = true
        case "offer": sess.isOnline = true; sess.offer = Mock.courierOffer()
        case "active", "delivery":
            sess.isOnline = true
            var j = Mock.courierOffer(); j.stage = .headingToCustomer
            sess.activeJob = j
        default: break
        }
        _session = State(initialValue: sess)
    }

    private let items = [
        KonoTabItem(title: "الرحلات", icon: "map", selectedIcon: "map.fill"),
        KonoTabItem(title: "الأرباح", icon: "banknote", selectedIcon: "banknote.fill"),
        KonoTabItem(title: "السجل", icon: "clock.arrow.circlepath", selectedIcon: "clock.arrow.circlepath"),
        KonoTabItem(title: "الملف", icon: "person", selectedIcon: "person.fill"),
    ]

    var body: some View {
        KonoTabScaffold(items: items, selection: $selection) { index in
            switch index {
            case 0: CourierHomeView()
            case 1: CourierEarningsView()
            case 2: CourierHistoryView()
            default: CourierProfileView()
            }
        }
        .environment(\.courier, session)
    }
}
