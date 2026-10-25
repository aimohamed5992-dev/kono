package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sa.kono.app.data.AppData
import sa.kono.app.data.Store
import sa.kono.app.data.VerticalKind
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.SkeletonBlock
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoSpace

private enum class Sort(val api: String, val titleAr: String) {
    Recommended("recommended", "موصى به"), Fastest("fastest", "الأسرع"),
    Rating("rating", "الأعلى تقييماً"), Nearest("nearest", "الأقرب"),
}

@Composable
fun VerticalListScreen(kind: VerticalKind, onBack: () -> Unit, onStore: (Store) -> Unit) {
    var sort by remember { mutableStateOf(Sort.Recommended) }
    var freeDelivery by remember { mutableStateOf(false) }
    var openNow by remember { mutableStateOf(true) }
    var promoOnly by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var remote by remember { mutableStateOf<List<Store>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(sort, freeDelivery, openNow) {
        loaded = false
        remote = AppData.stores(kind, sort.api, freeDelivery, openNow)
        loaded = true
    }
    val stores = if (promoOnly) remote.filter { it.promoTagAr != null } else remote

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            KonoBackButton(onBack)
            Text(kind.titleAr, style = KonoFont.Title, color = KonoColor.TextPrimary)
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
        ) {
            item { Text(kind.subtitleAr, style = KonoFont.Callout, color = KonoColor.TextSecondary) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Box {
                        KonoChip(sort.titleAr, icon = Icons.Filled.SwapVert, selected = true) { menuOpen = true }
                        DropdownMenu(menuOpen, { menuOpen = false }, containerColor = KonoColor.Surface) {
                            Sort.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.titleAr, style = KonoFont.Body) },
                                    onClick = { sort = opt; menuOpen = false },
                                )
                            }
                        }
                    }
                    KonoChip("توصيل مجاني", icon = Icons.Filled.DeliveryDining, selected = freeDelivery) { freeDelivery = !freeDelivery }
                    KonoChip("مفتوح الآن", icon = Icons.Filled.AccessTime, selected = openNow) { openNow = !openNow }
                    KonoChip("عروض", icon = Icons.Filled.LocalOffer, selected = promoOnly) { promoOnly = !promoOnly }
                }
            }
            if (!loaded) {
                item { Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) { repeat(4) { SkeletonBlock(104.dp, 360.dp, radius = 14.dp) } } }
            } else if (stores.isEmpty()) {
                item { KonoEmptyState("لا توجد نتائج", message = "جرّب تغيير الفلاتر أو العنوان") }
            } else {
                item { Text("${stores.size} نتيجة", style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
                itemsIndexed(stores, key = { _, s -> s.id }) { _, s -> StoreRow(s) { onStore(s) } }
            }
        }
    }
}
