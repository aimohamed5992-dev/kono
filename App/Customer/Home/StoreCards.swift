import SwiftUI

/// Full-width store card for the main feed.
struct StoreCard: View {
    let store: Store
    var onTap: () -> Void = {}
    @State private var fav: Bool

    init(store: Store, onTap: @escaping () -> Void = {}) {
        self.store = store
        self.onTap = onTap
        _fav = State(initialValue: store.isFavorite)
    }

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .topLeading) {
                    AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                               fallbackTint: store.kind.accent)
                        .frame(height: 150)
                        .frame(maxWidth: .infinity)

                    LinearGradient(colors: [.black.opacity(0.25), .clear],
                                   startPoint: .top, endPoint: .center)
                        .frame(height: 150)

                    HStack {
                        if let promo = store.promoTagAr {
                            KonoTag(text: promo, systemImage: "tag.fill",
                                    color: KonoColor.danger, filled: true)
                        }
                        Spacer()
                        Button {
                            #if canImport(UIKit)
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            #endif
                            withAnimation(KonoMotion.bouncy) { fav.toggle() }
                        } label: {
                            Image(systemName: fav ? "heart.fill" : "heart")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(fav ? KonoColor.danger : KonoColor.brown)
                                .frame(width: 32, height: 32)
                                .background(.ultraThinMaterial, in: Circle())
                                .scaleEffect(fav ? 1.1 : 1)
                                .heartBurst(fav)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(KonoSpace.s)

                    if store.isBusy || store.isClosed {
                        Color.black.opacity(0.35).frame(height: 150)
                        Text(store.isClosed ? "مغلق الآن" : "مزدحم — وقت أطول")
                            .font(KonoFont.caption)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10).padding(.vertical, 5)
                            .background(.black.opacity(0.5), in: Capsule())
                            .frame(height: 150)
                    }
                }
                .clipped()

                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    HStack(spacing: KonoSpace.s) {
                        LogoBubble(asset: store.logoAsset, symbol: store.kind.systemIcon, size: 38, name: store.name)
                            .offset(y: -22)
                            .padding(.bottom, -22)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(store.name).font(KonoFont.headline)
                                .foregroundStyle(KonoColor.textPrimary)
                            Text(store.cuisineAr).font(KonoFont.footnote)
                                .foregroundStyle(KonoColor.textSecondary)
                        }
                        Spacer()
                        RatingBadge(rating: store.rating, count: store.ratingCount, compact: true)
                    }

                    HStack(spacing: KonoSpace.m) {
                        Label("\(store.etaMinutes) دقيقة", systemImage: "clock")
                        Label(store.deliveryFeeText,
                              systemImage: store.deliveryFee == 0 ? "bicycle" : "bicycle")
                        Label("\(store.distanceKm.clean) كم", systemImage: "location")
                    }
                    .font(KonoFont.footnote)
                    .foregroundStyle(KonoColor.textSecondary)
                    .labelStyle(.titleAndIcon)
                }
                .padding(KonoSpace.l)
            }
            .background(KonoColor.surface)
            .konoCardShape(KonoRadius.l)
            .konoShadow(.sm)
        }
                .pressableScale(0.98)
    }
}

/// Compact horizontal card for "popular near you" rails.
struct StoreCardCompact: View {
    let store: Store
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                ZStack(alignment: .topLeading) {
                    AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                               fallbackTint: store.kind.accent)
                        .frame(width: 200, height: 116)
                    if let promo = store.promoTagAr {
                        KonoTag(text: promo, color: KonoColor.danger, filled: true)
                            .padding(KonoSpace.xs)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))

                Text(store.name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                    .lineLimit(1)
                HStack(spacing: KonoSpace.s) {
                    RatingBadge(rating: store.rating, compact: true)
                    Text("·").foregroundStyle(KonoColor.textTertiary)
                    Text("\(store.etaMinutes) د").font(KonoFont.footnote)
                        .foregroundStyle(KonoColor.textSecondary)
                    if store.distanceKm > 0 {
                        Text("·").foregroundStyle(KonoColor.textTertiary)
                        Text("\(store.distanceKm, specifier: "%.1f") كم").font(KonoFont.footnote)
                            .foregroundStyle(KonoColor.brown)
                    }
                }
                .lineLimit(1)
            }
            .frame(width: 200)
        }
                .pressableScale(0.97)
    }
}

/// Row used in search results and vertical lists. Plain content — the caller
/// wraps it in a `NavigationLink` or `Button`.
struct StoreRow: View {
    let store: Store

    var body: some View {
        HStack(spacing: KonoSpace.m) {
            AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                       fallbackTint: store.kind.accent)
                .frame(width: 76, height: 76)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(store.name).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    .lineLimit(1)
                Text(store.cuisineAr).font(KonoFont.footnote)
                    .foregroundStyle(KonoColor.textSecondary).lineLimit(1)
                HStack(spacing: KonoSpace.s) {
                    RatingBadge(rating: store.rating, compact: true)
                    Text("·").foregroundStyle(KonoColor.textTertiary)
                    Text("\(store.etaMinutes) دقيقة").font(KonoFont.footnote)
                        .foregroundStyle(KonoColor.textSecondary)
                    if store.distanceKm > 0 {
                        Text("·").foregroundStyle(KonoColor.textTertiary)
                        Text("\(store.distanceKm, specifier: "%.1f") كم").font(KonoFont.footnote)
                            .foregroundStyle(KonoColor.brown)
                    }
                    if let promo = store.promoTagAr {
                        KonoTag(text: promo, color: KonoColor.danger)
                    }
                }
            }
            Spacer()
            Image(systemName: "chevron.left").font(.system(size: 13, weight: .bold))
                .foregroundStyle(KonoColor.textTertiary)
        }
        .padding(KonoSpace.m)
        .frame(maxWidth: .infinity)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
        .contentShape(Rectangle())
    }
}
