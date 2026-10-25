import SwiftUI

struct JobOfferCard: View {
    let job: CourierJob
    var onAccept: () -> Void
    var onDecline: () -> Void

    @State private var secondsLeft = 15
    private let tick = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: KonoSpace.l) {
            HStack {
                KonoTag(text: "رحلة جديدة", systemImage: "bell.fill", color: KonoColor.brown, filled: true)
                Spacer()
                Text("\(secondsLeft) ثانية").font(KonoFont.subheadline)
                    .foregroundStyle(secondsLeft < 6 ? KonoColor.danger : KonoColor.textSecondary)
            }

            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    RiyalText(amount: job.payout, font: KonoFont.display(30))
                    Text("العائد المتوقع").font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Label("\(job.distanceKm.clean) كم", systemImage: "location")
                    Label("\(job.estMinutes) دقيقة", systemImage: "clock")
                    Label("\(job.itemCount) صنف", systemImage: "bag")
                }
                .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                .labelStyle(.titleAndIcon)
            }

            VStack(spacing: 0) {
                routeRow("bag.fill", KonoColor.caramel, "استلام", job.storeNameAr, job.storeAddressAr)
                HStack { Rectangle().fill(KonoColor.divider).frame(width: 2, height: 18)
                    .padding(.leading, 15); Spacer() }
                routeRow("house.fill", KonoColor.brown, "تسليم", job.customerNameAr, job.dropoffAddressAr)
            }

            HStack(spacing: KonoSpace.m) {
                Button(action: onDecline) {
                    Text("تجاهل").font(KonoFont.button).foregroundStyle(KonoColor.textSecondary)
                        .frame(width: 90, height: KonoSize.buttonHeight)
                        .background(KonoColor.surfaceSunken, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                }
                .buttonStyle(.plain)
                Button {
                    #if canImport(UIKit)
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    #endif
                    onAccept()
                } label: {
                    Text("قبول الرحلة").font(KonoFont.button).foregroundStyle(KonoColor.textOnBrown)
                        .frame(maxWidth: .infinity).frame(height: KonoSize.buttonHeight)
                        .background(KonoColor.brown, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.xl, style: .continuous))
        .konoShadow(.lg)
        .overlay(alignment: .top) {
            GeometryReader { geo in
                Rectangle().fill(KonoColor.caramel)
                    .frame(width: geo.size.width * CGFloat(secondsLeft) / 15, height: 4)
            }
            .frame(height: 4)
        }
        .konoScreenPadding()
        .padding(.bottom, 100)
        .onReceive(tick) { _ in
            if secondsLeft > 0 { secondsLeft -= 1 } else { onDecline() }
        }
    }

    private func routeRow(_ icon: String, _ tint: Color, _ label: String,
                          _ name: String, _ addr: String) -> some View {
        HStack(alignment: .top, spacing: KonoSpace.m) {
            Image(systemName: icon).font(.system(size: 13, weight: .bold)).foregroundStyle(.white)
                .frame(width: 32, height: 32).background(tint, in: Circle())
            VStack(alignment: .leading, spacing: 1) {
                Text(label).font(KonoFont.caption).foregroundStyle(KonoColor.textTertiary)
                Text(name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary).lineLimit(1)
                Text(addr).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary).lineLimit(1)
            }
            Spacer()
        }
    }
}

struct ActiveJobPanel: View {
    let job: CourierJob
    var onAdvance: () -> Void
    @State private var expanded = true
    @State private var showDetail = false

    var body: some View {
        VStack(spacing: KonoSpace.m) {
            Capsule().fill(KonoColor.divider).frame(width: 40, height: 4)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(job.stage.titleAr.k).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                        .contentTransition(.opacity)
                    Text(destinationLine).font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        .lineLimit(1)
                }
                Spacer()
                if let maps = navURL {
                    Link(destination: maps) {
                        Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                            .font(.system(size: 16, weight: .bold)).foregroundStyle(KonoColor.textOnBrown)
                            .frame(width: 44, height: 44).background(KonoColor.info, in: Circle())
                    }
                    .buttonStyle(.plain)
                }
            }

            // stage dots
            HStack(spacing: 4) {
                ForEach(JobStage.allCases.dropFirst(), id: \.rawValue) { s in
                    Capsule()
                        .fill(s.rawValue <= job.stage.rawValue ? KonoColor.brown : KonoColor.divider)
                        .frame(height: 5)
                }
            }

            HStack(spacing: KonoSpace.m) {
                infoChip("\(job.itemCount) صنف", "bag")
                infoChip("\(job.distanceKm.clean) كم", "location")
                infoChip("\(job.payout.clean) ر.س", "banknote")
                Spacer()
                if let tel = phoneURL {
                    Link(destination: tel) {
                        Image(systemName: "phone.fill").font(.system(size: 14, weight: .bold))
                            .foregroundStyle(KonoColor.textOnBrown)
                            .frame(width: 38, height: 38).background(KonoColor.brown, in: Circle())
                    }
                    .buttonStyle(.plain)
                }
            }

            if job.apiId != 0 {
                OrderChatButtons(orderId: job.apiId, compact: true)
                Button { showDetail = true } label: {
                    Label("تفاصيل الطلب كاملة", systemImage: "doc.text.magnifyingglass")
                        .font(KonoFont.subheadline.weight(.semibold))
                        .foregroundStyle(KonoColor.brown)
                        .frame(maxWidth: .infinity).padding(.vertical, KonoSpace.s)
                        .background(KonoColor.surfaceSunken, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            Button {
                #if canImport(UIKit)
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                #endif
                onAdvance()
            } label: {
                Text(job.stage.ctaAr.k).font(KonoFont.button).foregroundStyle(KonoColor.textOnBrown)
                    .frame(maxWidth: .infinity).frame(height: KonoSize.buttonHeight)
                    .background(KonoColor.brown, in: RoundedRectangle(cornerRadius: KonoRadius.m, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(KonoSpace.l)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.xl, style: .continuous))
        .konoShadow(.lg)
        .konoScreenPadding()
        .padding(.bottom, 100)
        .sheet(isPresented: $showDetail) {
            NavigationStack { OrderDetailView(orderId: job.apiId, audience: .courier) }
        }
    }

    private var destinationLine: String {
        job.stage.rawValue <= JobStage.atStore.rawValue
            ? "\(job.storeNameAr) · \(job.storeAddressAr)"
            : "\(job.customerNameAr) · \(job.dropoffAddressAr)"
    }

    /// Apple Maps directions to the current leg's destination.
    private var navURL: URL? {
        let toStore = job.stage.rawValue <= JobStage.atStore.rawValue
        let lat = toStore ? job.storeLat : job.dropLat
        let lng = toStore ? job.storeLng : job.dropLng
        guard lat != 0, lng != 0 else { return nil }
        return URL(string: "http://maps.apple.com/?daddr=\(lat),\(lng)&dirflg=d")
    }

    private var phoneURL: URL? {
        let digits = job.customerPhone.filter { $0.isNumber || $0 == "+" }
        return digits.isEmpty ? nil : URL(string: "tel:\(digits)")
    }

    private func infoChip(_ text: String, _ icon: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 10, weight: .bold))
            Text(text).font(.cairo(11, .bold))
        }
        .foregroundStyle(KonoColor.brown)
        .padding(.horizontal, 8).padding(.vertical, 5)
        .background(KonoColor.cream, in: Capsule())
    }
}
