using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api/admin")]
[Authorize(Roles = "Admin")]
public class AdminController : ControllerBase
{
    private readonly AdminService _admin;
    private readonly StoreAdminService _stores;

    public AdminController(AdminService admin, StoreAdminService stores)
    {
        _admin = admin;
        _stores = stores;
    }

    // ── dashboard ───────────────────────────────────────────────────────────
    [HttpGet("stats")]
    public async Task<ActionResult<AdminStatsDto>> Stats() => Ok(await _admin.StatsAsync());

    // ── orders ──────────────────────────────────────────────────────────────
    [HttpGet("orders")]
    public async Task<ActionResult<PagedResult<AdminOrderRow>>> Orders(
        [FromQuery] string? status, [FromQuery] int? storeId,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
        => Ok(await _admin.OrdersAsync(status, storeId, new PageQuery { Page = page, PageSize = pageSize }));

    [HttpGet("orders/{id:int}")]
    public async Task<ActionResult<OrderDetailDto>> Order(int id)
    {
        var o = await _admin.OrderAsync(id);
        return o is null ? NotFound() : Ok(o);
    }

    [HttpPost("orders/{id:int}/stage")]
    public async Task<ActionResult<OrderDto>> SetStage(int id, [FromBody] SetStageBody body)
    {
        if (!Enum.TryParse<OrderStage>(body.Stage, true, out var stage))
            return BadRequest(new { title = "مرحلة غير معروفة." });
        return Ok(await _admin.SetStageAsync(id, stage));
    }

    public record SetStageBody(string Stage);

    // ── stores CRUD ─────────────────────────────────────────────────────────
    [HttpGet("stores")]
    public async Task<ActionResult<PagedResult<StoreAdminRow>>> Stores(
        [FromQuery] string? search, [FromQuery] string? kind,
        [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
        => Ok(await _stores.ListAsync(search, kind, new PageQuery { Page = page, PageSize = pageSize }));

    [HttpGet("stores/{id:int}")]
    public async Task<ActionResult<StoreDetailDto>> Store(int id)
    {
        var s = await _stores.GetAsync(id);
        return s is null ? NotFound() : Ok(s);
    }

    [HttpPost("stores")]
    public async Task<ActionResult<StoreDetailDto>> CreateStore(StoreUpsertRequest req)
        => CreatedAtAction(nameof(Store), new { id = 0 }, await _stores.CreateAsync(req));

    [HttpPut("stores/{id:int}")]
    public async Task<ActionResult<StoreDetailDto>> UpdateStore(int id, StoreUpsertRequest req)
        => Ok(await _stores.UpdateAsync(id, req));

    [HttpPatch("stores/{id:int}/status")]
    public async Task<IActionResult> StoreStatus(int id, [FromBody] StoreStatusBody body)
    {
        await _stores.SetStatusAsync(id, body.IsActive, body.IsClosed);
        return NoContent();
    }

    public record StoreStatusBody(bool IsActive, bool? IsClosed);

    [HttpDelete("stores/{id:int}")]
    public async Task<IActionResult> DeleteStore(int id)
    {
        await _stores.DeleteAsync(id);
        return NoContent();
    }

    // ── products CRUD ───────────────────────────────────────────────────────
    [HttpGet("stores/{storeId:int}/menu")]
    public async Task<ActionResult<IReadOnlyList<MenuCategoryDto>>> Menu(int storeId)
        => Ok(await _stores.MenuAsync(storeId));

    [HttpPost("stores/{storeId:int}/products")]
    public async Task<ActionResult<ProductDto>> CreateProduct(int storeId, ProductUpsertRequest req)
        => Ok(await _stores.CreateProductAsync(storeId, req));

    [HttpPut("products/{id:int}")]
    public async Task<ActionResult<ProductDto>> UpdateProduct(int id, ProductUpsertRequest req)
        => Ok(await _stores.UpdateProductAsync(id, req));

    [HttpPatch("products/{id:int}/stock")]
    public async Task<IActionResult> ProductStock(int id, [FromBody] StockBody body)
    {
        await _stores.SetStockAsync(id, body.InStock);
        return NoContent();
    }

    public record StockBody(bool InStock);

    [HttpDelete("products/{id:int}")]
    public async Task<IActionResult> DeleteProduct(int id)
    {
        await _stores.DeleteProductAsync(id);
        return NoContent();
    }

    // ── live operations map ─────────────────────────────────────────────────
    [HttpGet("live")]
    public async Task<ActionResult<LiveBoardDto>> LiveBoard()
        => Ok(await _admin.LiveBoardAsync());

    // ── couriers ────────────────────────────────────────────────────────────
    [HttpGet("couriers")]
    public async Task<ActionResult<IReadOnlyList<CourierAdminRow>>> Couriers()
        => Ok(await _admin.CouriersAsync());

    [HttpPost("couriers")]
    public async Task<ActionResult<CourierAdminRow>> CreateCourier(CourierCreateRequest req)
        => Ok(await _admin.CreateCourierAsync(req));

    [HttpPut("couriers/{id:int}")]
    public async Task<ActionResult<CourierAdminRow>> UpdateCourier(int id, CourierUpdateRequest req)
        => Ok(await _admin.UpdateCourierAsync(id, req));

    // ── customers ───────────────────────────────────────────────────────────
    [HttpGet("customers")]
    public async Task<ActionResult<PagedResult<CustomerAdminRow>>> Customers(
        [FromQuery] string? search, [FromQuery] int page = 1, [FromQuery] int pageSize = 20)
        => Ok(await _admin.CustomersAsync(search, new PageQuery { Page = page, PageSize = pageSize }));

    [HttpPatch("users/{id:int}/status")]
    public async Task<IActionResult> UserStatus(int id, [FromBody] ActiveBody body)
    {
        await _admin.SetUserActiveAsync(id, body.IsActive);
        return NoContent();
    }

    public record ActiveBody(bool IsActive);

    // ── verticals ───────────────────────────────────────────────────────────
    [HttpGet("verticals")]
    public async Task<ActionResult<IReadOnlyList<VerticalDto>>> Verticals()
        => Ok(await _admin.VerticalsAsync());

    [HttpPut("verticals/{id:int}")]
    public async Task<ActionResult<VerticalDto>> UpdateVertical(int id, VerticalUpdateRequest req)
        => Ok(await _admin.UpdateVerticalAsync(id, req));

    // ── promotions CRUD ─────────────────────────────────────────────────────
    [HttpGet("promotions")]
    public async Task<ActionResult<IReadOnlyList<PromotionDto>>> Promotions()
        => Ok(await _admin.PromotionsAsync());

    [HttpPost("promotions")]
    public async Task<ActionResult<PromotionDto>> CreatePromotion(PromotionUpsertRequest req)
        => Ok(await _admin.CreatePromotionAsync(req));

    [HttpPut("promotions/{id:int}")]
    public async Task<ActionResult<PromotionDto>> UpdatePromotion(int id, PromotionUpsertRequest req)
        => Ok(await _admin.UpdatePromotionAsync(id, req));

    [HttpDelete("promotions/{id:int}")]
    public async Task<IActionResult> DeletePromotion(int id)
    {
        await _admin.DeletePromotionAsync(id);
        return NoContent();
    }
}
