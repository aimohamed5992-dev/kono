package sa.kono.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSize
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

enum class KonoButtonKind {
    Primary,     // brown fill, cream text — main CTA on yellow/cream
    Brand,       // yellow fill, brown text — CTA on brown/photo
    Secondary,   // cream fill, brown text, hairline
    Ghost,       // transparent, brown text
    Destructive,
}

@Composable
fun KonoButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    kind: KonoButtonKind = KonoButtonKind.Primary,
    fullWidth: Boolean = true,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val (bg, fg, border) = when (kind) {
        KonoButtonKind.Primary -> Triple(KonoColor.Brown, KonoColor.TextOnBrown, Color.Transparent)
        KonoButtonKind.Brand -> Triple(KonoColor.Yellow, KonoColor.TextOnYellow, Color.Transparent)
        KonoButtonKind.Secondary -> Triple(KonoColor.Cream, KonoColor.Brown, KonoColor.Brown.copy(alpha = 0.15f))
        KonoButtonKind.Ghost -> Triple(Color.Transparent, KonoColor.Brown, KonoColor.Brown.copy(alpha = 0.2f))
        KonoButtonKind.Destructive -> Triple(KonoColor.Danger.copy(alpha = 0.12f), KonoColor.Danger, Color.Transparent)
    }
    val shadow = if (kind == KonoButtonKind.Primary || kind == KonoButtonKind.Brand) KonoShadow.Sm else KonoShadow.None
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(KonoSize.buttonHeight)
            .alpha(if (enabled) 1f else 0.5f)
            .konoClickable(enabled = enabled && !loading, onClick = onClick)
            .konoShadow(shadow, KonoRadius.M)
            .clip(KonoRadius.M)
            .background(bg)
            .border(BorderStroke(1.dp, border), KonoRadius.M)
            .padding(horizontal = if (fullWidth) 0.dp else KonoSpace.xl),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = fg)
        }
        Text(title, style = KonoFont.Button, color = fg)
    }
}

/** Small pill action (e.g. "عرض الكل"). */
@Composable
fun KonoPillButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(KonoColor.Brown.copy(alpha = 0.07f))
            .konoClickable(onClick = onClick)
            .padding(horizontal = KonoSpace.m, vertical = KonoSpace.s),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = KonoFont.Subheadline, color = KonoColor.Brown)
        if (icon != null) Icon(icon, null, Modifier.size(12.dp), tint = KonoColor.Brown)
    }
}
