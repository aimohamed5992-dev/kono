import SwiftUI

/// Holds which Kono experience is showing. Set by `RootFlow` from the signed-in
/// account's role (`AppData.role`).
@Observable
final class AppRouter {
    var role: AppRole

    init(role: AppRole) { self.role = role }
}

private struct AppRouterKey: EnvironmentKey {
    static let defaultValue = AppRouter(role: .customer)
}

extension EnvironmentValues {
    var router: AppRouter {
        get { self[AppRouterKey.self] }
        set { self[AppRouterKey.self] = newValue }
    }
}
