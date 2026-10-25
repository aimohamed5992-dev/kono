namespace Kono.Api.Domain;

/// Which two parties a conversation connects. Every thread is strictly 1:1 and
/// scoped to a single order — there are no group chats.
public enum ChatTopic { CustomerMerchant, CustomerCourier, CourierMerchant }

public enum MessageKind { Text, Image, Voice, Location, System }

public enum CallKind { Voice, Video }
public enum CallStatus { Ringing, Answered, Declined, Missed, Ended }

public class Conversation : BaseEntity
{
    public int OrderId { get; set; }
    public Order Order { get; set; } = null!;
    public ChatTopic Topic { get; set; }

    public int PartyAUserId { get; set; }
    public int PartyBUserId { get; set; }
    public string PartyARole { get; set; } = "";   // customer | merchant | courier
    public string PartyBRole { get; set; } = "";

    public DateTime LastMessageAt { get; set; } = DateTime.UtcNow;
    public string LastMessagePreview { get; set; } = "";

    public List<ChatMessage> Messages { get; set; } = new();

    public bool Involves(int userId) => PartyAUserId == userId || PartyBUserId == userId;
    public int Other(int userId) => PartyAUserId == userId ? PartyBUserId : PartyAUserId;
    public string OtherRole(int userId) => PartyAUserId == userId ? PartyBRole : PartyARole;
}

public class ChatMessage : BaseEntity
{
    public int ConversationId { get; set; }
    public Conversation Conversation { get; set; } = null!;

    public int SenderUserId { get; set; }
    public MessageKind Kind { get; set; } = MessageKind.Text;

    public string Text { get; set; } = "";
    public string MediaUrl { get; set; } = "";
    public int DurationSec { get; set; }          // voice notes
    public double Lat { get; set; }
    public double Lng { get; set; }

    public DateTime? ReadAt { get; set; }
}

public class CallLog : BaseEntity
{
    public int ConversationId { get; set; }
    public Conversation Conversation { get; set; } = null!;

    public int CallerUserId { get; set; }
    public int CalleeUserId { get; set; }
    public CallKind Kind { get; set; } = CallKind.Voice;
    public CallStatus Status { get; set; } = CallStatus.Ringing;

    public DateTime StartedAt { get; set; } = DateTime.UtcNow;
    public DateTime? EndedAt { get; set; }
    public int DurationSec { get; set; }
}
