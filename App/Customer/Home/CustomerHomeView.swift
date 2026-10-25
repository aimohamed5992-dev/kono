import SwiftUI

struct CustomerHomeView: View {
    @State private var path: NavigationPath
    @State private var data = AppData.shared
    @State private var showAddressSheet = false
    @State private var searchText = ""
    @State private var loading = true

    init() {
        var p = NavigationPath()
        switch RootFlow.launch.screen {
        case "restaurants": p.append(CustomerRoute.vertical(.restaurants))
        case "groceries":   p.append(CustomerRoute.vertical(.groceries))
        case "pharmacy":    p.append(CustomerRoute.vertical(.pharmacy))
        case "store":       if let s = Mock.stores.first { p.append(CustomerRoute.store(s)) }
        case "cart":        p.append(CustomerRoute.store(Mock.stores[0])); p.append(CustomerRoute.cart)
        case "checkout":    p.append(CustomerRoute.store(Mock.stores[0])); p.append(CustomerRoute.cart); p.append(CustomerRoute.checkout)
        case "tracking":    p.append(CustomerRoute.tracking(Mock.activeOrder))
        case "addresses":   p.append(CustomerRoute.addresses)
        case "search":      p.append(CustomerRoute.search())
        default: break
        }
        _path = State(initialValue: p)
    }

    private func go(_ route: CustomerRoute) { path.append(route) }
    private func open(_ s: Store) { path.append(CustomerRoute.store(s)) }

    private var topAddress: Address {
        data.currentAddress ?? data.addresses.first ?? Mock.customer.addresses[0]
    }
    private var promoSlides: [PromoSlide] { data.online ? data.promos : Mock.promos }
    private var verticalsList: [Vertical] {
        (data.online && !data.verticals.isEmpty) ? data.verticals : Mock.verticals
    }

    // rails: server-provided when online, computed from mock otherwise
    private var homeRails: [AppData.HomeRail] {
        if data.online, !data.rails.isEmpty { return data.rails }
        let m = Mock.stores
        func rail(_ key: String, _ title: String, _ sub: String?, _ style: String, _ f: (Store) -> Bool,
                  _ sort: (Store, Store) -> Bool = { _, _ in false }) -> AppData.HomeRail {
            AppData.HomeRail(key: key, titleAr: title, subtitleAr: sub, style: style,
                             stores: Array(m.filter(f).sorted(by: sort).prefix(8)))
        }
        return [
            rail("nearby", "الأقرب إليك", "خدمات حولك الآن", "compact", { _ in true }),
            rail("featured", "مطاعم مميزة", "مختارة لك بعناية", "featured",
                 { $0.kind == .restaurants }, { $0.rating > $1.rating }),
            rail("deals", "عروض قوية اليوم", "خصومات لوقت محدود", "deal", { $0.promoTagAr != nil }),
            rail("popular", "الأكثر طلباً حولك", nil, "compact",
                 { $0.kind == .restaurants }, { $0.ratingCount > $1.ratingCount }),
            rail("groceries", "سوبرماركت ومقاضي", "من بندة ولولو والتميمي", "circle", { $0.kind == .groceries }),
            rail("pharmacy", "صيدليات توصلك بسرعة", "دواءك خلال دقائق", "quick", { $0.kind == .pharmacy }),
            rail("sweets", "حلا ومناسبات", "كيك وحلويات شرقية", "poster", { $0.kind == .sweets }),
        ].filter { !$0.stores.isEmpty }
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                LazyVStack(spacing: KonoSpace.xxl) {
                    HomeTopBar(address: topAddress,
                               onAddress: { showAddressSheet = true },
                               onNotifications: { go(.notifications) })
                        .konoScreenPadding()
                        .padding(.top, KonoSpace.s)

                    Button { go(.search()) } label: {
                        KonoSearchBar(text: $searchText, onTap: { go(.search()) })
                            .konoScreenPadding()
                    }
                    .buttonStyle(.plain)

                    verticalStrip

                    PromoCarousel(slides: promoSlides) { slide in
                        go(.deals(title: slide.titleAr))
                    }

                    if loading {
                        skeletons
                    } else {
                        ForEach(homeRails) { railView($0) }
                        cuisineRail
                    }
                }
                .padding(.bottom, 130)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationBarHidden(true)
            .customerDestinations()
        }
        .sheet(isPresented: $showAddressSheet) {
            AddressSwitcherSheet(
                onAdd: { showAddressSheet = false; go(.addAddress) },
                onManage: { showAddressSheet = false; go(.addresses) })
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
        }
        .task {
            await data.ensureSignedIn(role: .customer)
            await data.loadHome()
            withAnimation(KonoMotion.spring) { loading = false }
        }
        .refreshable {
            await data.loadAccount()
            await data.loadHome()
        }
    }

    // Categories — each opens its own page.
    private var verticalStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: KonoSpace.l) {
                ForEach(Array(verticalsList.enumerated()), id: \.element.id) { i, v in
                    VerticalTile(vertical: v) { go(.vertical(v.kind)) }
                        .riseIn(delay: Double(i) * 0.04)
                }
            }
            .konoScreenPadding()
        }
    }

    @ViewBuilder private func railView(_ rail: AppData.HomeRail) -> some View {
        HomeRail(title: rail.titleAr, subtitle: rail.subtitleAr,
                 onSeeAll: seeAll(for: rail.key)) {
            ForEach(rail.stores) { s in
                switch rail.style {
                case "featured": FeaturedStoreCard(store: s) { open(s) }
                case "deal":     DealCard(store: s) { open(s) }
                case "circle":   StoreCircleCard(store: s) { open(s) }
                case "quick":    QuickStoreCard(store: s, accent: KonoColor.info) { open(s) }
                case "poster":   PosterStoreCard(store: s) { open(s) }
                default:         StoreCardCompact(store: s) { open(s) }
                }
            }
        }
    }

    private func seeAll(for key: String) -> (() -> Void)? {
        switch key {
        case "featured", "popular": return { go(.vertical(.restaurants)) }
        case "groceries": return { go(.vertical(.groceries)) }
        case "pharmacy": return { go(.vertical(.pharmacy)) }
        case "sweets": return { go(.vertical(.sweets)) }
        case "deals": return { go(.deals(title: "عروض قوية اليوم")) }
        case "nearby": return { go(.nearby) }
        default: return nil
        }
    }

    private var cuisineRail: some View {
        HomeRail(title: "تصفّح حسب التصنيف") {
            ForEach(Self.cuisines, id: \.0) { c in
                CuisineTile(title: c.0, asset: c.1, symbol: c.2) {
                    go(.search(prefill: c.0))
                }
            }
        }
    }

    private var skeletons: some View {
        VStack(alignment: .leading, spacing: KonoSpace.xl) {
            ForEach(0..<2, id: \.self) { _ in
                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    SkeletonBlock(height: 20, width: 160).konoScreenPadding()
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: KonoSpace.m) {
                            ForEach(0..<3, id: \.self) { _ in
                                SkeletonBlock(height: 200, width: 280, radius: KonoRadius.l)
                            }
                        }
                        .konoScreenPadding()
                    }
                }
            }
        }
    }

    private static let cuisines: [(String, String, String)] = [
        ("برجر", "food_burger_double", "fork.knife"),
        ("بيتزا", "store_pizza", "fork.knife"),
        ("دجاج", "food_chicken", "fork.knife"),
        ("قهوة", "store_coffee", "cup.and.saucer.fill"),
        ("شاورما", "store_shawarma", "fork.knife"),
        ("حلا", "store_sweets", "birthday.cake.fill"),
    ]
}

/// Bottom sheet to switch the delivery address — real addresses from `AppData`.
struct AddressSwitcherSheet: View {
    var onAdd: () -> Void
    var onManage: () -> Void
    @State private var data = AppData.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            HStack {
                Text("عنوان التوصيل").font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                Button("إدارة", action: onManage)
                    .font(KonoFont.subheadline).foregroundStyle(KonoColor.brown)
            }
            .padding(.top, KonoSpace.l)

            if data.addresses.isEmpty {
                Text("لا توجد عناوين محفوظة")
                    .font(KonoFont.callout).foregroundStyle(KonoColor.textSecondary)
                    .frame(maxWidth: .infinity).padding(.vertical, KonoSpace.l)
            }

            ForEach(data.addresses) { addr in
                Button {
                    #if canImport(UIKit)
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    #endif
                    Task { await data.selectAddress(addr) }
                    dismiss()
                } label: {
                    HStack(spacing: KonoSpace.m) {
                        Image(systemName: addr.icon)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(KonoColor.brown)
                            .frame(width: 44, height: 44)
                            .background(KonoColor.cream, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(addr.labelAr.k).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            Text(addr.lineAr.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Image(systemName: data.currentAddress?.id == addr.id ? "checkmark.circle.fill" : "circle")
                            .font(.system(size: 18))
                            .foregroundStyle(data.currentAddress?.id == addr.id ? KonoColor.brown : KonoColor.textTertiary)
                    }
                    .padding(KonoSpace.m)
                    .background(KonoColor.surface)
                    .konoCardShape(KonoRadius.m)
                }
                .buttonStyle(.plain)
            }

            KonoButton(title: "إضافة عنوان جديد", icon: "plus", kind: .secondary, action: onAdd)
            Spacer()
        }
        .konoScreenPadding()
        .background(KonoColor.background)
        .task { await data.loadAccount() }
    }
}
