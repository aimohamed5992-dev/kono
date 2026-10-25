package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.Address
import sa.kono.app.data.ApiError
import sa.kono.app.data.AppData
import sa.kono.app.data.CartStore
import sa.kono.app.data.PaymentMethod
import sa.kono.app.data.clean
import sa.kono.app.data.placeOrder
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable

private val slots = listOf("٨:٣٠ م", "٩:٠٠ م", "٩:٣٠ م")
private val tips = listOf(0.0, 5.0, 10.0, 15.0)

private fun paymentIcon(name: String): ImageVector = when {
    name.contains("apple") || name.contains("iphone") -> Icons.Filled.PhoneIphone
    name.contains("banknote") || name.contains("cash") -> Icons.Filled.Payments
    else -> Icons.Filled.CreditCard
}

@Composable
fun CheckoutScreen(onBack: () -> Unit, onPlaced: (orderId: Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf<Address?>(AppData.currentAddress) }
    var payment by remember { mutableStateOf<PaymentMethod?>(null) }
    var scheduled by remember { mutableStateOf<String?>(null) }
    var tip by remember { mutableDoubleStateOf(0.0) }
    var placing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var addrMenu by remember { mutableStateOf(false) }
    var slotMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppData.loadAccount()
        address = AppData.currentAddress ?: AppData.addresses.firstOrNull()
        payment = AppData.payments.firstOrNull { it.isDefault } ?: AppData.payments.firstOrNull()
    }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("الدفع", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
        ) {
            // address
            Box {
                val shape = RoundedCornerShape(KonoRadius.m)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f) { addrMenu = true }.padding(KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(KonoRadius.s)).background(KonoColor.Cream), contentAlignment = Alignment.Center) {
                        Icon(addressIcon(address?.icon ?: ""), null, Modifier.size(20.dp), tint = KonoColor.Brown)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("التوصيل إلى", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                        Text(address?.labelAr ?: "اختر عنواناً", style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                        Text(address?.lineAr ?: "", style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
                }
                DropdownMenu(addrMenu, { addrMenu = false }, containerColor = KonoColor.Surface) {
                    AppData.addresses.forEach { a ->
                        DropdownMenuItem(
                            text = { Text("${a.labelAr} — ${a.lineAr}", style = KonoFont.Body, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { address = a; addrMenu = false; scope.launch { AppData.selectAddress(a) } },
                        )
                    }
                }
            }

            // timing
            Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("وقت التوصيل", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    KonoChip("في أسرع وقت", icon = Icons.Filled.Bolt, selected = scheduled == null) { scheduled = null }
                    Box {
                        KonoChip(scheduled ?: "جدولة لاحقاً", icon = Icons.Filled.CalendarMonth, selected = scheduled != null) { slotMenu = true }
                        DropdownMenu(slotMenu, { slotMenu = false }, containerColor = KonoColor.Surface) {
                            slots.forEach { s -> DropdownMenuItem(text = { Text(s, style = KonoFont.Body) }, onClick = { scheduled = s; slotMenu = false }) }
                        }
                    }
                }
            }

            // payment
            Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("طريقة الدفع", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                AppData.payments.forEach { pm ->
                    val sel = payment?.id == pm.id
                    val shape = RoundedCornerShape(KonoRadius.s)
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(if (sel) KonoColor.Cream else KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
                            .konoClickable(pressedScale = 0.99f) { payment = pm }.padding(KonoSpace.m),
                        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(paymentIcon(pm.icon), null, Modifier.size(22.dp), tint = KonoColor.Brown)
                        Text(pm.labelAr, style = KonoFont.Body, color = KonoColor.TextPrimary)
                        pm.detailAr?.let { Text(it, style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
                        Box(Modifier.weight(1f))
                        Icon(if (sel) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(22.dp), tint = if (sel) KonoColor.Brown else KonoColor.TextTertiary)
                    }
                }
            }

            // tip
            Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("إكرامية المندوب", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    tips.forEach { t -> KonoChip(if (t == 0.0) "لا شكراً" else "${t.clean()} ر.س", selected = tip == t) { tip = t } }
                }
            }

            PriceBreakdown(CartStore.subtotal, CartStore.deliveryFee, CartStore.serviceFee + tip, CartStore.discount, CartStore.total + tip)
        }
        BottomActionBar {
            KonoButton(
                "تأكيد الطلب · ${(CartStore.total + tip).clean()} ر.س",
                {
                    val a = address; val p = payment
                    if (a == null || p == null) { error = "اختر عنوان التوصيل وطريقة الدفع."; return@KonoButton }
                    placing = true
                    scope.launch {
                        try {
                            val order = AppData.placeOrder(CartStore, a, p.labelAr, tip, scheduled)
                            onPlaced(order.id)
                        } catch (e: ApiError) {
                            error = e.message ?: "تعذّر إرسال الطلب، حاول مرة أخرى."
                        } finally { placing = false }
                    }
                },
                loading = placing,
            )
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton({ error = null }) { Text("حسناً", style = KonoFont.Button, color = KonoColor.Brown) } },
            title = { Text("تعذّر إتمام الطلب", style = KonoFont.Headline) },
            text = { Text(error ?: "", style = KonoFont.Body) },
            containerColor = KonoColor.Surface,
        )
    }
}
