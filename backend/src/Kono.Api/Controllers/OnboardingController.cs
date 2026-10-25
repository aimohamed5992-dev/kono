using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api/onboarding")]
[Authorize]
public class OnboardingController : ControllerBase
{
    private readonly OnboardingService _svc;
    private readonly CurrentUser _me;

    public OnboardingController(OnboardingService svc, CurrentUser me)
    {
        _svc = svc;
        _me = me;
    }

    /// <summary>Current application state (status + the stored form JSON).</summary>
    [HttpGet]
    public async Task<ActionResult<OnboardingStateDto>> Get()
        => Ok(await _svc.GetAsync(_me.Id));

    [HttpPost("courier")]
    [Authorize(Roles = "Courier,Admin")]
    public async Task<ActionResult<OnboardingStateDto>> SubmitCourier(CourierOnboardingRequest req)
        => Ok(await _svc.SubmitCourierAsync(_me.Id, req));

    [HttpPost("merchant")]
    [Authorize(Roles = "Merchant,Admin")]
    public async Task<ActionResult<OnboardingStateDto>> SubmitMerchant(MerchantOnboardingRequest req)
        => Ok(await _svc.SubmitMerchantAsync(_me.Id, req));
}
