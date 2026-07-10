package com.nuvio.app.features.cloudstream

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.FileOutputStream

internal actual object CloudStreamPlatformStorage {
    private const val preferencesName = "nuvio_cloudstream"
    private const val stateKey = "state"
    private const val packagesDirectoryName = "cloudstream/packages"

    private var appContext: Context? = null
    private var preferences: SharedPreferences? = null

    actual fun initialize(context: Any?) {
        val androidContext = context as? Context ?: return
        appContext = androidContext.applicationContext
        preferences = androidContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadState(profileId: Int): String? =
        preferences?.getString("${stateKey}_$profileId", null)

    actual fun saveState(profileId: Int, payload: String) {
        preferences?.edit()?.putString("${stateKey}_$profileId", payload)?.apply()
    }

    actual fun savePackageAtomically(storageKey: String, bytes: ByteArray) {
        val directory = packagesDirectory()
        val destination = File(directory, "$storageKey.cs3")
        val temporary = File.createTempFile(storageKey, ".tmp", directory)
        val backup = File(directory, "$storageKey.backup")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (backup.exists()) backup.delete()
            if (destination.exists() && !destination.renameTo(backup)) {
                error("Could not prepare existing CloudStream package for update")
            }
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination)
                error("Could not commit CloudStream package")
            }
            backup.delete()
        } finally {
            temporary.delete()
        }
    }

    actual fun packageExists(storageKey: String): Boolean =
        File(packagesDirectory(), "$storageKey.cs3").isFile

    actual fun deletePackage(storageKey: String) {
        File(packagesDirectory(), "$storageKey.cs3").delete()
        File(packagesDirectory(), "$storageKey.backup").delete()
    }

    actual fun clearPackages() {
        packagesDirectory().deleteRecursively()
    }

    actual fun clearAllState() {
        preferences?.edit()?.clear()?.apply()
    }

    private fun packagesDirectory(): File {
        val context = requireNotNull(appContext) { "CloudStream storage is not initialized" }
        return File(context.filesDir, packagesDirectoryName).apply { mkdirs() }
    }
}
