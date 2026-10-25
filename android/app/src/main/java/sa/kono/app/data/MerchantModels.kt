package sa.kono.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

// ── wire models ─────────────────────────────────────────────────────────────

enum class TicketState(val api: String, val titleAr: String) {
    Incoming("incoming", "جديد"), Preparing("preparing", "قيد التجهيز"), Ready("ready", "جاهز"),
    HandedOver("handedOver", "تم التسليم"), Completed("completed", "مكتمل");

    companion object { fun from(s: String) = entries.firstOrNull { it.api == s } ?: Incoming }
}

@Serializable
data class MerchantTicket(
    val id: Int, val shortCode: String, val customerNameAr: String = "", val state: String = "incoming",
    val placedAt: String = "", val acceptDeadline: String = "", val isDelivery: Boolean = true,
    val courierNameAr: String? = null, val noteAr: String? = null, val itemCount: Int = 0, val total: Double = 0.0,
    val items: List<OrderItemDto> = emptyList(),
) { val stateEnum get() = TicketState.from(state) }

@Serializable
data class MerchantMenuItem(
    val productId: Int, val nameAr: String, val descAr: String = "", val price: Double,
    val imageUrl: String = "", val inStock: Boolean = true, val soldCount: Int = 0, val categoryAr: String = "",
)

@Serializable
data class MenuUpsertBody(
    val nameAr: String, val descAr: String, val price: Double, val inStock: Boolean,
    val imageUrl: String? = null, val categoryAr: String = "أصناف",
)

@Serializable
data class MerchantStore(
    val id: Int, val name: String, val kind: String = "", val cuisineAr: String = "", val rating: Double = 0.0,
    val ratingCount: Int = 0, val prepMinutes: Int = 15, val isBusy: Boolean = false, val isClosed: Boolean = false,
    val heroImageUrl: String = "", val addressAr: String = "", val cityAr: String = "", val openingHoursAr: String = "",
    val ibanLast4: String = "", val deliveryRadiusKm: Double = 8.0, val menuItemCount: Int = 0,
    val ownerNameAr: String = "", val ownerPhone: String = "", val onboardingStatus: String = "", val logoUrl: String = "",
)

@Serializable
data class StoreUpdateBody(
    val name: String? = null, val cuisineAr: String? = null, val addressAr: String? = null,
    val openingHoursAr: String? = null, val ibanLast4: String? = null,
)

@Serializable data class MerchantStatusBody(val isOnline: Boolean? = null, val isBusy: Boolean? = null, val prepMinutes: Int? = null)

@Serializable data class EarningsDay(val weekdayAr: String, val amount: Double, val trips: Int)
@Serializable data class TopItem(val nameAr: String, val count: Int, val revenue: Double = 0.0)

@Serializable
data class MerchantReports(
    val totalSales: Double = 0.0, val orderCount: Int = 0, val avgOrderValue: Double = 0.0, val rating: Double = 0.0,
    val prepMinutes: Int = 0, val days: List<EarningsDay> = emptyList(), val topItems: List<TopItem> = emptyList(),
)

@Serializable
data class OrderEvent(val stage: String, val at: String = "", val note: String? = null)

@Serializable
data class OrderDetail(
    val id: Int, val shortCode: String, val stage: String = "", val stageAr: String = "", val displayStage: String = "",
    val placedAt: String = "", val deliveredAt: String? = null, val isDelivery: Boolean = true,
    val paymentLabelAr: String = "", val scheduledForAr: String? = null, val noteAr: String? = null,
    val customerNameAr: String = "", val customerPhone: String = "",
    val addressLabelAr: String = "", val addressLineAr: String = "", val addressLat: Double = 0.0, val addressLng: Double = 0.0,
    val storeId: Int = 0, val storeNameAr: String = "", val storeCuisineAr: String = "", val storeAddressAr: String = "",
    val storePhone: String = "", val storeLat: Double = 0.0, val storeLng: Double = 0.0, val storeImageUrl: String = "",
    val storeEtaMinutes: Int = 0, val courier: CourierMini? = null, val courierPayout: Double = 0.0, val distanceKm: Double = 0.0,
    val subtotal: Double = 0.0, val deliveryFee: Double = 0.0, val serviceFee: Double = 0.0, val discount: Double = 0.0,
    val tip: Double = 0.0, val total: Double = 0.0, val itemCount: Int = 0,
    val items: List<OrderItemDto> = emptyList(), val events: List<OrderEvent> = emptyList(),
)

// ── session ─────────────────────────────────────────────────────────────────

/** All merchant-side state (tickets, menu, store, reports). Mirrors the iOS `MerchantSession`. */
object MerchantSession {
    val tickets = mutableStateListOf<MerchantTicket>()
    val menu = mutableStateListOf<MerchantMenuItem>()
    var store by mutableStateOf<MerchantStore?>(null); private set
    var reports by mutableStateOf<MerchantReports?>(null); private set
    var isOnline by mutableStateOf(true); private set
    var prepMinutes by mutableStateOf(15); private set
    var loaded by mutableStateOf(false); private set

    fun ticketsIn(s: TicketState) = tickets.filter { it.stateEnum == s }
    val newCount get() = ticketsIn(TicketState.Incoming).size
    val todayRevenue get() = reports?.days?.lastOrNull()?.amount ?: 0.0
    val todayOrders get() = reports?.days?.lastOrNull()?.trips ?: 0

    suspend fun bootstrap() {
        loadStore(); refreshTickets(); loadMenu(); loadReports(); loaded = true
    }

    suspend fun ensureBootstrapped() { if (!loaded) bootstrap() }

    suspend fun loadStore() {
        runCatching { KonoApi.get<MerchantStore>("/api/merchant/store") }.getOrNull()?.let { apply(it) }
    }

    private fun apply(s: MerchantStore) { store = s; isOnline = !s.isClosed; prepMinutes = s.prepMinutes }

    suspend fun updateStore(body: StoreUpdateBody) {
        runCatching { KonoApi.post<StoreUpdateBody, MerchantStore>("/api/merchant/store", body, method = "PATCH") }.getOrNull()?.let { apply(it) }
    }

    suspend fun refreshTickets() {
        val fresh = runCatching { KonoApi.get<List<MerchantTicket>>("/api/merchant/tickets") }.getOrNull() ?: return
        tickets.clear(); tickets.addAll(fresh)
    }

    suspend fun loadMenu() {
        val fresh = runCatching { KonoApi.get<List<MerchantMenuItem>>("/api/merchant/menu") }.getOrNull() ?: return
        menu.clear(); menu.addAll(fresh)
    }

    suspend fun loadReports() { runCatching { KonoApi.get<MerchantReports>("/api/merchant/reports") }.getOrNull()?.let { reports = it } }

    /** accept (new) / advance (preparing → ready → handed over). Returns the updated ticket. */
    suspend fun advance(t: MerchantTicket) {
        val path = if (t.stateEnum == TicketState.Incoming) "accept" else "advance"
        runCatching { KonoApi.post<MerchantTicket>("/api/merchant/tickets/${t.id}/$path") }.getOrNull()?.let { up ->
            val i = tickets.indexOfFirst { it.id == up.id }; if (i >= 0) tickets[i] = up
        }
    }

    suspend fun reject(t: MerchantTicket) {
        tickets.removeAll { it.id == t.id }
        runCatching { KonoApi.post<MerchantTicket>("/api/merchant/tickets/${t.id}/reject") }
    }

    suspend fun orderDetail(id: Int): OrderDetail? = runCatching { KonoApi.get<OrderDetail>("/api/merchant/tickets/$id") }.getOrNull()

    /** Creates (productId == null) or updates a menu item and refreshes the local row. */
    suspend fun saveItem(productId: Int?, nameAr: String, descAr: String, price: Double, inStock: Boolean, imageUrl: String? = null) {
        val body = MenuUpsertBody(nameAr, descAr, price, inStock, imageUrl)
        val saved = runCatching {
            if (productId != null) KonoApi.post<MenuUpsertBody, MerchantMenuItem>("/api/merchant/menu/$productId", body, method = "PUT")
            else KonoApi.post<MenuUpsertBody, MerchantMenuItem>("/api/merchant/menu", body)
        }.getOrNull() ?: return
        val i = menu.indexOfFirst { it.productId == saved.productId }
        if (i >= 0) menu[i] = saved else menu.add(0, saved)
    }

    suspend fun toggleAvailability(item: MerchantMenuItem) =
        saveItem(item.productId, item.nameAr, item.descAr, item.price, !item.inStock, null)

    private suspend fun status(body: MerchantStatusBody) { runCatching { KonoApi.post<MerchantStatusBody, Unit>("/api/merchant/status", body) } }
    suspend fun setOnline(v: Boolean) { isOnline = v; status(MerchantStatusBody(isOnline = v)) }
    suspend fun setBusy(v: Boolean) { store = store?.copy(isBusy = v); status(MerchantStatusBody(isBusy = v)) }
    suspend fun setPrep(m: Int) { prepMinutes = m; status(MerchantStatusBody(prepMinutes = m)) }

    fun reset() { tickets.clear(); menu.clear(); store = null; reports = null; loaded = false }
}
