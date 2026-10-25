import SwiftUI

/// A titled section with a horizontal scroller. Every home section uses this;
/// only the card inside changes.
struct HomeRail<Content: View>: View {
    let title: String
    var subtitle: String? = nil
    var onSeeAll: (() -> Void)? = nil
    var spacing: CGFloat = KonoSpace.m
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title.k).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                    if let subtitle {
                        Text(subtitle.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    }
                }
                Spacer()
                if let onSeeAll {
                    KonoPillButton(title: "الكل", icon: "chevron.left", action: onSeeAll)
                }
            }
            .konoScreenPadding()

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: spacing) {
                    content()
                }
                .konoScreenPadding()
            }
        }
    }
}

// MARK: - 1. Featured (large hero)

struct FeaturedStoreCard: View {
    let store: Store
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .bottomLeading) {
                    AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                               fallbackTint: store.kind.accent)
                        .frame(width: 300, height: 170)
                    LinearGradient(
                        stops: [.init(color: .clear, location: 0),
                                .init(color: .black.opacity(0.15), location: 0.45),
                                .init(color: .black.opacity(0.82), location: 1)],
                        startPoint: .top, endPoint: .bottom)
                        .frame(width: 300, height: 170)
                    HStack(spacing: KonoSpace.s) {
                        LogoBubble(asset: store.logoAsset, symbol: store.kind.systemIcon,
                                   size: 36, name: store.name)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(store.name).font(KonoFont.headline).foregroundStyle(.white)
                            Text(store.cuisineAr).font(KonoFont.caption).foregroundStyle(.white.opacity(0.85))
                        }
                    }
                    .padding(KonoSpace.m)
                    if let promo = store.promoTagAr {
                        KonoTag(text: promo, systemImage: "tag.fill", color: KonoColor.danger, filled: true)
                            .padding(KonoSpace.s)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))

                HStack(spacing: KonoSpace.m) {
                    Label("\(store.rating.formatted(.number.precision(.fractionLength(1))))",
                          systemImage: "star.fill")
                        .foregroundStyle(KonoColor.rating)
                    Label("\(store.etaMinutes) دقيقة", systemImage: "clock")
                    Label(store.deliveryFee == 0 ? "توصيل مجاني" : "\(store.deliveryFee.clean) ر.س",
                          systemImage: "bicycle")
                }
                .font(KonoFont.footnote)
                .foregroundStyle(KonoColor.textSecondary)
                .labelStyle(.titleAndIcon)
                .padding(.top, KonoSpace.s)
                .padding(.horizontal, KonoSpace.xs)
            }
            .frame(width: 300)
        }
                .pressableScale(0.98)
    }
}

// MARK: - 2. Deal card (offer emphasis)

struct DealCard: View {
    let store: Store
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .topTrailing) {
                    AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                               fallbackTint: store.kind.accent)
                        .frame(width: 230, height: 130)
                    Text(store.promoTagAr ?? "عرض")
                        .font(.cairo(12, .black))
                        .foregroundStyle(.white)
                        .padding(.horizontal, KonoSpace.s).padding(.vertical, 5)
                        .background(KonoColor.danger, in: Capsule())
                        .padding(KonoSpace.s)
                }
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))

                VStack(alignment: .leading, spacing: 3) {
                    Text(store.name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                        .lineLimit(1)
                    HStack(spacing: 4) {
                        RatingBadge(rating: store.rating, compact: true)
                        Text("· \(store.etaMinutes) د").font(KonoFont.footnote)
                            .foregroundStyle(KonoColor.textSecondary)
                    }
                }
                .padding(KonoSpace.s)
            }
            .frame(width: 230)
            .background(KonoColor.surface)
            .konoCardShape(KonoRadius.m)
            .konoShadow(.sm)
        }
                .pressableScale(0.97)
    }
}

// MARK: - 3. Circle card (supermarkets / brands)

struct StoreCircleCard: View {
    let store: Store
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: KonoSpace.s) {
                AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                           fallbackTint: store.kind.accent)
                    .frame(width: 96, height: 96)
                    .clipShape(Circle())
                    .overlay(Circle().strokeBorder(KonoColor.hairline, lineWidth: 1))
                    .konoShadow(.sm)
                    .overlay(alignment: .bottom) {
                        if store.deliveryFee == 0 {
                            Text("توصيل مجاني")
                                .font(.cairo(8, .bold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6).padding(.vertical, 3)
                                .background(KonoColor.success, in: Capsule())
                                .offset(y: 8)
                        }
                    }
                Text(store.name)
                    .font(.cairo(12, .semibold))
                    .foregroundStyle(KonoColor.textPrimary)
                    .lineLimit(1)
                Text("\(store.etaMinutes) دقيقة")
                    .font(.cairo(10, .medium))
                    .foregroundStyle(KonoColor.textSecondary)
            }
            .frame(width: 108)
        }
                .pressableScale()
    }
}

// MARK: - 4. Pharmacy / quick card (wide, accent bar)

struct QuickStoreCard: View {
    let store: Store
    var accent: Color = KonoColor.info
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: KonoSpace.m) {
                AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                           fallbackTint: accent)
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text(store.name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                        .lineLimit(1)
                    HStack(spacing: 4) {
                        RatingBadge(rating: store.rating, compact: true)
                    }
                    Label("\(store.etaMinutes) دقيقة", systemImage: "bolt.fill")
                        .font(.cairo(10, .bold))
                        .foregroundStyle(accent)
                }
                Spacer(minLength: 0)
            }
            .padding(KonoSpace.s)
            .frame(width: 250)
            .background(KonoColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .overlay(alignment: .leading) {
                Rectangle().fill(accent).frame(width: 4)
            }
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .konoShadow(.sm)
        }
                .pressableScale(0.98)
    }
}

// MARK: - 5. Cuisine tile (browse by category)

struct CuisineTile: View {
    let title: String
    let asset: String
    let symbol: String
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                AssetImage(name: asset, fallbackSymbol: symbol, fallbackTint: KonoColor.caramel)
                    .frame(width: 130, height: 92)
                LinearGradient(colors: [.clear, .black.opacity(0.5)],
                               startPoint: .center, endPoint: .bottom)
                Text(title)
                    .font(.cairo(13, .bold))
                    .foregroundStyle(.white)
                    .padding(KonoSpace.s)
            }
            .frame(width: 130, height: 92)
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
        }
                .pressableScale(0.97)
    }
}

// MARK: - 6. Poster card (sweets / occasions)

struct PosterStoreCard: View {
    let store: Store
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottom) {
                AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                           fallbackTint: store.kind.accent)
                    .frame(width: 170, height: 230)
                LinearGradient(colors: [.clear, .black.opacity(0.75)],
                               startPoint: .center, endPoint: .bottom)
                    .frame(width: 170, height: 230)
                VStack(alignment: .leading, spacing: 4) {
                    Text(store.name).font(KonoFont.headline).foregroundStyle(.white)
                    Text(store.cuisineAr).font(KonoFont.caption).foregroundStyle(.white.opacity(0.85))
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill").font(.system(size: 9)).foregroundStyle(KonoColor.yellow)
                        Text(store.rating.formatted(.number.precision(.fractionLength(1))))
                            .font(.cairo(11, .bold)).foregroundStyle(.white)
                        Text("· \(store.etaMinutes) د").font(.cairo(10))
                            .foregroundStyle(.white.opacity(0.8))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(KonoSpace.m)
            }
            .frame(width: 170, height: 230)
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
        }
                .pressableScale(0.98)
    }
}
