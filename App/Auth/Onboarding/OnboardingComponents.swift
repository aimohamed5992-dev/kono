import SwiftUI
import PhotosUI
import MapKit

/// Shared building blocks for the merchant / courier onboarding wizards.
enum OB {

    // MARK: step progress

    struct Progress: View {
        let step: Int
        let total: Int
        var body: some View {
            HStack(spacing: 5) {
                ForEach(0..<total, id: \.self) { i in
                    Capsule()
                        .fill(i <= step ? KonoColor.brown : KonoColor.brown.opacity(0.18))
                        .frame(height: 5)
                }
            }
            .animation(KonoMotion.snappy, value: step)
        }
    }

    // MARK: section card

    struct Section<C: View>: View {
        let title: String
        var subtitle: String? = nil
        @ViewBuilder var content: () -> C
        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.m) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                    if let subtitle {
                        Text(subtitle).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    }
                }
                content()
            }
        }
    }

    // MARK: text field

    struct Field: View {
        let label: String
        @Binding var text: String
        var required = false
        var icon: String? = nil
        var keyboard: UIKeyboardType = .default
        var hint: String? = nil

        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.xs) {
                labelRow(label, required)
                KonoTextField(placeholder: hint ?? label, text: $text, icon: icon, keyboard: keyboard)
            }
        }
    }

    // MARK: menu / picker

    struct Picker<T: Hashable>: View {
        let label: String
        @Binding var selection: T
        let options: [(value: T, title: String)]
        var required = false

        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.xs) {
                labelRow(label, required)
                Menu {
                    ForEach(options, id: \.value) { opt in
                        Button(opt.title) { selection = opt.value }
                    }
                } label: {
                    HStack {
                        Text(options.first { $0.value == selection }?.title ?? "اختر")
                            .foregroundStyle(KonoColor.textPrimary)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down").font(.system(size: 12))
                            .foregroundStyle(KonoColor.textTertiary)
                    }
                    .font(KonoFont.body)
                    .padding(.horizontal, KonoSpace.l)
                    .frame(height: KonoSize.fieldHeight)
                    .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                        .strokeBorder(KonoColor.hairline, lineWidth: 1))
                }
            }
        }
    }

    // MARK: date

    enum DateKind { case birth, expiry }

    struct DateField: View {
        let label: String
        @Binding var value: String                 // yyyy-MM-dd
        var required = false
        var kind: DateKind = .birth

        @State private var date = Date()
        private static let fmt: DateFormatter = {
            let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian)
            f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"; return f
        }()

        private var range: ClosedRange<Date> {
            let cal = Calendar(identifier: .gregorian)
            let now = Date()
            switch kind {
            case .birth:
                let oldest = cal.date(byAdding: .year, value: -75, to: now)!
                let youngest = cal.date(byAdding: .year, value: -18, to: now)!
                return oldest...youngest
            case .expiry:
                let far = cal.date(byAdding: .year, value: 10, to: now)!
                return now...far
            }
        }

        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.xs) {
                labelRow(label, required)
                DatePicker("", selection: $date, in: range, displayedComponents: .date)
                    .labelsHidden()
                    .datePickerStyle(.compact)
                    .environment(\.locale, Locale(identifier: "ar_SA"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, KonoSpace.l)
                    .frame(height: KonoSize.fieldHeight)
                    .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                        .strokeBorder(KonoColor.hairline, lineWidth: 1))
                    .onAppear {
                        if let d = Self.fmt.date(from: value) {
                            date = d
                        } else {
                            let cal = Calendar(identifier: .gregorian)
                            date = kind == .birth
                                ? cal.date(byAdding: .year, value: -28, to: Date())!
                                : cal.date(byAdding: .year, value: 1, to: Date())!
                            value = Self.fmt.string(from: date)
                        }
                    }
                    .onChange(of: date) { _, d in value = Self.fmt.string(from: d) }
            }
        }
    }

    // MARK: document row

    struct DocRow: View {
        @Binding var doc: OnboardingDoc
        @State private var pick: PhotosPickerItem?

        var body: some View {
            HStack(spacing: KonoSpace.m) {
                Image(systemName: doc.provided ? "checkmark.seal.fill" : "doc.text")
                    .font(.system(size: 17))
                    .foregroundStyle(doc.provided ? KonoColor.success : KonoColor.brown)
                    .frame(width: 26)
                VStack(alignment: .leading, spacing: 1) {
                    Text(doc.labelAr.k).font(KonoFont.callout).foregroundStyle(KonoColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(doc.provided ? "تم الإرفاق" : (doc.required ? "مطلوب" : "اختياري"))
                        .font(KonoFont.caption)
                        .foregroundStyle(doc.provided ? KonoColor.success
                                         : (doc.required ? KonoColor.danger : KonoColor.textTertiary))
                }
                Spacer()
                PhotosPicker(selection: $pick, matching: .images) {
                    Text(doc.provided ? "تغيير" : "إرفاق")
                        .font(KonoFont.caption.weight(.bold))
                        .foregroundStyle(KonoColor.textOnBrown)
                        .padding(.horizontal, KonoSpace.m).padding(.vertical, 7)
                        .background(KonoColor.brown, in: Capsule())
                }
            }
            .padding(.vertical, KonoSpace.s)
            .onChange(of: pick) { _, item in
                guard item != nil else { return }
                doc.provided = true
                if doc.fileName.isEmpty { doc.fileName = "\(doc.type).jpg" }
            }
        }
    }

    // MARK: logo / photo picker

    struct ImageCard: View {
        let title: String
        var required = false
        @Binding var dataUri: String?
        @State private var pick: PhotosPickerItem?
        @State private var preview: Image?

        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.xs) {
                labelRow(title, required)
                PhotosPicker(selection: $pick, matching: .images) {
                    ZStack {
                        if let preview {
                            preview.resizable().scaledToFill()
                        } else {
                            VStack(spacing: 6) {
                                Image(systemName: "photo.badge.plus").font(.system(size: 26))
                                Text("اختر صورة").font(KonoFont.footnote)
                            }
                            .foregroundStyle(KonoColor.brown.opacity(0.7))
                        }
                    }
                    .frame(maxWidth: .infinity).frame(height: 140)
                    .background(KonoColor.cream)
                    .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                        .strokeBorder(KonoColor.hairline, lineWidth: 1))
                }
            }
            .onChange(of: pick) { _, item in
                guard let item else { return }
                Task {
                    guard let data = try? await item.loadTransferable(type: Data.self),
                          let ui = UIImage(data: data) else { return }
                    let small = ui.downscaled(to: 512)
                    if let jpeg = small.jpegData(compressionQuality: 0.7) {
                        dataUri = "data:image/jpeg;base64," + jpeg.base64EncodedString()
                        preview = Image(uiImage: small)
                    }
                }
            }
        }
    }

    // MARK: compact map pin picker

    struct MapPicker: View {
        let label: String
        var required = false
        @Binding var latitude: Double?
        @Binding var longitude: Double?

        @State private var camera: MapCameraPosition = .region(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 24.7136, longitude: 46.6753),
            span: MKCoordinateSpan(latitudeDelta: 0.03, longitudeDelta: 0.03)))
        @State private var center = CLLocationCoordinate2D(latitude: 24.7136, longitude: 46.6753)

        var body: some View {
            VStack(alignment: .leading, spacing: KonoSpace.xs) {
                labelRow(label, required)
                ZStack {
                    Map(position: $camera, interactionModes: [.pan, .zoom])
                        .onMapCameraChange(frequency: .onEnd) { ctx in center = ctx.region.center }
                    Image(systemName: "mappin.circle.fill")
                        .font(.system(size: 30)).foregroundStyle(KonoColor.brown)
                        .background(Circle().fill(.white).frame(width: 14, height: 14).offset(y: 2))
                        .shadow(radius: 3, y: 1)
                        .allowsHitTesting(false)
                }
                .frame(height: 200)
                .clipShape(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous)
                    .strokeBorder(KonoColor.hairline, lineWidth: 1))

                Button {
                    latitude = center.latitude
                    longitude = center.longitude
                    #if canImport(UIKit)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    #endif
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: latitude == nil ? "mappin.and.ellipse" : "checkmark.circle.fill")
                        Text(latitude == nil ? "تثبيت موقع المتجر على الدبوس" : "تم تثبيت الموقع — اضغط للتحديث")
                    }
                    .font(KonoFont.footnote.weight(.semibold))
                    .foregroundStyle(latitude == nil ? KonoColor.brown : KonoColor.success)
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: consent

    struct Consent: View {
        @Binding var agreed: Bool
        var body: some View {
            Button { agreed.toggle() } label: {
                HStack(alignment: .top, spacing: KonoSpace.s) {
                    Image(systemName: agreed ? "checkmark.square.fill" : "square")
                        .font(.system(size: 20)).foregroundStyle(agreed ? KonoColor.brown : KonoColor.textTertiary)
                    Text("أقرّ بصحة جميع البيانات والوثائق المقدّمة، وأوافق على اتفاقية الشراكة وسياسة الخصوصية لمنصة كونو.")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        .multilineTextAlignment(.leading)
                    Spacer(minLength: 0)
                }
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: helpers

    @ViewBuilder
    static func labelRow(_ label: String, _ required: Bool) -> some View {
        HStack(spacing: 3) {
            Text(label).font(KonoFont.subheadline).foregroundStyle(KonoColor.textSecondary)
            if required { Text("*").font(KonoFont.subheadline).foregroundStyle(KonoColor.danger) }
        }
    }
}

private func labelRow(_ label: String, _ required: Bool) -> some View { OB.labelRow(label, required) }

/// A pill showing the merchant / courier application state.
struct OnboardingStatusChip: View {
    let status: String
    var hidden: Bool { status.isEmpty || status == "approved" }

    var body: some View {
        if !hidden {
            let (text, color, icon) = style
            HStack(spacing: 5) {
                Image(systemName: icon).font(.system(size: 11, weight: .bold))
                Text(text).font(.cairo(12, .bold))
            }
            .foregroundStyle(color)
            .padding(.horizontal, KonoSpace.s).padding(.vertical, 4)
            .background(color.opacity(0.14), in: Capsule())
        }
    }

    private var style: (String, Color, String) {
        switch status {
        case "under_review": return ("قيد المراجعة", KonoColor.caramel, "clock.fill")
        case "rejected":     return ("مرفوض — راجع البيانات", KonoColor.danger, "exclamationmark.triangle.fill")
        default:             return ("غير مكتمل", KonoColor.textTertiary, "circle.dashed")
        }
    }
}

extension UIImage {
    func downscaled(to maxDim: CGFloat) -> UIImage {
        let m = max(size.width, size.height)
        guard m > maxDim else { return self }
        let s = maxDim / m
        let newSize = CGSize(width: size.width * s, height: size.height * s)
        let r = UIGraphicsImageRenderer(size: newSize)
        return r.image { _ in draw(in: CGRect(origin: .zero, size: newSize)) }
    }
}
