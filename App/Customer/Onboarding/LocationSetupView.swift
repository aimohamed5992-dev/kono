import SwiftUI
import MapKit

struct LocationSetupView: View {
    var onConfirm: (Address) -> Void
    var onBack: (() -> Void)? = nil

    @State private var camera: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 24.7136, longitude: 46.6753),
            span: MKCoordinateSpan(latitudeDelta: 0.012, longitudeDelta: 0.012)))
    @State private var moving = false
    @State private var appear = false
    @State private var selectedLabel = "المنزل"
    @State private var detail = ""
    @State private var currentCenter = CLLocationCoordinate2D(latitude: 24.7136, longitude: 46.6753)
    @State private var district = "جارٍ تحديد الموقع…"
    @State private var streetLine = ""
    @State private var geocodeTask: Task<Void, Never>?
    private let geocoder = CLGeocoder()

    private let labels: [(String, String)] = [
        ("المنزل", "house.fill"), ("العمل", "briefcase.fill"), ("آخر", "mappin.circle.fill")
    ]

    /// Real reverse-geocode of the map centre (Apple CLGeocoder, Arabic locale).
    private func reverseGeocode() {
        geocodeTask?.cancel()
        let coord = currentCenter
        geocodeTask = Task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            guard !Task.isCancelled else { return }
            let loc = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
            let places = try? await geocoder.reverseGeocodeLocation(
                loc, preferredLocale: Locale(identifier: "ar_SA"))
            guard !Task.isCancelled, let p = places?.first else { return }
            let area = p.subLocality ?? p.locality ?? "موقع محدد"
            let street = [p.thoroughfare, p.subThoroughfare].compactMap { $0 }.joined(separator: " ")
            let city = p.locality ?? p.administrativeArea ?? "الرياض"
            withAnimation(KonoMotion.ease) {
                district = area
                streetLine = [street.isEmpty ? nil : street, city].compactMap { $0 }.joined(separator: "، ")
            }
        }
    }
    private var resolvedCity: String {
        streetLine.split(separator: "،").last.map { String($0).trimmingCharacters(in: .whitespaces) } ?? "الرياض"
    }

    var body: some View {
        ZStack(alignment: .top) {
            MapReader { _ in
                Map(position: $camera, interactionModes: [.pan, .zoom])
                    .onMapCameraChange(frequency: .continuous) { ctx in
                        currentCenter = ctx.camera.centerCoordinate
                        if !moving { withAnimation(KonoMotion.ease) { moving = true } }
                    }
                    .onMapCameraChange(frequency: .onEnd) { ctx in
                        currentCenter = ctx.region.center
                        withAnimation(KonoMotion.spring) { moving = false }
                        reverseGeocode()
                    }
                    .ignoresSafeArea()
            }

            // center pin (stays fixed while the map moves under it)
            CenterPin(lifted: moving)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .allowsHitTesting(false)
                .offset(y: -18)

            // top bar
            HStack {
                if let onBack {
                    circleButton("chevron.right", action: onBack)
                }
                Spacer()
                Text("تحديد موقع التوصيل")
                    .font(KonoFont.headline)
                    .foregroundStyle(KonoColor.textPrimary)
                    .padding(.horizontal, KonoSpace.l)
                    .frame(height: 40)
                    .background(.ultraThinMaterial, in: Capsule())
                Spacer()
                circleButton("location.fill") {
                    withAnimation(KonoMotion.spring) {
                        camera = .region(MKCoordinateRegion(
                            center: CLLocationCoordinate2D(latitude: 24.7742, longitude: 46.7386),
                            span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)))
                    }
                }
            }
            .konoScreenPadding()
            .padding(.top, KonoSpace.s)

            // bottom sheet
            VStack {
                Spacer()
                sheet
            }
        }
        .onAppear {
            withAnimation(KonoMotion.spring) { appear = true }
            reverseGeocode()
        }
    }

    private var sheet: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            Capsule().fill(KonoColor.divider).frame(width: 40, height: 4)
                .frame(maxWidth: .infinity)

            HStack(spacing: KonoSpace.m) {
                ZStack {
                    Circle().fill(KonoColor.yellow.opacity(0.25)).frame(width: 44, height: 44)
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(KonoColor.brown)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(district).font(KonoFont.headline).foregroundStyle(KonoColor.textPrimary)
                    Text(streetLine).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        .lineLimit(1)
                }
                Spacer()
            }

            HStack(spacing: KonoSpace.s) {
                ForEach(labels, id: \.0) { label, icon in
                    KonoChip(title: label, icon: icon, selected: selectedLabel == label) {
                        withAnimation(KonoMotion.snappy) { selectedLabel = label }
                    }
                }
            }

            KonoTextField(placeholder: "تفاصيل إضافية (رقم الشقة، علامة مميزة)",
                          text: $detail, icon: "text.alignright")

            KonoButton(title: "تأكيد الموقع", icon: "checkmark") {
                let line = detail.isEmpty ? (streetLine.isEmpty ? district : streetLine)
                                          : "\(streetLine.isEmpty ? district : streetLine) · \(detail)"
                let a = Address(labelAr: selectedLabel, lineAr: line, cityAr: resolvedCity,
                                icon: labels.first { $0.0 == selectedLabel }?.1 ?? "mappin",
                                latitude: currentCenter.latitude, longitude: currentCenter.longitude,
                                isDefault: true)
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
                onConfirm(a)
            }
        }
        .padding(KonoSpace.l)
        .padding(.bottom, KonoSpace.s)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.xl, style: .continuous))
        .konoShadow(.lg)
        .offset(y: appear ? 0 : 320)
    }

    private func circleButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(KonoColor.brown)
                .frame(width: 40, height: 40)
                .background(KonoColor.surface, in: Circle())
                .konoShadow(.md)
        }
        .buttonStyle(.plain)
    }
}

private struct CenterPin: View {
    let lifted: Bool
    var body: some View {
        VStack(spacing: 2) {
            ZStack {
                Circle().fill(KonoColor.brown).frame(width: 34, height: 34)
                Image(systemName: "fork.knife")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(KonoColor.yellow)
            }
            .konoShadow(.md)
            Triangle().fill(KonoColor.brown)
                .frame(width: 12, height: 8)
            Ellipse()
                .fill(KonoColor.brown.opacity(lifted ? 0.15 : 0.28))
                .frame(width: lifted ? 18 : 12, height: 5)
                .offset(y: 2)
        }
        .offset(y: lifted ? -10 : 0)
        .animation(KonoMotion.snappy, value: lifted)
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        p.closeSubpath()
        return p
    }
}

#Preview { LocationSetupView { _ in } }
