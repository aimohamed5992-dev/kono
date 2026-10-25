package sa.kono.app.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import sa.kono.app.data.AppData
import sa.kono.app.data.ChatConversation
import sa.kono.app.data.ChatMessage
import sa.kono.app.data.ChatRole
import sa.kono.app.data.ChatTarget
import sa.kono.app.data.ChatThread
import sa.kono.app.data.SendMessageBody
import sa.kono.app.data.chatConversation
import sa.kono.app.data.chatMessages
import sa.kono.app.data.chatTargets
import sa.kono.app.data.chatThreads
import sa.kono.app.data.endChatCall
import sa.kono.app.data.markChatRead
import sa.kono.app.data.openChat
import sa.kono.app.data.sendChat
import sa.kono.app.data.startChatCall
import sa.kono.app.data.uploadChatMedia
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoEmptyState
import sa.kono.app.ui.customer.KonoBackButton
import sa.kono.app.ui.customer.ScreenTopBar
import sa.kono.app.ui.customer.parseApiDate
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun roleIcon(role: String): ImageVector = when (role) { "merchant" -> Icons.Filled.Storefront; "courier" -> Icons.Filled.DeliveryDining; else -> Icons.Filled.Person }
private fun mmss(sec: Int) = "%d:%02d".format(Locale.US, sec / 60, sec % 60)
private fun timeOf(iso: String) = parseApiDate(iso)?.let { SimpleDateFormat("h:mm a", Locale("ar")).format(it) } ?: ""

// ── Order chat buttons (drop-in row) ────────────────────────────────────────

/** A row of "message X" buttons for an order; resolves who the current user can talk to. */
@Composable
fun OrderChatButtons(orderId: Int, onOpen: (conversationId: Int) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    var targets by remember { mutableStateOf<List<ChatTarget>>(emptyList()) }
    var opening by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(orderId) { targets = AppData.chatTargets(orderId)?.targets.orEmpty() }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        targets.forEach { t ->
            val enabled = t.available && opening == null
            Row(
                Modifier.weight(1f).clip(CircleShape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, CircleShape)
                    .konoClickable(enabled = enabled, pressedScale = 0.97f) {
                        opening = t.target
                        scope.launch {
                            AppData.openChat(orderId, t.target)?.let { onOpen(it.conversationId) }
                            opening = null
                        }
                    }.padding(horizontal = KonoSpace.m, vertical = KonoSpace.s),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ChatBubble, null, Modifier.size(15.dp), tint = if (t.available) KonoColor.Brown else KonoColor.TextTertiary)
                Text(if (compact) ChatRole.ar(t.otherPartyRole) else "محادثة ${ChatRole.ar(t.otherPartyRole)}", style = cairo(13, FontWeight.SemiBold), color = if (t.available) KonoColor.Brown else KonoColor.TextTertiary)
                if (t.unreadCount > 0) Text("${t.unreadCount}", style = cairo(10, FontWeight.Black), color = Color.White, modifier = Modifier.background(KonoColor.Danger, CircleShape).padding(horizontal = 5.dp, vertical = 1.dp))
            }
        }
    }
}

// ── Conversations list ──────────────────────────────────────────────────────

@Composable
fun ChatListScreen(onBack: () -> Unit, onOpen: (Int) -> Unit) {
    var threads by remember { mutableStateOf<List<ChatThread>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { threads = AppData.chatThreads(); loading = false; delay(5000) } }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar("المحادثات", onBack)
        if (!loading && threads.isEmpty()) {
            KonoEmptyState("لا توجد محادثات بعد", icon = Icons.Filled.ChatBubble, message = "تظهر هنا محادثاتك مع المتاجر والمناديب عند تقديم طلب.", modifier = Modifier.padding(top = 60.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(threads, key = { it.conversationId }) { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(t.conversationId) }.padding(horizontal = KonoSpace.l, vertical = KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssetImage(t.otherPartyPhoto, Modifier.size(46.dp).clip(CircleShape), roleIcon(t.otherPartyRole), KonoColor.Brown)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.otherPartyName, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(timeOf(t.lastMessageAt), style = cairo(10), color = KonoColor.TextTertiary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.lastMessagePreview.ifEmpty { "طلب ${t.orderShortCode}" }, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (t.unreadCount > 0) Text("${t.unreadCount}", style = cairo(11, FontWeight.Black), color = KonoColor.TextOnBrown, modifier = Modifier.background(KonoColor.Brown, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                        Text("${ChatRole.ar(t.otherPartyRole)} · ${t.orderShortCode}", style = cairo(10), color = KonoColor.TextTertiary)
                    }
                }
                androidx.compose.material3.Divider(Modifier.padding(start = 72.dp), color = KonoColor.Divider)
            }
        }
    }
}

// ── Conversation ────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
private fun currentLocation(ctx: Context): Location? {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
}

private fun encodeImage(ctx: Context, uri: Uri): String? = runCatching {
    val src = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) } ?: return null
    val max = 1280f; val k = minOf(1f, max / maxOf(src.width, src.height))
    val bmp = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true) else src
    val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 70, out)
    "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

@Composable
fun ChatScreen(conversationId: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var conv by remember { mutableStateOf<ChatConversation?>(null) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fullImage by remember { mutableStateOf<String?>(null) }
    var attachMenu by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx) }
    var cancelling by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    // initial load + short-polling (2.5 s), the same pattern as order tracking
    LaunchedEffect(conversationId) {
        AppData.chatConversation(conversationId)?.let { conv = it; messages.clear(); messages.addAll(it.messages) }
        AppData.markChatRead(conversationId)
        while (true) {
            delay(2500)
            val last = messages.maxOfOrNull { it.id } ?: 0
            val fresh = AppData.chatMessages(conversationId, last)
            if (fresh.isNotEmpty()) { messages.addAll(fresh.filter { f -> messages.none { it.id == f.id } }); AppData.markChatRead(conversationId) }
            else AppData.chatConversation(conversationId)?.let { full -> if (full.messages.size == messages.size) { messages.clear(); messages.addAll(full.messages) } }
        }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
    DisposableEffectStop(recorder)

    suspend fun push(body: SendMessageBody) {
        sending = true
        val m = AppData.sendChat(conversationId, body)
        if (m != null) { if (messages.none { it.id == m.id }) messages.add(m) } else error = "تعذّر إرسال الرسالة"
        sending = false
    }
    fun sendText() { val t = draft.trim(); if (t.isEmpty()) return; draft = ""; scope.launch { push(SendMessageBody("text", t)) } }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            sending = true
            val data = withContext(Dispatchers.Default) { encodeImage(ctx, uri) }
            val url = data?.let { AppData.uploadChatMedia(it, "image") }
            if (url != null) push(SendMessageBody("image", mediaUrl = url)) else { error = "تعذّر إرسال الصورة"; sending = false }
        }
    }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scope.launch {
            val l = currentLocation(ctx)
            val a = AppData.currentAddress
            val lat = l?.latitude ?: a?.latitude; val lng = l?.longitude ?: a?.longitude
            if (lat != null && lng != null) push(SendMessageBody("location", lat = lat, lng = lng)) else error = "تعذّر تحديد الموقع"
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding().imePadding().navigationBarsPadding()) {
        // header
        Row(Modifier.fillMaxWidth().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            KonoBackButton(onBack)
            conv?.let { c ->
                AssetImage(c.otherPartyPhoto, Modifier.size(34.dp).clip(CircleShape), roleIcon(c.otherPartyRole), KonoColor.Brown)
                Column(Modifier.weight(1f)) {
                    Text(c.otherPartyName, style = cairo(14, FontWeight.Bold), color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ChatRole.ar(c.otherPartyRole)} · طلب ${c.orderShortCode}", style = cairo(10), color = KonoColor.TextSecondary, maxLines = 1)
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(KonoColor.Brown).konoClickable(pressedScale = 0.9f) {
                        scope.launch {
                            val call = AppData.startChatCall(conversationId)
                            val phone = (call?.otherPartyPhone ?: c.otherPartyPhone).filter { it.isDigit() || it == '+' }
                            if (phone.isNotEmpty()) ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            call?.let { AppData.endChatCall(it.callId, 0) }
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Call, "اتصال", Modifier.size(18.dp), tint = KonoColor.TextOnBrown) }
            } ?: Box(Modifier.weight(1f))
        }
        androidx.compose.material3.Divider(color = KonoColor.Divider)

        // messages
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = KonoSpace.m), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(messages.size, key = { messages[it].id }) { i ->
                    val m = messages[i]
                    if (i == 0 || !sameDay(m.createdAt, messages[i - 1].createdAt)) DateChip(m.createdAt)
                    ChatBubble(m, onImage = { fullImage = it })
                }
            }
            if (messages.isEmpty() && conv != null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Icon(Icons.Filled.ChatBubble, null, Modifier.size(36.dp), tint = KonoColor.TextTertiary)
                    Text("ابدأ المحادثة بخصوص طلب ${conv?.orderShortCode}", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                }
            }
        }

        // composer
        Column(Modifier.background(KonoColor.SurfaceRaised)) {
            if (recorder.isRecording) {
                Row(Modifier.fillMaxWidth().background(KonoColor.Cream).padding(horizontal = KonoSpace.l, vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(KonoColor.Danger, CircleShape))
                    Text(mmss(recorder.elapsed.toInt()), style = KonoFont.Subheadline, color = KonoColor.TextPrimary)
                    Box(Modifier.weight(1f))
                    Text(if (cancelling) "أفلت للإلغاء" else "اسحب لأعلى للإلغاء", style = KonoFont.Footnote, color = if (cancelling) KonoColor.Danger else KonoColor.TextSecondary)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = KonoSpace.m, vertical = KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(KonoColor.SurfaceSunken).konoClickable(pressedScale = 0.9f) { attachMenu = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, "إرفاق", Modifier.size(20.dp), tint = KonoColor.Brown)
                    }
                    DropdownMenu(attachMenu, { attachMenu = false }, containerColor = KonoColor.Surface) {
                        DropdownMenuItem(text = { Text("صورة", style = KonoFont.Body) }, leadingIcon = { Icon(Icons.Filled.PhotoLibrary, null, tint = KonoColor.Brown) },
                            onClick = { attachMenu = false; photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                        DropdownMenuItem(text = { Text("الموقع الحالي", style = KonoFont.Body) }, leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = KonoColor.Brown) },
                            onClick = {
                                attachMenu = false
                                val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) scope.launch {
                                    val l = currentLocation(ctx); val a = AppData.currentAddress
                                    val lat = l?.latitude ?: a?.latitude; val lng = l?.longitude ?: a?.longitude
                                    if (lat != null && lng != null) push(SendMessageBody("location", lat = lat, lng = lng))
                                } else locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            })
                    }
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, RoundedCornerShape(20.dp)).padding(horizontal = KonoSpace.m, vertical = 9.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (draft.isEmpty()) Text("اكتب رسالة…", style = KonoFont.Body, color = KonoColor.TextTertiary)
                    BasicTextField(draft, { draft = it }, Modifier.fillMaxWidth(), maxLines = 4, textStyle = KonoFont.Body.copy(color = KonoColor.TextPrimary), cursorBrush = SolidColor(KonoColor.Brown))
                }
                if (draft.isBlank()) {
                    // hold to record, drag up to cancel
                    Box(
                        Modifier.size(38.dp).scale(if (recorder.isRecording) 1.15f else 1f).clip(CircleShape).background(if (recorder.isRecording) KonoColor.Danger else KonoColor.Brown)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) { micPermission.launch(Manifest.permission.RECORD_AUDIO); return@awaitEachGesture }
                                    cancelling = false
                                    if (!recorder.start(scope)) { error = "تعذّر بدء التسجيل"; return@awaitEachGesture }
                                    do {
                                        val ev = awaitPointerEvent()
                                        val dy = ev.changes.firstOrNull()?.position?.y?.minus(down.position.y) ?: 0f
                                        cancelling = dy < -60.dp.toPx()
                                        ev.changes.forEach { it.consume() }
                                    } while (ev.changes.any { it.pressed })
                                    if (cancelling) recorder.cancel()
                                    else recorder.stop()?.let { (file, secs) ->
                                        scope.launch {
                                            sending = true
                                            val b64 = withContext(Dispatchers.IO) { "data:audio/m4a;base64," + Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }
                                            val url = AppData.uploadChatMedia(b64, "voice")
                                            if (url != null) push(SendMessageBody("voice", mediaUrl = url, durationSec = maxOf(1, secs))) else { error = "تعذّر إرسال الرسالة الصوتية"; sending = false }
                                            file.delete()
                                        }
                                    }
                                    cancelling = false
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (recorder.isRecording) Icons.Filled.GraphicEq else Icons.Filled.Mic, "تسجيل", Modifier.size(20.dp), tint = KonoColor.TextOnBrown) }
                } else {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(KonoColor.Brown).konoClickable(pressedScale = 0.9f) { sendText() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowUpward, "إرسال", Modifier.size(18.dp), tint = KonoColor.TextOnBrown)
                    }
                }
            }
        }
    }

    if (error != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { androidx.compose.material3.TextButton({ error = null }) { Text("حسناً", style = KonoFont.Button, color = KonoColor.Brown) } },
            text = { Text(error ?: "", style = KonoFont.Body) }, containerColor = KonoColor.Surface,
        )
    }
    fullImage?.let { url ->
        Dialog({ fullImage = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AssetImage(url, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Icon(Icons.Filled.Close, "إغلاق", Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).size(40.dp).clip(CircleShape).background(Color(0x66000000)).konoClickable { fullImage = null }.padding(10.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun DisposableEffectStop(recorder: VoiceRecorder) {
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { recorder.cancel(); VoicePlayer.stop() } }
}

private fun sameDay(a: String, b: String): Boolean {
    val da = parseApiDate(a) ?: return true; val db = parseApiDate(b) ?: return true
    val ca = Calendar.getInstance().apply { time = da }; val cb = Calendar.getInstance().apply { time = db }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun DateChip(iso: String) {
    val d = parseApiDate(iso) ?: Date()
    val today = Calendar.getInstance(); val c = Calendar.getInstance().apply { time = d }
    val label = when {
        c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> "اليوم"
        else -> SimpleDateFormat("d MMM yyyy", Locale("ar")).format(d)
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, style = KonoFont.Caption, color = KonoColor.TextSecondary, modifier = Modifier.clip(CircleShape).background(KonoColor.SurfaceSunken).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

// ── bubbles ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(m: ChatMessage, onImage: (String) -> Unit) {
    val mine = m.mine
    val bg = if (mine) KonoColor.Yellow else KonoColor.Surface
    val fg = if (mine) KonoColor.TextOnYellow else KonoColor.TextPrimary
    // mine sits on the reading-start edge (right in Arabic); the tail corner is on the bottom-end/start respectively
    val shape = RoundedCornerShape(16.dp, 16.dp, if (mine) 5.dp else 16.dp, if (mine) 16.dp else 5.dp)
    val pad = if (m.kind == "text") PaddingValues2(12.dp, 9.dp) else PaddingValues2(5.dp, 5.dp)
    Row(Modifier.fillMaxWidth().padding(start = if (mine) 4.dp else 44.dp, end = if (mine) 44.dp else 4.dp), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
        Column(
            Modifier.widthIn(max = 290.dp).clip(shape).background(bg).then(if (mine) Modifier else Modifier.border(1.dp, KonoColor.Hairline, shape)).padding(horizontal = pad.h, vertical = pad.v),
            horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (m.kind) {
                "image" -> {
                    AssetImage(m.mediaUrl, Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)).clickable { onImage(m.mediaUrl) })
                    Meta(m, fg, Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                "voice" -> VoiceBubble(m, mine)
                "location" -> {
                    ChatMiniMap(m.lat, m.lng, Modifier.size(width = 220.dp, height = 140.dp).clip(RoundedCornerShape(14.dp)))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Start)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(12.dp), tint = fg.copy(alpha = 0.8f))
                        Text("موقع مُشارَك", style = KonoFont.Caption, color = fg.copy(alpha = 0.8f))
                    }
                    Meta(m, fg, Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                "system" -> Text(m.text, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                else -> {
                    Text(m.text, style = KonoFont.Body, color = fg, modifier = Modifier.align(Alignment.Start))
                    Meta(m, fg)
                }
            }
        }
    }
}

private class PaddingValues2(val h: androidx.compose.ui.unit.Dp, val v: androidx.compose.ui.unit.Dp)

@Composable
private fun Meta(m: ChatMessage, fg: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(timeOf(m.createdAt), style = cairo(10), color = fg.copy(alpha = 0.55f))
        if (m.mine) Icon(if (m.read) Icons.Filled.DoneAll else Icons.Filled.Check, null, Modifier.size(12.dp), tint = if (m.read) KonoColor.Info else fg.copy(alpha = 0.5f))
    }
}

@Composable
private fun VoiceBubble(m: ChatMessage, mine: Boolean) {
    val scope = rememberCoroutineScope()
    val playing = VoicePlayer.playingUrl == m.mediaUrl
    val tint = if (mine) KonoColor.TextOnYellow else KonoColor.Brown
    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(tint).konoClickable(pressedScale = 0.9f) { VoicePlayer.toggle(m.mediaUrl, scope) }, contentAlignment = Alignment.Center) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(18.dp), tint = if (mine) KonoColor.Yellow else KonoColor.TextOnBrown)
        }
        Column(Modifier.width(120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(tint.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(if (playing) VoicePlayer.progress.coerceIn(0.02f, 1f) else 0.02f).height(3.dp).background(tint, CircleShape))
            }
            Text(mmss(m.durationSec), style = cairo(10), color = tint.copy(alpha = 0.8f))
        }
        if (mine) Icon(if (m.read) Icons.Filled.DoneAll else Icons.Filled.Check, null, Modifier.size(12.dp), tint = if (m.read) KonoColor.Info else tint.copy(alpha = 0.5f))
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun ChatMiniMap(lat: Double, lng: Double, modifier: Modifier) {
    val ctx = LocalContext.current
    Box(modifier.clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) }) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                Configuration.getInstance().userAgentValue = c.packageName
                MapView(c).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0); controller.setCenter(GeoPoint(lat, lng))
                    overlays.add(Marker(this).apply { position = GeoPoint(lat, lng); setInfoWindow(null); icon = sa.kono.app.ui.customer.mapPin(c, KonoColor.Brown.toArgb(), sa.kono.app.R.drawable.ic_pin_home, 34); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) })
                    setOnTouchListener { _, _ -> true }
                }
            },
            onRelease = { it.onDetach() },
        )
        // swallow touches so the whole tile acts as a link
        Box(Modifier.fillMaxSize().clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) })
    }
}
