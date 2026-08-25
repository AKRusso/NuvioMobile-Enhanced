package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal actual object AnimeTrackingAuthStorage {
    private const val KEYCHAIN_SERVICE = "com.nuvio.media.anime-tracking"

    actual fun loadMetadata(provider: AnimeTrackingProvider): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(scopedKey(provider, "metadata"))

    actual fun saveMetadata(provider: AnimeTrackingProvider, payload: String?) {
        val key = scopedKey(provider, "metadata")
        if (payload.isNullOrBlank()) NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        else NSUserDefaults.standardUserDefaults.setObject(payload, forKey = key)
    }

    actual fun loadSettings(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(SETTINGS_KEY))

    actual fun saveSettings(payload: String?) {
        val key = ProfileScopedKey.of(SETTINGS_KEY)
        if (payload.isNullOrBlank()) NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        else NSUserDefaults.standardUserDefaults.setObject(payload, forKey = key)
    }

    actual fun loadSecret(provider: AnimeTrackingProvider, key: String): String? =
        loadKeychainValue(rawKey(provider, key))

    actual fun saveSecret(provider: AnimeTrackingProvider, key: String, value: String?) =
        saveKeychainValue(rawKey(provider, key), value)

    actual fun removeProfile(profileId: Int) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(SETTINGS_KEY, profileId))
        AnimeTrackingProvider.entries.forEach { provider ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(rawKey(provider, "metadata"), profileId))
            listOf("access_token", "refresh_token", "oauth_state", "pkce_verifier").forEach { key ->
                deleteKeychainValue(rawKey(provider, key), profileId)
            }
        }
    }

    private fun scopedKey(provider: AnimeTrackingProvider, key: String): String = ProfileScopedKey.of(rawKey(provider, key))
    private fun rawKey(provider: AnimeTrackingProvider, key: String): String = "${provider.name.lowercase()}_$key"

    @OptIn(ExperimentalForeignApi::class)
    private fun loadKeychainValue(key: String): String? = withQuery(key) { query ->
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound || status != errSecSuccess) return@memScoped null
            val data: CFDataRef = result.value?.reinterpret() ?: return@memScoped null
            try {
                val length = CFDataGetLength(data).toInt()
                val bytes = CFDataGetBytePtr(data) ?: return@memScoped null
                ByteArray(length) { index -> bytes[index].toByte() }.decodeToString()
            } finally {
                CFRelease(data)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun saveKeychainValue(key: String, value: String?) {
        deleteKeychainValue(key)
        if (value.isNullOrBlank()) return
        withQuery(key) { query ->
            val bytes = value.encodeToByteArray().toUByteArray()
            val data = CFDataCreate(null, bytes.refTo(0), bytes.size.toLong()) ?: error("Unable to encode credential")
            try {
                CFDictionarySetValue(query, kSecValueData, data)
                check(SecItemAdd(query, null) == errSecSuccess)
            } finally {
                CFRelease(data)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun deleteKeychainValue(key: String, profileId: Int? = null) {
        withQuery(key, profileId) { query -> SecItemDelete(query) }
    }

    @OptIn(ExperimentalForeignApi::class)
    private inline fun <T> withQuery(
        key: String,
        profileId: Int? = null,
        block: (CFMutableDictionaryRef) -> T,
    ): T {
        val service = CFStringCreateWithCString(null, KEYCHAIN_SERVICE, kCFStringEncodingUTF8)!!
        val accountValue = profileId?.let { ProfileScopedKey.of(key, it) } ?: ProfileScopedKey.of(key)
        val account = CFStringCreateWithCString(null, accountValue, kCFStringEncodingUTF8)!!
        val query = CFDictionaryCreateMutable(null, 0L, null, null)!!
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            return block(query)
        } finally {
            CFRelease(query)
            CFRelease(account)
            CFRelease(service)
        }
    }

    private const val SETTINGS_KEY = "anime_tracking_settings"
}
