import SwiftUI

struct CourierEarningsView: View {
    @Environment(\.courier) private var session
    @State private var range = 0
    @State private var summary: CourierSummaryDTO?
    @State private var confirmPayout = false
    @State private var payoutDone = false

    private var week: [(weekdayAr: String, amount: Double)] {
        if let w = summary?.week { return w.map { ($0.weekdayAr, $0.amount) } }
        return Mock.courierEarningsWeek().map { ($0.weekdayAr, $0.amount) }
    }
    private var deliveryPart: Double { summary?.deliveryPart ?? session.todayEarnings * 0.78 }
    private var tipsPart: Double { summary?.tipsPart ?? session.todayEarnings * 0.14 }
    private var bonusPart: Double { summary?.bonusPart ?? session.todayEarnings * 0.08 }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    // hero
                    VStack(spacing: KonoSpace.xs) {
                        Text("أرباح اليوم").font(KonoFont.footnote).foregroundStyle(KonoColor.brown.opacity(0.8))
                        RiyalText(amount: summary?.todayEarnings ?? session.todayEarnings, font: KonoFont.display(40), color: KonoColor.brown)
                        Text("\(summary?.todayTrips ?? session.todayTrips) رحلة مكتملة").font(KonoFont.subheadline)
                            .foregroundStyle(KonoColor.brown.opacity(0.8))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, KonoSpace.xl)
                    .background(KonoColor.brandGradient)
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))

                    KonoButton(title: payoutDone ? "تم إرسال طلب التحويل ✓" : "تحويل الأرباح إلى الحساب البنكي",
                               icon: "arrow.down.to.line") {
                        confirmPayout = true
                    }
                    .disabled((summary?.todayEarnings ?? session.todayEarnings) <= 0 || payoutDone)

                    // breakdown
                    VStack(spacing: KonoSpace.s) {
                        breakdownRow("توصيل الطلبات", deliveryPart, "bicycle")
                        breakdownRow("إكراميات العملاء", tipsPart, "gift.fill")
                        breakdownRow("حوافز وبونص", max(0, bonusPart), "bolt.fill")
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.m)

                    // week chart
                    VStack(alignment: .leading, spacing: KonoSpace.m) {
                        Picker("", selection: $range) {
                            Text("الأسبوع").tag(0); Text("الشهر").tag(1)
                        }
                        .pickerStyle(.segmented)
                        Text("إجمالي الأسبوع \(Int(week.reduce(0){$0+$1.amount})) ر.س")
                            .font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        BarChart(values: week.map(\.amount), labels: week.map(\.weekdayAr))
                            .frame(height: 160)
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.l).konoShadow(.sm)

                    // incentive
                    let weekTrips = summary?.week.reduce(0) { $0 + $1.trips } ?? session.todayTrips
                    HStack(spacing: KonoSpace.m) {
                        Image(systemName: "target").font(.system(size: 22)).foregroundStyle(KonoColor.caramel)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("تحدّي الويكند").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            Text("أكمل ٢٠ رحلة واحصل على ١٠٠ ر.س إضافية").font(KonoFont.footnote)
                                .foregroundStyle(KonoColor.textSecondary)
                            ProgressView(value: Double(min(weekTrips, 20)), total: 20).tint(KonoColor.caramel)
                            Text("\(weekTrips) / 20").font(KonoFont.caption).foregroundStyle(KonoColor.textTertiary)
                        }
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.m)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationTitle("الأرباح")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                await session.ensureBootstrapped()
                summary = await AppData.shared.courierSummary()
            }
            .refreshable {
                await session.refreshProfile()
                summary = await AppData.shared.courierSummary()
            }
            .confirmationDialog("تحويل \(Int(summary?.todayEarnings ?? session.todayEarnings)) ر.س إلى حسابك البنكي؟",
                                isPresented: $confirmPayout, titleVisibility: .visible) {
                Button("تأكيد التحويل") {
                    #if canImport(UIKit)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    #endif
                    withAnimation { payoutDone = true }
                }
                Button("إلغاء", role: .cancel) {}
            } message: {
                Text("تصل الحوالة خلال ١–٣ أيام عمل. الحد الأدنى للتحويل ٥٠ ر.س.")
            }
        }
    }

    private func breakdownRow(_ label: String, _ amount: Double, _ icon: String) -> some View {
        HStack(spacing: KonoSpace.m) {
            Image(systemName: icon).font(.system(size: 14)).foregroundStyle(KonoColor.brown).frame(width: 24)
            Text(LocalizedStringKey(label)).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
            Spacer()
            RiyalText(amount: amount, font: KonoFont.price)
        }
    }
}

struct CourierHistoryView: View {
    @State private var trips: [CourierTripDTO] = []
    @State private var loaded = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: KonoSpace.s) {
                    if loaded && trips.isEmpty {
                        KonoEmptyState(illustration: "illo_success", symbol: "bicycle",
                                       title: "ما في رحلات بعد",
                                       message: "رحلاتك المكتملة بتظهر هنا مع الأرباح والتقييم.")
                            .padding(.top, KonoSpace.xxl)
                    }
                    ForEach(Array(trips.enumerated()), id: \.offset) { _, t in
                        HStack(spacing: KonoSpace.m) {
                            Image(systemName: "checkmark.circle.fill").font(.system(size: 20))
                                .foregroundStyle(KonoColor.success)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("رحلة \(t.shortCode)").font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                                Text(t.whenAr.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                RiyalText(amount: t.amount, font: KonoFont.price)
                                RatingBadge(rating: t.rating, compact: true)
                            }
                        }
                        .padding(KonoSpace.m)
                        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
                    }
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationTitle("سجل الرحلات")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                await AppData.shared.ensureSignedIn(role: .courier)
                trips = await AppData.shared.courierSummary()?.recentTrips ?? []
                loaded = true
            }
        }
    }
}

struct CourierProfileView: View {
    @Environment(\.courier) private var session
    @State private var info: InfoTopic?
    @State private var showChats = false

    struct InfoTopic: Identifiable { let id = UUID(); let title: String; let body: String }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: KonoSpace.l) {
                    VStack(spacing: KonoSpace.s) {
                        ZStack {
                            Circle().fill(KonoColor.brandGradient).frame(width: 88, height: 88)
                            AssetImage(name: session.profile.photoAsset, fallbackSymbol: "person.fill",
                                       fallbackTint: KonoColor.brown)
                                .frame(width: 82, height: 82).clipShape(Circle())
                        }
                        Text(session.profile.name).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                        HStack(spacing: KonoSpace.s) {
                            RatingBadge(rating: session.profile.rating,
                                        count: max(session.profile.ratingCount, 1))
                            OnboardingStatusChip(status: session.profile.onboardingStatus)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.l).konoShadow(.sm)

                    if session.profile.onboardingStatus == "under_review" {
                        Label("طلب تسجيلك كمندوب قيد المراجعة — ستصلك رسالة عند الموافقة.",
                              systemImage: "clock.badge.checkmark")
                            .font(KonoFont.footnote).foregroundStyle(KonoColor.caramel)
                            .padding(KonoSpace.m)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(KonoColor.caramel.opacity(0.1), in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    }

                    HStack(spacing: KonoSpace.m) {
                        stat("\(session.profile.acceptanceRate)٪", "نسبة القبول")
                        stat("\(session.profile.completionRate)٪", "نسبة الإكمال")
                        stat(session.profile.vehicleAr, "المركبة")
                    }

                    Button { showChats = true } label: {
                        HStack(spacing: KonoSpace.m) {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                                .font(.system(size: 15)).foregroundStyle(KonoColor.brown).frame(width: 26)
                            Text("محادثات العملاء والمتاجر").font(KonoFont.body)
                                .foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            Image(systemName: "chevron.left").font(.system(size: 12, weight: .bold))
                                .foregroundStyle(KonoColor.textTertiary)
                        }
                        .padding(.vertical, KonoSpace.m).padding(.horizontal, KonoSpace.l)
                        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    LanguageToggleRow()

                    group("الحساب", [
                        ("الوثائق والرخص", "doc.text.fill", "مكتملة",
                         "الهوية، رخصة القيادة، واستمارة المركبة موثّقة وسارية. تُذكّرك كونو قبل انتهاء أي وثيقة بـ ٣٠ يوماً."),
                        ("بيانات المركبة", "bicycle", nil,
                         "المركبة المسجّلة: \(session.profile.vehicleAr) · اللوحة \(session.profile.plateAr). لتغيير نوع المركبة أو اللوحة تواصل مع دعم المندوبين لإعادة التوثيق."),
                        ("الحساب البنكي", "building.columns.fill", nil,
                         "تُحوَّل أرباحك يومياً بعد الساعة ٢ ظهراً إلى الآيبان المسجَّل (‏SA•• •••• •••• •••• 7734)."),
                        ("جدول المناوبات", "calendar", nil,
                         "أنت غير مرتبط بمناوبات ثابتة — تعمل وقتما تفعّل «متصل». مناطق الذروة: ١٢–٣ ظهراً و ٧–١١ مساءً."),
                    ])
                    group("الدعم", [
                        ("مركز المساعدة", "questionmark.circle.fill", nil,
                         "أسئلة شائعة حول الأرباح، الطلبات، والتقييم متوفرة في مركز المساعدة. الدعم المباشر متاح ٢٤/٧ عبر الدردشة."),
                        ("السلامة والطوارئ", "shield.lefthalf.filled", nil,
                         "زر الطوارئ داخل الرحلة يتصل مباشرة بـ ٩١١ ويشارك موقعك مع فريق السلامة. بلّغ عن أي حادث خلال ٢٤ ساعة."),
                    ])

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
            .navigationTitle("الملف")
            .navigationBarTitleDisplayMode(.inline)
            .task { await session.ensureBootstrapped() }
            .refreshable { await session.refreshProfile() }
        }
        .sheet(item: $info) { topic in
            NavigationStack {
                SimpleInfoView(title: topic.title, body: topic.body)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) { Button("تم") { info = nil } }
                    }
            }
        }
        .sheet(isPresented: $showChats) {
            NavigationStack { ChatListView() }
        }
    }

    private func stat(_ v: String, _ l: String) -> some View {
        VStack(spacing: 3) {
            Text(v).font(.cairo(15, .heavy)).foregroundStyle(KonoColor.textPrimary)
                .minimumScaleFactor(0.7).lineLimit(1)
            Text(l).font(.cairo(10, .medium)).foregroundStyle(KonoColor.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, KonoSpace.m)
        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
    }

    private func group(_ title: String, _ rows: [(String, String, String?, String)]) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            Text(title.k).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.offset) { i, row in
                    Button {
                        info = InfoTopic(title: row.0, body: row.3)
                    } label: {
                        HStack(spacing: KonoSpace.m) {
                            Image(systemName: row.1).font(.system(size: 15)).foregroundStyle(KonoColor.brown).frame(width: 26)
                            Text(LocalizedStringKey(row.0)).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            if let d = row.2 { Text(d).font(KonoFont.footnote).foregroundStyle(KonoColor.success) }
                            Image(systemName: "chevron.left").font(.system(size: 12, weight: .bold))
                                .foregroundStyle(KonoColor.textTertiary)
                        }
                        .padding(.vertical, KonoSpace.m).padding(.horizontal, KonoSpace.l)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if i < rows.count - 1 { Divider().padding(.leading, 52) }
                }
            }
            .background(KonoColor.surface).konoCardShape(KonoRadius.m)
        }
    }
}
