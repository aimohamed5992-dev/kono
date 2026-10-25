import SwiftUI

struct ProductSheet: View {
    let product: Product
    let store: Store
    @Environment(\.cart) private var cart
    @Environment(\.dismiss) private var dismiss

    @State private var quantity = 1
    @State private var selections: [UUID: Set<UUID>] = [:]
    @State private var note = ""

    private var extrasTotal: Double {
        var sum = 0.0
        for group in product.options {
            let picked = selections[group.id] ?? []
            for choice in group.choices where picked.contains(choice.id) {
                sum += choice.priceDelta
            }
        }
        return sum
    }
    private var unitPrice: Double { product.price + extrasTotal }
    private var lineTotal: Double { unitPrice * Double(quantity) }

    private var requiredSatisfied: Bool {
        product.options.filter(\.required).allSatisfy { !(selections[$0.id] ?? []).isEmpty }
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    AssetImage(name: product.imageAsset,
                               fallbackSymbol: "takeoutbag.and.cup.and.straw.fill",
                               fallbackTint: store.kind.accent)
                        .frame(height: 220)
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))

                    VStack(alignment: .leading, spacing: KonoSpace.xs) {
                        Text(product.nameAr).font(KonoFont.title).foregroundStyle(KonoColor.textPrimary)
                        Text(product.descAr).font(KonoFont.callout).foregroundStyle(KonoColor.textSecondary)
                        HStack(spacing: KonoSpace.s) {
                            RiyalText(amount: product.price, font: KonoFont.priceLarge)
                            if let cal = product.calories {
                                Text("· \(cal) سعرة حرارية").font(KonoFont.footnote)
                                    .foregroundStyle(KonoColor.textTertiary)
                            }
                        }
                    }

                    ForEach(product.options) { group in
                        optionGroup(group)
                    }

                    VStack(alignment: .leading, spacing: KonoSpace.s) {
                        Text("ملاحظات للمطعم").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                        KonoTextField(placeholder: "مثال: الصوص على جنب", text: $note, icon: "pencil")
                    }
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .scrollIndicators(.hidden)

            // bottom bar
            HStack(spacing: KonoSpace.m) {
                QuantityStepper(quantity: $quantity)
                KonoButton(title: "إضافة · \(lineTotal.clean) ر.س",
                           enabled: requiredSatisfied) {
                    var opts: [String] = []
                    var optionIds: [Int] = []
                    for group in product.options {
                        let picked = selections[group.id] ?? []
                        let chosen = group.choices.filter { picked.contains($0.id) }
                        opts += chosen.map(\.nameAr)
                        optionIds += chosen.map(\.apiId).filter { $0 != 0 }
                    }
                    cart.add(CartLine(product: product, quantity: quantity,
                                      selectedOptionsAr: opts, selectedOptionIds: optionIds,
                                      optionsDelta: extrasTotal, note: note), from: store)
                    #if canImport(UIKit)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    #endif
                    dismiss()
                }
            }
            .konoScreenPadding()
            .padding(.vertical, KonoSpace.m)
            .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
        .background(KonoColor.background)
    }

    private func optionGroup(_ group: ProductOptionGroup) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            HStack {
                Text(group.titleAr.k).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                Spacer()
                KonoTag(text: group.required ? "مطلوب" : "اختياري",
                        color: group.required ? KonoColor.danger : KonoColor.textSecondary)
            }
            VStack(spacing: KonoSpace.xs) {
                ForEach(group.choices) { choice in
                    let picked = (selections[group.id] ?? []).contains(choice.id)
                    Button {
                        toggle(choice, in: group)
                    } label: {
                        HStack(spacing: KonoSpace.m) {
                            Image(systemName: group.multiSelect
                                  ? (picked ? "checkmark.square.fill" : "square")
                                  : (picked ? "largecircle.fill.circle" : "circle"))
                                .font(.system(size: 18))
                                .foregroundStyle(picked ? KonoColor.brown : KonoColor.textTertiary)
                            Text(choice.nameAr).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            if choice.priceDelta != 0 {
                                Text("+ \(choice.priceDelta.clean) ر.س").font(KonoFont.footnote)
                                    .foregroundStyle(KonoColor.textSecondary)
                            }
                        }
                        .padding(KonoSpace.m)
                        .background(picked ? KonoColor.cream : KonoColor.surface)
                        .konoCardShape(KonoRadius.s)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func toggle(_ choice: ProductOption, in group: ProductOptionGroup) {
        #if canImport(UIKit)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        #endif
        var set = selections[group.id] ?? []
        if group.multiSelect {
            if set.contains(choice.id) { set.remove(choice.id) } else { set.insert(choice.id) }
        } else {
            set = [choice.id]
        }
        withAnimation(KonoMotion.snappy) { selections[group.id] = set }
    }
}
