import SwiftUI

// MARK: - Addresses

struct AddressesView: View {
    @State private var data = AppData.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            LazyVStack(spacing: KonoSpace.m) {
                if data.addresses.isEmpty {
                    KonoEmptyState(illustration: "illo_location", symbol: "mappin.slash",
                                   title: "لا توجد عناوين",
                                   message: "أضف عنواناً لتصلك الخدمات القريبة منك.")
                        .padding(.top, KonoSpace.xxl)
                }
                ForEach(data.addresses) { addr in
                    AddressRow(addr: addr,
                               isCurrent: data.currentAddress?.id == addr.id,
                               onUse: { Task { await data.selectAddress(addr) } },
                               onDelete: { Task { await data.deleteAddress(addr) } })
                }
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)
            .padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .navigationTitle("العناوين المحفوظة")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .safeAreaInset(edge: .bottom) {
            NavigationLink(value: CustomerRoute.addAddress) {
                Label("إضافة عنوان جديد", systemImage: "plus")
                    .font(KonoFont.button).foregroundStyle(KonoColor.textOnBrown)
                    .frame(maxWidth: .infinity).frame(height: KonoSize.buttonHeight)
                    .background(KonoColor.brown, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            }
            .konoScreenPadding().padding(.vertical, KonoSpace.m)
            .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
        .task { await data.loadAccount() }
    }
}

private struct AddressRow: View {
    let addr: Address
    let isCurrent: Bool
    var onUse: () -> Void
    var onDelete: () -> Void
    @State private var confirmDelete = false

    var body: some View {
        HStack(spacing: KonoSpace.m) {
            Image(systemName: addr.icon)
                .font(.system(size: 17, weight: .bold)).foregroundStyle(KonoColor.brown)
                .frame(width: 46, height: 46)
                .background(KonoColor.cream, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(addr.labelAr.k).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    if isCurrent { KonoTag(text: "مختار", color: KonoColor.success) }
                    else if addr.isDefault { KonoTag(text: "افتراضي", color: KonoColor.brown) }
                }
                Text(addr.lineAr.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary).lineLimit(2)
            }
            Spacer()
            Menu {
                Button("استخدام هذا العنوان", systemImage: "checkmark.circle", action: onUse)
                Button("حذف", systemImage: "trash", role: .destructive) { confirmDelete = true }
            } label: {
                Image(systemName: "ellipsis").font(.system(size: 16, weight: .bold))
                    .foregroundStyle(KonoColor.textSecondary).frame(width: 32, height: 32)
            }
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
        .contentShape(Rectangle())
        .onTapGesture(perform: onUse)
        .confirmationDialog("حذف «\(addr.labelAr)»؟", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("حذف", role: .destructive, action: onDelete)
        }
    }
}

/// Map picker → saves a real address to the API.
struct AddAddressFlow: View {
    var editing: Address?
    @State private var data = AppData.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        LocationSetupView(onConfirm: { picked in
            Task {
                await data.addAddress(labelAr: picked.labelAr, lineAr: picked.lineAr,
                                      cityAr: picked.cityAr, icon: picked.icon,
                                      lat: picked.latitude, lng: picked.longitude, makeDefault: true)
                dismiss()
            }
        }, onBack: { dismiss() })
        .navigationBarHidden(true)
        .hidesKonoTabBar()
    }
}

// MARK: - Payment methods

struct PaymentMethodsView: View {
    @State private var data = AppData.shared
    @State private var showAdd = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: KonoSpace.m) {
                if data.payments.isEmpty {
                    KonoEmptyState(illustration: "illo_wallet", symbol: "creditcard",
                                   title: "لا توجد طرق دفع", message: "أضف بطاقة أو محفظة.")
                        .padding(.top, KonoSpace.xxl)
                }
                ForEach(data.payments) { pm in
                    HStack(spacing: KonoSpace.m) {
                        Image(systemName: pm.icon).font(.system(size: 17)).foregroundStyle(KonoColor.brown)
                            .frame(width: 44, height: 44)
                            .background(KonoColor.cream, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(pm.labelAr.k).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            if let d = pm.detailAr {
                                Text(d).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                            }
                        }
                        Spacer()
                        if pm.isDefault { KonoTag(text: "افتراضي", color: KonoColor.brown) }
                        Button(role: .destructive) {
                            Task { await data.deletePayment(pm) }
                        } label: {
                            Image(systemName: "trash").font(.system(size: 14)).foregroundStyle(KonoColor.danger)
                        }
                    }
                    .padding(KonoSpace.m).background(KonoColor.surface).konoCardShape(KonoRadius.m)
                }
            }
            .konoScreenPadding().padding(.top, KonoSpace.s).padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .navigationTitle("طرق الدفع")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .safeAreaInset(edge: .bottom) {
            KonoButton(title: "إضافة طريقة دفع", icon: "plus") { showAdd = true }
                .konoScreenPadding().padding(.vertical, KonoSpace.m)
                .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
        .task { await data.loadAccount() }
        .sheet(isPresented: $showAdd) {
            AddPaymentSheet { label, icon, detail in
                Task { await data.addPayment(labelAr: label, icon: icon, detailAr: detail) }
            }
            .presentationDetents([.medium])
        }
    }
}

private struct AddPaymentSheet: View {
    var onAdd: (String, String, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var kind = 0
    @State private var last4 = ""

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            Text("طريقة دفع جديدة").font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                .padding(.top, KonoSpace.l)
            Picker("", selection: $kind) {
                Text("مدى").tag(0); Text("بطاقة ائتمان").tag(1); Text("Apple Pay").tag(2)
            }
            .pickerStyle(.segmented)
            if kind != 2 {
                KonoTextField(placeholder: "آخر ٤ أرقام", text: $last4, icon: "creditcard")
            }
            KonoButton(title: "إضافة") {
                let label = kind == 0 ? "مدى" : kind == 1 ? "بطاقة ائتمان" : "Apple Pay"
                let icon = kind == 2 ? "apple.logo" : "creditcard.fill"
                onAdd(label, icon, last4.isEmpty ? nil : "**** \(last4)")
                dismiss()
            }
            Spacer()
        }
        .konoScreenPadding()
        .background(KonoColor.background)
    }
}

// MARK: - Store lists (deals / nearby / cuisine handled by search)

struct DealsListView: View {
    let title: String
    @State private var stores: [Store] = []
    @State private var loading = true

    var body: some View {
        StoreListScroll(stores: stores, loading: loading, empty: "لا توجد عروض حالياً")
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .hidesKonoTabBar()
            .task {
                let all = await AppData.shared.stores(kind: .restaurants, sort: "recommended",
                                                      freeDelivery: false, openNow: false)
                let more = await AppData.shared.stores(kind: .groceries, sort: "recommended",
                                                       freeDelivery: false, openNow: false)
                stores = (all + more).filter { $0.promoTagAr != nil }
                if stores.isEmpty {
                    stores = (AppData.shared.online ? AppData.shared.stores : Mock.stores)
                        .filter { $0.promoTagAr != nil }
                }
                loading = false
            }
    }
}

struct NearbyView: View {
    @State private var data = AppData.shared

    var body: some View {
        let pool = data.online ? data.stores : Mock.stores
        StoreListScroll(stores: pool.sorted { $0.distanceKm < $1.distanceKm },
                        loading: false, empty: "لا توجد خدمات قريبة")
            .navigationTitle("الأقرب إليك")
            .navigationBarTitleDisplayMode(.inline)
            .hidesKonoTabBar()
            .task { await data.loadHome() }
    }
}

struct StoreListScroll: View {
    let stores: [Store]
    let loading: Bool
    var empty: String = "لا توجد نتائج"

    var body: some View {
        ScrollView {
            LazyVStack(spacing: KonoSpace.m) {
                if loading {
                    ForEach(0..<5, id: \.self) { _ in StoreCardSkeleton().konoScreenPadding() }
                } else if stores.isEmpty {
                    Text(empty).font(KonoFont.callout).foregroundStyle(KonoColor.textSecondary)
                        .frame(maxWidth: .infinity).padding(.top, KonoSpace.xxxl)
                } else {
                    ForEach(stores) { store in
                        NavigationLink(value: CustomerRoute.store(store)) { StoreRow(store: store) }
                            .konoScreenPadding()
                    }
                }
            }
            .padding(.top, KonoSpace.s).padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .scrollIndicators(.hidden)
    }
}

// MARK: - Notifications

struct NotificationsView: View {
    var body: some View {
        KonoEmptyState(illustration: "illo_favorites", symbol: "bell.slash",
                       title: "لا توجد إشعارات",
                       message: "سنخبرك هنا بحالة طلباتك وأحدث العروض.")
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(KonoColor.background)
        .navigationTitle("الإشعارات")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
    }
}

// MARK: - Kono Plus

struct KonoPlusView: View {
    @State private var data = AppData.shared
    @State private var working = false

    var body: some View {
        ScrollView {
            VStack(spacing: KonoSpace.l) {
                VStack(spacing: KonoSpace.s) {
                    Image(systemName: "crown.fill").font(.system(size: 44)).foregroundStyle(KonoColor.brown)
                    Text("كونو بلس").font(KonoFont.display(30)).foregroundStyle(KonoColor.espresso)
                    Text(data.me?.konoPlus == true ? "اشتراكك فعّال" : "٢٩ ر.س / شهرياً")
                        .font(KonoFont.headline).foregroundStyle(KonoColor.textSecondary)
                }
                .frame(maxWidth: .infinity).padding(.vertical, KonoSpace.xl)
                .background(KonoColor.brandGradient)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))

                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    benefit("bicycle", "توصيل مجاني", "على كل الطلبات فوق ٣٠ ر.س")
                    benefit("percent", "خصومات حصرية", "عروض أسبوعية لأعضاء بلس فقط")
                    benefit("bolt.fill", "أولوية في الدعم", "ردّ أسرع على استفساراتك")
                }
                .padding(KonoSpace.l).frame(maxWidth: .infinity, alignment: .leading)
                .background(KonoColor.surface).konoCardShape(KonoRadius.m)

                KonoButton(title: data.me?.konoPlus == true ? "إلغاء الاشتراك" : "اشترك الآن",
                           kind: data.me?.konoPlus == true ? .secondary : .primary, loading: working) {
                    working = true
                    Task { await data.toggleKonoPlus(); working = false }
                }
            }
            .konoScreenPadding().padding(.top, KonoSpace.s).padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .navigationTitle("كونو بلس")
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
    }

    private func benefit(_ icon: String, _ title: String, _ sub: String) -> some View {
        HStack(spacing: KonoSpace.m) {
            Image(systemName: icon).font(.system(size: 16, weight: .bold)).foregroundStyle(KonoColor.brown)
                .frame(width: 36, height: 36).background(KonoColor.cream, in: Circle())
            VStack(alignment: .leading, spacing: 1) {
                Text(title.k).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                Text(sub.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            }
            Spacer()
        }
    }
}

// MARK: - Simple info

struct SimpleInfoView: View {
    let title: String
    let body_: String
    init(title: String, body: String) { self.title = title; self.body_ = body }

    var body: some View {
        ScrollView {
            Text(body_.k)
                .font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                .lineSpacing(6)
                .frame(maxWidth: .infinity, alignment: .leading)
                .konoScreenPadding().padding(.top, KonoSpace.l)
        }
        .background(KonoColor.background)
        .navigationTitle(LocalizedStringKey(title))
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
    }
}
