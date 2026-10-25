import SwiftUI

/// In-memory cart for the prototype. One store at a time (like most delivery apps).
@Observable
final class CartStore {
    private(set) var store: Store?
    var lines: [CartLine] = []
    var promoApplied = false

    var itemCount: Int { lines.reduce(0) { $0 + $1.quantity } }
    var subtotal: Double { lines.reduce(0) { $0 + $1.lineTotal } }
    var deliveryFee: Double { store?.deliveryFee ?? 0 }
    var serviceFee: Double { lines.isEmpty ? 0 : 2 }
    var discount: Double { promoApplied ? min(subtotal * 0.2, 25) : 0 }
    var total: Double { max(0, subtotal + deliveryFee + serviceFee - discount) }
    var isEmpty: Bool { lines.isEmpty }

    func add(_ line: CartLine, from store: Store) {
        if self.store?.id != store.id {
            self.store = store
            lines = []
            promoApplied = false
        }
        if let idx = lines.firstIndex(where: {
            $0.product.id == line.product.id && $0.selectedOptionsAr == line.selectedOptionsAr
        }) {
            lines[idx].quantity += line.quantity
        } else {
            lines.append(line)
        }
    }

    func setQuantity(_ q: Int, for line: CartLine) {
        guard let idx = lines.firstIndex(where: { $0.id == line.id }) else { return }
        if q <= 0 { lines.remove(at: idx) }
        else { lines[idx].quantity = q }
        if lines.isEmpty { store = nil; promoApplied = false }
    }

    func quantity(of product: Product) -> Int {
        lines.filter { $0.product.id == product.id }.reduce(0) { $0 + $1.quantity }
    }

    func clear() {
        store = nil; lines = []; promoApplied = false
    }

    /// Seed a demo cart so tracking / orders screens have content.
    static var seeded: CartStore {
        let c = CartStore()
        c.store = Mock.stores[0]
        c.lines = Mock.sampleCart
        return c
    }
}

private struct CartStoreKey: EnvironmentKey {
    static let defaultValue = CartStore()
}

extension EnvironmentValues {
    var cart: CartStore {
        get { self[CartStoreKey.self] }
        set { self[CartStoreKey.self] = newValue }
    }
}
