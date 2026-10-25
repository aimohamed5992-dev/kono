package sa.kono.app.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import sa.kono.app.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sa.kono.app.data.MerchantSession
import sa.kono.app.data.OrderDetail
import sa.kono.app.data.TicketState
import sa.kono.app.ui.components.KonoButton
import sa.kono.app.ui.components.KonoButtonKind
import sa.kono.app.ui.customer.BottomActionBar
import sa.kono.app.ui.customer.ScreenTopBar
import sa.kono.app.ui.shared.OrderAudience
import sa.kono.app.ui.shared.OrderDetailSections
import sa.kono.app.ui.theme.KonoColor
import sa.kono.app.ui.theme.KonoFont

@Composable
fun TicketDetailScreen(orderId: Int, onBack: () -> Unit, onChat: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<OrderDetail?>(null) }
    val ticket = MerchantSession.tickets.firstOrNull { it.id == orderId }

    // reload the detail whenever the ticket changes state (accept / advance)
    LaunchedEffect(orderId, ticket?.state) { detail = MerchantSession.orderDetail(orderId) }

    Column(Modifier.fillMaxSize().background(KonoColor.Background).statusBarsPadding()) {
        ScreenTopBar(detail?.shortCode ?: ticket?.shortCode ?: "الطلب", onBack)
        Box(Modifier.weight(1f)) {
            val d = detail
            if (d != null) Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) { OrderDetailSections(d, OrderAudience.Merchant, onChat) }
            else CircularProgressIndicator(Modifier.align(Alignment.Center), color = KonoColor.Brown)
        }
        BottomActionBar {
            when (ticket?.stateEnum) {
                TicketState.Incoming -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KonoButton("رفض", { scope.launch { MerchantSession.reject(ticket); onBack() } }, Modifier.weight(1f), kind = KonoButtonKind.Destructive)
                    KonoButton("قبول", { scope.launch { MerchantSession.advance(ticket) } }, Modifier.weight(1f))
                }
                TicketState.Preparing -> KonoButton("الطلب جاهز", { scope.launch { MerchantSession.advance(ticket) } }, icon = Icons.Filled.Check, kind = KonoButtonKind.Brand)
                TicketState.Ready -> KonoButton(if (ticket.isDelivery) "تسليم للمندوب" else "تم الاستلام", { scope.launch { MerchantSession.advance(ticket) } }, icon = Icons.Filled.ShoppingBag)
                else -> KonoButton("تم", onBack, kind = KonoButtonKind.Secondary)
            }
        }
    }
}
