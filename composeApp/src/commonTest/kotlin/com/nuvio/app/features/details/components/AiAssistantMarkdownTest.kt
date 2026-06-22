package com.nuvio.app.features.details.components

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class AiAssistantMarkdownTest {
    @Test
    fun rendersBoldMarkersWithoutShowingAsterisks() {
        val result = parseAiBoldMarkdown("**Title:** The Simpsons\n- **Genre:** Comedy")

        assertEquals("Title: The Simpsons\n- Genre: Comedy", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)
        assertEquals("Title:", result.text.substring(result.spanStyles[0].start, result.spanStyles[0].end))
        assertEquals("Genre:", result.text.substring(result.spanStyles[1].start, result.spanStyles[1].end))
    }

    @Test
    fun keepsUnclosedMarkersAsPlainText() {
        val result = parseAiBoldMarkdown("Normal **unfinished")

        assertEquals("Normal **unfinished", result.text)
        assertEquals(0, result.spanStyles.size)
    }
}
