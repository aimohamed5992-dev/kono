import SwiftUI

// MARK: - Card

struct KonoCard<Content: View>: View {
    var padding: CGFloat = KonoSpace.l
    var radius: CGFloat = KonoRadius.l
    var shadow: KonoShadow = .sm
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(KonoColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(KonoColor.hairline, lineWidth: 1)
            )
            .konoShadow(shadow)
    }
}

// MARK: - Chip

struct KonoChip: View {
    let title: String
    var icon: String? = nil
    var selected: Bool = false
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                if let icon { Image(systemName: icon).font(.system(size: 12, weight: .bold)) }
                Text(title.k).font(KonoFont.subheadline)
            }
            .foregroundStyle(selected ? KonoColor.textOnBrown : KonoColor.brown)
            .padding(.horizontal, KonoSpace.m)
            .frame(height: KonoSize.chipHeight)
            .background(selected ? KonoColor.brown : KonoColor.surface)
            .clipShape(Capsule())
            .overlay(Capsule().strokeBorder(
                selected ? .clear : KonoColor.brown.opacity(0.16), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .animation(KonoMotion.snappy, value: selected)
    }
}

// MARK: - Tag / Badge

struct KonoTag: View {
    let text: String
    var systemImage: String? = nil
    var color: Color = KonoColor.brown
    var filled: Bool = false

    var body: some View {
        HStack(spacing: 3) {
            if let systemImage {
                Image(systemName: systemImage).font(.system(size: 9, weight: .bold))
            }
            Text(text.k).font(.cairo(11, .bold))
        }
        .foregroundStyle(filled ? .white : color)
        .padding(.horizontal, 7)
        .padding(.vertical, 4)
        .background(filled ? color : color.opacity(0.12), in: Capsule())
    }
}

struct RatingBadge: View {
    let rating: Double
    var count: Int? = nil
    var compact: Bool = false

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "star.fill")
                .font(.system(size: compact ? 9 : 11))
                .foregroundStyle(KonoColor.rating)
            Text(rating.formatted(.number.precision(.fractionLength(1))))
                .font(.cairo(compact ? 11 : 13, .bold))
                .foregroundStyle(KonoColor.textPrimary)
            if let count {
                Text("(\(count.formatted()))")
                    .font(.cairo(compact ? 10 : 11, .medium))
                    .foregroundStyle(KonoColor.textSecondary)
            }
        }
    }
}

// MARK: - Section header

struct SectionHeader: View {
    let title: String
    var subtitle: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title.k).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                if let subtitle {
                    Text(subtitle.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
            }
            Spacer()
            if let actionTitle, let action {
                KonoPillButton(title: actionTitle, icon: "chevron.left", action: action)
            }
        }
    }
}

// MARK: - Quantity stepper

struct QuantityStepper: View {
    @Binding var quantity: Int
    var minValue: Int = 1
    var maxValue: Int = 99
    var compact: Bool = false

    var body: some View {
        HStack(spacing: compact ? 8 : 14) {
            stepButton("minus") {
                if quantity > minValue { quantity -= 1 }
            }
            Text("\(quantity)")
                .font(.cairo(compact ? 14 : 16, .heavy))
                .foregroundStyle(KonoColor.textPrimary)
                .frame(minWidth: 18)
                .contentTransition(.numericText())
            stepButton("plus") {
                if quantity < maxValue { quantity += 1 }
            }
        }
        .padding(.horizontal, compact ? 6 : 8)
        .padding(.vertical, compact ? 4 : 6)
        .background(KonoColor.cream, in: Capsule())
        .overlay(Capsule().strokeBorder(KonoColor.brown.opacity(0.12), lineWidth: 1))
        .animation(KonoMotion.snappy, value: quantity)
    }

    private func stepButton(_ symbol: String, _ act: @escaping () -> Void) -> some View {
        Button {
            #if canImport(UIKit)
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            #endif
            act()
        } label: {
            Image(systemName: symbol)
                .font(.system(size: compact ? 10 : 12, weight: .black))
                .foregroundStyle(KonoColor.brown)
                .frame(width: compact ? 22 : 28, height: compact ? 22 : 28)
                .background(KonoColor.surface, in: Circle())
                .konoShadow(.sm)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Empty state

struct KonoEmptyState: View {
    var illustration: String? = nil
    var symbol: String = "tray"
    let title: String
    var message: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    @State private var appear = false

    var body: some View {
        VStack(spacing: KonoSpace.l) {
            Group {
                if let illustration {
                    AssetImage(name: illustration, fallbackSymbol: symbol,
                               fallbackTint: KonoColor.caramel, contentMode: .fit)
                } else {
                    Image(systemName: symbol)
                        .font(.system(size: 54, weight: .light))
                        .foregroundStyle(KonoColor.caramel.opacity(0.6))
                        .frame(width: 160, height: 160)
                }
            }
            .frame(width: 200, height: 200)
            .scaleEffect(appear ? 1 : 0.8)
            .opacity(appear ? 1 : 0)

            VStack(spacing: KonoSpace.xs) {
                Text(title.k).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                if let message {
                    Text(message.k)
                        .font(KonoFont.callout)
                        .foregroundStyle(KonoColor.textSecondary)
                        .multilineTextAlignment(.center)
                }
            }
            .riseIn(delay: 0.1)

            if let actionTitle, let action {
                KonoButton(title: actionTitle, kind: .primary, fullWidth: false, action: action)
                    .riseIn(delay: 0.18)
            }
        }
        .padding(KonoSpace.xl)
        .frame(maxWidth: .infinity)
        .onAppear { withAnimation(KonoMotion.bouncy) { appear = true } }
    }
}
