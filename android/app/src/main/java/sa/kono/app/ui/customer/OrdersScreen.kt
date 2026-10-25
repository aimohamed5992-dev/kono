package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.OrderDto
import sa.kono.app.data.OrderStage
import sa.kono.app.data.myOrders
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.LogoBubble
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

fun OrderStage.icon(): ImageVector = when (this) {
    OrderStage.Placed -> Icons.Filled.CheckCircle
    OrderStage.Confirmed -> Icons.Filled.Verified
    OrderStage.Preparing -> Icons.Filled.LocalFireDepartment
    OrderStage.ReadyForPickup -> Icons.Filled.ShoppingBag
    OrderStage.OnTheWay -> Icons.Filled.DeliveryDining
    OrderStage.Delivered -> Icons.Filled.Home
}

/** Parses the API's ISO timestamps (with or without a zone) into a [Date]. */
fun parseApiDate(s: String): Date? = runCatching { Date.from(Instant.parse(s)) }
    .recoverCatching { Date.from(LocalDateTime.parse(s).toInstant(ZoneOffset.UTC)) }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(onOrder: (Int) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var active by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var past by remember { mutableStateOf<List<OrderDto>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() { active = AppData.myOrders("active"); past = AppData.myOrders("past") }
    LaunchedEffect(Unit) { load() }
    // keep running orders fresh while this tab is open
    LaunchedEffect(Unit) { while (true) { delay(6000); active = AppData.myOrders("active") } }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        Text("طلباتي", style = KonoFont.LargeTitle, color = KonoColor.Espresso, modifier = Modifier.padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s))
        SegmentedTabs(listOf("الحالية", "السابقة"), tab, { tab = it }, Modifier.padding(horizontal = KonoSpace.screen, vertical = KonoSpace.m))
        PullToRefreshBox(refreshing, { scope.launch { refreshing = true; load(); refreshing = false } }) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                val list = if (tab == 0) active else past
                if (list.isEmpty()) {
                    item {
                        KonoEmptyState(
                            if (tab == 0) "ما عندك طلبات حالية" else "ما عندك طلبات سابقة", icon = Icons.Filled.ShoppingBag,
                            message = if (tab == 0) "أطلب من أي متجر وتابع طلبك من هنا." else "طلباتك المكتملة بتظهر هنا.",
                            modifier = Modifier.padding(top = KonoSpace.xxl),
                        )
                    }
                } else if (tab == 0) {
                    items(list, key = { it.id }) { o -> ActiveOrderCard(o) { onOrder(o.id) } }
                } else {
                    items(list, key = { it.id }) { o -> PastOrderRow(o) { onOrder(o.id) } }
                }
            }
        }
    }
}

/** Two-segment control in the Kono style. */
@Composable
fun SegmentedTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(CircleShape).background(KonoColor.SurfaceSunken).padding(4.dp)) {
        titles.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(38.dp).clip(CircleShape).background(if (on) KonoColor.Surface else KonoColor.SurfaceSunken)
                    .konoClickable(pressedScale = 0.98f) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(t, style = cairo(14, if (on) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium), color = if (on) KonoColor.Brown else KonoColor.TextSecondary) }
        }
    }
}

@Composable
fun StageProgressBar(stage: OrderStage, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OrderStage.entries.forEach { s ->
            val filled = s.ordinal <= stage.ordinal
            val c by androidx.compose.animation.animateColorAsState(if (filled) KonoColor.Brown else KonoColor.Divider, KonoMotion.spring(), label = "seg")
            Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c))
        }
    }
}

@Composable
private fun ActiveOrderCard(o: OrderDto, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.l)
    Column(
        Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
            .konoClickable(pressedScale = 0.98f, onClick = onTap).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            LogoBubble(o.storeImageUrl, o.storeName, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(o.storeName, style = KonoFont.Headline, color = KonoColor.TextPrimary)
                Text("${o.itemCount} صنف · ${o.shortCode}", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
            KonoTag("جارٍ", color = KonoColor.Success, filled = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(o.stageEnum.icon(), null, Modifier.size(16.dp), tint = KonoColor.Brown)
            Text(o.stageEnum.titleAr, style = KonoFont.Subheadline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
            Text("${o.storeEtaMinutes} دقيقة", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        }
        StageProgressBar(o.stageEnum)
    }
}

@Composable
private fun PastOrderRow(o: OrderDto, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    val date = remember(o.placedAt) { parseApiDate(o.placedAt)?.let { SimpleDateFormat("d MMM", Locale("ar")).format(it) } ?: "" }
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.98f, onClick = onTap).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetImage(o.storeImageUrl, Modifier.size(60.dp).clip(RoundedCornerShape(KonoRadius.s)), Icons.Filled.Fastfood)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(o.storeName, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            Text("${o.itemCount} صنف · $date", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            RiyalText(o.total, style = KonoFont.Subheadline)
        }
        KonoTag(if (o.displayStage == "cancelled") "ملغي" else "تم التوصيل", icon = Icons.Filled.Check, color = if (o.displayStage == "cancelled") KonoColor.Danger else KonoColor.Success)
    }
}
