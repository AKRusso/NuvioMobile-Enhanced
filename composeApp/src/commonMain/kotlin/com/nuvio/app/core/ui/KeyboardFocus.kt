package com.nuvio.app.core.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal val LocalNuvioKeyboardInput = staticCompositionLocalOf { false }
private val LocalNuvioFocusMemory = staticCompositionLocalOf<NuvioFocusMemory?> { null }

private class NuvioFocusMemory {
    var lastContentFocusRequester: FocusRequester? = null
}

internal val NuvioKeyboardFocused = SemanticsPropertyKey<Boolean>("NuvioKeyboardFocused")
internal var SemanticsPropertyReceiver.nuvioKeyboardFocused by NuvioKeyboardFocused

@Composable
internal fun NuvioKeyboardInputProvider(content: @Composable () -> Unit) {
    var keyboardInput by remember { mutableStateOf(false) }
    val focusMemory = remember { NuvioFocusMemory() }

    CompositionLocalProvider(
        LocalNuvioKeyboardInput provides keyboardInput,
        LocalNuvioFocusMemory provides focusMemory,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) keyboardInput = true
                    false
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        keyboardInput = false
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
internal fun Modifier.nuvioKeyboardFocusIndicator(
    shape: Shape,
    enabled: Boolean = true,
    rememberAsContentFocus: Boolean = true,
): Modifier {
    if (!enabled) return this

    val keyboardInput = LocalNuvioKeyboardInput.current
    val focusMemory = LocalNuvioFocusMemory.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val showIndicator = keyboardInput && focused

    return focusRequester(focusRequester)
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused && rememberAsContentFocus) {
                focusMemory?.lastContentFocusRequester = focusRequester
            }
        }
        .then(
            if (showIndicator) {
                Modifier
                    .border(3.dp, androidx.compose.material3.MaterialTheme.nuvio.colors.focusRing, shape)
                    .semantics { nuvioKeyboardFocused = true }
            } else {
                Modifier
            },
        )
}

internal fun Modifier.nuvioExcludeFromKeyboardFocus(): Modifier =
    focusProperties { canFocus = false }

@Composable
internal fun Modifier.nuvioRestoreLastContentFocusOnUp(): Modifier =
    nuvioRestoreLastContentFocusOn(Key.DirectionUp)

@Composable
internal fun Modifier.nuvioRestoreLastContentFocusOnDown(): Modifier =
    nuvioRestoreLastContentFocusOn(Key.DirectionDown)

@Composable
internal fun Modifier.nuvioRestoreLastContentFocusOnRight(): Modifier =
    nuvioRestoreLastContentFocusOn(Key.DirectionRight)

@Composable
private fun Modifier.nuvioRestoreLastContentFocusOn(exitKey: Key): Modifier {
    val focusMemory = LocalNuvioFocusMemory.current
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.key != exitKey) {
            false
        } else {
            focusMemory?.lastContentFocusRequester
                ?.let { requester -> runCatching { requester.requestFocus() }.getOrDefault(false) }
                ?: false
        }
    }
}
