import SwiftUI

struct KonoTabItem: Identifiable {
    let id = UUID()
    let title: String
    let icon: String
    let selectedIcon: String
    var badge: Int? = nil
}

/// Custom floating tab bar — icon-only, with a coin-style selection disc that
/// glides between tabs and a small bounce on change.
struct KonoTabBar: View {
    let items: [KonoTabItem]
    @Binding var selection: Int
    var namespace: Namespace.ID

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                let active = index == selection
                Button {
                    guard selection != index else { return }
                    #if canImport(UIKit)
                    UIImpactFeedbackGenerator(style: .soft).impactOccurred()
                    #endif
                    withAnimation(KonoMotion.spring) { selection = index }
                } label: {
                    ZStack {
                        if active {
                            Circle()
                                .fill(KonoColor.brown)
                                .matchedGeometryEffect(id: "tabdisc", in: namespace)
                                .frame(width: 44, height: 44)
                                .konoShadow(.sm)
                        }
                        Image(systemName: active ? item.selectedIcon : item.icon)
                            .font(.system(size: 20, weight: active ? .semibold : .regular))
                            .foregroundStyle(active ? KonoColor.textOnBrown : KonoColor.espresso.opacity(0.4))
                            .symbolEffect(.bounce, value: active)
                            .overlay(alignment: .topTrailing) {
                                if let badge = item.badge, badge > 0 {
                                    Text("\(badge)")
                                        .font(.cairo(9, .black))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 4).padding(.vertical, 2)
                                        .background(KonoColor.danger, in: Capsule())
                                        .overlay(Capsule().strokeBorder(KonoColor.surface, lineWidth: 1.5))
                                        .offset(x: 13, y: -10)
                                }
                            }
                    }
                    .frame(height: 48)
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                    .accessibilityLabel(item.title)
                    .accessibilityAddTraits(active ? [.isSelected] : [])
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, KonoSpace.m)
        .padding(.vertical, KonoSpace.s)
        .background(
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .fill(KonoColor.surface)
                .konoShadow(.md)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .strokeBorder(KonoColor.hairline, lineWidth: 1)
        )
        .padding(.horizontal, KonoSpace.xl)
    }
}

/// Preference used by pushed detail screens to hide the floating tab bar.
struct KonoTabBarHiddenKey: PreferenceKey {
    static var defaultValue = false
    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

extension View {
    /// Hide the Kono tab bar while this screen is on top of a navigation stack.
    func hidesKonoTabBar() -> some View {
        preference(key: KonoTabBarHiddenKey.self, value: true)
    }
}

/// Container that pairs a switchable content area with the Kono tab bar.
struct KonoTabScaffold<Content: View>: View {
    let items: [KonoTabItem]
    @Binding var selection: Int
    @ViewBuilder var content: (Int) -> Content

    @Namespace private var ns
    @State private var barHidden = false

    var body: some View {
        ZStack(alignment: .bottom) {
            content(selection)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            KonoTabBar(items: items, selection: $selection, namespace: ns)
                .padding(.bottom, KonoSpace.xs)
                .offset(y: barHidden ? 160 : 0)
                .opacity(barHidden ? 0 : 1)
                .animation(KonoMotion.spring, value: barHidden)
        }
        .ignoresSafeArea(.keyboard)
        .onPreferenceChange(KonoTabBarHiddenKey.self) { barHidden = $0 }
    }
}
