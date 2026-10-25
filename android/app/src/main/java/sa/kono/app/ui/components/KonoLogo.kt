package sa.kono.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sa.kono.app.R
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoShadow

/** The "k" mark inside a rounded-square tile — the app's compact identity. */
@Composable
fun KonoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = KonoColor.Yellow,
    foreground: Color = KonoColor.Brown,
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(modifier.size(size).clip(shape).background(background), contentAlignment = Alignment.Center) {
        Text("k", style = cairo((size.value * 0.62f).toInt(), FontWeight.Black), color = foreground)
    }
}

@Composable
fun KonoWordmark(modifier: Modifier = Modifier, size: Int = 34, color: Color = KonoColor.Brown) {
    Text("kono", modifier, style = cairo(size, FontWeight.Black), color = color)
}

/** Animated splash: the mark springs in, a ring pulses out, then the wordmark rises. */
@Composable
fun KonoSplash(onFinished: () -> Unit) {
    val mark = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { mark.animateTo(1f, KonoMotion.bouncy()) }
            launch { delay(150); ring.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
            launch { delay(350); word.animateTo(1f, KonoMotion.spring()) }
        }
        delay(1300)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.splash_bg), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(KonoSpace.xl)) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(130.dp)
                        .scale(0.6f + 1.2f * ring.value)
                        .alpha(1f - ring.value)
                        .drawBehind {
                            drawCircle(KonoColor.Brown.copy(alpha = 0.25f), style = Stroke(2.dp.toPx()))
                        }
                )
                KonoMark(
                    Modifier
                        .konoShadow(KonoShadow.Lg, RoundedCornerShape(27.dp))
                        .scale(0.3f + 0.7f * mark.value)
                        .rotate(-18f * (1f - mark.value))
                        .alpha(mark.value.coerceIn(0f, 1f)),
                    size = 96.dp, background = KonoColor.Brown, foreground = KonoColor.Yellow,
                )
            }
            KonoWordmark(Modifier.alpha(word.value).offset(y = (12 * (1 - word.value)).dp))
            Text(
                "أكل · مقاضي · صيدليات · توصيل سريع",
                Modifier.alpha(word.value),
                style = KonoFont.Subheadline, color = KonoColor.Brown.copy(alpha = 0.7f),
            )
        }
    }
}
