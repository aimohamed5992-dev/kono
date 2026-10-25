package sa.kono.app.ui.customer

import android.annotation.SuppressLint
import android.location.Geocoder
import android.view.MotionEvent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import sa.kono.app.data.Address
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import java.util.Locale

private val labels = listOf(Triple("المنزل", "house.fill", Icons.Filled.Home), Triple("العمل", "briefcase.fill", Icons.Filled.Work), Triple("آخر", "mappin.circle.fill", Icons.Filled.PinDrop))

/** "Set your delivery location": map with a fixed centre pin + a bottom sheet that confirms the address. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun LocationSetupScreen(onConfirm: (labelAr: String, lineAr: String, cityAr: String, icon: String, lat: Double, lng: Double) -> Unit, onBack: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    var lat by remember { mutableDoubleStateOf(24.7136) }
    var lng by remember { mutableDoubleStateOf(46.6753) }
    var moving by remember { mutableStateOf(false) }
    var district by remember { mutableStateOf("جارٍ تحديد الموقع…") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("الرياض") }
    var selected by remember { mutableStateOf("المنزل") }
    var detail by remember { mutableStateOf("") }
    var mapRef by remember { mutableStateOf<MapView?>(null) }

    // Real reverse-geocode of the map centre (Arabic), debounced by the map listener.
    LaunchedEffect(lat, lng) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(ctx, Locale("ar", "SA")).getFromLocation(lat, lng, 1)?.firstOrNull()
            }.getOrNull()
        }
        if (result != null) {
            district = result.subLocality ?: result.locality ?: "موقع محدد"
            city = result.locality ?: result.adminArea ?: "الرياض"
            street = listOfNotNull(
                listOfNotNull(result.thoroughfare, result.subThoroughfare).joinToString(" ").ifEmpty { null }, city,
            ).joinToString("، ")
        } else {
            district = "موقع محدد"
            street = "%.4f, %.4f".format(Locale.US, lat, lng)
        }
    }

    Box(Modifier.fillMaxSize().background(KonoColor.Background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                Configuration.getInstance().userAgentValue = c.packageName
                MapView(c).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(lat, lng))
                    setOnTouchListener { v, e ->
                        if (e.action == MotionEvent.ACTION_MOVE) moving = true
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        false
                    }
                    addMapListener(DelayedMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { settle(this@apply); return false }
                        override fun onZoom(event: ZoomEvent?): Boolean { settle(this@apply); return false }
                        private fun settle(m: MapView) {
                            lat = m.mapCenter.latitude; lng = m.mapCenter.longitude; moving = false
                        }
                    }, 350))
                    mapRef = this
                }
            },
            onRelease = { it.onDetach() },
        )

        CenterPin(moving, Modifier.align(Alignment.Center).offset(y = (-22).dp))

        // top bar
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) KonoBackButton(onBack) else Box(Modifier.size(40.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "تحديد موقع التوصيل", style = KonoFont.Headline, color = KonoColor.TextPrimary,
                    modifier = Modifier.clip(CircleShape).background(KonoColor.Surface.copy(alpha = 0.92f)).padding(horizontal = KonoSpace.l, vertical = 8.dp),
                )
            }
            Box(
                Modifier.size(40.dp).konoShadow(KonoShadow.Md, CircleShape).clip(CircleShape).background(KonoColor.Surface)
                    .konoClickable(pressedScale = 0.9f) { mapRef?.controller?.animateTo(GeoPoint(24.7742, 46.7386)) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.MyLocation, "موقعي", Modifier.size(20.dp), tint = KonoColor.Brown) }
        }

        // bottom sheet
        val sheetShape = RoundedCornerShape(topStart = KonoRadius.xl, topEnd = KonoRadius.xl)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().konoShadow(KonoShadow.Lg, sheetShape).clip(sheetShape)
                .background(KonoColor.Surface).imePadding().navigationBarsPadding().padding(KonoSpace.l),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(KonoColor.Divider))
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(KonoColor.Yellow.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PinDrop, null, Modifier.size(22.dp), tint = KonoColor.Brown)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(district, style = KonoFont.Headline, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(street, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                labels.forEach { (label, _, icon) -> KonoChip(label, icon = icon, selected = selected == label) { selected = label } }
            }
            KonoTextField(detail, { detail = it }, "تفاصيل إضافية (رقم الشقة، علامة مميزة)", icon = Icons.Filled.Notes)
            KonoButton("تأكيد الموقع", {
                val base = street.ifEmpty { district }
                val line = if (detail.isBlank()) base else "$base · $detail"
                onConfirm(selected, line, city, labels.first { it.first == selected }.second, lat, lng)
            }, icon = Icons.Filled.Check)
        }
    }
}

/** Brown pin that stays fixed while the map moves under it, lifting while dragging. */
@Composable
private fun CenterPin(lifted: Boolean, modifier: Modifier = Modifier) {
    val lift by animateDpAsState(if (lifted) (-10).dp else 0.dp, KonoMotion.snappy(), label = "lift")
    Column(modifier.offset(y = lift), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.size(34.dp).konoShadow(KonoShadow.Md, CircleShape).background(KonoColor.Brown, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Restaurant, null, Modifier.size(16.dp), tint = KonoColor.Yellow)
        }
        Box(Modifier.size(width = 12.dp, height = 8.dp).drawBehind {
            drawPath(Path().apply { moveTo(size.width / 2, size.height); lineTo(0f, 0f); lineTo(size.width, 0f); close() }, KonoColor.Brown)
        })
        Box(Modifier.size(width = if (lifted) 18.dp else 12.dp, height = 5.dp).background(KonoColor.Brown.copy(alpha = if (lifted) 0.15f else 0.28f), CircleShape))
    }
}
