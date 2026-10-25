package sa.kono.app.ui.courier

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.CourierSession
import sa.kono.app.data.OrderDetail
import sa.kono.app.data.clean
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.RatingBadge
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.customer.BottomActionBar
import sa.kono.app.ui.customer.ScreenTopBar
import sa.kono.app.ui.customer.SegmentedTabs
import sa.kono.app.ui.merchant.BarChart
import sa.kono.app.ui.merchant.StatusChip
import sa.kono.app.ui.shared.OrderAudience
import sa.kono.app.ui.shared.OrderDetailSections
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

// ── Earnings ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierEarningsScreen() {
    var range by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var paid by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { CourierSession.ensureBootstrapped(); CourierSession.loadSummary() }
    val s = CourierSession.summary
    val today = s?.todayEarnings ?: CourierSession.profile.todayEarnings
    val week = s?.week.orEmpty()
    val weekTrips = week.sumOf { it.trips }

    PullToRefreshBox(refreshing, { scope.launch { refreshing = true; CourierSession.refreshProfile(); CourierSession.loadSummary(); refreshing = false } }, Modifier.fillMaxSize().background(KonoColor.Background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Text("الأرباح", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.l)).background(KonoColor.BrandGradient).padding(vertical = KonoSpace.xl),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.xs),
            ) {
                Text("أرباح اليوم", style = KonoFont.Footnote, color = KonoColor.Brown.copy(alpha = 0.8f))
                RiyalText(today, style = KonoFont.display(40), color = KonoColor.Brown)
                Text("${s?.todayTrips ?: CourierSession.profile.todayTrips} رحلة مكتملة", style = KonoFont.Subheadline, color = KonoColor.Brown.copy(alpha = 0.8f))
            }
            KonoButton(if (paid) "تم إرسال طلب التحويل ✓" else "تحويل الأرباح إلى الحساب البنكي", { confirm = true }, icon = Icons.Filled.FileDownload, enabled = today > 0 && !paid)

            val shape = RoundedCornerShape(KonoRadius.m)
            Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                BreakdownRow("توصيل الطلبات", s?.deliveryPart ?: 0.0, Icons.Filled.DeliveryDining)
                BreakdownRow("إكراميات العملاء", s?.tipsPart ?: 0.0, Icons.Filled.CardGiftcard)
                BreakdownRow("حوافز وبونص", maxOf(0.0, s?.bonusPart ?: 0.0), Icons.Filled.Bolt)
            }

            val lshape = RoundedCornerShape(KonoRadius.l)
            Column(Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, lshape).clip(lshape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, lshape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                SegmentedTabs(listOf("الأسبوع", "الشهر"), range, { range = it })
                Text("إجمالي الأسبوع ${week.sumOf { it.amount }.toInt()} ر.س", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                BarChart(week.map { it.amount.toFloat() }, week.map { it.weekdayAr }, Modifier.fillMaxWidth().height(160.dp))
            }

            Row(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Flag, null, Modifier.size(26.dp), tint = KonoColor.Caramel)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("تحدّي الويكند", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                    Text("أكمل ٢٠ رحلة واحصل على ١٠٠ ر.س إضافية", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(KonoColor.Divider)) { Box(Modifier.fillMaxWidth(minOf(weekTrips, 20) / 20f).height(4.dp).background(KonoColor.Caramel, CircleShape)) }
                    Text("$weekTrips / 20", style = KonoFont.Caption, color = KonoColor.TextTertiary)
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        confirmButton = { TextButton({ confirm = false; paid = true }) { Text("تأكيد التحويل", style = KonoFont.Button, color = KonoColor.Brown) } },
        dismissButton = { TextButton({ confirm = false }) { Text("إلغاء", style = KonoFont.Button, color = KonoColor.TextSecondary) } },
        title = { Text("تحويل ${today.toInt()} ر.س إلى حسابك البنكي؟", style = KonoFont.Headline) },
        text = { Text("تصل الحوالة خلال ١–٣ أيام عمل. الحد الأدنى للتحويل ٥٠ ر.س.", style = KonoFont.Body) },
        containerColor = KonoColor.Surface,
    )
}

@Composable
private fun BreakdownRow(label: String, amount: Double, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = KonoColor.Brown)
        Text(label, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
        RiyalText(amount)
    }
}

// ── History ─────────────────────────────────────────────────────────────────

@Composable
fun CourierHistoryScreen() {
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { CourierSession.loadSummary(); loaded = true }
    val trips = CourierSession.summary?.recentTrips.orEmpty()
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        Text("سجل الرحلات", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary, modifier = Modifier.padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.m))
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            if (loaded && trips.isEmpty()) item { KonoEmptyState("ما في رحلات بعد", icon = Icons.Filled.DeliveryDining, message = "رحلاتك المكتملة بتظهر هنا مع الأرباح والتقييم.", modifier = Modifier.padding(top = KonoSpace.xxl)) }
            itemsIndexed(trips) { _, t ->
                val shape = RoundedCornerShape(KonoRadius.m)
                Row(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(24.dp), tint = KonoColor.Success)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("رحلة ${t.shortCode}", style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                        Text(t.whenAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) { RiyalText(t.amount); RatingBadge(t.rating, compact = true) }
                }
            }
        }
    }
}

// ── Profile ─────────────────────────────────────────────────────────────────

@Composable
fun CourierProfileScreen(onChats: () -> Unit, onInfo: (String, String) -> Unit, onSignOut: () -> Unit) {
    LaunchedEffect(Unit) { CourierSession.ensureBootstrapped(); CourierSession.refreshProfile() }
    val p = CourierSession.profile
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
        Text("الملف", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary)
        val lshape = RoundedCornerShape(KonoRadius.l)
        Column(Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, lshape).clip(lshape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, lshape).padding(KonoSpace.l), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
            Box(Modifier.size(88.dp).background(KonoColor.BrandGradient, CircleShape), contentAlignment = Alignment.Center) {
                AssetImage(p.photoUrl, Modifier.size(82.dp).clip(CircleShape), Icons.Filled.Person, KonoColor.Brown)
            }
            Text(p.name, style = KonoFont.Title2, color = KonoColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                RatingBadge(p.rating, count = maxOf(p.ratingCount, 1)); StatusChip(p.onboardingStatus)
            }
        }
        if (p.onboardingStatus == "under_review") {
            Text("طلب تسجيلك كمندوب قيد المراجعة — ستصلك رسالة عند الموافقة.", style = KonoFont.Footnote, color = KonoColor.Caramel, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.m)).background(KonoColor.Caramel.copy(alpha = 0.1f)).padding(KonoSpace.m))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            Stat("${p.acceptanceRate}٪", "نسبة القبول", Modifier.weight(1f)); Stat("${p.completionRate}٪", "نسبة الإكمال", Modifier.weight(1f)); Stat(p.vehicleAr, "المركبة", Modifier.weight(1f))
        }
        Rows("", listOf(R("محادثات العملاء والمتاجر", Icons.Filled.ChatBubble, null, null, onChats)))
        sa.kono.app.ui.i18n.LanguageToggleRow()
        Rows("الحساب", listOf(
            R("الوثائق والرخص", Icons.Filled.Description, "مكتملة", KonoColor.Success) { onInfo("الوثائق والرخص", "الهوية، رخصة القيادة، واستمارة المركبة موثّقة وسارية. تُذكّرك كونو قبل انتهاء أي وثيقة بـ ٣٠ يوماً.") },
            R("بيانات المركبة", Icons.Filled.DeliveryDining, null, null) { onInfo("بيانات المركبة", "المركبة المسجّلة: ${p.vehicleAr} · اللوحة ${p.plateAr}. لتغيير نوع المركبة أو اللوحة تواصل مع دعم المندوبين لإعادة التوثيق.") },
            R("الحساب البنكي", Icons.Filled.AccountBalance, null, null) { onInfo("الحساب البنكي", "تُحوَّل أرباحك يومياً بعد الساعة ٢ ظهراً إلى الآيبان المسجَّل.") },
            R("جدول المناوبات", Icons.Filled.CalendarMonth, null, null) { onInfo("جدول المناوبات", "أنت غير مرتبط بمناوبات ثابتة — تعمل وقتما تفعّل «متصل». مناطق الذروة: ١٢–٣ ظهراً و ٧–١١ مساءً.") },
        ))
        Rows("الدعم", listOf(
            R("مركز المساعدة", Icons.Filled.Help, null, null) { onInfo("مركز المساعدة", "أسئلة شائعة حول الأرباح، الطلبات، والتقييم متوفرة في مركز المساعدة. الدعم المباشر متاح ٢٤/٧ عبر الدردشة.") },
            R("السلامة والطوارئ", Icons.Filled.Shield, null, null) { onInfo("السلامة والطوارئ", "زر الطوارئ داخل الرحلة يتصل مباشرة بـ ٩١١ ويشارك موقعك مع فريق السلامة. بلّغ عن أي حادث خلال ٢٤ ساعة.") },
        ))
        Text("تسجيل الخروج", style = cairo(14, FontWeight.SemiBold), color = KonoColor.Danger, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(46.dp).konoClickable(onClick = onSignOut).padding(top = 10.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(modifier.clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(vertical = KonoSpace.m), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = cairo(15, FontWeight.ExtraBold), color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
    }
}

private class R(val title: String, val icon: ImageVector, val detail: String?, val detailColor: androidx.compose.ui.graphics.Color?, val onClick: () -> Unit)

@Composable
private fun Rows(title: String, rows: List<R>) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        if (title.isNotEmpty()) Text(title, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
        Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)) {
            rows.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().konoClickable(pressedScale = 0.995f, onClick = r.onClick).padding(vertical = KonoSpace.m, horizontal = KonoSpace.l), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                    Icon(r.icon, null, Modifier.size(22.dp), tint = KonoColor.Brown)
                    Text(r.title, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    r.detail?.let { Text(it, style = KonoFont.Footnote, color = r.detailColor ?: KonoColor.TextTertiary) }
                    Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
                }
                if (i < rows.lastIndex) Divider(Modifier.padding(start = 52.dp), color = KonoColor.Divider)
            }
        }
    }
}

// ── Full order detail ───────────────────────────────────────────────────────

@Composable
fun CourierOrderDetailScreen(orderId: Int, onBack: () -> Unit, onChat: (Int) -> Unit) {
    var detail by remember { mutableStateOf<OrderDetail?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(orderId) { detail = CourierSession.orderDetail(orderId); failed = detail == null }
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar(detail?.shortCode ?: "الطلب", onBack)
        Box(Modifier.weight(1f)) {
            val d = detail
            when {
                d != null -> Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) { OrderDetailSections(d, OrderAudience.Courier, onChat) }
                failed -> Text("تعذّر تحميل تفاصيل الطلب", style = KonoFont.Footnote, color = KonoColor.TextSecondary, modifier = Modifier.align(Alignment.Center))
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = KonoColor.Brown)
            }
        }
    }
}
