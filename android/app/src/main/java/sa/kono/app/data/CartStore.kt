package sa.kono.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CartLine(
    val product: Product,
    val quantity: Int = 1,
    val selectedOptionsAr: List<String> = emptyList(),
    val selectedOptionIds: List<Int> = emptyList(),
    val optionsDelta: Double = 0.0,
    val note: String = "",
) {
    val unitPrice get() = product.price + optionsDelta
    val lineTotal get() = unitPrice * quantity
}

/** In-memory cart. One store at a time (like most delivery apps). */
object CartStore {
    var store by mutableStateOf<Store?>(null); private set
    val lines = mutableStateListOf<CartLine>()
    var promoApplied by mutableStateOf(false)

    val itemCount get() = lines.sumOf { it.quantity }
    val subtotal get() = lines.sumOf { it.lineTotal }
    val deliveryFee get() = store?.deliveryFee ?: 0.0
    val serviceFee get() = if (lines.isEmpty()) 0.0 else 2.0
    val discount get() = if (promoApplied) minOf(subtotal * 0.2, 25.0) else 0.0
    val total get() = maxOf(0.0, subtotal + deliveryFee + serviceFee - discount)
    val isEmpty get() = lines.isEmpty()

    fun add(line: CartLine, from: Store) {
        if (store?.id != from.id) { store = from; lines.clear(); promoApplied = false }
        val i = lines.indexOfFirst { it.product.id == line.product.id && it.selectedOptionsAr == line.selectedOptionsAr }
        if (i >= 0) lines[i] = lines[i].copy(quantity = lines[i].quantity + line.quantity) else lines.add(line)
    }

    fun setQuantity(q: Int, line: CartLine) {
        val i = lines.indexOf(line)
        if (i < 0) return
        if (q <= 0) lines.removeAt(i) else lines[i] = line.copy(quantity = q)
        if (lines.isEmpty()) { store = null; promoApplied = false }
    }

    fun quantityOf(product: Product) = lines.filter { it.product.id == product.id }.sumOf { it.quantity }

    fun clear() { store = null; lines.clear(); promoApplied = false }
}
