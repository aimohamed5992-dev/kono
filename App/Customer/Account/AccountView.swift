import SwiftUI

struct AccountView: View {
    @State private var data = AppData.shared
    @State private var path = NavigationPath()
    @State private var editingProfile = false

    private var name: String { data.me?.name ?? "ضيف" }
    private var phone: String { data.me?.phone ?? "" }
    private var wallet: Double { data.me?.walletBalance ?? 0 }
    private var points: Int { data.me?.points ?? 0 }
    private var isPlus: Bool { data.me?.konoPlus ?? false }

    var body: some View {
        NavigationStack(path: $path) {
            ScrollView {
                VStack(spacing: KonoSpace.l) {
                    header

                    HStack(spacing: KonoSpace.m) {
                        statCard("محفظة كونو", "\(wallet.clean) ر.س", "wallet.pass.fill")
                        statCard("نقاطك", "\(points)", "star.circle.fill")
                    }

                    Button { path.append(CustomerRoute.konoPlus) } label: { konoPlusCard }
                        .buttonStyle(.plain)

                    settingsGroup("الطلبات والعناوين", [
                        Row("العناوين المحفوظة", "mappin.and.ellipse", "\(data.addresses.count)", .addresses),
                        Row("طرق الدفع", "creditcard.fill", "\(data.payments.count)", .payments),
                    ])

                    settingsGroup("الحساب", [
                        Row("المحادثات", "bubble.left.and.bubble.right.fill", nil, .chats),
                        Row("الإشعارات", "bell.fill", nil, .notifications),
                        Row("المساعدة والدعم", "questionmark.circle.fill", nil, .help),
                        Row("عن كونو", "info.circle.fill", "١.٠", .about),
                    ])

                    LanguageToggleRow()

                    Button {
                        Task {
                            await data.signOut()
                            path = NavigationPath()
                        }
                    } label: {
                        Label("تسجيل الخروج", systemImage: "rectangle.portrait.and.arrow.right")
                            .font(KonoFont.button).foregroundStyle(KonoColor.danger)
                            .frame(maxWidth: .infinity).frame(height: 50)
                            .background(KonoColor.danger.opacity(0.1),
                                       in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                .padding(.bottom, 120)
            }
            .background(KonoColor.background)
            .scrollIndicators(.hidden)
            .navigationTitle("حسابي")
            .navigationBarTitleDisplayMode(.inline)
            .customerDestinations()
            .task {
                await data.ensureSignedIn(role: .customer)
                await data.loadAccount()
                if ["chat", "chats"].contains(RootFlow.launch.screen), path.isEmpty {
                    path.append(CustomerRoute.chats)
                }
            }
            .sheet(isPresented: $editingProfile) {
                EditProfileSheet(name: name, email: data.me?.email ?? "") { n, e in
                    Task { await data.updateProfile(name: n, email: e.isEmpty ? nil : e) }
                }
                .presentationDetents([.medium])
            }
        }
    }

    private struct Row: Identifiable {
        let id = UUID()
        let title: String, icon: String
        let detail: String?
        let route: CustomerRoute
        init(_ t: String, _ i: String, _ d: String?, _ r: CustomerRoute) {
            title = t; icon = i; detail = d; route = r
        }
    }

    private var header: some View {
        Button { editingProfile = true } label: {
            HStack(spacing: KonoSpace.m) {
                ZStack {
                    Circle().fill(KonoColor.brandGradient).frame(width: 64, height: 64)
                    Text(String(name.prefix(1)))
                        .font(.cairo(26, .heavy))
                        .foregroundStyle(KonoColor.brown)
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(name).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                    Text(phone).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        .environment(\.layoutDirection, .leftToRight)
                }
                Spacer()
                Image(systemName: "pencil.circle.fill").font(.system(size: 24))
                    .foregroundStyle(KonoColor.brown.opacity(0.5))
            }
            .padding(KonoSpace.l)
            .background(KonoColor.surface)
            .konoCardShape(KonoRadius.l)
            .konoShadow(.sm)
        }
        .buttonStyle(.plain)
    }

    private func statCard(_ title: String, _ value: String, _ icon: String) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.xs) {
            Image(systemName: icon).font(.system(size: 18)).foregroundStyle(KonoColor.caramel)
            Text(value).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
            Text(title.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .konoCardShape(KonoRadius.m)
    }

    private var konoPlusCard: some View {
        HStack(spacing: KonoSpace.m) {
            Image(systemName: "crown.fill").font(.system(size: 22)).foregroundStyle(KonoColor.brown)
            VStack(alignment: .leading, spacing: 2) {
                Text("كونو بلس").font(KonoFont.headline).foregroundStyle(KonoColor.brown)
                Text(isPlus ? "اشتراكك فعّال — توصيل مجاني بلا حدود" : "وفّر رسوم التوصيل طوال الشهر")
                    .font(KonoFont.footnote).foregroundStyle(KonoColor.brown.opacity(0.8))
            }
            Spacer()
            Image(systemName: "chevron.left").font(.system(size: 13, weight: .bold))
                .foregroundStyle(KonoColor.brown)
        }
        .padding(KonoSpace.l)
        .background(KonoColor.brandGradient)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
    }

    private func settingsGroup(_ title: String, _ rows: [Row]) -> some View {
        VStack(alignment: .leading, spacing: KonoSpace.s) {
            Text(title.k).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
                .padding(.horizontal, KonoSpace.xs)
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                    Button { path.append(row.route) } label: {
                        HStack(spacing: KonoSpace.m) {
                            Image(systemName: row.icon).font(.system(size: 15)).foregroundStyle(KonoColor.brown)
                                .frame(width: 26)
                            Text(row.title.k).font(KonoFont.body).foregroundStyle(KonoColor.textPrimary)
                            Spacer()
                            if let detail = row.detail {
                                Text(detail.k).font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                            }
                            Image(systemName: "chevron.left").font(.system(size: 12, weight: .bold))
                                .foregroundStyle(KonoColor.textTertiary)
                        }
                        .padding(.vertical, KonoSpace.m)
                        .padding(.horizontal, KonoSpace.l)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    if i < rows.count - 1 { Divider().padding(.leading, 52) }
                }
            }
            .background(KonoColor.surface)
            .konoCardShape(KonoRadius.m)
        }
    }
}

private struct EditProfileSheet: View {
    @State var name: String
    @State var email: String
    var onSave: (String, String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            Text("تعديل الملف").font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                .padding(.top, KonoSpace.l)
            KonoTextField(placeholder: "الاسم", text: $name, icon: "person")
            KonoTextField(placeholder: "البريد الإلكتروني", text: $email, icon: "envelope",
                          keyboard: .emailAddress)
            KonoButton(title: "حفظ") {
                onSave(name.trimmingCharacters(in: .whitespaces), email.trimmingCharacters(in: .whitespaces))
                dismiss()
            }
            Spacer()
        }
        .konoScreenPadding()
        .background(KonoColor.background)
    }
}
