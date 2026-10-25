import SwiftUI

struct SearchView: View {
    var prefill: String? = nil
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var scope: Scope = .all
    @FocusState private var focused: Bool

    enum Scope: String, CaseIterable {
        case all, stores, items
        var titleAr: String {
            switch self {
            case .all: return "الكل"
            case .stores: return "المتاجر"
            case .items: return "الأصناف"
            }
        }
    }

    @State private var data = AppData.shared
    @State private var storeHits: [Store] = []
    @State private var itemHits: [(Store, Product)] = []
    @State private var searchTask: Task<Void, Never>?

    private func runSearch() {
        searchTask?.cancel()
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { storeHits = []; itemHits = []; return }
        searchTask = Task {
            try? await Task.sleep(nanoseconds: 250_000_000)   // debounce
            guard !Task.isCancelled else { return }
            let (stores, items) = await AppData.shared.search(q, scope: scope.rawValue)
            guard !Task.isCancelled else { return }
            if stores.isEmpty && items.isEmpty && !AppData.shared.online {
                // offline: fall back to the bundled catalog
                storeHits = Mock.stores.filter {
                    $0.name.localizedCaseInsensitiveContains(q)
                    || $0.cuisineAr.localizedCaseInsensitiveContains(q)
                }
                var out: [(Store, Product)] = []
                for s in Mock.stores {
                    for c in Mock.menu(for: s) {
                        for it in c.items where it.nameAr.localizedCaseInsensitiveContains(q) {
                            out.append((s, it))
                        }
                    }
                }
                itemHits = out
            } else {
                storeHits = stores
                itemHits = items
            }
            if !storeHits.isEmpty || !itemHits.isEmpty { data.recordSearch(q) }
        }
    }

    var body: some View {
        VStack(spacing: KonoSpace.m) {
            HStack(spacing: KonoSpace.s) {
                Button { focused = false; dismiss() } label: {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(KonoColor.brown)
                        .frame(width: 44, height: 44)
                        .background(KonoColor.surface, in: Circle())
                        .overlay(Circle().strokeBorder(KonoColor.hairline, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("رجوع")

                HStack(spacing: KonoSpace.s) {
                    Image(systemName: "magnifyingglass").font(.system(size: 15, weight: .bold))
                        .foregroundStyle(KonoColor.brown.opacity(0.7))
                    TextField("ابحث عن مطعم، صيدلية، أو صنف", text: $query)
                        .font(KonoFont.body)
                        .focused($focused)
                        .submitLabel(.search)
                    if !query.isEmpty {
                        Button { query = "" } label: {
                            Image(systemName: "xmark.circle.fill").foregroundStyle(KonoColor.textTertiary)
                        }.buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, KonoSpace.l)
                .frame(height: 48)
                .background(KonoColor.surface, in: Capsule())
                .overlay(Capsule().strokeBorder(focused ? KonoColor.brown : KonoColor.hairline,
                                                lineWidth: focused ? 1.5 : 1))
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)

            if query.isEmpty {
                discovery
            } else {
                results
            }
        }
        .background(KonoColor.background)
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .onAppear {
            if let prefill, query.isEmpty { query = prefill }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { focused = true }
        }
        .onChange(of: query) { _, _ in runSearch() }
        .onChange(of: scope) { _, _ in runSearch() }
    }

    private var discovery: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KonoSpace.xl) {
                if !data.recentSearches.isEmpty {
                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        HStack {
                            Text("عمليات بحث سابقة").font(KonoFont.headline)
                                .foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            Button("مسح") { withAnimation { data.clearRecentSearches() } }
                                .font(KonoFont.footnote).foregroundStyle(KonoColor.brown)
                        }
                        ForEach(data.recentSearches, id: \.self) { s in
                            Button { query = s } label: {
                                HStack {
                                    Image(systemName: "clock.arrow.circlepath")
                                        .font(.system(size: 13)).foregroundStyle(KonoColor.textTertiary)
                                    Text(s).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                                    Spacer()
                                    Image(systemName: "arrow.up.left")
                                        .font(.system(size: 11)).foregroundStyle(KonoColor.textTertiary)
                                }
                                .padding(.vertical, 6)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    Text("الأكثر بحثاً").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    FlowChips(items: data.trendingSearches) { query = $0 }
                }
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)
        }
        .scrollIndicators(.hidden)
    }

    private var results: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KonoSpace.l) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: KonoSpace.s) {
                        ForEach(Scope.allCases, id: \.self) { s in
                            KonoChip(title: s.titleAr, selected: scope == s) {
                                withAnimation(KonoMotion.snappy) { scope = s }
                            }
                        }
                    }
                }

                if storeHits.isEmpty && itemHits.isEmpty {
                    KonoEmptyState(illustration: "illo_location", symbol: "magnifyingglass",
                                   title: "ما لقينا نتائج",
                                   message: "جرّب كلمة أبسط أو تأكد من الإملاء.")
                        .padding(.top, KonoSpace.xxl)
                }

                if scope != .items, !storeHits.isEmpty {
                    Text("متاجر").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                    ForEach(storeHits) { store in
                        NavigationLink(value: CustomerRoute.store(store)) { StoreRow(store: store) }
                            .buttonStyle(.plain)
                    }
                }

                if scope != .stores, !itemHits.isEmpty {
                    Text("أصناف").font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                    ForEach(Array(itemHits.enumerated()), id: \.offset) { _, pair in
                        NavigationLink(value: CustomerRoute.store(pair.0)) {
                            ItemHitRow(store: pair.0, product: pair.1)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)
        }
        .scrollIndicators(.hidden)
    }
}

private struct ItemHitRow: View {
    let store: Store
    let product: Product
    var body: some View {
        HStack(spacing: KonoSpace.m) {
            AssetImage(name: product.imageAsset, fallbackSymbol: "takeoutbag.and.cup.and.straw.fill",
                       fallbackTint: store.kind.accent)
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            VStack(alignment: .leading, spacing: 3) {
                Text(product.nameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                Text(store.name).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
            }
            Spacer()
            RiyalText(amount: product.price)
        }
        .padding(KonoSpace.s)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }
}

/// Simple wrapping chip layout.
struct FlowChips: View {
    let items: [String]
    var onTap: (String) -> Void

    var body: some View {
        var width = CGFloat.zero
        var rows: [[String]] = [[]]
        let limit: CGFloat = UIScreen.main.bounds.width - 2 * KonoSpace.screen
        for item in items {
            let est = CGFloat(item.count) * 11 + 40
            if width + est > limit {
                rows.append([item]); width = est
            } else {
                rows[rows.count - 1].append(item); width += est
            }
        }
        return VStack(alignment: .leading, spacing: KonoSpace.s) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: KonoSpace.s) {
                    ForEach(row, id: \.self) { item in
                        Button { onTap(item) } label: {
                            Text(item).font(KonoFont.subheadline).foregroundStyle(KonoColor.brown)
                                .padding(.horizontal, KonoSpace.m)
                                .frame(height: KonoSize.chipHeight)
                                .background(KonoColor.surface, in: Capsule())
                                .overlay(Capsule().strokeBorder(KonoColor.brown.opacity(0.16), lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }
}
