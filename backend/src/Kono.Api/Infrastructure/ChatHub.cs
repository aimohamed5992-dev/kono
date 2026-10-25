using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;

namespace Kono.Api.Infrastructure;

/// Real-time transport for chat + calls. Web clients (merchant portal, admin)
/// connect here; the iOS app also connects when available and otherwise falls
/// back to short-polling the REST endpoints.
///
/// Server → client events: "message", "read", "call", "callEnded".
[Authorize]
public class ChatHub : Hub
{
    public static string ConvGroup(int conversationId) => $"conv-{conversationId}";
    public static string UserGroup(int userId) => $"user-{userId}";

    private int? UserId =>
        int.TryParse(Context.User?.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;

    public override async Task OnConnectedAsync()
    {
        if (UserId is int id)
            await Groups.AddToGroupAsync(Context.ConnectionId, UserGroup(id));
        await base.OnConnectedAsync();
    }

    public Task JoinConversation(int conversationId)
        => Groups.AddToGroupAsync(Context.ConnectionId, ConvGroup(conversationId));

    public Task LeaveConversation(int conversationId)
        => Groups.RemoveFromGroupAsync(Context.ConnectionId, ConvGroup(conversationId));
}
