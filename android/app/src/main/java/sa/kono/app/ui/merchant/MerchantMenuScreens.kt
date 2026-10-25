package sa.kono.app.ui.merchant

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sa.kono.app.data.MerchantMenuItem
import sa.kono.app.data.MerchantSession
import sa.kono.app.data.clean
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoPillButton
import sa.kono.app.ui.components.KonoSearchBar
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.customer.BottomActionBar
import sa.kono.app.ui.customer.ScreenTopBar
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import java.io.ByteArrayOutputStream

@Composable
fun MerchantMenuScreen(onItem: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { MerchantSession.ensureBootstrapped(); MerchantSession.loadMenu() }
    val items = MerchantSession.menu.filter { query.isBlank() || it.nameAr.contains(query, ignoreCase = true) }
    val unavailable = MerchantSession.menu.count { !it.inStock }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = KonoSpace.screen, end = KonoSpace.screen, top = KonoSpace.s, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("القائمة", style = KonoFont.LargeTitle, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                    KonoPillButton("إضافة صنف", { adding = true }, icon = Icons.Filled.Add)
                }
            }
            item { KonoSearchBar(query, { query = it }, placeholder = "ابحث في القائمة") }
            if (unavailable > 0) item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(KonoRadius.s)).background(KonoColor.Caramel.copy(alpha = 0.1f)).padding(KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.VisibilityOff, null, Modifier.size(15.dp), tint = KonoColor.Caramel)
                    Text("$unavailable صنف غير متاح حالياً للعملاء", style = KonoFont.Footnote, color = KonoColor.Caramel)
                }
            }
            items(items, key = { it.productId }) { it -> MenuItemRow(it, onToggle = { scope.launch { MerchantSession.toggleAvailability(it) } }, onOpen = { onItem(it.productId) }) }
        }
    }
    if (adding) AddItemSheet(onDismiss = { adding = false })
}

@Composable
private fun MenuItemRow(item: MerchantMenuItem, onToggle: () -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f, onClick = onOpen).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetImage(item.imageUrl, Modifier.size(60.dp).clip(RoundedCornerShape(KonoRadius.s)).alpha(if (item.inStock) 1f else 0.4f), Icons.Filled.Fastfood)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.nameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                RiyalText(item.price, style = KonoFont.Subheadline)
                Text("· بيع اليوم ${item.soldCount}", style = KonoFont.Footnote, color = KonoColor.TextTertiary)
            }
        }
        Switch(item.inStock, { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = KonoColor.Success, checkedThumbColor = Color.White, uncheckedTrackColor = KonoColor.SurfaceSunken, uncheckedThumbColor = Color.White, uncheckedBorderColor = KonoColor.Divider))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemSheet(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val valid = name.trim().isNotEmpty() && price.toDoubleOrNull() != null
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KonoColor.Background) {
        Column(Modifier.padding(horizontal = KonoSpace.screen).navigationBarsPadding().padding(bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Text("صنف جديد", style = KonoFont.Title2, color = KonoColor.TextPrimary)
            LabeledField("اسم الصنف", name) { name = it }
            LabeledField("السعر (ر.س)", price, KeyboardType.Decimal) { price = cleanDecimal(it) }
            LabeledField("الوصف", desc) { desc = it }
            KonoButton("إضافة الصنف", { scope.launch { MerchantSession.saveItem(null, name.trim(), desc.trim(), price.toDouble(), true); onDismiss() } }, enabled = valid)
        }
    }
}

/** Digits with at most one decimal point. */
private fun cleanDecimal(s: String): String {
    val f = s.filter { it.isDigit() || it == '.' }
    val i = f.indexOf('.')
    return if (i < 0) f else f.substring(0, i + 1) + f.substring(i + 1).replace(".", "")
}

@Composable
private fun LabeledField(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        Text(label, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
        KonoTextField(value, onChange, label, keyboardType = keyboard)
    }
}

/** Edit one menu item: photo, name, price, description, availability. */
@Composable
fun MenuItemEditScreen(productId: Int, onBack: () -> Unit) {
    val item = MerchantSession.menu.firstOrNull { it.productId == productId }
    if (item == null) { ScreenTopBar("الصنف", onBack); return }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(item.nameAr) }
    var price by remember { mutableStateOf(item.price.clean()) }
    var desc by remember { mutableStateOf(item.descAr) }
    var available by remember { mutableStateOf(item.inStock) }
    var newImage by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            newImage = withContext(Dispatchers.Default) {
                runCatching {
                    val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
                    val k = minOf(1f, 900f / maxOf(src.width, src.height))
                    val bmp = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true) else src
                    val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                }.getOrNull()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar(item.nameAr, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s, bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
            Box(Modifier.fillMaxWidth().height(180.dp).clip(KonoRadius.L)) {
                AssetImage(newImage ?: item.imageUrl, Modifier.fillMaxSize(), Icons.Filled.Fastfood)
                Row(
                    Modifier.align(Alignment.BottomStart).padding(KonoSpace.s).clip(CircleShape).background(KonoColor.Brown).konoClickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(horizontal = KonoSpace.m, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(14.dp), tint = KonoColor.TextOnBrown)
                    Text("تغيير الصورة", style = KonoFont.Caption, color = KonoColor.TextOnBrown)
                }
            }
            LabeledField("اسم الصنف", name) { name = it }
            LabeledField("السعر (ر.س)", price, KeyboardType.Decimal) { price = cleanDecimal(it) }
            LabeledField("الوصف", desc) { desc = it }
            val shape = RoundedCornerShape(KonoRadius.m)
            Row(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("متاح للطلب", style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                    Text("إيقافه يخفيه من قائمة العملاء فوراً", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                }
                Switch(available, { available = it }, colors = SwitchDefaults.colors(checkedTrackColor = KonoColor.Success, checkedThumbColor = Color.White, uncheckedTrackColor = KonoColor.SurfaceSunken, uncheckedThumbColor = Color.White, uncheckedBorderColor = KonoColor.Divider))
            }
            Column(Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                Text("مبيعات اليوم", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.l)) {
                    Column { Text("${item.soldCount}", style = KonoFont.Title2, color = KonoColor.TextPrimary); Text("طلب", style = KonoFont.Footnote, color = KonoColor.TextSecondary) }
                    Column { Text((item.soldCount * item.price).clean(), style = KonoFont.Title2, color = KonoColor.TextPrimary); Text("ر.س", style = KonoFont.Footnote, color = KonoColor.TextSecondary) }
                }
            }
        }
        BottomActionBar {
            KonoButton(if (saved) "تم الحفظ ✓" else "حفظ التغييرات", {
                scope.launch {
                    MerchantSession.saveItem(item.productId, name.trim(), desc, price.toDoubleOrNull() ?: item.price, available, newImage)
                    saved = true; delay(600); onBack()
                }
            }, enabled = name.trim().isNotEmpty())
        }
    }
}
