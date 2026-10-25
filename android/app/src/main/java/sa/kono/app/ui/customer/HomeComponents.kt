package sa.kono.app.ui.customer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sa.kono.app.data.Address
import sa.kono.app.data.PromotionDto
import sa.kono.app.data.VerticalKind
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoPillButton
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.res.painterResource

fun VerticalKind.icon(): ImageVector = when (this) {
    VerticalKind.Restaurants -> Icons.Filled.Restaurant
    VerticalKind.Groceries -> Icons.Filled.ShoppingBasket
    VerticalKind.Pharmacy -> Icons.Filled.LocalPharmacy
    VerticalKind.Sweets -> Icons.Filled.Cake
    VerticalKind.Flowers -> Icons.Filled.LocalFlorist
    VerticalKind.Water -> Icons.Filled.WaterDrop
}

/** Saved-address icon names come from the backend (SF Symbol names). */
fun addressIcon(name: String): ImageVector = when {
    name.startsWith("house") -> Icons.Filled.Home
    name.startsWith("briefcase") -> Icons.Filled.Work
    else -> Icons.Filled.LocationOn
}

/** Category tile in the vertical strip — a photo bubble with a caption. */
@Composable
fun VerticalTile(kind: VerticalKind, modifier: Modifier = Modifier, selected: Boolean = false, onTap: () -> Unit) {
    Column(
        modifier.konoClickable(pressedScale = 0.94f, onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KonoSpace.s),
    ) {
        AssetImage(
            kind.asset,
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(KonoRadius.l))
                .border(if (selected) 2.5.dp else 1.dp, if (selected) kind.accent else KonoColor.Hairline, RoundedCornerShape(KonoRadius.l)),
            fallbackIcon = kind.icon(), fallbackTint = kind.accent,
        )
        Text(
            kind.titleAr, style = cairo(12, FontWeight.SemiBold),
            color = if (selected) KonoColor.TextPrimary else KonoColor.TextSecondary,
        )
    }
}

/** A titled section with a horizontal scroller. Every home section uses this. */
@Composable
fun HomeRail(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    spacing: androidx.compose.ui.unit.Dp = KonoSpace.m,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = KonoSpace.screen), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = KonoFont.Title2, color = KonoColor.TextPrimary)
                if (subtitle != null) Text(subtitle, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
            if (onSeeAll != null) KonoPillButton("الكل", onSeeAll, icon = Icons.Filled.ChevronLeft)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) { content() }
    }
}

/**
 * Auto-advancing promo carousel. Slides are stacked and cross-faded by opacity only —
 * no pager or layout-moving transition, so advancing can never tug the vertical home scroll.
 */
@Composable
fun PromoCarousel(slides: List<PromotionDto>, modifier: Modifier = Modifier, onTap: (PromotionDto) -> Unit = {}) {
    if (slides.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val safe = index.coerceIn(0, slides.lastIndex)
    fun advance(by: Int) { if (slides.size > 1) index = (safe + by + slides.size) % slides.size }

    LaunchedEffect(slides.size, safe) { delay(4500); advance(1) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Box(
            Modifier
                .padding(horizontal = KonoSpace.screen)
                .fillMaxWidth()
                .height(188.dp)
                .clip(KonoRadius.L)
                .pointerInput(slides.size) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { _, dx -> total += dx },
                        onDragEnd = { if (kotlin.math.abs(total) > 80) advance(if (total < 0) 1 else -1) },
                    )
                },
        ) {
            slides.forEachIndexed { i, slide ->
                val a by androidx.compose.animation.core.animateFloatAsState(if (i == safe) 1f else 0f, tween(350), label = "slide")
                if (a > 0.01f || i == safe) Box(Modifier.fillMaxSize().alpha(a)) { PromoSlideView(slide, onTap) }
            }
        }
        if (slides.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                slides.indices.forEach { i ->
                    val w by animateDpAsState(if (i == safe) 18.dp else 6.dp, KonoMotion.snappy(), label = "pd")
                    Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (i == safe) KonoColor.Brown else KonoColor.Brown.copy(alpha = 0.2f)))
                }
            }
        }
    }
}

@Composable
private fun PromoSlideView(slide: PromotionDto, onTap: (PromotionDto) -> Unit) {
    val bg = remember(slide.backgroundHex) {
        runCatching { Color(android.graphics.Color.parseColor("#" + slide.backgroundHex.trimStart('#'))) }.getOrDefault(Color(0xFFFFE79A))
    }
    Box(Modifier.fillMaxSize().background(bg).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap(slide) }) {
        AssetImage(slide.imageUrl, Modifier.fillMaxSize(), fallbackIcon = Icons.Filled.CardGiftcard, fallbackTint = KonoColor.Brown)
        // heavy legibility panel on the copy side (start == right in RTL)
        Box(
            Modifier.fillMaxSize().background(
                // absolute left→right: clear on the art side, dark on the right (copy side in RTL)
                Brush.horizontalGradient(
                    0.1f to Color.Transparent, 0.38f to Color(0x592A1403), 0.7f to Color(0xCC2A1403), 1f to Color(0xEB2A1403),
                )
            )
        )
        Column(
            Modifier.align(Alignment.CenterStart).widthIn(max = 235.dp).fillMaxHeight().padding(KonoSpace.l),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(slide.titleAr, style = cairo(20, FontWeight.ExtraBold), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(slide.subtitleAr, style = KonoFont.Footnote, color = Color.White.copy(alpha = 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(Modifier.weight(1f))
            Row(
                Modifier
                    .konoShadow(KonoShadow.Md, CircleShape)
                    .clip(CircleShape)
                    .background(KonoColor.Yellow)
                    .konoClickable { onTap(slide) }
                    .padding(horizontal = KonoSpace.m, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(slide.ctaAr, style = cairo(13, FontWeight.Bold), color = KonoColor.Espresso)
                Icon(Icons.Filled.ArrowForward, null, Modifier.size(12.dp), tint = KonoColor.Espresso)
            }
        }
    }
}

/** Top bar: address selector + notifications. */
@Composable
fun HomeTopBar(address: Address?, onAddress: () -> Unit, onNotifications: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
        Row(
            Modifier.weight(1f).konoClickable(pressedScale = 0.98f, onClick = onAddress),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.xs + 2.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(addressIcon(address?.icon ?: ""), null, Modifier.size(18.dp), tint = KonoColor.Brown)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("التوصيل إلى ${address?.labelAr ?: "موقعك"}", style = cairo(12, FontWeight.Bold), color = KonoColor.TextSecondary)
                    Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(14.dp), tint = KonoColor.TextSecondary)
                }
                Text(address?.lineAr ?: "حدّد عنوان التوصيل", style = cairo(13, FontWeight.ExtraBold), color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, CircleShape)
                .konoClickable(pressedScale = 0.9f, onClick = onNotifications),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Notifications, "الإشعارات", Modifier.size(20.dp), tint = KonoColor.Brown)
            Box(Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 9.dp).size(8.dp).background(KonoColor.Danger, CircleShape))
        }
    }
}

/** Round back button used by pushed screens. */
@Composable
fun KonoBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).konoShadow(KonoShadow.Sm, CircleShape).clip(CircleShape).background(KonoColor.Surface)
            .konoClickable(pressedScale = 0.9f, onClick = onBack),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", Modifier.size(20.dp), tint = KonoColor.Brown) }
}
