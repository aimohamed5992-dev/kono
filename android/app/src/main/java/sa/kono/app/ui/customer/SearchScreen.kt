package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sa.kono.app.data.AppData
import sa.kono.app.data.ItemHit
import sa.kono.app.data.RecentSearches
import sa.kono.app.data.Store
import sa.kono.app.data.SearchResults
import sa.kono.app.data.search
import sa.kono.app.data.trendingSearches
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable

private enum class Scope(val api: String, val titleAr: String) { All("all", "الكل"), Stores("stores", "المتاجر"), Items("items", "الأصناف") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(prefill: String?, onBack: () -> Unit, onStore: (Int) -> Unit) {
    var query by remember { mutableStateOf(prefill.orEmpty()) }
    var scope by remember { mutableStateOf(Scope.All) }
    var results by remember { mutableStateOf(SearchResults()) }
    var searched by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { delay(300); runCatching { focus.requestFocus() } }
    LaunchedEffect(query, scope) {
        val q = query.trim()
        if (q.isEmpty()) { results = SearchResults(); searched = false; return@LaunchedEffect }
        delay(250) // debounce
        results = AppData.search(q, scope.api)
        searched = true
        if (results.stores.isNotEmpty() || results.items.isNotEmpty()) RecentSearches.record(q)
    }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
        Row(Modifier.padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            KonoBackButton(onBack)
            Row(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, CircleShape).padding(horizontal = KonoSpace.l),
                horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(18.dp), tint = KonoColor.Brown.copy(alpha = 0.7f))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("ابحث عن مطعم، صيدلية، أو صنف", style = KonoFont.Body, color = KonoColor.TextTertiary)
                    BasicTextField(
                        query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                        textStyle = KonoFont.Body.copy(color = KonoColor.TextPrimary), cursorBrush = SolidColor(KonoColor.Brown),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(),
                    )
                }
                if (query.isNotEmpty()) Icon(Icons.Filled.Cancel, "مسح", Modifier.size(18.dp).konoClickable(pressedScale = 0.85f) { query = "" }, tint = KonoColor.TextTertiary)
            }
        }

        if (query.isBlank()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s), verticalArrangement = Arrangement.spacedBy(KonoSpace.xl)) {
                if (RecentSearches.items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("عمليات بحث سابقة", style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                            Text("مسح", style = KonoFont.Footnote, color = KonoColor.Brown, modifier = Modifier.clickable { RecentSearches.clear() })
                        }
                        RecentSearches.items.forEach { s ->
                            Row(Modifier.fillMaxWidth().clickable { query = s }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.History, null, Modifier.size(16.dp), tint = KonoColor.TextTertiary)
                                Text(s, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.NorthWest, null, Modifier.size(14.dp), tint = KonoColor.TextTertiary)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                    Text("الأكثر بحثاً", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                        AppData.trendingSearches.forEach { t -> KonoChip(t) { query = t } }
                    }
                }
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Scope.entries.forEach { s -> KonoChip(s.titleAr, selected = scope == s) { scope = s } }
                }
                if (searched && results.stores.isEmpty() && results.items.isEmpty()) {
                    KonoEmptyState("ما لقينا نتائج", icon = Icons.Filled.SearchOff, message = "جرّب كلمة أبسط أو تأكد من الإملاء.", modifier = Modifier.padding(top = KonoSpace.xxl))
                }
                if (scope != Scope.Items && results.stores.isNotEmpty()) {
                    Text("متاجر", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
                    results.stores.forEach { s -> StoreRow(s) { onStore(s.id) } }
                }
                if (scope != Scope.Stores && results.items.isNotEmpty()) {
                    Text("أصناف", style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
                    results.items.forEach { hit -> ItemHitRow(hit) { onStore(hit.storeId) } }
                }
            }
        }
    }
}

@Composable
private fun ItemHitRow(hit: ItemHit, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.98f, onClick = onTap).padding(KonoSpace.s),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetImage(hit.imageUrl, Modifier.size(60.dp).clip(RoundedCornerShape(KonoRadius.s)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(hit.nameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hit.storeName, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        }
        RiyalText(hit.price)
    }
}
