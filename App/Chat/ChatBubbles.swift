import SwiftUI
import MapKit

struct ChatBubble: View {
    let msg: ChatMessageDTO
    var onTapImage: (String) -> Void = { _ in }

    private var mine: Bool { msg.mine }
    private var bg: Color { mine ? KonoColor.yellow : KonoColor.surface }
    private var fg: Color { mine ? KonoColor.textOnYellow : KonoColor.textPrimary }

    var body: some View {
        HStack {
            content
                .padding(.horizontal, msg.kind == "text" ? 12 : 5)
                .padding(.vertical, msg.kind == "text" ? 9 : 5)
                .background(bg)
                .clipShape(BubbleShape(mine: mine))
                .overlay(BubbleShape(mine: mine).strokeBorder(KonoColor.hairline, lineWidth: mine ? 0 : 1))
                .frame(maxWidth: 290, alignment: mine ? .trailing : .leading)
        }
        .frame(maxWidth: .infinity, alignment: mine ? .leading : .trailing)
        .padding(mine ? .trailing : .leading, 44)
        .padding(mine ? .leading : .trailing, 4)
    }

    @ViewBuilder
    private var content: some View {
        switch msg.kind {
        case "image":  imageBubble
        case "voice":  VoiceBubble(msg: msg, mine: mine)
        case "location": locationBubble
        case "system": Text(msg.text).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
        default:       textBubble
        }
    }

    private var textBubble: some View {
        VStack(alignment: .trailing, spacing: 2) {
            Text(msg.text)
                .font(KonoFont.body)
                .foregroundStyle(fg)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            meta
        }
    }

    private var imageBubble: some View {
        VStack(alignment: .trailing, spacing: 3) {
            AssetImage(name: msg.mediaUrl, fallbackSymbol: "photo")
                .frame(width: 220, height: 220)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .onTapGesture { onTapImage(msg.mediaUrl) }
            meta.padding(.horizontal, 6).padding(.bottom, 3)
        }
    }

    private var locationBubble: some View {
        VStack(alignment: .trailing, spacing: 3) {
            ChatLocationSnapshot(lat: msg.lat, lng: msg.lng)
                .frame(width: 220, height: 140)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .onTapGesture {
                    let u = URL(string: "http://maps.apple.com/?ll=\(msg.lat),\(msg.lng)&q=الموقع")
                    if let u { UIApplication.shared.open(u) }
                }
            HStack(spacing: 4) {
                Image(systemName: "location.fill").font(.system(size: 10))
                Text("موقع مُشارَك").font(KonoFont.caption)
            }
            .foregroundStyle(fg.opacity(0.8))
            meta.padding(.horizontal, 6).padding(.bottom, 3)
        }
    }

    private var meta: some View {
        HStack(spacing: 3) {
            Text(msg.createdAt.formatted(date: .omitted, time: .shortened))
                .font(.system(size: 10))
                .foregroundStyle(fg.opacity(0.55))
            if mine {
                Image(systemName: msg.read ? "checkmark.circle.fill" : "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(msg.read ? KonoColor.info : fg.opacity(0.5))
            }
        }
    }
}

private struct BubbleShape: InsettableShape {
    var mine: Bool
    var inset: CGFloat = 0

    func inset(by amount: CGFloat) -> some InsettableShape {
        var c = self; c.inset += amount; return c
    }

    func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: inset, dy: inset)
        let big: CGFloat = 16, small: CGFloat = 5
        // in RTL, "mine" sits on the reading-start (right) edge
        let tl = mine ? big : big, tr = mine ? big : big
        let bl = mine ? big : small, br = mine ? small : big
        return Path(roundedRect: r, cornerRadii: .init(topLeading: tl, bottomLeading: bl,
                                                       bottomTrailing: br, topTrailing: tr))
    }
}

struct VoiceBubble: View {
    let msg: ChatMessageDTO
    let mine: Bool
    @State private var player = VoicePlayer.shared

    private var isThis: Bool { player.playingURL == msg.mediaUrl }
    private var tint: Color { mine ? KonoColor.textOnYellow : KonoColor.brown }

    var body: some View {
        HStack(spacing: 9) {
            Button { player.toggle(msg.mediaUrl) } label: {
                Image(systemName: isThis ? "pause.fill" : "play.fill")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(mine ? KonoColor.yellow : KonoColor.textOnBrown)
                    .frame(width: 32, height: 32)
                    .background(tint, in: Circle())
            }
            .buttonStyle(.plain)

            VStack(alignment: .leading, spacing: 4) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule().fill(tint.opacity(0.25)).frame(height: 3)
                        Capsule().fill(tint)
                            .frame(width: max(3, geo.size.width * (isThis ? player.progress : 0)), height: 3)
                    }
                }
                .frame(height: 3)
                Text(mmss(msg.durationSec))
                    .font(.system(size: 10)).foregroundStyle(tint.opacity(0.8))
            }
            .frame(width: 120)

            if mine {
                Image(systemName: msg.read ? "checkmark.circle.fill" : "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(msg.read ? KonoColor.info : tint.opacity(0.5))
            }
        }
        .padding(.horizontal, 8).padding(.vertical, 5)
    }
}

struct ChatLocationSnapshot: View {
    let lat: Double
    let lng: Double

    var body: some View {
        Map(initialPosition: .region(MKCoordinateRegion(
            center: .init(latitude: lat, longitude: lng),
            span: .init(latitudeDelta: 0.008, longitudeDelta: 0.008)))) {
            Marker("", coordinate: .init(latitude: lat, longitude: lng))
                .tint(KonoColor.brown)
        }
        .disabled(true)
        .allowsHitTesting(false)
    }
}

struct ChatDateChip: View {
    let date: Date
    var body: some View {
        Text(label)
            .font(KonoFont.caption)
            .foregroundStyle(KonoColor.textSecondary)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(KonoColor.surfaceSunken, in: Capsule())
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
    }
    private var label: String {
        if Calendar.current.isDateInToday(date) { return "اليوم" }
        if Calendar.current.isDateInYesterday(date) { return "أمس" }
        return date.formatted(.dateTime.day().month().year())
    }
}
