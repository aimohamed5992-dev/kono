package sa.kono.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storefront
import coil.compose.SubcomposeAsyncImage
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.cairo

/**
 * Displays a catalog image by name or URL.
 * Order: `data:` URI (e.g. an uploaded logo) → the copy bundled in the app (instant, offline)
 * → remote URL via Coil → branded placeholder.
 */
@Composable
fun AssetImage(
    name: String,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Filled.Image,
    fallbackTint: Color = KonoColor.Caramel,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val ctx = LocalContext.current
    if (name.startsWith("data:")) {
        val bmp = remember(name) {
            runCatching {
                val bytes = Base64.decode(name.substringAfter(','), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (bmp != null) { Image(bmp, null, modifier, contentScale = contentScale); return }
    }
    val resId = remember(name) {
        // `/img/store_burger.jpg` (remote) also ships as drawable `store_burger`.
        val base = name.substringAfterLast('/').substringBeforeLast('.').lowercase()
        if (base.isEmpty()) 0 else ctx.resources.getIdentifier(base, "drawable", ctx.packageName)
    }
    when {
        resId != 0 -> Image(painterResource(resId), null, modifier, contentScale = contentScale)
        name.startsWith("http") -> SubcomposeAsyncImage(
            model = name, contentDescription = null, modifier = modifier, contentScale = contentScale,
            loading = { Box(Modifier.fillMaxSize().background(KonoColor.SurfaceSunken)) },
            error = { Placeholder(fallbackIcon, fallbackTint) },
        )
        else -> Box(modifier) { Placeholder(fallbackIcon, fallbackTint) }
    }
}

@Composable
private fun Placeholder(icon: ImageVector, tint: Color) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.14f), KonoColor.Cream))
        ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(32.dp), tint = tint.copy(alpha = 0.55f)) }
}

/** Round store/brand logo; falls back to a coloured monogram of the store name. */
@Composable
fun LogoBubble(asset: String, name: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val ctx = LocalContext.current
    val hasImage = asset.startsWith("data:") || asset.startsWith("http") ||
        ctx.resources.getIdentifier(asset.substringAfterLast('/').substringBeforeLast('.').lowercase(), "drawable", ctx.packageName) != 0
    val palette = listOf(KonoColor.Brown, KonoColor.Caramel, KonoColor.Restaurants, KonoColor.Groceries, KonoColor.Pharmacy, KonoColor.Flowers)
    Box(
        modifier.size(size).clip(CircleShape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage && asset.isNotEmpty()) {
            AssetImage(asset, Modifier.fillMaxSize(), Icons.Filled.Storefront, KonoColor.Brown)
        } else {
            Text(
                name.take(1), style = cairo((size.value * 0.44f).toInt(), FontWeight.ExtraBold),
                color = palette[Math.abs(name.hashCode()) % palette.size],
            )
        }
    }
}
