import SwiftUI

struct CartView: View {
    @Environment(\.cart) private var cart
    @Environment(\.dismiss) private var dismiss
    @State private var promoCode = ""
    @State private var goCheckout = false

    var body: some View {
        Group {
            if cart.isEmpty {
                KonoEmptyState(illustration: "illo_success", symbol: "bag",
                               title: "سلتك فاضية",
                               message: "أضف أصناف من أي متجر وتلقاها هنا.",
                               actionTitle: "تصفّح المتاجر") { dismiss() }
            } else {
                content
            }
        }
        .background(KonoColor.background)
        .navigationTitle("السلة")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .navigationDestination(isPresented: $goCheckout) { CheckoutView() }
    }

    private var content: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    if let store = cart.store {
                        HStack(spacing: KonoSpace.s) {
                            LogoBubble(asset: store.logoAsset, symbol: store.kind.systemIcon, size: 36, name: store.name)
                            Text(store.name).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            Text("\(store.etaMinutes) دقيقة").font(KonoFont.footnote)
                                .foregroundStyle(KonoColor.textSecondary)
                        }
                    }

                    VStack(spacing: KonoSpace.s) {
                        ForEach(cart.lines) { line in
                            CartLineRow(line: line) { q in
                                withAnimation(KonoMotion.snappy) { cart.setQuantity(q, for: line) }
                            }
                        }
                    }

                    // promo
                    HStack(spacing: KonoSpace.s) {
                        Image(systemName: "ticket.fill").foregroundStyle(KonoColor.caramel)
                        TextField("رمز الخصم", text: $promoCode)
                            .font(KonoFont.body)
                        Button(cart.promoApplied ? "مطبّق" : "تطبيق") {
                            withAnimation(KonoMotion.spring) { cart.promoApplied = true }
                            #if canImport(UIKit)
                            UINotificationFeedbackGenerator().notificationOccurred(.success)
                            #endif
                        }
                        .font(KonoFont.subheadline)
                        .foregroundStyle(cart.promoApplied ? KonoColor.success : KonoColor.brown)
                    }
                    .padding(KonoSpace.m)
                    .background(KonoColor.surface)
                    .konoCardShape(KonoRadius.m)

                    PriceBreakdown(subtotal: cart.subtotal, delivery: cart.deliveryFee,
                                   service: cart.serviceFee, discount: cart.discount, total: cart.total)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .scrollIndicators(.hidden)

            KonoButton(title: "متابعة الدفع · \(cart.total.clean) ر.س") { goCheckout = true }
                .konoScreenPadding()
                .padding(.vertical, KonoSpace.m)
                .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
    }
}

struct CartLineRow: View {
    let line: CartLine
    var onQuantity: (Int) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: KonoSpace.m) {
            AssetImage(name: line.product.imageAsset,
                       fallbackSymbol: "takeoutbag.and.cup.and.straw.fill",
                       fallbackTint: KonoColor.caramel)
                .frame(width: 64, height: 64)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(line.product.nameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                if !line.selectedOptionsAr.isEmpty {
                    Text(line.selectedOptionsAr.joined(separator: " · "))
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary).lineLimit(2)
                }
                if !line.note.isEmpty {
                    Text("ملاحظة: \(line.note)").font(KonoFont.footnote)
                        .foregroundStyle(KonoColor.textTertiary).lineLimit(1)
                }
                RiyalText(amount: line.lineTotal, font: KonoFont.price)
            }
            Spacer(minLength: 0)
            QuantityStepper(quantity: Binding(
                get: { line.quantity },
                set: { onQuantity($0) }), minValue: 0, compact: true)
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }
}

struct PriceBreakdown: View {
    let subtotal: Double
    let delivery: Double
    let service: Double
    let discount: Double
    let total: Double

    var body: some View {
        VStack(spacing: KonoSpace.s) {
            row("المجموع الفرعي", subtotal)
            row("رسوم التوصيل", delivery, free: delivery == 0)
            row("رسوم الخدمة", service)
            if discount > 0 { row("الخصم", -discount, highlight: true) }
            Divider()
            HStack {
                Text("الإجمالي").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                RiyalText(amount: total, font: KonoFont.priceLarge)
            }
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }

    private func row(_ label: String, _ amount: Double, free: Bool = false, highlight: Bool = false) -> some View {
        HStack {
            Text(label).font(KonoFont.callout).foregroundStyle(KonoColor.textSecondary)
            Spacer()
            if free {
                Text("مجاني").font(KonoFont.subheadline).foregroundStyle(KonoColor.success)
            } else {
                RiyalText(amount: amount, font: KonoFont.subheadline,
                          color: highlight ? KonoColor.success : KonoColor.textPrimary)
            }
        }
    }
}
