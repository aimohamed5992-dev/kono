import SwiftUI
import AVFoundation

/// Hold-to-record voice notes. Writes an .m4a to a temp file.
@MainActor
@Observable
final class VoiceRecorder: NSObject {
    private var recorder: AVAudioRecorder?
    private(set) var isRecording = false
    private(set) var elapsed: TimeInterval = 0
    private var timer: Timer?
    private(set) var fileURL: URL?

    func requestPermissionAndStart() {
        AVAudioApplication.requestRecordPermission { [weak self] granted in
            Task { @MainActor in if granted { self?.start() } }
        }
    }

    private func start() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
        try? session.setActive(true)

        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("voice-\(UUID().uuidString).m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 32_000,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue,
        ]
        do {
            recorder = try AVAudioRecorder(url: url, settings: settings)
            recorder?.record()
            fileURL = url
            isRecording = true
            elapsed = 0
            timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
                Task { @MainActor in self?.elapsed = self?.recorder?.currentTime ?? 0 }
            }
        } catch {
            isRecording = false
        }
    }

    /// Stops and returns (url, seconds) if the clip is long enough to send.
    @discardableResult
    func stop() -> (URL, Int)? {
        timer?.invalidate(); timer = nil
        recorder?.stop()
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false)
        guard let url = fileURL, elapsed >= 0.6 else { return nil }
        return (url, Int(elapsed.rounded()))
    }

    func cancel() {
        timer?.invalidate(); timer = nil
        recorder?.stop()
        isRecording = false
        if let url = fileURL { try? FileManager.default.removeItem(at: url) }
        fileURL = nil
    }
}

/// Plays a single remote voice note.
@MainActor
@Observable
final class VoicePlayer: NSObject, AVAudioPlayerDelegate {
    static let shared = VoicePlayer()

    private var player: AVAudioPlayer?
    private(set) var playingURL: String?
    private(set) var progress: Double = 0
    private var timer: Timer?

    func toggle(_ urlString: String) {
        if playingURL == urlString { stop(); return }
        guard let url = URL(string: urlString) else { return }
        stop()
        Task {
            guard let data = try? await URLSession.shared.data(from: url).0 else { return }
            try? AVAudioSession.sharedInstance().setCategory(.playback)
            try? AVAudioSession.sharedInstance().setActive(true)
            player = try? AVAudioPlayer(data: data)
            player?.delegate = self
            player?.play()
            playingURL = urlString
            timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
                Task { @MainActor in
                    guard let p = self?.player else { return }
                    self?.progress = p.duration > 0 ? p.currentTime / p.duration : 0
                }
            }
        }
    }

    func stop() {
        timer?.invalidate(); timer = nil
        player?.stop(); player = nil
        playingURL = nil
        progress = 0
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in self.stop() }
    }
}

func mmss(_ seconds: Int) -> String {
    String(format: "%d:%02d", seconds / 60, seconds % 60)
}
