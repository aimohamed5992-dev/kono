import SwiftUI

struct CheckoutView: View {
    @Environment(\.cart) private var cart
    @Environment(\.dismiss) private var dismiss
    @State private var data = AppData.shared

    @State private var address: Address = Mock.customer.addresses[0]
    @State private var payment: PaymentMethod = Mock.payments[0]
    @State private var timing: Timing = .asap
    @State private var tip: Double = 0
    @State private var placing = false
    @State private var placedOrder: Order?

    private var addresses: [Address] { data.addresses.isEmpty ? Mock.customer.addresses : data.addresses }
    private var payments: [PaymentMethod] { data.payments.isEmpty ? Mock.payments : data.payments }

    enum Timing: Hashable { case asap, scheduled(String) }
    private let slots = ["خلال ٣٠–٤٥ دقيقة", "٨:٣٠ م", "٩:٠٠ م", "٩:٣٠ م"]
    private let tips: [Double] = [0, 5, 10, 15]

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    Menu {
                        ForEach(addresses) { a in
                            Button {
                                address = a
                                Task { await data.selectAddress(a) }
                            } label: { Label("\(a.labelAr) — \(a.lineAr)", systemImage: a.icon) }
                        }
                    } label: {
                        sectionCard(title: "التوصيل إلى", icon: address.icon) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(address.labelAr.k).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                                Text(address.lineAr.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                                    .lineLimit(1)
                            }
                        }
                    }

                    // timing
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("وقت التوصيل").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        HStack(spacing: KonoSpace.s) {
                            KonoChip(title: "في أسرع وقت", icon: "bolt.fill",
                                     selected: timing == .asap) { timing = .asap }
                            Menu {
                                ForEach(slots.dropFirst(), id: \.self) { s in
                                    Button(s) { timing = .scheduled(s) }
                                }
                            } label: {
                                KonoChip(title: scheduledLabel, icon: "calendar",
                                         selected: { if case .scheduled = timing { return true }; return false }())
                            }
                        }
                    }

                    // payment
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("طريقة الدفع").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        ForEach(payments) { pm in
                            Button {
                                #if canImport(UIKit)
                                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                #endif
                                withAnimation(KonoMotion.snappy) { payment = pm }
                            } label: {
                                HStack(spacing: KonoSpace.m) {
                                    Image(systemName: pm.icon).font(.system(size: 15))
                                        .foregroundStyle(KonoColor.brown).frame(width: 26)
                                    Text(pm.labelAr.k).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                                    if let d = pm.detailAr {
                                        Text(d).font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                                    }
                                    Spacer()
                                    Image(systemName: payment.id == pm.id ? "largecircle.fill.circle" : "circle")
                                        .foregroundStyle(payment.id == pm.id ? KonoColor.brown : KonoColor.textTertiary)
                                }
                                .padding(KonoSpace.m)
                                .background(payment.id == pm.id ? KonoColor.cream : KonoColor.surface)
                                .konoCardShape(KonoRadius.s)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // tip
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("إكرامية المندوب").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        HStack(spacing: KonoSpace.s) {
                            ForEach(tips, id: \.self) { t in
                                KonoChip(title: t == 0 ? "لا شكراً" : "\(t.clean) ر.س",
                                         selected: tip == t) {
                                    withAnimation(KonoMotion.snappy) { tip = t }
                                }
                            }
                        }
                    }

                    PriceBreakdown(subtotal: cart.subtotal, delivery: cart.deliveryFee,
                                   service: cart.serviceFee + tip, discount: cart.discount,
                                   total: cart.total + tip)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .scrollIndicators(.hidden)

            KonoButton(title: "تأكيد الطلب · \((cart.total + tip).clean) ر.س", loading: placing) {
                placeOrder()
            }
            .konoScreenPadding()
            .padding(.vertical, KonoSpace.m)
            .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
        .background(KonoColor.background)
        .navigationTitle("الدفع")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .navigationDestination(item: $placedOrder) { order in
            OrderTrackingView(order: order, justPlaced: true)
        }
        .task {
            await data.loadAccount()
            if let cur = data.currentAddress ?? addresses.first { address = cur }
            if let def = payments.first(where: { $0.isDefault }) ?? payments.first { payment = def }
        }
    }

    private var scheduledLabel: String {
        if case .scheduled(let s) = timing { return s }
        return "جدولة لاحقاً"
    }

    private func sectionCard<C: View>(title: String, icon: String,
                                     @ViewBuilder content: () -> C) -> some View {
        HStack(spacing: KonoSpace.m) {
            Image(systemName: icon).font(.system(size: 16, weight: .bold))
                .foregroundStyle(KonoColor.brown)
                .frame(width: 40, height: 40)
                .background(KonoColor.cream, in: RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                content()
            }
            Spacer()
            Image(systemName: "chevron.left").font(.system(size: 12, weight: .bold))
                .foregroundStyle(KonoColor.textTertiary)
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }

    @State private var placeError: String?

    private func placeOrder() {
        placing = true
        placeError = nil
        Task {
            defer { placing = false }
            do {
                let order = try await AppData.shared.placeOrder(
                    cart: cart, address: address, payment: payment.labelAr,
                    tip: tip, scheduledFor: timing == .asap ? nil : scheduledLabel)
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
                placedOrder = order
            } catch {
                // offline / not wired → fall back to a local order so the demo flows
                placedOrder = Order(
                    shortCode: "KON-\(Int.random(in: 4000...4999))",
                    store: cart.store ?? Mock.stores[0],
                    lines: cart.lines, stage: .placed, placedAt: Date(),
                    address: address, courier: Mock.courier,
                    paymentLabelAr: payment.labelAr, scheduledForAr: scheduledLabel)
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
            }
        }
    }
}
