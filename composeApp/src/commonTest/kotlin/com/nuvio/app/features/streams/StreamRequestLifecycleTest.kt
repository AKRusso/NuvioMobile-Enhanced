package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamRequestLifecycleTest {
    @Test
    fun `live loading request can be reused`() {
        assertTrue(
            shouldReuseStreamRequest(
                sameRequest = true,
                hasResult = false,
                isLoading = true,
                jobActive = true,
            ),
        )
    }

    @Test
    fun `dead loading request must restart`() {
        assertFalse(
            shouldReuseStreamRequest(
                sameRequest = true,
                hasResult = true,
                isLoading = true,
                jobActive = false,
            ),
        )
    }

    @Test
    fun `completed request can reuse its result`() {
        assertTrue(
            shouldReuseStreamRequest(
                sameRequest = true,
                hasResult = true,
                isLoading = false,
                jobActive = false,
            ),
        )
    }

    @Test
    fun `different request never reuses global state`() {
        assertFalse(
            shouldReuseStreamRequest(
                sameRequest = false,
                hasResult = true,
                isLoading = true,
                jobActive = true,
            ),
        )
    }
}
