import SwiftUI
import PhotosUI
import CoreLocation

struct ChatView: View {
    @State var store: ChatStore
    @Environment(\.dismiss) private var dismiss

    @State private var draft = ""
    @State private var photoItem: PhotosPickerItem?
    @State private var fullImage: String?
    @State private var recorder = VoiceRecorder()
    @State private var recordCancelled = false
    @StateObject private var loc = OneShotLocation()

    var body: some View {
        VStack(spacing: 0) {
            messagesList
            Divider()
            composer
        }
        .background(KonoColor.background)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { header }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    Task {
                        if let url = await store.startCall() { await UIApplication.shared.open(url) }
                    }
                } label: {
                    Image(systemName: "phone.fill").font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(KonoColor.brown)
                }
            }
        }
        .onAppear { store.start() }
        .onDisappear { store.stop() }
        .task(id: photoItem) {
            guard let photoItem,
                  let data = try? await photoItem.loadTransferable(type: Data.self),
                  let img = UIImage(data: data) else { return }
            store.sendImage(img)
            self.photoItem = nil
        }
        .fullScreenCover(item: Binding(get: { fullImage.map { IdString(id: $0) } },
                                       set: { fullImage = $0?.id })) { item in
            ImageViewer(url: item.id) { fullImage = nil }
        }
    }

    // MARK: header

    private var header: some View {
        HStack(spacing: 8) {
            AssetImage(name: store.otherPhoto, fallbackSymbol: ChatRole.icon(store.otherRole),
                       fallbackTint: KonoColor.brown)
                .frame(width: 30, height: 30).clipShape(Circle())
            VStack(alignment: .leading, spacing: 0) {
                Text(store.otherName).font(KonoFont.subheadline.weight(.bold))
                    .foregroundStyle(KonoColor.textPrimary).lineLimit(1)
                Text("\(ChatRole.ar(store.otherRole)) · طلب \(store.orderShortCode)")
                    .font(.system(size: 10)).foregroundStyle(KonoColor.textSecondary).lineLimit(1)
            }
        }
    }

    // MARK: messages

    private var messagesList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 3) {
                    ForEach(Array(store.messages.enumerated()), id: \.element.id) { idx, msg in
                        if showDateChip(at: idx) { ChatDateChip(date: msg.createdAt) }
                        ChatBubble(msg: msg) { fullImage = $0 }
                            .id(msg.id)
                    }
                    Color.clear.frame(height: 1).id("bottom")
                }
                .padding(.vertical, KonoSpace.m)
            }
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: store.messages.count) {
                withAnimation(.easeOut(duration: 0.2)) { proxy.scrollTo("bottom", anchor: .bottom) }
            }
            .onAppear { proxy.scrollTo("bottom", anchor: .bottom) }
            .overlay {
                if store.messages.isEmpty && !store.loading {
                    VStack(spacing: KonoSpace.s) {
                        Image(systemName: "bubble.left.and.bubble.right")
                            .font(.system(size: 34)).foregroundStyle(KonoColor.textTertiary)
                        Text("ابدأ المحادثة بخصوص طلب \(store.orderShortCode)")
                            .font(KonoFont.footnote).foregroundStyle(KonoColor.textSecondary)
                    }
                }
            }
        }
    }

    private func showDateChip(at idx: Int) -> Bool {
        guard idx < store.messages.count else { return false }
        guard idx > 0 else { return true }
        return !Calendar.current.isDate(store.messages[idx].createdAt,
                                        inSameDayAs: store.messages[idx - 1].createdAt)
    }

    // MARK: composer

    private var composer: some View {
        VStack(spacing: 0) {
            if recorder.isRecording { recordingBar }
            HStack(spacing: KonoSpace.s) {
                Menu {
                    PhotosPicker(selection: $photoItem, matching: .images) {
                        Label("صورة", systemImage: "photo")
                    }
                    Button {
                        loc.request { c in store.sendLocation(lat: c.latitude, lng: c.longitude) }
                    } label: { Label("الموقع الحالي", systemImage: "location.fill") }
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(KonoColor.brown)
                        .frame(width: 34, height: 34)
                        .background(KonoColor.surfaceSunken, in: Circle())
                }

                TextField("اكتب رسالة…", text: $draft, axis: .vertical)
                    .font(KonoFont.body)
                    .lineLimit(1...4)
                    .padding(.horizontal, KonoSpace.m)
                    .padding(.vertical, 9)
                    .background(KonoColor.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(KonoColor.hairline, lineWidth: 1))

                if draft.trimmingCharacters(in: .whitespaces).isEmpty {
                    Image(systemName: recorder.isRecording ? "waveform" : "mic.fill")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(KonoColor.textOnBrown)
                        .frame(width: 38, height: 38)
                        .background(recorder.isRecording ? KonoColor.danger : KonoColor.brown, in: Circle())
                        .scaleEffect(recorder.isRecording ? 1.15 : 1)
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onChanged { v in
                                    if !recorder.isRecording {
                                        recordCancelled = false
                                        recorder.requestPermissionAndStart()
                                    }
                                    recordCancelled = v.translation.height < -60
                                }
                                .onEnded { _ in
                                    if recordCancelled { recorder.cancel() }
                                    else if let (url, dur) = recorder.stop() { store.sendVoice(url, duration: dur) }
                                    recordCancelled = false
                                }
                        )
                } else {
                    Button {
                        store.sendText(draft); draft = ""
                    } label: {
                        Image(systemName: "arrow.up")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(KonoColor.textOnBrown)
                            .frame(width: 38, height: 38)
                            .background(KonoColor.brown, in: Circle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, KonoSpace.m)
            .padding(.vertical, KonoSpace.s)
        }
        .background(KonoColor.surfaceRaised)
    }

    private var recordingBar: some View {
        HStack(spacing: KonoSpace.s) {
            Circle().fill(KonoColor.danger).frame(width: 9, height: 9)
                .opacity(0.6)
            Text(mmss(Int(recorder.elapsed))).font(KonoFont.subheadline.monospacedDigit())
                .foregroundStyle(KonoColor.textPrimary)
            Spacer()
            Text(recordCancelled ? "أفلت للإلغاء" : "اسحب لأعلى للإلغاء")
                .font(KonoFont.footnote)
                .foregroundStyle(recordCancelled ? KonoColor.danger : KonoColor.textSecondary)
        }
        .padding(.horizontal, KonoSpace.l).padding(.vertical, KonoSpace.s)
        .background(KonoColor.cream)
    }
}

private struct IdString: Identifiable { let id: String }

struct ImageViewer: View {
    let url: String
    var onClose: () -> Void
    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            AssetImage(name: url, contentMode: .fit).ignoresSafeArea()
            Button(action: onClose) {
                Image(systemName: "xmark").font(.system(size: 16, weight: .bold))
                    .foregroundStyle(.white).padding(12).background(.black.opacity(0.4), in: Circle())
            }
            .padding()
        }
    }
}

/// One-shot current-location fetch for sharing in chat.
final class OneShotLocation: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var handler: ((CLLocationCoordinate2D) -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func request(_ completion: @escaping (CLLocationCoordinate2D) -> Void) {
        handler = completion
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways: manager.requestLocation()
        default: break
        }
    }

    func locationManagerDidChangeAuthorization(_ m: CLLocationManager) {
        if m.authorizationStatus == .authorizedWhenInUse || m.authorizationStatus == .authorizedAlways {
            m.requestLocation()
        }
    }
    func locationManager(_ m: CLLocationManager, didUpdateLocations locs: [CLLocation]) {
        if let c = locs.last?.coordinate { handler?(c); handler = nil }
    }
    func locationManager(_ m: CLLocationManager, didFailWithError error: Error) { handler = nil }
}
