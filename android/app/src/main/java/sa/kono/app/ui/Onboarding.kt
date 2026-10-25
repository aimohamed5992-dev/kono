package sa.kono.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import sa.kono.app.R
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.components.KonoMark
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoMotion
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

private class OnbPage(val image: Int, val title: String, val body: String)

private val pages = listOf(
    OnbPage(R.drawable.onb_choice, "كل شي تحتاجه في مكان واحد",
        "مطاعم، مقاضي من بندة ولولو والتميمي، وصيدليات النهدي والدواء — بطلب واحد."),
    OnbPage(R.drawable.onb_delivery, "توصيل سريع توثق فيه",
        "مندوبينا حواليك على طول، وطلبك يوصلك وهو سخن خلال دقائق."),
    OnbPage(R.drawable.onb_track, "تابع طلبك خطوة بخطوة",
        "من لحظة التأكيد لين يدق بابك — تعرف وين وصل مندوبك بالضبط."),
    OnbPage(R.drawable.onb_food, "ألذ الأطباق من مطاعمك المفضلة",
        "عروض وخصومات أسبوعية، وتوصيل مجاني على أول طلباتك."),
)

/** Full-bleed cinematic onboarding: the illustration fills the screen, copy sits over a bottom scrim. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val last = index == pages.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(KonoColor.Brown)
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { _, dx -> total += dx },
                    onDragEnd = {
                        // Arabic UI: next page lives to the left, so a left swipe advances.
                        if (total < -80 && index < pages.lastIndex) index++
                        else if (total > 80 && index > 0) index--
                    },
                )
            },
    ) {
        Crossfade(index, animationSpec = tween(450), label = "onbImage") { i ->
            Image(painterResource(pages[i].image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // legibility scrim — stays out of the art until the lower third
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to Color.Transparent,
                    0.62f to Color(0x1F241203),
                    0.82f to Color(0xB8241203),
                    1f to Color(0xF2180C01),
                )
            )
        )

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KonoMark(Modifier.konoShadow(KonoShadow.Md, androidx.compose.foundation.shape.RoundedCornerShape(11.dp)), size = 40.dp)
            Box(Modifier.weight(1f))
            if (!last) {
                Text(
                    "تخطّي", style = KonoFont.Subheadline, color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onFinish() }
                        .padding(horizontal = KonoSpace.m, vertical = KonoSpace.xs + 2.dp),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KonoSpace.screen)
                .padding(bottom = KonoSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i ->
                    val w by animateDpAsState(if (i == index) 26.dp else 8.dp, KonoMotion.snappy(), label = "dot")
                    Box(
                        Modifier.height(8.dp).width(w)
                            .clip(CircleShape)
                            .background(if (i == index) KonoColor.Yellow else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
            AnimatedContent(
                index, label = "onbCopy",
                transitionSpec = {
                    (slideInHorizontally(KonoMotion.spring()) { -it / 4 } + fadeIn(tween(300))) togetherWith fadeOut(tween(150))
                },
            ) { i ->
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Text(pages[i].title, style = KonoFont.display(28), color = Color.White)
                    Text(pages[i].body, style = KonoFont.Body.copy(lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        color = Color.White.copy(alpha = 0.82f))
                }
            }
            KonoButton(
                title = if (last) "يلا نبدأ" else "التالي",
                icon = if (last) Icons.AutoMirrored.Filled.ArrowForward else null,
                kind = KonoButtonKind.Brand,
                onClick = { if (!last) index++ else onFinish() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("عندك حساب؟", style = KonoFont.Footnote, color = Color.White.copy(alpha = 0.7f))
                Text("سجّل دخولك", style = KonoFont.Subheadline, color = KonoColor.Yellow,
                    modifier = Modifier.clickable { onFinish() })
            }
        }
    }
}
