package sa.kono.app.ui.customer

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import sa.kono.app.R
import sa.kono.app.data.AppData
import sa.kono.app.data.OrderDto
import sa.kono.app.data.OrderStage
import sa.kono.app.data.order
import sa.kono.app.data.storeDetail
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.RatingBadge
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import kotlin.random.Random

/** Live order tracking: map with store / home / courier, plus a status sheet. Polls the server every 4 s. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun TrackingScreen(orderId: Int, justPlaced: Boolean, onClose: () -> Unit, onChat: (Int) -> Unit = {}) {
    val ctx = LocalContext.current
    var order by remember { mutableStateOf<OrderDto?>(null) }
    var storePoint by remember { mutableStateOf<GeoPoint?>(null) }
    var confetti by remember { mutableStateOf(justPlaced) }

    LaunchedEffect(orderId) {
        while (true) {
            AppData.order(orderId)?.let { order = it }
            delay(4000)
        }
    }
    LaunchedEffect(order?.storeId) {
        val sid = order?.storeId ?: return@LaunchedEffect
        AppData.storeDetail(sid)?.let { storePoint = GeoPoint(it.latitude, it.longitude) }
    }
    LaunchedEffect(Unit) { if (justPlaced) { delay(2400); confetti = false } }

    val o = order
    val home = o?.let { GeoPoint(it.addressLat, it.addressLng) }

    Box(Modifier.fillMaxSize().background(KonoColor.Background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                Configuration.getInstance().userAgentValue = c.packageName
                MapView(c).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(14.5)
                    setOnTouchListener { v, _ -> v.parent?.requestDisallowInterceptTouchEvent(true); false }
                }
            },
            update = { map ->
                map.overlays.clear()
                val s = storePoint; val h = home
                if (s != null && h != null && o != null) {
                    val courierPoint = o.courier?.takeIf { it.latitude != 0.0 }?.let { GeoPoint(it.latitude, it.longitude) }
                    val onTheWay = o.stageEnum.ordinal >= OrderStage.OnTheWay.ordinal
                    if (onTheWay && courierPoint != null) {
                        map.overlays.add(Polyline().apply { setPoints(listOf(s, courierPoint)); outlinePaint.color = KonoColor.Brown.toArgb(); outlinePaint.strokeWidth = 10f })
                        map.overlays.add(Polyline().apply {
                            setPoints(listOf(courierPoint, h)); outlinePaint.color = KonoColor.Brown.copy(alpha = 0.3f).toArgb(); outlinePaint.strokeWidth = 10f
                            outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 24f), 0f)
                        })
                    }
                    fun pin(p: GeoPoint, color: Color, glyph: Int) = Marker(map).apply {
                        position = p; icon = mapPin(map.context, color.toArgb(), glyph); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null)
                    }
                    map.overlays.add(pin(s, KonoColor.Caramel, R.drawable.ic_pin_store))
                    map.overlays.add(pin(h, KonoColor.Brown, R.drawable.ic_pin_home))
                    if (courierPoint != null && o.stageEnum.ordinal >= OrderStage.Confirmed.ordinal) map.overlays.add(pin(courierPoint, KonoColor.Info, R.drawable.ic_pin_courier))
                    if (map.tag == null) {
                        map.tag = true
                        map.controller.setCenter(GeoPoint((s.latitude + h.latitude) / 2, (s.longitude + h.longitude) / 2))
                        map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(s, h)).increaseByScale(1.8f), false)
                    }
                }
                map.invalidate()
            },
            onRelease = { it.onDetach() },
        )

        // top bar
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).konoShadow(KonoShadow.Md, CircleShape).clip(CircleShape).background(KonoColor.Surface).konoClickable(pressedScale = 0.9f, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(if (justPlaced) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, "إغلاق", Modifier.size(20.dp), tint = KonoColor.Brown) }
            Box(Modifier.weight(1f))
            Text(
                o?.shortCode ?: "", style = KonoFont.Subheadline, color = KonoColor.TextPrimary,
                modifier = Modifier.konoShadow(KonoShadow.Sm, CircleShape).clip(CircleShape).background(KonoColor.Surface).padding(horizontal = KonoSpace.m, vertical = 8.dp),
            )
        }

        // status sheet
        if (o != null) StatusSheet(o, onChat, Modifier.align(Alignment.BottomCenter))
        if (confetti) Confetti(Modifier.fillMaxSize())
    }
}

@Composable
private fun StatusSheet(o: OrderDto, onChat: (Int) -> Unit, modifier: Modifier) {
    val stage = o.stageEnum
    val shape = RoundedCornerShape(KonoRadius.xl)
    Column(
        modifier.navigationBarsPadding().padding(horizontal = KonoSpace.screen).padding(bottom = KonoSpace.s)
            .konoShadow(KonoShadow.Lg, shape).clip(shape).background(KonoColor.Surface).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(KonoColor.Divider))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(stage.titleAr, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "stageTitle") {
                    Text(it, style = KonoFont.Title2, color = KonoColor.TextPrimary)
                }
                Text(
                    if (stage == OrderStage.Delivered) "نتمنى لك وجبة هنية" else "الوصول المتوقع ${o.storeEtaMinutes} دقيقة",
                    style = KonoFont.Footnote, color = KonoColor.TextSecondary,
                )
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(KonoColor.Yellow.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Icon(stage.icon(), null, Modifier.size(24.dp), tint = KonoColor.Brown)
            }
        }
        StageProgressBar(stage)
        if (stage != OrderStage.Delivered) sa.kono.app.ui.chat.OrderChatButtons(o.id, onChat)
        val c = o.courier
        if (c != null && stage.ordinal >= OrderStage.Confirmed.ordinal) {
            Divider(color = KonoColor.Divider)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
                AssetImage(c.photoUrl, Modifier.size(48.dp).clip(CircleShape), Icons.Filled.Person, KonoColor.Brown)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(c.name, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(c.rating, compact = true)
                        Text("· ${c.vehicleAr} · ${c.plateAr}", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                    }
                }
            }
        }
    }
}

/** Lightweight confetti for the "order placed" moment. */
@Composable
private fun Confetti(modifier: Modifier) {
    val t = remember { Animatable(0f) }
    val pieces = remember { List(40) { Triple(Random.nextFloat() * 2 - 1, Random.nextFloat() * 1.4f - 0.2f, Random.nextFloat() * 360f) } }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1800, easing = LinearOutSlowInEasing)) }
    val colors = listOf(KonoColor.Yellow, KonoColor.Brown, KonoColor.Caramel, KonoColor.Cream)
    Canvas(modifier) {
        pieces.forEachIndexed { i, (dx, dy, rot) ->
            val x = size.width / 2 + dx * 180.dp.toPx() * t.value
            val y = size.height / 3 + dy * 300.dp.toPx() * t.value
            rotate(rot + t.value * 180f, Offset(x, y)) {
                drawRoundRect(colors[i % colors.size].copy(alpha = 1f - t.value), Offset(x - 4.dp.toPx(), y - 6.dp.toPx()), Size(8.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
    }
}
