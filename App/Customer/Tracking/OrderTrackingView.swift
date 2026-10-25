import SwiftUI
import MapKit

struct OrderTrackingView: View {
    let order: Order
    var justPlaced: Bool = false

    @Environment(\.cart) private var cart
    @Environment(\.dismiss) private var dismiss

    @State private var stage: OrderStage
    @State private var courierProgress: CGFloat = 0.15
    @State private var showConfetti = false
    @State private var camera: MapCameraPosition

    private let advance = Timer.publish(every: 3.5, on: .main, in: .common).autoconnect()

    init(order: Order, justPlaced: Bool = false) {
        self.order = order
        self.justPlaced = justPlaced
        _stage = State(initialValue: order.stage)
        let mid = CLLocationCoordinate2D(
            latitude: (order.address.latitude + 24.826) / 2,
            longitude: (order.address.longitude + 46.639) / 2)
        _camera = State(initialValue: .region(MKCoordinateRegion(
            center: mid, span: MKCoordinateSpan(latitudeDelta: 0.04, longitudeDelta: 0.04))))
    }

    private var storeCoord: CLLocationCoordinate2D { .init(latitude: 24.826, longitude: 46.639) }
    private var homeCoord: CLLocationCoordinate2D {
        .init(latitude: order.address.latitude, longitude: order.address.longitude)
    }
    private var courierCoord: CLLocationCoordinate2D {
        .init(latitude: storeCoord.latitude + (homeCoord.latitude - storeCoord.latitude) * courierProgress,
              longitude: storeCoord.longitude + (homeCoord.longitude - storeCoord.longitude) * courierProgress)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Map(position: $camera, interactionModes: [.pan, .zoom]) {
                Annotation("المتجر", coordinate: storeCoord) {
                    mapPin("bag.fill", KonoColor.caramel)
                }
                Annotation("أنت", coordinate: homeCoord) {
                    mapPin("house.fill", KonoColor.brown)
                }
                if stage.rawValue >= OrderStage.onTheWay.rawValue {
                    Annotation("المندوب", coordinate: courierCoord) {
                        mapPin("figure.outdoor.cycle", KonoColor.info)
                            .scaleEffect(1.1)
                    }
                    MapPolyline(coordinates: [storeCoord, courierCoord])
                        .stroke(KonoColor.brown, style: .init(lineWidth: 4, lineCap: .round))
                    MapPolyline(coordinates: [courierCoord, homeCoord])
                        .stroke(KonoColor.brown.opacity(0.3), style: .init(lineWidth: 4, lineCap: .round, dash: [2, 6]))
                }
            }
            .ignoresSafeArea()

            VStack {
                HStack {
                    Button {
                        if justPlaced { cart.clear() }
                        dismiss()
                    } label: {
                        Image(systemName: justPlaced ? "xmark" : "chevron.right")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundStyle(KonoColor.brown)
                            .frame(width: 40, height: 40)
                            .background(KonoColor.surface, in: Circle())
                            .konoShadow(.md)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Text(order.shortCode)
                        .font(KonoFont.subheadline).foregroundStyle(KonoColor.textPrimary)
                        .padding(.horizontal, KonoSpace.m).frame(height: 40)
                        .background(KonoColor.surface, in: Capsule()).konoShadow(.sm)
                }
                .konoScreenPadding()
                .padding(.top, KonoSpace.s)
                Spacer()
            }

            trackingSheet

            if showConfetti { ConfettiBurst().allowsHitTesting(false) }
        }
        .navigationBarHidden(true)
        .hidesKonoTabBar()
        .onReceive(advance) { _ in
            // Live orders are driven by the server (polled in .task); only the
            // offline demo advances itself on a timer.
            guard order.apiId == 0, stage != .delivered else { return }
            withAnimation(KonoMotion.spring) {
                stage = OrderStage(rawValue: stage.rawValue + 1) ?? .delivered
            }
            if stage.rawValue >= OrderStage.onTheWay.rawValue {
                withAnimation(.easeInOut(duration: 3)) { courierProgress = min(1, courierProgress + 0.3) }
            }
        }
        .onAppear {
            if justPlaced {
                showConfetti = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.4) { showConfetti = false }
            }
        }
        .task {
            guard order.apiId != 0 else { return }
            while !Task.isCancelled {
                let fresh = await AppData.shared.refreshOrder(order)
                withAnimation(KonoMotion.spring) {
                    if fresh.stage != stage { stage = fresh.stage }
                    if stage.rawValue >= OrderStage.onTheWay.rawValue {
                        courierProgress = min(1, courierProgress + 0.25)
                    }
                }
                try? await Task.sleep(nanoseconds: 4_000_000_000)
            }
        }
    }

    private var trackingSheet: some View {
        VStack(alignment: .leading, spacing: KonoSpace.l) {
            Capsule().fill(KonoColor.divider).frame(width: 40, height: 4).frame(maxWidth: .infinity)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(stage.titleAr.k).font(KonoFont.title2).foregroundStyle(KonoColor.textPrimary)
                        .contentTransition(.opacity)
                    Text(stage == .delivered ? "نتمنى لك وجبة هنية" : "الوصول المتوقع \(order.store.etaMinutes) دقيقة")
                        .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                }
                Spacer()
                ZStack {
                    Circle().fill(KonoColor.yellow.opacity(0.25)).frame(width: 52, height: 52)
                    Image(systemName: stage.icon).font(.system(size: 20, weight: .bold))
                        .foregroundStyle(KonoColor.brown)
                        .contentTransition(.symbolEffect(.replace))
                }
            }

            StageProgressBar(stage: stage)

            if order.apiId != 0, stage != .delivered {
                OrderChatButtons(orderId: order.apiId)
            }

            if stage.rawValue >= OrderStage.confirmed.rawValue, let courier = order.courier {
                Divider()
                HStack(spacing: KonoSpace.m) {
                    AssetImage(name: courier.photoAsset, fallbackSymbol: "person.fill",
                               fallbackTint: KonoColor.brown)
                        .frame(width: 48, height: 48).clipShape(Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(courier.name).font(KonoFont.bodyMedium).foregroundStyle(KonoColor.textPrimary)
                        HStack(spacing: 4) {
                            RatingBadge(rating: courier.rating, compact: true)
                            Text("· \(courier.vehicleAr) · \(courier.plateAr)")
                                .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                        }
                    }
                    Spacer()
                    let tel = courier.phone.filter { $0.isNumber || $0 == "+" }
                    contactLink("phone.fill", url: URL(string: "tel:\(tel)"))
                    contactLink("message.fill", url: URL(string: "sms:\(tel)"))
                }
            }
        }
        .padding(KonoSpace.l)
        .padding(.bottom, KonoSpace.s)
        .background(KonoColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: KonoRadius.xl, style: .continuous))
        .konoShadow(.lg)
        .konoScreenPadding()
        .padding(.bottom, KonoSpace.s)
    }

    @ViewBuilder
    private func contactLink(_ icon: String, url: URL?) -> some View {
        if let url {
            Link(destination: url) {
                Image(systemName: icon).font(.system(size: 14, weight: .bold))
                    .foregroundStyle(KonoColor.textOnBrown)
                    .frame(width: 38, height: 38)
                    .background(KonoColor.brown, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private func mapPin(_ icon: String, _ color: Color) -> some View {
        ZStack {
            Circle().fill(color).frame(width: 36, height: 36)
            Image(systemName: icon).font(.system(size: 15, weight: .bold)).foregroundStyle(.white)
        }
        .konoShadow(.md)
    }
}

struct StageProgressBar: View {
    let stage: OrderStage

    var body: some View {
        HStack(spacing: 4) {
            ForEach(OrderStage.allCases, id: \.rawValue) { s in
                Capsule()
                    .fill(s.rawValue <= stage.rawValue ? KonoColor.brown : KonoColor.divider)
                    .frame(height: 5)
                    .animation(KonoMotion.spring.delay(Double(s.rawValue) * 0.05), value: stage)
            }
        }
    }
}

/// Lightweight confetti for the "order placed" moment.
struct ConfettiBurst: View {
    @State private var go = false
    private let pieces = 40

    var body: some View {
        GeometryReader { geo in
            ZStack {
                ForEach(0..<pieces, id: \.self) { i in
                    let colors: [Color] = [KonoColor.yellow, KonoColor.brown, KonoColor.caramel, KonoColor.cream]
                    RoundedRectangle(cornerRadius: 2)
                        .fill(colors[i % colors.count])
                        .frame(width: 8, height: 12)
                        .rotationEffect(.degrees(Double(i) * 37))
                        .position(x: geo.size.width / 2, y: geo.size.height / 3)
                        .offset(x: go ? CGFloat.random(in: -180...180) : 0,
                                y: go ? CGFloat.random(in: -80...400) : 0)
                        .opacity(go ? 0 : 1)
                        .animation(.easeOut(duration: 1.8).delay(Double(i) * 0.01), value: go)
                }
            }
            .onAppear { go = true }
        }
    }
}
