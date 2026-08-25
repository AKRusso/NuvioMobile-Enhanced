package com.nuvio.app.features.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeroRefreshHapticPolicyTest {
    @Test
    fun `disabled policy never performs haptic even above threshold`() {
        val decision = heroRefreshHapticDecision(progress = 1.2f, enabled = false, armed = true)

        assertFalse(decision.shouldPerform)
        assertTrue(decision.armed)
    }

    @Test
    fun `crossing threshold performs once until pull resets`() {
        val threshold = heroRefreshHapticDecision(progress = 1f, enabled = true, armed = true)
        val held = heroRefreshHapticDecision(progress = 1.1f, enabled = true, armed = threshold.armed)
        val reset = heroRefreshHapticDecision(progress = 0.1f, enabled = true, armed = held.armed)
        val crossedAgain = heroRefreshHapticDecision(progress = 1f, enabled = true, armed = reset.armed)

        assertTrue(threshold.shouldPerform)
        assertFalse(held.shouldPerform)
        assertTrue(reset.armed)
        assertTrue(crossedAgain.shouldPerform)
    }

    @Test
    fun `hysteresis band preserves disarmed state`() {
        val decision = heroRefreshHapticDecision(progress = 0.5f, enabled = true, armed = false)

        assertFalse(decision.shouldPerform)
        assertFalse(decision.armed)
    }
}
