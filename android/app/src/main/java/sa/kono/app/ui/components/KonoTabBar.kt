package sa.kono.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

data class KonoTabItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badge: Int? = null,
)

/**
 * Floating, icon-only tab bar with a brown "coin" disc that glides under the
 * active tab — the same identity as the iOS `KonoTabBar`.
 */
@Composable
fun KonoTabBar(
    items: List<KonoTabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(30.dp)
    BoxWithConstraints(
        modifier
            .padding(horizontal = KonoSpace.xl)
            .fillMaxWidth()
            .konoShadow(KonoShadow.Md, barShape)
            .clip(barShape)
            .background(KonoColor.Surface)
            .border(BorderStroke(1.dp, KonoColor.Hairline), barShape)
            .padding(horizontal = KonoSpace.m, vertical = KonoSpace.s),
    ) {
        val cell = maxWidth / items.size
        // offset() and CenterStart both follow the layout direction, so no manual RTL mirroring.
        val discX by animateDpAsState(cell * selected + (cell - 44.dp) / 2, KonoMotion.spring(), label = "disc")
        Box(
            Modifier
                .offset(x = discX)
                .align(Alignment.CenterStart)
                .size(44.dp)
                .konoShadow(KonoShadow.Sm, CircleShape)
                .background(KonoColor.Brown, CircleShape)
        )
        Row {
            items.forEachIndexed { i, item ->
                val active = i == selected
                val tint by animateColorAsState(
                    if (active) KonoColor.TextOnBrown else KonoColor.Espresso.copy(alpha = 0.4f), label = "tint",
                )
                Box(
                    Modifier
                        .width(cell)
                        .height(48.dp)
                        .konoClickable(pressedScale = 0.9f) { if (!active) onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, item.title, Modifier.size(24.dp), tint = tint)
                    if (item.badge != null && item.badge > 0) {
                        Text(
                            "${item.badge}", style = cairo(9, FontWeight.Black), color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = 13.dp, y = (-12).dp)
                                .background(KonoColor.Danger, CircleShape)
                                .border(1.5.dp, KonoColor.Surface, CircleShape)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}
