package sa.kono.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Int, val conversationId: Int = 0, val senderUserId: Int = 0, val mine: Boolean = false,
    val kind: String = "text", val text: String = "", val mediaUrl: String = "", val durationSec: Int = 0,
    val lat: Double = 0.0, val lng: Double = 0.0, val read: Boolean = false, val createdAt: String = "",
)

@Serializable
data class ChatConversation(
    val conversationId: Int, val orderId: Int, val orderShortCode: String, val topic: String = "",
    val otherPartyName: String = "", val otherPartyRole: String = "", val otherPartyPhone: String = "",
    val otherPartyPhoto: String = "", val storeNameAr: String = "", val messages: List<ChatMessage> = emptyList(),
)

@Serializable
data class ChatThread(
    val conversationId: Int, val orderId: Int, val orderShortCode: String, val topic: String = "",
    val otherPartyName: String = "", val otherPartyRole: String = "", val otherPartyPhone: String = "",
    val otherPartyPhoto: String = "", val storeNameAr: String = "", val lastMessagePreview: String = "",
    val lastMessageAt: String = "", val unreadCount: Int = 0,
)

@Serializable
data class ChatTarget(
    val target: String, val topic: String = "", val otherPartyRole: String = "", val otherPartyName: String = "",
    val otherPartyPhone: String = "", val otherPartyPhoto: String = "", val available: Boolean = true,
    val unavailableReason: String? = null, val conversationId: Int? = null, val unreadCount: Int = 0,
)

@Serializable
data class OrderChatTargets(val orderId: Int, val orderShortCode: String = "", val targets: List<ChatTarget> = emptyList())

@Serializable
data class SendMessageBody(
    val kind: String = "text", val text: String? = null, val mediaUrl: String? = null,
    val durationSec: Int = 0, val lat: Double = 0.0, val lng: Double = 0.0,
)

@Serializable data class ChatUploadBody(val dataUri: String, val kind: String = "image")
@Serializable data class ChatUploadResponse(val url: String)
@Serializable data class StartCallBody(val kind: String = "voice")
@Serializable data class EndCallBody(val status: String = "ended", val durationSec: Int = 0)

@Serializable
data class CallInfo(
    val callId: Int, val conversationId: Int = 0, val kind: String = "voice", val status: String = "",
    val otherPartyName: String = "", val otherPartyPhone: String = "",
)

object ChatRole {
    fun ar(role: String) = when (role) { "merchant" -> "المتجر"; "courier" -> "المندوب"; "customer" -> "العميل"; else -> role }
}

// ── account bodies ──
@Serializable data class UpdateProfileBody(val name: String, val email: String? = null)
@Serializable data class AddPaymentBody(val labelAr: String, val icon: String, val detailAr: String? = null, val makeDefault: Boolean = false)

// ── gateway functions ──
suspend fun AppData.chatThreads(): List<ChatThread> = try { KonoApi.get("/api/chat/threads") } catch (e: ApiError) { emptyList() }
suspend fun AppData.chatTargets(orderId: Int): OrderChatTargets? = try { KonoApi.get("/api/chat/order/$orderId/targets") } catch (e: ApiError) { null }
suspend fun AppData.openChat(orderId: Int, target: String): ChatConversation? =
    try { KonoApi.post<kotlinx.serialization.json.JsonObject, ChatConversation>("/api/chat/order/$orderId/$target", kotlinx.serialization.json.JsonObject(emptyMap())) } catch (e: ApiError) { null }
suspend fun AppData.chatConversation(id: Int): ChatConversation? = try { KonoApi.get("/api/chat/$id") } catch (e: ApiError) { null }
suspend fun AppData.chatMessages(id: Int, afterId: Int): List<ChatMessage> =
    try { KonoApi.get("/api/chat/$id/messages", mapOf("afterId" to afterId.toString())) } catch (e: ApiError) { emptyList() }
suspend fun AppData.sendChat(id: Int, body: SendMessageBody): ChatMessage? =
    try { KonoApi.post<SendMessageBody, ChatMessage>("/api/chat/$id/messages", body) } catch (e: ApiError) { null }
suspend fun AppData.markChatRead(id: Int) { runCatching { KonoApi.call("POST", "/api/chat/$id/read") } }
suspend fun AppData.uploadChatMedia(dataUri: String, kind: String): String? =
    try { KonoApi.post<ChatUploadBody, ChatUploadResponse>("/api/chat/upload", ChatUploadBody(dataUri, kind)).url } catch (e: ApiError) { null }
suspend fun AppData.startChatCall(id: Int): CallInfo? = try { KonoApi.post<StartCallBody, CallInfo>("/api/chat/$id/call", StartCallBody()) } catch (e: ApiError) { null }
suspend fun AppData.endChatCall(callId: Int, durationSec: Int) {
    runCatching { KonoApi.post<EndCallBody, Unit>("/api/chat/calls/$callId/end", EndCallBody("ended", durationSec)) }
}

// account
suspend fun AppData.deleteAddress(a: Address) { runCatching { KonoApi.call("DELETE", "/api/addresses/${a.id}") }; loadAccount() }
suspend fun AppData.addPayment(label: String, icon: String, detail: String?) {
    runCatching { KonoApi.post<AddPaymentBody, Unit>("/api/payment-methods", AddPaymentBody(label, icon, detail)) }; loadAccount()
}
suspend fun AppData.deletePayment(p: PaymentMethod) { runCatching { KonoApi.call("DELETE", "/api/payment-methods/${p.id}") }; loadAccount() }
suspend fun AppData.updateProfile(name: String, email: String?) {
    runCatching { replaceMe(KonoApi.post<UpdateProfileBody, AuthUser>("/api/auth/me", UpdateProfileBody(name, email), method = "PUT")) }
}
suspend fun AppData.toggleKonoPlus() { runCatching { replaceMe(KonoApi.post<AuthUser>("/api/auth/kono-plus")) } }
