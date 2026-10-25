import SwiftUI

/// The app's single gateway to remote data. Views read from here; on any network
/// failure it silently falls back to the bundled `Mock` content so the prototype
/// still demos offline.
@MainActor
@Observable
final class AppData {
    static let shared = AppData()

    private let api = KonoAPI.shared

    var online = false
    var lastError: String?
    var signedOut = false            // set by signOut(); RootFlow returns to entry

    // cached catalog
    var verticals: [Vertical] = Mock.verticals
    var promos: [PromoSlide] = Mock.promos
    var rails: [HomeRail] = []
    var stores: [Store] = Mock.stores

    // account
    var me: AuthDTO.User?
    var addresses: [Address] = []
    var payments: [PaymentMethod] = []
    var currentAddress: Address?

    struct HomeRail: Identifiable {
        var id: String { key }          // stable — recomputing the home feed must not churn ForEach identity
        let key: String
        let titleAr: String
        let subtitleAr: String?
        let style: String
        let stores: [Store]
    }

    private var geo: [String: String] {
        guard let a = currentAddress else { return [:] }
        return ["lat": String(a.latitude), "lng": String(a.longitude)]
    }

    // MARK: Auth

    /// The signed-in user's experience, derived from their account role.
    var role: AppRole { AppRole(apiRole: me?.role) ?? .customer }
    var isAuthenticated: Bool { get async { await api.isAuthenticated } }

    /// "" (not started) | under_review | approved | rejected
    var onboardingStatus: String { me?.onboardingStatus ?? "" }
    var needsOnboarding: Bool { role != .customer && onboardingStatus.isEmpty }

    // MARK: Merchant / courier onboarding

    @discardableResult
    func submitCourierOnboarding(_ ob: CourierOnboarding) async -> Bool {
        guard let state: OnboardingStateDTO =
                try? await api.post("/api/onboarding/courier", body: ob) else { return false }
        applyOnboarding(state)
        return true
    }

    @discardableResult
    func submitMerchantOnboarding(_ ob: MerchantOnboarding) async -> Bool {
        guard let state: OnboardingStateDTO =
                try? await api.post("/api/onboarding/merchant", body: ob) else { return false }
        applyOnboarding(state)
        return true
    }

    private func applyOnboarding(_ state: OnboardingStateDTO) {
        if var u = me { u.onboardingStatus = state.status; me = u }
        online = true
    }

    /// Seeded accounts used ONLY by the `KONO_ROLE` launch hook (screenshots / dev),
    /// never surfaced to real users.
    private let devCreds: [AppRole: (String, String)] = [
        .customer: ("+966551234567", "Kono@12345"),
        .merchant: ("+966500000001", "Kono@12345"),
        .courier:  ("+966560000001", "Kono@12345"),
    ]
    private var isDevRoleLaunch: Bool { RootFlow.launch.role != nil }

    /// Dev hook: after ordering, the `KONO_ROLE=merchant` launch shows that store.
    var demoMerchantStoreId: Int? = {
        let v = UserDefaults.standard.integer(forKey: "kono.demoMerchantStore")
        return v == 0 ? nil : v
    }() {
        didSet { UserDefaults.standard.set(demoMerchantStoreId ?? 0, forKey: "kono.demoMerchantStore") }
    }

    /// Restores a persisted session on launch. Returns whether a valid session was found.
    @discardableResult
    func restoreSession() async -> Bool {
        await api.restore()
        guard await api.isAuthenticated else { return false }
        guard let u: AuthDTO.User = try? await api.get("/api/auth/me") else {
            await api.logout()               // token no longer valid
            return false
        }
        me = u
        online = true
        signedOut = false
        if role == .customer { await loadAccount() }
        return true
    }

    /// Passwordless phone sign-in / sign-up (used by the OTP screen).
    /// `role`/`name` are only honoured when the phone is new (registration).
    func completePhoneAuth(phone: String, name: String? = nil, role: AppRole = .customer) async throws {
        let res = try await api.phoneLogin(phone: phone, name: name, role: role.apiName)
        me = res.user
        online = true
        signedOut = false
        if self.role == .customer { await loadAccount() }
    }

    /// Legacy convenience — a plain customer phone sign-in.
    func signIn(phone: String) async throws {
        try await completePhoneAuth(phone: phone, name: nil, role: .customer)
    }

    /// Makes sure a usable session exists before an experience loads its data.
    /// In normal use `RootFlow` has already signed the user in, so this just
    /// validates and (for the customer) loads the account. The only time it signs
    /// anyone in is the `KONO_ROLE` launch hook, which drops straight into an
    /// experience with no session for screenshots / development.
    func ensureSignedIn(role: AppRole) async {
        if await api.isAuthenticated {
            if me == nil { me = try? await api.get("/api/auth/me") }
            online = true
            if role == .customer, addresses.isEmpty { await loadAccount() }
            if !isDevRoleLaunch { return }
            // dev hook: allow re-targeting to a different demo role/store
            let current = me?.role.lowercased()
            if current == role.rawValue || current == "admin" {
                if !(role == .merchant && demoMerchantStoreId != nil) { return }
            }
        } else if !isDevRoleLaunch {
            return                              // RootFlow owns real auth
        }

        // ── dev/screenshot only from here ──────────────────────────────────
        if role == .merchant, let sid = demoMerchantStoreId,
           let res = try? await api.demoStoreMerchant(storeId: sid) {
            me = res.user; online = true; signedOut = false
            return
        }
        guard let creds = devCreds[role] else { return }
        do {
            let res = try await api.login(phone: creds.0, password: creds.1)
            me = res.user; online = true; signedOut = false
            if self.role == .customer { await loadAccount() }
        } catch {
            online = false
            lastError = (error as? LocalizedError)?.errorDescription
        }
    }

    func signOut() async {
        await api.logout()
        me = nil
        addresses = []
        payments = []
        currentAddress = nil
        rails = []
        online = false
        signedOut = true
    }

    // MARK: Account

    func loadAccount() async {
        async let a: [AddressDTO] = (try? api.get("/api/addresses")) ?? []
        async let p: [PaymentMethodDTO] = (try? api.get("/api/payment-methods")) ?? []
        let (addrDTOs, payDTOs) = await (a, p)
        addresses = addrDTOs.map(Wire.address)
        payments = payDTOs.map(Wire.payment)
        if currentAddress == nil || !addresses.contains(where: { $0.id == currentAddress?.id }) {
            currentAddress = addresses.first(where: { $0.isDefault }) ?? addresses.first
        }
    }

    @discardableResult
    func addAddress(labelAr: String, lineAr: String, cityAr: String, icon: String,
                    lat: Double, lng: Double, makeDefault: Bool) async -> Address? {
        struct Body: Encodable {
            let labelAr, lineAr, cityAr, icon: String
            let latitude, longitude: Double
            let makeDefault: Bool
        }
        do {
            let dto: AddressDTO = try await api.post("/api/addresses", body: Body(
                labelAr: labelAr, lineAr: lineAr, cityAr: cityAr, icon: icon,
                latitude: lat, longitude: lng, makeDefault: makeDefault))
            await loadAccount()
            let added = Wire.address(dto)
            if makeDefault || addresses.count == 1 { await selectAddress(added) }
            return added
        } catch {
            lastError = (error as? LocalizedError)?.errorDescription
            return nil
        }
    }

    func deleteAddress(_ a: Address) async {
        try? await api.postVoid("/api/addresses/\(a.apiId)", authed: true, method: "DELETE")
        await loadAccount()
    }

    func selectAddress(_ a: Address) async {
        currentAddress = a
        if a.apiId != 0 { try? await api.postVoid("/api/addresses/\(a.apiId)/default") }
        await loadHome()
    }

    func updateProfile(name: String, email: String?) async {
        struct Body: Encodable { let name: String; let email: String? }
        if let res: AuthDTO.User = try? await api.post("/api/auth/me", body: Body(name: name, email: email), method: "PUT") {
            me = res
        }
    }

    func toggleKonoPlus() async {
        if let res: AuthDTO.User = try? await api.post("/api/auth/kono-plus") {
            me = res
        }
    }

    @discardableResult
    func addPayment(labelAr: String, icon: String, detailAr: String?) async -> Bool {
        struct Body: Encodable { let labelAr, icon: String; let detailAr: String?; let makeDefault: Bool }
        let ok: EmptyResponse? = try? await api.post("/api/payment-methods",
            body: Body(labelAr: labelAr, icon: icon, detailAr: detailAr, makeDefault: false))
        await loadAccount()
        return ok != nil
    }

    func deletePayment(_ p: PaymentMethod) async {
        try? await api.postVoid("/api/payment-methods/\(p.apiId)", method: "DELETE")
        await loadAccount()
    }

    // MARK: Catalog

    func loadHome() async {
        do {
            let feed: HomeFeedDTO = try await api.get("/api/home", query: geo, authed: false)
            verticals = feed.verticals.map(Wire.vertical)
            promos = feed.promotions.map(Wire.promo)
            rails = feed.rails.map { r in
                HomeRail(key: r.key, titleAr: r.titleAr, subtitleAr: r.subtitleAr,
                         style: r.style, stores: r.stores.map(Wire.store))
            }
            let s: PagedDTO<StoreDTO> = try await api.get(
                "/api/stores", query: geo.merging(["pageSize": "100"]) { a, _ in a }, authed: false)
            stores = s.items.map(Wire.store)
            online = true
        } catch {
            online = false
            rails = []                // views fall back to their own Mock rails
        }
    }

    // MARK: Search history

    private let recentKey = "kono.recentSearches"
    var recentSearches: [String] = UserDefaults.standard.stringArray(forKey: "kono.recentSearches") ?? []

    /// Trending terms derived from the live catalog: the verticals the merchant
    /// enabled, plus the most common cuisines among nearby stores.
    var trendingSearches: [String] {
        var out = verticals.map { $0.kind.titleAr }
        let cuisines = stores
            .flatMap { $0.cuisineAr.components(separatedBy: " · ") }
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { $0.count >= 2 }
        let counts = Dictionary(cuisines.map { ($0, 1) }, uniquingKeysWith: +)
        out += counts.sorted { $0.value > $1.value }.map { $0.key }
        var seen = Set<String>()
        return out.filter { seen.insert($0).inserted && !$0.isEmpty }.prefix(10).map { $0 }
    }

    func recordSearch(_ raw: String) {
        let q = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard q.count >= 2 else { return }
        var list = recentSearches.filter { $0.caseInsensitiveCompare(q) != .orderedSame }
        list.insert(q, at: 0)
        recentSearches = Array(list.prefix(8))
        UserDefaults.standard.set(recentSearches, forKey: recentKey)
    }

    func clearRecentSearches() {
        recentSearches = []
        UserDefaults.standard.removeObject(forKey: recentKey)
    }

    func stores(kind: VerticalKind, sort: String, freeDelivery: Bool, openNow: Bool) async -> [Store] {
        do {
            let res: PagedDTO<StoreDTO> = try await api.get("/api/stores", query: geo.merging([
                "kind": kind.apiName, "sort": sort,
                "freeDelivery": String(freeDelivery), "openNow": String(openNow),
                "pageSize": "50",
            ]) { a, _ in a }, authed: false)
            return res.items.map(Wire.store)
        } catch {
            return Mock.stores(for: kind)
        }
    }

    func storeDetail(_ store: Store) async -> (Store, [MenuCategory]) {
        guard store.apiId != 0 else { return (store, Mock.menu(for: store)) }
        do {
            let d: StoreDetailDTO = try await api.get(
                "/api/stores/\(store.apiId)", query: geo, authed: false)
            return (Wire.store(detail: d), d.menu.map(Wire.menuCategory))
        } catch {
            return (store, Mock.menu(for: store))
        }
    }

    func search(_ q: String, scope: String) async -> ([Store], [(Store, Product)]) {
        do {
            let r: SearchResultsDTO = try await api.get(
                "/api/search", query: geo.merging(["q": q, "scope": scope]) { a, _ in a }, authed: false)
            let stores = r.stores.map(Wire.store)
            let byId = Dictionary(uniqueKeysWithValues: (stores + self.stores).map { ($0.apiId, $0) })
            let items = r.items.map { hit -> (Store, Product) in
                let store = byId[hit.storeId] ?? Store(apiId: hit.storeId, name: hit.storeName,
                    kind: .restaurants, cuisineAr: "", rating: 0, ratingCount: 0, etaMinutes: 0,
                    deliveryFee: 0, minOrder: 0, distanceKm: 0, heroAsset: hit.imageUrl, logoAsset: "")
                let p = Product(apiId: hit.productId, nameAr: hit.nameAr, descAr: "",
                                price: hit.price, imageAsset: hit.imageUrl)
                return (store, p)
            }
            return (stores, items)
        } catch {
            return ([], [])
        }
    }

    // MARK: Orders (customer)

    func placeOrder(cart: CartStore, address: Address, payment: String,
                    tip: Double, scheduledFor: String?) async throws -> Order {
        guard let store = cart.store, store.apiId != 0 else {
            throw APIError.server(status: 400, message: "المتجر غير متصل بالخادم")
        }
        let body = PlaceOrderBody(
            storeId: store.apiId,
            items: cart.lines.map {
                CartItemBody(productId: $0.product.apiId, quantity: $0.quantity,
                             optionIds: $0.selectedOptionIds, noteAr: $0.note.isEmpty ? nil : $0.note)
            },
            addressLabelAr: address.labelAr, addressLineAr: address.lineAr,
            addressLat: address.latitude, addressLng: address.longitude,
            isDelivery: true, paymentLabelAr: payment, tip: tip,
            scheduledForAr: scheduledFor, promoCode: cart.promoApplied ? "KONO20" : nil, noteAr: nil)
        let dto: OrderDTO = try await api.post("/api/orders", body: body)
        // Demo: the merchant experience now represents the store this order went to.
        demoMerchantStoreId = store.apiId
        return Wire.order(dto)
    }

    func myOrders(status: String) async -> [Order] {
        do {
            let dtos: [OrderDTO] = try await api.get("/api/orders", query: ["status": status])
            return dtos.map(Wire.order)
        } catch {
            return status == "past" ? Mock.pastOrders : []
        }
    }

    func refreshOrder(_ order: Order) async -> Order {
        guard order.apiId != 0 else { return order }
        do {
            let dto: OrderDTO = try await api.get("/api/orders/\(order.apiId)")
            return Wire.order(dto)
        } catch {
            return order
        }
    }

    // MARK: Merchant

    func merchantTickets(state: String?) async -> [MerchantTicket] {
        do {
            var q: [String: String] = [:]
            if let state { q["state"] = state }
            let dtos: [MerchantTicketDTO] = try await api.get("/api/merchant/tickets", query: q)
            return dtos.map(Wire.ticket)
        } catch {
            return Mock.merchantTickets()
        }
    }

    func merchantAction(_ ticket: MerchantTicket, _ action: String) async -> MerchantTicket? {
        guard ticket.apiId != 0 else { return nil }
        let path = action == "accept" ? "accept" : action == "reject" ? "reject" : "advance"
        do {
            let dto: MerchantTicketDTO = try await api.post("/api/merchant/tickets/\(ticket.apiId)/\(path)")
            return Wire.ticket(dto)
        } catch { return nil }
    }

    func merchantMenu() async -> [MenuItemStatus] {
        do {
            let dtos: [MerchantMenuItemDTO] = try await api.get("/api/merchant/menu")
            return dtos.map(Wire.menuItem)
        } catch {
            return Mock.merchantMenuStatus()
        }
    }

    /// Creates (productId == nil) or updates a menu item. Returns the saved row, or nil offline.
    func saveMenuItem(productId: Int?, nameAr: String, descAr: String, price: Double,
                      inStock: Bool, imageUrl: String? = nil,
                      categoryAr: String = "أصناف") async -> MenuItemStatus? {
        let body = MerchantMenuUpsertBody(nameAr: nameAr, descAr: descAr, price: price,
                                          inStock: inStock, imageUrl: imageUrl, categoryAr: categoryAr)
        do {
            let dto: MerchantMenuItemDTO
            if let productId {
                dto = try await api.post("/api/merchant/menu/\(productId)", body: body, method: "PUT")
            } else {
                dto = try await api.post("/api/merchant/menu", body: body)
            }
            return Wire.menuItem(dto)
        } catch { return nil }
    }

    func merchantReports() async -> MerchantReportsDTO? {
        try? await api.get("/api/merchant/reports")
    }

    func merchantStore() async -> MerchantProfile? {
        guard let dto: MerchantStoreDTO = try? await api.get("/api/merchant/store") else { return nil }
        return Wire.merchantStore(dto)
    }

    func updateMerchantStore(name: String? = nil, cuisineAr: String? = nil, addressAr: String? = nil,
                             openingHoursAr: String? = nil, ibanLast4: String? = nil) async -> MerchantProfile? {
        let body = MerchantStoreUpdateBody(name: name, cuisineAr: cuisineAr, addressAr: addressAr,
                                           openingHoursAr: openingHoursAr, ibanLast4: ibanLast4)
        guard let dto: MerchantStoreDTO = try? await api.post("/api/merchant/store", body: body, method: "PATCH")
        else { return nil }
        return Wire.merchantStore(dto)
    }

    func merchantStatus(isOnline: Bool? = nil, isBusy: Bool? = nil, prepMinutes: Int? = nil) async {
        try? await api.postVoid("/api/merchant/status",
                                body: MerchantStatusBody(isOnline: isOnline, isBusy: isBusy,
                                                         prepMinutes: prepMinutes))
    }

    // MARK: Courier

    func courierProfile() async -> CourierProfile? {
        guard let dto: CourierProfileDTO = try? await api.get("/api/courier/me") else { return nil }
        return Wire.courierProfile(dto)
    }

    func courierSummary() async -> CourierSummaryDTO? {
        try? await api.get("/api/courier/summary")
    }

    func courierStatus(online: Bool, lat: Double, lng: Double) async {
        try? await api.postVoid("/api/courier/status",
                                body: CourierStatusBody(isOnline: online, latitude: lat, longitude: lng))
    }

    func courierOffers() async -> [CourierJob] {
        do {
            let dtos: [CourierJobDTO] = try await api.get("/api/courier/offers")
            return dtos.map(Wire.job)
        } catch { return [] }
    }

    func courierActiveJob() async -> CourierJob? {
        do {
            let dto: CourierJobDTO = try await api.get("/api/courier/jobs/active")
            return Wire.job(dto)
        } catch { return nil }
    }

    func courierAccept(_ orderId: Int) async -> CourierJob? {
        do {
            let dto: CourierJobDTO = try await api.post("/api/courier/offers/\(orderId)/accept")
            return Wire.job(dto)
        } catch { return nil }
    }

    func courierAdvance(_ orderId: Int) async -> CourierJob? {
        do {
            let dto: CourierJobDTO = try await api.post("/api/courier/jobs/\(orderId)/advance")
            return Wire.job(dto)
        } catch { return nil }
    }

    // MARK: Order detail (merchant + courier)

    func merchantOrderDetail(orderId: Int) async -> OrderDetailDTO? {
        try? await api.get("/api/merchant/tickets/\(orderId)")
    }

    func courierOrderDetail(orderId: Int) async -> OrderDetailDTO? {
        try? await api.get("/api/courier/jobs/\(orderId)")
    }

    // MARK: Chat

    func chatThreads() async -> [ChatThreadDTO] {
        (try? await api.get("/api/chat/threads")) ?? []
    }

    func chatTargets(orderId: Int) async -> OrderChatTargetsDTO? {
        try? await api.get("/api/chat/order/\(orderId)/targets")
    }

    func openChat(orderId: Int, target: String) async throws -> ChatConversationDTO {
        try await api.post("/api/chat/order/\(orderId)/\(target)", body: EmptyBody())
    }

    func chatConversation(_ id: Int) async throws -> ChatConversationDTO {
        try await api.get("/api/chat/\(id)")
    }

    func chatMessages(conversationId: Int, afterId: Int) async -> [ChatMessageDTO] {
        (try? await api.get("/api/chat/\(conversationId)/messages", query: ["afterId": String(afterId)])) ?? []
    }

    func sendChat(conversationId: Int, _ body: SendMessageBody) async throws -> ChatMessageDTO {
        try await api.post("/api/chat/\(conversationId)/messages", body: body)
    }

    func markChatRead(_ conversationId: Int) async {
        try? await api.postVoid("/api/chat/\(conversationId)/read")
    }

    func uploadChatMedia(dataUri: String, kind: String) async throws -> String {
        let res: ChatUploadResponseDTO = try await api.post(
            "/api/chat/upload", body: ChatUploadBody(dataUri: dataUri, kind: kind))
        return res.url
    }

    func startChatCall(conversationId: Int) async throws -> CallDTO {
        try await api.post("/api/chat/\(conversationId)/call", body: StartCallBody())
    }

    func endChatCall(callId: Int, status: String, duration: Int) async {
        try? await api.postVoid("/api/chat/calls/\(callId)/end",
                                body: EndCallBody(status: status, durationSec: duration))
    }
}

struct EmptyBody: Encodable {}

extension VerticalKind {
    var apiName: String {
        switch self {
        case .restaurants: return "Restaurants"
        case .groceries: return "Groceries"
        case .pharmacy: return "Pharmacy"
        case .sweets: return "Sweets"
        case .flowers: return "Flowers"
        case .water: return "Water"
        }
    }
    init(apiName: String) {
        switch apiName {
        case "Groceries": self = .groceries
        case "Pharmacy": self = .pharmacy
        case "Sweets": self = .sweets
        case "Flowers": self = .flowers
        case "Water": self = .water
        default: self = .restaurants
        }
    }
}
