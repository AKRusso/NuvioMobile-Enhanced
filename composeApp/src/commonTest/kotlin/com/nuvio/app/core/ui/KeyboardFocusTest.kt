package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardFocusTest {
    @Test
    fun `physical keyboard starts keyboard input mode without tv layout`() {
        assertTrue(
            shouldStartInKeyboardInputMode(
                hasHardwareKeyboard = true,
                hasDpadNavigation = false,
                isTelevision = false,
            ),
        )
    }

    @Test
    fun `dpad and android tv start keyboard input mode`() {
        assertTrue(
            shouldStartInKeyboardInputMode(
                hasHardwareKeyboard = false,
                hasDpadNavigation = true,
                isTelevision = false,
            ),
        )
        assertTrue(
            shouldStartInKeyboardInputMode(
                hasHardwareKeyboard = false,
                hasDpadNavigation = false,
                isTelevision = true,
            ),
        )
    }

    @Test
    fun `touch only devices do not start keyboard input mode`() {
        assertFalse(
            shouldStartInKeyboardInputMode(
                hasHardwareKeyboard = false,
                hasDpadNavigation = false,
                isTelevision = false,
            ),
        )
    }
}
