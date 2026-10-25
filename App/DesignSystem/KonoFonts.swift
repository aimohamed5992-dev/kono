import SwiftUI
import CoreText

/// Cairo is the Kono brand face (Arabic-first, with a matching Latin set for the
/// English locale). The `.ttf` files ship in `App/Resources/Fonts/`. We register
/// them from the bundle at launch so no `Info.plist` `UIAppFonts` entry is needed
/// (the project generates its Info.plist).
enum KonoFonts {

    /// PostScript names — what `Font.custom(_:size:)` resolves against.
    enum Face: String, CaseIterable {
        case regular   = "Cairo-Regular"
        case medium    = "Cairo-Medium"
        case semibold  = "Cairo-SemiBold"
        case bold      = "Cairo-Bold"
        case extraBold = "Cairo-ExtraBold"
        case black     = "Cairo-Black"

        static func name(for weight: Font.Weight) -> String {
            switch weight {
            case .black:              return Face.black.rawValue
            case .heavy:              return Face.extraBold.rawValue
            case .bold:               return Face.bold.rawValue
            case .semibold:           return Face.semibold.rawValue
            case .medium:             return Face.medium.rawValue
            case .light, .thin, .ultraLight: return Face.regular.rawValue
            default:                  return Face.regular.rawValue
            }
        }
    }

    private static var didRegister = false

    static func register() {
        guard !didRegister else { return }
        didRegister = true
        for face in Face.allCases {
            guard let url = Bundle.main.url(forResource: face.rawValue, withExtension: "ttf") else {
                #if DEBUG
                print("⚠️ Kono: missing font \(face.rawValue).ttf in bundle")
                #endif
                continue
            }
            var err: Unmanaged<CFError>?
            if !CTFontManagerRegisterFontsForURL(url as CFURL, .process, &err) {
                #if DEBUG
                print("⚠️ Kono: could not register \(face.rawValue): \(String(describing: err))")
                #endif
            }
        }
    }
}

extension Font {
    /// Brand face at an explicit point size + weight, scaling with Dynamic Type
    /// relative to the given text style.
    static func cairo(_ size: CGFloat,
                      _ weight: Font.Weight = .regular,
                      relativeTo textStyle: Font.TextStyle = .body) -> Font {
        .custom(KonoFonts.Face.name(for: weight), size: size, relativeTo: textStyle)
    }

    /// Non-scaling brand face — for tab bars, badges and other fixed chrome.
    static func cairoFixed(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .custom(KonoFonts.Face.name(for: weight), fixedSize: size)
    }
}

extension UIFont {
    /// UIKit brand face — used where SwiftUI `Font` can't reach (nav-bar titles,
    /// `UITextView`, etc.). Falls back to the system face if Cairo isn't loaded.
    static func cairo(_ size: CGFloat, weight: UIFont.Weight = .regular) -> UIFont {
        let name: String
        switch weight {
        case .black:                       name = "Cairo-Black"
        case .heavy:                       name = "Cairo-ExtraBold"
        case .bold:                        name = "Cairo-Bold"
        case .semibold:                    name = "Cairo-SemiBold"
        case .medium:                      name = "Cairo-Medium"
        default:                           name = "Cairo-Regular"
        }
        return UIFont(name: name, size: size) ?? .systemFont(ofSize: size, weight: weight)
    }
}
