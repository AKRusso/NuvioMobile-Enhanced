package com.nuvio.app.features.anime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AnimeTrackingRepositoryTest {
    @Test
    fun acceptsOnlyTheConfiguredCallbackEndpoint() {
        val redirect = "nuvioenhanced://auth/anilist"

        assertTrue(isExpectedAnimeAuthCallback("$redirect?code=123&state=abc", redirect))
        assertTrue(isExpectedAnimeAuthCallback("$redirect/#access_token=token", redirect))
        assertFalse(isExpectedAnimeAuthCallback("nuvioenhanced://auth/anilist-evil?code=123", redirect))
        assertFalse(isExpectedAnimeAuthCallback("nuvioenhanced://attacker/anilist?code=123", redirect))
        assertFalse(isExpectedAnimeAuthCallback("https://auth/anilist?code=123", redirect))
    }

    @Test
    fun mapsProviderStatusesWithoutChangingTheirMeaning() {
        assertEquals(AnimeTrackingUserStatus.WATCHING, AnimeTrackingUserStatus.fromAniList("CURRENT"))
        assertEquals(AnimeTrackingUserStatus.REWATCHING, AnimeTrackingUserStatus.fromAniList("REPEATING"))
        assertEquals(AnimeTrackingUserStatus.ON_HOLD, AnimeTrackingUserStatus.fromMal("on_hold"))
        assertEquals(AnimeTrackingUserStatus.PLAN_TO_WATCH, AnimeTrackingUserStatus.fromMal("plan_to_watch"))
    }

    @Test
    fun mapsAniListDatesClearsAndAdvancedScores() {
        val variables = buildAniListEntryVariables(
            mediaId = 42,
            update = update(
                startDate = AnimeTrackingDate(2024, 2, 3),
                clearFinishDate = true,
                advancedScores = AnimeTrackingAdvancedScores(8.0, 7.0, 9.0, 6.0, 10.0),
            ),
        )

        assertEquals(2024, variables["startedAt"]!!.jsonObject["year"]!!.jsonPrimitive.content.toInt())
        assertEquals(JsonNull, variables["completedAt"]!!.jsonObject["year"])
        assertEquals("[8.0,7.0,9.0,6.0,10.0]", variables["advancedScores"].toString())
    }

    @Test
    fun buildsMalPayloadWithDiscreteScoreEncodedCommentsAndDateClears() {
        val payload = buildMalEntryPayload(
            update(
                score = 8.9,
                startDate = AnimeTrackingDate(2024, 2, 3),
                clearFinishDate = true,
                repeat = 2,
                notes = "one & two",
                priority = 1,
                rewatchValue = 4,
            ),
        )

        assertEquals(
            "status=watching&is_rewatching=false&num_watched_episodes=3&score=8" +
                "&start_date=2024-02-03&finish_date=" +
                "&num_times_rewatched=2&comments=one%20%26%20two&priority=1&rewatch_value=4",
            payload,
        )
        assertEquals(AnimeTrackingDate(2024, 2, 3), parseMalDate("2024-02-03"))
    }

    @Test
    fun mapsMalRewatchingAndConstrainsDiscreteFields() {
        val payload = buildMalEntryPayload(
            update(
                status = AnimeTrackingUserStatus.REWATCHING,
                score = 12.7,
                priority = 8,
                rewatchValue = -1,
            ),
        )

        assertEquals(
            "status=watching&is_rewatching=true&num_watched_episodes=3&score=10" +
                "&num_times_rewatched=0&comments=&priority=2&rewatch_value=0",
            payload,
        )
    }

    private fun update(
        status: AnimeTrackingUserStatus = AnimeTrackingUserStatus.WATCHING,
        score: Double = 7.5,
        startDate: AnimeTrackingDate? = null,
        clearFinishDate: Boolean = false,
        repeat: Int = 0,
        notes: String = "",
        priority: Int? = null,
        rewatchValue: Int? = null,
        advancedScores: AnimeTrackingAdvancedScores? = null,
    ) = AnimeTrackingEntryUpdate(
        status = status,
        progress = 3,
        score = score,
        startDate = startDate,
        clearFinishDate = clearFinishDate,
        repeat = repeat,
        notes = notes,
        priority = priority,
        rewatchValue = rewatchValue,
        advancedScores = advancedScores,
    )
}
