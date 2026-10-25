import SwiftUI

/// 4-pt spacing grid.
enum KonoSpace {
    static let xxs: CGFloat = 2
    static let xs: CGFloat = 4
    static let s: CGFloat = 8
    static let m: CGFloat = 12
    static let l: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 28
    static let xxxl: CGFloat = 40
    static let screen: CGFloat = 16
}

/// Corner radii — generous and soft, matching the wordmark's rounded terminals.
enum KonoRadius {
    static let xs: CGFloat = 6
    static let s: CGFloat = 10
    static let m: CGFloat = 14
    static let l: CGFloat = 20
    static let xl: CGFloat = 28
    static let pill: CGFloat = 999
}

enum KonoSize {
    static let buttonHeight: CGFloat = 54
    static let fieldHeight: CGFloat = 52
    static let chipHeight: CGFloat = 36
    static let iconTile: CGFloat = 56
    static let avatar: CGFloat = 44
    static let minTap: CGFloat = 44
}

extension View {
    /// Standard screen horizontal padding.
    func konoScreenPadding() -> some View {
        padding(.horizontal, KonoSpace.screen)
    }

    /// Rounded rectangle clip + hairline border used by most cards.
    func konoCardShape(_ radius: CGFloat = KonoRadius.l) -> some View {
        clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(KonoColor.hairline, lineWidth: 1)
            )
    }
}
