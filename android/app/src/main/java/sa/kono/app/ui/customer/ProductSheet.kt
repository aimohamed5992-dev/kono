package sa.kono.app.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import sa.kono.app.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import sa.kono.app.data.CartLine
import sa.kono.app.data.CartStore
import sa.kono.app.data.Product
import sa.kono.app.data.ProductOptionGroup
import sa.kono.app.data.Store
import sa.kono.app.data.clean
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.KonoTextField
import sa.kono.app.ui.components.QuantityStepper
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow

/** Product detail sheet: photo, option groups (required / multi-select), note, quantity and add-to-cart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSheet(product: Product, store: Store, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var quantity by remember { mutableIntStateOf(1) }
    var selections by remember { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var note by remember { mutableStateOf("") }

    val extras = product.optionGroups.sumOf { g ->
        val picked = selections[g.id].orEmpty()
        g.choices.filter { it.id in picked }.sumOf { it.priceDelta }
    }
    val lineTotal = (product.price + extras) * quantity
    val ready = product.optionGroups.filter { it.required }.all { selections[it.id].orEmpty().isNotEmpty() }

    fun toggle(g: ProductOptionGroup, choiceId: Int) {
        val cur = selections[g.id].orEmpty()
        val next = if (g.multiSelect) (if (choiceId in cur) cur - choiceId else cur + choiceId) else setOf(choiceId)
        selections = selections + (g.id to next)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = KonoColor.Background) {
        Column {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen),
                verticalArrangement = Arrangement.spacedBy(KonoSpace.l),
            ) {
                AssetImage(product.imageUrl, Modifier.fillMaxWidth().height(220.dp).clip(KonoRadius.L), fallbackTint = store.kindEnum.accent)
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.xs)) {
                    Text(product.nameAr, style = KonoFont.Title, color = KonoColor.TextPrimary)
                    if (product.descAr.isNotBlank()) Text(product.descAr, style = KonoFont.Callout, color = KonoColor.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                        RiyalText(product.price, style = KonoFont.PriceLarge)
                        product.calories?.let { Text("· $it سعرة حرارية", style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
                    }
                }
                product.optionGroups.forEach { g ->
                    Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(g.titleAr, style = KonoFont.Headline, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                            KonoTag(if (g.required) "مطلوب" else "اختياري", color = if (g.required) KonoColor.Danger else KonoColor.TextSecondary)
                        }
                        g.choices.forEach { c ->
                            val picked = c.id in selections[g.id].orEmpty()
                            val shape = RoundedCornerShape(KonoRadius.s)
                            Row(
                                Modifier.fillMaxWidth().clip(shape).background(if (picked) KonoColor.Cream else KonoColor.Surface)
                                    .border(1.dp, KonoColor.Hairline, shape).konoClickable(pressedScale = 0.99f) { toggle(g, c.id) }.padding(KonoSpace.m),
                                horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (g.multiSelect) (if (picked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank)
                                    else (if (picked) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked),
                                    null, Modifier.size(22.dp), tint = if (picked) KonoColor.Brown else KonoColor.TextTertiary,
                                )
                                Text(c.nameAr, style = KonoFont.Body, color = KonoColor.TextPrimary, modifier = Modifier.weight(1f))
                                if (c.priceDelta != 0.0) Text("+ ${c.priceDelta.clean()} ر.س", style = KonoFont.Footnote, color = KonoColor.TextSecondary)
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(KonoSpace.s)) {
                    Text("ملاحظات للمتجر", style = KonoFont.Headline, color = KonoColor.TextPrimary)
                    KonoTextField(note, { note = it }, "مثال: الصوص على جنب", icon = Icons.Filled.Edit)
                }
                Column(Modifier.height(KonoSpace.s)) {}
            }
            Row(
                Modifier.fillMaxWidth().konoShadow(KonoShadow.Lg, RoundedCornerShape(0.dp)).background(KonoColor.Surface)
                    .navigationBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.m),
                horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(quantity, { quantity = it })
                KonoButton("إضافة · ${lineTotal.clean()} ر.س", {
                    val names = mutableListOf<String>(); val ids = mutableListOf<Int>()
                    product.optionGroups.forEach { g ->
                        val picked = selections[g.id].orEmpty()
                        g.choices.filter { it.id in picked }.forEach { names += it.nameAr; ids += it.id }
                    }
                    CartStore.add(CartLine(product, quantity, names, ids, extras, note), store)
                    scope.launch { sheet.hide(); onDismiss() }
                }, enabled = ready && product.inStock, modifier = Modifier.weight(1f))
            }
        }
    }
}
