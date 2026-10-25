import SwiftUI

/// Drives one open conversation. The backend pushes over SignalR to web clients;
/// the app keeps it simple and short-polls `/messages?afterId=` while the screen
/// is visible (same pattern as order tracking).
@MainActor
@Observable
final class ChatStore: Identifiable {
    nonisolated let id = UUID()
    private let data = AppData.shared

    var conversationId: Int
    let orderId: Int
    let orderShortCode: String
    var otherName: String
    var otherRole: String
    var otherPhone: String
    var otherPhoto: String

    var messages: [ChatMessageDTO] = []
    var loading = true
    var sending = false
    var errorText: String?

    // active call
    var activeCall: CallDTO?

    private var polling: Task<Void, Never>?
    private var lastId: Int { messages.map(\.id).max() ?? 0 }

    init(conversation c: ChatConversationDTO) {
        conversationId = c.conversationId
        orderId = c.orderId
        orderShortCode = c.orderShortCode
        otherName = c.otherPartyName
        otherRole = c.otherPartyRole
        otherPhone = c.otherPartyPhone
        otherPhoto = c.otherPartyPhoto
        messages = c.messages
        loading = false
    }

    func start() {
        Task { await data.markChatRead(conversationId) }
        polling?.cancel()
        polling = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(2.5))
                guard let self, !Task.isCancelled else { return }
                let new = await self.data.chatMessages(conversationId: self.conversationId, afterId: self.lastId)
                if !new.isEmpty {
                    self.messages.append(contentsOf: new)
                    await self.data.markChatRead(self.conversationId)
                } else {
                    // refresh read receipts on my own messages
                    await self.refreshReceipts()
                }
            }
        }
    }

    func stop() { polling?.cancel(); polling = nil }

    private func refreshReceipts() async {
        guard let full = try? await data.chatConversation(conversationId) else { return }
        // only adopt if counts match to avoid clobbering optimistic sends
        if full.messages.count == messages.count { messages = full.messages }
    }

    // MARK: sending

    func sendText(_ raw: String) {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !sending else { return }
        Task { await push(SendMessageBody(kind: "text", text: text)) }
    }

    func sendImage(_ image: UIImage) {
        guard let jpeg = image.downscaled(to: 1280).jpegData(compressionQuality: 0.7) else { return }
        let dataUri = "data:image/jpeg;base64," + jpeg.base64EncodedString()
        Task {
            sending = true
            defer { sending = false }
            do {
                let url = try await data.uploadChatMedia(dataUri: dataUri, kind: "image")
                await push(SendMessageBody(kind: "image", mediaUrl: url))
            } catch { errorText = "تعذّر إرسال الصورة" }
        }
    }

    func sendVoice(_ fileURL: URL, duration: Int) {
        guard let bytes = try? Data(contentsOf: fileURL) else { return }
        let dataUri = "data:audio/m4a;base64," + bytes.base64EncodedString()
        Task {
            sending = true
            defer { sending = false }
            do {
                let url = try await data.uploadChatMedia(dataUri: dataUri, kind: "voice")
                await push(SendMessageBody(kind: "voice", mediaUrl: url, durationSec: max(1, duration)))
            } catch { errorText = "تعذّر إرسال الرسالة الصوتية" }
        }
    }

    func sendLocation(lat: Double, lng: Double) {
        Task { await push(SendMessageBody(kind: "location", lat: lat, lng: lng)) }
    }

    private func push(_ body: SendMessageBody) async {
        sending = true
        defer { sending = false }
        do {
            let msg = try await data.sendChat(conversationId: conversationId, body)
            if !messages.contains(where: { $0.id == msg.id }) { messages.append(msg) }
        } catch {
            errorText = "تعذّر إرسال الرسالة"
        }
    }

    // MARK: calls

    func startCall() async -> URL? {
        do {
            let call = try await data.startChatCall(conversationId: conversationId)
            activeCall = call
            let tel = call.otherPartyPhone.filter { $0.isNumber || $0 == "+" }
            return URL(string: "tel:\(tel)")
        } catch {
            errorText = "تعذّر بدء المكالمة"
            return nil
        }
    }

    func endCall(duration: Int) {
        guard let call = activeCall else { return }
        activeCall = nil
        Task { await data.endChatCall(callId: call.callId, status: "ended", duration: duration) }
    }
}
