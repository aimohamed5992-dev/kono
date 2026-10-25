package sa.kono.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val Scheme = lightColorScheme(
    primary = KonoColor.Brown,
    onPrimary = KonoColor.TextOnBrown,
    secondary = KonoColor.Yellow,
    onSecondary = KonoColor.TextOnYellow,
    background = KonoColor.Background,
    onBackground = KonoColor.TextPrimary,
    surface = KonoColor.Surface,
    onSurface = KonoColor.TextPrimary,
    error = KonoColor.Danger,
    outline = KonoColor.Divider,
)

/** Kono is light-only (warm cream canvas) and Arabic-first → RTL unless [rtl] is false. */
@Composable
fun KonoTheme(rtl: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = Scheme, typography = KonoTypography, content = content)
    }
}
