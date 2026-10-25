using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api/orders")]
[Authorize]
public class OrdersController : ControllerBase
{
    private readonly OrderService _orders;
    private readonly CurrentUser _me;

    public OrdersController(OrderService orders, CurrentUser me)
    {
        _orders = orders;
        _me = me;
    }

    [HttpPost]
    public async Task<ActionResult<OrderDto>> Place(PlaceOrderRequest req)
    {
        var order = await _orders.PlaceAsync(_me.Id, req);
        return CreatedAtAction(nameof(Get), new { id = order.Id }, order);
    }

    /// <summary><c>status</c>: active | past (default: all).</summary>
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<OrderDto>>> List([FromQuery] string status = "all")
        => Ok(await _orders.ListForCustomerAsync(_me.Id, status));

    [HttpGet("{id:int}")]
    public async Task<ActionResult<OrderDto>> Get(int id)
    {
        var order = await _orders.GetAsync(_me.Id, id);
        return order is null ? NotFound() : Ok(order);
    }

    [HttpPost("{id:int}/cancel")]
    public async Task<ActionResult<OrderDto>> Cancel(int id)
        => Ok(await _orders.CancelAsync(_me.Id, id));
}
