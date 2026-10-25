package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable

/** Bottom sheet to switch the delivery address — real addresses from [AppData]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSwitcherSheet(onDismiss: () -> Unit, onAdd: () -> Unit, onManage: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AppData.loadAccount() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = KonoColor.Background) {
        Column(Modifier.padding(horizontal = KonoSpace.screen).navigationBarsPadding().padding(bottom = KonoSpace.l), verticalArrangement = Arrangement.spacedBy(KonoSpace.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("عنوان التوصيل", style = KonoFont.Title2, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                Text("إدارة", style = KonoFont.Subheadline, color = KonoColor.Brown, modifier = Modifier.konoClickable(onClick = onManage))
            }
            if (AppData.addresses.isEmpty()) {
                Text("لا توجد عناوين محفوظة", style = KonoFont.Callout, color = KonoColor.TextSecondary, modifier = Modifier.padding(vertical = KonoSpace.l))
            }
            AppData.addresses.forEach { addr ->
                val selected = AppData.currentAddress?.id == addr.id
                val shape = RoundedCornerShape(KonoRadius.m)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape)
                        .konoClickable(pressedScale = 0.98f) {
                            scope.launch { AppData.selectAddress(addr); sheet.hide(); onDismiss() }
                        }.padding(KonoSpace.m),
                    horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(shape).background(KonoColor.Cream), contentAlignment = Alignment.Center) {
                        Icon(addressIcon(addr.icon), null, Modifier.size(20.dp), tint = KonoColor.Brown)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(addr.labelAr, style = KonoFont.Headline, color = KonoColor.TextPrimary)
                        Text(addr.lineAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(22.dp), tint = if (selected) KonoColor.Brown else KonoColor.TextTertiary)
                }
            }
            KonoButton("إضافة عنوان جديد", onAdd, icon = Icons.Filled.Add, kind = KonoButtonKind.Secondary)
        }
    }
}
