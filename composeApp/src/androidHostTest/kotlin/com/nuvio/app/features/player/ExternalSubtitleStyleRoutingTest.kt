package com.nuvio.app.features.player

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalSubtitleStyleRoutingTest {
    @Test
    fun `external ssa uses Media3 styling path`() {
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.TEXT_SSA)
            .build()

        assertTrue(shouldUseMedia3ParserForExternalSsa(format))
    }

    @Test
    fun `embedded Matroska ssa retains libass rendering`() {
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.TEXT_SSA)
            .setContainerMimeType(MimeTypes.VIDEO_MATROSKA)
            .build()

        assertFalse(shouldUseMedia3ParserForExternalSsa(format))
    }

    @Test
    fun `all ass codecs receive libmpv style override`() {
        assertTrue(isAssSubtitleCodec("ass"))
        assertTrue(isAssSubtitleCodec("SSA"))
        assertFalse(isAssSubtitleCodec("subrip"))
        assertFalse(isAssSubtitleCodec("hdmv_pgs_subtitle"))
        assertFalse(isAssSubtitleCodec(null))
    }
}
