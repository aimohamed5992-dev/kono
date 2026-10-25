import SwiftUI

/// App-wide language toggle. The bundle ships Arabic (base) + English strings;
/// switching updates the SwiftUI `locale` and `layoutDirection` everywhere.
@MainActor
@Observable
final class AppLanguage {
    static let shared = AppLanguage()

    enum Lang: String, CaseIterable {
        case ar, en
        var displayName: String { self == .ar ? "العربية" : "English" }
        var otherName: String { self == .ar ? "English" : "العربية" }
    }

    private let key = "kono.lang"

    var current: Lang {
        didSet {
            UserDefaults.standard.set(current.rawValue, forKey: key)
            // keep UIKit-side APIs (formatters, share sheets) aligned too
            UserDefaults.standard.set([current.rawValue], forKey: "AppleLanguages")
        }
    }

    private init() {
        let env = ProcessInfo.processInfo.environment["KONO_LANG"]
        let saved = UserDefaults.standard.string(forKey: "kono.lang")
        let start = Lang(rawValue: env ?? "") ?? Lang(rawValue: saved ?? "") ?? .ar
        current = start
        UserDefaults.standard.set(start.rawValue, forKey: key)
        UserDefaults.standard.set([start.rawValue], forKey: "AppleLanguages")
    }

    var locale: Locale { Locale(identifier: current == .ar ? "ar" : "en") }
    var layoutDirection: LayoutDirection { current == .ar ? .rightToLeft : .leftToRight }
    var isRTL: Bool { current == .ar }

    func toggle() {
        current = current == .ar ? .en : .ar
    }
}

extension EnvironmentValues {
    @Entry var appLanguage: AppLanguage = .shared
}

/// In-app language switching: SwiftUI `Text("literal")` resolves against
/// `Bundle.main`, which normally follows the *system* language. We reclass
/// `Bundle.main` so every `localizedString(forKey:…)` reads from the `.lproj`
/// the user picked — no app relaunch needed.
final class KonoBundle: Bundle, @unchecked Sendable {
    override func localizedString(forKey key: String, value: String?, table: String?) -> String {
        let lang = UserDefaults.standard.string(forKey: "kono.lang") ?? "ar"
        if let path = objc_getAssociatedObject(self, &KonoBundle.pathsKey) as? [String: String],
           let lproj = path[lang], let b = Bundle(path: lproj) {
            return b.localizedString(forKey: key, value: value, table: table)
        }
        return super.localizedString(forKey: key, value: value, table: table)
    }

    private nonisolated(unsafe) static var pathsKey = 0

    static func install() {
        let base = Bundle.main
        var paths: [String: String] = [:]
        for l in ["ar", "en"] {
            if let p = base.path(forResource: l, ofType: "lproj") { paths[l] = p }
        }
        object_setClass(base, KonoBundle.self)
        objc_setAssociatedObject(base, &pathsKey, paths, .OBJC_ASSOCIATION_RETAIN)
    }
}

extension String {
    /// Kono-localized: resolves this string against the chosen language bundle.
    /// `Text("literal")` already localizes via SwiftUI; use `.k` for strings that
    /// reach a view as a plain `String` (component titles, computed values).
    var k: String {
        let lang = UserDefaults.standard.string(forKey: "kono.lang") ?? "ar"
        guard lang != "ar",
              let path = Bundle.main.path(forResource: lang, ofType: "lproj"),
              let b = Bundle(path: path) else { return self }
        return b.localizedString(forKey: self, value: self, table: nil)
    }
}

/// A settings row that flips the app between Arabic and English.
struct LanguageToggleRow: View {
    @State private var lang = AppLanguage.shared

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.25)) { lang.toggle() }
        } label: {
            HStack(spacing: KonoSpace.m) {
                Image(systemName: "globe").font(.system(size: 15))
                    .foregroundStyle(KonoColor.brown).frame(width: 26)
                Text("اللغة").font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                Text(lang.current.displayName)
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 12, weight: .bold)).foregroundStyle(KonoColor.textTertiary)
            }
            .padding(.vertical, KonoSpace.m).padding(.horizontal, KonoSpace.l)
            .background(KonoColor.surface).konoCardShape(KonoRadius.m)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Apply the chosen language's locale + direction to a subtree.
struct LocalizedRoot<Content: View>: View {
    @State private var lang = AppLanguage.shared
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .environment(\.locale, lang.locale)
            .environment(\.layoutDirection, lang.layoutDirection)
            .environment(\.appLanguage, lang)
            .id(lang.current)          // force a clean re-layout on switch
    }
}
