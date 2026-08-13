package com.nuvio.app.features.player

import java.io.File
import java.io.RandomAccessFile

/** Reads the embedded family name required by libass/fontconfig, not the file name. */
internal fun subtitleFontFamilyName(path: String): String? = runCatching {
    val file = File(path)
    if (!file.isFile || file.length() < 12L) return@runCatching null

    RandomAccessFile(file, "r").use { input ->
        input.seek(4L)
        val tableCount = input.readUnsignedShort()
        input.skipBytes(6)
        var nameTableOffset = -1L
        var nameTableLength = 0L
        repeat(tableCount) {
            val tag = input.readInt()
            input.readInt()
            val offset = input.readInt().toLong() and 0xFFFF_FFFFL
            val length = input.readInt().toLong() and 0xFFFF_FFFFL
            if (tag == NAME_TABLE_TAG) {
                nameTableOffset = offset
                nameTableLength = length
            }
        }
        if (nameTableOffset < 0L || nameTableLength < 6L || nameTableOffset + nameTableLength > input.length()) {
            return@use null
        }

        input.seek(nameTableOffset + 2L)
        val recordCount = input.readUnsignedShort()
        val stringOffset = input.readUnsignedShort().toLong()
        val recordsOffset = nameTableOffset + 6L
        val stringsOffset = nameTableOffset + stringOffset
        if (recordsOffset + recordCount * 12L > nameTableOffset + nameTableLength || stringsOffset > nameTableOffset + nameTableLength) {
            return@use null
        }

        val candidates = mutableListOf<EmbeddedFontName>()
        repeat(recordCount) {
            val platformId = input.readUnsignedShort()
            input.readUnsignedShort()
            val languageId = input.readUnsignedShort()
            val nameId = input.readUnsignedShort()
            val byteLength = input.readUnsignedShort()
            val relativeOffset = input.readUnsignedShort()
            if (nameId !in setOf(TYPOGRAPHIC_FAMILY_NAME_ID, FONT_FAMILY_NAME_ID)) return@repeat
            val valueOffset = stringsOffset + relativeOffset
            if (byteLength == 0 || valueOffset + byteLength > nameTableOffset + nameTableLength) return@repeat
            val returnPosition = input.filePointer
            input.seek(valueOffset)
            val bytes = ByteArray(byteLength)
            input.readFully(bytes)
            input.seek(returnPosition)
            val value = if (platformId == 0 || platformId == 3) {
                bytes.toString(Charsets.UTF_16BE)
            } else {
                bytes.toString(Charsets.ISO_8859_1)
            }.trim().takeIf { it.isNotBlank() && it.length <= 200 }
            value?.let {
                candidates += EmbeddedFontName(
                    value = it,
                    priority = when {
                        nameId == TYPOGRAPHIC_FAMILY_NAME_ID && languageId == ENGLISH_US_LANGUAGE_ID -> 0
                        nameId == TYPOGRAPHIC_FAMILY_NAME_ID -> 1
                        languageId == ENGLISH_US_LANGUAGE_ID -> 2
                        else -> 3
                    },
                )
            }
        }
        candidates.minByOrNull(EmbeddedFontName::priority)?.value
    }
}.getOrNull()

private data class EmbeddedFontName(
    val value: String,
    val priority: Int,
)

private const val NAME_TABLE_TAG = 0x6E616D65
private const val FONT_FAMILY_NAME_ID = 1
private const val TYPOGRAPHIC_FAMILY_NAME_ID = 16
private const val ENGLISH_US_LANGUAGE_ID = 0x0409
