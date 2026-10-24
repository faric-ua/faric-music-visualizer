package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class StoryTagParserTest {
    private fun syncSafe(n: Int) = byteArrayOf(((n shr 21) and 0x7f).toByte(), ((n shr 14) and 0x7f).toByte(), ((n shr 7) and 0x7f).toByte(), (n and 0x7f).toByte())

    private fun int32(n: Int) = byteArrayOf((n shr 24).toByte(), (n shr 16).toByte(), (n shr 8).toByte(), n.toByte())

    private fun frame(
        id: String,
        body: ByteArray,
        v4: Boolean,
    ): ByteArray = id.toByteArray(Charsets.ISO_8859_1) + (if (v4) syncSafe(body.size) else int32(body.size)) + byteArrayOf(0, 0) + body

    private fun tag(
        v4: Boolean,
        vararg frames: ByteArray,
    ): ByteArray {
        val body = ByteArrayOutputStream().apply { frames.forEach { write(it) }; write(ByteArray(16)) }.toByteArray()
        return "ID3".toByteArray() + byteArrayOf(if (v4) 4 else 3, 0, 0) + syncSafe(body.size) + body
    }

    private fun txxxUtf8(
        key: String,
        value: String,
    ) = byteArrayOf(3) + key.toByteArray() + byteArrayOf(0) + value.toByteArray()

    @Test fun readsUtf8TxxxInId3v24() {
        val story = "Смисл. Нічна дорога.\n\nЗвучання. 138 BPM."
        val parsed =
            StoryTagParser.parseId3(
                tag(true, frame("TIT2", byteArrayOf(3) + "Song".toByteArray(), true), frame("TXXX", txxxUtf8("FARIC_STORY_UK", story), true)),
            )
        assertEquals(story, parsed["FARIC_STORY_UK"])
    }

    @Test fun readsUtf16TxxxAndUsltInId3v23() {
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val txxx = byteArrayOf(1) + bom + "faric_story_en".toByteArray(Charsets.UTF_16LE) + byteArrayOf(0, 0) + bom + "Meaning. Night road.".toByteArray(Charsets.UTF_16LE)
        val uslt = byteArrayOf(3) + "eng".toByteArray() + byteArrayOf(0) + "La la la\nNa na".toByteArray()
        val parsed = StoryTagParser.parseId3(tag(false, frame("TXXX", txxx, false), frame("USLT", uslt, false)))
        assertEquals("Meaning. Night road.", parsed["FARIC_STORY_EN"])
        assertEquals("La la la\nNa na", parsed["LYRICS"])
    }

    @Test fun brokenOrForeignDataGivesNothing() {
        assertTrue(StoryTagParser.parseId3(ByteArray(3)).isEmpty())
        assertTrue(StoryTagParser.parseId3("ID3".toByteArray() + byteArrayOf(2, 0, 0, 0, 0, 0, 10) + ByteArray(10)).isEmpty())
        val cut = tag(true, frame("TXXX", txxxUtf8("FARIC_STORY_UK", "x".repeat(50)), true)).copyOf(30)
        assertTrue(StoryTagParser.parseId3(cut).isEmpty())
    }

    @Test fun readsVorbisComments() {
        fun le(n: Int) = byteArrayOf(n.toByte(), (n shr 8).toByte(), (n shr 16).toByte(), (n shr 24).toByte())
        val vendor = "ref".toByteArray()
        val entries = listOf("FARIC_ALBUM_STORY_UK=Про диск", "lyrics=Слова").map { it.toByteArray() }
        val block = le(vendor.size) + vendor + le(entries.size) + entries.flatMap { (le(it.size) + it).toList() }.toByteArray()
        val parsed = StoryTagParser.parseVorbisComments(block)
        assertEquals("Про диск", parsed["FARIC_ALBUM_STORY_UK"])
        assertEquals("Слова", parsed["LYRICS"])
    }

    @Test fun anEmptyFrameDoesNotHideTheStoryAfterIt() {
        // V193: Audio Converter «Лише теги» keeps old frames as they were (an empty one too) and adds the
        // story at the END of the tag — the reader must step over the empty frame.
        val story = "Смисл. Після порожнього кадру."
        for (v4 in listOf(false, true)) {
            val parsed =
                StoryTagParser.parseId3(
                    tag(v4, frame("TIT2", byteArrayOf(3) + "Song".toByteArray(), v4), frame("TPE1", ByteArray(0), v4), frame("TXXX", txxxUtf8("FARIC_STORY_UK", story), v4)),
                )
            assertEquals("v2." + (if (v4) 4 else 3), story, parsed["FARIC_STORY_UK"])
        }
    }

    @Test fun aV24TagWithPlainSizesStillReads() {
        val story = "Смисл. " + "довга історія ".repeat(20) // > 127 bytes: the sync-safe and plain sizes differ
        val bad = frame("TXXX", txxxUtf8("FARIC_STORY_UK", story), v4 = false) // plain size in a v2.4 tag
        val parsed = StoryTagParser.parseId3(tag(true, frame("TIT2", byteArrayOf(3) + "Song".toByteArray(), true), bad, frame("TXXX", txxxUtf8("FARIC_STORY_EN", "Meaning."), true)))
        assertEquals(story.trim(), parsed["FARIC_STORY_UK"])
        assertEquals("Meaning.", parsed["FARIC_STORY_EN"])
    }
}
