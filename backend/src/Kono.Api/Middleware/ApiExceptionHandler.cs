using Kono.Api.Services;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;

namespace Kono.Api.Middleware;

/// <summary>Turns known exceptions into ProblemDetails responses.</summary>
public class ApiExceptionHandler : IExceptionHandler
{
    private readonly ILogger<ApiExceptionHandler> _log;
    public ApiExceptionHandler(ILogger<ApiExceptionHandler> log) => _log = log;

    public async ValueTask<bool> TryHandleAsync(HttpContext ctx, Exception ex, CancellationToken ct)
    {
        var (status, title) = ex switch
        {
            AuthException a => (a.StatusCode, a.Message),
            KeyNotFoundException => (404, "المورد غير موجود."),
            UnauthorizedAccessException => (401, "غير مصرّح."),
            InvalidOperationException io when io.Message.Length < 200 => (400, io.Message),
            _ => (500, "حدث خطأ غير متوقع.")
        };

        if (status == 500) _log.LogError(ex, "Unhandled exception");

        var problem = new ProblemDetails
        {
            Status = status,
            Title = title,
            Type = $"https://httpstatuses.com/{status}"
        };
        ctx.Response.StatusCode = status;
        await ctx.Response.WriteAsJsonAsync(problem, ct);
        return true;
    }
}
