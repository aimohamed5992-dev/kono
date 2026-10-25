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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.launch
import sa.kono.app.data.Address
import sa.kono.app.data.AppData
import sa.kono.app.data.PaymentMethod
import sa.kono.app.data.addPayment
import sa.kono.app.data.clean
import sa.kono.app.data.deleteAddress
import sa.kono.app.data.deletePayment
import sa.kono.app.data.toggleKonoPlus
import sa.kono.app.data.updateProfile
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

fun paymentIconOf(name: String): ImageVector = when {
    name.contains("apple") || name.contains("iphone") -> Icons.Filled.PhoneIphone
    name.contains("banknote") || name.contains("cash") -> Icons.Filled.Payments
    name.contains("wallet") -> Icons.Filled.AccountBalanceWallet
    else -> Icons.Filled.CreditCard
}

// ── Account tab ─────────────────────────────────────────────────────────────

class AccountActions(
    val addresses: () -> Unit, val payments: () -> Unit, val chats: () -> Unit, val notifications: () -> Unit,
    val help: () -> Unit, val about: () -> Unit, val konoPlus: () -> Unit, val signOut: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(actions: AccountActions) {
    val me = AppData.me
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { AppData.loadAccount() }

    Column(
        Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
    ) {
        Text("حسابي", style = KonoFont.LargeTitle, color = KonoColor.Espresso)

        // header card
        val shape = RoundedCornerShape(KonoRadius.l)
        Row(
            Modifier.fillMaxWidth().konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
                .konoClickable(pressedScale = 0.99f) { editing = true }.padding(KonoSpace.l),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(KonoColor.BrandGradient), contentAlignment = Alignment.Center) {
                Text((me?.name ?: "ض").take(1), style = cairo(26, FontWeight.ExtraBold), color = KonoColor.Brown)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(me?.name ?: "ضيف", style = KonoFont.Title2, color = KonoColor.TextPrimary)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(me?.phone ?: "", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                }
            }
            Icon(Icons.Filled.Edit, "تعديل", Modifier.size(22.dp), tint = KonoColor.Brown.copy(alpha = 0.5f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            StatCard("محفظة كونو", "${(me?.walletBalance ?: 0.0).clean()} ر.س", Icons.Filled.AccountBalanceWallet, Modifier.weight(1f))
            StatCard("نقاطك", "${me?.points ?: 0}", Icons.Filled.Stars, Modifier.weight(1f))
        }

        // Kono Plus banner
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.l)).background(KonoColor.BrandGradient).konoClickable(pressedScale = 0.99f, onClick = actions.konoPlus).padding(KonoSpace.l),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(28.dp), tint = KonoColor.Brown)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("كونو بلس", style = KonoFont.Headline, color = KonoColor.Brown)
                Text(if (me?.konoPlus == true) "اشتراكك فعّال — توصيل مجاني بلا حدود" else "وفّر رسوم التوصيل طوال الشهر", style = KonoFont.Footnote, color = KonoColor.Brown.copy(alpha = 0.8f))
            }
            Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.Brown)
        }

        SettingsGroup("الطلبات والعناوين", listOf(
            SettingsRow("العناوين المحفوظة", Icons.Filled.Place, "${AppData.addresses.size}", actions.addresses),
            SettingsRow("طرق الدفع", Icons.Filled.CreditCard, "${AppData.payments.size}", actions.payments),
        ))
        SettingsGroup("الحساب", listOf(
            SettingsRow("المحادثات", Icons.Filled.ChatBubble, null, actions.chats),
            SettingsRow("الإشعارات", Icons.Filled.Notifications, null, actions.notifications),
            SettingsRow("المساعدة والدعم", Icons.Filled.Help, null, actions.help),
            SettingsRow("عن كونو", Icons.Filled.Info, "١.٠", actions.about),
        ))

        sa.kono.app.ui.i18n.LanguageToggleRow()

        Row(
            Modifier.fillMaxWidth().height(50.dp).clip(KonoRadius.M).background(KonoColor.Danger.copy(alpha = 0.1f)).konoClickable(onClick = actions.signOut),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.s, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Logout, null, Modifier.size(20.dp), tint = KonoColor.Danger)
            Text("تسجيل الخروج", style = KonoFont.Button, color = KonoColor.Danger)
        }
    }

    if (editing) EditProfileSheet(me?.name.orEmpty(), me?.email.orEmpty(), onDismiss = { editing = false })
}

private class SettingsRow(val title: String, val icon: ImageVector, val detail: String?, val onClick: () -> Unit)

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(modifier.clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        Icon(icon, null, Modifier.size(22.dp), tint = KonoColor.Caramel)
        Text(value, style = KonoFont.Title2, color = KonoColor.TextPrimary)
        Text(title, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
    }
}

@Composable
private fun SettingsGroup(title: String, rows: List<SettingsRow>) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Text(title, style = KonoFont.Subheadline, color = KonoColor.TextSecondary, modifier = Modifier.padding(horizontal = KonoSpace.xs))
        Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)) {
            rows.forEachIndexed { i, r ->
                Row(
                    Modifier.fillMaxWidth().konoClickable(pressedScale = 0.995f, onClick = r.onClick).padding(vertical = KonoSpace.m, horizontal = KonoSpace.l),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(r.icon, null, Modifier.size(22.dp), tint = KonoColor.Brown)
                    Text(r.title, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    r.detail?.let { Text(it, style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
                    Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
                }
                if (i < rows.lastIndex) Divider(Modifier.padding(start = 52.dp), color = KonoColor.Divider)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(name: String, email: String, onDismiss: () -> Unit) {
    var n by remember { mutableStateOf(name) }
    var e by remember { mutableStateOf(email) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KonoColor.Background) {
        Column(Modifier.padding(horizontal = KonoSpace.screen).navigationBarsPadding().padding(bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Text("تعديل الملف", style = KonoFont.Title2, color = KonoColor.TextPrimary)
            KonoTextField(n, { n = it }, "الاسم", icon = Icons.Filled.Person)
            KonoTextField(e, { e = it }, "البريد الإلكتروني", icon = Icons.Filled.Email, keyboardType = KeyboardType.Email)
            KonoButton("حفظ", { scope.launch { AppData.updateProfile(n.trim(), e.trim().ifEmpty { null }); onDismiss() } }, enabled = n.trim().length >= 2)
        }
    }
}

// ── Addresses ───────────────────────────────────────────────────────────────

@Composable
fun AddressesScreen(onBack: () -> Unit, onAdd: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AppData.loadAccount() }
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("العناوين المحفوظة", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            if (AppData.addresses.isEmpty()) KonoEmptyState("لا توجد عناوين", icon = Icons.Filled.Place, message = "أضف عنواناً لتصلك الخدمات القريبة منك.", modifier = Modifier.padding(top = KonoSpace.xxl))
            AppData.addresses.forEach { a ->
                AddressRow(a, AppData.currentAddress?.id == a.id, onUse = { scope.launch { AppData.selectAddress(a) } }, onDelete = { scope.launch { AppData.deleteAddress(a) } })
            }
        }
        BottomActionBar { KonoButton("إضافة عنوان جديد", onAdd, icon = Icons.Filled.Add) }
    }
}

@Composable
private fun AddressRow(a: Address, current: Boolean, onUse: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f, onClick = onUse).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(shape).background(KonoColor.Cream), contentAlignment = Alignment.Center) { Icon(addressIcon(a.icon), null, Modifier.size(22.dp), tint = KonoColor.Brown) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.labelAr, style = KonoFont.Headline, color = KonoColor.TextPrimary)
                if (current) KonoTag("مختار", color = KonoColor.Success) else if (a.isDefault) KonoTag("افتراضي", color = KonoColor.Brown)
            }
            Text(a.lineAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Icon(Icons.Filled.MoreVert, "المزيد", Modifier.size(32.dp).konoClickable(pressedScale = 0.9f) { menu = true }.padding(5.dp), tint = KonoColor.TextSecondary)
            DropdownMenu(menu, { menu = false }, containerColor = KonoColor.Surface) {
                DropdownMenuItem(text = { Text("استخدام هذا العنوان", style = KonoFont.Body) }, onClick = { menu = false; onUse() })
                DropdownMenuItem(text = { Text("حذف", style = KonoFont.Body, color = KonoColor.Danger) }, onClick = { menu = false; confirm = true })
            }
        }
    }
    if (confirm) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirm = false },
        confirmButton = { androidx.compose.material3.TextButton({ confirm = false; onDelete() }) { Text("حذف", style = KonoFont.Button, color = KonoColor.Danger) } },
        dismissButton = { androidx.compose.material3.TextButton({ confirm = false }) { Text("إلغاء", style = KonoFont.Button, color = KonoColor.Brown) } },
        title = { Text("حذف «${a.labelAr}»؟", style = KonoFont.Headline) }, containerColor = KonoColor.Surface,
    )
}

// ── Payment methods ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { AppData.loadAccount() }
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("طرق الدفع", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            if (AppData.payments.isEmpty()) KonoEmptyState("لا توجد طرق دفع", icon = Icons.Filled.CreditCard, message = "أضف بطاقة أو محفظة.", modifier = Modifier.padding(top = KonoSpace.xxl))
            AppData.payments.forEach { pm -> PaymentRow(pm) { scope.launch { AppData.deletePayment(pm) } } }
        }
        BottomActionBar { KonoButton("إضافة طريقة دفع", { showAdd = true }, icon = Icons.Filled.Add) }
    }
    if (showAdd) AddPaymentSheet(onDismiss = { showAdd = false })
}

@Composable
private fun PaymentRow(pm: PaymentMethod, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(shape).background(KonoColor.Cream), contentAlignment = Alignment.Center) { Icon(paymentIconOf(pm.icon), null, Modifier.size(22.dp), tint = KonoColor.Brown) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pm.labelAr, style = KonoFont.Headline, color = KonoColor.TextPrimary)
            pm.detailAr?.let { Text(it, style = KonoFont.Footnote, color = KonoColor.TextSecondary) }
        }
        if (pm.isDefault) KonoTag("افتراضي", color = KonoColor.Brown)
        Icon(Icons.Filled.Delete, "حذف", Modifier.size(36.dp).konoClickable(pressedScale = 0.9f, onClick = onDelete).padding(8.dp), tint = KonoColor.Danger)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentSheet(onDismiss: () -> Unit) {
    var kind by remember { mutableIntStateOf(0) }
    var last4 by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KonoColor.Background) {
        Column(Modifier.padding(horizontal = KonoSpace.screen).navigationBarsPadding().padding(bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Text("طريقة دفع جديدة", style = KonoFont.Title2, color = KonoColor.TextPrimary)
            SegmentedTabs(listOf("مدى", "بطاقة ائتمان", "Apple Pay"), kind, { kind = it })
            if (kind != 2) KonoTextField(last4, { last4 = it.filter(Char::isDigit).take(4) }, "آخر ٤ أرقام", icon = Icons.Filled.CreditCard, keyboardType = KeyboardType.Number)
            KonoButton("إضافة", {
                val label = listOf("مدى", "بطاقة ائتمان", "Apple Pay")[kind]
                val icon = if (kind == 2) "apple.logo" else "creditcard.fill"
                scope.launch { AppData.addPayment(label, icon, if (last4.isEmpty() || kind == 2) null else "**** $last4"); onDismiss() }
            }, enabled = kind == 2 || last4.length == 4)
        }
    }
}

// ── Notifications / Kono Plus / info ────────────────────────────────────────

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("الإشعارات", onBack)
        KonoEmptyState("لا توجد إشعارات", icon = Icons.Filled.NotificationsOff, message = "سنخبرك هنا بحالة طلباتك وأحدث العروض.", modifier = Modifier.padding(top = 60.dp))
    }
}

@Composable
fun KonoPlusScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    val plus = AppData.me?.konoPlus == true
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("كونو بلس", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.l)).background(KonoColor.BrandGradient).padding(vertical = KonoSpace.xl),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.s),
            ) {
                Icon(Icons.Filled.WorkspacePremium, null, Modifier.size(48.dp), tint = KonoColor.Brown)
                Text("كونو بلس", style = KonoFont.display(30), color = KonoColor.Espresso)
                Text(if (plus) "اشتراكك فعّال" else "٢٩ ر.س / شهرياً", style = KonoFont.Headline, color = KonoColor.TextSecondary)
            }
            val shape = RoundedCornerShape(KonoRadius.m)
            Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
                Benefit(Icons.Filled.DeliveryDining, "توصيل مجاني", "على كل الطلبات فوق ٣٠ ر.س")
                Benefit(Icons.Filled.Percent, "خصومات حصرية", "عروض أسبوعية لأعضاء بلس فقط")
                Benefit(Icons.Filled.Bolt, "أولوية في الدعم", "ردّ أسرع على استفساراتك")
            }
            KonoButton(if (plus) "إلغاء الاشتراك" else "اشترك الآن", { scope.launch { working = true; AppData.toggleKonoPlus(); working = false } }, kind = if (plus) KonoButtonKind.Secondary else KonoButtonKind.Primary, loading = working)
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, sub: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(KonoColor.Cream), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = KonoColor.Brown) }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            Text(sub, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        }
    }
}

@Composable
fun SimpleInfoScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar(title, onBack)
        Text(body, style = KonoFont.Body.copy(lineHeight = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)), color = KonoColor.TextPrimary, modifier = Modifier.padding(horizontal = KonoSpace.screen, vertical = KonoSpace.l))
    }
}
