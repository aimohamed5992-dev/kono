package sa.kono.app.ui.customer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.AppData
import sa.kono.app.data.CartLine
import sa.kono.app.data.CartStore
import sa.kono.app.data.Product
import sa.kono.app.data.Store
import sa.kono.app.data.StoreDetail
import sa.kono.app.data.clean
import sa.kono.app.data.storeDetail
import sa.kono.app.ui.components.AssetImage
import sa.kono.app.ui.components.KonoChip
import sa.kono.app.ui.components.KonoShadowCard
import sa.kono.app.ui.components.KonoTag
import sa.kono.app.ui.components.LogoBubble
import sa.kono.app.ui.components.RiyalText
import sa.kono.app.ui.components.SkeletonBlock
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont
import sa.kono.app.ui.theme.KonoRadius
import sa.kono.app.ui.theme.KonoShadow
import sa.kono.app.ui.theme.KonoSpace
import sa.kono.app.ui.theme.cairo
import sa.kono.app.ui.theme.konoClickable
import sa.kono.app.ui.theme.konoShadow
import java.util.Locale

private val HeroHeight = 240.dp

@Composable
fun StoreDetailScreen(storeId: Int, onBack: () -> Unit, onCart: () -> Unit) {
    var detail by remember { mutableStateOf<StoreDetail?>(null) }
    var failed by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var sheetProduct by remember { mutableStateOf<Product?>(null) }
    var fav by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val collapsePx = with(androidx.compose.ui.platform.LocalDensity.current) { (HeroHeight - 90.dp).toPx() }

    LaunchedEffect(storeId) {
        detail = AppData.storeDetail(storeId)
        failed = detail == null
    }
    val d = detail
    val store = d?.toStore()
    val categories = d?.menu.orEmpty()

    val collapsed by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > collapsePx } }

    fun addDirect(p: Product, s: Store) = CartStore.add(CartLine(p, 1), s)

    Box(Modifier.fillMaxSize().background(KonoColor.Background)) {
        LazyColumn(state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 140.dp)) {
            // hero
            item(key = "hero") {
                Box(Modifier.fillMaxWidth().height(HeroHeight)) {
                    AssetImage(d?.heroImageUrl.orEmpty(), Modifier.fillMaxSize(), fallbackTint = store?.kindEnum?.accent ?: KonoColor.Caramel)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x4D000000), Color.Transparent, Color(0x26000000)))))
                }
            }
            if (d != null && store != null) {
                item(key = "info") {
                    InfoCard(d, store, Modifier.padding(horizontal = KonoSpace.screen).offset(y = (-30).dp))
                }
                item(key = "chips") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = KonoSpace.screen),
                        horizontalArrangement = Arrangement.spacedBy(KonoSpace.s),
                    ) {
                        categories.forEachIndexed { i, c ->
                            KonoChip(c.nameAr, selected = selectedCategory == i) {
                                selectedCategory = i
                                // hero, info, chips precede the first category header; each category = header + items
                                val target = 3 + categories.take(i).sumOf { it.items.size + 1 }
                                scope.launch { list.animateScrollToItem(target) }
                            }
                        }
                    }
                }
                categories.forEach { cat ->
                    item(key = "cat-${cat.id}") {
                        Text(cat.nameAr, style = KonoFont.Title2, color = KonoColor.TextPrimary, modifier = Modifier.padding(start = KonoSpace.screen, end = KonoSpace.screen, top = KonoSpace.xl, bottom = KonoSpace.m))
                    }
                    cat.items.forEach { p ->
                        item(key = "p-${p.id}") {
                            MenuItemRow(
                                p, store, CartStore.quantityOf(p),
                                Modifier.padding(horizontal = KonoSpace.screen).padding(bottom = KonoSpace.m),
                            ) { if (p.optionGroups.isEmpty()) addDirect(p, store) else sheetProduct = p }
                        }
                    }
                }
            } else if (!failed) {
                items(4) {
                    SkeletonBlock(104.dp, 360.dp, Modifier.padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s), 14.dp)
                }
            }
        }

        // top bar (transparent over the hero, solid once scrolled)
        val barBg = if (collapsed) KonoColor.Surface else Color.Transparent
        Row(
            Modifier.fillMaxWidth().background(barBg).statusBarsPadding().padding(horizontal = KonoSpace.screen, vertical = KonoSpace.s),
            horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", onClick = onBack)
            Box(Modifier.weight(1f)) {
                if (collapsed && d != null) Text(d.name, style = KonoFont.Headline, color = KonoColor.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (d != null) {
                GlassButton(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "المفضلة", tint = if (fav) KonoColor.Danger else KonoColor.Brown) { fav = !fav }
                GlassButton(Icons.Filled.Share, "مشاركة") {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "جرّب ${d.name} على كونو — ${d.cuisineAr}") }
                    ctx.startActivity(Intent.createChooser(send, null))
                }
            }
        }

        if (failed) Text("تعذّر تحميل المتجر", style = KonoFont.Callout, color = KonoColor.TextSecondary, modifier = Modifier.align(Alignment.Center))

        // floating cart bar
        AnimatedVisibility(
            visible = CartStore.itemCount > 0 && CartStore.store?.id == storeId,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
        ) { CartBar(CartStore.itemCount, CartStore.total, onCart) }
    }

    val sp = sheetProduct
    if (sp != null && store != null) ProductSheet(sp, store, onDismiss = { sheetProduct = null })
}

@Composable
private fun GlassButton(icon: ImageVector, desc: String, tint: Color = KonoColor.Brown, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).konoShadow(KonoShadow.Sm, CircleShape).clip(CircleShape).background(KonoColor.Surface.copy(alpha = 0.88f))
            .konoClickable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(19.dp), tint = tint) }
}

@Composable
private fun InfoCard(d: StoreDetail, store: Store, modifier: Modifier = Modifier) {
    KonoShadowCard(modifier, shadow = KonoShadow.Md) {
        Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
            LogoBubble(d.logoImageUrl, d.name, size = 52.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(d.name, style = KonoFont.Title2, color = KonoColor.TextPrimary)
                Text(d.cuisineAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = KonoSpace.m), verticalAlignment = Alignment.CenterVertically) {
            InfoPill(Icons.Filled.Star, String.format(Locale.US, "%.1f", d.rating), "${d.ratingCount}+ تقييم", KonoColor.Rating, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(34.dp).background(KonoColor.Divider))
            InfoPill(Icons.Filled.AccessTime, "${d.etaMinutes} د", "وقت التوصيل", KonoColor.Brown, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(34.dp).background(KonoColor.Divider))
            InfoPill(Icons.Filled.DeliveryDining, if (d.deliveryFee == 0.0) "مجاني" else "${d.deliveryFee.clean()} ر.س", "التوصيل", KonoColor.Brown, Modifier.weight(1f))
        }
        if (d.isClosed || d.isBusy) {
            Text(
                if (d.isClosed) "مغلق الآن" else "مزدحم — وقت التحضير أطول",
                style = KonoFont.Subheadline, color = KonoColor.Warning,
                modifier = Modifier.padding(top = KonoSpace.m).fillMaxWidth().clip(RoundedCornerShape(KonoRadius.s)).background(KonoColor.Warning.copy(alpha = 0.12f)).padding(KonoSpace.m, KonoSpace.s),
            )
        }
        if (d.promoTagAr != null) {
            Row(
                Modifier.padding(top = KonoSpace.m).fillMaxWidth().clip(RoundedCornerShape(KonoRadius.s)).background(KonoColor.Danger.copy(alpha = 0.1f)).padding(KonoSpace.m, KonoSpace.s),
                horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalOffer, null, Modifier.size(14.dp), tint = KonoColor.Danger)
                Text(d.promoTagAr, style = KonoFont.Subheadline, color = KonoColor.Danger)
            }
        }
    }
}

@Composable
private fun InfoPill(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(13.dp), tint = tint)
            Text(value, style = cairo(14, FontWeight.ExtraBold), color = KonoColor.TextPrimary)
        }
        Text(label, style = cairo(10, FontWeight.Medium), color = KonoColor.TextSecondary)
    }
}

/** A menu / catalog row: text on the start side, product photo with a quick-add button on the end. */
@Composable
fun MenuItemRow(product: Product, store: Store, quantity: Int, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    val shape = RoundedCornerShape(KonoRadius.m)
    Row(
        modifier.fillMaxWidth().alpha(if (product.inStock) 1f else 0.7f).clip(shape).background(KonoColor.Surface).border(1.dp, KonoColor.Hairline, shape).padding(KonoSpace.m),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (product.isPopular) KonoTag("الأكثر طلباً", icon = Icons.Filled.LocalFireDepartment, color = KonoColor.Danger)
            Text(product.nameAr, style = KonoFont.BodyMedium, color = KonoColor.TextPrimary)
            if (product.descAr.isNotBlank()) Text(product.descAr, style = KonoFont.Footnote, color = KonoColor.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(KonoSpace.s), verticalAlignment = Alignment.CenterVertically) {
                RiyalText(product.price)
                val old = product.oldPrice
                if (old != null && old > product.price) Text(old.clean(), style = KonoFont.Footnote.copy(textDecoration = TextDecoration.LineThrough), color = KonoColor.TextTertiary)
                product.calories?.let { Text("· $it سعرة", style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
                product.unitAr?.let { Text("· $it", style = KonoFont.Footnote, color = KonoColor.TextTertiary) }
            }
            if (!product.inStock) Text("غير متوفر", style = KonoFont.Caption, color = KonoColor.Danger)
        }
        Box(Modifier.size(96.dp)) {
            AssetImage(product.imageUrl, Modifier.fillMaxSize().clip(RoundedCornerShape(KonoRadius.m)).alpha(if (product.inStock) 1f else 0.4f), fallbackTint = store.kindEnum.accent)
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 6.dp).size(30.dp)
                    .konoShadow(KonoShadow.Sm, CircleShape).background(if (quantity > 0) KonoColor.Yellow else KonoColor.Brown, CircleShape)
                    .border(2.dp, KonoColor.Surface, CircleShape)
                    .konoClickable(enabled = product.inStock, pressedScale = 0.88f, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                if (quantity > 0) Text("$quantity", style = cairo(13, FontWeight.Black), color = KonoColor.Brown)
                else Icon(Icons.Filled.Add, "إضافة", Modifier.size(17.dp), tint = KonoColor.TextOnBrown)
            }
        }
    }
}

/** Floating "view cart" bar. */
@Composable
fun CartBar(count: Int, total: Double, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = KonoSpace.screen).padding(bottom = KonoSpace.s)
            .konoShadow(KonoShadow.Lg, KonoRadius.M).clip(KonoRadius.M).background(KonoColor.Brown)
            .konoClickable(pressedScale = 0.98f, onClick = onClick).height(54.dp).padding(horizontal = KonoSpace.l),
        horizontalArrangement = Arrangement.spacedBy(KonoSpace.m), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(Icons.Filled.ShoppingBag, null, Modifier.size(22.dp), tint = KonoColor.TextOnBrown)
            Text("$count", style = cairo(10, FontWeight.Black), color = KonoColor.Brown,
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = (-6).dp).size(16.dp).background(KonoColor.Yellow, CircleShape).wrapContentSize(Alignment.Center))
        }
        Text("عرض السلة", style = KonoFont.Button, color = KonoColor.TextOnBrown, modifier = Modifier.weight(1f))
        RiyalText(total, style = KonoFont.Button, color = KonoColor.TextOnBrown)
    }
}
