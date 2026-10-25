using Kono.Api.Domain;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api")]
public class CatalogController : ControllerBase
{
    private readonly CatalogService _catalog;

    public CatalogController(CatalogService catalog) => _catalog = catalog;

    private static GeoPoint? Origin(double? lat, double? lng) =>
        lat is { } la && lng is { } ln && la != 0 && ln != 0 ? new GeoPoint(la, ln) : null;

    [HttpGet("verticals")]
    public async Task<ActionResult<IReadOnlyList<VerticalDto>>> Verticals()
        => Ok(await _catalog.GetVerticalsAsync());

    [HttpGet("promotions")]
    public async Task<ActionResult<IReadOnlyList<PromotionDto>>> Promotions()
        => Ok(await _catalog.GetPromotionsAsync());

    /// <summary>The full multi-rail home payload. Pass <c>lat</c>/<c>lng</c> to
    /// rank everything by proximity to the chosen address.</summary>
    [HttpGet("home")]
    public async Task<ActionResult<HomeFeedDto>> Home([FromQuery] double? lat, [FromQuery] double? lng)
        => Ok(await _catalog.GetHomeAsync(Origin(lat, lng)));

    [HttpGet("stores")]
    public async Task<ActionResult<PagedResult<StoreListItemDto>>> Stores(
        [FromQuery] string? kind,
        [FromQuery] string? search,
        [FromQuery] string? sort,
        [FromQuery] bool freeDelivery = false,
        [FromQuery] bool openNow = false,
        [FromQuery] bool promo = false,
        [FromQuery] double? lat = null,
        [FromQuery] double? lng = null,
        [FromQuery] double? radiusKm = null,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 20)
    {
        VerticalKind? kindEnum = Enum.TryParse<VerticalKind>(kind, true, out var k) ? k : null;
        var sortEnum = Enum.TryParse<StoreSort>(sort, true, out var s) ? s : StoreSort.Recommended;

        var filter = new StoreFilter(kindEnum, search, sortEnum, freeDelivery, openNow, promo,
            Origin(lat, lng), radiusKm);
        return Ok(await _catalog.GetStoresAsync(filter, new PageQuery { Page = page, PageSize = pageSize }));
    }

    [HttpGet("stores/{id:int}")]
    public async Task<ActionResult<StoreDetailDto>> Store(int id,
        [FromQuery] double? lat, [FromQuery] double? lng)
    {
        var store = await _catalog.GetStoreAsync(id, Origin(lat, lng));
        return store is null ? NotFound() : Ok(store);
    }

    [HttpGet("search")]
    public async Task<ActionResult<SearchResultsDto>> Search(
        [FromQuery] string q, [FromQuery] string scope = "all",
        [FromQuery] double? lat = null, [FromQuery] double? lng = null)
        => Ok(await _catalog.SearchAsync(q, scope, Origin(lat, lng)));
}
