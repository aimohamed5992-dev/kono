import SwiftUI

/// Warm-tinted elevation. Kono never uses neutral grey shadows — they read cold
/// against the cream canvas — so every shadow is a low-opacity brown.
struct KonoShadow {
    let color: Color
    let radius: CGFloat
    let x: CGFloat
    let y: CGFloat

    static let none = KonoShadow(color: .clear, radius: 0, x: 0, y: 0)

    static let sm = KonoShadow(
        color: Color(hex: 0x582808, alpha: 0.08), radius: 6, x: 0, y: 3
    )
    static let md = KonoShadow(
        color: Color(hex: 0x582808, alpha: 0.10), radius: 16, x: 0, y: 8
    )
    static let lg = KonoShadow(
        color: Color(hex: 0x582808, alpha: 0.14), radius: 28, x: 0, y: 14
    )
    static let yellowGlow = KonoShadow(
        color: Color(hex: 0xF3B90A, alpha: 0.45), radius: 20, x: 0, y: 10
    )
}

extension View {
    func konoShadow(_ shadow: KonoShadow) -> some View {
        self.shadow(color: shadow.color, radius: shadow.radius, x: shadow.x, y: shadow.y)
    }
}
