package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MpvOperationGateTest {
    @Test
    fun `release admits exactly one destroy`() {
        val gate = MpvOperationGate()
        var destroyCount = 0

        repeat(3) {
            if (gate.beginRelease()) destroyCount++
        }

        assertEquals(1, destroyCount)
    }

    @Test
    fun `submission after release is rejected`() {
        val gate = MpvOperationGate()

        assertTrue(gate.beginRelease())

        assertFalse(gate.acceptSubmission())
    }

    @Test
    fun `queued operation becomes stale after release`() {
        val gate = MpvOperationGate()

        assertTrue(gate.acceptSubmission())
        assertTrue(gate.beginRelease())

        assertFalse(gate.allowExecution())
    }

    @Test
    fun `successful snapshot read replaces last good value`() {
        val snapshots = LastGoodSnapshotCoordinator("initial")

        val result = snapshots.read { "current" }

        assertEquals("current", result)
        assertEquals("current", snapshots.lastGood())
    }

    @Test
    fun `failed snapshot read returns last good value`() {
        val snapshots = LastGoodSnapshotCoordinator("initial")
        snapshots.read { "current" }

        val result = snapshots.read { error("read failed") }

        assertEquals("current", result)
        assertEquals("current", snapshots.lastGood())
    }

    @Test
    fun `load transaction applies source and temporary subtitle headers in order`() {
        val sourceHeaders = mapOf("Authorization" to "source")
        val subtitleHeaders = mapOf("Referer" to "subtitle")
        val operations = mutableListOf<String>()

        runMpvLoadHeaderTransaction(
            sourceHeaders = sourceHeaders,
            subtitleHeaders = listOf(subtitleHeaders, emptyMap()),
            applyHeaders = { headers -> operations += "headers:$headers" },
            loadSource = {
                operations += "load"
                true
            },
            addSubtitle = { index ->
                operations += "subtitle:$index"
                true
            },
        )

        assertEquals(
            listOf(
                "headers:$sourceHeaders",
                "load",
                "headers:${sourceHeaders + subtitleHeaders}",
                "subtitle:0",
                "headers:$sourceHeaders",
                "subtitle:1",
            ),
            operations,
        )
    }

    @Test
    fun `load transaction restores source headers when subtitle addition fails`() {
        val sourceHeaders = mapOf("Authorization" to "source")
        val subtitleHeaders = mapOf("Authorization" to "subtitle")
        val appliedHeaders = mutableListOf<Map<String, String>>()

        assertFailsWith<IllegalStateException> {
            runMpvLoadHeaderTransaction(
                sourceHeaders = sourceHeaders,
                subtitleHeaders = listOf(subtitleHeaders),
                applyHeaders = appliedHeaders::add,
                loadSource = { true },
                addSubtitle = { error("subtitle failed") },
            )
        }

        assertEquals(
            listOf(sourceHeaders, sourceHeaders + subtitleHeaders, sourceHeaders),
            appliedHeaders,
        )
    }
}
