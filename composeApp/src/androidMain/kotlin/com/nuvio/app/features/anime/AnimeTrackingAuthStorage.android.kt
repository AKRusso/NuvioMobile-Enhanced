package com.nuvio.app.features.anime

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nuvio.app.core.storage.ProfileScopedKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal actual object AnimeTrackingAuthStorage {
    private const val PREFERENCES_NAME = "nuvio_anime_tracking_auth"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "nuvio.anime.tracking.credentials.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadMetadata(provider: AnimeTrackingProvider): String? =
        preferences?.getString(scopedKey(provider, "metadata"), null)

    actual fun saveMetadata(provider: AnimeTrackingProvider, payload: String?) {
        val editor = preferences?.edit() ?: return
        if (payload.isNullOrBlank()) editor.remove(scopedKey(provider, "metadata"))
        else editor.putString(scopedKey(provider, "metadata"), payload)
        editor.apply()
    }

    actual fun loadSettings(): String? =
        preferences?.getString(ProfileScopedKey.of(SETTINGS_KEY), null)

    actual fun saveSettings(payload: String?) {
        val key = ProfileScopedKey.of(SETTINGS_KEY)
        val editor = preferences?.edit() ?: return
        if (payload.isNullOrBlank()) editor.remove(key) else editor.putString(key, payload)
        editor.apply()
    }

    actual fun loadSecret(provider: AnimeTrackingProvider, key: String): String? {
        val storageKey = scopedKey(provider, key)
        val stored = preferences?.getString(storageKey, null) ?: return null
        return runCatching { decrypt(stored) }
            .onFailure { preferences?.edit()?.remove(storageKey)?.apply() }
            .getOrNull()
    }

    actual fun saveSecret(provider: AnimeTrackingProvider, key: String, value: String?) {
        val storageKey = scopedKey(provider, key)
        val editor = preferences?.edit() ?: return
        if (value.isNullOrBlank()) editor.remove(storageKey) else editor.putString(storageKey, encrypt(value))
        editor.apply()
    }

    actual fun removeProfile(profileId: Int) {
        val editor = preferences?.edit() ?: return
        editor.remove(ProfileScopedKey.of(SETTINGS_KEY, profileId))
        AnimeTrackingProvider.entries.forEach { provider ->
            listOf("metadata", "access_token", "refresh_token", "oauth_state", "pkce_verifier").forEach { key ->
                editor.remove(ProfileScopedKey.of(rawKey(provider, key), profileId))
            }
        }
        editor.apply()
    }

    private fun scopedKey(provider: AnimeTrackingProvider, key: String): String = ProfileScopedKey.of(rawKey(provider, key))
    private fun rawKey(provider: AnimeTrackingProvider, key: String): String = "${provider.name.lowercase()}_$key"

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return "${cipher.iv.toBase64()}.${cipher.doFinal(value.encodeToByteArray()).toBase64()}"
    }

    private fun decrypt(value: String): String {
        val separator = value.indexOf('.')
        require(separator > 0 && separator < value.lastIndex)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_BITS, value.substring(0, separator).fromBase64()),
        )
        return cipher.doFinal(value.substring(separator + 1).fromBase64()).decodeToString()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private const val SETTINGS_KEY = "anime_tracking_settings"
}
