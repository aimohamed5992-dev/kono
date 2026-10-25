package sa.kono.app.ui.customer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.CartStore
import sa.kono.app.data.VerticalKind
import sa.kono.app.ui.components.KonoTabBar
import sa.kono.app.ui.components.KonoTabItem
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoSpace

private val tabs = listOf(
    KonoTabItem("الرئيسية", Icons.Outlined.Home, Icons.Filled.Home),
    KonoTabItem("العروض", Icons.Outlined.Sell, Icons.Filled.Sell),
    KonoTabItem("الطلبات", Icons.Outlined.ShoppingBag, Icons.Filled.ShoppingBag),
    KonoTabItem("حسابي", Icons.Outlined.Person, Icons.Filled.Person),
)

private enum class Gate { Checking, Location, App }

/**
 * Customer experience. Auth happens before we get here; this owns the one-time
 * "set your delivery location" step for a fresh account, then the tabs + navigation.
 */
@Composable
fun CustomerRoot(onSignOut: () -> Unit) {
    var gate by rememberSaveable { mutableStateOf(Gate.Checking) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppData.loadAccount()
        gate = if (AppData.addresses.isEmpty()) Gate.Location else Gate.App
    }

    AnimatedContent(
        gate, label = "customerGate",
        transitionSpec = {
            if (targetState == Gate.Location) (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith fadeOut()
            else fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
        },
    ) { g ->
        when (g) {
            Gate.Checking -> Box(Modifier.fillMaxSize().androidxBg())
            Gate.Location -> LocationSetupScreen(
                onConfirm = { label, line, city, icon, lat, lng ->
                    scope.launch { AppData.addAddress(label, line, city, icon, lat, lng, makeDefault = true); gate = Gate.App }
                },
                onBack = if (AppData.addresses.isNotEmpty()) ({ gate = Gate.App }) else null,
            )
            Gate.App -> CustomerNav(onSignOut)
        }
    }
}

private fun Modifier.androidxBg() = this.then(Modifier.background(KonoColor.Background))

@Composable
private fun CustomerNav(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    NavHost(
        nav, startDestination = "tabs",
        enterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
    ) {
        composable("tabs") {
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> CustomerHomeScreen(
                        HomeActions(
                            openStore = { nav.navigate("store/${it.id}") },
                            openVertical = { nav.navigate("vertical/${it.name}") },
                            openSearch = { nav.navigate("search?q=${android.net.Uri.encode(it ?: "")}") },
                            openDeals = { nav.navigate("deals/${android.net.Uri.encode(it)}") },
                            openNearby = { nav.navigate("nearby") },
                            openNotifications = { nav.navigate("notifications") },
                            addAddress = { nav.navigate("addAddress") },
                            manageAddresses = { nav.navigate("addresses") },
                        )
                    )
                    1 -> OffersScreen(
                        onStore = { nav.navigate("store/$it") },
                        onDeals = { nav.navigate("deals/${android.net.Uri.encode(it)}") },
                        onKonoPlus = { nav.navigate("konoPlus") },
                    )
                    2 -> OrdersScreen(onOrder = { nav.navigate("tracking/$it?justPlaced=false") })
                    else -> AccountScreen(AccountActions(
                        addresses = { nav.navigate("addresses") }, payments = { nav.navigate("payments") },
                        chats = { nav.navigate("chats") }, notifications = { nav.navigate("notifications") },
                        help = { nav.navigate("help") }, about = { nav.navigate("about") },
                        konoPlus = { nav.navigate("konoPlus") }, signOut = onSignOut,
                    ))
                }
                KonoTabBar(tabs, tab, { tab = it }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = KonoSpace.xs))
            }
        }
        composable("vertical/{kind}") { e ->
            val kind = VerticalKind.valueOf(e.arguments?.getString("kind") ?: "Restaurants")
            VerticalListScreen(kind, onBack = { nav.popBackStack() }, onStore = { nav.navigate("store/${it.id}") })
        }
        composable("addAddress") {
            LocationSetupScreen(
                onConfirm = { label, line, city, icon, lat, lng ->
                    scope.launch { AppData.addAddress(label, line, city, icon, lat, lng, makeDefault = true); nav.popBackStack() }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable("store/{id}") { e ->
            StoreDetailScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }, onCart = { nav.navigate("cart") })
        }
        composable("search?q={q}") { e ->
            SearchScreen(e.arguments?.getString("q")?.ifEmpty { null }, onBack = { nav.popBackStack() }, onStore = { nav.navigate("store/$it") })
        }
        composable("deals/{title}") { e ->
            DealsListScreen(e.arguments?.getString("title").orEmpty(), onBack = { nav.popBackStack() }, onStore = { nav.navigate("store/$it") })
        }
        composable("nearby") { NearbyScreen(onBack = { nav.popBackStack() }, onStore = { nav.navigate("store/$it") }) }
        composable("cart") { CartScreen(onBack = { nav.popBackStack() }, onCheckout = { nav.navigate("checkout") }) }
        composable("checkout") {
            CheckoutScreen(onBack = { nav.popBackStack() }, onPlaced = { id ->
                CartStore.clear()
                nav.navigate("tracking/$id?justPlaced=true") { popUpTo("tabs") }
            })
        }
        composable("tracking/{id}?justPlaced={jp}", arguments = listOf(
            androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType },
            androidx.navigation.navArgument("jp") { type = androidx.navigation.NavType.BoolType; defaultValue = false },
        )) { e ->
            TrackingScreen(e.arguments?.getInt("id") ?: 0, e.arguments?.getBoolean("jp") ?: false, onClose = { nav.popBackStack() }, onChat = { nav.navigate("chat/$it") })
        }
        composable("konoPlus") { KonoPlusScreen { nav.popBackStack() } }
        composable("notifications") { NotificationsScreen { nav.popBackStack() } }
        composable("addresses") { AddressesScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate("addAddress") }) }
        composable("payments") { PaymentsScreen { nav.popBackStack() } }
        composable("chats") { sa.kono.app.ui.chat.ChatListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("chat/$it") }) }
        composable("chat/{id}") { e -> sa.kono.app.ui.chat.ChatScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }) }
        composable("help") { SimpleInfoScreen("المساعدة والدعم", "فريق دعم كونو متاح ٢٤ ساعة. راسلنا عبر الدردشة أو اتصل على ٩٢٠٠٠٠٠٠٠.") { nav.popBackStack() } }
        composable("about") { SimpleInfoScreen("عن كونو", "كونو — منصة توصيل سعودية للأكل والمقاضي والصيدليات. الإصدار ١.٠") { nav.popBackStack() } }
    }
}
