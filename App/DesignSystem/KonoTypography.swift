import SwiftUI

/// Type scale for Kono. The brand face is **Cairo** (Arabic-first, with a Latin
/// companion for the English locale) — registered at launch by `KonoFonts`.
/// Sizes scale with Dynamic Type relative to a matching text style.
enum KonoFont {

    static func display(_ size: CGFloat = 34) -> Font {
        .cairo(size, .black, relativeTo: .largeTitle)
    }

    static let largeTitle   = Font.cairo(30, .bold,     relativeTo: .largeTitle)
    static let title        = Font.cairo(24, .bold,     relativeTo: .title)
    static let title2       = Font.cairo(20, .bold,     relativeTo: .title2)
    static let headline     = Font.cairo(17, .semibold, relativeTo: .headline)
    static let body         = Font.cairo(15, .regular,  relativeTo: .body)
    static let bodyMedium   = Font.cairo(15, .medium,   relativeTo: .body)
    static let callout      = Font.cairo(14, .regular,  relativeTo: .callout)
    static let subheadline  = Font.cairo(13, .medium,   relativeTo: .subheadline)
    static let footnote     = Font.cairo(12, .regular,  relativeTo: .footnote)
    static let caption      = Font.cairo(11, .semibold, relativeTo: .caption)
    static let priceLarge   = Font.cairo(22, .black,    relativeTo: .title2)
    static let price        = Font.cairo(15, .bold,     relativeTo: .body)
    static let button       = Font.cairo(16, .bold,     relativeTo: .headline)
    static let tab          = Font.cairoFixed(10, .semibold)
}

extension Text {
    func konoStyle(_ font: Font, color: Color = KonoColor.textPrimary) -> some View {
        self.font(font).foregroundStyle(color)
    }
}

/// A number formatted as Saudi Riyal, e.g. `24.50 ر.س`.
struct RiyalText: View {
    let amount: Double
    var font: Font = KonoFont.price
    var color: Color = KonoColor.textPrimary
    var showSymbol: Bool = true

    var body: some View {
        HStack(spacing: 3) {
            Text(amount.formatted(.number.precision(.fractionLength(2))))
                .font(font)
                .foregroundStyle(color)
                .contentTransition(.numericText(value: amount))
            if showSymbol {
                Text("ر.س")
                    .font(.cairo(11, .bold))
                    .foregroundStyle(color.opacity(0.7))
            }
        }
        .monospacedDigit()
    }
}
