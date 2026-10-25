namespace Kono.Api.Infrastructure;

/// <summary>Turns stored relative image paths (<c>/img/x.jpg</c>) into absolute
/// URLs against the current request host, so any client can load them directly.</summary>
public class ImageUrlBuilder
{
    private readonly string _base;

    public ImageUrlBuilder(IHttpContextAccessor accessor, IConfiguration config)
    {
        var configured = config["App:PublicBaseUrl"];
        if (!string.IsNullOrWhiteSpace(configured))
        {
            _base = configured.TrimEnd('/');
        }
        else
        {
            var req = accessor.HttpContext?.Request;
            _base = req is null ? "" : $"{req.Scheme}://{req.Host}";
        }
    }

    public string Resolve(string? path)
    {
        if (string.IsNullOrWhiteSpace(path)) return "";
        if (path.StartsWith("http://") || path.StartsWith("https://") || path.StartsWith("data:")) return path;
        return $"{_base}/{path.TrimStart('/')}";
    }

    public Func<string, string> Func => Resolve!;
}
