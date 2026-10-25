package sa.kono.app.ui.onboarding

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import sa.kono.app.data.OnboardingDoc
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.merchant.StatusChip
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSize
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale

/** Building blocks shared by the merchant and courier registration wizards. */
@Composable
fun OBProgress(step: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i <= step) KonoColor.Brown else KonoColor.Brown.copy(alpha = 0.18f))) }
    }
}

@Composable
fun OBSection(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = KonoFont.Title2, color = KonoColor.TextPrimary)
            if (subtitle != null) Text(subtitle, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        }
        content()
    }
}

@Composable
private fun LabelRow(label: String, required: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = KonoFont.Subheadline, color = KonoColor.TextSecondary)
        if (required) Text("*", style = KonoFont.Subheadline, color = KonoColor.Danger)
    }
}

@Composable
fun OBField(label: String, value: String, onChange: (String) -> Unit, required: Boolean = false, icon: ImageVector? = null, keyboard: KeyboardType = KeyboardType.Text, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        LabelRow(label, required)
        KonoTextField(value, onChange, hint ?: label, icon = icon, keyboardType = keyboard)
    }
}

/** Dropdown picker; [options] are (value, title). */
@Composable
fun OBPicker(label: String, selection: String, onSelect: (String) -> Unit, options: List<Pair<String, String>>, required: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        LabelRow(label, required)
        Box {
            val shape = RoundedCornerShape(KonoRadius.m)
            Row(
                Modifier.fillMaxWidth().height(KonoSize.fieldHeight).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f) { open = true }.padding(horizontal = KonoSpace.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(options.firstOrNull { it.first == selection }?.second ?: "اختر", style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.UnfoldMore, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
            }
            DropdownMenu(open, { open = false }, containerColor = KonoColor.Surface) {
                options.forEach { (v, t) -> DropdownMenuItem(text = { Text(t, style = KonoFont.Body) }, onClick = { onSelect(v); open = false }) }
            }
        }
    }
}

enum class DateKind { Birth, Expiry }

/** Date field storing `yyyy-MM-dd`; birth is limited to 18–75 years ago, expiry to the next 10 years. */
@Composable
fun OBDateField(label: String, value: String, onChange: (String) -> Unit, required: Boolean = false, kind: DateKind = DateKind.Birth) {
    val ctx = LocalContext.current
    fun show() {
        val cal = Calendar.getInstance()
        runCatching { val (y, m, d) = value.split("-").map { it.toInt() }; cal.set(y, m - 1, d) }.onFailure {
            cal.add(Calendar.YEAR, if (kind == DateKind.Birth) -28 else 1)
        }
        val dlg = DatePickerDialog(ctx, { _, y, m, d -> onChange(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        val now = Calendar.getInstance()
        if (kind == DateKind.Birth) {
            dlg.datePicker.minDate = Calendar.getInstance().apply { add(Calendar.YEAR, -75) }.timeInMillis
            dlg.datePicker.maxDate = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }.timeInMillis
        } else {
            dlg.datePicker.minDate = now.timeInMillis
            dlg.datePicker.maxDate = Calendar.getInstance().apply { add(Calendar.YEAR, 10) }.timeInMillis
        }
        dlg.show()
    }
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        LabelRow(label, required)
        val shape = RoundedCornerShape(KonoRadius.m)
        Row(
            Modifier.fillMaxWidth().height(KonoSize.fieldHeight).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f) { show() }.padding(horizontal = KonoSpace.l),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp), tint = KonoColor.Brown)
            Text(value.ifEmpty { "اختر التاريخ" }, style = KonoFont.Body, color = if (value.isEmpty()) KonoColor.TextTertiary else KonoColor.TextPrimary)
        }
    }
}

/** A checklist row: attach a photo/scan (the prototype records name + type only). */
@Composable
fun OBDocRow(doc: OnboardingDoc, onChange: (OnboardingDoc) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onChange(doc.copy(provided = true, fileName = doc.fileName.ifEmpty { "${doc.type}.jpg" }))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (doc.provided) Icons.Filled.Verified else Icons.Filled.Description, null, Modifier.size(24.dp), tint = if (doc.provided) KonoColor.Success else KonoColor.Brown)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(doc.labelAr, style = KonoFont.Callout, color = KonoColor.TextPrimary)
            Text(if (doc.provided) "تم الإرفاق" else if (doc.required) "مطلوب" else "اختياري", style = KonoFont.Caption, color = if (doc.provided) KonoColor.Success else if (doc.required) KonoColor.Danger else KonoColor.TextTertiary)
        }
        Text(
            if (doc.provided) "تغيير" else "إرفاق", style = cairo(11, FontWeight.Bold), color = KonoColor.TextOnBrown,
            modifier = Modifier.clip(CircleShape).background(KonoColor.Brown).konoClickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(horizontal = KonoSpace.m, vertical = 7.dp),
        )
    }
}

/** Logo picker: downscales to 512 px and stores a JPEG data URI. */
@Composable
fun OBImageCard(title: String, dataUri: String?, onChange: (String) -> Unit, required: Boolean = false) {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) runCatching {
            val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            val k = minOf(1f, 512f / maxOf(src.width, src.height))
            val bmp = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true) else src
            val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
            onChange("data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
        }
    }
    val preview = remember(dataUri) { dataUri?.let { runCatching { val b = Base64.decode(it.substringAfter(','), Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap() }.getOrNull() } }
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        LabelRow(title, required)
        val shape = RoundedCornerShape(KonoRadius.m)
        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(shape).background(KonoColor.Cream).border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) androidx.compose.foundation.Image(preview, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(30.dp), tint = KonoColor.Brown.copy(alpha = 0.7f))
                Text("اختر صورة", style = KonoFont.Footnote, color = KonoColor.Brown.copy(alpha = 0.7f))
            }
        }
    }
}

/** A compact map with a fixed centre pin; the button pins the store location at the map centre. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OBMapPicker(label: String, latitude: Double?, longitude: Double?, onPick: (Double, Double) -> Unit, required: Boolean = false) {
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
        LabelRow(label, required)
        val shape = RoundedCornerShape(KonoRadius.m)
        Box(Modifier.fillMaxWidth().height(200.dp).clip(shape).border(1.dp, KonoColor.Hairline, shape)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    Configuration.getInstance().userAgentValue = c.packageName
                    MapView(c).apply {
                        setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(15.0); controller.setCenter(GeoPoint(latitude ?: 24.7136, longitude ?: 46.6753))
                        setOnTouchListener { v, _ -> v.parent?.requestDisallowInterceptTouchEvent(true); false }
                        mapRef = this
                    }
                },
                onRelease = { it.onDetach() },
            )
            Icon(Icons.Filled.PinDrop, null, Modifier.align(Alignment.Center).size(34.dp).padding(bottom = 16.dp), tint = KonoColor.Brown)
        }
        Row(
            Modifier.konoClickable(pressedScale = 0.98f) { mapRef?.let { onPick(it.mapCenter.latitude, it.mapCenter.longitude) } },
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (latitude == null) Icons.Filled.PinDrop else Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = if (latitude == null) KonoColor.Brown else KonoColor.Success)
            Text(if (latitude == null) "تثبيت موقع المتجر على الدبوس" else "تم تثبيت الموقع — اضغط للتحديث", style = cairo(13, FontWeight.SemiBold), color = if (latitude == null) KonoColor.Brown else KonoColor.Success)
        }
    }
}

@Composable
fun OBConsent(agreed: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.Top) {
        Icon(if (agreed) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, null, Modifier.size(24.dp), tint = if (agreed) KonoColor.Brown else KonoColor.TextTertiary)
        Text("أقرّ بصحة جميع البيانات والوثائق المقدّمة، وأوافق على اتفاقية الشراكة وسياسة الخصوصية لمنصة كونو.", style = KonoFont.Footnote, color = KonoColor.TextSecondary, modifier = Modifier.weight(1f))
    }
}

/** A pill showing the application state ("" or approved = hidden). */
@Composable
fun OnboardingStatusChip(status: String) = StatusChip(status)
