import Foundation

// Wire models for the /api/chat endpoints.

struct ChatMessageDTO: Decodable, Identifiable {
    let id: Int
    let conversationId: Int
    let senderUserId: Int
    let mine: Bool
    let kind: String            // text | image | voice | location | system
    let text: String
    let mediaUrl: String
    let durationSec: Int
    let lat: Double
    let lng: Double
    let read: Bool
    let createdAt: Date
}

struct ChatConversationDTO: Decodable {
    let conversationId: Int
    let orderId: Int
    let orderShortCode: String
    let topic: String
    let otherPartyName: String
    let otherPartyRole: String      // customer | merchant | courier
    let otherPartyPhone: String
    let otherPartyPhoto: String
    let storeNameAr: String
    let messages: [ChatMessageDTO]
}

struct ChatThreadDTO: Decodable, Identifiable {
    var id: Int { conversationId }
    let conversationId: Int
    let orderId: Int
    let orderShortCode: String
    let topic: String
    let otherPartyName: String
    let otherPartyRole: String
    let otherPartyPhone: String
    let otherPartyPhoto: String
    let storeNameAr: String
    let lastMessagePreview: String
    let lastMessageAt: Date
    let unreadCount: Int
}

struct ChatTargetDTO: Decodable, Identifiable {
    var id: String { target }
    let target: String              // customer | merchant | courier
    let topic: String
    let otherPartyRole: String
    let otherPartyName: String
    let otherPartyPhone: String
    let otherPartyPhoto: String
    let available: Bool
    let unavailableReason: String?
    let conversationId: Int?
    let unreadCount: Int
}

struct OrderChatTargetsDTO: Decodable {
    let orderId: Int
    let orderShortCode: String
    let targets: [ChatTargetDTO]
}

struct ChatUploadResponseDTO: Decodable { let url: String }

struct CallDTO: Decodable {
    let callId: Int
    let conversationId: Int
    let kind: String
    let status: String
    let callerUserId: Int
    let calleeUserId: Int
    let otherPartyName: String
    let otherPartyPhone: String
    let startedAt: Date
}

// Request bodies

struct SendMessageBody: Encodable {
    var kind: String = "text"
    var text: String? = nil
    var mediaUrl: String? = nil
    var durationSec: Int = 0
    var lat: Double = 0
    var lng: Double = 0
}

struct ChatUploadBody: Encodable {
    let dataUri: String
    var kind: String = "image"
}

struct StartCallBody: Encodable { var kind: String = "voice" }
struct EndCallBody: Encodable { var status: String = "ended"; var durationSec: Int = 0 }

enum ChatRole {
    static func ar(_ role: String) -> String {
        switch role {
        case "merchant": return "المتجر"
        case "courier": return "المندوب"
        case "customer": return "العميل"
        default: return role
        }
    }
    static func icon(_ role: String) -> String {
        switch role {
        case "merchant": return "storefront.fill"
        case "courier": return "bicycle"
        case "customer": return "person.fill"
        default: return "person.fill"
        }
    }
}
