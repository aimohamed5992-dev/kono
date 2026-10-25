package sa.kono.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSize
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import java.util.Locale

/** White rounded card with a hairline border and warm shadow. */
@Composable
fun KonoCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = KonoSpace.l,
    radius: androidx.compose.ui.unit.Dp = KonoRadius.l,
    shadow: KonoShadow = KonoShadow.Sm,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .konoShadow(shadow, shape)
            .clip(shape)
            .background(KonoColor.Surface)
            .border(BorderStroke(1.dp, KonoColor.Hairline), shape)
            .then(if (onClick != null) Modifier.konoClickable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun KonoChip(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val bg by animateColorAsState(if (selected) KonoColor.Brown else KonoColor.Surface, label = "chipBg")
    val fg = if (selected) KonoColor.TextOnBrown else KonoColor.Brown
    Row(
        modifier = modifier
            .height(KonoSize.chipHeight)
            .clip(CircleShape)
            .background(bg)
            .border(BorderStroke(1.dp, if (selected) Color.Transparent else KonoColor.Brown.copy(alpha = 0.16f)), CircleShape)
            .konoClickable(onClick = onClick)
            .padding(horizontal = KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = fg)
        Text(title, style = KonoFont.Subheadline, color = fg)
    }
}

/** Small status label: tinted background, or solid when [filled]. */
@Composable
fun KonoTag(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = KonoColor.Brown,
    filled: Boolean = false,
) {
    val fg = if (filled) Color.White else color
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(10.dp), tint = fg)
        Text(text, style = cairo(11, FontWeight.Bold), color = fg)
    }
}

@Composable
fun RatingBadge(rating: Double, modifier: Modifier = Modifier, count: Int? = null, compact: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, null, Modifier.size(if (compact) 10.dp else 12.dp), tint = KonoColor.Rating)
        Text(
            String.format(Locale.US, "%.1f", rating),
            style = cairo(if (compact) 11 else 13, FontWeight.Bold), color = KonoColor.TextPrimary,
        )
        if (count != null) {
            Text("($count)", style = cairo(if (compact) 10 else 11, FontWeight.Medium), color = KonoColor.TextSecondary)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = KonoFont.Title2, color = KonoColor.TextPrimary)
            if (subtitle != null) Text(subtitle, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
        }
        if (actionTitle != null && onAction != null) {
            KonoPillButton(actionTitle, onAction, icon = Icons.Filled.ChevronLeft)
        }
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    compact: Boolean = false,
) {
    val btn = if (compact) 22.dp else 28.dp
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(KonoColor.Cream)
            .border(BorderStroke(1.dp, KonoColor.Brown.copy(alpha = 0.12f)), CircleShape)
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Filled.Remove, btn) { if (quantity > min) onChange(quantity - 1) }
        Text(
            "$quantity",
            style = cairo(if (compact) 14 else 16, FontWeight.ExtraBold), color = KonoColor.TextPrimary,
            modifier = Modifier.widthIn(min = 18.dp), textAlign = TextAlign.Center,
        )
        StepButton(Icons.Filled.Add, btn) { if (quantity < max) onChange(quantity + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .konoShadow(KonoShadow.Sm, CircleShape)
            .clip(CircleShape)
            .background(KonoColor.Surface)
            .konoClickable(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(size * 0.55f), tint = KonoColor.Brown) }
}

@Composable
fun KonoEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
    message: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(KonoSpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
    ) {
        Icon(icon, null, Modifier.size(72.dp), tint = KonoColor.Caramel.copy(alpha = 0.6f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
            Text(title, style = KonoFont.Title2, color = KonoColor.TextPrimary)
            if (message != null) {
                Text(message, style = KonoFont.Callout, color = KonoColor.TextSecondary, textAlign = TextAlign.Center)
            }
        }
        if (actionTitle != null && onAction != null) {
            KonoButton(actionTitle, onAction, fullWidth = false)
        }
    }
}

/** A price in Saudi Riyal, e.g. `24.50 ر.س`. */
@Composable
fun RiyalText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = KonoFont.Price,
    color: Color = KonoColor.TextPrimary,
    showSymbol: Boolean = true,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        Text(String.format(Locale.US, "%.2f", amount), style = style, color = color)
        if (showSymbol) Text("ر.س", style = cairo(11, FontWeight.Bold), color = color.copy(alpha = 0.7f))
    }
}

/** Like [KonoCard] but takes a [Modifier] first-class and keeps its content in a plain column. */
@Composable
fun KonoShadowCard(
    modifier: Modifier = Modifier,
    shadow: KonoShadow = KonoShadow.Sm,
    padding: androidx.compose.ui.unit.Dp = KonoSpace.l,
    content: @Composable ColumnScope.() -> Unit,
) = KonoCard(modifier, padding = padding, shadow = shadow, content = content)
