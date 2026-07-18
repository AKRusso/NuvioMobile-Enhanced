import Foundation
#if canImport(ActivityKit) && os(iOS) && !targetEnvironment(macCatalyst)
import ActivityKit
#endif

private let downloadsLiveStatusUpdatedNotification = Notification.Name("NuvioDownloadsLiveStatusUpdated")
private let downloadsLiveStatusPayloadKey = "nuvio.downloads.live_status.payload"

final class DownloadsLiveActivityManager {
    static let shared = DownloadsLiveActivityManager()

    private var observer: NSObjectProtocol?

    private init() {}

    func start() {
        guard observer == nil else { return }

        observer = NotificationCenter.default.addObserver(
            forName: downloadsLiveStatusUpdatedNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.syncFromPayloadStore()
        }

        syncFromPayloadStore()
    }

    private func syncFromPayloadStore() {
#if canImport(ActivityKit) && os(iOS) && !targetEnvironment(macCatalyst)
        guard #available(iOS 16.1, *) else { return }

        let payload = loadPayload()
        Task {
            await apply(payload)
        }
#endif
    }

    private func loadPayload() -> DownloadsLiveStatusPayload? {
        guard let encoded = UserDefaults.standard.string(forKey: downloadsLiveStatusPayloadKey) else {
            return nil
        }
        let data = Data(encoded.utf8)
        return try? JSONDecoder().decode(DownloadsLiveStatusPayload.self, from: data)
    }

#if canImport(ActivityKit) && os(iOS) && !targetEnvironment(macCatalyst)
    @available(iOS 16.1, *)
    private func apply(_ payload: DownloadsLiveStatusPayload?) async {
        let existing = Activity<DownloadsLiveActivityAttributes>.activities.first

        guard let payload else {
            if let existing {
                await existing.end(dismissalPolicy: .immediate)
            }
            return
        }

        let state = DownloadsLiveActivityAttributes.ContentState(
            status: payload.status,
            progressPercent: payload.progressPercent,
            transferredText: transferredText(payload)
        )

        if let existing, existing.attributes.downloadId == payload.id {
            await existing.update(using: state)
            return
        }

        if let existing {
            await existing.end(dismissalPolicy: .immediate)
        }

        let attributes = DownloadsLiveActivityAttributes(
            downloadId: payload.id,
            title: payload.title,
            subtitle: payload.subtitle,
            providerName: payload.providerName,
            streamTitle: payload.streamTitle,
            artworkUrl: payload.artworkUrl
        )

        _ = try? Activity<DownloadsLiveActivityAttributes>.request(
            attributes: attributes,
            contentState: state,
            pushType: nil
        )
    }
#endif

    private func transferredText(_ payload: DownloadsLiveStatusPayload) -> String {
        let downloaded = formatBytes(payload.downloadedBytes)
        let sizeText: String
        if let total = payload.totalBytes {
            sizeText = "\(downloaded) / \(formatBytes(total))"
        } else {
            sizeText = downloaded
        }
        return [
            sizeText,
            speedText(payload.downloadSpeedBytesPerSecond),
            etaText(payload.estimatedRemainingSeconds),
        ]
            .compactMap { $0 }
            .joined(separator: " • ")
    }

    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useKB, .useMB, .useGB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }

    private func speedText(_ bytesPerSecond: Int64?) -> String? {
        guard let bytesPerSecond, bytesPerSecond > 0 else { return nil }
        return "⚡ \(formatBytes(bytesPerSecond))/s"
    }

    private func etaText(_ seconds: Int64?) -> String? {
        guard let seconds else { return nil }
        return "ETA \(formatDuration(seconds))"
    }

    private func formatDuration(_ seconds: Int64) -> String {
        let safeSeconds = max(0, seconds)
        if safeSeconds < 60 { return "\(safeSeconds)s" }
        let minutes = safeSeconds / 60
        let remainingSeconds = safeSeconds % 60
        if minutes < 60 {
            if remainingSeconds > 0 && minutes < 10 {
                return "\(minutes)m \(remainingSeconds)s"
            }
            return "\(minutes)m"
        }
        let hours = minutes / 60
        let remainingMinutes = minutes % 60
        if remainingMinutes > 0 {
            return "\(hours)h \(remainingMinutes)m"
        }
        return "\(hours)h"
    }
}

#if canImport(ActivityKit) && os(iOS) && !targetEnvironment(macCatalyst)
@available(iOS 16.1, *)
struct DownloadsLiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        let status: String
        let progressPercent: Int
        let transferredText: String
    }

    let downloadId: String
    let title: String
    let subtitle: String
    let providerName: String
    let streamTitle: String
    let artworkUrl: String?
}
#endif

private struct DownloadsLiveStatusPayload: Decodable {
    let id: String
    let title: String
    let subtitle: String
    let providerName: String
    let streamTitle: String
    let artworkUrl: String?
    let status: String
    let downloadedBytes: Int64
    let totalBytes: Int64?
    let downloadSpeedBytesPerSecond: Int64?
    let estimatedRemainingSeconds: Int64?
    let progressPercent: Int
}
