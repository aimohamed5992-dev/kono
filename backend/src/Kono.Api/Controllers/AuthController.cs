using Kono.Api.Auth;
using Kono.Api.Dtos;
using Kono.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AuthService _auth;
    private readonly CurrentUser _me;

    public AuthController(AuthService auth, CurrentUser me)
    {
        _auth = auth;
        _me = me;
    }

    /// <summary>Create an account (phone + password). Default role: Customer.</summary>
    [HttpPost("register")]
    public async Task<ActionResult<AuthResponse>> Register(RegisterRequest req)
        => Ok(await _auth.RegisterAsync(req));

    /// <summary>Exchange phone + password for tokens.</summary>
    [HttpPost("login")]
    public async Task<ActionResult<AuthResponse>> Login(LoginRequest req)
        => Ok(await _auth.LoginAsync(req));

    /// <summary>Passwordless sign-in by phone (demo OTP). Creates a customer if new.</summary>
    [HttpPost("phone-login")]
    public async Task<ActionResult<AuthResponse>> PhoneLogin(PhoneLoginRequest req)
        => Ok(await _auth.PhoneLoginAsync(req.Phone, req.Name, req.Role));

    /// <summary>Demo helper — sign in as the merchant that owns the given store.</summary>
    [HttpPost("demo-store-merchant")]
    public async Task<ActionResult<AuthResponse>> DemoStoreMerchant(DemoStoreMerchantRequest req)
        => Ok(await _auth.DemoStoreMerchantAsync(req.StoreId));

    /// <summary>Rotate a refresh token for a fresh token pair.</summary>
    [HttpPost("refresh")]
    public async Task<ActionResult<AuthResponse>> Refresh(RefreshRequest req)
        => Ok(await _auth.RefreshAsync(req.RefreshToken));

    /// <summary>Revoke a refresh token.</summary>
    [HttpPost("logout")]
    public async Task<IActionResult> Logout(RefreshRequest req)
    {
        await _auth.LogoutAsync(req.RefreshToken);
        return NoContent();
    }

    /// <summary>The authenticated user.</summary>
    [HttpGet("me")]
    [Authorize]
    public async Task<ActionResult<UserSummary>> Me()
    {
        var user = await _auth.GetAsync(_me.Id);
        return user is null ? Unauthorized() : Ok(UserSummary.From(user));
    }
}
