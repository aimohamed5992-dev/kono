import SwiftUI

/// Drop-in row of "message X" buttons for an order. Resolves who the current
/// user can talk to (merchant / courier / customer) and opens the thread.
struct OrderChatButtons: View {
    let orderId: Int
    var compact = false

    @State private var targets: [ChatTargetDTO] = []
    @State private var opening: String?
    @State private var sheet: ChatStore?

    var body: some View {
        HStack(spacing: KonoSpace.s) {
            ForEach(targets) { t in
                Button {
                    open(t)
                } label: {
                    HStack(spacing: 6) {
                        if opening == t.target {
                            ProgressView().controlSize(.mini).tint(KonoColor.brown)
                        } else {
                            Image(systemName: "bubble.left.fill").font(.system(size: 12, weight: .bold))
                        }
                        Text(compact ? ChatRole.ar(t.otherPartyRole) : "محادثة \(ChatRole.ar(t.otherPartyRole))")
                            .font(KonoFont.subheadline.weight(.semibold))
                        if t.unreadCount > 0 {
                            Text("\(t.unreadCount)").font(.system(size: 10, weight: .black))
                                .foregroundStyle(KonoColor.textOnBrown)
                                .padding(.horizontal, 5).padding(.vertical, 1)
                                .background(KonoColor.danger, in: Capsule())
                        }
                    }
                    .foregroundStyle(t.available ? KonoColor.brown : KonoColor.textTertiary)
                    .padding(.horizontal, KonoSpace.m).padding(.vertical, KonoSpace.s)
                    .frame(maxWidth: .infinity)
                    .background(KonoColor.surface, in: Capsule())
                    .overlay(Capsule().strokeBorder(KonoColor.hairline, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .disabled(!t.available || opening != nil)
            }
        }
        .task { await load() }
        .sheet(item: $sheet) { store in
            NavigationStack { ChatView(store: store) }
        }
    }

    private func load() async {
        guard let res = await AppData.shared.chatTargets(orderId: orderId) else { return }
        targets = res.targets
    }

    private func open(_ t: ChatTarget) {
        guard t.available else { return }
        opening = t.target
        Task {
            defer { opening = nil }
            if let conv = try? await AppData.shared.openChat(orderId: orderId, target: t.target) {
                sheet = ChatStore(conversation: conv)
                await load()
            }
        }
    }

    private typealias ChatTarget = ChatTargetDTO
}

/// Full list of the current user's conversations.
struct ChatListView: View {
    @State private var threads: [ChatThreadDTO] = []
    @State private var loading = true
    @State private var open: ChatStore?

    var body: some View {
        Group {
            if loading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if threads.isEmpty {
                VStack(spacing: KonoSpace.s) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 40)).foregroundStyle(KonoColor.textTertiary)
                    Text("لا توجد محادثات بعد").font(KonoFont.headline).foregroundStyle(KonoColor.textSecondary)
                    Text("تظهر هنا محادثاتك مع المتاجر والمناديب عند تقديم طلب.")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textTertiary)
                        .multilineTextAlignment(.center)
                }
                .konoScreenPadding()
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(threads) { t in
                            Button { openThread(t) } label: { ThreadRow(thread: t) }
                                .buttonStyle(.plain)
                            Divider().padding(.leading, 68)
                        }
                    }
                }
            }
        }
        .background(KonoColor.background)
        .navigationTitle("المحادثات")
        .navigationBarTitleDisplayMode(.inline)
        .task { await reload() }
        .refreshable { await reload() }
        .sheet(item: $open) { store in
            NavigationStack { ChatView(store: store) }
        }
    }

    private func reload() async {
        threads = await AppData.shared.chatThreads()
        loading = false
        if RootFlow.launch.screen == "chat", open == nil, let first = threads.first {
            openThread(first)
        }
    }

    private func openThread(_ t: ChatThreadDTO) {
        Task {
            if let conv = try? await AppData.shared.chatConversation(t.conversationId) {
                open = ChatStore(conversation: conv)
                await reload()
            }
        }
    }
}

private struct ThreadRow: View {
    let thread: ChatThreadDTO

    var body: some View {
        HStack(spacing: KonoSpace.m) {
            AssetImage(name: thread.otherPartyPhoto, fallbackSymbol: ChatRole.icon(thread.otherPartyRole),
                       fallbackTint: KonoColor.brown)
                .frame(width: 46, height: 46).clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(thread.otherPartyName).font(KonoFont.bodyMedium)
                        .foregroundStyle(KonoColor.textPrimary).lineLimit(1)
                    Spacer()
                    Text(thread.lastMessageAt.formatted(date: .omitted, time: .shortened))
                        .font(.system(size: 10)).foregroundStyle(KonoColor.textTertiary)
                }
                HStack {
                    Text(thread.lastMessagePreview.isEmpty
                         ? "طلب \(thread.orderShortCode)" : thread.lastMessagePreview)
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary).lineLimit(1)
                    Spacer()
                    if thread.unreadCount > 0 {
                        Text("\(thread.unreadCount)").font(.system(size: 11, weight: .black))
                            .foregroundStyle(KonoColor.textOnBrown)
                            .frame(minWidth: 18).padding(.horizontal, 4).padding(.vertical, 1)
                            .background(KonoColor.brown, in: Capsule())
                    }
                }
                Text("\(ChatRole.ar(thread.otherPartyRole)) · \(thread.orderShortCode)")
                    .font(.system(size: 10)).foregroundStyle(KonoColor.textTertiary)
            }
        }
        .padding(.horizontal, KonoSpace.l).padding(.vertical, KonoSpace.m)
        .contentShape(Rectangle())
    }
}
