package sa.kono.app.ui.courier

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import sa.kono.app.R
import sa.kono.app.data.CourierJob
import sa.kono.app.data.CourierSession
import sa.kono.app.data.JobStage
import sa.kono.app.data.clean
import sa.kono.app.ui.chat.OrderChatButtons
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.components.KonoMark
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.customer.mapPin
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import java.util.Locale

private fun hereDot(ctx: android.content.Context): BitmapDrawable {
    val d = ctx.resources.displayMetrics.density
    val px = (44 * d).toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawCircle(px / 2f, px / 2f, px / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = KonoColor.Info.copy(alpha = 0.25f).toArgb() })
    c.drawCircle(px / 2f, px / 2f, 10 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    c.drawCircle(px / 2f, px / 2f, 7.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = KonoColor.Info.toArgb() })
    return BitmapDrawable(ctx.resources, bmp)
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CourierHomeScreen(onDetail: (Int) -> Unit, onChat: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val p = CourierSession.profile
    val job = CourierSession.activeJob
    val offer = CourierSession.offer

    Box(Modifier.fillMaxSize().background(KonoColor.Background)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                Configuration.getInstance().userAgentValue = c.packageName
                MapView(c).apply {
                    setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(14.0)
                    setOnTouchListener { v, _ -> v.parent?.requestDisallowInterceptTouchEvent(true); false }
                }
            },
            update = { map ->
                map.overlays.clear()
                val here = GeoPoint(p.latitude, p.longitude)
                val j = job ?: offer
                if (j != null && j.storeLat != 0.0) {
                    val store = GeoPoint(j.storeLat, j.storeLng); val drop = GeoPoint(j.dropLat, j.dropLng)
                    map.overlays.add(Polyline().apply { setPoints(listOf(here, store, drop)); outlinePaint.color = KonoColor.Brown.toArgb(); outlinePaint.strokeWidth = 9f })
                    map.overlays.add(Marker(map).apply { position = store; icon = mapPin(map.context, KonoColor.Caramel.toArgb(), R.drawable.ic_pin_store); setAnchor(0.5f, 0.5f); setInfoWindow(null) })
                    map.overlays.add(Marker(map).apply { position = drop; icon = mapPin(map.context, KonoColor.Brown.toArgb(), R.drawable.ic_pin_home); setAnchor(0.5f, 0.5f); setInfoWindow(null) })
                    if (map.tag != j.orderId) {
                        map.tag = j.orderId
                        map.zoomToBoundingBox(org.osmdroid.util.BoundingBox.fromGeoPoints(listOf(here, store, drop)).increaseByScale(1.5f), false)
                    }
                } else if (map.tag == null) { map.tag = "home"; map.controller.setCenter(here) }
                map.overlays.add(Marker(map).apply { position = here; icon = hereDot(map.context); setAnchor(0.5f, 0.5f); setInfoWindow(null) })
                map.invalidate()
            },
            onRelease = { it.onDetach() },
        )

        // top status card
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = KonoSpace.screen).padding(top = KonoSpace.s)) {
            val shape = RoundedCornerShape(KonoRadius.l)
            Row(
                Modifier.fillMaxWidth().konoShadow(KonoShadow.Md, shape).clip(shape).background(KonoColor.Surface).padding(KonoSpace.m),
                horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
            ) {
                KonoMark(size = 38.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(if (CourierSession.isOnline) "متصل" else "غير متصل", style = KonoFont.Headline, color = if (CourierSession.isOnline) KonoColor.Success else KonoColor.TextSecondary)
                    Text(if (CourierSession.isOnline) "نبحث لك عن رحلات قريبة" else "فعّل الاتصال لاستقبال الرحلات", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(p.todayEarnings.clean(), style = cairo(16, FontWeight.ExtraBold), color = KonoColor.Brown)
                    Text("ر.س اليوم", style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
                }
            }
        }

        // bottom panel
        Box(Modifier.align(Alignment.BottomCenter)) {
            AnimatedContent(
                targetState = when { job != null -> 2; offer != null -> 1; else -> 0 }, label = "panel",
                transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
            ) { mode ->
                when (mode) {
                    2 -> job?.let { ActiveJobPanel(it, onAdvance = { scope.launch { CourierSession.advance() } }, onDetail = { onDetail(it.orderId) }, onChat = onChat) }
                    1 -> offer?.let { JobOfferCard(it, onAccept = { scope.launch { CourierSession.accept() } }, onDecline = { CourierSession.decline() }) }
                    else -> OnlineControl(onToggle = { scope.launch { if (CourierSession.isOnline) CourierSession.goOffline() else CourierSession.goOnline() } })
                }
            }
        }
    }
}

@Composable
private fun PanelShell(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.xl)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = KonoSpace.screen).padding(bottom = 100.dp).konoShadow(KonoShadow.Lg, shape).clip(shape).background(KonoColor.Surface).padding(KonoSpace.l),
        verticalArrangement = Arrangement.spacedBy(KonoSpace.m),
    ) { content() }
}

@Composable
private fun OnlineControl(onToggle: () -> Unit) {
    val p = CourierSession.profile
    PanelShell {
        Row(Modifier.fillMaxWidth().padding(vertical = KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            Summary("${p.todayTrips}", "رحلات", Icons.Filled.DeliveryDining, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(KonoColor.Divider))
            Summary(String.format(Locale.US, "%.1f", p.todayDistanceKm), "كم", Icons.Filled.Place, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(36.dp).background(KonoColor.Divider))
            Summary("${p.acceptanceRate}٪", "قبول", Icons.Filled.CheckCircle, Modifier.weight(1f))
        }
        val online = CourierSession.isOnline
        KonoButton(if (online) "إيقاف الاتصال" else "بدء الاتصال", onToggle, kind = if (online) KonoButtonKind.Destructive else KonoButtonKind.Primary)
    }
}

@Composable
private fun Summary(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = KonoColor.Caramel)
        Text(value, style = cairo(15, FontWeight.ExtraBold), color = KonoColor.TextPrimary)
        Text(label, style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
    }
}

/** An incoming trip offer with a 15-second countdown, after which it is declined automatically. */
@Composable
fun JobOfferCard(job: CourierJob, onAccept: () -> Unit, onDecline: () -> Unit) {
    var seconds by remember(job.orderId) { mutableIntStateOf(15) }
    LaunchedEffect(job.orderId) { while (seconds > 0) { delay(1000); seconds-- }; onDecline() }
    PanelShell {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(KonoColor.Divider)) {
            Box(Modifier.fillMaxWidth(seconds / 15f).height(4.dp).background(KonoColor.Caramel, CircleShape))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KonoTag("رحلة جديدة", icon = Icons.Filled.Notifications, color = KonoColor.Brown, filled = true)
            Box(Modifier.weight(1f))
            Text("$seconds ثانية", style = KonoFont.Subheadline, color = if (seconds < 6) KonoColor.Danger else KonoColor.TextSecondary)
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                RiyalText(job.payout, style = KonoFont.display(30))
                Text("العائد المتوقع", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MetaLine(Icons.Filled.Place, "${job.distanceKm.clean()} كم")
                MetaLine(Icons.Filled.AccessTime, "${job.estMinutes} دقيقة")
                MetaLine(Icons.Filled.ShoppingBag, "${job.itemCount} صنف")
            }
        }
        Column {
            RouteRow(Icons.Filled.Storefront, KonoColor.Caramel, "استلام", job.storeNameAr, job.storeAddressAr)
            Box(Modifier.padding(start = 15.dp, top = 2.dp, bottom = 2.dp).width(2.dp).height(18.dp).background(KonoColor.Divider))
            RouteRow(Icons.Filled.Home, KonoColor.Brown, "تسليم", job.customerNameAr, job.dropoffAddressAr)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            Box(
                Modifier.width(90.dp).height(54.dp).clip(KonoRadius.M).background(KonoColor.SurfaceSunken).konoClickable(onClick = onDecline),
                contentAlignment = Alignment.Center,
            ) { Text("تجاهل", style = KonoFont.Button, color = KonoColor.TextSecondary) }
            KonoButton("قبول الرحلة", onAccept, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(13.dp), tint = KonoColor.TextSecondary)
        Text(text, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
    }
}

@Composable
private fun RouteRow(icon: ImageVector, tint: Color, label: String, name: String, addr: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(32.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(16.dp), tint = Color.White) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = KonoFont.Caption, color = KonoColor.TextTertiary)
            Text(name, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(addr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The running trip: stage, navigation, customer call, chat, full detail and the next-step button. */
@Composable
fun ActiveJobPanel(job: CourierJob, onAdvance: () -> Unit, onDetail: () -> Unit, onChat: (Int) -> Unit) {
    val ctx = LocalContext.current
    val stage = job.stageEnum
    val toStore = stage.ordinal <= JobStage.AtStore.ordinal
    val lat = if (toStore) job.storeLat else job.dropLat
    val lng = if (toStore) job.storeLng else job.dropLng
    val phone = job.customerPhone.filter { it.isDigit() || it == '+' }
    PanelShell {
        Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(KonoColor.Divider))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stage.titleAr, style = KonoFont.Title2, color = KonoColor.TextPrimary)
                Text(if (toStore) "${job.storeNameAr} · ${job.storeAddressAr}" else "${job.customerNameAr} · ${job.dropoffAddressAr}", style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (lat != 0.0) Box(
                Modifier.size(44.dp).clip(CircleShape).background(KonoColor.Info).konoClickable(pressedScale = 0.9f) {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))) }
                        .onFailure { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) } }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Navigation, "ملاحة", Modifier.size(22.dp), tint = KonoColor.TextOnBrown) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            JobStage.entries.drop(1).forEach { s ->
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (s.ordinal <= stage.ordinal) KonoColor.Brown else KonoColor.Divider))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            InfoChip("${job.itemCount} صنف", Icons.Filled.ShoppingBag)
            InfoChip("${job.distanceKm.clean()} كم", Icons.Filled.Place)
            InfoChip("${job.payout.clean()} ر.س", Icons.Filled.Payments)
            Box(Modifier.weight(1f))
            if (phone.isNotEmpty()) Box(
                Modifier.size(38.dp).clip(CircleShape).background(KonoColor.Brown).konoClickable(pressedScale = 0.9f) { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Call, "اتصال", Modifier.size(18.dp), tint = KonoColor.TextOnBrown) }
        }
        OrderChatButtons(job.orderId, onChat, compact = true)
        Row(
            Modifier.fillMaxWidth().clip(CircleShape).background(KonoColor.SurfaceSunken).konoClickable(pressedScale = 0.98f, onClick = onDetail).padding(vertical = KonoSpace.s),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Description, null, Modifier.size(16.dp), tint = KonoColor.Brown)
            Text("تفاصيل الطلب كاملة", style = cairo(13, FontWeight.SemiBold), color = KonoColor.Brown)
        }
        KonoButton(stage.ctaAr, onAdvance)
    }
}

@Composable
private fun InfoChip(text: String, icon: ImageVector) {
    Row(Modifier.clip(CircleShape).background(KonoColor.Cream).padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(12.dp), tint = KonoColor.Brown)
        Text(text, style = cairo(11, FontWeight.Bold), color = KonoColor.Brown)
    }
}
