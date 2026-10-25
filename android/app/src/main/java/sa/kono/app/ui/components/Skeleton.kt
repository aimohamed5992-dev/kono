package sa.kono.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sa.kono.app.ui.theme.KonoColor

/** Pulsing placeholder block shown while a section loads. */
@Composable
fun SkeletonBlock(height: Dp, width: Dp, modifier: Modifier = Modifier, radius: Dp = 10.dp) {
    val a = rememberInfiniteTransition(label = "skel").animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a",
    )
    Box(
        modifier.width(width).height(height).alpha(a.value)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .background(KonoColor.SurfaceSunken)
    )
}
