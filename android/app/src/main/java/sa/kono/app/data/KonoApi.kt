package sa.kono.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sa.kono.app.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiError(message: String) : Exception(message) {
    class Offline : ApiError("تعذّر الاتصال بالخادم")
    class Unauthorized : ApiError("الجلسة منتهية")
    class Server(val status: Int, msg: String?) : ApiError(msg ?: "خطأ من الخادم")
    class Decoding(cause: Throwable) : ApiError("تعذّرت قراءة البيانات") { init { initCause(cause) } }
}

/**
 * Thin HTTP client for the Kono backend. Persists the JWT pair and transparently
 * refreshes on 401. Screens talk to [AppData], not to this directly.
 */
object KonoApi {
    val baseUrl: String = BuildConfig.API_BASE
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false; isLenient = true }

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private lateinit var prefs: android.content.SharedPreferences
    private val refreshLock = Mutex()
    @Volatile private var accessToken: String? = null
    @Volatile private var refreshToken: String? = null

    /** Loads persisted tokens. Call once from `Application`/`MainActivity` before any request. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("kono.session", Context.MODE_PRIVATE)
        accessToken = prefs.getString("access", null)
        refreshToken = prefs.getString("refresh", null)
    }

    val isAuthenticated get() = accessToken != null

    // ---- Auth ----

    suspend fun phoneLogin(phone: String, name: String?, role: AppRole?): AuthResponse {
        val res: AuthResponse = post("/api/auth/phone-login", PhoneLoginRequest(phone, name, role?.api), authed = false)
        apply(res)
        return res
    }

    fun logout() {
        accessToken = null; refreshToken = null
        prefs.edit().clear().apply()
    }

    private fun apply(res: AuthResponse) {
        accessToken = res.accessToken
        refreshToken = res.refreshToken
        prefs.edit().putString("access", res.accessToken).putString("refresh", res.refreshToken).apply()
    }

    private suspend fun refresh() {
        val rt = refreshToken ?: throw ApiError.Unauthorized()
        val res: AuthResponse = post("/api/auth/refresh", RefreshRequest(rt), authed = false)
        apply(res)
    }

    // ---- Generic ----

    suspend inline fun <reified T> get(path: String, query: Map<String, String> = emptyMap(), authed: Boolean = true): T =
        request("GET", path, null, query, authed, serializer<T>())

    suspend inline fun <reified B, reified T> post(path: String, body: B, authed: Boolean = true, method: String = "POST"): T =
        request(method, path, json.encodeToString(serializer<B>(), body), emptyMap(), authed, serializer<T>())

    suspend inline fun <reified T> post(path: String, authed: Boolean = true): T =
        request("POST", path, "{}", emptyMap(), authed, serializer<T>())

    /** A body-less request (DELETE, or a POST whose response we ignore). */
    suspend inline fun call(method: String, path: String) =
        request(method, path, "{}", emptyMap(), true, serializer<Unit>())

    @PublishedApi
    internal suspend fun <T> request(
        method: String, path: String, body: String?, query: Map<String, String>,
        authed: Boolean, deserializer: kotlinx.serialization.KSerializer<T>, isRetry: Boolean = false,
    ): T = withContext(Dispatchers.IO) {
        val url = okhttp3.HttpUrl.Builder().apply {
            val u = baseUrl.toHttpUrlOrThrow()
            scheme(u.scheme); host(u.host); port(u.port)
            path.trimStart('/').split('/').forEach { addPathSegment(it) }
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val req = Request.Builder().url(url)
            .method(method, if (method == "GET") null else (body ?: "{}").toRequestBody(jsonType))
            .apply { if (authed) accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()
        val (code, text) = try {
            client.newCall(req).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: IOException) {
            throw ApiError.Offline()
        }
        if (code == 401 && authed && !isRetry && refreshToken != null) {
            try { refreshLock.withLock { refresh() } } catch (e: ApiError) { logout(); throw ApiError.Unauthorized() }
            return@withContext request(method, path, body, query, authed, deserializer, isRetry = true)
        }
        if (code !in 200..299) {
            val title = runCatching { json.decodeFromString(ProblemDetails.serializer(), text).title }.getOrNull()
            throw ApiError.Server(code, title)
        }
        @Suppress("UNCHECKED_CAST")
        if (deserializer.descriptor.serialName == "kotlin.Unit") return@withContext Unit as T
        try { json.decodeFromString(deserializer, text) } catch (e: Exception) { throw ApiError.Decoding(e) }
    }

    private fun String.toHttpUrlOrThrow() = toHttpUrlOrNull() ?: error("bad base url")
}
