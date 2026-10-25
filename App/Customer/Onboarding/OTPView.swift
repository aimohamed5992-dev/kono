import SwiftUI

struct OTPView: View {
    let phone: String
    var onVerified: () -> Void
    var onBack: (() -> Void)? = nil

    /// The prototype has no real SMS — this is the code shown on screen.
    static let demoCode = "1234"

    private let length = 4
    @State private var code = ""
    @State private var secondsLeft = 30
    @State private var wrong = false
    @State private var verifying = false
    @State private var appear = false
    @FocusState private var focused: Bool

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            KonoColor.background.ignoresSafeArea()

            VStack(alignment: .leading, spacing: KonoSpace.l) {
                if let onBack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(KonoColor.brown)
                            .frame(width: 40, height: 40)
                            .background(KonoColor.surface, in: Circle())
                            .konoShadow(.sm)
                    }
                    .buttonStyle(.plain)
                    .padding(.top, KonoSpace.s)
                }

                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    KonoMark(size: 48).konoShadow(.md)
                        .scaleEffect(appear ? 1 : 0.7).opacity(appear ? 1 : 0)
                    Text("رمز التحقق")
                        .font(KonoFont.largeTitle)
                        .foregroundStyle(KonoColor.espresso)
                        .riseIn()
                    (Text("أرسلنا رمزاً من ٤ أرقام إلى ")
                        .foregroundColor(KonoColor.textSecondary)
                     + Text(phone).foregroundColor(KonoColor.textPrimary).bold())
                        .font(KonoFont.body)
                        .environment(\.layoutDirection, .leftToRight)
                        .riseIn(delay: 0.05)
                }
                .padding(.top, KonoSpace.s)

                // demo code banner
                HStack(spacing: KonoSpace.m) {
                    Image(systemName: "wand.and.stars")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(KonoColor.brown)
                    VStack(alignment: .leading, spacing: 1) {
                        Text("رمز التجربة").font(KonoFont.caption).foregroundStyle(KonoColor.textSecondary)
                        Text(Self.demoCode)
                            .font(.cairo(22, .heavy))
                            .foregroundStyle(KonoColor.textPrimary)
                            .kerning(4)
                    }
                    Spacer()
                    Button {
                        withAnimation(KonoMotion.snappy) { code = Self.demoCode }
                        verify()
                    } label: {
                        Text("تعبئة تلقائية")
                            .font(KonoFont.caption.weight(.bold))
                            .foregroundStyle(KonoColor.textOnBrown)
                            .padding(.horizontal, KonoSpace.m).padding(.vertical, 8)
                            .background(KonoColor.brown, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(KonoSpace.m)
                .background(KonoColor.cream, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                    .strokeBorder(KonoColor.brown.opacity(0.16), lineWidth: 1))
                .riseIn(delay: 0.1)

                // OTP boxes
                ZStack {
                    TextField("", text: $code)
                        .keyboardType(.numberPad)
                        .textContentType(.oneTimeCode)
                        .focused($focused)
                        .opacity(0.02)
                        .onChange(of: code) { _, new in
                            code = String(new.filter(\.isNumber).prefix(length))
                            wrong = false
                            if code.count == length { verify() }
                        }

                    HStack(spacing: KonoSpace.m) {
                        ForEach(0..<length, id: \.self) { i in otpCell(i) }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                }
                .riseIn(delay: 0.14)
                .modifier(Shake(animatableData: wrong ? 1 : 0))

                HStack(spacing: 4) {
                    if secondsLeft > 0 {
                        Text("إعادة إرسال الرمز خلال \(secondsLeft) ثانية")
                            .font(KonoFont.footnote)
                            .foregroundStyle(KonoColor.textTertiary)
                    } else {
                        Button("إعادة إرسال الرمز") {
                            secondsLeft = 30
                            #if canImport(UIKit)
                            UINotificationFeedbackGenerator().notificationOccurred(.success)
                            #endif
                        }
                        .font(KonoFont.subheadline)
                        .foregroundStyle(KonoColor.brown)
                    }
                }

                Spacer()

                KonoButton(title: "تأكيد", loading: verifying,
                           enabled: code.count == length) { verify() }
                    .padding(.bottom, KonoSpace.l)
            }
            .konoScreenPadding()
        }
        .onReceive(timer) { _ in if secondsLeft > 0 { secondsLeft -= 1 } }
        .onAppear {
            withAnimation(KonoMotion.spring) { appear = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { focused = true }
        }
    }

    private func otpCell(_ i: Int) -> some View {
        let chars = Array(code)
        let filled = i < chars.count
        let active = i == chars.count && focused
        return Text(filled ? String(chars[i]) : "")
            .font(.cairo(26, .heavy))
            .foregroundStyle(KonoColor.textPrimary)
            .frame(width: 62, height: 66)
            .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                    .strokeBorder(wrong ? KonoColor.danger
                                  : (active || filled ? KonoColor.brown : KonoColor.brown.opacity(0.14)),
                                  lineWidth: active ? 2 : 1.2)
            )
            .scaleEffect(filled ? 1 : (active ? 1.04 : 1))
            .animation(KonoMotion.snappy, value: code)
            .animation(KonoMotion.snappy, value: focused)
    }

    private func verify() {
        guard !verifying else { return }
        focused = false
        verifying = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            verifying = false
            // Prototype: the demo code — or any 4 digits — passes.
            if code.count == length {
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
                onVerified()
            } else {
                withAnimation(KonoMotion.snappy) { wrong = true }
                code = ""
                focused = true
            }
        }
    }
}

/// Horizontal shake for invalid input.
struct Shake: GeometryEffect {
    var animatableData: CGFloat
    func effectValue(size: CGSize) -> ProjectionTransform {
        let travel = 8 * sin(animatableData * .pi * 4)
        return ProjectionTransform(CGAffineTransform(translationX: travel, y: 0))
    }
}

#Preview { OTPView(phone: "+966 55 123 4567") {} }
