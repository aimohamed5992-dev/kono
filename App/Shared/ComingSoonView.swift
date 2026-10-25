import SwiftUI

/// Placeholder for screens delivered in a later build step. Styled so the shell
/// still feels finished during review.
struct ComingSoonView: View {
    let title: String
    var illustration: String = "illo_tracking"
    var note: String = "تُنفَّذ في خطوة لاحقة"

    @State private var float = false

    var body: some View {
        ZStack {
            KonoColor.background.ignoresSafeArea()
            VStack(spacing: KonoSpace.l) {
                AssetImage(name: illustration, fallbackSymbol: "sparkles",
                           fallbackTint: KonoColor.caramel, contentMode: .fit)
                    .frame(width: 180, height: 180)
                    .offset(y: float ? -8 : 8)
                    .animation(KonoMotion.ambient, value: float)

                Text(title.k).font(KonoFont.title).foregroundStyle(KonoColor.textPrimary)
                KonoTag(text: note, systemImage: "hammer.fill", color: KonoColor.caramel)
            }
        }
        .onAppear { float = true }
    }
}
