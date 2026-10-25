package sa.kono.app.data

import kotlinx.serialization.Serializable

/** The three experiences of the app. [api] is the role name the backend uses. */
enum class AppRole(val api: String, val titleAr: String) {
    Customer("Customer", "عميل"),
    Merchant("Merchant", "متجر"),
    Courier("Courier", "مندوب");

    companion object {
        fun fromApi(s: String?) = entries.firstOrNull { it.api.equals(s, ignoreCase = true) } ?: Customer
    }
}

@Serializable
data class AuthUser(
    val id: Int,
    val phone: String,
    val name: String,
    val email: String? = null,
    val role: String,
    val konoPlus: Boolean = false,
    val walletBalance: Double = 0.0,
    val points: Int = 0,
    val onboardingStatus: String = "",
)

@Serializable
data class AuthResponse(val accessToken: String, val refreshToken: String, val user: AuthUser)

@Serializable
data class PhoneLoginRequest(val phone: String, val name: String? = null, val role: String? = null)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ProblemDetails(val title: String? = null)

// ── catalog ─────────────────────────────────────────────────────────────────

/** The service lines Kono offers: food plus q-commerce. [api] is the backend's kind name. */
enum class VerticalKind(val api: String, val titleAr: String, val subtitleAr: String, val accent: androidx.compose.ui.graphics.Color) {
    Restaurants("Restaurants", "مطاعم", "وجبتك المفضلة توصلك سريع", sa.kono.app.ui.theme.KonoColor.Restaurants),
    Groceries("Groceries", "بقالة", "مقاضي البيت من بندة ولولو والتميمي", sa.kono.app.ui.theme.KonoColor.Groceries),
    Pharmacy("Pharmacy", "صيدليات", "دواءك يوصلك خلال دقائق", sa.kono.app.ui.theme.KonoColor.Pharmacy),
    Sweets("Sweets", "حلويات", "حلا ومناسبات", sa.kono.app.ui.theme.KonoColor.Sweets),
    Flowers("Flowers", "ورود", "بوكيهات وهدايا", sa.kono.app.ui.theme.KonoColor.Flowers),
    Water("Water", "مياه", "قوارير ومياه معدنية", sa.kono.app.ui.theme.KonoColor.Water);

    /** Name of the bundled category photo (`cat_restaurants`, …). */
    val asset get() = "cat_${name.lowercase()}"

    companion object {
        fun fromApi(s: String?) = entries.firstOrNull { it.api.equals(s, ignoreCase = true) } ?: Restaurants
    }
}

@Serializable
data class VerticalDto(
    val id: Int, val kind: String, val titleAr: String, val subtitleAr: String,
    val icon: String = "", val imageUrl: String = "", val sortOrder: Int = 0,
) { val kindEnum get() = VerticalKind.fromApi(kind) }

@Serializable
data class PromotionDto(
    val id: Int, val titleAr: String, val subtitleAr: String, val imageUrl: String,
    val backgroundHex: String = "", val sortOrder: Int = 0,
) {
    /** Call-to-action copy, derived from the headline like the iOS app does. */
    val ctaAr: String get() = when {
        titleAr.contains("كونو بلس") || titleAr.contains("اشترك") -> "اشترك الآن"
        titleAr.contains("مقاضي") || titleAr.contains("تسوّق") || titleAr.contains("سوبرماركت") -> "تسوّق الآن"
        else -> "اطلب الآن"
    }
}

@Serializable
data class Store(
    val id: Int, val name: String, val kind: String, val cuisineAr: String = "",
    val rating: Double = 0.0, val ratingCount: Int = 0, val etaMinutes: Int = 0,
    val deliveryFee: Double = 0.0, val minOrder: Double = 0.0, val distanceKm: Double = 0.0,
    val isBusy: Boolean = false, val isClosed: Boolean = false, val promoTagAr: String? = null,
    val heroImageUrl: String = "", val logoImageUrl: String = "", val priceLevel: Int = 2,
    val tags: List<String> = emptyList(),
) {
    val kindEnum get() = VerticalKind.fromApi(kind)
    val deliveryFeeText get() = if (deliveryFee == 0.0) "توصيل مجاني" else "${deliveryFee.clean()} ر.س توصيل"
}

@Serializable
data class HomeRailDto(
    val key: String, val titleAr: String, val subtitleAr: String? = null,
    val style: String = "compact", val stores: List<Store> = emptyList(),
)

@Serializable
data class HomeFeedDto(
    val verticals: List<VerticalDto> = emptyList(),
    val promotions: List<PromotionDto> = emptyList(),
    val rails: List<HomeRailDto> = emptyList(),
)

@Serializable
data class Paged<T>(
    val items: List<T> = emptyList(), val page: Int = 1, val pageSize: Int = 0,
    val total: Int = 0, val totalPages: Int = 1, val hasMore: Boolean = false,
)

@Serializable
data class Address(
    val id: Int, val labelAr: String, val lineAr: String, val cityAr: String = "",
    val icon: String = "mappin", val latitude: Double, val longitude: Double, val isDefault: Boolean = false,
)

@Serializable
data class AddressBody(
    val labelAr: String, val lineAr: String, val cityAr: String, val icon: String,
    val latitude: Double, val longitude: Double, val makeDefault: Boolean,
)

@Serializable
data class PaymentMethod(
    val id: Int, val labelAr: String, val icon: String = "", val detailAr: String? = null, val isDefault: Boolean = false,
)

/** `12.0` → `12`, `12.5` → `12.50` (same as the iOS `Double.clean`). */
fun Double.clean(): String = if (this % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", this) else String.format(java.util.Locale.US, "%.2f", this)

// ── store detail / menu / search ────────────────────────────────────────────

@Serializable
data class ProductOption(val id: Int, val nameAr: String, val priceDelta: Double = 0.0)

@Serializable
data class ProductOptionGroup(
    val id: Int, val titleAr: String, val required: Boolean = false, val multiSelect: Boolean = false,
    val choices: List<ProductOption> = emptyList(),
)

@Serializable
data class Product(
    val id: Int, val storeId: Int = 0, val menuCategoryId: Int = 0, val nameAr: String, val descAr: String = "",
    val price: Double, val oldPrice: Double? = null, val imageUrl: String = "", val isPopular: Boolean = false,
    val calories: Int? = null, val unitAr: String? = null, val inStock: Boolean = true,
    val optionGroups: List<ProductOptionGroup> = emptyList(),
)

@Serializable
data class MenuCategory(val id: Int, val nameAr: String, val sortOrder: Int = 0, val items: List<Product> = emptyList())

@Serializable
data class StoreDetail(
    val id: Int, val name: String, val kind: String, val cuisineAr: String = "",
    val rating: Double = 0.0, val ratingCount: Int = 0, val etaMinutes: Int = 0,
    val deliveryFee: Double = 0.0, val minOrder: Double = 0.0, val distanceKm: Double = 0.0,
    val isBusy: Boolean = false, val isClosed: Boolean = false, val promoTagAr: String? = null,
    val heroImageUrl: String = "", val logoImageUrl: String = "", val priceLevel: Int = 2,
    val prepMinutes: Int = 0, val latitude: Double = 0.0, val longitude: Double = 0.0,
    val addressAr: String = "", val cityAr: String = "", val tags: List<String> = emptyList(),
    val menu: List<MenuCategory> = emptyList(),
) {
    fun toStore() = Store(
        id, name, kind, cuisineAr, rating, ratingCount, etaMinutes, deliveryFee, minOrder, distanceKm,
        isBusy, isClosed, promoTagAr, heroImageUrl, logoImageUrl, priceLevel, tags,
    )
}

@Serializable
data class ItemHit(
    val productId: Int, val storeId: Int, val storeName: String, val nameAr: String, val price: Double, val imageUrl: String = "",
)

@Serializable
data class SearchResults(val stores: List<Store> = emptyList(), val items: List<ItemHit> = emptyList())

// ── orders ──────────────────────────────────────────────────────────────────

/** The customer-facing 6-step ladder; the API's `displayStage` maps onto it. */
enum class OrderStage(val titleAr: String) {
    Placed("تم استلام الطلب"), Confirmed("أكد المطعم الطلب"), Preparing("يتم تجهيز طلبك"),
    ReadyForPickup("الطلب جاهز للاستلام"), OnTheWay("المندوب في الطريق إليك"), Delivered("تم التوصيل");

    companion object {
        fun fromDisplay(s: String?) = when (s) {
            "confirmed" -> Confirmed; "preparing" -> Preparing; "readyForPickup" -> ReadyForPickup
            "onTheWay" -> OnTheWay; "delivered" -> Delivered; else -> Placed
        }
    }
}

@Serializable
data class OrderItemDto(
    val id: Int, val productId: Int? = null, val nameAr: String, val imageUrl: String = "",
    val unitPrice: Double, val quantity: Int, val selectedOptionsAr: String = "", val noteAr: String = "", val lineTotal: Double,
)

@Serializable
data class CourierMini(
    val id: Int, val name: String, val rating: Double = 0.0, val trips: Int = 0, val vehicleAr: String = "",
    val plateAr: String = "", val photoUrl: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0,
)

@Serializable
data class OrderDto(
    val id: Int, val shortCode: String, val stage: String, val displayStage: String,
    val storeId: Int, val storeName: String, val storeImageUrl: String = "", val storeEtaMinutes: Int = 0,
    val placedAt: String = "", val deliveredAt: String? = null, val isDelivery: Boolean = true,
    val paymentLabelAr: String = "", val scheduledForAr: String? = null, val noteAr: String? = null,
    val addressLabelAr: String = "", val addressLineAr: String = "", val addressLat: Double = 0.0, val addressLng: Double = 0.0,
    val subtotal: Double = 0.0, val deliveryFee: Double = 0.0, val serviceFee: Double = 0.0, val discount: Double = 0.0,
    val tip: Double = 0.0, val total: Double = 0.0, val itemCount: Int = 0,
    val items: List<OrderItemDto> = emptyList(), val courier: CourierMini? = null,
) {
    val stageEnum get() = OrderStage.fromDisplay(displayStage)
}

@Serializable
data class CartItemBody(val productId: Int, val quantity: Int, val optionIds: List<Int>, val noteAr: String? = null)

@Serializable
data class PlaceOrderBody(
    val storeId: Int, val items: List<CartItemBody>,
    val addressLabelAr: String, val addressLineAr: String, val addressLat: Double, val addressLng: Double,
    val isDelivery: Boolean, val paymentLabelAr: String, val tip: Double,
    val scheduledForAr: String? = null, val promoCode: String? = null, val noteAr: String? = null,
)
