import SwiftUI

/// Displays a named catalog image, and if that image is missing falls back to a
/// branded placeholder (soft gradient + glyph). This lets screens reference a
/// full photo library by name while the prototype only ships a subset.
struct AssetImage: View {
    let name: String
    var fallbackSymbol: String = "photo"
    var fallbackTint: Color = KonoColor.caramel
    var contentMode: ContentMode = .fill

    private var remoteURL: URL? {
        (name.hasPrefix("http://") || name.hasPrefix("https://")) ? URL(string: name) : nil
    }

    /// `data:image/...;base64,...` — e.g. a logo the merchant uploaded during onboarding.
    private var dataImage: UIImage? {
        guard name.hasPrefix("data:"), let comma = name.firstIndex(of: ","),
              let data = Data(base64Encoded: String(name[name.index(after: comma)...])) else { return nil }
        return UIImage(data: data)
    }

    /// A remote `/img/store_burger.jpg` also ships in the bundle as `store_burger` —
    /// use the local copy first (instant, offline).
    private var bundleName: String? {
        let n = remoteURL?.deletingPathExtension().lastPathComponent ?? name
        #if canImport(UIKit)
        return UIImage(named: n) != nil ? n : nil
        #else
        return nil
        #endif
    }

    var body: some View {
        GeometryReader { geo in
            Group {
                if let dataImage {
                    Image(uiImage: dataImage).resizable().aspectRatio(contentMode: contentMode)
                } else if let bundleName {
                    Image(bundleName).resizable().aspectRatio(contentMode: contentMode)
                } else if let remoteURL {
                    AsyncImage(url: remoteURL, transaction: .init(animation: .easeOut(duration: 0.25))) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable().aspectRatio(contentMode: contentMode)
                        case .failure:
                            placeholder(geo)
                        default:
                            KonoColor.surfaceSunken.shimmer()
                        }
                    }
                } else {
                    placeholder(geo)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .clipped()
        }
    }

    private func placeholder(_ geo: GeometryProxy) -> some View {
        ZStack {
            LinearGradient(
                colors: [fallbackTint.opacity(0.28), fallbackTint.opacity(0.14), KonoColor.cream],
                startPoint: .topLeading, endPoint: .bottomTrailing)
            Image(systemName: fallbackSymbol)
                .font(.system(size: min(geo.size.width, geo.size.height) * 0.3, weight: .semibold))
                .foregroundStyle(fallbackTint.opacity(0.55))
        }
    }
}

/// Round store/brand logo bubble. Falls back to a coloured monogram of the
/// store name — reads far better than a generic icon.
struct LogoBubble: View {
    let asset: String
    var symbol: String = "storefront.fill"
    var size: CGFloat = 48
    var name: String? = nil

    private var hasAsset: Bool {
        #if canImport(UIKit)
        UIImage(named: asset) != nil
        #else
        false
        #endif
    }

    private var monogramColor: Color {
        let palette: [Color] = [KonoColor.brown, KonoColor.caramel, KonoColor.vertical(.restaurants),
                                KonoColor.vertical(.groceries), KonoColor.vertical(.pharmacy),
                                KonoColor.vertical(.flowers)]
        let key = abs((name ?? asset).hashValue)
        return palette[key % palette.count]
    }

    var body: some View {
        Group {
            if hasAsset {
                AssetImage(name: asset, fallbackSymbol: symbol, fallbackTint: KonoColor.brown)
            } else if let name, let first = name.first {
                KonoColor.surface
                    .overlay(
                        Text(String(first))
                            .font(.cairo(size * 0.44, .heavy))
                            .foregroundStyle(monogramColor)
                    )
            } else {
                KonoColor.cream.overlay(
                    Image(systemName: symbol).font(.system(size: size * 0.4, weight: .semibold))
                        .foregroundStyle(KonoColor.brown.opacity(0.6)))
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(Circle().strokeBorder(KonoColor.hairline, lineWidth: 1))
        .konoShadow(.sm)
    }
}
