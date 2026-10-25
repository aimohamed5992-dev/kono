package sa.kono.app.ui.merchant

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.MerchantSession
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoShadow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantReportsScreen() {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { MerchantSession.ensureBootstrapped(); MerchantSession.loadReports() }
    val r = MerchantSession.reports
    val days = r?.days.orEmpty()
    val total = r?.totalSales ?: 0.0
    val orders = r?.orderCount ?: 0

    PullToRefreshBox(refreshing, { scope.launch { refreshing = true; MerchantSession.loadReports(); refreshing = false } }, Modifier.fillMaxSize().background(KonoColor.Background)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
        ) {
            Text("التقارير", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary)
            Text("آخر ٧ أيام", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                BigStat("إجمالي المبيعات", "${total.toInt()} ر.س", Modifier.weight(1f))
                BigStat("عدد الطلبات", "$orders", Modifier.weight(1f))
            }
            val shape = RoundedCornerShape(KonoRadius.l)
            Column(Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                Text("المبيعات اليومية", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                BarChart(days.map { it.amount.toFloat() }, days.map { it.weekdayAr }, Modifier.fillMaxWidth().height(180.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                OpStat("متوسط التجهيز", "${r?.prepMinutes ?: MerchantSession.prepMinutes} د", Icons.Filled.Timer, Modifier.weight(1f))
                OpStat("متوسط الطلب", "${(r?.avgOrderValue ?: 0.0).toInt()} ر.س", Icons.Filled.ShoppingCart, Modifier.weight(1f))
                OpStat("تقييم المتجر", String.format(Locale.US, "%.1f", r?.rating ?: MerchantSession.store?.rating ?: 0.0), Icons.Filled.Star, Modifier.weight(1f))
            }
            val mshape = RoundedCornerShape(KonoRadius.m)
            Column(Modifier.fillMaxWidth().clip(mshape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, mshape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("الأصناف الأكثر مبيعاً", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                val top = r?.topItems.orEmpty().take(4)
                if (top.isEmpty()) Text("لا توجد مبيعات في هذه الفترة", style = KonoFont.Footnote, color = KonoColor.TextTertiary, modifier = Modifier.padding(vertical = 8.dp))
                top.forEachIndexed { i, it ->
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", style = KonoFont.Headline, color = KonoColor.Brown, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                        Text(it.nameAr, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                        Text("${it.count} مبيع", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                    }
                    if (i < top.lastIndex) Divider(color = KonoColor.Divider)
                }
            }
        }
    }
}

@Composable
private fun BigStat(title: String, value: String, modifier: Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(modifier.clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        Text(title, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        Text(value, style = cairo(19, FontWeight.ExtraBold), color = KonoColor.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun OpStat(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(modifier.clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(vertical = KonoSpace.m), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = KonoColor.Caramel)
        Text(value, style = cairo(15, FontWeight.ExtraBold), color = KonoColor.TextPrimary)
        Text(title, style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Animated bar chart in brand colours — the latest day is brown, the rest yellow. */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val grow = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { grow.animateTo(1f, tween(700)) }
    val maxV = maxOf(values.maxOrNull() ?: 1f, 1f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.height((8 + (150 * (v / maxV) * grow.value)).dp).fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(if (i == values.lastIndex) KonoColor.Brown else KonoColor.Yellow))
                Text(labels.getOrElse(i) { "" }, style = cairo(9, FontWeight.Medium), color = KonoColor.TextSecondary, maxLines = 1)
            }
        }
    }
}
