import SwiftUI
import PhotosUI

struct MerchantMenuView: View {
    @Environment(\.merchant) private var session
    @State private var path = NavigationPath()
    @State private var query = ""
    @State private var addingItem = false

    private var filtered: [MenuItemStatus] {
        query.isEmpty ? session.menu
            : session.menu.filter { $0.product.nameAr.localizedCaseInsensitiveContains(query) }
    }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                LazyVStack(spacing: KonoSpace.m) {
                    HStack {
                        Text("القائمة").font(KonoFont.largeTitle).foregroundStyle(KonoColor.textPrimary)
                        Spacer()
                        KonoPillButton(title: "إضافة صنف", icon: "plus") { addingItem = true }
                    }
                    .padding(.top, KonoSpace.s)

                    KonoSearchBar(placeholder: "ابحث في القائمة", text: $query)

                    let unavailable = session.menu.filter { !$0.available }.count
                    if unavailable > 0 {
                        HStack(spacing: KonoSpace.s) {
                            Image(systemName: "eye.slash.fill").font(.system(size: 12))
                            Text("\(unavailable) صنف غير متاح حالياً للعملاء").font(KonoFont.footnote)
                            Spacer()
                        }
                        .foregroundStyle(KonoColor.caramel)
                        .padding(KonoSpace.m)
                        .background(KonoColor.caramel.opacity(0.1), in: RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
                    }

                    ForEach(filtered) { item in
                        MenuItemStatusRow(item: item,
                                          onToggle: { withAnimation(KonoMotion.snappy) { session.toggleAvailability(item) } },
                                          onOpen: { path.append(MerchantRoute.menuItem(item)) })
                    }
                }
                .konoScreenPadding()
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationBarHidden(true)
            .navigationDestination(for: MerchantRoute.self) { route in
                if case .menuItem(let m) = route { MenuItemEditView(item: m) }
            }
            .sheet(isPresented: $addingItem) {
                AddMenuItemSheet { name, price, desc in
                    session.addItem(nameAr: name, price: price, descAr: desc)
                }
            }
            .task { await session.ensureBootstrapped() }
        }
    }
}

struct AddMenuItemSheet: View {
    @Environment(\.dismiss) private var dismiss
    var onSave: (String, Double, String) -> Void

    @State private var name = ""
    @State private var price = ""
    @State private var desc = ""

    private var valid: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty && Double(price) != nil }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: KonoSpace.l) {
                    field("اسم الصنف", $name)
                    field("السعر (ر.س)", $price)
                    field("الوصف", $desc)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.m)
            }
            .background(KonoColor.background)
            .navigationTitle("صنف جديد")
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                KonoButton(title: "إضافة الصنف") {
                    onSave(name.trimmingCharacters(in: .whitespaces), Double(price) ?? 0,
                           desc.trimmingCharacters(in: .whitespaces))
                    dismiss()
                }
                .disabled(!valid)
                .opacity(valid ? 1 : 0.5)
                .konoScreenPadding().padding(.vertical, KonoSpace.m)
                .background(KonoColor.surface.ignoresSafeArea(edges: .bottom))
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("إلغاء") { dismiss() }
                }
            }
        }
    }

    private func field(_ label: String, _ text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.xs) {
            Text(label).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            KonoTextField(placeholder: label, text: text)
        }
    }
}

struct MenuItemStatusRow: View {
    let item: MenuItemStatus
    var onToggle: () -> Void
    var onOpen: () -> Void

    var body: some View {
        HStack(spacing: KonoSpace.m) {
            AssetImage(name: item.product.imageAsset,
                       fallbackSymbol: "takeoutbag.and.cup.and.straw.fill",
                       fallbackTint: KonoColor.caramel)
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.s, style: .continuous))
                .opacity(item.available ? 1 : 0.4)
            VStack(alignment: .leading, spacing: 3) {
                Text(item.product.nameAr).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                    .lineLimit(1)
                HStack(spacing: KonoSpace.s) {
                    RiyalText(amount: item.product.price, font: KonoFont.subheadline)
                    Text("· بيع اليوم \(item.soldToday)").font(KonoFont.footnote)
                        .foregroundStyle(KonoColor.textTertiary)
                }
            }
            Spacer()
            Toggle("", isOn: Binding(get: { item.available }, set: { _ in onToggle() }))
                .labelsHidden()
                .tint(KonoColor.success)
        }
        .padding(KonoSpace.m)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
    }
}

struct MenuItemEditView: View {
    let item: MenuItemStatus
    @Environment(\.merchant) private var session
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var price: String
    @State private var desc: String
    @State private var available: Bool
    @State private var photoPick: PhotosPickerItem?
    @State private var pickedImage: Image?
    @State private var saved = false

    init(item: MenuItemStatus) {
        self.item = item
        _name = State(initialValue: item.product.nameAr)
        _price = State(initialValue: item.product.price.clean)
        _desc = State(initialValue: item.product.descAr)
        _available = State(initialValue: item.available)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: KonoSpace.l) {
                Group {
                    if let pickedImage {
                        pickedImage.resizable().scaledToFill()
                    } else {
                        AssetImage(name: item.product.imageAsset,
                                   fallbackSymbol: "photo", fallbackTint: KonoColor.caramel)
                    }
                }
                    .frame(height: 180).frame(maxWidth: .infinity)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
                    .overlay(alignment: .bottomTrailing) {
                        PhotosPicker(selection: $photoPick, matching: .images) {
                            Label("تغيير الصورة", systemImage: "camera.fill")
                                .font(KonoFont.caption).foregroundStyle(KonoColor.textOnBrown)
                                .padding(.horizontal, KonoSpace.m).padding(.vertical, 8)
                                .background(KonoColor.brown, in: Capsule())
                        }
                        .padding(KonoSpace.s)
                    }
                    .onChange(of: photoPick) { _, newItem in
                        guard let newItem else { return }
                        Task {
                            if let data = try? await newItem.loadTransferable(type: Data.self),
                               let ui = UIImage(data: data) {
                                pickedImage = Image(uiImage: ui)
                            }
                        }
                    }

                field("اسم الصنف", $name)
                field("السعر (ر.س)", $price)
                field("الوصف", $desc)

                Toggle(isOn: $available) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("متاح للطلب").font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                        Text("إيقافه يخفيه من قائمة العملاء فوراً").font(KonoFont.footnote)
                            .foregroundStyle(KonoColor.textSecondary)
                    }
                }
                .tint(KonoColor.success)
                .padding(KonoSpace.m)
                .background(KonoColor.surface).konoCardShape(KonoRadius.m)

                VStack(alignment: .leading, spacing: KonoSpace.s) {
                    Text("مبيعات اليوم").font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    HStack(spacing: KonoSpace.l) {
                        miniStat("\(item.soldToday)", "طلب")
                        miniStat("\((Double(item.soldToday) * item.product.price).clean)", "ر.س")
                    }
                }
                .padding(KonoSpace.l).frame(maxWidth: .infinity, alignment: .leading)
                .background(KonoColor.surface).konoCardShape(KonoRadius.m)
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)
            .padding(.bottom, 120)
        }
        .background(KonoColor.background)
        .scrollIndicators(.hidden)
        .navigationTitle(item.product.nameAr)
        .navigationBarTitleDisplayMode(.inline)
        .hidesKonoTabBar()
        .safeAreaInset(edge: .bottom) {
            KonoButton(title: saved ? "تم الحفظ ✓" : "حفظ التغييرات") {
                session.updateItem(item, nameAr: name.trimmingCharacters(in: .whitespaces),
                                   price: Double(price) ?? item.product.price,
                                   descAr: desc, available: available)
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
                withAnimation { saved = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { dismiss() }
            }
                .konoScreenPadding().padding(.vertical, KonoSpace.m)
                .background(KonoColor.surface.ignoresSafeArea(edges: .bottom).konoShadow(.lg))
        }
    }

    private func field(_ label: String, _ text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.xs) {
            Text(label).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            KonoTextField(placeholder: label, text: text)
        }
    }

    private func miniStat(_ v: String, _ l: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(v).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
            Text(l).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
        }
    }
}
