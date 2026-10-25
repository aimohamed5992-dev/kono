import SwiftUI

/// Top-level flow: splash → (first-run onboarding) → sign-in → the role's experience.
/// A signed-in session is remembered, so returning users land straight in the app.
struct RootFlow: View {
    private enum Phase { case splash, onboarding, auth, applying, running }

    /// Demo hooks — see `LaunchOptions`. `KONO_ROLE`, `KONO_SKIP_INTRO`,
    /// `KONO_SCREEN`, `KONO_INTRO` (env) or `-role x` / `-skipIntro` / `-intro` (args).
    static let launch = LaunchOptions()

    @AppStorage("kono.hasOnboarded") private var hasOnboarded = false

    @State private var phase: Phase
    @State private var router = AppRouter(role: RootFlow.launch.role ?? .customer)
    @State private var data = AppData.shared
    @State private var authStep: AuthFlow.Step = .welcome
    @State private var applyRole: AppRole = .merchant

    init() {
        let l = RootFlow.launch
        if l.screen == "apply_merchant" || l.screen == "apply_courier" {
            _phase = State(initialValue: .applying)
            _applyRole = State(initialValue: l.screen == "apply_courier" ? .courier : .merchant)
        } else if l.screen == "onboarding" {
            _phase = State(initialValue: .onboarding)
        } else if l.screen == "auth" || l.screen == "otp" || l.screen == "register" {
            _phase = State(initialValue: .auth)
        } else if l.role != nil {
            _phase = State(initialValue: .running)          // screenshots / demo deep-link
        } else {
            _phase = State(initialValue: .splash)
        }
    }

    struct LaunchOptions {
        let role: AppRole?
        let skipIntro: Bool
        let screen: String?
        /// Whether the customer first-run flow (onboarding → auth → …) should play.
        let wantsIntro: Bool

        init() {
            let info = ProcessInfo.processInfo
            let args = info.arguments
            var picked: AppRole?
            if let i = args.firstIndex(of: "-role"), i + 1 < args.count {
                picked = AppRole(rawValue: args[i + 1])
            }
            if picked == nil, let env = info.environment["KONO_ROLE"] {
                picked = AppRole(rawValue: env)
            }
            role = picked
            let s = info.environment["KONO_SCREEN"]
            screen = s
            skipIntro = args.contains("-skipIntro")
                || info.environment["KONO_SKIP_INTRO"] != nil
                || picked != nil || s != nil
            wantsIntro = args.contains("-intro")
                || info.environment["KONO_INTRO"] != nil
                || ["onboarding", "auth", "otp", "location"].contains(s)
        }
    }

    var body: some View {
        ZStack {
            KonoColor.background.ignoresSafeArea()

            switch phase {
            case .splash:
                KonoSplash { resolveEntry() }
                    .transition(.opacity)

            case .onboarding:
                OnboardingView {
                    hasOnboarded = true
                    withAnimation(KonoMotion.spring) { phase = .auth }
                }
                .transition(.opacity)

            case .auth:
                AuthFlow(initialStep: {
                    switch RootFlow.launch.screen {
                    case "otp": return .otp
                    case "register": return .register
                    default: return authStep
                    }
                }()) {
                    router.role = data.role
                    withAnimation(KonoMotion.spring) {
                        phase = data.needsOnboarding ? .applying : .running
                    }
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))

            case .applying:
                OnboardingFlow(role: data.me == nil ? applyRole : data.role) {
                    withAnimation(KonoMotion.spring) { phase = .running }
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))

            case .running:
                experience
                    .id(router.role)
                    .transition(.opacity)
            }
        }
        .environment(\.router, router)
        .onChange(of: data.signedOut) { _, out in
            if out {
                // Logout restarts the whole entry journey: splash → onboarding → auth.
                hasOnboarded = false
                withAnimation(KonoMotion.spring) { phase = .splash }
            }
        }
    }

    /// Called when the splash finishes: restore a session if one exists.
    private func resolveEntry() {
        Task {
            let signedIn = await data.restoreSession()
            await MainActor.run {
                withAnimation(KonoMotion.spring) {
                    if signedIn {
                        router.role = data.role
                        phase = data.needsOnboarding ? .applying : .running
                    } else if !hasOnboarded {
                        phase = .onboarding
                    } else {
                        phase = .auth
                    }
                }
            }
        }
    }

    @ViewBuilder private var experience: some View {
        switch router.role {
        case .customer: CustomerRoot()
        case .merchant: MerchantTabView()
        case .courier:  CourierTabView()
        }
    }
}

#Preview { RootFlow() }
