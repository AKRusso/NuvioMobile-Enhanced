package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleRepositoryAggregationTest {

    @Test
    fun timeoutAndFailureBlocksDoNotDiscardSuccessfulResults() {
        val aggregated = aggregateOrderedSubtitleBlocks(
            blocks = listOf(listOf("first"), null, emptyList(), listOf("last")),
            generation = 4L,
            activeGeneration = 4L,
        )

        assertEquals(listOf("first", "last"), aggregated)
    }

    @Test
    fun successfulBlocksStayInAddonOrdinalOrder() {
        val aggregated = aggregateOrderedSubtitleBlocks(
            blocks = listOf(listOf("addon-1-a", "addon-1-b"), listOf("addon-2")),
            generation = 7L,
            activeGeneration = 7L,
        )

        assertEquals(listOf("addon-1-a", "addon-1-b", "addon-2"), aggregated)
    }

    @Test
    fun staleGenerationCannotProduceAResultForPublication() {
        assertNull(
            aggregateOrderedSubtitleBlocks(
                blocks = listOf(listOf("stale")),
                generation = 8L,
                activeGeneration = 9L,
            ),
        )
    }
}
