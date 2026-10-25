package sa.kono.app.ui.shared

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import sa.kono.app.data.OrderDetail
import sa.kono.app.data.clean
import sa.kono.app.ui.chat.OrderChatButtons
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.MiniMap
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.customer.parseApiDate
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import java.text.SimpleDateFormat
import java.util.Locale

enum class OrderAudience { Merchant, Courier }

private fun fmt(iso: String, pattern: String) = parseApiDate(iso)?.let { SimpleDateFormat(pattern, Locale("ar")).format(it) } ?: ""
private fun digits(s: String) = s.filter { it.isDigit() || it == '+' }

private fun stageColor(raw: String) = when (raw) {
    "Placed" -> KonoColor.Danger; "Delivered" -> KonoColor.Success; "Cancelled" -> KonoColor.TextTertiary; else -> KonoColor.Caramel
}

private fun stageAr(raw: String) = when (raw) {
    "Placed" -> "استُلم الطلب"; "Confirmed" -> "أكّد المتجر"; "Preparing" -> "بدأ التجهيز"; "ReadyForPickup" -> "جاهز للاستلام"
    "CourierAssigned" -> "أُسند لمندوب"; "PickedUp" -> "استلمه المندوب"; "OnTheWay" -> "انطلق للعميل"; "Arrived" -> "وصل للعميل"
    "Delivered" -> "تم التوصيل"; "Cancelled" -> "أُلغي"; else -> raw
}

/**
 * The full order card stack — customer, address, store (courier), items, money, payment, note, timeline.
 * Shared by the merchant ticket screen and the courier job screen.
 */
@Composable
fun OrderDetailSections(d: OrderDetail, audience: OrderAudience, onOpenChat: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = KonoSpace.screen).padding(vertical = KonoSpace.m), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(d.shortCode, style = KonoFont.Title, color = KonoColor.TextPrimary)
                Text(fmt(d.placedAt, "d MMM  h:mm a"), style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
            KonoTag(d.stageAr, color = stageColor(d.stage), filled = true)
        }

        Section("العميل", Icons.Filled.Person) {
            Text(d.customerNameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(d.customerPhone, style = KonoFont.Footnote, color = KonoColor.TextSecondary, modifier = Modifier.weight(1f, fill = false))
                }
                Box1(Modifier.weight(1f))
                ContactButton(Icons.Filled.Call, "tel:${digits(d.customerPhone)}")
                ContactButton(Icons.Filled.Message, "sms:${digits(d.customerPhone)}")
            }
            Text(if (d.isDelivery) "توصيل عبر مندوب كونو" else "استلام من الفرع", style = KonoFont.Footnote, color = KonoColor.TextTertiary)
            OrderChatButtons(d.id, onOpenChat, compact = true)
        }

        Section("عنوان التوصيل", Icons.Filled.Place) {
            Text(d.addressLabelAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            Text(d.addressLineAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            if (d.addressLat != 0.0) {
                MiniMap(d.addressLat, d.addressLng, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(KonoRadius.s)))
                DirectionsLink(d.addressLat, d.addressLng, "الاتجاهات إلى العميل")
            }
        }

        if (audience == OrderAudience.Courier) {
            Section("المتجر", Icons.Filled.Storefront) {
                Text(d.storeNameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                Text(listOf(d.storeCuisineAr, d.storeAddressAr).filter { it.isNotEmpty() }.joinToString(" · "), style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, null, Modifier.size(13.dp), tint = KonoColor.TextTertiary)
                    Text(" ${d.storeEtaMinutes} دقيقة تجهيز", style = KonoFont.Caption, color = KonoColor.TextTertiary, modifier = Modifier.weight(1f))
                    if (d.storePhone.isNotEmpty()) ContactButton(Icons.Filled.Call, "tel:${digits(d.storePhone)}")
                }
                if (d.storeLat != 0.0) {
                    MiniMap(d.storeLat, d.storeLng, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(KonoRadius.s)))
                    DirectionsLink(d.storeLat, d.storeLng, "الاتجاهات إلى المتجر")
                }
            }
        }

        Section("الأصناف (${d.itemCount})", Icons.Filled.ShoppingBag) {
            d.items.forEachIndexed { i, line ->
                Row(Modifier.padding(vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.Top) {
                    Text("${line.quantity}×", style = KonoFont.BodyMedium, color = KonoColor.Brown)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(line.nameAr, style = KonoFont.Body, color = KonoColor.TextPrimary)
                        if (line.selectedOptionsAr.isNotEmpty()) Text(line.selectedOptionsAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                        if (line.noteAr.isNotEmpty()) Text("ملاحظة: ${line.noteAr}", style = KonoFont.Caption, color = KonoColor.Caramel)
                    }
                    RiyalText(line.lineTotal, style = KonoFont.Subheadline)
                }
                if (i < d.items.lastIndex) Divider(color = KonoColor.Divider)
            }
        }

        Section("الحساب", Icons.Filled.Money) {
            TotalRow("المجموع الفرعي", d.subtotal)
            TotalRow("رسوم التوصيل", d.deliveryFee)
            TotalRow("رسوم الخدمة", d.serviceFee)
            if (d.discount > 0) TotalRow("خصم", -d.discount, KonoColor.Success)
            if (d.tip > 0) TotalRow("بقشيش", d.tip)
            Divider(Modifier.padding(vertical = 2.dp), color = KonoColor.Divider)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("الإجمالي", style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                RiyalText(d.total, style = KonoFont.PriceLarge)
            }
            if (audience == OrderAudience.Courier) {
                Divider(Modifier.padding(vertical = 2.dp), color = KonoColor.Divider)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeliveryDining, null, Modifier.size(16.dp), tint = KonoColor.Brown)
                    Text(" أجرتك من الرحلة", style = KonoFont.Subheadline, color = KonoColor.Brown, modifier = Modifier.weight(1f))
                    RiyalText(d.courierPayout, color = KonoColor.Brown)
                }
                Text("المسافة ~${String.format(Locale.US, "%.1f", d.distanceKm)} كم", style = KonoFont.Caption, color = KonoColor.TextTertiary)
            }
        }

        Section("الدفع", Icons.Filled.CreditCard) {
            Text(d.paymentLabelAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            d.scheduledForAr?.takeIf { it.isNotEmpty() }?.let { Text("مجدول لـ $it", style = KonoFont.Footnote, color = KonoColor.Caramel) }
        }

        d.noteAr?.takeIf { it.isNotEmpty() }?.let { note ->
            Section("ملاحظة العميل", Icons.Filled.ReportProblem, KonoColor.Caramel) { Text(note, style = KonoFont.Callout, color = KonoColor.TextPrimary) }
        }

        Section("سجل الطلب", Icons.Filled.History) {
            d.events.forEach { ev ->
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.size(7.dp).background(KonoColor.Brown, CircleShape)) {}
                    Text(stageAr(ev.stage), style = KonoFont.Footnote, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    Text(fmt(ev.at, "h:mm a"), style = cairo(11), color = KonoColor.TextTertiary)
                }
            }
        }
    }
}

@Composable private fun Box1(modifier: Modifier) = androidx.compose.foundation.layout.Box(modifier)

@Composable
private fun Section(title: String, icon: ImageVector, tint: Color = KonoColor.Brown, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.s),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp), tint = tint)
            Text(title, style = KonoFont.Subheadline, color = tint)
        }
        content()
    }
}

@Composable
private fun TotalRow(label: String, amount: Double, tint: Color = KonoColor.TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = KonoFont.Footnote, color = KonoColor.TextSecondary, modifier = Modifier.weight(1f))
        RiyalText(amount, style = KonoFont.Footnote, color = tint)
    }
}

@Composable
fun ContactButton(icon: ImageVector, uri: String) {
    val ctx = LocalContext.current
    androidx.compose.foundation.layout.Box(
        Modifier.size(34.dp).clip(CircleShape).background(KonoColor.Brown).konoClickable(pressedScale = 0.9f) {
            runCatching { ctx.startActivity(Intent(if (uri.startsWith("tel:")) Intent.ACTION_DIAL else Intent.ACTION_VIEW, Uri.parse(uri))) }
        },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(16.dp), tint = KonoColor.TextOnBrown) }
}

@Composable
fun DirectionsLink(lat: Double, lng: Double, label: String) {
    val ctx = LocalContext.current
    Row(
        Modifier.konoClickable(pressedScale = 0.97f) {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))) }
                .onFailure { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) } }
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Directions, null, Modifier.size(20.dp), tint = KonoColor.Brown)
        Text(label, style = cairo(13, FontWeight.SemiBold), color = KonoColor.Brown)
    }
}
