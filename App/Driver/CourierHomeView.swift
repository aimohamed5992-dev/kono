import SwiftUI
import MapKit

struct CourierHomeView: View {
    @Environment(\.courier) private var session

    @State private var camera: MapCameraPosition = .region(
        MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 24.774, longitude: 46.738),
                           span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)))
    @State private var pulse = false

    private static func ar(_ s: String) -> String {
        let d: [Character: Character] = ["0":"٠","1":"١","2":"٢","3":"٣","4":"٤",
                                         "5":"٥","6":"٦","7":"٧","8":"٨","9":"٩",".":"٫"]
        return String(s.map { d[$0] ?? $0 })
    }

    private var here: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: session.profile.latitude, longitude: session.profile.longitude)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Map(position: $camera, interactionModes: [.pan, .zoom]) {
                Annotation("أنت", coordinate: here) {
                    ZStack {
                        Circle().fill(KonoColor.info.opacity(0.25))
                            .frame(width: pulse ? 60 : 30, height: pulse ? 60 : 30)
                        Circle().fill(KonoColor.info).frame(width: 20, height: 20)
                            .overlay(Circle().strokeBorder(.white, lineWidth: 3))
                    }
                }
            }
            .ignoresSafeArea()
            .onAppear { withAnimation(KonoMotion.ambient) { pulse = true } }
            .task {
                await session.bootstrap()
                withAnimation(KonoMotion.spring) {
                    camera = .region(MKCoordinateRegion(
                        center: here,
                        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)))
                }
            }

            // top status
            VStack {
                HStack(spacing: KonoSpace.m) {
                    KonoMark(size: 38)
                    VStack(alignment: .leading, spacing: 1) {
                        Text(session.isOnline ? "متصل" : "غير متصل")
                            .font(KonoFont.headline)
                            .foregroundStyle(session.isOnline ? KonoColor.success : KonoColor.textSecondary)
                        Text(session.isOnline ? "نبحث لك عن رحلات قريبة" : "فعّل الاتصال لاستقبال الرحلات")
                            .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    }
                    Spacer()
                    miniStat("\(session.todayEarnings.clean)", "ر.س اليوم")
                }
                .padding(KonoSpace.m)
                .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: KonoRadius.l, style: .continuous))
                .konoShadow(.md)
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                Spacer()
            }

            // bottom control
            if let job = session.activeJob {
                ActiveJobPanel(job: job) { session.advanceJob() }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            } else if let offer = session.offer {
                JobOfferCard(job: offer,
                             onAccept: { session.accept() },
                             onDecline: { session.declineOffer() })
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                onlineToggle
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(KonoMotion.spring, value: session.isOnline)
        .animation(KonoMotion.spring, value: session.offer?.id)
        .animation(KonoMotion.spring, value: session.activeJob?.id)
    }

    private var onlineToggle: some View {
        VStack(spacing: KonoSpace.m) {
            HStack(spacing: KonoSpace.l) {
                summaryStat("\(session.todayTrips)", "رحلات", "bicycle")
                Divider().frame(height: 36)
                summaryStat(Self.ar(String(format: "%.1f", session.profile.todayDistanceKm)),
                            "كم", "location.fill")
                Divider().frame(height: 36)
                summaryStat("\(session.profile.acceptanceRate)٪", "قبول", "checkmark.circle.fill")
            }
            .padding(.vertical, KonoSpace.s)

            Button {
                #if canImport(UIKit)
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                #endif
                if session.isOnline { session.goOffline() } else { session.goOnline() }
            } label: {
                Text(session.isOnline ? "إيقاف الاتصال" : "بدء الاتصال")
                    .font(KonoFont.button)
                    .foregroundStyle(session.isOnline ? KonoColor.danger : KonoColor.textOnBrown)
                    .frame(maxWidth: .infinity).frame(height: KonoSize.buttonHeight)
                    .background(session.isOnline ? KonoColor.danger.opacity(0.12) : KonoColor.brown,
                                in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.xl, style: .continuous))
        .konoShadow(.lg)
        .konoScreenPadding()
        .padding(.bottom, 100)
    }

    private func miniStat(_ v: String, _ l: String) -> some View {
        VStack(alignment: .trailing, spacing: 0) {
            Text(v).font(.cairo(16, .heavy)).foregroundStyle(KonoColor.brown)
            Text(l).font(.cairo(10, .medium)).foregroundStyle(KonoColor.textSecondary)
        }
    }

    private func summaryStat(_ v: String, _ l: String, _ icon: String) -> some View {
        VStack(spacing: 3) {
            Image(systemName: icon).font(.system(size: 13)).foregroundStyle(KonoColor.caramel)
            Text(v).font(.cairo(15, .heavy)).foregroundStyle(KonoColor.textPrimary)
            Text(l).font(.cairo(10, .medium)).foregroundStyle(KonoColor.textSecondary)
        }
        .frame(maxWidth: .infinity)
    }
}
