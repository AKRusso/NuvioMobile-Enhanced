package com.nuvio.app.features.details

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class CoordinatedMetadataResult<T>(
    val value: T,
    val coalesced: Boolean,
)

internal class MetadataRequestCoordinator<T>(
    private val scope: CoroutineScope,
) {
    private val lock = SynchronizedObject()
    private val inFlight = mutableMapOf<String, CompletableDeferred<T>>()

    suspend fun execute(key: String, request: suspend () -> T): CoordinatedMetadataResult<T> {
        val created = CompletableDeferred<T>()
        val (deferred, ownsRequest) = synchronized(lock) {
            inFlight[key]?.let { it to false }
                ?: created.also { inFlight[key] = it }.let { it to true }
        }

        if (ownsRequest) {
            scope.launch {
                try {
                    deferred.complete(request())
                } catch (error: Throwable) {
                    deferred.completeExceptionally(error)
                } finally {
                    synchronized(lock) {
                        if (inFlight[key] === deferred) inFlight.remove(key)
                    }
                }
            }
        }

        return CoordinatedMetadataResult(
            value = deferred.await(),
            coalesced = !ownsRequest,
        )
    }
}
