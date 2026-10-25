package sa.kono.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 4-dp spacing grid. */
object KonoSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val xxxl = 40.dp
    val screen = 16.dp
}

/** Generous, soft radii — matching the wordmark's rounded terminals. */
object KonoRadius {
    val xs = 6.dp
    val s = 10.dp
    val m = 14.dp
    val l = 20.dp
    val xl = 28.dp

    val Xs = RoundedCornerShape(xs)
    val S = RoundedCornerShape(s)
    val M = RoundedCornerShape(m)
    val L = RoundedCornerShape(l)
    val Xl = RoundedCornerShape(xl)
    val Pill = RoundedCornerShape(percent = 50)
}

object KonoSize {
    val buttonHeight = 54.dp
    val fieldHeight = 52.dp
    val chipHeight = 36.dp
    val iconTile = 56.dp
    val avatar = 44.dp
    val minTap = 44.dp
}
