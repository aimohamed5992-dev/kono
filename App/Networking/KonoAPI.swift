import Foundation

/// Thin async HTTP client for the Kono backend. Handles JWT storage and
/// transparent refresh; every screen talks to `AppData`, not to this directly.
actor KonoAPI {
    static let shared = KonoAPI()

    /// Override with the `KONO_API` launch env var, else localhost.
    nonisolated static var baseURL: URL {
        if let s = ProcessInfo.processInfo.environment["KONO_API"], let u = URL(string: s) { return u }
        return URL(string: "http://localhost:5210")!
    }

    private var accessToken: String?
    private var refreshToken: String?
    private(set) var currentUser: AuthDTO.User?
    private var restored = false

    private let session: URLSession = {
        let c = URLSessionConfiguration.default
        c.timeoutIntervalForRequest = 12
        c.waitsForConnectivity = false
        return URLSession(configuration: c)
    }()

    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601withFractional
        return d
    }()

    var isAuthenticated: Bool { accessToken != nil }

    /// Loads any persisted tokens from the Keychain into memory. Cheap, idempotent —
    /// call once on launch before the first authed request.
    func restore() {
        guard !restored else { return }
        restored = true
        accessToken = Keychain.get("access")
        refreshToken = Keychain.get("refresh")
    }

    // MARK: Auth

    @discardableResult
    func login(phone: String, password: String) async throws -> AuthDTO.Response {
        let res: AuthDTO.Response = try await send(
            "/api/auth/login", method: "POST",
            body: ["phone": phone, "password": password], authed: false)
        apply(res)
        return res
    }

    @discardableResult
    func register(phone: String, name: String, password: String, role: String = "Customer")
        async throws -> AuthDTO.Response
    {
        let res: AuthDTO.Response = try await send(
            "/api/auth/register", method: "POST",
            body: ["phone": phone, "name": name, "password": password, "role": role], authed: false)
        apply(res)
        return res
    }

    @discardableResult
    func phoneLogin(phone: String, name: String? = nil, role: String? = nil) async throws -> AuthDTO.Response {
        var body: [String: Any] = ["phone": phone]
        if let name { body["name"] = name }
        if let role { body["role"] = role }
        let res: AuthDTO.Response = try await send(
            "/api/auth/phone-login", method: "POST", body: body, authed: false)
        apply(res)
        return res
    }

    /// Demo helper — sign in as the merchant that owns `storeId`.
    @discardableResult
    func demoStoreMerchant(storeId: Int) async throws -> AuthDTO.Response {
        let res: AuthDTO.Response = try await send(
            "/api/auth/demo-store-merchant", method: "POST",
            body: ["storeId": storeId], authed: false)
        apply(res)
        return res
    }

    func logout() {
        accessToken = nil; refreshToken = nil; currentUser = nil
        Keychain.clear()
    }

    private func apply(_ res: AuthDTO.Response) {
        accessToken = res.accessToken
        refreshToken = res.refreshToken
        currentUser = res.user
        Keychain.set(res.accessToken, for: "access")
        Keychain.set(res.refreshToken, for: "refresh")
    }

    private func refresh() async throws {
        guard let rt = refreshToken else { throw APIError.unauthorized }
        let res: AuthDTO.Response = try await send(
            "/api/auth/refresh", method: "POST", body: ["refreshToken": rt], authed: false)
        apply(res)
    }

    // MARK: Generic requests

    func get<T: Decodable>(_ path: String, query: [String: String] = [:], authed: Bool = true) async throws -> T {
        try await send(path, method: "GET", query: query, authed: authed)
    }

    func post<T: Decodable>(_ path: String, body: Encodable? = nil, authed: Bool = true,
                            method: String = "POST") async throws -> T {
        try await send(path, method: method, encodable: body, authed: authed)
    }

    /// A request with no meaningful response body.
    func postVoid(_ path: String, body: Encodable? = nil, authed: Bool = true,
                  method: String = "POST") async throws {
        let _: EmptyResponse = try await send(path, method: method, encodable: body, authed: authed)
    }

    // MARK: Core

    private func send<T: Decodable>(
        _ path: String, method: String,
        query: [String: String] = [:],
        body: [String: Any]? = nil,
        encodable: Encodable? = nil,
        authed: Bool = true,
        isRetry: Bool = false
    ) async throws -> T {
        var comps = URLComponents(url: Self.baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if !query.isEmpty {
            comps.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        var req = URLRequest(url: comps.url!)
        req.httpMethod = method
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if authed, let token = accessToken {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            req.httpBody = try JSONSerialization.data(withJSONObject: body)
        } else if let encodable {
            req.httpBody = try JSONEncoder().encode(AnyEncodable(encodable))
        }

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await session.data(for: req)
        } catch {
            throw APIError.offline
        }
        guard let http = response as? HTTPURLResponse else { throw APIError.badResponse }

        if http.statusCode == 401, authed, !isRetry, refreshToken != nil {
            do { try await refresh() }
            catch { logout(); throw APIError.unauthorized }
            return try await send(path, method: method, query: query, body: body,
                                  encodable: encodable, authed: authed, isRetry: true)
        }
        guard (200..<300).contains(http.statusCode) else {
            let title = (try? JSONDecoder().decode(ProblemDetails.self, from: data))?.title
            throw APIError.server(status: http.statusCode, message: title)
        }

        if T.self == EmptyResponse.self { return EmptyResponse() as! T }
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decoding(error)
        }
    }
}

// MARK: - Support

struct EmptyResponse: Decodable {}

struct ProblemDetails: Decodable { let title: String? }

enum APIError: LocalizedError {
    case offline, badResponse, unauthorized
    case server(status: Int, message: String?)
    case decoding(Error)

    var errorDescription: String? {
        switch self {
        case .offline: return "تعذّر الاتصال بالخادم"
        case .badResponse: return "استجابة غير صحيحة"
        case .unauthorized: return "الجلسة منتهية"
        case .server(_, let m): return m ?? "خطأ من الخادم"
        case .decoding: return "تعذّرت قراءة البيانات"
        }
    }
}

struct AnyEncodable: Encodable {
    private let encode: (Encoder) throws -> Void
    init(_ wrapped: Encodable) { encode = wrapped.encode }
    func encode(to encoder: Encoder) throws { try encode(encoder) }
}

extension JSONDecoder.DateDecodingStrategy {
    static let iso8601withFractional = custom { decoder in
        let s = try decoder.singleValueContainer().decode(String.self)
        let fmts = [ISO8601DateFormatter.Options([.withInternetDateTime, .withFractionalSeconds]),
                    ISO8601DateFormatter.Options([.withInternetDateTime])]
        for opts in fmts {
            let f = ISO8601DateFormatter(); f.formatOptions = opts
            if let d = f.date(from: s) { return d }
        }
        return Date()
    }
}
