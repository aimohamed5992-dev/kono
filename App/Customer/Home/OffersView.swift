import SwiftUI

/// The "العروض" tab — promo banners plus every store currently running an offer.
struct OffersView: View {
    @State private var data = AppData.shared
    @State private var path = NavigationPath()
    @State private var stores: [Store] = []
    @State private var loading = true

    private var slides: [PromoSlide] { data.online && !data.promos.isEmpty ? data.promos : Mock.promos }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(alignment: .leading, spacing: 0) {
                Text("العروض")
                    .font(KonoFont.largeTitle).foregroundStyle(KonoColor.espresso)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .konoScreenPadding()
                    .padding(.top, KonoSpace.s)
                    .padding(.bottom, KonoSpace.m)

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: KonoSpace.xl) {
                    PromoCarousel(slides: slides) { slide in
                        if slide.titleAr.contains("كونو بلس") { path.append(CustomerRoute.konoPlus) }
                        else { path.append(CustomerRoute.deals(title: slide.titleAr)) }
                    }

                    if loading {
                        ForEach(0..<5, id: \.self) { _ in StoreCardSkeleton().konoScreenPadding() }
                    } else if stores.isEmpty {
                        KonoEmptyState(illustration: "illo_offer", symbol: "tag.slash",
                                       title: "لا توجد عروض الآن",
                                       message: "تابعنا — العروض الجديدة بتظهر هنا أول بأول.")
                            .padding(.top, KonoSpace.xl)
                    } else {
                        Text("متاجر عليها عروض الآن")
                            .font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                            .konoScreenPadding()
                        ForEach(stores) { store in
                            NavigationLink(value: CustomerRoute.store(store)) { StoreRow(store: store) }
                                .buttonStyle(.plain)
                                .konoScreenPadding()
                        }
                    }
                    }
                    .padding(.bottom, 130)
                }
                .scrollIndicators(.hidden)
                .refreshable { await load() }
            }
            .background(KonoColor.background)
            .navigationBarHidden(true)
            .customerDestinations()
            .task {
                await data.ensureSignedIn(role: .customer)
                await load()
            }
        }
    }

    private func load() async {
        let r = await data.stores(kind: .restaurants, sort: "recommended", freeDelivery: false, openNow: false)
        let g = await data.stores(kind: .groceries, sort: "recommended", freeDelivery: false, openNow: false)
        let p = await data.stores(kind: .pharmacy, sort: "recommended", freeDelivery: false, openNow: false)
        var all = (r + g + p).filter { $0.promoTagAr != nil }
        if all.isEmpty {
            all = (data.online ? data.stores : Mock.stores).filter { $0.promoTagAr != nil }
        }
        // de-dupe by apiId / name, keep nearest first
        var seen = Set<String>()
        stores = all
            .sorted { $0.distanceKm < $1.distanceKm }
            .filter { seen.insert($0.apiId != 0 ? "\($0.apiId)" : $0.name).inserted }
        loading = false
    }
}
