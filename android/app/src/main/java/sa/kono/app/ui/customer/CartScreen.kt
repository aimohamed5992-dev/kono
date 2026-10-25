package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sa.kono.app.data.CartLine
import sa.kono.app.data.CartStore
import sa.kono.app.data.clean
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.LogoBubble
import sa.kono.app.ui.components.QuantityStepper
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

/** Top bar for pushed screens: back button + title. */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
        KonoBackButton(onBack)
        Text(title, style = KonoFont.Title, color = KonoColor.TextPrimary)
    }
}

/** A sticky footer with a primary button (cart / checkout). */
@Composable
fun BottomActionBar(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().konoShadow(KonoShadow.Lg, RoundedCornerShape(0.dp)).background(KonoColor.Surface)
            .navigationBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.m),
    ) { content() }
}

@Composable
fun CartScreen(onBack: () -> Unit, onCheckout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("السلة", onBack)
        if (CartStore.isEmpty) {
            KonoEmptyState("سلتك فاضية", icon = Icons.Filled.ShoppingBag, message = "أضف أصناف من أي متجر وتلقاها هنا.", actionTitle = "تصفّح المتاجر", onAction = onBack, modifier = Modifier.padding(top = KonoSpace.xxl))
        } else {
            var promo by remember { mutableStateOf("") }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l),
                verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
            ) {
                CartStore.store?.let { s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                        LogoBubble(s.logoImageUrl, s.name, size = 36.dp)
                        Text(s.name, style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                        Text("${s.etaMinutes} دقيقة", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    CartStore.lines.toList().forEach { line -> CartLineRow(line) { q -> CartStore.setQuantity(q, line) } }
                }
                val shape = RoundedCornerShape(KonoRadius.m)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ConfirmationNumber, null, Modifier.size(20.dp), tint = KonoColor.Caramel)
                    androidx.compose.foundation.text.BasicTextField(
                        promo, { promo = it }, Modifier.weight(1f), singleLine = true,
                        textStyle = KonoFont.Body.copy(color = KonoColor.TextPrimary),
                        decorationBox = { inner -> if (promo.isEmpty()) Text("رمز الخصم", style = KonoFont.Body, color = KonoColor.TextTertiary); inner() },
                    )
                    Text(
                        if (CartStore.promoApplied) "مطبّق" else "تطبيق", style = KonoFont.Subheadline,
                        color = if (CartStore.promoApplied) KonoColor.Success else KonoColor.Brown,
                        modifier = Modifier.konoClickable { CartStore.promoApplied = true },
                    )
                }
                PriceBreakdown(CartStore.subtotal, CartStore.deliveryFee, CartStore.serviceFee, CartStore.discount, CartStore.total)
            }
            BottomActionBar { KonoButton("متابعة الدفع · ${CartStore.total.clean()} ر.س", onCheckout) }
        }
    }
}

@Composable
fun CartLineRow(line: CartLine, onQuantity: (Int) -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.Top,
    ) {
        AssetImage(line.product.imageUrl, Modifier.size(64.dp).clip(RoundedCornerShape(KonoRadius.s)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(line.product.nameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            if (line.selectedOptionsAr.isNotEmpty()) Text(line.selectedOptionsAr.joinToString(" · "), style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (line.note.isNotEmpty()) Text("ملاحظة: ${line.note}", style = KonoFont.Footnote, color = KonoColor.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            RiyalText(line.lineTotal)
        }
        QuantityStepper(line.quantity, onQuantity, min = 0, compact = true)
    }
}

/** Subtotal / delivery / service / discount / total card. */
@Composable
fun PriceBreakdown(subtotal: Double, delivery: Double, service: Double, discount: Double, total: Double, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(
        modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.s),
    ) {
        PriceRow("المجموع الفرعي", subtotal)
        PriceRow("رسوم التوصيل", delivery, free = delivery == 0.0)
        PriceRow("رسوم الخدمة", service)
        if (discount > 0) PriceRow("الخصم", -discount, highlight = true)
        Divider(color = KonoColor.Divider)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("الإجمالي", style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
            RiyalText(total, style = KonoFont.PriceLarge)
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, free: Boolean = false, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = KonoFont.Callout, color = KonoColor.TextSecondary, modifier = Modifier.weight(1f))
        if (free) Text("مجاني", style = KonoFont.Subheadline, color = KonoColor.Success)
        else RiyalText(amount, style = KonoFont.Subheadline, color = if (highlight) KonoColor.Success else KonoColor.TextPrimary)
    }
}
