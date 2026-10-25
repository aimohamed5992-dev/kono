package sa.kono.app.ui.courier

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import sa.kono.app.data.CourierSession
import sa.kono.app.ui.chat.ChatListScreen
import sa.kono.app.ui.chat.ChatScreen
import sa.kono.app.ui.components.KonoTabBar
import sa.kono.app.ui.components.KonoTabItem
import sa.kono.app.ui.customer.ScreenTopBar
import sa.kono.app.ui.customer.SimpleInfoScreen
import sa.kono.app.ui.theme.KonoSpace

/** Courier experience: Trips (map) · Earnings · History · Profile. */
@Composable
fun CourierRoot(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { CourierSession.bootstrap() }
    // while online with no active trip, look for offers every few seconds
    LaunchedEffect(Unit) { while (true) { delay(4000); CourierSession.pollOffers() } }

    val tabs = listOf(
        KonoTabItem("الرحلات", Icons.Outlined.Map, Icons.Filled.Map),
        KonoTabItem("الأرباح", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
        KonoTabItem("السجل", Icons.Outlined.History, Icons.Filled.History),
        KonoTabItem("الملف", Icons.Outlined.Person, Icons.Filled.Person),
    )

    NavHost(
        nav, startDestination = "tabs",
        enterTransition = { slideInHorizontally { -it / 3 } + fadeIn() }, exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() }, popExitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
    ) {
        composable("tabs") {
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> CourierHomeScreen(onDetail = { nav.navigate("order/$it") }, onChat = { nav.navigate("chat/$it") })
                    1 -> CourierEarningsScreen()
                    2 -> CourierHistoryScreen()
                    else -> CourierProfileScreen(onChats = { nav.navigate("chats") }, onInfo = { t, b -> nav.navigate("info/${android.net.Uri.encode(t)}/${android.net.Uri.encode(b)}") }, onSignOut = onSignOut)
                }
                KonoTabBar(tabs, tab, { tab = it }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = KonoSpace.xs))
            }
        }
        composable("order/{id}") { e -> CourierOrderDetailScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }, onChat = { nav.navigate("chat/$it") }) }
        composable("chats") { ChatListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("chat/$it") }) }
        composable("chat/{id}") { e -> ChatScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }) }
        composable("info/{title}/{body}") { e -> SimpleInfoScreen(e.arguments?.getString("title").orEmpty(), e.arguments?.getString("body").orEmpty()) { nav.popBackStack() } }
    }
}
