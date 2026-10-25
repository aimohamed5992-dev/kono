import SwiftUI

/// Shared motion vocabulary. Kono's animation language is "quick, springy, warm":
/// short durations, gentle overshoot, nothing that keeps the user waiting.
enum KonoMotion {

    /// Default spring for most view transitions and layout changes.
    static let spring = Animation.spring(response: 0.42, dampingFraction: 0.82, blendDuration: 0.1)

    /// Snappier spring for taps, toggles and chips.
    static let snappy = Animation.spring(response: 0.30, dampingFraction: 0.72)

    /// Soft spring with a touch more bounce — used for celebratory moments.
    static let bouncy = Animation.spring(response: 0.5, dampingFraction: 0.6)

    /// Smooth ease for opacity / colour cross-fades.
    static let ease = Animation.easeInOut(duration: 0.25)

    /// Slow ambient loop (e.g. splash shimmer, live pulse).
    static let ambient = Animation.easeInOut(duration: 1.6).repeatForever(autoreverses: true)

    /// Staggered entrance delay for list items.
    static func stagger(_ index: Int, step: Double = 0.055, cap: Int = 12) -> Animation {
        spring.delay(step * Double(min(index, cap)))
    }
}

/// Press-scale button style used by cards and tappable rows.
/// Implemented as a `ButtonStyle` (not a gesture) so taps keep working inside
/// scroll views — a `simultaneousGesture(DragGesture(minimumDistance: 0))` on a
/// button swallows the tap when the button lives in a `ScrollView`.
struct KonoPressStyle: ButtonStyle {
    var scale: CGFloat = 0.97

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .scaleEffect(configuration.isPressed ? scale : 1)
            .animation(KonoMotion.snappy, value: configuration.isPressed)
    }
}

/// Fade + rise entrance. Self-triggers on appear; pass `trigger` to re-play it
/// when a parent value changes (e.g. a filter switch rebuilding a list).
struct RiseIn: ViewModifier {
    var delay: Double = 0
    var distance: CGFloat = 16
    var trigger: AnyHashable? = nil

    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : distance)
            .onAppear {
                guard !shown else { return }
                withAnimation(KonoMotion.spring.delay(delay)) { shown = true }
            }
            .onChange(of: trigger ?? AnyHashable(0)) { _, _ in
                shown = false
                withAnimation(KonoMotion.spring.delay(delay)) { shown = true }
            }
    }
}

extension View {
    /// Apply to a `Button` — replaces `.buttonStyle(.plain)` and adds a press-scale.
    func pressableScale(_ scale: CGFloat = 0.97) -> some View {
        buttonStyle(KonoPressStyle(scale: scale))
    }

    func riseIn(delay: Double = 0, distance: CGFloat = 16, trigger: AnyHashable? = nil) -> some View {
        modifier(RiseIn(delay: delay, distance: distance, trigger: trigger))
    }

    /// Shimmering placeholder sweep for skeleton loading states.
    func shimmer(active: Bool = true) -> some View {
        modifier(Shimmer(active: active))
    }
}

struct Shimmer: ViewModifier {
    let active: Bool
    @State private var phase: CGFloat = -1

    func body(content: Content) -> some View {
        content
            .overlay {
                if active {
                    GeometryReader { geo in
                        let w = geo.size.width
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.55), .clear],
                            startPoint: .leading, endPoint: .trailing
                        )
                        .frame(width: w * 0.6)
                        .offset(x: phase * w * 1.6)
                        .blendMode(.plusLighter)
                    }
                    .allowsHitTesting(false)
                    .onAppear {
                        withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) {
                            phase = 1
                        }
                    }
                }
            }
            .clipped()
    }
}
