import SwiftUI

struct OnboardingPage: Identifiable {
    let id = UUID()
    let illustration: String
    let fallbackSymbol: String
    let title: String
    let body: String
}

/// Full-bleed cinematic onboarding — the illustration fills the screen and the
/// copy sits over a soft bottom scrim. No card, no bottom sheet.
struct OnboardingView: View {
    var onFinish: () -> Void

    private let pages: [OnboardingPage] = [
        OnboardingPage(
            illustration: "onb_choice", fallbackSymbol: "square.grid.2x2.fill",
            title: "كل شي تحتاجه في مكان واحد",
            body: "مطاعم، مقاضي من بندة ولولو والتميمي، وصيدليات النهدي والدواء — بطلب واحد."),
        OnboardingPage(
            illustration: "onb_delivery", fallbackSymbol: "figure.outdoor.cycle",
            title: "توصيل سريع توثق فيه",
            body: "مندوبينا حواليك على طول، وطلبك يوصلك وهو سخن خلال دقائق."),
        OnboardingPage(
            illustration: "onb_track", fallbackSymbol: "map.fill",
            title: "تابع طلبك خطوة بخطوة",
            body: "من لحظة التأكيد لين يدق بابك — تعرف وين وصل مندوبك بالضبط."),
        OnboardingPage(
            illustration: "onb_food", fallbackSymbol: "fork.knife",
            title: "ألذ الأطباق من مطاعمك المفضلة",
            body: "عروض وخصومات أسبوعية، وتوصيل مجاني على أول طلباتك."),
    ]

    @State private var index = Int(ProcessInfo.processInfo.environment["KONO_ONB_PAGE"] ?? "0") ?? 0

    var body: some View {
        ZStack {
            KonoColor.brown.ignoresSafeArea()

            // full-bleed illustration, cross-fading between pages
            ForEach(Array(pages.enumerated()), id: \.element.id) { i, page in
                if i == index {
                    AssetImage(name: page.illustration, fallbackSymbol: page.fallbackSymbol,
                               fallbackTint: KonoColor.yellow, contentMode: .fill)
                        .ignoresSafeArea()
                        .transition(.opacity)
                        .id(page.id)
                }
            }
            .animation(.easeInOut(duration: 0.45), value: index)

            // legibility scrim — stays out of the art until the lower third
            LinearGradient(
                stops: [
                    .init(color: .clear, location: 0),
                    .init(color: .clear, location: 0.5),
                    .init(color: Color(hex: 0x241203, alpha: 0.12), location: 0.62),
                    .init(color: Color(hex: 0x241203, alpha: 0.72), location: 0.82),
                    .init(color: Color(hex: 0x180C01, alpha: 0.95), location: 1),
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // top row
                HStack {
                    Button("تخطّي") { finish() }
                        .font(KonoFont.subheadline)
                        .foregroundStyle(.white.opacity(0.9))
                        .padding(.horizontal, KonoSpace.m).padding(.vertical, KonoSpace.xs)
                        .background(.ultraThinMaterial, in: Capsule())
                        .environment(\.colorScheme, .dark)
                        .opacity(index == pages.count - 1 ? 0 : 1)
                    Spacer()
                    KonoMark(size: 40)
                        .konoShadow(.md)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)

                Spacer()

                // copy + controls over the scrim
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    HStack(spacing: 6) {
                        ForEach(pages.indices, id: \.self) { i in
                            Capsule()
                                .fill(i == index ? KonoColor.yellow : Color.white.opacity(0.35))
                                .frame(width: i == index ? 26 : 8, height: 8)
                                .animation(KonoMotion.snappy, value: index)
                        }
                    }

                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text(pages[index].title)
                            .font(KonoFont.display(28))
                            .foregroundStyle(.white)
                            .fixedSize(horizontal: false, vertical: true)
                            .id("t\(index)")
                            .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity),
                                                    removal: .opacity))
                        Text(pages[index].body)
                            .font(KonoFont.body)
                            .foregroundStyle(.white.opacity(0.82))
                            .lineSpacing(4)
                            .id("b\(index)")
                            .transition(.opacity)
                    }
                    .animation(KonoMotion.spring, value: index)

                    HStack(spacing: KonoSpace.m) {
                        KonoButton(title: index == pages.count - 1 ? "يلا نبدأ" : "التالي",
                                   icon: index == pages.count - 1 ? "arrow.left" : nil,
                                   kind: .brand) {
                            if index < pages.count - 1 {
                                withAnimation(KonoMotion.spring) { index += 1 }
                            } else { finish() }
                        }
                    }

                    HStack(spacing: 4) {
                        Text("عندك حساب؟").font(KonoFont.footnote).foregroundStyle(.white.opacity(0.7))
                        Button("سجّل دخولك") { finish() }
                            .font(KonoFont.subheadline).foregroundStyle(KonoColor.yellow)
                        Spacer()
                    }
                }
                .konoScreenPadding()
                .padding(.bottom, KonoSpace.xxl)
            }

        }
        .gesture(
            DragGesture(minimumDistance: 24)
                .onEnded { v in
                    // RTL: next page lives to the left, so a left swipe advances.
                    withAnimation(KonoMotion.spring) {
                        if v.translation.width < -40, index < pages.count - 1 { index += 1 }
                        else if v.translation.width > 40, index > 0 { index -= 1 }
                    }
                }
        )
    }

    private func finish() {
        #if canImport(UIKit)
        UIImpactFeedbackGenerator(style: .soft).impactOccurred()
        #endif
        onFinish()
    }
}

#Preview { OnboardingView {} }
