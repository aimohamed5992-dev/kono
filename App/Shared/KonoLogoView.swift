import SwiftUI

/// The "k" mark inside a rounded-square tile — the app's compact identity,
/// reused in nav bars, the splash, auth headers and the courier/merchant chrome.
struct KonoMark: View {
    var size: CGFloat = 44
    var background: Color = KonoColor.yellow
    var foreground: Color = KonoColor.brown

    var body: some View {
        Text("k")
            .font(.cairo(size * 0.62, .black))
            .foregroundStyle(foreground)
            .frame(width: size, height: size)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.28, style: .continuous))
    }
}

/// Full wordmark. Uses the supplied logo asset when present, otherwise renders
/// the text so previews and early builds still show branding.
struct KonoWordmark: View {
    var height: CGFloat = 30
    var onYellow: Bool = false

    private var hasAsset: Bool {
        #if canImport(UIKit)
        UIImage(named: "KonoLogo") != nil
        #else
        false
        #endif
    }

    var body: some View {
        if hasAsset && onYellow {
            Image("KonoLogo")
                .resizable()
                .scaledToFit()
                .frame(height: height * 2.4)
        } else {
            Text("kono")
                .font(.cairo(height, .black))
                .foregroundStyle(onYellow ? KonoColor.brown : KonoColor.brown)
                .kerning(-0.5)
        }
    }
}

/// Animated splash: the mark scales in with a spring, a soft ring pulses out,
/// then the wordmark fades up beneath it.
struct KonoSplash: View {
    var onFinished: () -> Void = {}

    @State private var markIn = false
    @State private var ring = false
    @State private var wordIn = false
    @State private var lift = false

    private var hasBackdrop: Bool {
        #if canImport(UIKit)
        UIImage(named: "splash_bg") != nil
        #else
        false
        #endif
    }

    var body: some View {
        ZStack {
            // Manus-generated full-bleed branded backdrop, with a graceful fallback.
            if hasBackdrop {
                Image("splash_bg")
                    .resizable()
                    .scaledToFill()
                    .ignoresSafeArea()
                    .scaleEffect(lift ? 1.06 : 1)
                    .animation(.easeInOut(duration: 6).repeatForever(autoreverses: true), value: lift)
            } else {
                KonoColor.splashGradient.ignoresSafeArea()
                ForEach(0..<6, id: \.self) { i in
                    Circle()
                        .fill(KonoColor.brown.opacity(0.05))
                        .frame(width: CGFloat(40 + i * 26))
                        .offset(x: CGFloat((i % 2 == 0 ? -1 : 1) * (60 + i * 18)),
                                y: CGFloat((i - 3) * 90))
                        .blur(radius: 2)
                        .scaleEffect(markIn ? 1 : 0.6)
                        .animation(KonoMotion.spring.delay(Double(i) * 0.04), value: markIn)
                }
            }

            VStack(spacing: KonoSpace.xl) {
                ZStack {
                    Circle()
                        .stroke(KonoColor.brown.opacity(0.25), lineWidth: 2)
                        .frame(width: 130, height: 130)
                        .scaleEffect(ring ? 1.8 : 0.6)
                        .opacity(ring ? 0 : 1)
                    KonoMark(size: 96, background: KonoColor.brown, foreground: KonoColor.yellow)
                        .konoShadow(.lg)
                        .scaleEffect(markIn ? 1 : 0.3)
                        .rotationEffect(.degrees(markIn ? 0 : -18))
                        .opacity(markIn ? 1 : 0)
                }

                KonoWordmark(height: 34)
                    .opacity(wordIn ? 1 : 0)
                    .offset(y: wordIn ? 0 : 12)

                Text("أكل · مقاضي · صيدليات · توصيل سريع")
                    .font(KonoFont.subheadline)
                    .foregroundStyle(KonoColor.brown.opacity(0.7))
                    .opacity(wordIn ? 1 : 0)
            }
            .offset(y: lift ? -8 : 0)
        }
        .onAppear {
            withAnimation(KonoMotion.bouncy) { markIn = true }
            withAnimation(.easeOut(duration: 1.1).delay(0.15)) { ring = true }
            withAnimation(KonoMotion.spring.delay(0.35)) { wordIn = true }
            withAnimation(KonoMotion.ambient) { lift = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0, execute: onFinished)
        }
    }
}
