package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object NuvioEnhancedSettingsStorage {
    private const val payloadKey = "enhanced_settings_payload"
    private const val onboardingCompletedKey = "enhanced_onboarding_completed"
    private const val communitySnapshotKey = "enhanced_community_snapshot"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadOnboardingCompleted(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(onboardingCompletedKey) == null) return null
        return defaults.boolForKey(onboardingCompletedKey)
    }

    actual fun saveOnboardingCompleted(completed: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(completed, forKey = onboardingCompletedKey)
    }

    actual fun loadCommunitySnapshot(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(communitySnapshotKey)

    actual fun saveCommunitySnapshot(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = communitySnapshotKey)
    }
}
