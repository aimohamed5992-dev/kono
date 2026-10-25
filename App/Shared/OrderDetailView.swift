import SwiftUI
import MapKit

enum OrderAudience { case merchant, courier }

/// Full order detail — every field about the customer, order, store, courier,
/// money and timeline. Shared by the merchant ticket screen and the courier job
/// screen (the admin dashboard renders the same data on the web).
struct OrderDetailView: View {
    let orderId: Int
    let audience: OrderAudience

    @State private var detail: OrderDetailDTO?
    @State private var loading = true

    var body: some View {
        ScrollView {
            if let d = detail {
                OrderDetailSections(d: d, audience: audience)
                    .padding(.bottom, 100)
            } else if loading {
                ProgressView().frame(maxWidth: .infinity).padding(.top, 80)
            } else {
                Text("تعذّر تحميل تفاصيل الطلب")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    .frame(maxWidth: .infinity).padding(.top, 80)
            }
        }
        .background(KonoColor.background)
        .navigationTitle(detail?.shortCode ?? "الطلب")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .refreshable { await load() }
    }

    private func load() async {
        detail = audience == .merchant
            ? await AppData.shared.merchantOrderDetail(orderId: orderId)
            : await AppData.shared.courierOrderDetail(orderId: orderId)
        loading = false
    }
}

/// The card stack — reused inside the merchant ticket screen (which adds its own
/// accept / advance action bar) and the standalone courier job screen.
struct OrderDetailSections: View {
    let d: OrderDetailDTO
    let audience: OrderAudience

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            headerRow(d)
            customerCard(d)
            addressCard(d)
            if audience == .courier { storeCard(d) }
            itemsCard(d)
            totalsCard(d)
            paymentCard(d)
            if let note = d.noteAr, !note.isEmpty { noteCard(note) }
            timelineCard(d)
        }
        .konoScreenPadding()
        .padding(.vertical, KonoSpace.m)
    }

    // MARK: sections

    private func headerRow(_ d: OrderDetailDTO) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text(d.shortCode).font(KonoFont.title).foregroundStyle(KonoColor.textPrimary)
                Text(d.placedAt.formatted(.dateTime.day().month().hour().minute()))
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            }
            Spacer()
            KonoTag(text: d.stageAr, color: stageColor(d.stage), filled: true)
        }
    }

    private func customerCard(_ d: OrderDetailDTO) -> some View {
        card("العميل", icon: "person.fill") {
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                Text(d.customerNameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                HStack(spacing: KonoSpace.s) {
                    Text(d.customerPhone).font(KonoFont.footnote.monospacedDigit())
                        .foregroundStyle(KonoColor.textSecondary)
                    Spacer()
                    contactButton("phone.fill", "tel:\(digits(d.customerPhone))")
                    contactButton("message.fill", "sms:\(digits(d.customerPhone))")
                }
                Text(d.isDelivery ? "توصيل عبر مندوب كونو" : "استلام من الفرع")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                OrderChatButtons(orderId: d.id, compact: true)
            }
        }
    }

    private func addressCard(_ d: OrderDetailDTO) -> some View {
        card("عنوان التوصيل", icon: "mappin.and.ellipse") {
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                Text(d.addressLabelAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                Text(d.addressLineAr).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                miniMap(lat: d.addressLat, lng: d.addressLng)
                mapsLink(lat: d.addressLat, lng: d.addressLng, label: "الاتجاهات إلى العميل")
            }
        }
    }

    private func storeCard(_ d: OrderDetailDTO) -> some View {
        card("المتجر", icon: "storefront.fill") {
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                Text(d.storeNameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                Text([d.storeCuisineAr, d.storeAddressAr].filter { !$0.isEmpty }.joined(separator: " · "))
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                HStack {
                    Label("\(d.storeEtaMinutes) دقيقة تجهيز", systemImage: "clock")
                        .font(KonoFont.caption).foregroundStyle(KonoColor.textTertiary)
                    Spacer()
                    if !d.storePhone.isEmpty { contactButton("phone.fill", "tel:\(digits(d.storePhone))") }
                }
                miniMap(lat: d.storeLat, lng: d.storeLng)
                mapsLink(lat: d.storeLat, lng: d.storeLng, label: "الاتجاهات إلى المتجر")
            }
        }
    }

    private func itemsCard(_ d: OrderDetailDTO) -> some View {
        card("الأصناف (\(d.itemCount))", icon: "bag.fill") {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(d.items, id: \.id) { line in
                    HStack(alignment: .top, spacing: KonoSpace.s) {
                        Text("\(line.quantity)×").font(KonoFont.bodyMedium).foregroundStyle(KonoColor.brown)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(line.nameAr).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                            if !line.selectedOptionsAr.isEmpty {
                                Text(line.selectedOptionsAr).font(KonoFont.footnote)
                                    .foregroundStyle(KonoColor.textSecondary)
                            }
                            if !line.noteAr.isEmpty {
                                Text("ملاحظة: \(line.noteAr)").font(KonoFont.caption)
                                    .foregroundStyle(KonoColor.caramel)
                            }
                        }
                        Spacer()
                        RiyalText(amount: line.lineTotal, font: KonoFont.subheadline)
                    }
                    .padding(.vertical, KonoSpace.s)
                    if line.id != d.items.last?.id { Divider() }
                }
            }
        }
    }

    private func totalsCard(_ d: OrderDetailDTO) -> some View {
        card("الحساب", icon: "banknote.fill") {
            VStack(spacing: KonoSpace.xs) {
                totalRow("المجموع الفرعي", d.subtotal)
                totalRow("رسوم التوصيل", d.deliveryFee)
                totalRow("رسوم الخدمة", d.serviceFee)
                if d.discount > 0 { totalRow("خصم", -d.discount, tint: KonoColor.success) }
                if d.tip > 0 { totalRow("بقشيش", d.tip) }
                Divider().padding(.vertical, 2)
                HStack {
                    Text("الإجمالي").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    Spacer()
                    RiyalText(amount: d.total, font: KonoFont.priceLarge)
                }
                if audience == .courier {
                    Divider().padding(.vertical, 2)
                    HStack {
                        Label("أجرتك من الرحلة", systemImage: "bicycle")
                            .font(KonoFont.subheadline).foregroundStyle(KonoColor.brown)
                        Spacer()
                        RiyalText(amount: d.courierPayout, font: KonoFont.price, color: KonoColor.brown)
                    }
                    Text("المسافة ~\(d.distanceKm, specifier: "%.1f") كم")
                        .font(KonoFont.caption).foregroundStyle(KonoColor.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    private func paymentCard(_ d: OrderDetailDTO) -> some View {
        card("الدفع", icon: "creditcard.fill") {
            VStack(alignment: .leading, spacing: 4) {
                Text(d.paymentLabelAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                if let s = d.scheduledForAr, !s.isEmpty {
                    Text("مجدول لـ \(s)").font(KonoFont.footnote).foregroundStyle(KonoColor.caramel)
                }
            }
        }
    }

    private func noteCard(_ note: String) -> some View {
        card("ملاحظة العميل", icon: "exclamationmark.bubble.fill", tint: KonoColor.caramel) {
            Text(note).font(KonoFont.callout).foregroundStyle(KonoColor.textPrimary)
        }
    }

    private func timelineCard(_ d: OrderDetailDTO) -> some View {
        card("سجل الطلب", icon: "clock.arrow.circlepath") {
            VStack(alignment: .leading, spacing: KonoSpace.s) {
                ForEach(Array(d.events.enumerated()), id: \.offset) { _, ev in
                    HStack(spacing: KonoSpace.s) {
                        Circle().fill(KonoColor.brown).frame(width: 7, height: 7)
                        Text(LocalizedStringKey(stageAr(ev.stage))).font(KonoFont.footnote).foregroundStyle(KonoColor.textPrimary)
                        Spacer()
                        Text(ev.at.formatted(date: .omitted, time: .shortened))
                            .font(.system(size: 11)).foregroundStyle(KonoColor.textTertiary)
                    }
                }
            }
        }
    }

    // MARK: helpers

    private func card<C: View>(_ title: String, icon: String, tint: Color = KonoColor.brown,
                               @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            Label(LocalizedStringKey(title), systemImage: icon).font(KonoFont.subheadline).foregroundStyle(tint)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(KonoSpace.l)
        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
    }

    private func totalRow(_ label: String, _ amount: Double, tint: Color = KonoColor.textSecondary) -> some View {
        HStack {
            Text(LocalizedStringKey(label)).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            Spacer()
            RiyalText(amount: amount, font: KonoFont.footnote, color: tint, showSymbol: true)
        }
    }

    private func miniMap(lat: Double, lng: Double) -> some View {
        Map(initialPosition: .region(MKCoordinateRegion(
            center: .init(latitude: lat, longitude: lng),
            span: .init(latitudeDelta: 0.01, longitudeDelta: 0.01)))) {
            Marker("", coordinate: .init(latitude: lat, longitude: lng)).tint(KonoColor.brown)
        }
        .frame(height: 120)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private func mapsLink(lat: Double, lng: Double, label: String) -> some View {
        if lat != 0, lng != 0, let url = URL(string: "http://maps.apple.com/?daddr=\(lat),\(lng)&dirflg=d") {
            Link(destination: url) {
                Label(LocalizedStringKey(label), systemImage: "arrow.triangle.turn.up.right.circle.fill")
                    .font(KonoFont.subheadline.weight(.semibold))
                    .foregroundStyle(KonoColor.brown)
            }
        }
    }

    @ViewBuilder
    private func contactButton(_ icon: String, _ urlString: String) -> some View {
        if let url = URL(string: urlString) {
            Link(destination: url) {
                Image(systemName: icon).font(.system(size: 13, weight: .bold))
                    .foregroundStyle(KonoColor.textOnBrown)
                    .frame(width: 34, height: 34).background(KonoColor.brown, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private func digits(_ s: String) -> String { s.filter { $0.isNumber || $0 == "+" } }

    private func stageColor(_ raw: String) -> Color {
        switch raw {
        case "Placed": return KonoColor.danger
        case "Delivered": return KonoColor.success
        case "Cancelled": return KonoColor.textTertiary
        default: return KonoColor.caramel
        }
    }

    private func stageAr(_ raw: String) -> String {
        switch raw {
        case "Placed": return "استُلم الطلب"
        case "Confirmed": return "أكّد المتجر"
        case "Preparing": return "بدأ التجهيز"
        case "ReadyForPickup": return "جاهز للاستلام"
        case "CourierAssigned": return "أُسند لمندوب"
        case "PickedUp": return "استلمه المندوب"
        case "OnTheWay": return "انطلق للعميل"
        case "Arrived": return "وصل للعميل"
        case "Delivered": return "تم التوصيل"
        case "Cancelled": return "أُلغي"
        default: return raw
        }
    }
}
