import SwiftUI

@main
struct KonoApp: App {
    init() {
        KonoBundle.install()
        KonoFonts.register()
        KonoAppearance.apply()
    }

    var body: some Scene {
        WindowGroup {
            LocalizedRoot {
                RootFlow()
                    .font(KonoFont.body)
                    .tint(KonoColor.brown)
                    .preferredColorScheme(.light)
            }
        }
    }
}

/// UIKit chrome that SwiftUI modifiers don't reach — nav-bar + tab-bar titles
/// use the Cairo face so the whole app is typographically consistent.
enum KonoAppearance {
    static func apply() {
        let nav = UINavigationBarAppearance()
        nav.configureWithTransparentBackground()
        nav.titleTextAttributes = [.font: UIFont.cairo(17, weight: .bold)]
        nav.largeTitleTextAttributes = [.font: UIFont.cairo(30, weight: .bold)]
        UINavigationBar.appearance().standardAppearance = nav
        UINavigationBar.appearance().scrollEdgeAppearance = nav
        UINavigationBar.appearance().compactAppearance = nav

        UIBarButtonItem.appearance().setTitleTextAttributes(
            [.font: UIFont.cairo(15, weight: .semibold)], for: .normal)
    }
}
