import SwiftUI

@Observable
final class MerchantSession {
    var isOnline = true
    var prepMinutes = 15
    var tickets: [MerchantTicket] = Mock.merchantTickets()
    var menu: [MenuItemStatus] = Mock.merchantMenuStatus()
    var online = false

    var profile = Mock.merchant
    var reports: MerchantReportsDTO?
    var loadedStore = false

    func tickets(in state: TicketState) -> [MerchantTicket] {
        tickets.filter { $0.state == state }
    }

    var newCount: Int { tickets(in: .incoming).count }
    var activeCount: Int { tickets.filter { $0.state == .preparing || $0.state == .ready }.count }

    @MainActor func bootstrap() async {
        await AppData.shared.ensureSignedIn(role: .merchant)
        await loadStore()
        await refresh()
        await loadMenu()
        await loadReports()
    }

    /// Runs `bootstrap()` at most once — for tabs opened without the dashboard.
    @MainActor func ensureBootstrapped() async {
        guard !loadedStore else { return }
        await bootstrap()
    }

    @MainActor func loadStore() async {
        if let s = await AppData.shared.merchantStore() { applyStore(s); loadedStore = true }
    }

    /// Persists edits to the store profile (name / cuisine / address / hours / IBAN).
    @MainActor func updateStore(name: String? = nil, cuisineAr: String? = nil, addressAr: String? = nil,
                                openingHoursAr: String? = nil, ibanLast4: String? = nil) async {
        if let s = await AppData.shared.updateMerchantStore(
            name: name, cuisineAr: cuisineAr, addressAr: addressAr,
            openingHoursAr: openingHoursAr, ibanLast4: ibanLast4) {
            applyStore(s)
        }
    }

    @MainActor private func applyStore(_ s: MerchantProfile) {
        let today = profile.todayRevenue, orders = profile.todayOrders
        profile = s
        profile.todayRevenue = today          // keep report-derived figures
        profile.todayOrders = orders
        isOnline = s.isOnline
        prepMinutes = s.prepMinutes
        online = true
    }

    @MainActor func loadReports() async {
        if let r = await AppData.shared.merchantReports() { reports = r; online = true }
    }

    @MainActor func refresh() async {
        let fresh = await AppData.shared.merchantTickets(state: nil)
        if fresh.contains(where: { $0.apiId != 0 }) {
            tickets = fresh
            online = true
        }
    }

    @MainActor func loadMenu() async {
        await AppData.shared.ensureSignedIn(role: .merchant)
        let fresh = await AppData.shared.merchantMenu()
        if fresh.contains(where: { $0.product.apiId != 0 }) {
            menu = fresh
            online = true
        }
    }

    func advance(_ ticket: MerchantTicket) {
        let wasIncoming = ticket.state == .incoming
        applyLocalAdvance(ticket)
        guard ticket.apiId != 0 else { return }
        Task { @MainActor in
            if let updated = await AppData.shared.merchantAction(ticket, wasIncoming ? "accept" : "advance") {
                replace(updated)
            }
        }
    }

    func reject(_ ticket: MerchantTicket) {
        tickets.removeAll { $0.id == ticket.id }
        guard ticket.apiId != 0 else { return }
        Task { @MainActor in _ = await AppData.shared.merchantAction(ticket, "reject") }
    }

    private func applyLocalAdvance(_ ticket: MerchantTicket) {
        guard let i = tickets.firstIndex(where: { $0.id == ticket.id }) else { return }
        let order: [TicketState] = [.incoming, .preparing, .ready, .handedOver, .completed]
        if let idx = order.firstIndex(of: tickets[i].state), idx < order.count - 1 {
            tickets[i].state = order[idx + 1]
        }
    }

    private func replace(_ updated: MerchantTicket) {
        if let i = tickets.firstIndex(where: { $0.apiId == updated.apiId }) {
            tickets[i] = updated
        }
    }

    func toggleAvailability(_ item: MenuItemStatus) {
        guard let i = menu.firstIndex(where: { $0.id == item.id }) else { return }
        menu[i].available.toggle()
        persist(menu[i])
    }

    /// Saves an edited item back to the server and updates the local row.
    func updateItem(_ item: MenuItemStatus, nameAr: String, price: Double,
                    descAr: String, available: Bool) {
        guard let i = menu.firstIndex(where: { $0.id == item.id }) else { return }
        menu[i].product.nameAr = nameAr
        menu[i].product.price = price
        menu[i].product.descAr = descAr
        menu[i].available = available
        persist(menu[i])
    }

    /// Appends a brand-new item (optimistically) and creates it on the server.
    func addItem(nameAr: String, price: Double, descAr: String) {
        let row = MenuItemStatus(
            product: Product(nameAr: nameAr, descAr: descAr, price: price, imageAsset: "menu_placeholder"),
            available: true, soldToday: 0)
        menu.insert(row, at: 0)
        Task { @MainActor in
            if let saved = await AppData.shared.saveMenuItem(
                productId: nil, nameAr: nameAr, descAr: descAr, price: price, inStock: true) {
                if let i = menu.firstIndex(where: { $0.id == row.id }) { menu[i] = saved }
            }
        }
    }

    private func persist(_ row: MenuItemStatus) {
        let pid = row.product.apiId
        guard pid != 0 else { return }
        Task { @MainActor in
            _ = await AppData.shared.saveMenuItem(
                productId: pid, nameAr: row.product.nameAr, descAr: row.product.descAr,
                price: row.product.price, inStock: row.available)
        }
    }

    func setOnline(_ v: Bool) {
        isOnline = v
        profile.isOnline = v
        Task { @MainActor in await AppData.shared.merchantStatus(isOnline: v) }
    }

    func setBusy(_ v: Bool) {
        profile.isBusy = v
        Task { @MainActor in await AppData.shared.merchantStatus(isBusy: v) }
    }

    func setPrep(_ minutes: Int) {
        prepMinutes = minutes
        profile.prepMinutes = minutes
        Task { @MainActor in await AppData.shared.merchantStatus(prepMinutes: minutes) }
    }

    var todayRevenue: Double { reports?.days.last?.amount ?? profile.todayRevenue }
    var todayOrders: Int { reports?.days.last?.trips ?? profile.todayOrders }
}

private struct MerchantSessionKey: EnvironmentKey {
    static let defaultValue = MerchantSession()
}

extension EnvironmentValues {
    var merchant: MerchantSession {
        get { self[MerchantSessionKey.self] }
        set { self[MerchantSessionKey.self] = newValue }
    }
}

enum MerchantRoute: Hashable {
    case ticket(MerchantTicket)
    case menuItem(MenuItemStatus)
}
