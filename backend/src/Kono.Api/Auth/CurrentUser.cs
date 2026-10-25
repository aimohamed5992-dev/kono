using System.Security.Claims;
using Kono.Api.Domain;

namespace Kono.Api.Auth;

/// <summary>Convenience accessor for the authenticated principal.</summary>
public class CurrentUser
{
    private readonly IHttpContextAccessor _accessor;
    public CurrentUser(IHttpContextAccessor accessor) => _accessor = accessor;

    private ClaimsPrincipal? Principal => _accessor.HttpContext?.User;

    public bool IsAuthenticated => Principal?.Identity?.IsAuthenticated ?? false;

    public int? IdOrNull =>
        int.TryParse(Principal?.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;

    public int Id => IdOrNull ?? throw new UnauthorizedAccessException("No authenticated user.");

    public UserRole Role =>
        Enum.TryParse<UserRole>(Principal?.FindFirstValue(ClaimTypes.Role), out var r) ? r : UserRole.Customer;

    public string? Phone => Principal?.FindFirst("phone")?.Value;

    public bool InRole(UserRole role) => Role == role;
}
