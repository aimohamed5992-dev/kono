import SwiftUI

/// Kono brand palette — derived from the wordmark logo:
/// brand yellow `#FDD003` and deep espresso brown `#582808`.
/// Every colour in the app pulls from here so the identity stays consistent
/// across the customer, merchant and courier surfaces.
enum KonoColor {

    // MARK: Brand

    /// Primary brand yellow. Backgrounds, primary buttons, highlights.
    static let yellow = Color(hex: 0xFDD003)
    /// A slightly deeper yellow for pressed states and gradients.
    static let yellowDeep = Color(hex: 0xF3B90A)
    /// Soft warm cream — app canvas, cards sitting on yellow.
    static let cream = Color(hex: 0xFFF7E0)

    /// Primary brand brown. Text on yellow, primary iconography.
    static let brown = Color(hex: 0x582808)
    /// Mid caramel — secondary accents, badges, illustration fills.
    static let caramel = Color(hex: 0xC8811E)
    /// Very dark brown for high-contrast headings.
    static let espresso = Color(hex: 0x3A1B05)

    // MARK: Semantic surface

    static let background = Color(hex: 0xFFFBF2)
    static let surface = Color.white
    static let surfaceRaised = Color(hex: 0xFFFDF8)
    static let surfaceSunken = Color(hex: 0xF6EEDD)
    static let divider = Color(hex: 0x582808, alpha: 0.10)
    static let hairline = Color(hex: 0x582808, alpha: 0.06)
    static let overlay = Color(hex: 0x2A1403, alpha: 0.45)

    // MARK: Semantic text

    static let textPrimary = Color(hex: 0x2E1703)
    static let textSecondary = Color(hex: 0x8A6B4A)
    static let textTertiary = Color(hex: 0xB09675)
    static let textOnYellow = Color(hex: 0x582808)
    static let textOnBrown = Color(hex: 0xFFF7E0)

    // MARK: Status

    static let success = Color(hex: 0x2E9E5B)
    static let warning = Color(hex: 0xE0A000)
    static let danger = Color(hex: 0xD1493B)
    static let info = Color(hex: 0x2F7DBE)
    static let rating = Color(hex: 0xF2A200)

    /// Vertical accent colours — used for category theming while staying in family.
    static func vertical(_ kind: VerticalKind) -> Color {
        switch kind {
        case .restaurants: return Color(hex: 0xD1493B)
        case .groceries:   return Color(hex: 0x2E9E5B)
        case .pharmacy:    return Color(hex: 0x2F7DBE)
        case .sweets:      return Color(hex: 0xC8811E)
        case .flowers:     return Color(hex: 0xB5497E)
        case .water:       return Color(hex: 0x3AA6C8)
        }
    }

    // MARK: Gradients

    static var brandGradient: LinearGradient {
        LinearGradient(
            colors: [yellow, yellowDeep],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static var splashGradient: LinearGradient {
        LinearGradient(
            colors: [Color(hex: 0xFFDE3D), yellow, Color(hex: 0xF2B60A)],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    static var creamGradient: LinearGradient {
        LinearGradient(
            colors: [cream, Color(hex: 0xFFFDF6)],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}
