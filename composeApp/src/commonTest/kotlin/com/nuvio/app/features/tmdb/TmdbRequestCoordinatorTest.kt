package com.nuvio.app.features.tmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class TmdbRequestCoordinatorTest {
    @Test
    fun `limits concurrent distinct requests`() = runBlocking {
        val coordinator = TmdbRequestCoordinator(maxConcurrentRequests = 3)
        val active = atomic(0)
        val entered = atomic(0)
        val firstWaveEntered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        coroutineScope {
            val requests = (1..10).map { index ->
                async {
                    coordinator.execute("request-$index") {
                        active.incrementAndGet()
                        if (entered.incrementAndGet() == 3) firstWaveEntered.complete(Unit)
                        release.await()
                        active.decrementAndGet()
                        "result-$index"
                    }
                }
            }
            firstWaveEntered.await()
            assertEquals(3, active.value)
            release.complete(Unit)
            assertEquals(10, requests.awaitAll().size)
        }
    }

    @Test
    fun `coalesces duplicate in-flight requests`() = runBlocking {
        val coordinator = TmdbRequestCoordinator(maxConcurrentRequests = 4)
        val calls = atomic(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        coroutineScope {
            val owner = async {
                coordinator.execute("same-url") {
                    calls.incrementAndGet()
                    entered.complete(Unit)
                    release.await()
                    "shared"
                }
            }
            entered.await()
            val waiters = List(5) {
                async { coordinator.execute("same-url") { error("duplicate request") } }
            }
            release.complete(Unit)
            assertEquals(List(6) { "shared" }, listOf(owner.await()) + waiters.awaitAll())
            assertEquals(1, calls.value)
        }
    }

    @Test
    fun `failed request is removed and can be retried`() = runBlocking {
        val coordinator = TmdbRequestCoordinator(maxConcurrentRequests = 1)
        assertFailsWith<IllegalStateException> {
            coordinator.execute("retry") { error("first failure") }
        }
        assertEquals("success", coordinator.execute("retry") { "success" })
    }
}
