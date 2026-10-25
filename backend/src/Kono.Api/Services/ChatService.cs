using Kono.Api.Data;
using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Infrastructure;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Services;

public class ChatService
{
    private readonly KonoDbContext _db;
    private readonly Func<string, string> _img;
    private readonly IHubContext<ChatHub> _hub;
    private readonly IWebHostEnvironment _env;

    public ChatService(KonoDbContext db, ImageUrlBuilder img, IHubContext<ChatHub> hub, IWebHostEnvironment env)
    {
        _db = db;
        _img = img.Func;
        _hub = hub;
        _env = env;
    }

    // ── party resolution ────────────────────────────────────────────────────
    private record Party(int? UserId, string Role, string Name, string Phone, string Photo);

    private async Task<(Order Order, Party Customer, Party Merchant, Party Courier)> PartiesAsync(int orderId)
    {
        var order = await _db.Orders.AsNoTracking()
            .Include(o => o.Customer)
            .Include(o => o.Store).ThenInclude(s => s.Owner)
            .Include(o => o.Courier).ThenInclude(c => c!.User)
            .FirstOrDefaultAsync(o => o.Id == orderId)
            ?? throw new KeyNotFoundException("الطلب غير موجود.");

        var customer = new Party(order.CustomerUserId, "customer",
            order.Customer.NameAr, order.Customer.Phone, "");

        var merchant = order.Store.Owner is null
            ? new Party(null, "merchant", order.Store.Name, "", _img(order.Store.LogoImageUrl))
            : new Party(order.Store.OwnerUserId, "merchant", order.Store.Name,
                order.Store.Owner.Phone, _img(order.Store.LogoImageUrl));

        var courier = order.Courier is null
            ? new Party(null, "courier", "", "", "")
            : new Party(order.Courier.UserId, "courier", order.Courier.Name,
                order.Courier.User.Phone, _img(order.Courier.PhotoUrl));

        return (order, customer, merchant, courier);
    }

    /// The topic + the two parties for a (caller, target) pair.
    private static (ChatTopic Topic, string RoleA, string RoleB) TopicFor(string callerRole, string target)
    {
        var pair = string.Join("+", new[] { callerRole, target }.OrderBy(x => x));
        return pair switch
        {
            "customer+merchant" => (ChatTopic.CustomerMerchant, "customer", "merchant"),
            "courier+customer" => (ChatTopic.CustomerCourier, "customer", "courier"),
            "courier+merchant" => (ChatTopic.CourierMerchant, "courier", "merchant"),
            _ => throw new InvalidOperationException("محادثة غير مسموح بها."),
        };
    }

    private static Party Pick(string role, Party customer, Party merchant, Party courier) => role switch
    {
        "customer" => customer,
        "merchant" => merchant,
        "courier" => courier,
        _ => throw new InvalidOperationException("طرف غير معروف."),
    };

    private async Task<string> CallerRoleAsync(int userId)
    {
        var role = await _db.Users.Where(u => u.Id == userId).Select(u => u.Role).FirstOrDefaultAsync();
        return role switch
        {
            UserRole.Merchant => "merchant",
            UserRole.Courier => "courier",
            _ => "customer",
        };
    }

    // ── get-or-create ───────────────────────────────────────────────────────
    public async Task<ChatConversationDto> OpenAsync(int userId, int orderId, string target)
    {
        var callerRole = await CallerRoleAsync(userId);
        var (topic, roleA, roleB) = TopicFor(callerRole, target);

        var (_, customer, merchant, courier) = await PartiesAsync(orderId);
        var pa = Pick(roleA, customer, merchant, courier);
        var pb = Pick(roleB, customer, merchant, courier);

        // caller must be one of the two parties
        if (pa.UserId != userId && pb.UserId != userId)
            throw new UnauthorizedAccessException("لست طرفاً في هذا الطلب.");

        var counterpart = pa.UserId == userId ? pb : pa;
        if (counterpart.UserId is null)
            throw new InvalidOperationException(target switch
            {
                "courier" => "لم يُسند مندوب لهذا الطلب بعد.",
                "merchant" => "هذا المتجر بلا حساب مدير بعد.",
                _ => "الطرف الآخر غير متاح.",
            });

        var conv = await _db.Conversations
            .FirstOrDefaultAsync(c => c.OrderId == orderId && c.Topic == topic);

        if (conv is null)
        {
            conv = new Conversation
            {
                OrderId = orderId,
                Topic = topic,
                PartyAUserId = pa.UserId ?? 0,
                PartyBUserId = pb.UserId ?? 0,
                PartyARole = roleA,
                PartyBRole = roleB,
                LastMessageAt = DateTime.UtcNow,
                LastMessagePreview = "",
            };
            _db.Conversations.Add(conv);
            await _db.SaveChangesAsync();
        }
        else if (conv.PartyAUserId == 0 || conv.PartyBUserId == 0)
        {
            // a party (usually the courier) has since been assigned — backfill
            conv.PartyAUserId = pa.UserId ?? conv.PartyAUserId;
            conv.PartyBUserId = pb.UserId ?? conv.PartyBUserId;
            await _db.SaveChangesAsync();
        }

        return await ConversationAsync(userId, conv.Id);
    }

    public async Task<ChatConversationDto> ConversationAsync(int userId, int conversationId)
    {
        var conv = await _db.Conversations.AsNoTracking()
            .Include(c => c.Order)
            .FirstOrDefaultAsync(c => c.Id == conversationId)
            ?? throw new KeyNotFoundException("المحادثة غير موجودة.");
        if (!conv.Involves(userId)) throw new UnauthorizedAccessException();

        var (_, customer, merchant, courier) = await PartiesAsync(conv.OrderId);
        var other = OtherParty(conv, userId, customer, merchant, courier);

        var msgs = await _db.ChatMessages.AsNoTracking()
            .Where(m => m.ConversationId == conversationId)
            .OrderBy(m => m.Id)
            .ToListAsync();

        return new ChatConversationDto(
            conv.Id, conv.OrderId, conv.Order.ShortCode, TopicName(conv.Topic),
            other.Name, other.Role, other.Phone, other.Photo, merchant.Name,
            msgs.Select(m => ToDto(m, userId)).ToList());
    }

    private static Party OtherParty(Conversation conv, int userId, Party customer, Party merchant, Party courier)
    {
        var role = conv.OtherRole(userId);
        return role switch
        {
            "customer" => customer,
            "merchant" => merchant,
            "courier" => courier,
            _ => customer,
        };
    }

    // ── threads list ────────────────────────────────────────────────────────
    public async Task<IReadOnlyList<ChatThreadDto>> ThreadsAsync(int userId)
    {
        var convs = await _db.Conversations.AsNoTracking()
            .Include(c => c.Order)
            .Where(c => c.PartyAUserId == userId || c.PartyBUserId == userId)
            .OrderByDescending(c => c.LastMessageAt)
            .ToListAsync();

        var ids = convs.Select(c => c.Id).ToList();
        var unread = await _db.ChatMessages.AsNoTracking()
            .Where(m => ids.Contains(m.ConversationId) && m.SenderUserId != userId && m.ReadAt == null)
            .GroupBy(m => m.ConversationId)
            .Select(g => new { g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        var list = new List<ChatThreadDto>();
        foreach (var c in convs)
        {
            var (_, customer, merchant, courier) = await PartiesAsync(c.OrderId);
            var other = OtherParty(c, userId, customer, merchant, courier);
            list.Add(new ChatThreadDto(
                c.Id, c.OrderId, c.Order.ShortCode, TopicName(c.Topic),
                other.Name, other.Role, other.Phone, other.Photo, merchant.Name,
                c.LastMessagePreview, c.LastMessageAt, unread.GetValueOrDefault(c.Id)));
        }
        return list;
    }

    // ── order targets (who can I message about this order) ───────────────────
    public async Task<OrderChatTargetsDto> OrderTargetsAsync(int userId, int orderId)
    {
        var callerRole = await CallerRoleAsync(userId);
        var (order, customer, merchant, courier) = await PartiesAsync(orderId);

        var wanted = callerRole switch
        {
            "customer" => new[] { "merchant", "courier" },
            "merchant" => new[] { "customer", "courier" },
            "courier" => new[] { "customer", "merchant" },
            _ => Array.Empty<string>(),
        };

        var existing = await _db.Conversations.AsNoTracking()
            .Where(c => c.OrderId == orderId)
            .ToListAsync();
        var unreadByConv = await _db.ChatMessages.AsNoTracking()
            .Where(m => existing.Select(e => e.Id).Contains(m.ConversationId)
                        && m.SenderUserId != userId && m.ReadAt == null)
            .GroupBy(m => m.ConversationId)
            .Select(g => new { g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        var targets = new List<ChatTargetDto>();
        foreach (var target in wanted)
        {
            var (topic, _, _) = TopicFor(callerRole, target);
            var p = Pick(target, customer, merchant, courier);
            var conv = existing.FirstOrDefault(c => c.Topic == topic);
            string? reason = p.UserId is null
                ? (target == "courier" ? "لم يُسند مندوب بعد" : "غير متاح")
                : null;
            targets.Add(new ChatTargetDto(
                target, TopicName(topic), target, p.Name, p.Phone, p.Photo,
                p.UserId is not null, reason,
                conv?.Id, conv is null ? 0 : unreadByConv.GetValueOrDefault(conv.Id)));
        }
        return new OrderChatTargetsDto(orderId, order.ShortCode, targets);
    }

    // ── send / read ─────────────────────────────────────────────────────────
    public async Task<ChatMessageDto> SendAsync(int userId, int conversationId, SendMessageRequest req)
    {
        var conv = await _db.Conversations.FirstOrDefaultAsync(c => c.Id == conversationId)
            ?? throw new KeyNotFoundException("المحادثة غير موجودة.");
        if (!conv.Involves(userId)) throw new UnauthorizedAccessException();

        var kind = Enum.TryParse<MessageKind>(req.Kind, true, out var k) ? k : MessageKind.Text;
        if (kind == MessageKind.Text && string.IsNullOrWhiteSpace(req.Text))
            throw new InvalidOperationException("الرسالة فارغة.");

        var msg = new ChatMessage
        {
            ConversationId = conversationId,
            SenderUserId = userId,
            Kind = kind,
            Text = (req.Text ?? "").Trim(),
            MediaUrl = req.MediaUrl ?? "",
            DurationSec = req.DurationSec,
            Lat = req.Lat,
            Lng = req.Lng,
        };
        _db.ChatMessages.Add(msg);

        conv.LastMessageAt = DateTime.UtcNow;
        conv.LastMessagePreview = Preview(kind, msg.Text);
        await _db.SaveChangesAsync();

        var forRecipient = ToDto(msg, conv.Other(userId));
        var forSender = ToDto(msg, userId);
        await _hub.Clients.Group(ChatHub.ConvGroup(conversationId)).SendAsync("message", forRecipient);
        await _hub.Clients.Group(ChatHub.UserGroup(conv.Other(userId))).SendAsync("message", forRecipient);
        return forSender;
    }

    public async Task<IReadOnlyList<ChatMessageDto>> MessagesAsync(int userId, int conversationId, int afterId)
    {
        var conv = await _db.Conversations.AsNoTracking().FirstOrDefaultAsync(c => c.Id == conversationId)
            ?? throw new KeyNotFoundException();
        if (!conv.Involves(userId)) throw new UnauthorizedAccessException();

        var msgs = await _db.ChatMessages.AsNoTracking()
            .Where(m => m.ConversationId == conversationId && m.Id > afterId)
            .OrderBy(m => m.Id)
            .ToListAsync();
        return msgs.Select(m => ToDto(m, userId)).ToList();
    }

    public async Task MarkReadAsync(int userId, int conversationId)
    {
        var conv = await _db.Conversations.FirstOrDefaultAsync(c => c.Id == conversationId)
            ?? throw new KeyNotFoundException();
        if (!conv.Involves(userId)) throw new UnauthorizedAccessException();

        var unread = await _db.ChatMessages
            .Where(m => m.ConversationId == conversationId && m.SenderUserId != userId && m.ReadAt == null)
            .ToListAsync();
        if (unread.Count == 0) return;

        var now = DateTime.UtcNow;
        foreach (var m in unread) m.ReadAt = now;
        await _db.SaveChangesAsync();

        await _hub.Clients.Group(ChatHub.ConvGroup(conversationId))
            .SendAsync("read", new { conversationId, readerUserId = userId, at = now });
    }

    // ── calls ───────────────────────────────────────────────────────────────
    public async Task<CallDto> StartCallAsync(int userId, int conversationId, string kind)
    {
        var conv = await _db.Conversations.AsNoTracking().Include(c => c.Order)
            .FirstOrDefaultAsync(c => c.Id == conversationId)
            ?? throw new KeyNotFoundException("المحادثة غير موجودة.");
        if (!conv.Involves(userId)) throw new UnauthorizedAccessException();

        var (_, customer, merchant, courier) = await PartiesAsync(conv.OrderId);
        var other = OtherParty(conv, userId, customer, merchant, courier);
        if (other.UserId is null) throw new InvalidOperationException("الطرف الآخر غير متاح للاتصال.");

        var call = new CallLog
        {
            ConversationId = conversationId,
            CallerUserId = userId,
            CalleeUserId = other.UserId.Value,
            Kind = Enum.TryParse<CallKind>(kind, true, out var ck) ? ck : CallKind.Voice,
            Status = CallStatus.Ringing,
        };
        _db.CallLogs.Add(call);
        await _db.SaveChangesAsync();

        var me = await _db.Users.Where(u => u.Id == userId)
            .Select(u => new { u.NameAr, u.Phone }).FirstAsync();

        var dto = new CallDto(call.Id, conversationId, call.Kind.ToString().ToLower(),
            call.Status.ToString().ToLower(), userId, other.UserId.Value,
            other.Name, other.Phone, call.StartedAt);

        // ring the callee
        await _hub.Clients.Group(ChatHub.UserGroup(other.UserId.Value)).SendAsync("call", new
        {
            callId = call.Id,
            conversationId,
            kind = call.Kind.ToString().ToLower(),
            fromUserId = userId,
            fromName = me.NameAr,
            fromPhone = me.Phone,
            orderShortCode = conv.Order.ShortCode,
        });
        return dto;
    }

    public async Task EndCallAsync(int userId, int callId, EndCallRequest req)
    {
        var call = await _db.CallLogs.Include(c => c.Conversation)
            .FirstOrDefaultAsync(c => c.Id == callId)
            ?? throw new KeyNotFoundException("المكالمة غير موجودة.");
        if (call.CallerUserId != userId && call.CalleeUserId != userId)
            throw new UnauthorizedAccessException();

        call.Status = Enum.TryParse<CallStatus>(req.Status, true, out var s) ? s : CallStatus.Ended;
        call.EndedAt = DateTime.UtcNow;
        call.DurationSec = req.DurationSec;
        await _db.SaveChangesAsync();

        var otherId = call.CallerUserId == userId ? call.CalleeUserId : call.CallerUserId;
        await _hub.Clients.Group(ChatHub.UserGroup(otherId)).SendAsync("callEnded", new
        {
            callId, status = call.Status.ToString().ToLower(), durationSec = call.DurationSec,
        });
        await _hub.Clients.Group(ChatHub.ConvGroup(call.ConversationId)).SendAsync("callEnded", new
        {
            callId, status = call.Status.ToString().ToLower(), durationSec = call.DurationSec,
        });
    }

    // ── media upload ────────────────────────────────────────────────────────
    public async Task<ChatUploadResponse> UploadAsync(ChatUploadRequest req)
    {
        var (bytes, ext) = DecodeDataUri(req.DataUri);
        if (bytes.Length > 8 * 1024 * 1024)
            throw new InvalidOperationException("الملف كبير جداً (الحد ٨ ميغابايت).");

        var root = _env.WebRootPath ?? Path.Combine(AppContext.BaseDirectory, "wwwroot");
        var dir = Path.Combine(root, "uploads", "chat");
        Directory.CreateDirectory(dir);

        var name = $"{Guid.NewGuid():N}.{ext}";
        await File.WriteAllBytesAsync(Path.Combine(dir, name), bytes);
        return new ChatUploadResponse(_img($"/uploads/chat/{name}"));
    }

    private static (byte[] Bytes, string Ext) DecodeDataUri(string dataUri)
    {
        // data:<mime>;base64,<payload>
        var comma = dataUri.IndexOf(',');
        if (!dataUri.StartsWith("data:") || comma < 0)
            throw new InvalidOperationException("صيغة الملف غير صحيحة.");
        var header = dataUri[5..comma];
        var mime = header.Split(';')[0].Trim().ToLowerInvariant();
        var bytes = Convert.FromBase64String(dataUri[(comma + 1)..]);
        var ext = mime switch
        {
            "image/jpeg" or "image/jpg" => "jpg",
            "image/png" => "png",
            "image/webp" => "webp",
            "image/heic" => "heic",
            "audio/m4a" or "audio/mp4" or "audio/x-m4a" => "m4a",
            "audio/aac" => "aac",
            "audio/mpeg" => "mp3",
            "audio/wav" or "audio/x-wav" => "wav",
            "audio/webm" => "webm",
            _ => "bin",
        };
        return (bytes, ext);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private ChatMessageDto ToDto(ChatMessage m, int viewerUserId) => new(
        m.Id, m.ConversationId, m.SenderUserId, m.SenderUserId == viewerUserId,
        m.Kind.ToString().ToLower(), m.Text,
        string.IsNullOrEmpty(m.MediaUrl) ? "" : _img(m.MediaUrl),
        m.DurationSec, m.Lat, m.Lng, m.ReadAt != null, m.CreatedAt);

    private static string Preview(MessageKind kind, string text) => kind switch
    {
        MessageKind.Image => "📷 صورة",
        MessageKind.Voice => "🎤 رسالة صوتية",
        MessageKind.Location => "📍 موقع",
        MessageKind.System => text,
        _ => text.Length > 80 ? text[..80] : text,
    };

    private static string TopicName(ChatTopic t) => t switch
    {
        ChatTopic.CustomerMerchant => "customer_merchant",
        ChatTopic.CustomerCourier => "customer_courier",
        ChatTopic.CourierMerchant => "courier_merchant",
        _ => t.ToString(),
    };
}
