package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.Store
import sa.kono.app.data.VerticalKind
import sa.kono.app.ui.components.KonoSearchBar
import sa.kono.app.ui.components.SkeletonBlock
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoSpace

/** Everything the home screen can navigate to. */
class HomeActions(
    val openStore: (Store) -> Unit,
    val openVertical: (VerticalKind) -> Unit,
    val openSearch: (String?) -> Unit,
    val openDeals: (String) -> Unit,
    val openNearby: () -> Unit,
    val openNotifications: () -> Unit,
    val addAddress: () -> Unit,
    val manageAddresses: () -> Unit,
)

private val cuisines = listOf(
    "برجر" to "food_burger_double", "بيتزا" to "store_pizza", "دجاج" to "food_chicken",
    "قهوة" to "store_coffee", "شاورما" to "store_shawarma", "حلا" to "store_sweets",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(actions: HomeActions) {
    var showAddresses by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { AppData.loadHome() }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { scope.launch { refreshing = true; AppData.loadAccount(); AppData.loadHome(); refreshing = false } },
        modifier = Modifier.fillMaxSize().background(KonoColor.Background),
    ) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.xxl),
        ) {
            item(key = "top") {
                HomeTopBar(
                    AppData.currentAddress, onAddress = { showAddresses = true }, onNotifications = actions.openNotifications,
                    modifier = Modifier.padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s),
                )
            }
            item(key = "search") {
                KonoSearchBar("", {}, Modifier.padding(horizontal = KonoSpace.screen), onTap = { actions.openSearch(null) })
            }
            item(key = "verticals") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.l),
                ) {
                    val kinds = AppData.verticals.map { it.kindEnum }.ifEmpty { VerticalKind.entries }
                    kinds.forEach { VerticalTile(it) { actions.openVertical(it) } }
                }
            }
            item(key = "promos") {
                PromoCarousel(AppData.promos) { actions.openDeals(it.titleAr) }
            }
            if (!AppData.homeLoaded) {
                item(key = "skeleton") {
                    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xl)) {
                        repeat(2) {
                            Column(Modifier.padding(horizontal = KonoSpace.screen), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                                SkeletonBlock(20.dp, 160.dp)
                                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                                    repeat(2) { SkeletonBlock(200.dp, 280.dp, radius = 20.dp) }
                                }
                            }
                        }
                    }
                }
            } else {
                items(AppData.rails, key = { it.key }) { rail ->
                    HomeRail(
                        rail.titleAr, subtitle = rail.subtitleAr,
                        onSeeAll = when (rail.key) {
                            "featured", "popular" -> ({ actions.openVertical(VerticalKind.Restaurants) })
                            "groceries" -> ({ actions.openVertical(VerticalKind.Groceries) })
                            "pharmacy" -> ({ actions.openVertical(VerticalKind.Pharmacy) })
                            "sweets" -> ({ actions.openVertical(VerticalKind.Sweets) })
                            "deals" -> ({ actions.openDeals("عروض قوية اليوم") })
                            "nearby" -> ({ actions.openNearby() })
                            else -> null
                        },
                    ) {
                        rail.stores.forEach { s ->
                            val open = { actions.openStore(s) }
                            when (rail.style) {
                                "featured" -> FeaturedStoreCard(s, open)
                                "deal" -> DealCard(s, open)
                                "circle" -> StoreCircleCard(s, open)
                                "quick" -> QuickStoreCard(s, KonoColor.Info, open)
                                "poster" -> PosterStoreCard(s, open)
                                else -> StoreCardCompact(s, open)
                            }
                        }
                    }
                }
                item(key = "cuisines") {
                    HomeRail("تصفّح حسب التصنيف") {
                        cuisines.forEach { (title, asset) -> CuisineTile(title, asset) { actions.openSearch(title) } }
                    }
                }
            }
        }
    }

    if (showAddresses) {
        AddressSwitcherSheet(
            onDismiss = { showAddresses = false },
            onAdd = { showAddresses = false; actions.addAddress() },
            onManage = { showAddresses = false; actions.manageAddresses() },
        )
    }
}
