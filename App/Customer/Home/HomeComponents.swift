import SwiftUI

/// Category tile in the vertical selector strip — a professional photo bubble.
struct VerticalTile: View {
    let vertical: Vertical
    var selected: Bool = false
    var onTap: () -> Void = {}

    private var asset: String { "cat_\(vertical.kind.rawValue)" }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: KonoSpace.s) {
                AssetImage(name: asset, fallbackSymbol: vertical.kind.systemIcon,
                           fallbackTint: vertical.kind.accent)
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous)
                            .strokeBorder(selected ? vertical.kind.accent : KonoColor.hairline,
                                          lineWidth: selected ? 2.5 : 1)
                    )
                    .konoShadow(selected ? .sm : .none)
                    .scaleEffect(selected ? 1.05 : 1)
                    .animation(KonoMotion.snappy, value: selected)
                Text(vertical.kind.titleAr.k)
                    .font(.cairo(12, .semibold))
                    .foregroundStyle(selected ? KonoColor.textPrimary : KonoColor.textSecondary)
            }
        }
                .pressableScale()
    }
}

struct PromoSlide: Identifiable {
    let id = UUID()
    var apiId: Int = 0
    let titleAr: String
    let subtitleAr: String
    var ctaAr: String = "اطلب الآن"
    let illustration: String
    let bg: Color
}

/// Auto-advancing promo carousel. Every slide is a designed ad banner: a full
/// photo behind a legibility scrim, with a bold headline, a line of copy and a
/// call-to-action button. Slides are stacked and cross-faded by toggling opacity
/// only — no `ScrollView`, `TabView` or layout-moving transition, so advancing
/// can never tug the vertical home scroll.
struct PromoCarousel: View {
    let slides: [PromoSlide]
    var onTap: (PromoSlide) -> Void = { _ in }
    var height: CGFloat = 188
    @State private var index = 0
    private let timer = Timer.publish(every: 4.5, on: .main, in: .common).autoconnect()

    private var safeIndex: Int { slides.isEmpty ? 0 : min(index, slides.count - 1) }

    var body: some View {
        VStack(spacing: KonoSpace.s) {
            ZStack {
                ForEach(Array(slides.enumerated()), id: \.element.id) { i, slide in
                    slideView(slide)
                        .opacity(i == safeIndex ? 1 : 0)
                        .allowsHitTesting(i == safeIndex)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
            .konoScreenPadding()
            .highPriorityGesture(
                DragGesture(minimumDistance: 20)
                    .onEnded { v in
                        guard slides.count > 1,
                              abs(v.translation.width) > abs(v.translation.height),
                              abs(v.translation.width) > 40 else { return }
                        advance(by: v.translation.width < 0 ? 1 : -1)
                    }
            )
            .animation(.easeInOut(duration: 0.35), value: safeIndex)

            if slides.count > 1 {
                HStack(spacing: 5) {
                    ForEach(slides.indices, id: \.self) { i in
                        Capsule()
                            .fill(i == safeIndex ? KonoColor.brown : KonoColor.brown.opacity(0.2))
                            .frame(width: i == safeIndex ? 18 : 6, height: 6)
                            .animation(KonoMotion.snappy, value: safeIndex)
                    }
                }
            }
        }
        .onReceive(timer) { _ in advance(by: 1) }
    }

    private func advance(by step: Int) {
        guard slides.count > 1 else { return }
        index = (safeIndex + step + slides.count) % slides.count
    }

    private func slideView(_ slide: PromoSlide) -> some View {
        ZStack(alignment: .topLeading) {
            AssetImage(name: slide.illustration, fallbackSymbol: "gift.fill",
                       fallbackTint: KonoColor.brown)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(slide.bg)

            // heavy legibility panel on the copy side (leading == right in RTL)
            LinearGradient(
                stops: [
                    .init(color: Color(hex: 0x2A1403, alpha: 0.92), location: 0.0),
                    .init(color: Color(hex: 0x2A1403, alpha: 0.80), location: 0.30),
                    .init(color: Color(hex: 0x2A1403, alpha: 0.35), location: 0.62),
                    .init(color: .clear, location: 0.9),
                ],
                startPoint: .leading, endPoint: .trailing)

            VStack(alignment: .leading, spacing: 3) {
                Text(slide.titleAr.k)
                    .font(.cairo(20, .heavy))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                    .shadow(color: .black.opacity(0.35), radius: 4, y: 1)
                Text(slide.subtitleAr.k)
                    .font(KonoFont.footnote)
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(2)
                    .shadow(color: .black.opacity(0.3), radius: 3, y: 1)
                Spacer(minLength: KonoSpace.s)
                Button { onTap(slide) } label: {
                    HStack(spacing: 5) {
                        Text(slide.ctaAr.k).font(.cairo(13, .bold))
                        Image(systemName: "arrow.left").font(.system(size: 11, weight: .bold))
                    }
                    .foregroundStyle(KonoColor.espresso)
                    .padding(.horizontal, KonoSpace.m).padding(.vertical, 9)
                    .background(KonoColor.yellow, in: Capsule())
                    .konoShadow(.md)
                }
                .buttonStyle(.plain)
            }
            .padding(KonoSpace.l)
            .frame(maxWidth: 235, alignment: .leading)
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap(slide) }
    }
}

/// Sticky-ish top bar: address selector + actions.
struct HomeTopBar: View {
    let address: Address
    var onAddress: () -> Void = {}
    var onNotifications: () -> Void = {}

    var body: some View {
        HStack(spacing: KonoSpace.m) {
            Button(action: onAddress) {
                HStack(spacing: KonoSpace.xs) {
                    Image(systemName: address.icon).font(.system(size: 13, weight: .bold))
                        .foregroundStyle(KonoColor.brown)
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(spacing: 3) {
                            Text("التوصيل إلى \(address.labelAr)")
                                .font(.cairo(12, .bold))
                                .foregroundStyle(KonoColor.textSecondary)
                            Image(systemName: "chevron.down").font(.system(size: 9, weight: .bold))
                                .foregroundStyle(KonoColor.textSecondary)
                        }
                        Text(address.lineAr.k)
                            .font(.cairo(13, .heavy))
                            .foregroundStyle(KonoColor.textPrimary)
                            .lineLimit(1)
                    }
                }
            }
            .buttonStyle(.plain)
            Spacer()
            Button(action: onNotifications) {
                Image(systemName: "bell.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(KonoColor.brown)
                    .frame(width: 40, height: 40)
                    .background(KonoColor.surface, in: Circle())
                    .overlay(Circle().strokeBorder(KonoColor.hairline, lineWidth: 1))
                    .overlay(alignment: .topTrailing) {
                        Circle().fill(KonoColor.danger).frame(width: 8, height: 8).offset(x: -2, y: 2)
                    }
            }
            .buttonStyle(.plain)
        }
    }
}
