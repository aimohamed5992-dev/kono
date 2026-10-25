package sa.kono.app.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.MerchantSession
import sa.kono.app.data.StoreUpdateBody
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.components.KonoShadowCard
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.components.RatingBadge
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable

private fun arDigits(n: Int) = n.toString().map { if (it in '0'..'9') "٠١٢٣٤٥٦٧٨٩"[it - '0'] else it }.joinToString("")

@Composable
fun MerchantStoreScreen(onChats: () -> Unit, onInfo: (String, String) -> Unit, onSignOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var prep by remember { mutableFloatStateOf(15f) }
    LaunchedEffect(Unit) { MerchantSession.ensureBootstrapped(); MerchantSession.loadStore(); MerchantSession.loadReports() }
    val p = MerchantSession.store
    LaunchedEffect(p?.prepMinutes) { p?.let { prep = it.prepMinutes.toFloat() } }

    Column(
        Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
    ) {
        Text("المتجر", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary)

        if (p != null) {
            KonoShadowCard(Modifier.konoClickable(pressedScale = 0.99f) { editing = true }) {
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                    AssetImage(p.logoUrl.ifEmpty { p.heroImageUrl }, Modifier.size(64.dp).clip(CircleShape), Icons.Filled.Storefront, KonoColor.Caramel)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(p.name, style = KonoFont.Title2, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                            RatingBadge(MerchantSession.reports?.rating ?: p.rating, count = maxOf(p.ratingCount, 1), compact = true)
                            StatusChip(p.onboardingStatus)
                        }
                        Text(p.addressAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Filled.Edit, "تعديل", Modifier.size(20.dp), tint = KonoColor.Brown)
                }
            }
            if (p.onboardingStatus == "under_review") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.m)).background(KonoColor.Caramel.copy(alpha = 0.1f)).padding(KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PendingActions, null, Modifier.size(18.dp), tint = KonoColor.Caramel)
                    Text("طلب تسجيل متجرك قيد المراجعة — بعض الميزات محدودة حتى الموافقة.", style = KonoFont.Footnote, color = KonoColor.Caramel)
                }
            }

            // prep time
            val shape = RoundedCornerShape(KonoRadius.m)
            Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("متوسط وقت التجهيز", style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    Text("${prep.toInt()} دقيقة", style = KonoFont.Price, color = KonoColor.Brown)
                }
                Slider(
                    prep, { prep = it }, valueRange = 5f..45f, steps = 7,
                    onValueChangeFinished = { scope.launch { MerchantSession.setPrep(prep.toInt()) } },
                    colors = SliderDefaults.colors(thumbColor = KonoColor.Brown, activeTrackColor = KonoColor.Brown, inactiveTrackColor = KonoColor.Divider, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
                )
                Text("يُعرض للعميل ويؤثر على وقت التوصيل المتوقع.", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }

            Row(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("وضع الازدحام", style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                    Text("يضيف ١٥ دقيقة لوقت التجهيز مؤقتاً", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                }
                Switch(p.isBusy, { v -> scope.launch { MerchantSession.setBusy(v) } }, colors = SwitchDefaults.colors(checkedTrackColor = KonoColor.Caramel, checkedThumbColor = Color.White, uncheckedTrackColor = KonoColor.SurfaceSunken, uncheckedThumbColor = Color.White, uncheckedBorderColor = KonoColor.Divider))
            }

            Group("المتجر", listOf(
                Row_("ساعات العمل", Icons.Filled.AccessTime, p.openingHoursAr.ifEmpty { "غير محدد" }) { editing = true },
                Row_("منطقة التوصيل", Icons.Filled.Map, "ضمن ${arDigits(p.deliveryRadiusKm.toInt())} كم") {
                    onInfo("منطقة التوصيل", "طلباتك تُعرض على المندوبين ضمن ${arDigits(p.deliveryRadiusKm.toInt())} كم من موقع المتجر (${p.addressAr}). العملاء خارج هذا النطاق لا يرون متجرك في «الأقرب إليك».")
                },
                Row_("الحساب البنكي والتحويلات", Icons.Filled.AccountBalance, if (p.ibanLast4.isEmpty()) "أضف حساباً" else "\u200E•••• ${p.ibanLast4}") { editing = true },
                Row_("حساب المتجر", Icons.Filled.Badge, p.ownerNameAr.ifEmpty { null }) {
                    onInfo("حساب المتجر", "المالك: ${p.ownerNameAr}\nالجوال: ${p.ownerPhone}\n\nهذا هو الحساب الوحيد الذي يدير المتجر حالياً. لإضافة موظفين بصلاحيات محدودة تواصل مع دعم الشركاء.")
                },
                Row_("عدد الأصناف في القائمة", Icons.Filled.List, "${arDigits(p.menuItemCount)} صنف", null),
            ))
            sa.kono.app.ui.i18n.LanguageToggleRow()
            Group("التواصل", listOf(Row_("محادثات العملاء والمناديب", Icons.Filled.ChatBubble, null, onChats)))
            Group("الدعم", listOf(
                Row_("مركز مساعدة الشركاء", Icons.Filled.Help, null) { onInfo("مركز مساعدة الشركاء", "أدلة التشغيل، سياسات الإلغاء والاسترجاع، وحل المشكلات الشائعة متوفرة في مركز الشركاء. للطوارئ اتصل على ٩٢٠٠٠٠٠٠٠.") },
                Row_("تواصل مع كونو للأعمال", Icons.Filled.SupportAgent, null) { onInfo("تواصل مع كونو للأعمال", "فريق حسابات الشركاء متاح يومياً ٩ ص – ١١ م عبر الدردشة، أو على partners@kono.sa خلال ٢٤ ساعة.") },
            ))
        }

        Text(
            "تسجيل الخروج", style = cairo(14, FontWeight.SemiBold), color = KonoColor.Danger,
            modifier = Modifier.fillMaxWidth().height(46.dp).konoClickable(onClick = onSignOut).wrapContentCenter(),
        )
    }

    if (editing && p != null) StoreEditorSheet(p.name, p.cuisineAr, p.addressAr, p.openingHoursAr, p.ibanLast4, onDismiss = { editing = false })
}

private fun Modifier.wrapContentCenter() = this.then(Modifier.wrapContentSize(Alignment.Center))

private class Row_(val title: String, val icon: ImageVector, val trailing: String?, val onClick: (() -> Unit)?)

@Composable
private fun Group(title: String, rows: List<Row_>) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Text(title, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
        Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)) {
            rows.forEachIndexed { i, r ->
                Row(
                    Modifier.fillMaxWidth().then(if (r.onClick != null) Modifier.konoClickable(pressedScale = 0.995f, onClick = r.onClick) else Modifier).padding(vertical = KonoSpace.m, horizontal = KonoSpace.l),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(r.icon, null, Modifier.size(22.dp), tint = KonoColor.Brown)
                    Text(r.title, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    r.trailing?.let { Text(it, style = KonoFont.Footnote, color = KonoColor.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp)) }
                    if (r.onClick != null) Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
                }
                if (i < rows.lastIndex) Divider(Modifier.padding(start = 52.dp), color = KonoColor.Divider)
            }
        }
    }
}

/** Review status chip for the registration application ("" = none). */
@Composable
fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "under_review" -> "قيد المراجعة" to KonoColor.Caramel
        "approved" -> "معتمد" to KonoColor.Success
        "rejected" -> "مرفوض" to KonoColor.Danger
        else -> return
    }
    sa.kono.app.ui.components.KonoTag(label, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreEditorSheet(name0: String, cuisine0: String, address0: String, hours0: String, iban0: String, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(name0) }
    var cuisine by remember { mutableStateOf(cuisine0) }
    var address by remember { mutableStateOf(address0) }
    var hours by remember { mutableStateOf(hours0) }
    var iban by remember { mutableStateOf(iban0) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KonoColor.Background) {
        Column(Modifier.padding(horizontal = KonoSpace.screen).navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            Text("معلومات المتجر", style = KonoFont.Title2, color = KonoColor.TextPrimary)
            Field("اسم المتجر", name, Icons.Filled.Storefront) { name = it }
            Field("التصنيف", cuisine, Icons.Filled.Category) { cuisine = it }
            Field("عنوان المتجر", address, Icons.Filled.Place) { address = it }
            Field("ساعات العمل", hours, Icons.Filled.AccessTime) { hours = it }
            Field("آخر ٤ أرقام من الآيبان", iban, Icons.Filled.AccountBalance, KeyboardType.Number) { iban = it.filter(Char::isDigit).take(4) }
            KonoButton("حفظ", {
                scope.launch {
                    MerchantSession.updateStore(StoreUpdateBody(name.trim(), cuisine.trim(), address.trim(), hours.trim(), iban.ifEmpty { null }))
                    onDismiss()
                }
            }, enabled = name.trim().isNotEmpty())
        }
    }
}

@Composable
private fun Field(label: String, value: String, icon: ImageVector, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        Text(label, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
        KonoTextField(value, onChange, label, icon = icon, keyboardType = keyboard)
    }
}
