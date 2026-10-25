namespace Kono.Api.Auth;

public class JwtOptions
{
    public const string Section = "Jwt";

    public string Issuer { get; set; } = "kono-api";
    public string Audience { get; set; } = "kono-clients";
    public string Key { get; set; } = "";
    public int AccessTokenMinutes { get; set; } = 120;
    public int RefreshTokenDays { get; set; } = 30;
}
