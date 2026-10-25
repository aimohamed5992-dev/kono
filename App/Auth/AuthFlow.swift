import SwiftUI

/// The app-level authentication flow: shown once (after splash / onboarding) until
/// the user signs in — then the session is remembered across launches.
struct AuthFlow: View {
    /// Called once a session exists. The caller reads `AppData.shared.role`.
    var onAuthenticated: () -> Void

    enum Step: Equatable { case welcome, register, otp }

    @State private var step: Step
    @State private var phone = ""
    @State private var name = ""
    @State private var role: AppRole = .customer
    @State private var busy = false
    @State private var error: String?
    @State private var data = AppData.shared

    init(initialStep: Step = .welcome, onAuthenticated: @escaping () -> Void) {
        _step = State(initialValue: initialStep)
        self.onAuthenticated = onAuthenticated
    }

    var body: some View {
        ZStack {
            KonoColor.background.ignoresSafeArea()

            switch step {
            case .welcome:
                AuthWelcomeView(
                    busy: busy,
                    onLogin: { p in phone = p; name = ""; role = .customer; go(.otp) },
                    onRegister: { go(.register) })
                    .transition(.opacity)

            case .register:
                RegisterView(
                    onBack: { go(.welcome) },
                    onContinue: { n, p, r in
                        name = n; phone = p; role = r; go(.otp)
                    })
                    .transition(.move(edge: .trailing).combined(with: .opacity))

            case .otp:
                OTPView(
                    phone: phone,
                    onVerified: verifyOTP,
                    onBack: { go(step == .otp && !name.isEmpty ? .register : .welcome) })
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .animation(KonoMotion.spring, value: step)
        .alert("تعذّر تسجيل الدخول", isPresented: Binding(
            get: { error != nil }, set: { if !$0 { error = nil } })) {
            Button("حسناً", role: .cancel) {}
        } message: { Text(error ?? "") }
    }

    private func go(_ s: Step) { withAnimation(KonoMotion.spring) { step = s } }

    private func verifyOTP() {
        Task {
            do {
                try await data.completePhoneAuth(
                    phone: phone,
                    name: name.trimmingCharacters(in: .whitespaces).isEmpty ? nil : name,
                    role: role)
                onAuthenticated()
            } catch {
                self.error = (error as? LocalizedError)?.errorDescription ?? "حدث خطأ، حاول مرة أخرى."
                go(.welcome)
            }
        }
    }
}

// MARK: - Welcome / login

struct AuthWelcomeView: View {
    var busy: Bool
    var onLogin: (String) -> Void
    var onRegister: () -> Void

    @State private var phone = ""
    @State private var appear = false
    @FocusState private var focused: Bool?

    private var digits: String { phone.filter(\.isNumber) }
    private var isValid: Bool { digits.count == 9 && digits.first == "5" }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KonoSpace.xl) {
                // brand header
                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    KonoMark(size: 56).konoShadow(.md)
                        .scaleEffect(appear ? 1 : 0.7).opacity(appear ? 1 : 0)
                    Text("أهلاً بك في كونو")
                        .font(KonoFont.display(28)).foregroundStyle(KonoColor.espresso)
                        .riseIn(delay: 0.04)
                    Text("سجّل دخولك برقم جوالك، ولا تحتاج تسجّل كل مرة.")
                        .font(KonoFont.body).foregroundStyle(KonoColor.textSecondary)
                        .riseIn(delay: 0.08)
                }
                .padding(.top, KonoSpace.l)

                // phone entry
                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    Text("رقم الجوال").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                    PhonePrefixField(text: $phone, focused: $focused)
                    KonoButton(title: "متابعة", loading: busy, enabled: isValid) {
                        focused = false
                        onLogin("+966" + digits)
                    }
                }
                .riseIn(delay: 0.12)

                // register
                HStack(spacing: 5) {
                    Text("ما عندك حساب؟").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                    Button("أنشئ حساب جديد", action: onRegister)
                        .font(KonoFont.subheadline.weight(.bold)).foregroundStyle(KonoColor.brown)
                    Spacer()
                }
                .riseIn(delay: 0.16)

                Spacer(minLength: KonoSpace.xl)

                Text("بمتابعتك فإنك توافق على الشروط والأحكام وسياسة الخصوصية.")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                    .frame(maxWidth: .infinity).multilineTextAlignment(.center)
                    .padding(.bottom, KonoSpace.l)
            }
            .konoScreenPadding()
            .frame(minHeight: 480)
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear { withAnimation(KonoMotion.spring) { appear = true } }
    }
}

// MARK: - Register

struct RegisterView: View {
    var onBack: () -> Void
    var onContinue: (_ name: String, _ phone: String, _ role: AppRole) -> Void

    @State private var role: AppRole = .customer
    @State private var name = ""
    @State private var phone = ""
    @State private var appear = false
    @FocusState private var focus: Field?
    private enum Field { case name, phone }

    private var digits: String { phone.filter(\.isNumber) }
    private var isValid: Bool {
        digits.count == 9 && digits.first == "5" &&
        name.trimmingCharacters(in: .whitespaces).count >= 2
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KonoSpace.xl) {
                Button(action: onBack) {
                    Image(systemName: "chevron.right").font(.system(size: 16, weight: .bold))
                        .foregroundStyle(KonoColor.brown).frame(width: 40, height: 40)
                        .background(KonoColor.surface, in: Circle()).konoShadow(.sm)
                }
                .buttonStyle(.plain)

                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    Text("حساب جديد").font(KonoFont.largeTitle).foregroundStyle(KonoColor.espresso)
                        .riseIn()
                    Text("اختر نوع الحساب وأدخل بياناتك.")
                        .font(KonoFont.body).foregroundStyle(KonoColor.textSecondary).riseIn(delay: 0.05)
                }

                // role picker
                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    Text("نوع الحساب").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                    HStack(spacing: KonoSpace.s) {
                        ForEach(AppRole.allCases) { r in
                            RolePill(role: r, selected: role == r) {
                                withAnimation(KonoMotion.snappy) { role = r }
                            }
                        }
                    }
                }
                .riseIn(delay: 0.1)

                // fields
                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text(nameLabel).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                        KonoTextField(placeholder: nameLabel, text: $name, icon: "person")
                            .focused($focus, equals: .name)
                    }
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("رقم الجوال").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                        PhonePrefixField(text: $phone, focused: $focus, equals: .phone)
                    }
                }
                .riseIn(delay: 0.15)

                KonoButton(title: "متابعة", enabled: isValid) {
                    focus = nil
                    onContinue(name.trimmingCharacters(in: .whitespaces), "+966" + digits, role)
                }
                .riseIn(delay: 0.2)

                Text("بإنشائك حساباً فإنك توافق على الشروط والأحكام وسياسة الخصوصية.")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                    .frame(maxWidth: .infinity).multilineTextAlignment(.center)
                    .padding(.bottom, KonoSpace.xl)
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private var nameLabel: String {
        switch role {
        case .customer: return "الاسم"
        case .merchant: return "اسم المتجر / المطعم"
        case .courier:  return "اسم المندوب"
        }
    }
}

private struct RolePill: View {
    let role: AppRole
    var selected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: role.icon).font(.system(size: 18, weight: .bold))
                Text(role.rawShortAr).font(.cairo(12, .bold))
            }
            .foregroundStyle(selected ? KonoColor.textOnBrown : KonoColor.brown)
            .frame(maxWidth: .infinity).frame(height: 68)
            .background(selected ? KonoColor.brown : KonoColor.surface,
                        in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                .strokeBorder(selected ? .clear : KonoColor.hairline, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Shared phone field

struct PhonePrefixField<T: Hashable>: View {
    @Binding var text: String
    var focused: FocusState<T?>.Binding
    var equals: T

    init(text: Binding<String>, focused: FocusState<T?>.Binding, equals: T) {
        _text = text; self.focused = focused; self.equals = equals
    }

    var body: some View {
        HStack(spacing: KonoSpace.s) {
            HStack(spacing: 6) {
                Text("🇸🇦").font(.system(size: 18))
                Text("+966").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
            }
            .padding(.horizontal, KonoSpace.m)
            .frame(height: KonoSize.fieldHeight)
            .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                .strokeBorder(KonoColor.hairline, lineWidth: 1))

            TextField("5X XXX XXXX", text: $text)
                .font(KonoFont.headline)
                .keyboardType(.numberPad)
                .focused(focused, equals: equals)
                .environment(\.layoutDirection, .leftToRight)
                .onChange(of: text) { _, v in text = String(v.filter(\.isNumber).prefix(9)) }
                .padding(.horizontal, KonoSpace.m)
                .frame(height: KonoSize.fieldHeight)
                .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                    .strokeBorder(focused.wrappedValue == equals ? KonoColor.brown : KonoColor.hairline,
                                  lineWidth: focused.wrappedValue == equals ? 1.5 : 1))
                .animation(KonoMotion.ease, value: focused.wrappedValue)
        }
    }
}

/// Convenience for the login screen, which has a single field.
extension PhonePrefixField where T == Bool {
    init(text: Binding<String>, focused: FocusState<Bool?>.Binding) {
        self.init(text: text, focused: focused, equals: true)
    }
}

extension AppRole {
    var rawShortAr: String {
        switch self {
        case .customer: return "عميل"
        case .merchant: return "متجر"
        case .courier:  return "مندوب"
        }
    }
}
