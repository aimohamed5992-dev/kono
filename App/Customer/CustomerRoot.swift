import SwiftUI

/// Customer experience. Auth happens in `RootFlow` before we get here; this only
/// owns the one-time "set your delivery location" step for a fresh account.
struct CustomerRoot: View {
    enum Step { case location, app }

    @State private var step: Step
    @State private var data = AppData.shared
    @State private var checked = false

    init() {
        _step = State(initialValue: RootFlow.launch.screen == "location" ? .location : .app)
    }

    var body: some View {
        ZStack {
            switch step {
            case .location:
                LocationSetupView(onConfirm: { addr in
                    Task {
                        await data.addAddress(labelAr: addr.labelAr, lineAr: addr.lineAr,
                                              cityAr: addr.cityAr, icon: addr.icon,
                                              lat: addr.latitude, lng: addr.longitude, makeDefault: true)
                        go(.app)
                    }
                }, onBack: { go(.app) })
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            case .app:
                CustomerTabView()
                    .transition(.opacity)
            }
        }
        .animation(KonoMotion.spring, value: step)
        .task {
            guard !checked else { return }
            checked = true
            await data.ensureSignedIn(role: .customer)
            if data.addresses.isEmpty && RootFlow.launch.screen == nil {
                withAnimation(KonoMotion.spring) { step = .location }
            }
        }
    }

    private func go(_ s: Step) { withAnimation(KonoMotion.spring) { step = s } }
}
