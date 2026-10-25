package sa.kono.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import sa.kono.app.R

/** Brand face: **Cairo** (Arabic-first, with a Latin companion for the English locale). */
val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
    Font(R.font.cairo_black, FontWeight.Black),
)

fun cairo(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontFamily = Cairo, fontSize = size.sp, fontWeight = weight, lineHeight = lineHeight)

/** Type scale — same sizes/weights as the iOS `KonoFont`. */
object KonoFont {
    fun display(size: Int = 34) = cairo(size, FontWeight.Black)
    val LargeTitle = cairo(30, FontWeight.Bold)
    val Title = cairo(24, FontWeight.Bold)
    val Title2 = cairo(20, FontWeight.Bold)
    val Headline = cairo(17, FontWeight.SemiBold)
    val Body = cairo(15)
    val BodyMedium = cairo(15, FontWeight.Medium)
    val Callout = cairo(14)
    val Subheadline = cairo(13, FontWeight.Medium)
    val Footnote = cairo(12)
    val Caption = cairo(11, FontWeight.SemiBold)
    val PriceLarge = cairo(22, FontWeight.Black)
    val Price = cairo(15, FontWeight.Bold)
    val Button = cairo(16, FontWeight.Bold)
}

internal val KonoTypography = Typography(
    displayLarge = KonoFont.display(),
    headlineLarge = KonoFont.LargeTitle,
    headlineMedium = KonoFont.Title,
    headlineSmall = KonoFont.Title2,
    titleLarge = KonoFont.Title2,
    titleMedium = KonoFont.Headline,
    bodyLarge = KonoFont.Body,
    bodyMedium = KonoFont.Callout,
    bodySmall = KonoFont.Footnote,
    labelLarge = KonoFont.Button,
    labelMedium = KonoFont.Subheadline,
    labelSmall = KonoFont.Caption,
)
