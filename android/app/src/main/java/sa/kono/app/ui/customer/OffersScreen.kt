package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.ExperimentalMaterial3Api
import sa.kono.app.ui.i18n.Text
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
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.SkeletonBlock
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoSpace

/** Every store currently running an offer, nearest first. */
private suspend fun loadOfferStores(): List<Store> {
    val all = listOf(VerticalKind.Restaurants, VerticalKind.Groceries, VerticalKind.Pharmacy)
        .flatMap { AppData.stores(it, "recommended", freeDelivery = false, openNow = false) }
        .filter { it.promoTagAr != null }
        .ifEmpty { AppData.allStores.filter { it.promoTagAr != null } }
    return all.sortedBy { it.distanceKm }.distinctBy { it.id }
}

/** The "العروض" tab — promo banners plus every store currently running an offer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(onStore: (Int) -> Unit, onDeals: (String) -> Unit, onKonoPlus: () -> Unit) {
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { stores = loadOfferStores(); loading = false }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        Text("العروض", style = KonoFont.LargeTitle, color = KonoColor.Espresso, modifier = Modifier.padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.m))
        PullToRefreshBox(refreshing, { scope.launch { refreshing = true; stores = loadOfferStores(); refreshing = false } }) {
            LazyColumn(
                Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(KonoSpace.xl),
            ) {
                item { PromoCarousel(AppData.promos) { if (it.titleAr.contains("كونو بلس")) onKonoPlus() else onDeals(it.titleAr) } }
                if (loading) {
                    items(4) { SkeletonBlock(104.dp, 360.dp, Modifier.padding(horizontal = KonoSpace.screen), 14.dp) }
                } else if (stores.isEmpty()) {
                    item { KonoEmptyState("لا توجد عروض الآن", icon = Icons.Filled.LocalOffer, message = "تابعنا — العروض الجديدة بتظهر هنا أول بأول.") }
                } else {
                    item { Text("متاجر عليها عروض الآن", style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.padding(horizontal = KonoSpace.screen)) }
                    items(stores, key = { it.id }) { s -> StoreRow(s, Modifier.padding(horizontal = KonoSpace.screen)) { onStore(s.id) } }
                }
            }
        }
    }
}

/** A plain titled list of stores (deals / nearby). */
@Composable
fun StoreListScreen(title: String, loading: Boolean, stores: List<Store>, empty: String, onBack: () -> Unit, onStore: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            KonoBackButton(onBack)
            Text(title, style = KonoFont.Title, color = KonoColor.TextPrimary)
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            if (loading) items(4) { SkeletonBlock(104.dp, 360.dp, radius = 14.dp) }
            else if (stores.isEmpty()) item { Text(empty, style = KonoFont.Callout, color = KonoColor.TextSecondary, modifier = Modifier.padding(top = KonoSpace.xxxl)) }
            else items(stores, key = { it.id }) { s -> StoreRow(s) { onStore(s.id) } }
        }
    }
}

@Composable
fun DealsListScreen(title: String, onBack: () -> Unit, onStore: (Int) -> Unit) {
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { stores = loadOfferStores(); loading = false }
    StoreListScreen(title, loading, stores, "لا توجد عروض حالياً", onBack, onStore)
}

@Composable
fun NearbyScreen(onBack: () -> Unit, onStore: (Int) -> Unit) {
    LaunchedEffect(Unit) { AppData.loadHome() }
    StoreListScreen("الأقرب إليك", false, AppData.allStores.sortedBy { it.distanceKm }, "لا توجد خدمات قريبة", onBack, onStore)
}
