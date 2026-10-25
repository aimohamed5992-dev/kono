package sa.kono.app.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sa.kono.app.data.MerchantSession
import sa.kono.app.data.MerchantTicket
import sa.kono.app.data.TicketState
import sa.kono.app.data.clean
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.KonoMark
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.customer.parseApiDate
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDashboard(onTicket: (Int) -> Unit) {
    var filter by remember { mutableStateOf(TicketState.Incoming) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val store = MerchantSession.store

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        // header
        Row(
            Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, RoundedCornerShape(0.dp)).background(KonoColor.Surface).padding(horizontal = KonoSpace.screen, vertical = KonoSpace.m),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
        ) {
            KonoMark(size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(store?.name ?: "متجرك", style = KonoFont.Headline, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (MerchantSession.isOnline) "مفتوح ويستقبل الطلبات" else "مغلق مؤقتاً", style = KonoFont.Footnote, color = if (MerchantSession.isOnline) KonoColor.Success else KonoColor.Danger)
            }
            Switch(
                MerchantSession.isOnline, { v -> scope.launch { MerchantSession.setOnline(v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = KonoColor.Success, checkedThumbColor = Color.White, uncheckedTrackColor = KonoColor.SurfaceSunken, uncheckedThumbColor = Color.White, uncheckedBorderColor = KonoColor.Divider),
            )
        }

        PullToRefreshBox(refreshing, { scope.launch { refreshing = true; MerchantSession.refreshTickets(); MerchantSession.loadReports(); refreshing = false } }, Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, top = KonoSpace.s, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                        StatTile("مبيعات اليوم", "${MerchantSession.todayRevenue.clean()} ر.س", Icons.Filled.Payments, KonoColor.Success, Modifier.weight(1f))
                        StatTile("طلبات اليوم", "${MerchantSession.todayOrders}", Icons.Filled.ShoppingBag, KonoColor.Brown, Modifier.weight(1f))
                        StatTile("متوسط التجهيز", "${MerchantSession.prepMinutes} د", Icons.Filled.Timer, KonoColor.Caramel, Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                        listOf(TicketState.Incoming to "جديدة", TicketState.Preparing to "قيد التجهيز", TicketState.Ready to "جاهزة", TicketState.HandedOver to "تم التسليم").forEach { (st, label) ->
                            CountChip(label, MerchantSession.ticketsIn(st).size, filter == st) { filter = st }
                        }
                    }
                }
                val list = MerchantSession.ticketsIn(filter)
                if (list.isEmpty()) {
                    item { KonoEmptyState("ما في طلبات هنا", icon = Icons.Filled.Inbox, message = "الطلبات الجديدة بتظهر هنا مباشرة.", modifier = Modifier.padding(top = KonoSpace.xl)) }
                } else {
                    items(list, key = { it.id }) { t ->
                        TicketCard(
                            t, onOpen = { onTicket(t.id) },
                            onAccept = { scope.launch { MerchantSession.advance(t) } },
                            onReject = { scope.launch { MerchantSession.reject(t) } },
                            onNext = { scope.launch { MerchantSession.advance(t) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(modifier.clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Text(value, style = cairo(16, FontWeight.ExtraBold), color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title, style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
    }
}

@Composable
private fun CountChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height36().clip(CircleShape).background(if (selected) KonoColor.Brown else KonoColor.Surface)
            .border(1.dp, if (selected) Color.Transparent else KonoColor.Brown.copy(alpha = 0.16f), CircleShape).konoClickable(onClick = onClick).padding(horizontal = KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = KonoFont.Subheadline, color = if (selected) KonoColor.TextOnBrown else KonoColor.Brown)
        if (count > 0) Box(Modifier.size(18.dp).background(if (selected) KonoColor.Yellow else KonoColor.Brown, CircleShape), contentAlignment = Alignment.Center) {
            Text("$count", style = cairo(11, FontWeight.Black), color = if (selected) KonoColor.Brown else KonoColor.TextOnBrown)
        }
    }
}

private fun Modifier.height36() = this.then(Modifier.height(36.dp))

/** A ticket with a live accept countdown (new), and the next-step action for its state. */
@Composable
fun TicketCard(t: MerchantTicket, onOpen: () -> Unit, onAccept: () -> Unit, onReject: () -> Unit, onNext: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(t.id) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val deadline = remember(t.acceptDeadline) { parseApiDate(t.acceptDeadline)?.time ?: now }
    val placed = remember(t.placedAt) { parseApiDate(t.placedAt)?.time ?: now }
    val secondsLeft = ((deadline - now) / 1000).toInt().coerceIn(0, 60)
    val minutesAgo = ((now - placed) / 60000).toInt().coerceAtLeast(1)
    val incoming = t.stateEnum == TicketState.Incoming
    val shape = RoundedCornerShape(KonoRadius.l)

    Column(
        Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
            .konoClickable(pressedScale = 0.99f, onClick = onOpen).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.shortCode, style = KonoFont.Headline, color = KonoColor.TextPrimary)
                    KonoTag(if (t.isDelivery) "توصيل" else "استلام", icon = if (t.isDelivery) Icons.Filled.DeliveryDining else Icons.Filled.ShoppingBag, color = KonoColor.Brown)
                }
                Text("${t.customerNameAr} · ${t.itemCount} صنف · قبل $minutesAgo د", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
            RiyalText(t.total)
        }
        if (incoming) {
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, Modifier.size(14.dp), tint = KonoColor.Danger)
                Text("قبول خلال $secondsLeft ثانية", style = KonoFont.Subheadline, color = KonoColor.Danger)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(KonoColor.Divider)) {
                Box(Modifier.fillMaxWidth(secondsLeft / 60f).height(4.dp).background(if (secondsLeft < 15) KonoColor.Danger else KonoColor.Caramel, CircleShape))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            t.items.take(3).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Text("${line.quantity}×", style = KonoFont.Subheadline, color = KonoColor.Brown)
                    Text(line.nameAr, style = KonoFont.Callout, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (t.items.size > 3) Text("+ ${t.items.size - 3} أصناف أخرى", style = KonoFont.Footnote, color = KonoColor.TextTertiary)
        }
        t.noteAr?.takeIf { it.isNotBlank() }?.let { note ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.s)).background(KonoColor.Caramel.copy(alpha = 0.1f)).padding(KonoSpace.s),
                horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ReportProblem, null, Modifier.size(14.dp), tint = KonoColor.Caramel)
                Text(note, style = KonoFont.Footnote, color = KonoColor.Caramel)
            }
        }
        when (t.stateEnum) {
            TicketState.Incoming -> Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                KonoButton("رفض", onReject, Modifier.weight(1f), kind = KonoButtonKind.Destructive)
                KonoButton("قبول الطلب", onAccept, Modifier.weight(1f))
            }
            TicketState.Preparing -> KonoButton("الطلب جاهز", onNext, icon = Icons.Filled.Check, kind = KonoButtonKind.Brand)
            TicketState.Ready -> KonoButton(if (t.isDelivery) "تسليم للمندوب" else "تم الاستلام", onNext, icon = Icons.Filled.ShoppingBag)
            else -> Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Verified, null, Modifier.size(18.dp), tint = KonoColor.Success)
                Text("اكتمل", style = KonoFont.Subheadline, color = KonoColor.Success, modifier = Modifier.weight(1f))
                t.courierNameAr?.let { Text("المندوب: $it", style = KonoFont.Footnote, color = KonoColor.TextSecondary) }
            }
        }
    }
}
