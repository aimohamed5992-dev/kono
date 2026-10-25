using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api")]
[Authorize]
public class AccountController : ControllerBase
{
    private readonly AccountService _acct;
    private readonly CurrentUser _me;

    public AccountController(AccountService acct, CurrentUser me)
    {
        _acct = acct;
        _me = me;
    }

    // ── addresses ───────────────────────────────────────────────────────────
    [HttpGet("addresses")]
    public async Task<ActionResult<IReadOnlyList<AddressDto>>> Addresses()
        => Ok(await _acct.AddressesAsync(_me.Id));

    [HttpPost("addresses")]
    public async Task<ActionResult<AddressDto>> AddAddress(AddressUpsertRequest req)
        => Ok(await _acct.AddAddressAsync(_me.Id, req));

    [HttpPut("addresses/{id:int}")]
    public async Task<ActionResult<AddressDto>> UpdateAddress(int id, AddressUpsertRequest req)
        => Ok(await _acct.UpdateAddressAsync(_me.Id, id, req));

    [HttpPost("addresses/{id:int}/default")]
    public async Task<IActionResult> SetDefaultAddress(int id)
    {
        await _acct.SetDefaultAddressAsync(_me.Id, id);
        return NoContent();
    }

    [HttpDelete("addresses/{id:int}")]
    public async Task<IActionResult> DeleteAddress(int id)
    {
        await _acct.DeleteAddressAsync(_me.Id, id);
        return NoContent();
    }

    // ── payment methods ─────────────────────────────────────────────────────
    [HttpGet("payment-methods")]
    public async Task<ActionResult<IReadOnlyList<PaymentMethodDto>>> Payments()
        => Ok(await _acct.PaymentsAsync(_me.Id));

    [HttpPost("payment-methods")]
    public async Task<ActionResult<PaymentMethodDto>> AddPayment(PaymentMethodUpsertRequest req)
        => Ok(await _acct.AddPaymentAsync(_me.Id, req));

    [HttpDelete("payment-methods/{id:int}")]
    public async Task<IActionResult> DeletePayment(int id)
    {
        await _acct.DeletePaymentAsync(_me.Id, id);
        return NoContent();
    }

    // ── profile ─────────────────────────────────────────────────────────────
    [HttpPut("auth/me")]
    public async Task<ActionResult<UserSummary>> UpdateProfile(UpdateProfileRequest req)
        => Ok(await _acct.UpdateProfileAsync(_me.Id, req));

    [HttpPost("auth/kono-plus")]
    public async Task<ActionResult<UserSummary>> ToggleKonoPlus()
        => Ok(await _acct.ToggleKonoPlusAsync(_me.Id));
}
