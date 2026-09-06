package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class MetadataRequestCoordinatorTest {
    @Test
    fun `coalesces duplicate metadata requests`() = runBlocking {
        val coordinator = MetadataRequestCoordinator<String>(this)
        val calls = atomic(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        coroutineScope {
            val owner = async {
                coordinator.execute("same-request") {
                    calls.incrementAndGet()
                    entered.complete(Unit)
                    release.await()
                    "metadata"
                }
            }
            entered.await()
            val waiter = async {
                coordinator.execute("same-request") { error("duplicate request") }
            }
            release.complete(Unit)

            assertEquals(CoordinatedMetadataResult("metadata", coalesced = false), owner.await())
            assertEquals(CoordinatedMetadataResult("metadata", coalesced = true), waiter.await())
            assertEquals(1, calls.value)
        }
    }

    @Test
    fun `keeps different metadata requests independent`() = runBlocking {
        val coordinator = MetadataRequestCoordinator<String>(this)

        coroutineScope {
            val first = async { coordinator.execute("first") { "one" } }
            val second = async { coordinator.execute("second") { "two" } }

            assertEquals("one", first.await().value)
            assertEquals("two", second.await().value)
        }
    }

    @Test
    fun `removes failed metadata request so it can retry`() = runBlocking {
        val coordinator = MetadataRequestCoordinator<String>(this)

        assertFailsWith<IllegalStateException> {
            coordinator.execute("retry") { error("first failure") }
        }

        assertEquals("success", coordinator.execute("retry") { "success" }.value)
    }
}
