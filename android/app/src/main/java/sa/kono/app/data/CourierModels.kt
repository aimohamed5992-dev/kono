package sa.kono.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

enum class JobStage(val api: String, val titleAr: String, val ctaAr: String) {
    Offered("offered", "عرض رحلة جديدة", "قبول الرحلة"),
    HeadingToStore("headingToStore", "متجه إلى المتجر", "وصلت المتجر"),
    AtStore("atStore", "وصلت المتجر", "استلمت الطلب"),
    PickedUp("pickedUp", "استلمت الطلب", "بدء التوصيل"),
    HeadingToCustomer("headingToCustomer", "متجه إلى العميل", "وصلت للعميل"),
    Arrived("arrived", "وصلت موقع العميل", "تأكيد التسليم"),
    Delivered("delivered", "تم التوصيل", "إنهاء");

    companion object { fun from(s: String) = entries.firstOrNull { it.api == s } ?: Offered }
}

@Serializable
data class CourierJob(
    val orderId: Int, val shortCode: String, val stage: String = "offered", val cta: String = "",
    val storeNameAr: String = "", val storeAddressAr: String = "", val storeLat: Double = 0.0, val storeLng: Double = 0.0,
    val customerNameAr: String = "", val dropoffAddressAr: String = "", val dropLat: Double = 0.0, val dropLng: Double = 0.0,
    val distanceKm: Double = 0.0, val estMinutes: Int = 0, val payout: Double = 0.0, val itemCount: Int = 0,
    val customerPhone: String = "",
) { val stageEnum get() = JobStage.from(stage) }

@Serializable
data class CourierProfile(
    val id: Int = 0, val name: String = "", val rating: Double = 0.0, val ratingCount: Int = 0, val trips: Int = 0,
    val vehicleAr: String = "", val plateAr: String = "", val photoUrl: String = "", val isOnline: Boolean = false,
    val acceptanceRate: Int = 0, val completionRate: Int = 0, val latitude: Double = 24.774, val longitude: Double = 46.738,
    val todayEarnings: Double = 0.0, val todayTrips: Int = 0, val todayDistanceKm: Double = 0.0, val onboardingStatus: String = "",
)

@Serializable data class CourierTrip(val shortCode: String, val whenAr: String = "", val amount: Double = 0.0, val rating: Double = 0.0)

@Serializable
data class CourierSummary(
    val todayEarnings: Double = 0.0, val todayTrips: Int = 0, val todayDistanceKm: Double = 0.0,
    val deliveryPart: Double = 0.0, val tipsPart: Double = 0.0, val bonusPart: Double = 0.0,
    val week: List<EarningsDay> = emptyList(), val recentTrips: List<CourierTrip> = emptyList(),
)

@Serializable data class CourierStatusBody(val isOnline: Boolean, val latitude: Double, val longitude: Double)

/** All courier-side state: online flag, the live offer, and the active job. Mirrors the iOS `CourierSession`. */
object CourierSession {
    var isOnline by mutableStateOf(false); private set
    var offer by mutableStateOf<CourierJob?>(null); private set
    var activeJob by mutableStateOf<CourierJob?>(null); private set
    var profile by mutableStateOf(CourierProfile()); private set
    var summary by mutableStateOf<CourierSummary?>(null); private set
    var loaded by mutableStateOf(false); private set

    suspend fun bootstrap() {
        runCatching { KonoApi.get<CourierProfile>("/api/courier/me") }.getOrNull()?.let { profile = it; loaded = true; if (it.isOnline) isOnline = true }
        runCatching { KonoApi.get<CourierJob>("/api/courier/jobs/active") }.getOrNull()?.let { activeJob = it; isOnline = true }
    }

    suspend fun ensureBootstrapped() { if (!loaded) bootstrap() }
    suspend fun refreshProfile() { runCatching { KonoApi.get<CourierProfile>("/api/courier/me") }.getOrNull()?.let { profile = it } }
    suspend fun loadSummary() { runCatching { KonoApi.get<CourierSummary>("/api/courier/summary") }.getOrNull()?.let { summary = it } }

    private suspend fun status(online: Boolean) {
        runCatching { KonoApi.post<CourierStatusBody, Unit>("/api/courier/status", CourierStatusBody(online, profile.latitude, profile.longitude)) }
    }

    suspend fun goOnline() { isOnline = true; status(true); pollOffers() }
    suspend fun goOffline() { isOnline = false; offer = null; status(false) }

    /** Looks for an open offer; called on a timer while online with no active job. */
    suspend fun pollOffers() {
        if (!isOnline || activeJob != null || offer != null) return
        runCatching { KonoApi.get<List<CourierJob>>("/api/courier/offers") }.getOrNull()?.firstOrNull()?.let { offer = it }
    }

    suspend fun accept() {
        val o = offer ?: return
        offer = null
        runCatching { KonoApi.post<CourierJob>("/api/courier/offers/${o.orderId}/accept") }.getOrNull()?.let { activeJob = it }
    }

    fun decline() { offer = null }

    suspend fun advance() {
        val j = activeJob ?: return
        val up = runCatching { KonoApi.post<CourierJob>("/api/courier/jobs/${j.orderId}/advance") }.getOrNull() ?: return
        if (up.stageEnum == JobStage.Delivered) { activeJob = null; refreshProfile(); loadSummary() } else activeJob = up
    }

    suspend fun orderDetail(id: Int): OrderDetail? = runCatching { KonoApi.get<OrderDetail>("/api/courier/jobs/$id") }.getOrNull()

    fun reset() { isOnline = false; offer = null; activeJob = null; profile = CourierProfile(); summary = null; loaded = false }
}
