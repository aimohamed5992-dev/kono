import SwiftUI

struct KonoTextField: View {
    let placeholder: String
    @Binding var text: String
    var icon: String? = nil
    var keyboard: UIKeyboardType = .default
    var isSecure: Bool = false
    var submitLabel: SubmitLabel = .done
    var onSubmit: () -> Void = {}

    @FocusState private var focused: Bool
    @State private var reveal = false

    var body: some View {
        HStack(spacing: KonoSpace.s) {
            if let icon {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(focused ? KonoColor.brown : KonoColor.textTertiary)
            }
            Group {
                if isSecure && !reveal {
                    SecureField(LocalizedStringKey(placeholder), text: $text)
                } else {
                    TextField(LocalizedStringKey(placeholder), text: $text)
                }
            }
            .font(KonoFont.body)
            .foregroundStyle(KonoColor.textPrimary)
            .keyboardType(keyboard)
            .focused($focused)
            .submitLabel(submitLabel)
            .onSubmit(onSubmit)

            if isSecure {
                Button { reveal.toggle() } label: {
                    Image(systemName: reveal ? "eye.slash.fill" : "eye.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(KonoColor.textTertiary)
                }
                .buttonStyle(.plain)
            } else if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 15))
                        .foregroundStyle(KonoColor.textTertiary)
                }
                .buttonStyle(.plain)
                .transition(.opacity)
            }
        }
        .padding(.horizontal, KonoSpace.l)
        .frame(height: KonoSize.fieldHeight)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                .strokeBorder(focused ? KonoColor.brown : KonoColor.brown.opacity(0.14),
                              lineWidth: focused ? 1.5 : 1)
        )
        .animation(KonoMotion.ease, value: focused)
        .animation(KonoMotion.ease, value: text.isEmpty)
    }
}

/// Search bar used across discovery screens.
struct KonoSearchBar: View {
    var placeholder: String = "ابحث عن مطعم أو صنف"
    @Binding var text: String
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: KonoSpace.s) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(KonoColor.brown.opacity(0.7))
            if let onTap {
                Text(LocalizedStringKey(placeholder))
                    .font(KonoFont.body)
                    .foregroundStyle(KonoColor.textTertiary)
                Spacer()
                Button(action: onTap) { Color.clear }.buttonStyle(.plain)
            } else {
                TextField(LocalizedStringKey(placeholder), text: $text)
                    .font(KonoFont.body)
            }
        }
        .padding(.horizontal, KonoSpace.l)
        .frame(height: 48)
        .background(KonoColor.surface, in: Capsule())
        .overlay(Capsule().strokeBorder(KonoColor.brown.opacity(0.1), lineWidth: 1))
        .konoShadow(.sm)
        .contentShape(Capsule())
        .onTapGesture { onTap?() }
    }
}
