package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class Mp4TaggerTest {
    private fun box(type: String, body: ByteArray = ByteArray(0)): ByteArray =
        ByteBuffer.allocate(8 + body.size).putInt(8 + body.size).put(type.toByteArray(Charsets.ISO_8859_1)).put(body).array()

    private fun mp4(moovLast: Boolean): ByteArray {
        val ftyp = box("ftyp", "mp42".toByteArray() + ByteArray(4))
        val mdat = box("mdat", ByteArray(100) { it.toByte() })
        val moov = box("moov", box("mvhd", ByteArray(100)) + box("udta", box("SDLN", ByteArray(8))) + box("trak", ByteArray(50)))
        return if (moovLast) ftyp + mdat + moov else ftyp + moov + mdat
    }

    private fun count(haystack: ByteArray, needle: String): Int {
        val n = needle.toByteArray(Charsets.ISO_8859_1)
        return (0..haystack.size - n.size).count { i -> n.indices.all { haystack[i + it] == n[it] } }
    }

    private fun tagFile(bytes: ByteArray, tags: Mp4Tagger.Tags): Pair<Boolean, ByteArray> {
        val f = File.createTempFile("pulsedeck", ".mp4")
        f.writeBytes(bytes)
        val ok = RandomAccessFile(f, "rw").use { Mp4Tagger.apply(it.channel, it.channel, tags) }
        return ok to f.readBytes().also { f.delete() }
    }

    @Test
    fun addsTagsKeepsMdatAndPhoneUdta() {
        val src = mp4(moovLast = true)
        val (ok, out) = tagFile(src, Mp4Tagger.Tags(title = "Shivers", artist = "Ed Sheeran", album = "="))
        assertTrue(ok)
        // ftyp + mdat untouched byte for byte
        assertTrue(out.copyOfRange(0, 124).contentEquals(src.copyOfRange(0, 124)))
        assertEquals(1, count(out, "SDLN"))
        assertEquals(1, count(out, "ilst"))
        assertEquals(1, count(out, "Shivers"))
        assertEquals(out.size - 124, ByteBuffer.wrap(out, 124, 4).int)
    }

    @Test
    fun taggingTwiceReplacesNotDuplicates() {
        val (_, once) = tagFile(mp4(moovLast = true), Mp4Tagger.Tags(title = "A"))
        val (ok, twice) = tagFile(once, Mp4Tagger.Tags(title = "B"))
        assertTrue(ok)
        assertEquals(1, count(twice, "ilst"))
        assertEquals(0, count(twice, "\u0000A"))
    }

    @Test
    fun moovNotLastIsLeftAlone() {
        val src = mp4(moovLast = false)
        val (ok, out) = tagFile(src, Mp4Tagger.Tags(title = "X"))
        assertFalse(ok)
        assertTrue(out.contentEquals(src))
    }

    @Test
    fun albumFolder() {
        assertEquals("Movies/PulseDeck/FARIC Vol. 3/", BatchPlan.albumRelativePath("FARIC Vol. 3"))
        assertEquals("Movies/PulseDeck/AC DC/", BatchPlan.albumRelativePath("AC/DC"))
        assertEquals(
            "Movies/PulseDeck/White Side/Тест 10с 21.10 01-25/White Side/",
            BatchPlan.albumSubfolder("Movies/PulseDeck/White Side/Тест 10с 21.10 01-25/", "White Side"),
        )
    }
}
