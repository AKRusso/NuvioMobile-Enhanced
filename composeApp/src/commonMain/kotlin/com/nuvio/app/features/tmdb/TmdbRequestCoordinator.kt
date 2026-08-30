package com.nuvio.app.features.tmdb

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

internal class TmdbRequestCoordinator(maxConcurrentRequests: Int) {
    private val mutex = Mutex()
    private val semaphore = Semaphore(maxConcurrentRequests)
    private val inFlight = mutableMapOf<String, CompletableDeferred<String?>>()

    suspend fun execute(key: String, request: suspend () -> String?): String? {
        val created = CompletableDeferred<String?>()
        val (result, ownsRequest) = mutex.withLock {
            inFlight[key]?.let { it to false }
                ?: created.also { inFlight[key] = it }.let { it to true }
        }
        if (!ownsRequest) return result.await()

        return try {
            semaphore.withPermit { request() }.also(result::complete)
        } catch (error: Throwable) {
            result.completeExceptionally(error)
            throw error
        } finally {
            mutex.withLock {
                if (inFlight[key] === result) inFlight.remove(key)
            }
        }
    }
}
