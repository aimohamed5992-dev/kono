package sa.kono.app.ui.i18n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import sa.kono.app.ui.customer.SegmentedTabs
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace

/** Arabic / English switch — flips every string and the layout direction. */
@Composable
fun LanguageToggleRow(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Language, null, Modifier.size(22.dp), tint = KonoColor.Brown)
        Text("اللغة", style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
        SegmentedTabs(listOf("العربية", "English"), if (AppLanguage.isArabic) 0 else 1, { AppLanguage.set(if (it == 0) "ar" else "en") }, Modifier.size(width = 190.dp, height = 46.dp))
    }
}
