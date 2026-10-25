import SwiftUI

struct TicketCard: View {
    let ticket: MerchantTicket
    var onOpen: () -> Void
    var onAccept: () -> Void
    var onReject: () -> Void
    var onNext: () -> Void

    @State private var now = Date()
    private let tick = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private var secondsLeft: Int {
        max(0, Int(ticket.acceptDeadline.timeIntervalSince(now)))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: KonoSpace.s) {
                        Text(ticket.shortCode).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        KonoTag(text: ticket.isDelivery ? "توصيل" : "استلام",
                                systemImage: ticket.isDelivery ? "bicycle" : "bag",
                                color: KonoColor.brown)
                    }
                    Text("\(ticket.customerNameAr) · \(ticket.itemCount) صنف · قبل \(minutesAgo) د")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
                RiyalText(amount: ticket.total, font: KonoFont.price)
            }

            if ticket.state == .incoming {
                // countdown ring
                HStack(spacing: KonoSpace.s) {
                    Image(systemName: "timer").font(.system(size: 12)).foregroundStyle(KonoColor.danger)
                    Text("قبول خلال \(secondsLeft) ثانية").font(KonoFont.subheadline)
                        .foregroundStyle(KonoColor.danger)
                    Spacer()
                }
                ProgressView(value: Double(secondsLeft), total: 60)
                    .tint(secondsLeft < 15 ? KonoColor.danger : KonoColor.caramel)
            }

            VStack(alignment: .leading, spacing: 4) {
                ForEach(ticket.lines.prefix(3)) { line in
                    HStack(spacing: KonoSpace.s) {
                        Text("\(line.quantity)×").font(KonoFont.subheadline).foregroundStyle(KonoColor.brown)
                        Text(line.product.nameAr).font(KonoFont.callout).foregroundStyle(KonoColor.textPrimary)
                            .lineLimit(1)
                        Spacer()
                    }
                }
                if ticket.lines.count > 3 {
                    Text("+ \(ticket.lines.count - 3) أصناف أخرى").font(KonoFont.footnote)
                        .foregroundStyle(KonoColor.textTertiary)
                }
            }

            if let note = ticket.noteAr {
                HStack(spacing: KonoSpace.s) {
                    Image(systemName: "exclamationmark.bubble.fill").font(.system(size: 11))
                    Text(note).font(KonoFont.footnote)
                }
                .foregroundStyle(KonoColor.caramel)
                .padding(KonoSpace.s)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(KonoColor.caramel.opacity(0.1), in: RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            }

            actions
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.l)
        .konoShadow(.sm)
        .overlay(alignment: .top) {
            if ticket.state == .incoming {
                Capsule().fill(KonoColor.danger).frame(width: 44, height: 4).offset(y: -2)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .onReceive(tick) { now = $0 }
    }

    private var minutesAgo: Int { max(1, Int(Date().timeIntervalSince(ticket.placedAt) / 60)) }

    @ViewBuilder private var actions: some View {
        switch ticket.state {
        case .incoming:
            HStack(spacing: KonoSpace.s) {
                KonoButton(title: "رفض", kind: .destructive, action: onReject)
                KonoButton(title: "قبول الطلب", kind: .primary, action: onAccept)
            }
        case .preparing:
            KonoButton(title: "الطلب جاهز", icon: "checkmark", kind: .brand, action: onNext)
        case .ready:
            KonoButton(title: ticket.isDelivery ? "تسليم للمندوب" : "تم الاستلام",
                       icon: "bag.fill", kind: .primary, action: onNext)
        case .handedOver, .completed:
            HStack(spacing: KonoSpace.s) {
                Image(systemName: "checkmark.seal.fill").foregroundStyle(KonoColor.success)
                Text("اكتمل").font(KonoFont.subheadline).foregroundStyle(KonoColor.success)
                Spacer()
                if let c = ticket.courierNameAr {
                    Text("المندوب: \(c)").font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
            }
        }
    }
}
