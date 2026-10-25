package sa.kono.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSize
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

@Composable
fun KonoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    secure: Boolean = false,
    singleLine: Boolean = true,
    height: androidx.compose.ui.unit.Dp = KonoSize.fieldHeight,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    var reveal by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) KonoColor.Brown else KonoColor.Brown.copy(alpha = 0.14f), label = "fieldBorder",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(KonoRadius.M)
            .background(KonoColor.Surface)
            .border(BorderStroke(if (focused) 1.5.dp else 1.dp, borderColor), KonoRadius.M)
            .padding(horizontal = KonoSpace.l),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (focused) KonoColor.Brown else KonoColor.TextTertiary)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = KonoFont.Body, color = KonoColor.TextTertiary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = KonoFont.Body.copy(color = KonoColor.TextPrimary),
                cursorBrush = SolidColor(KonoColor.Brown),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (secure && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                interactionSource = source,
            )
        }
        if (secure) {
            Icon(
                if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null,
                Modifier.size(18.dp).konoClickable(pressedScale = 0.85f) { reveal = !reveal },
                tint = KonoColor.TextTertiary,
            )
        } else if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel, null,
                Modifier.size(18.dp).konoClickable(pressedScale = 0.85f) { onValueChange("") },
                tint = KonoColor.TextTertiary,
            )
        }
    }
}

/** Pill search field. With [onTap] it acts as a button (placeholder only). */
@Composable
fun KonoSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "ابحث عن مطعم أو صنف",
    onTap: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .konoShadow(KonoShadow.Sm, CircleShape)
            .clip(CircleShape)
            .background(KonoColor.Surface)
            .border(BorderStroke(1.dp, KonoColor.Brown.copy(alpha = 0.1f)), CircleShape)
            .then(if (onTap != null) Modifier.konoClickable(pressedScale = 0.98f, onClick = onTap) else Modifier)
            .padding(horizontal = KonoSpace.l),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(18.dp), tint = KonoColor.Brown.copy(alpha = 0.7f))
        if (onTap != null) {
            Text(placeholder, style = KonoFont.Body, color = KonoColor.TextTertiary)
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = KonoFont.Body, color = KonoColor.TextTertiary)
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = KonoFont.Body.copy(color = KonoColor.TextPrimary),
                    cursorBrush = SolidColor(KonoColor.Brown),
                )
            }
        }
    }
}
