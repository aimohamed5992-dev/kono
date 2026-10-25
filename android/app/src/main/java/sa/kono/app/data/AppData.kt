package sa.kono.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single gateway between the UI and the backend (mirrors the iOS `AppData`).
 * Observable via Compose state; screens read `me`/`role` and call the suspend functions.
 */
object AppData {
    // ── customer catalog / account state ──
    var verticals by mutableStateOf<List<VerticalDto>>(emptyList()); private set
    var promos by mutableStateOf<List<PromotionDto>>(emptyList()); private set
    var rails by mutableStateOf<List<HomeRailDto>>(emptyList()); private set
    var addresses by mutableStateOf<List<Address>>(emptyList()); private set
    var payments by mutableStateOf<List<PaymentMethod>>(emptyList()); private set
    var currentAddress by mutableStateOf<Address?>(null); private set
    var allStores by mutableStateOf<List<Store>>(emptyList()); private set
    var homeLoaded by mutableStateOf(false); private set

    var me by mutableStateOf<AuthUser?>(null)
        private set
    var online by mutableStateOf(true)
        private set

    val role: AppRole get() = AppRole.fromApi(me?.role)

    /** Validates a persisted session. True when the user can go straight into the app. */
    suspend fun restoreSession(): Boolean {
        if (!KonoApi.isAuthenticated) return false
        return try {
            me = KonoApi.get<AuthUser>("/api/auth/me")
            online = true
            true
        } catch (e: ApiError.Offline) {
            online = false
            false
        } catch (e: ApiError) {
            KonoApi.logout()
            false
        }
    }

    /** Passwordless phone sign-in / sign-up. `role`/`name` only apply when the phone is new. */
    suspend fun completePhoneAuth(phone: String, name: String? = null, role: AppRole = AppRole.Customer) {
        val res = KonoApi.phoneLogin(phone, name, role)
        me = res.user
        online = true
    }

    fun replaceMe(u: AuthUser) { me = u }

    fun signOut() {
        KonoApi.logout()
        me = null
        addresses = emptyList(); payments = emptyList(); currentAddress = null
        rails = emptyList(); promos = emptyList(); verticals = emptyList(); homeLoaded = false
        MerchantSession.reset()
        CourierSession.reset()
    }

    /** Distance-ranking origin for catalog calls. */
    private val geo: Map<String, String>
        get() = currentAddress?.let { mapOf("lat" to it.latitude.toString(), "lng" to it.longitude.toString()) } ?: emptyMap()

    // ── account ──
    suspend fun loadAccount() {
        addresses = runCatching { KonoApi.get<List<Address>>("/api/addresses") }.getOrDefault(addresses)
        payments = runCatching { KonoApi.get<List<PaymentMethod>>("/api/payment-methods") }.getOrDefault(payments)
        if (currentAddress == null || addresses.none { it.id == currentAddress?.id }) {
            currentAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
        }
    }

    suspend fun addAddress(labelAr: String, lineAr: String, cityAr: String, icon: String, lat: Double, lng: Double, makeDefault: Boolean): Address? =
        try {
            val dto: Address = KonoApi.post("/api/addresses", AddressBody(labelAr, lineAr, cityAr, icon, lat, lng, makeDefault))
            loadAccount()
            if (makeDefault || addresses.size == 1) selectAddress(dto)
            dto
        } catch (e: ApiError) { null }

    suspend fun selectAddress(a: Address) {
        currentAddress = a
        runCatching { KonoApi.post<Unit>("/api/addresses/${a.id}/default") }
        loadHome()
    }

    // ── catalog ──
    /** Home feed, scoped to the chosen address (server applies an 8 km radius). */
    suspend fun loadHome() {
        try {
            val feed: HomeFeedDto = KonoApi.get("/api/home", geo, authed = false)
            verticals = feed.verticals
            promos = feed.promotions
            rails = feed.rails
            allStores = runCatching {
                KonoApi.get<Paged<Store>>("/api/stores", geo + mapOf("pageSize" to "100"), authed = false).items
            }.getOrDefault(allStores)
            online = true
        } catch (e: ApiError) {
            online = false
        }
        homeLoaded = true
    }

    suspend fun stores(kind: VerticalKind, sort: String, freeDelivery: Boolean, openNow: Boolean): List<Store> =
        try {
            KonoApi.get<Paged<Store>>(
                "/api/stores",
                geo + mapOf("kind" to kind.api, "sort" to sort, "freeDelivery" to freeDelivery.toString(),
                    "openNow" to openNow.toString(), "pageSize" to "50"),
                authed = false,
            ).items
        } catch (e: ApiError) { emptyList() }
}

// ── store detail / search / recent searches ──
suspend fun AppData.storeDetail(id: Int): StoreDetail? =
    try { KonoApi.get<StoreDetail>("/api/stores/$id", currentGeo(), authed = false) } catch (e: ApiError) { null }

suspend fun AppData.search(q: String, scope: String): SearchResults =
    try { KonoApi.get<SearchResults>("/api/search", currentGeo() + mapOf("q" to q, "scope" to scope), authed = false) }
    catch (e: ApiError) { SearchResults() }

fun AppData.currentGeo(): Map<String, String> =
    currentAddress?.let { mapOf("lat" to it.latitude.toString(), "lng" to it.longitude.toString()) } ?: emptyMap()

/** Trending terms derived from the live catalog: the enabled verticals plus the most common cuisines. */
val AppData.trendingSearches: List<String>
    get() {
        val out = verticals.map { it.kindEnum.titleAr }.toMutableList()
        val counts = allStores.flatMap { it.cuisineAr.split(" · ") }.map { it.trim() }.filter { it.length >= 2 }
            .groupingBy { it }.eachCount()
        out += counts.entries.sortedByDescending { it.value }.map { it.key }
        return out.filter { it.isNotEmpty() }.distinct().take(10)
    }

// ── orders ──
/** Places the order; the server re-prices it. Throws [ApiError] on failure. */
suspend fun AppData.placeOrder(cart: CartStore, address: Address, payment: String, tip: Double, scheduledFor: String?): OrderDto {
    val store = cart.store ?: throw ApiError.Server(400, "السلة فارغة")
    val body = PlaceOrderBody(
        storeId = store.id,
        items = cart.lines.map { CartItemBody(it.product.id, it.quantity, it.selectedOptionIds, it.note.ifBlank { null }) },
        addressLabelAr = address.labelAr, addressLineAr = address.lineAr,
        addressLat = address.latitude, addressLng = address.longitude,
        isDelivery = true, paymentLabelAr = payment, tip = tip, scheduledForAr = scheduledFor,
        promoCode = if (cart.promoApplied) "KONO20" else null,
    )
    return KonoApi.post("/api/orders", body)
}

suspend fun AppData.myOrders(status: String): List<OrderDto> =
    try { KonoApi.get<List<OrderDto>>("/api/orders", mapOf("status" to status)) } catch (e: ApiError) { emptyList() }

suspend fun AppData.order(id: Int): OrderDto? =
    try { KonoApi.get<OrderDto>("/api/orders/$id") } catch (e: ApiError) { null }
