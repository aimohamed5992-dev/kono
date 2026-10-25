import SwiftUI

struct MerchantDashboardView: View {
    @Environment(\.merchant) private var session
    @State private var path = NavigationPath()
    @State private var filter: TicketState = .incoming

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                header

                ScrollView {
                    LazyVStack(spacing: KonoSpace.m) {
                        statsStrip
                            .padding(.top, KonoSpace.s)

                        segmented

                        let list = section(for: filter)
                        if list.isEmpty {
                            KonoEmptyState(illustration: "illo_success", symbol: "tray",
                                           title: "ما في طلبات هنا",
                                           message: "الطلبات الجديدة بتظهر هنا مباشرة.")
                                .padding(.top, KonoSpace.xl)
                        } else {
                            ForEach(list) { ticket in
                                TicketCard(ticket: ticket,
                                           onOpen: { path.append(MerchantRoute.ticket(ticket)) },
                                           onAccept: { withAnimation(KonoMotion.spring) { session.advance(ticket) } },
                                           onReject: { withAnimation(KonoMotion.spring) { session.reject(ticket) } },
                                           onNext: { withAnimation(KonoMotion.spring) { session.advance(ticket) } })
                            }
                        }
                    }
                    .konoScreenPadding()
                    .padding(.bottom, 120)
                }
                .scrollIndicators(.hidden)
            }
            .background(KonoColor.background)
            .navigationBarHidden(true)
            .navigationDestination(for: MerchantRoute.self) { route in
                switch route {
                case .ticket(let t):   TicketDetailView(ticket: t)
                case .menuItem(let m): MenuItemEditView(item: m)
                }
            }
            .task {
                await session.bootstrap()
                if RootFlow.launch.screen == "ticket", path.isEmpty,
                   let t = session.tickets.first(where: { $0.apiId != 0 }) {
                    path.append(MerchantRoute.ticket(t))
                }
            }
            .refreshable { await session.refresh() }
        }
    }

    private var header: some View {
        HStack(spacing: KonoSpace.m) {
            KonoMark(size: 40)
            VStack(alignment: .leading, spacing: 1) {
                Text(session.profile.storeName).font(KonoFont.headline)
                    .foregroundStyle(KonoColor.textPrimary).lineLimit(1)
                Text(session.isOnline ? "مفتوح ويستقبل الطلبات" : "مغلق مؤقتاً")
                    .font(KonoFont.footnote)
                    .foregroundStyle(session.isOnline ? KonoColor.success : KonoColor.danger)
            }
            Spacer()
            Toggle("", isOn: Binding(get: { session.isOnline },
                                     set: { v in withAnimation(KonoMotion.snappy) { session.setOnline(v) } }))
                .labelsHidden()
                .tint(KonoColor.success)
        }
        .konoScreenPadding()
        .padding(.vertical, KonoSpace.m)
        .background(KonoColor.surface.konoShadow(.sm))
    }

    private var statsStrip: some View {
        HStack(spacing: KonoSpace.s) {
            statTile("مبيعات اليوم", "\(session.todayRevenue.clean) ر.س", "banknote.fill", KonoColor.success)
            statTile("طلبات اليوم", "\(session.todayOrders)", "bag.fill", KonoColor.brown)
            statTile("متوسط التجهيز", "\(session.prepMinutes) د", "timer", KonoColor.caramel)
        }
    }

    private func statTile(_ t: String, _ v: String, _ icon: String, _ tint: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Image(systemName: icon).font(.system(size: 14)).foregroundStyle(tint)
            Text(v).font(.cairo(16, .heavy))
                .foregroundStyle(KonoColor.textPrimary).minimumScaleFactor(0.7).lineLimit(1)
            Text(t).font(.cairo(10, .medium))
                .foregroundStyle(KonoColor.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }

    private var segmented: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: KonoSpace.s) {
                chip(.incoming, "جديدة", session.newCount)
                chip(.preparing, "قيد التجهيز", session.tickets(in: .preparing).count)
                chip(.ready, "جاهزة", session.tickets(in: .ready).count)
                chip(.handedOver, "تم التسليم", session.tickets(in: .handedOver).count)
            }
        }
    }

    private func chip(_ state: TicketState, _ label: String, _ count: Int) -> some View {
        Button {
            withAnimation(KonoMotion.snappy) { filter = state }
        } label: {
            HStack(spacing: 5) {
                Text(label).font(KonoFont.subheadline)
                if count > 0 {
                    Text("\(count)").font(.cairo(11, .black))
                        .foregroundStyle(filter == state ? KonoColor.brown : KonoColor.textOnBrown)
                        .frame(width: 18, height: 18)
                        .background(filter == state ? KonoColor.yellow : KonoColor.brown, in: Circle())
                }
            }
            .foregroundStyle(filter == state ? KonoColor.textOnBrown : KonoColor.brown)
            .padding(.horizontal, KonoSpace.m)
            .frame(height: KonoSize.chipHeight)
            .background(filter == state ? KonoColor.brown : KonoColor.surface, in: Capsule())
            .overlay(Capsule().strokeBorder(filter == state ? .clear : KonoColor.brown.opacity(0.16), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func section(for state: TicketState) -> [MerchantTicket] {
        session.tickets(in: state)
    }
}
