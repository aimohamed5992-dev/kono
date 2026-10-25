import SwiftUI

struct MerchantStoreView: View {
    @Environment(\.merchant) private var session
    @State private var prep: Double = 15
    @State private var busyMode = false
    @State private var info: InfoTopic?
    @State private var editing = false
    @State private var showChats = false

    struct InfoTopic: Identifiable { let id = UUID(); let title: String; let body: String }

    private var p: MerchantProfile { session.profile }

    private func ar(_ n: Int) -> String {
        let d: [Character: Character] = ["0":"٠","1":"١","2":"٢","3":"٣","4":"٤",
                                         "5":"٥","6":"٦","7":"٧","8":"٨","9":"٩"]
        return String("\(n)".map { d[$0] ?? $0 })
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    // header — tap to edit store info
                    Button { editing = true } label: {
                        HStack(spacing: KonoSpace.m) {
                            AssetImage(name: p.logoAsset.isEmpty ? p.heroAsset : p.logoAsset,
                                       fallbackSymbol: "storefront.fill", fallbackTint: KonoColor.caramel)
                                .frame(width: 64, height: 64).clipShape(Circle())
                            VStack(alignment: .leading, spacing: 3) {
                                Text(p.storeName).font(KonoFont.title2)
                                    .foregroundStyle(KonoColor.textPrimary).lineLimit(1)
                                HStack(spacing: KonoSpace.s) {
                                    RatingBadge(rating: session.reports?.rating ?? p.rating,
                                                count: max(p.ratingCount, 1), compact: true)
                                    OnboardingStatusChip(status: p.onboardingStatus)
                                }
                                Text(p.addressAr.k)
                                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary).lineLimit(1)
                            }
                            Spacer()
                            Image(systemName: "square.and.pencil").font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(KonoColor.brown)
                        }
                        .padding(KonoSpace.l)
                        .background(KonoColor.surface).konoCardShape(KonoRadius.l).konoShadow(.sm)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    if p.onboardingStatus == "under_review" {
                        Label("طلب تسجيل متجرك قيد المراجعة — بعض الميزات محدودة حتى الموافقة.",
                              systemImage: "clock.badge.checkmark")
                            .font(KonoFont.footnote).foregroundStyle(KonoColor.caramel)
                            .padding(KonoSpace.m)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(KonoColor.caramel.opacity(0.1), in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    }

                    // prep time
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        HStack {
                            Text("متوسط وقت التجهيز").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            Text("\(Int(prep)) دقيقة").font(KonoFont.price).foregroundStyle(KonoColor.brown)
                        }
                        Slider(value: $prep, in: 5...45, step: 5)
                            .tint(KonoColor.brown)
                            .onChange(of: prep) { _, v in session.setPrep(Int(v)) }
                        Text("يُعرض للعميل ويؤثر على وقت التوصيل المتوقع.")
                            .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.m)

                    Toggle(isOn: $busyMode) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("وضع الازدحام").font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                            Text("يضيف ١٥ دقيقة لوقت التجهيز مؤقتاً").font(KonoFont.footnote)
                                .foregroundStyle(KonoColor.textSecondary)
                        }
                    }
                    .onChange(of: busyMode) { _, v in session.setBusy(v) }
                    .tint(KonoColor.caramel)
                    .padding(KonoSpace.m)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.m)

                    // store settings — real values from /api/merchant/store
                    group("المتجر") {
                        row("ساعات العمل", "clock.fill",
                            trailing: p.openingHoursAr.isEmpty ? "غير محدد" : p.openingHoursAr) { editing = true }
                        row("منطقة التوصيل", "map.fill",
                            trailing: "ضمن \(ar(Int(p.deliveryRadiusKm))) كم") {
                            info = InfoTopic(title: "منطقة التوصيل",
                                body: "طلباتك تُعرض على المندوبين ضمن \(ar(Int(p.deliveryRadiusKm))) كم من موقع المتجر (\(p.addressAr)). العملاء خارج هذا النطاق لا يرون متجرك في «الأقرب إليك».")
                        }
                        row("الحساب البنكي والتحويلات", "building.columns.fill",
                            trailing: p.ibanLast4.isEmpty ? "أضف حساباً" : "•••• \(p.ibanLast4)") { editing = true }
                        row("حساب المتجر", "person.badge.key.fill",
                            trailing: p.ownerNameAr.isEmpty ? nil : p.ownerNameAr) {
                            info = InfoTopic(title: "حساب المتجر",
                                body: "المالك: \(p.ownerNameAr)\nالجوال: \(p.ownerPhone)\n\nهذا هو الحساب الوحيد الذي يدير المتجر حالياً. لإضافة موظفين بصلاحيات محدودة تواصل مع دعم الشركاء.")
                        }
                        row("عدد الأصناف في القائمة", "list.bullet",
                            trailing: "\(ar(p.menuItemCount)) صنف", chevron: false, action: nil)
                    }

                    group("التواصل") {
                        row("محادثات العملاء والمناديب", "bubble.left.and.bubble.right.fill",
                            trailing: nil) { showChats = true }
                    }

                    LanguageToggleRow()

                    group("الدعم") {
                        row("مركز مساعدة الشركاء", "questionmark.circle.fill", trailing: nil) {
                            info = InfoTopic(title: "مركز مساعدة الشركاء",
                                body: "أدلة التشغيل، سياسات الإلغاء والاسترجاع، وحل المشكلات الشائعة متوفرة في مركز الشركاء. للطوارئ اتصل على ٩٢٠٠٠٠٠٠٠.")
                        }
                        row("تواصل مع كونو للأعمال", "bubble.left.and.text.bubble.right.fill", trailing: nil) {
                            info = InfoTopic(title: "تواصل مع كونو للأعمال",
                                body: "فريق حسابات الشركاء متاح يومياً ٩ ص – ١١ م عبر الدردشة، أو على partners@kono.sa خلال ٢٤ ساعة.")
                        }
                    }

                    Button(role: .destructive) {
                        Task { await AppData.shared.signOut() }
                    } label: {
                        Text("تسجيل الخروج")
                            .font(KonoFont.subheadline.weight(.semibold)).foregroundStyle(KonoColor.danger)
                            .frame(maxWidth: .infinity).frame(height: 46)
                    }
                    .buttonStyle(.plain)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationTitle("المتجر")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task {
            await session.ensureBootstrapped()
            await session.loadReports()
            sync()
        }
        .refreshable { await session.loadStore(); await session.loadReports() }
        .onAppear(perform: sync)
        .sheet(item: $info) { topic in
            NavigationStack {
                SimpleInfoView(title: topic.title, body: topic.body)
                    .toolbar { ToolbarItem(placement: .confirmationAction) { Button("تم") { info = nil } } }
            }
        }
        .sheet(isPresented: $showChats) {
            NavigationStack { ChatListView() }
        }
        .sheet(isPresented: $editing) {
            StoreInfoEditor(profile: session.profile) { name, cuisine, address, hours, iban in
                Task { await session.updateStore(name: name, cuisineAr: cuisine, addressAr: address,
                                                 openingHoursAr: hours, ibanLast4: iban) }
            }
        }
    }

    private func sync() {
        prep = Double(session.profile.prepMinutes)
        busyMode = session.profile.isBusy
    }

    // MARK: settings rows

    @ViewBuilder
    private func group<C: View>(_ title: String, @ViewBuilder _ content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            Text(title.k).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            VStack(spacing: 0) { content() }
                .background(KonoColor.surface).konoCardShape(KonoRadius.m)
        }
    }

    @ViewBuilder
    private func row(_ label: String, _ icon: String, trailing: String?,
                     chevron: Bool = true, action: (() -> Void)?) -> some View {
        Button { action?() } label: {
            HStack(spacing: KonoSpace.m) {
                Image(systemName: icon).font(.system(size: 15)).foregroundStyle(KonoColor.brown).frame(width: 26)
                Text(LocalizedStringKey(label)).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                if let trailing { Text(trailing).font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary).lineLimit(1) }
                if chevron, action != nil {
                    Image(systemName: "chevron.left").font(.system(size: 12, weight: .bold))
                        .foregroundStyle(KonoColor.textTertiary)
                }
            }
            .padding(.vertical, KonoSpace.m).padding(.horizontal, KonoSpace.l)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(action == nil)
        Divider().padding(.leading, 52)
    }
}

/// Editable store profile — writes back through `PATCH /api/merchant/store`.
private struct StoreInfoEditor: View {
    let profile: MerchantProfile
    var onSave: (_ name: String, _ cuisine: String, _ address: String, _ hours: String, _ iban: String?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var cuisine = ""
    @State private var address = ""
    @State private var hours = ""
    @State private var iban = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    field("اسم المتجر", $name, "storefront")
                    field("التصنيف", $cuisine, "tag")
                    field("عنوان المتجر", $address, "mappin.and.ellipse")
                    field("ساعات العمل", $hours, "clock")
                    VStack(alignment: .leading, spacing: KonoSpace.xs) {
                        Text("آخر ٤ أرقام من الآيبان").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                        KonoTextField(placeholder: "4821", text: $iban, icon: "building.columns")
                            .keyboardType(.numberPad)
                            .onChange(of: iban) { _, v in iban = String(v.filter(\.isNumber).prefix(4)) }
                    }
                }
                .konoScreenPadding().padding(.top, KonoSpace.m)
            }
            .background(KonoColor.background)
            .navigationTitle("معلومات المتجر")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("إلغاء") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("حفظ") {
                        onSave(name.trimmingCharacters(in: .whitespaces),
                               cuisine.trimmingCharacters(in: .whitespaces),
                               address.trimmingCharacters(in: .whitespaces),
                               hours.trimmingCharacters(in: .whitespaces),
                               iban.isEmpty ? nil : iban)
                        dismiss()
                    }
                    .fontWeight(.bold)
                }
            }
            .onAppear {
                name = profile.storeName
                cuisine = profile.cuisineAr
                address = profile.addressAr
                hours = profile.openingHoursAr
                iban = profile.ibanLast4
            }
        }
    }

    private func field(_ label: String, _ text: Binding<String>, _ icon: String) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.xs) {
            Text(LocalizedStringKey(label)).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            KonoTextField(placeholder: label, text: text, icon: icon)
        }
    }
}
