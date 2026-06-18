import Foundation

// Portable attention-value math for the (future) iOS app. Mirrors
// android `domain/EstimateEngine.kt` and `backend/app/estimate.py` exactly —
// keep all three in sync with docs/ESTIMATE_SPEC.md. Everything here is a
// transparent estimate, never an exact claim.

public struct PlatformConfig {
    public let platform: String
    public let bundleId: String
    public let adsPerMinute: Double
    public let lowCpmInr: Double
    public let highCpmInr: Double
    public let monetized: Bool

    public init(platform: String, bundleId: String, adsPerMinute: Double,
                lowCpmInr: Double, highCpmInr: Double, monetized: Bool = true) {
        self.platform = platform
        self.bundleId = bundleId
        self.adsPerMinute = adsPerMinute
        self.lowCpmInr = lowCpmInr
        self.highCpmInr = highCpmInr
        self.monetized = monetized
    }
}

public struct PlatformEstimate {
    public let platform: String
    public let bundleId: String
    public let minutes: Double
    public let estimatedAdsSeen: Int
    public let valueLowInr: Double
    public let valueHighInr: Double
}

public struct AttentionReceipt {
    public let totalMinutes: Double
    public let estimatedAdsSeen: Int
    public let estimatedValueLowInr: Int
    public let estimatedValueHighInr: Int
    public let userReceivedInr: Int
    public let perPlatform: [PlatformEstimate]
}

public enum EstimateEngine {

    private static func roundHalfUp(_ value: Double) -> Int {
        // Values are non-negative, so away-from-zero == half-up.
        Int(value.rounded(.toNearestOrAwayFromZero))
    }

    public static func estimatePlatform(
        config: PlatformConfig,
        durationSeconds: Int,
        personalAdsPerMinute: Double? = nil
    ) -> PlatformEstimate {
        let seconds = max(0, durationSeconds)
        let minutes = Double(seconds) / 60.0

        if !config.monetized {
            return PlatformEstimate(platform: config.platform, bundleId: config.bundleId,
                                    minutes: minutes, estimatedAdsSeen: 0,
                                    valueLowInr: 0, valueHighInr: 0)
        }

        let rate = personalAdsPerMinute ?? config.adsPerMinute
        let ads = roundHalfUp(minutes * rate)
        return PlatformEstimate(
            platform: config.platform,
            bundleId: config.bundleId,
            minutes: minutes,
            estimatedAdsSeen: ads,
            valueLowInr: Double(ads) * config.lowCpmInr / 1000.0,
            valueHighInr: Double(ads) * config.highCpmInr / 1000.0
        )
    }

    public static func buildReceipt(
        configsByBundle: [String: PlatformConfig],
        usageSecondsByBundle: [String: Int],
        personalRatesByBundle: [String: Double] = [:]
    ) -> AttentionReceipt {
        var totalMinutes = 0.0, totalLow = 0.0, totalHigh = 0.0
        var totalAds = 0
        var perPlatform: [PlatformEstimate] = []

        for (bundle, seconds) in usageSecondsByBundle {
            guard let config = configsByBundle[bundle] else { continue }
            let est = estimatePlatform(config: config, durationSeconds: seconds,
                                       personalAdsPerMinute: personalRatesByBundle[bundle])
            perPlatform.append(est)
            totalMinutes += est.minutes
            totalAds += est.estimatedAdsSeen
            totalLow += est.valueLowInr
            totalHigh += est.valueHighInr
        }

        perPlatform.sort { $0.minutes > $1.minutes }

        return AttentionReceipt(
            totalMinutes: totalMinutes,
            estimatedAdsSeen: totalAds,
            estimatedValueLowInr: roundHalfUp(totalLow),
            estimatedValueHighInr: roundHalfUp(totalHigh),
            userReceivedInr: 0,
            perPlatform: perPlatform
        )
    }
}
