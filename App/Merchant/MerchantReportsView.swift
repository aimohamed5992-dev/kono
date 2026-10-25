import SwiftUI

struct MerchantReportsView: View {
    @Environment(\.merchant) private var session
    @State private var reports: MerchantReportsDTO?

    private var days: [(weekdayAr: String, amount: Double, trips: Int)] {
        if let d = reports?.days { return d.map { ($0.weekdayAr, $0.amount, $0.trips) } }
        return Mock.courierEarningsWeek().map { ($0.weekdayAr, $0.amount * 6, $0.trips * 5) }
    }
    private var totalSales: Double { reports?.totalSales ?? days.reduce(0) { $0 + $1.amount } }
    private var orderCount: Int { reports?.orderCount ?? days.reduce(0) { $0 + $1.trips } }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    Text("آخر ٧ أيام")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        .padding(.top, KonoSpace.s)

                    HStack(spacing: KonoSpace.m) {
                        bigStat("إجمالي المبيعات", "\(Int(totalSales)) ر.س", KonoColor.success)
                        bigStat("عدد الطلبات", "\(orderCount)", KonoColor.brown)
                    }

                    // bar chart
                    VStack(alignment: .leading, spacing: KonoSpace.m) {
                        Text("المبيعات اليومية").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        BarChart(values: days.map(\.amount), labels: days.map(\.weekdayAr))
                            .frame(height: 180)
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.l).konoShadow(.sm)

                    // ops metrics
                    HStack(spacing: KonoSpace.m) {
                        opStat("متوسط التجهيز", "\(reports?.prepMinutes ?? session.prepMinutes) د", "timer")
                        opStat("متوسط الطلب",
                               "\(Int(reports?.avgOrderValue ?? (orderCount == 0 ? 0 : totalSales / Double(orderCount)))) ر.س",
                               "cart")
                        opStat("تقييم المتجر",
                               (reports?.rating ?? session.profile.rating).formatted(.number.precision(.fractionLength(1))),
                               "star.fill")
                    }

                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("الأصناف الأكثر مبيعاً").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        let top = topItems
                        if top.isEmpty {
                            Text("لا توجد مبيعات في هذه الفترة")
                                .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                                .padding(.vertical, 8)
                        }
                        ForEach(Array(top.prefix(4).enumerated()), id: \.offset) { i, item in
                            HStack(spacing: KonoSpace.m) {
                                Text("\(i + 1)").font(KonoFont.headline).foregroundStyle(KonoColor.brown)
                                    .frame(width: 24)
                                Text(item.name).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                                Spacer()
                                Text("\(item.count) مبيع").font(KonoFont.footnote)
                                    .foregroundStyle(KonoColor.textSecondary)
                            }
                            .padding(.vertical, 8)
                            if i < min(top.count, 4) - 1 { Divider() }
                        }
                    }
                    .padding(KonoSpace.l)
                    .background(KonoColor.surface).konoCardShape(KonoRadius.m)
                }
                .konoScreenPadding()
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationTitle("التقارير")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                await session.ensureBootstrapped()
                reports = await AppData.shared.merchantReports()
            }
            .refreshable { reports = await AppData.shared.merchantReports() }
        }
    }

    private var topItems: [(name: String, count: Int)] {
        if let t = reports?.topItems { return t.map { ($0.nameAr, $0.count) } }
        return session.menu.sorted { $0.soldToday > $1.soldToday }
            .prefix(4).map { ($0.product.nameAr, $0.soldToday * 7) }
    }

    private func bigStat(_ t: String, _ v: String, _ tint: Color) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.xs) {
            Text(t).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            Text(v).font(.cairo(19, .heavy))
                .foregroundStyle(KonoColor.textPrimary).minimumScaleFactor(0.6).lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(KonoSpace.l)
        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
        .overlay(alignment: .topTrailing) {
            Circle().fill(tint.opacity(0.15)).frame(width: 10, height: 10).padding(KonoSpace.m)
        }
    }

    private func opStat(_ t: String, _ v: String, _ icon: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 15)).foregroundStyle(KonoColor.caramel)
            Text(v).font(.cairo(15, .heavy)).foregroundStyle(KonoColor.textPrimary)
            Text(t).font(.cairo(10, .medium))
                .foregroundStyle(KonoColor.textSecondary).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, KonoSpace.m)
        .background(KonoColor.surface).konoCardShape(KonoRadius.m)
    }
}

/// Simple animated bar chart in brand colours.
struct BarChart: View {
    let values: [Double]
    let labels: [String]
    @State private var grow = false

    var body: some View {
        let maxV = max(values.max() ?? 1, 1)
        GeometryReader { geo in
            HStack(alignment: .bottom, spacing: geo.size.width / CGFloat(max(values.count, 1)) * 0.2) {
                ForEach(Array(values.enumerated()), id: \.offset) { i, v in
                    VStack(spacing: 6) {
                        RoundedRectangle(cornerRadius: 5, style: .continuous)
                            .fill(i == values.count - 1 ? KonoColor.brown : KonoColor.yellow)
                            .frame(height: grow ? max(6, (geo.size.height - 24) * CGFloat(v / maxV)) : 6)
                        Text(labels[i]).font(.cairo(9, .medium))
                            .foregroundStyle(KonoColor.textSecondary)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                    .animation(KonoMotion.spring.delay(Double(i) * 0.05), value: grow)
                }
            }
        }
        .onAppear { grow = true }
    }
}
