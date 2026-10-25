package sa.kono.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Warm-tinted elevation. Kono never uses neutral grey shadows — they read cold
 * against the cream canvas — so every shadow is a low-opacity brown.
 */
enum class KonoShadow(val elevation: Dp, val color: Color) {
    None(0.dp, Color.Transparent),
    Sm(4.dp, Color(0xFF582808)),
    Md(10.dp, Color(0xFF582808)),
    Lg(18.dp, Color(0xFF582808)),
    YellowGlow(14.dp, Color(0xFFF3B90A)),
}

fun Modifier.konoShadow(level: KonoShadow, shape: Shape = RoundedCornerShape(KonoRadius.l)): Modifier =
    if (level == KonoShadow.None) this
    else shadow(level.elevation, shape, clip = false, ambientColor = level.color, spotColor = level.color)
