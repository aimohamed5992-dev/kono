using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Authorize]
[Route("api/chat")]
public class ChatController : ControllerBase
{
    private readonly ChatService _chat;
    private readonly CurrentUser _me;

    public ChatController(ChatService chat, CurrentUser me)
    {
        _chat = chat;
        _me = me;
    }

    /// All my 1:1 threads, most recent first.
    [HttpGet("threads")]
    public async Task<ActionResult<IReadOnlyList<ChatThreadDto>>> Threads()
        => Ok(await _chat.ThreadsAsync(_me.Id));

    /// Who I can message about a given order (merchant / courier / customer).
    [HttpGet("order/{orderId:int}/targets")]
    public async Task<ActionResult<OrderChatTargetsDto>> Targets(int orderId)
        => Ok(await _chat.OrderTargetsAsync(_me.Id, orderId));

    /// Open (or create) the thread with `target` ∈ customer|merchant|courier.
    [HttpPost("order/{orderId:int}/{target}")]
    public async Task<ActionResult<ChatConversationDto>> Open(int orderId, string target)
        => Ok(await _chat.OpenAsync(_me.Id, orderId, target.ToLowerInvariant()));

    [HttpGet("{conversationId:int}")]
    public async Task<ActionResult<ChatConversationDto>> Conversation(int conversationId)
        => Ok(await _chat.ConversationAsync(_me.Id, conversationId));

    [HttpGet("{conversationId:int}/messages")]
    public async Task<ActionResult<IReadOnlyList<ChatMessageDto>>> Messages(
        int conversationId, [FromQuery] int afterId = 0)
        => Ok(await _chat.MessagesAsync(_me.Id, conversationId, afterId));

    [HttpPost("{conversationId:int}/messages")]
    public async Task<ActionResult<ChatMessageDto>> Send(int conversationId, SendMessageRequest req)
        => Ok(await _chat.SendAsync(_me.Id, conversationId, req));

    [HttpPost("{conversationId:int}/read")]
    public async Task<IActionResult> Read(int conversationId)
    {
        await _chat.MarkReadAsync(_me.Id, conversationId);
        return NoContent();
    }

    [HttpPost("{conversationId:int}/call")]
    public async Task<ActionResult<CallDto>> Call(int conversationId, StartCallRequest req)
        => Ok(await _chat.StartCallAsync(_me.Id, conversationId, req.Kind));

    [HttpPost("calls/{callId:int}/end")]
    public async Task<IActionResult> EndCall(int callId, EndCallRequest req)
    {
        await _chat.EndCallAsync(_me.Id, callId, req);
        return NoContent();
    }

    [HttpPost("upload")]
    public async Task<ActionResult<ChatUploadResponse>> Upload(ChatUploadRequest req)
        => Ok(await _chat.UploadAsync(req));
}
