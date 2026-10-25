import SwiftUI

struct TicketDetailView: View {
    let ticket: MerchantTicket
    @Environment(\.merchant) private var session
    @Environment(\.dismiss) private var dismiss

    private var current: MerchantTicket {
        session.tickets.first { $0.id == ticket.id } ?? ticket
    }

    @State private var detail: OrderDetailDTO?

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                if let d = detail {
                    OrderDetailSections(d: d, audience: .merchant)
                        .padding(.bottom, 120)
                } else if current.apiId != 0 {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 80)
                } else {
                    fallbackContent
                }
            }
            .scrollIndicators(.hidden)

            bottomAction
        }
        .background(KonoColor.background)
        .navigationTitle(current.shortCode)
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .task(id: current.apiId) {
            guard current.apiId != 0 else { return }
            detail = await AppData.shared.merchantOrderDetail(orderId: current.apiId)
        }
    }

    private var fallbackContent: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(current.shortCode).font(KonoFont.title).foregroundStyle(KonoColor.textPrimary)
                    Text(current.placedAt.formatted(.dateTime.hour().minute()))
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
                KonoTag(text: current.state.titleAr,
                        color: current.state == .incoming ? KonoColor.danger : KonoColor.caramel,
                        filled: true)
            }
            card("العميل", icon: "person.fill") {
                Text(current.customerNameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
            }
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                Text("الأصناف").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                ForEach(current.lines) { line in
                    HStack(alignment: .top, spacing: KonoSpace.s) {
                        Text("\(line.quantity)×").font(KonoFont.bodyMedium).foregroundStyle(KonoColor.brown)
                        Text(line.product.nameAr).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                        Spacer()
                        RiyalText(amount: line.lineTotal, font: KonoFont.subheadline)
                    }
                    .padding(.vertical, 6)
                    Divider()
                }
                HStack {
                    Text("الإجمالي").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    Spacer()
                    RiyalText(amount: current.total, font: KonoFont.priceLarge)
                }
            }
            .padding(KonoSpace.l).background(KonoColor.surface).konoCardShape(KonoRadius.m)
        }
        .konoScreenPadding().padding(.top, KonoSpace.s).padding(.bottom, 120)
    }

    private func card<C: View>(_ title: String, icon: String, tint: Color = KonoColor.brown,
                               @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            Label(LocalizedStringKey(title), systemImage: icon).font(KonoFont.subheadline).foregroundStyle(tint)
            content()
        }
        .padding(KonoSpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }

    @ViewBuilder private var bottomAction: some View {
        let s = current.state
        VStack {
            switch s {
            case .incoming:
                HStack(spacing: KonoSpace.s) {
                    KonoButton(title: "رفض", kind: .destructive) {
                        session.reject(current); dismiss()
                    }
                    KonoButton(title: "قبول") { withAnimation { session.advance(current) } }
                }
            case .preparing:
                KonoButton(title: "الطلب جاهز", icon: "checkmark", kind: .brand) {
                    withAnimation { session.advance(current) }
                }
            case .ready:
                KonoButton(title: current.isDelivery ? "تسليم للمندوب" : "تم الاستلام", kind: .primary) {
                    withAnimation { session.advance(current) }
                }
            default:
                KonoButton(title: "تم", kind: .secondary) { dismiss() }
            }
        }
        .konoScreenPadding()
        .padding(.vertical, KonoSpace.m)
        .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
    }
}
