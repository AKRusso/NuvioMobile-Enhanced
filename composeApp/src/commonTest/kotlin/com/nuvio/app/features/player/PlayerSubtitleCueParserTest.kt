package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerSubtitleCueParserTest {

    @Test
    fun parsesSubtitleCueEndTime() {
        val cues = PlayerSubtitleCueParser.parse(
            """
            1
            00:00:01,000 --> 00:00:02,500
            Good morning
            """.trimIndent(),
            sourceUrl = "subtitle.srt",
        )

        assertEquals(1_000L, cues.single().startTimeMs)
        assertEquals(2_500L, cues.single().endTimeMs)
    }

    @Test
    fun activeCueDoesNotRemainHighlightedBetweenLines() {
        val cues = listOf(
            SubtitleSyncCue(startTimeMs = 1_000L, endTimeMs = 2_000L, text = "First"),
            SubtitleSyncCue(startTimeMs = 3_000L, endTimeMs = 4_000L, text = "Second"),
        )

        assertEquals("First", activeSubtitleSyncCue(cues, 1_500L)?.text)
        assertNull(activeSubtitleSyncCue(cues, 2_500L))
        assertEquals("Second", activeSubtitleSyncCue(cues, 3_500L)?.text)
    }

    @Test
    fun selectedCueIsAlignedToCapturedPlaybackPosition() {
        val anchorPositionMs = 60_000L
        val cueStartTimeMs = 42_000L

        val delayMs = subtitleDelayForCue(anchorPositionMs, cueStartTimeMs)

        assertEquals(18_000, delayMs)
        assertEquals(cueStartTimeMs, anchorPositionMs - delayMs)
    }
}
