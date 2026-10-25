import SwiftUI

struct StoreDetailView: View {
    let store: Store
    @Environment(\.cart) private var cart
    @Environment(\.dismiss) private var dismiss

    @State private var categories: [MenuCategory] = []
    @State private var selectedCategory = 0
    @State private var scrollOffset: CGFloat = 0
    @State private var sheetProduct: Product?
    @State private var showCart = false
    @State private var fav: Bool

    init(store: Store) {
        self.store = store
        _fav = State(initialValue: store.isFavorite)
    }

    private let heroHeight: CGFloat = 240

    var body: some View {
        ZStack(alignment: .top) {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 0) {
                        hero
                        infoCard
                            .konoScreenPadding()
                            .padding(.top, -30)
                        if categories.isEmpty {
                            VStack(spacing: KonoSpace.m) {
                                ForEach(0..<4, id: \.self) { _ in
                                    MenuRowSkeleton().konoScreenPadding()
                                }
                            }
                            .padding(.top, KonoSpace.l)
                        } else {
                            categoryBar
                                .padding(.top, KonoSpace.m)
                            menu(proxy: proxy)
                        }
                    }
                    .background(GeometryReader { g in
                        Color.clear.preference(key: ScrollOffsetKey.self,
                                               value: g.frame(in: .named("storeScroll")).minY)
                    })
                    .padding(.bottom, 140)
                }
                .coordinateSpace(name: "storeScroll")
                .onPreferenceChange(ScrollOffsetKey.self) { scrollOffset = $0 }
            }
            .scrollIndicators(.hidden)

            topBar
        }
        .background(KonoColor.background)
        .navigationBarHidden(true)
        .hidesKonoTabBar()
        .ignoresSafeArea(edges: .top)
        .task {
            guard categories.isEmpty else { return }
            let (_, menu) = await AppData.shared.storeDetail(store)
            withAnimation(KonoMotion.spring) { categories = menu }
        }
        .overlay(alignment: .bottom) {
            if cart.itemCount > 0 && cart.store?.id == store.id {
                CartBar(count: cart.itemCount, total: cart.total) { showCart = true }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(KonoMotion.spring, value: cart.itemCount)
        .sheet(item: $sheetProduct) { p in
            ProductSheet(product: p, store: store)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .navigationDestination(isPresented: $showCart) {
            CartView()
        }
    }

    private var collapsed: Bool { scrollOffset < -heroHeight + 90 }

    private var hero: some View {
        GeometryReader { g in
            let stretch = max(0, g.frame(in: .named("storeScroll")).minY)
            AssetImage(name: store.heroAsset, fallbackSymbol: store.kind.systemIcon,
                       fallbackTint: store.kind.accent)
                .frame(width: g.size.width, height: heroHeight + stretch)
                .clipped()
                .offset(y: -stretch)
                .overlay(
                    LinearGradient(colors: [.black.opacity(0.3), .clear, .black.opacity(0.15)],
                                   startPoint: .top, endPoint: .bottom)
                        .frame(height: heroHeight + stretch)
                        .offset(y: -stretch)
                )
        }
        .frame(height: heroHeight)
    }

    private var topBar: some View {
        HStack(spacing: KonoSpace.m) {
            circleButton("chevron.right") { dismiss() }
            if collapsed {
                Text(store.name).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    .transition(.opacity)
            }
            Spacer()
            circleButton(fav ? "heart.fill" : "heart", tint: fav ? KonoColor.danger : KonoColor.brown) {
                #if canImport(UIKit)
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                #endif
                withAnimation(KonoMotion.bouncy) { fav.toggle() }
            }
            ShareLink(item: "جرّب \(store.name) على كونو — \(store.cuisineAr)") {
                Image(systemName: "square.and.arrow.up")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(KonoColor.brown)
                    .frame(width: 38, height: 38)
                    .background(.ultraThinMaterial, in: Circle())
                    .konoShadow(.sm)
            }
        }
        .konoScreenPadding()
        .padding(.top, 56)
        .padding(.bottom, KonoSpace.s)
        .background(collapsed ? AnyView(KonoColor.surface.ignoresSafeArea(edges: .top)) : AnyView(Color.clear))
        .animation(KonoMotion.ease, value: collapsed)
    }

    private var infoCard: some View {
        VStack(alignment: .leading, spacing: KonoSpace.m) {
            HStack(spacing: KonoSpace.m) {
                LogoBubble(asset: store.logoAsset, symbol: store.kind.systemIcon, size: 52, name: store.name)
                VStack(alignment: .leading, spacing: 2) {
                    Text(store.name).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                    Text(store.cuisineAr).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
            }

            HStack(spacing: 0) {
                infoPill("star.fill", "\(store.rating.formatted(.number.precision(.fractionLength(1))))",
                         "\(store.ratingCount)+ تقييم", KonoColor.rating)
                Divider().frame(height: 34)
                infoPill("clock.fill", "\(store.etaMinutes) د", "وقت التوصيل", KonoColor.brown)
                Divider().frame(height: 34)
                infoPill("bicycle", store.deliveryFee == 0 ? "مجاني" : "\(store.deliveryFee.clean) ر.س",
                         "التوصيل", KonoColor.brown)
            }

            if let promo = store.promoTagAr {
                HStack(spacing: KonoSpace.s) {
                    Image(systemName: "tag.fill").font(.system(size: 12))
                    Text(promo).font(KonoFont.subheadline)
                }
                .foregroundStyle(KonoColor.danger)
                .padding(.horizontal, KonoSpace.m).padding(.vertical, KonoSpace.s)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(KonoColor.danger.opacity(0.1), in: RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
            }
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.l)
        .konoShadow(.md)
    }

    private func infoPill(_ icon: String, _ value: String, _ label: String, _ tint: Color) -> some View {
        VStack(spacing: 3) {
            HStack(spacing: 3) {
                Image(systemName: icon).font(.system(size: 11)).foregroundStyle(tint)
                Text(value).font(.cairo(14, .heavy))
                    .foregroundStyle(KonoColor.textPrimary)
            }
            Text(label).font(.cairo(10, .medium))
                .foregroundStyle(KonoColor.textSecondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var categoryBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: KonoSpace.s) {
                ForEach(Array(categories.enumerated()), id: \.element.id) { i, cat in
                    KonoChip(title: cat.nameAr, selected: selectedCategory == i) {
                        withAnimation(KonoMotion.snappy) { selectedCategory = i }
                    }
                    .id("chip-\(i)")
                }
            }
            .konoScreenPadding()
        }
    }

    private func menu(proxy: ScrollViewProxy) -> some View {
        LazyVStack(alignment: .leading, spacing: KonoSpace.xl, pinnedViews: []) {
            ForEach(Array(categories.enumerated()), id: \.element.id) { ci, cat in
                VStack(alignment: .leading, spacing: KonoSpace.m) {
                    Text(cat.nameAr).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                        .konoScreenPadding()
                        .id("cat-\(ci)")
                    ForEach(cat.items) { item in
                        MenuItemRow(product: item, store: store,
                                    quantity: cart.quantity(of: item)) {
                            if item.options.isEmpty {
                                withAnimation(KonoMotion.spring) {
                                    cart.add(CartLine(product: item, quantity: 1), from: store)
                                }
                                #if canImport(UIKit)
                                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                #endif
                            } else {
                                sheetProduct = item
                            }
                        }
                        .konoScreenPadding()
                    }
                }
            }
        }
        .padding(.top, KonoSpace.m)
        .onChange(of: selectedCategory) { _, new in
            withAnimation(KonoMotion.spring) { proxy.scrollTo("cat-\(new)", anchor: .top) }
        }
    }

    private func circleButton(_ symbol: String, tint: Color = KonoColor.brown,
                              action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(tint)
                .frame(width: 38, height: 38)
                .background(.ultraThinMaterial, in: Circle())
                .konoShadow(.sm)
        }
        .buttonStyle(.plain)
    }
}

struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

// MARK: - Menu item row

struct MenuItemRow: View {
    let product: Product
    let store: Store
    var quantity: Int = 0
    var onAdd: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: KonoSpace.m) {
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 5) {
                    if product.isPopular {
                        KonoTag(text: "الأكثر طلباً", systemImage: "flame.fill", color: KonoColor.danger)
                    }
                    Text(product.nameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                }
                Text(product.descAr).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    .lineLimit(2)
                HStack(spacing: KonoSpace.s) {
                    RiyalText(amount: product.price, font: KonoFont.price)
                    if let old = product.oldPrice, old > product.price {
                        Text(old.clean).font(KonoFont.footnote).strikethrough()
                            .foregroundStyle(KonoColor.textTertiary)
                    }
                    if let cal = product.calories {
                        Text("· \(cal) سعرة").font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                    }
                }
            }
            Spacer(minLength: 0)
            ZStack(alignment: .bottomTrailing) {
                AssetImage(name: product.imageAsset,
                           fallbackSymbol: "takeoutbag.and.cup.and.straw.fill",
                           fallbackTint: store.kind.accent)
                    .frame(width: 96, height: 96)
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    .opacity(product.inStock ? 1 : 0.4)

                Button(action: onAdd) {
                    Image(systemName: quantity > 0 ? "\(quantity).circle.fill" : "plus")
                        .font(.system(size: quantity > 0 ? 22 : 14, weight: .bold))
                        .foregroundStyle(quantity > 0 ? KonoColor.brown : KonoColor.textOnBrown)
                        .frame(width: 30, height: 30)
                        .background(quantity > 0 ? KonoColor.yellow : KonoColor.brown, in: Circle())
                        .overlay(Circle().strokeBorder(KonoColor.surface, lineWidth: 2))
                        .konoShadow(.sm)
                }
                .buttonStyle(.plain)
                .offset(x: 6, y: 6)
                .disabled(!product.inStock)
            }
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
        .opacity(product.inStock ? 1 : 0.7)
    }
}

// MARK: - Floating cart bar

struct CartBar: View {
    let count: Int
    let total: Double
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: KonoSpace.m) {
                ZStack {
                    Image(systemName: "bag.fill").font(.system(size: 16, weight: .bold))
                    Text("\(count)")
                        .font(.cairo(10, .black))
                        .foregroundStyle(KonoColor.brown)
                        .frame(width: 16, height: 16)
                        .background(KonoColor.yellow, in: Circle())
                        .offset(x: 12, y: -10)
                }
                .foregroundStyle(KonoColor.textOnBrown)
                Text("عرض السلة").font(KonoFont.button).foregroundStyle(KonoColor.textOnBrown)
                Spacer()
                RiyalText(amount: total, font: KonoFont.button, color: KonoColor.textOnBrown)
            }
            .padding(.horizontal, KonoSpace.l)
            .frame(height: KonoSize.buttonHeight)
            .background(KonoColor.brown, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            .konoShadow(.lg)
        }
                .pressableScale(0.98)
        .konoScreenPadding()
        .padding(.bottom, KonoSpace.s)
    }
}
