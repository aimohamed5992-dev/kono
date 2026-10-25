import SwiftUI

enum KonoButtonStyleKind {
    case primary      // brown fill, cream text — main CTA on yellow/cream
    case brand        // yellow fill, brown text — CTA on brown/photo
    case secondary    // cream fill, brown text, hairline
    case ghost        // transparent, brown text
    case destructive
}

struct KonoButton: View {
    let title: String
    var icon: String? = nil
    var kind: KonoButtonStyleKind = .primary
    var fullWidth: Bool = true
    var loading: Bool = false
    var enabled: Bool = true
    let action: () -> Void

    @State private var pop = false

    var body: some View {
        Button {
            guard enabled, !loading else { return }
            #if canImport(UIKit)
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            #endif
            withAnimation(KonoMotion.snappy) { pop = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.14) {
                withAnimation(KonoMotion.snappy) { pop = false }
            }
            action()
        } label: {
            HStack(spacing: KonoSpace.s) {
                if loading {
                    ProgressView().tint(fg)
                } else if let icon {
                    Image(systemName: icon).font(.system(size: 16, weight: .bold))
                }
                Text(title.k).font(KonoFont.button)
            }
            .foregroundStyle(fg)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .frame(height: KonoSize.buttonHeight)
            .padding(.horizontal, fullWidth ? 0 : KonoSpace.xl)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                    .strokeBorder(border, lineWidth: 1)
            )
            .konoShadow(kind == .primary || kind == .brand ? .sm : .none)
        }
        .buttonStyle(.plain)
        .scaleEffect(pop ? 0.96 : 1)
        .opacity(enabled ? 1 : 0.5)
        .disabled(!enabled || loading)
    }

    private var bg: some View {
        Group {
            switch kind {
            case .primary:     KonoColor.brown
            case .brand:       KonoColor.yellow
            case .secondary:   KonoColor.cream
            case .ghost:       Color.clear
            case .destructive: KonoColor.danger.opacity(0.12)
            }
        }
    }

    private var fg: Color {
        switch kind {
        case .primary:     return KonoColor.textOnBrown
        case .brand:       return KonoColor.textOnYellow
        case .secondary:   return KonoColor.brown
        case .ghost:       return KonoColor.brown
        case .destructive: return KonoColor.danger
        }
    }

    private var border: Color {
        switch kind {
        case .secondary: return KonoColor.brown.opacity(0.15)
        case .ghost:     return KonoColor.brown.opacity(0.2)
        default:         return .clear
        }
    }
}

/// Small pill button for inline actions ("عرض الكل", "تعديل").
struct KonoPillButton: View {
    let title: String
    var icon: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Text(title.k).font(KonoFont.subheadline)
                if let icon { Image(systemName: icon).font(.system(size: 11, weight: .bold)) }
            }
            .foregroundStyle(KonoColor.brown)
            .padding(.horizontal, KonoSpace.m)
            .padding(.vertical, KonoSpace.s)
            .background(KonoColor.brown.opacity(0.07), in: Capsule())
        }
        .buttonStyle(.plain)
    }
}
