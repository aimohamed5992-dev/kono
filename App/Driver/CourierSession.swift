import SwiftUI

@Observable
final class CourierSession {
    var isOnline = false
    var offer: CourierJob?
    var activeJob: CourierJob?
    var profile = Mock.courierProfile
    var backed = false           // talking to the API
    var loadedProfile = false

    private var lat: Double { profile.latitude }
    private var lng: Double { profile.longitude }

    var todayEarnings: Double { profile.todayEarnings }
    var todayTrips: Int { profile.todayTrips }

    @MainActor func bootstrap() async {
        await AppData.shared.ensureSignedIn(role: .courier)
        backed = AppData.shared.online
        if let fresh = await AppData.shared.courierProfile() {
            profile = fresh
            loadedProfile = true
            if fresh.isOnline { isOnline = true }
        }
        if let job = await AppData.shared.courierActiveJob() {
            activeJob = job
            isOnline = true
        }
    }

    /// Runs `bootstrap()` at most once — for tabs opened without the map tab.
    @MainActor func ensureBootstrapped() async {
        guard !loadedProfile else { return }
        await bootstrap()
    }

    /// Re-pulls the courier's server figures (earnings, trips, acceptance…).
    @MainActor func refreshProfile() async {
        if let fresh = await AppData.shared.courierProfile() { profile = fresh }
    }

    func goOnline() {
        isOnline = true
        Task { @MainActor in
            await AppData.shared.courierStatus(online: true, lat: lat, lng: lng)
            await pollOffers()
        }
    }

    func goOffline() {
        isOnline = false
        offer = nil
        Task { @MainActor in await AppData.shared.courierStatus(online: false, lat: lat, lng: lng) }
    }

    @MainActor private func pollOffers() async {
        guard isOnline, activeJob == nil else { return }
        if backed {
            let offers = await AppData.shared.courierOffers()
            if let first = offers.first {
                withAnimation(KonoMotion.spring) { offer = first }
                return
            }
        }
        // demo fallback
        try? await Task.sleep(nanoseconds: 1_200_000_000)
        if isOnline, activeJob == nil, offer == nil {
            withAnimation(KonoMotion.spring) { offer = Mock.courierOffer() }
        }
    }

    func accept() {
        guard let job = offer else { return }
        var j = job
        j.stage = .headingToStore
        withAnimation(KonoMotion.spring) { activeJob = j; offer = nil }
        guard backed, job.apiId != 0 else { return }
        Task { @MainActor in
            if let real = await AppData.shared.courierAccept(job.apiId) { activeJob = real }
        }
    }

    func declineOffer() {
        withAnimation(KonoMotion.spring) { offer = nil }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            await pollOffers()
        }
    }

    func advanceJob() {
        guard var j = activeJob else { return }
        if j.stage == .delivered { complete(); return }
        j.stage = j.stage.next
        withAnimation(KonoMotion.spring) { activeJob = j }
        guard backed, j.apiId != 0 else { return }
        Task { @MainActor in
            if let real = await AppData.shared.courierAdvance(j.apiId) {
                if real.stage == .delivered { complete() } else { activeJob = real }
            }
        }
    }

    private func complete() {
        profile.todayTrips += 1
        profile.todayEarnings += activeJob?.payout ?? 0
        withAnimation(KonoMotion.spring) { activeJob = nil }
        Task { @MainActor in
            await refreshProfile()          // authoritative today figures from the server
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            await pollOffers()
        }
    }
}

private struct CourierSessionKey: EnvironmentKey {
    static let defaultValue = CourierSession()
}

extension EnvironmentValues {
    var courier: CourierSession {
        get { self[CourierSessionKey.self] }
        set { self[CourierSessionKey.self] = newValue }
    }
}
