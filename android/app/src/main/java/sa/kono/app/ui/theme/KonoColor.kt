package sa.kono.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Kono brand palette — brand yellow `#FDD003` and deep espresso brown `#582808`.
 * Mirrors the iOS `KonoColor` so all three surfaces (customer, merchant, courier) match.
 */
object KonoColor {
    // Brand
    val Yellow = Color(0xFFFDD003)
    val YellowDeep = Color(0xFFF3B90A)
    val Cream = Color(0xFFFFF7E0)
    val Brown = Color(0xFF582808)
    val Caramel = Color(0xFFC8811E)
    val Espresso = Color(0xFF3A1B05)

    // Surfaces
    val Background = Color(0xFFFFFBF2)
    val Surface = Color.White
    val SurfaceRaised = Color(0xFFFFFDF8)
    val SurfaceSunken = Color(0xFFF6EEDD)
    val Divider = Brown.copy(alpha = 0.10f)
    val Hairline = Brown.copy(alpha = 0.06f)
    val Overlay = Color(0xFF2A1403).copy(alpha = 0.45f)

    // Text
    val TextPrimary = Color(0xFF2E1703)
    val TextSecondary = Color(0xFF8A6B4A)
    val TextTertiary = Color(0xFFB09675)
    val TextOnYellow = Brown
    val TextOnBrown = Cream

    // Status
    val Success = Color(0xFF2E9E5B)
    val Warning = Color(0xFFE0A000)
    val Danger = Color(0xFFD1493B)
    val Info = Color(0xFF2F7DBE)
    val Rating = Color(0xFFF2A200)

    // Vertical accents
    val Restaurants = Color(0xFFD1493B)
    val Groceries = Color(0xFF2E9E5B)
    val Pharmacy = Color(0xFF2F7DBE)
    val Sweets = Color(0xFFC8811E)
    val Flowers = Color(0xFFB5497E)
    val Water = Color(0xFF3AA6C8)

    val BrandGradient = Brush.linearGradient(listOf(Yellow, YellowDeep))
    val SplashGradient = Brush.verticalGradient(
        listOf(Color(0xFFFFDE3D), Yellow, Color(0xFFF2B60A))
    )
    val CreamGradient = Brush.verticalGradient(listOf(Cream, Color(0xFFFFFDF6)))
}
