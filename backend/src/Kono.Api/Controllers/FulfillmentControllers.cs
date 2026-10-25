using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api/merchant")]
[Authorize(Roles = "Merchant,Admin")]
public class MerchantController : ControllerBase
{
    private readonly FulfillmentService _svc;
    private readonly CurrentUser _me;

    public MerchantController(FulfillmentService svc, CurrentUser me)
    {
        _svc = svc;
        _me = me;
    }

    /// <summary><c>state</c>: incoming | preparing | ready | handedOver | completed.</summary>
    [HttpGet("tickets")]
    public async Task<ActionResult<IReadOnlyList<MerchantTicketDto>>> Tickets([FromQuery] string? state)
        => Ok(await _svc.TicketsAsync(_me.Id, state));

    [HttpGet("tickets/{orderId:int}")]
    public async Task<ActionResult<OrderDetailDto>> TicketDetail(int orderId)
        => Ok(await _svc.MerchantOrderDetailAsync(_me.Id, orderId));

    [HttpPost("tickets/{orderId:int}/accept")]
    public async Task<ActionResult<MerchantTicketDto>> Accept(int orderId)
        => Ok(await _svc.MerchantAdvanceAsync(_me.Id, orderId, "accept"));

    /// <summary>Move the ticket to the next state (Confirmed→Preparing→Ready→handover).</summary>
    [HttpPost("tickets/{orderId:int}/advance")]
    public async Task<ActionResult<MerchantTicketDto>> Advance(int orderId)
        => Ok(await _svc.MerchantAdvanceAsync(_me.Id, orderId, "advance"));

    [HttpPost("tickets/{orderId:int}/reject")]
    public async Task<ActionResult<MerchantTicketDto>> Reject(int orderId)
        => Ok(await _svc.MerchantAdvanceAsync(_me.Id, orderId, "reject"));

    [HttpGet("menu")]
    public async Task<ActionResult<IReadOnlyList<MerchantMenuItemDto>>> Menu()
        => Ok(await _svc.MenuAsync(_me.Id));

    [HttpPost("menu")]
    public async Task<ActionResult<MerchantMenuItemDto>> AddMenuItem(MerchantMenuUpsertRequest req)
        => Ok(await _svc.UpsertMenuItemAsync(_me.Id, null, req));

    [HttpPut("menu/{productId:int}")]
    public async Task<ActionResult<MerchantMenuItemDto>> UpdateMenuItem(int productId, MerchantMenuUpsertRequest req)
        => Ok(await _svc.UpsertMenuItemAsync(_me.Id, productId, req));

    [HttpPost("status")]
    public async Task<IActionResult> Status(MerchantStatusRequest req)
    {
        await _svc.SetMerchantStatusAsync(_me.Id, req);
        return NoContent();
    }

    [HttpGet("store")]
    public async Task<ActionResult<MerchantStoreDto>> Store()
        => Ok(await _svc.StoreAsync(_me.Id));

    [HttpPatch("store")]
    public async Task<ActionResult<MerchantStoreDto>> UpdateStore(MerchantStoreUpdateRequest req)
        => Ok(await _svc.UpdateStoreAsync(_me.Id, req));

    [HttpGet("reports")]
    public async Task<ActionResult<MerchantReportsDto>> Reports()
        => Ok(await _svc.ReportsAsync(_me.Id));
}

[ApiController]
[Route("api/courier")]
[Authorize(Roles = "Courier,Admin")]
public class CourierController : ControllerBase
{
    private readonly FulfillmentService _svc;
    private readonly CurrentUser _me;

    public CourierController(FulfillmentService svc, CurrentUser me)
    {
        _svc = svc;
        _me = me;
    }

    [HttpPost("status")]
    public async Task<ActionResult<CourierStatusRequest>> Status(CourierStatusRequest req)
        => Ok(await _svc.SetStatusAsync(_me.Id, req));

    [HttpGet("offers")]
    public async Task<ActionResult<IReadOnlyList<CourierJobDto>>> Offers()
        => Ok(await _svc.OffersAsync(_me.Id));

    [HttpGet("me")]
    public async Task<ActionResult<CourierProfileDto>> Me()
        => Ok(await _svc.MeAsync(_me.Id));

    [HttpGet("summary")]
    public async Task<ActionResult<CourierSummaryDto>> Summary()
        => Ok(await _svc.SummaryAsync(_me.Id));

    [HttpGet("jobs/active")]
    public async Task<ActionResult<CourierJobDto>> ActiveJob()
    {
        var job = await _svc.ActiveJobAsync(_me.Id);
        return job is null ? NoContent() : Ok(job);
    }

    [HttpGet("jobs/{orderId:int}")]
    public async Task<ActionResult<OrderDetailDto>> JobDetail(int orderId)
        => Ok(await _svc.CourierOrderDetailAsync(_me.Id, orderId));

    [HttpPost("offers/{orderId:int}/accept")]
    public async Task<ActionResult<CourierJobDto>> Accept(int orderId)
        => Ok(await _svc.AcceptAsync(_me.Id, orderId));

    /// <summary>Advance the active job: headingToStore→pickedUp→onTheWay→arrived→delivered.</summary>
    [HttpPost("jobs/{orderId:int}/advance")]
    public async Task<ActionResult<CourierJobDto>> Advance(int orderId)
        => Ok(await _svc.CourierAdvanceAsync(_me.Id, orderId));
}
