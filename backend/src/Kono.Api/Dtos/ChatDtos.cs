using System.ComponentModel.DataAnnotations;

namespace Kono.Api.Dtos;

public record ChatMessageDto(
    int Id, int ConversationId, int SenderUserId, bool Mine,
    string Kind, string Text, string MediaUrl, int DurationSec,
    double Lat, double Lng, bool Read, DateTime CreatedAt);

public record ChatConversationDto(
    int ConversationId, int OrderId, string OrderShortCode, string Topic,
    string OtherPartyName, string OtherPartyRole, string OtherPartyPhone, string OtherPartyPhoto,
    string StoreNameAr, IReadOnlyList<ChatMessageDto> Messages);

public record ChatThreadDto(
    int ConversationId, int OrderId, string OrderShortCode, string Topic,
    string OtherPartyName, string OtherPartyRole, string OtherPartyPhone, string OtherPartyPhoto,
    string StoreNameAr, string LastMessagePreview, DateTime LastMessageAt, int UnreadCount);

public record ChatTargetDto(
    string Target, string Topic, string OtherPartyRole, string OtherPartyName,
    string OtherPartyPhone, string OtherPartyPhoto, bool Available, string? UnavailableReason,
    int? ConversationId, int UnreadCount);

public record OrderChatTargetsDto(int OrderId, string OrderShortCode, IReadOnlyList<ChatTargetDto> Targets);

public record SendMessageRequest(
    string Kind = "text", string? Text = null, string? MediaUrl = null,
    int DurationSec = 0, double Lat = 0, double Lng = 0);

public record ChatUploadRequest([Required] string DataUri, string Kind = "image");
public record ChatUploadResponse(string Url);

public record StartCallRequest(string Kind = "voice");
public record EndCallRequest(string Status = "ended", int DurationSec = 0);
public record CallDto(
    int CallId, int ConversationId, string Kind, string Status,
    int CallerUserId, int CalleeUserId,
    string OtherPartyName, string OtherPartyPhone, DateTime StartedAt);
