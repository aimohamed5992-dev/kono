import SwiftUI

struct OrdersListView: View {
    @Environment(\.cart) private var cart
    @State private var path = NavigationPath()
    @State private var tab = 0
    @State private var activeOrders: [Order] = []
    @State private var pastOrders: [Order] = []
    @State private var loaded = false

    private var localActive: Order? {
        guard !cart.isEmpty, let store = cart.store else { return nil }
        return Order(shortCode: "KON-4821", store: store, lines: cart.lines,
                     stage: .onTheWay, placedAt: Date().addingTimeInterval(-900),
                     address: Mock.customer.addresses.first ?? Mock.addresses[0],
                     courier: Mock.courier, paymentLabelAr: "Apple Pay", scheduledForAr: nil)
    }

    private var active: [Order] {
        if !activeOrders.isEmpty { return activeOrders }
        return localActive.map { [$0] } ?? []
    }
    private var past: [Order] {
        loaded ? pastOrders : Mock.pastOrders
    }

    private func load() async {
        activeOrders = await AppData.shared.myOrders(status: "active")
        pastOrders = await AppData.shared.myOrders(status: "past")
        loaded = true
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: KonoSpace.m) {
                Picker("", selection: $tab) {
                    Text("الحالية").tag(0)
                    Text("السابقة").tag(1)
                }
                .pickerStyle(.segmented)
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)

                ScrollView {
                    LazyVStack(spacing: KonoSpace.m) {
                        if tab == 0 {
                            if active.isEmpty {
                                KonoEmptyState(illustration: "illo_success", symbol: "bag",
                                               title: "ما عندك طلبات حالية",
                                               message: "أطلب من أي متجر وتابع طلبك من هنا.")
                                    .padding(.top, KonoSpace.xxl)
                            } else {
                                ForEach(active) { order in
                                    Button { path.append(order) } label: { ActiveOrderCard(order: order) }
                                        .buttonStyle(.plain)
                                        .konoScreenPadding()
                                }
                            }
                        } else {
                            ForEach(past) { order in
                                Button { path.append(order) } label: { PastOrderRow(order: order) }
                                    .buttonStyle(.plain)
                                    .konoScreenPadding()
                            }
                        }
                    }
                    .padding(.bottom, 120)
                }
                .scrollIndicators(.hidden)
                .refreshable { await load() }
            }
            .background(KonoColor.background)
            .navigationTitle("طلباتي")
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: Order.self) { OrderTrackingView(order: $0) }
            .task { await load() }
        }
    }
}

struct ActiveOrderCard: View {
    let order: Order
    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            HStack(spacing: KonoSpace.s) {
                LogoBubble(asset: order.store.logoAsset, symbol: order.store.kind.systemIcon, size: 40, name: order.store.name)
                VStack(alignment: .leading, spacing: 2) {
                    Text(order.store.name).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    Text("\(order.itemCount) صنف · \(order.shortCode)")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
                KonoTag(text: "جارٍ", systemImage: "circle.fill", color: KonoColor.success, filled: true)
            }
            HStack(spacing: KonoSpace.s) {
                Image(systemName: order.stage.icon).font(.system(size: 13))
                    .foregroundStyle(KonoColor.brown)
                Text(order.stage.titleAr.k).font(KonoFont.subheadline).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                Text("\(order.store.etaMinutes) دقيقة").font(KonoFont.footnote)
                    .foregroundStyle(KonoColor.textSecondary)
            }
            StageProgressBar(stage: order.stage)
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.l)
        .konoShadow(.sm)
    }
}

struct PastOrderRow: View {
    let order: Order
    var body: some View {
        HStack(spacing: KonoSpace.m) {
            AssetImage(name: order.store.heroAsset, fallbackSymbol: order.store.kind.systemIcon,
                       fallbackTint: order.store.kind.accent)
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(order.store.name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                Text("\(order.itemCount) صنف · \(order.placedAt.formatted(.dateTime.day().month()))")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                RiyalText(amount: order.total, font: KonoFont.subheadline)
            }
            Spacer()
            VStack(spacing: KonoSpace.xs) {
                KonoTag(text: "تم التوصيل", color: KonoColor.success)
                Text("إعادة الطلب").font(KonoFont.caption).foregroundStyle(KonoColor.brown)
                    .padding(.horizontal, KonoSpace.s).padding(.vertical, 4)
                    .background(KonoColor.brown.opacity(0.08), in: Capsule())
            }
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }
}
