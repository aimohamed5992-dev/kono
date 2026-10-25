import SwiftUI

struct VerticalListView: View {
    let kind: VerticalKind
    @Environment(\.dismiss) private var dismiss

    enum Sort: String, CaseIterable { case recommended, fastest, rating, nearest
        var titleAr: String {
            switch self {
            case .recommended: return "موصى به"
            case .fastest:     return "الأسرع"
            case .rating:      return "الأعلى تقييماً"
            case .nearest:     return "الأقرب"
            }
        }
    }

    @State private var sort: Sort = .recommended
    @State private var freeDelivery = false
    @State private var openNow = true
    @State private var appear = false
    @State private var promoOnly = false
    @State private var remote: [Store] = []
    @State private var loaded = false

    private var stores: [Store] {
        var s = loaded && !remote.isEmpty ? remote : Mock.stores.filter { $0.kind == kind }
        if freeDelivery { s = s.filter { $0.deliveryFee == 0 } }
        if openNow { s = s.filter { !$0.isClosed } }
        if promoOnly { s = s.filter { $0.promoTagAr != nil } }
        switch sort {
        case .recommended: break
        case .fastest: s.sort { $0.etaMinutes < $1.etaMinutes }
        case .rating:  s.sort { $0.rating > $1.rating }
        case .nearest: s.sort { $0.distanceKm < $1.distanceKm }
        }
        return s
    }

    private func load() async {
        // make sure the chosen address is loaded so results are distance-ranked
        await AppData.shared.ensureSignedIn(role: .customer)
        remote = await AppData.shared.stores(
            kind: kind, sort: sort.rawValue, freeDelivery: freeDelivery, openNow: openNow)
        loaded = true
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: KonoSpace.m) {
                Text(kind.subtitleAr.k)
                    .font(KonoFont.callout)
                    .foregroundStyle(KonoColor.textSecondary)
                    .konoScreenPadding()
                    .padding(.top, KonoSpace.s)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: KonoSpace.s) {
                        Menu {
                            ForEach(Sort.allCases, id: \.self) { opt in
                                Button(opt.titleAr) { withAnimation(KonoMotion.snappy) { sort = opt } }
                            }
                        } label: {
                            KonoChip(title: sort.titleAr, icon: "arrow.up.arrow.down", selected: true)
                        }
                        KonoChip(title: "توصيل مجاني", icon: "bicycle", selected: freeDelivery) {
                            withAnimation(KonoMotion.snappy) { freeDelivery.toggle() }
                        }
                        KonoChip(title: "مفتوح الآن", icon: "clock", selected: openNow) {
                            withAnimation(KonoMotion.snappy) { openNow.toggle() }
                        }
                        KonoChip(title: "عروض", icon: "tag", selected: promoOnly) {
                            withAnimation(KonoMotion.snappy) { promoOnly.toggle() }
                        }
                    }
                    .konoScreenPadding()
                }

                Text("\(stores.count) نتيجة")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                    .konoScreenPadding()

                ForEach(Array(stores.enumerated()), id: \.element.id) { i, store in
                    NavigationLink(value: CustomerRoute.store(store)) {
                        StoreRow(store: store)
                    }
                    .buttonStyle(.plain)
                    .konoScreenPadding()
                    .riseIn(delay: Double(min(i, 8)) * 0.04)
                }
            }
            .padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .scrollIndicators(.hidden)
        .navigationTitle(kind.titleAr)
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .onAppear { withAnimation(KonoMotion.spring) { appear = true } }
        .task(id: "\(sort.rawValue)-\(freeDelivery)-\(openNow)") { await load() }
    }
}
