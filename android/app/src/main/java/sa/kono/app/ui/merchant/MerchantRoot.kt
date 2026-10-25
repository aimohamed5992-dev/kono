package sa.kono.app.ui.merchant

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Storefront
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
import sa.kono.app.data.AppData
import sa.kono.app.data.MerchantSession
import sa.kono.app.ui.chat.ChatListScreen
import sa.kono.app.ui.chat.ChatScreen
import sa.kono.app.ui.components.KonoTabBar
import sa.kono.app.ui.components.KonoTabItem
import sa.kono.app.ui.customer.SimpleInfoScreen
import sa.kono.app.ui.theme.KonoSpace

/** Merchant experience: Orders · Menu · Reports · Store, plus ticket / menu-item / chat screens. */
@Composable
fun MerchantRoot(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { MerchantSession.bootstrap() }
    // keep tickets live while the app is open (new orders arrive without a push channel)
    LaunchedEffect(Unit) { while (true) { delay(5000); MerchantSession.refreshTickets() } }

    val tabs = listOf(
        KonoTabItem("الطلبات", Icons.Outlined.Inbox, Icons.Filled.Inbox, badge = MerchantSession.newCount),
        KonoTabItem("القائمة", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
        KonoTabItem("التقارير", Icons.Outlined.BarChart, Icons.Filled.BarChart),
        KonoTabItem("المتجر", Icons.Outlined.Storefront, Icons.Filled.Storefront),
    )

    NavHost(
        nav, startDestination = "tabs",
        enterTransition = { slideInHorizontally { -it / 3 } + fadeIn() }, exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() }, popExitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
    ) {
        composable("tabs") {
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> MerchantDashboard(onTicket = { nav.navigate("ticket/$it") })
                    1 -> MerchantMenuScreen(onItem = { nav.navigate("menuItem/$it") })
                    2 -> MerchantReportsScreen()
                    else -> MerchantStoreScreen(onChats = { nav.navigate("chats") }, onInfo = { t, b -> nav.navigate("info/${android.net.Uri.encode(t)}/${android.net.Uri.encode(b)}") }, onSignOut = onSignOut)
                }
                KonoTabBar(tabs, tab, { tab = it }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = KonoSpace.xs))
            }
        }
        composable("ticket/{id}") { e -> TicketDetailScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }, onChat = { nav.navigate("chat/$it") }) }
        composable("menuItem/{id}") { e -> MenuItemEditScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }) }
        composable("chats") { ChatListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("chat/$it") }) }
        composable("chat/{id}") { e -> ChatScreen(e.arguments?.getString("id")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() }) }
        composable("info/{title}/{body}") { e ->
            SimpleInfoScreen(e.arguments?.getString("title").orEmpty(), e.arguments?.getString("body").orEmpty()) { nav.popBackStack() }
        }
    }
}
