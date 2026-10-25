import SwiftUI

/// Shimmering placeholder block for loading states.
struct SkeletonBlock: View {
    var height: CGFloat = 16
    var width: CGFloat? = nil
    var radius: CGFloat = KonoRadius.s

    var body: some View {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
            .fill(KonoColor.surfaceSunken)
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil)
            .shimmer()
    }
}

/// Skeleton mimic of a full-width store card.
struct StoreCardSkeleton: View {
    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            SkeletonBlock(height: 150, radius: 0)
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                HStack(spacing: KonoSpace.s) {
                    Circle().fill(KonoColor.surfaceSunken).frame(width: 38, height: 38)
                    SkeletonBlock(height: 14, width: 140)
                    Spacer()
                    SkeletonBlock(height: 14, width: 40)
                }
                SkeletonBlock(height: 12, width: 200)
            }
            .padding(KonoSpace.l)
        }
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.l)
        .konoShadow(.sm)
    }
}

struct MenuRowSkeleton: View {
    var body: some View {
        HStack(alignment: .top, spacing: KonoSpace.m) {
            VStack(alignment: .leading, spacing: 8) {
                SkeletonBlock(height: 15, width: 160)
                SkeletonBlock(height: 12)
                SkeletonBlock(height: 12, width: 120)
                SkeletonBlock(height: 14, width: 60)
            }
            Spacer(minLength: 0)
            SkeletonBlock(height: 96, width: 96, radius: KonoRadius.m)
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }
}

/// Branded pull-to-refresh / inline loader — the Kono mark spinning gently.
struct KonoLoader: View {
    var size: CGFloat = 32
    @State private var spin = false

    var body: some View {
        KonoMark(size: size)
            .rotationEffect(.degrees(spin ? 360 : 0))
            .animation(.linear(duration: 1).repeatForever(autoreverses: false), value: spin)
            .onAppear { spin = true }
    }
}
