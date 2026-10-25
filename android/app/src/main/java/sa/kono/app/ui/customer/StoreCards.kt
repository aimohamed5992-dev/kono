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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sa.kono.app.data.Store
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.LogoBubble
import sa.kono.app.ui.components.RatingBadge
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import sa.kono.app.data.clean
import java.util.Locale

@Composable
private fun MetaLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = KonoColor.TextSecondary) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(13.dp), tint = tint)
        Text(text, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
    }
}

/** 1. Featured — large hero. */
@Composable
fun FeaturedStoreCard(store: Store, onTap: () -> Unit) {
    Column(Modifier.width(300.dp).konoClickable(pressedScale = 0.98f, onClick = onTap)) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(KonoRadius.L)) {
            AssetImage(store.heroImageUrl, Modifier.fillMaxSize(), store.kindEnum.icon(), store.kindEnum.accent)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color(0x26000000), 1f to Color(0xD1000000))))
            Row(Modifier.align(Alignment.BottomStart).padding(KonoSpace.m), horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                LogoBubble(store.logoImageUrl, store.name, size = 36.dp)
                Column {
                    Text(store.name, style = KonoFont.Headline, color = Color.White)
                    Text(store.cuisineAr, style = KonoFont.Caption, color = Color.White.copy(alpha = 0.85f))
                }
            }
            if (store.promoTagAr != null) {
                KonoTag(store.promoTagAr, Modifier.align(Alignment.TopStart).padding(KonoSpace.s), icon = Icons.Filled.LocalOffer, color = KonoColor.Danger, filled = true)
            }
        }
        Row(Modifier.padding(top = KonoSpace.s, start = KonoSpace.xs, end = KonoSpace.xs), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            MetaLabel(Icons.Filled.Star, String.format(Locale.US, "%.1f", store.rating), KonoColor.Rating)
            MetaLabel(Icons.Filled.AccessTime, "${store.etaMinutes} دقيقة")
            MetaLabel(Icons.Filled.DeliveryDining, if (store.deliveryFee == 0.0) "توصيل مجاني" else "${store.deliveryFee.clean()} ر.س")
        }
    }
}

/** 2. Deal card — offer emphasis. */
@Composable
fun DealCard(store: Store, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Column(
        Modifier.width(230.dp).konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface)
            .border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.97f, onClick = onTap),
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp)) {
            AssetImage(store.heroImageUrl, Modifier.fillMaxSize(), store.kindEnum.icon(), store.kindEnum.accent)
            Text(
                store.promoTagAr ?: "عرض", style = cairo(12, FontWeight.Black), color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(KonoSpace.s).clip(CircleShape).background(KonoColor.Danger).padding(horizontal = KonoSpace.s, vertical = 5.dp),
            )
        }
        Column(Modifier.padding(KonoSpace.s), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(store.name, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                RatingBadge(store.rating, compact = true)
                Text("· ${store.etaMinutes} د", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
        }
    }
}

/** 3. Circle card — supermarkets / brands. */
@Composable
fun StoreCircleCard(store: Store, onTap: () -> Unit) {
    Column(Modifier.width(108.dp).konoClickable(onClick = onTap), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Box(contentAlignment = Alignment.BottomCenter) {
            AssetImage(
                store.heroImageUrl,
                Modifier.size(96.dp).konoShadow(KonoShadow.Sm, CircleShape).clip(CircleShape).border(1.dp, KonoColor.Hairline, CircleShape),
                store.kindEnum.icon(), store.kindEnum.accent,
            )
            if (store.deliveryFee == 0.0) {
                Text(
                    "توصيل مجاني", style = cairo(8, FontWeight.Bold), color = Color.White,
                    modifier = Modifier.offset(y = 8.dp).clip(CircleShape).background(KonoColor.Success).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Text(store.name, style = cairo(12, FontWeight.SemiBold), color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${store.etaMinutes} دقيقة", style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
    }
}

/** 4. Pharmacy / quick card — wide with an accent bar. */
@Composable
fun QuickStoreCard(store: Store, accent: Color = KonoColor.Info, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        Modifier.width(250.dp).konoShadow(KonoShadow.Sm, shape).clip(shape).background(KonoColor.Surface).konoClickable(pressedScale = 0.98f, onClick = onTap),
    ) {
        Box(Modifier.width(4.dp).height(80.dp).background(accent))
        Row(Modifier.padding(KonoSpace.s), horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
            AssetImage(store.heroImageUrl, Modifier.size(64.dp).clip(RoundedCornerShape(KonoRadius.s)), store.kindEnum.icon(), accent)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(store.name, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                RatingBadge(store.rating, compact = true)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(11.dp), tint = accent)
                    Text("${store.etaMinutes} دقيقة", style = cairo(10, FontWeight.Bold), color = accent)
                }
            }
        }
    }
}

/** 5. Cuisine tile — browse by category. */
@Composable
fun CuisineTile(title: String, asset: String, onTap: () -> Unit) {
    Box(Modifier.size(width = 130.dp, height = 92.dp).clip(RoundedCornerShape(KonoRadius.m)).konoClickable(pressedScale = 0.97f, onClick = onTap)) {
        AssetImage(asset, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0x80000000))))
        Text(title, style = cairo(13, FontWeight.Bold), color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(KonoSpace.s))
    }
}

/** 6. Poster card — sweets / occasions. */
@Composable
fun PosterStoreCard(store: Store, onTap: () -> Unit) {
    Box(Modifier.size(width = 170.dp, height = 230.dp).clip(KonoRadius.L).konoClickable(pressedScale = 0.98f, onClick = onTap)) {
        AssetImage(store.heroImageUrl, Modifier.fillMaxSize(), store.kindEnum.icon(), store.kindEnum.accent)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xBF000000))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(KonoSpace.m), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(store.name, style = KonoFont.Headline, color = Color.White)
            Text(store.cuisineAr, style = KonoFont.Caption, color = Color.White.copy(alpha = 0.85f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, Modifier.size(10.dp), tint = KonoColor.Yellow)
                Text(String.format(Locale.US, "%.1f", store.rating), style = cairo(11, FontWeight.Bold), color = Color.White)
                Text("· ${store.etaMinutes} د", style = cairo(10), color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

/** Compact card for "popular near you" rails. */
@Composable
fun StoreCardCompact(store: Store, onTap: () -> Unit) {
    Column(Modifier.width(200.dp).konoClickable(pressedScale = 0.97f, onClick = onTap), verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
        Box(Modifier.size(width = 200.dp, height = 116.dp).clip(RoundedCornerShape(KonoRadius.m))) {
            AssetImage(store.heroImageUrl, Modifier.fillMaxSize(), store.kindEnum.icon(), store.kindEnum.accent)
            if (store.promoTagAr != null) KonoTag(store.promoTagAr, Modifier.padding(KonoSpace.xs), color = KonoColor.Danger, filled = true)
        }
        Text(store.name, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
            RatingBadge(store.rating, compact = true)
            Text("·", color = KonoColor.TextTertiary)
            Text("${store.etaMinutes} د", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            if (store.distanceKm > 0) {
                Text("·", color = KonoColor.TextTertiary)
                Text(String.format(Locale.US, "%.1f", store.distanceKm) + " كم", style = KonoFont.Footnote, color = KonoColor.Brown)
            }
        }
    }
}

/** Row used in search results and vertical lists. */
@Composable
fun StoreRow(store: Store, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
            .konoClickable(pressedScale = 0.98f, onClick = onTap).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AssetImage(store.heroImageUrl, Modifier.size(76.dp).clip(RoundedCornerShape(KonoRadius.m)), store.kindEnum.icon(), store.kindEnum.accent)
            if (store.isClosed) {
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(KonoRadius.m)).background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                    Text("مغلق", style = KonoFont.Caption, color = Color.White)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(store.name, style = KonoFont.Headline, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(store.cuisineAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                RatingBadge(store.rating, compact = true)
                Text("·", color = KonoColor.TextTertiary)
                Text("${store.etaMinutes} دقيقة", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                if (store.distanceKm > 0) {
                    Text("·", color = KonoColor.TextTertiary)
                    Text(String.format(Locale.US, "%.1f", store.distanceKm) + " كم", style = KonoFont.Footnote, color = KonoColor.Brown)
                }
            }
            if (store.promoTagAr != null) KonoTag(store.promoTagAr, color = KonoColor.Danger)
        }
        Icon(Icons.Filled.ChevronLeft, null, Modifier.size(18.dp), tint = KonoColor.TextTertiary)
    }
}
