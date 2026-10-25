import SwiftUI

/// Little radial particle burst — overlay on a favourite button when it turns on.
struct HeartBurst: View {
    let active: Bool
    var color: Color = KonoColor.danger

    @State private var fire = false

    var body: some View {
        ZStack {
            ForEach(0..<8, id: \.self) { i in
                Circle()
                    .fill(color)
                    .frame(width: 5, height: 5)
                    .offset(y: fire ? -22 : 0)
                    .rotationEffect(.degrees(Double(i) / 8 * 360))
                    .opacity(fire ? 0 : 1)
                    .scaleEffect(fire ? 0.2 : 1)
            }
        }
        .onChange(of: active) { _, on in
            guard on else { return }
            fire = false
            withAnimation(.easeOut(duration: 0.5)) { fire = true }
        }
        .allowsHitTesting(false)
    }
}

extension View {
    func heartBurst(_ active: Bool, color: Color = KonoColor.danger) -> some View {
        overlay(HeartBurst(active: active, color: color))
    }
}
