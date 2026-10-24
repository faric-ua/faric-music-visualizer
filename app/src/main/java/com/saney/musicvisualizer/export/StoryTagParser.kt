package com.saney.musicvisualizer.export

import java.nio.charset.Charset

/**
 * V135: reads the text tags Android's MediaMetadataRetriever does not give — the FARIC story
 * (ID3 TXXX / Vorbis comment) and the song's words (ID3 USLT / Vorbis LYRICS). Pure Kotlin on bytes,
 * tested in StoryTagParserTest. Keys come back upper-case; TXXX by its description.
 */
object StoryTagParser {
    /** [bytes] — the whole ID3v2 tag from the file start (header included). */
    fun parseId3(bytes: ByteArray): Map<String, String> {
        if (bytes.size < 10 || bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) return emptyMap()
        val major = bytes[3].toInt()
        if (major != 3 && major != 4) return emptyMap()
        val flags = bytes[5].toInt()
        val tagEnd = minOf(bytes.size, 10 + syncSafe(bytes, 6))
        var pos = 10
        if (flags and 0x40 != 0 && pos + 4 <= tagEnd) {
            // Extended header: v2.4 size is sync-safe and includes itself; v2.3 excludes its 4 size bytes.
            pos += if (major == 4) syncSafe(bytes, pos) else int32(bytes, pos) + 4
        }
        val out = linkedMapOf<String, String>()
        while (pos + 10 <= tagEnd) {
            val id = String(bytes, pos, 4, Charsets.ISO_8859_1)
            if (!isFrameId(id)) break // padding (zeros) or the end of what we understand
            var size = if (major == 4) syncSafe(bytes, pos + 4) else int32(bytes, pos + 4)
            // V193: some writers put plain 32-bit sizes into a v2.4 tag — if the sync-safe one does not land on
            // the next frame (or the end), the plain one is tried.
            if (major == 4 && !landsWell(bytes, pos + 10 + size, tagEnd)) {
                val plain = int32(bytes, pos + 4)
                if (plain >= 0 && landsWell(bytes, pos + 10 + plain, tagEnd)) size = plain
            }
            val start = pos + 10
            val end = start + size
            // V193 (faric 24.10: «історії не видно» — розбір: кадр розміром 0 зупиняв читання, а конвертер
            // дописує історії в КІНЕЦЬ тегу): an empty frame is skipped, not the end of the tag.
            if (size == 0) {
                pos = start
                continue
            }
            if (size < 0 || end > tagEnd) break
            when (id) {
                "TXXX" -> parseTxxx(bytes, start, end)?.let { (key, value) -> out.putIfAbsent(key, value) }
                "USLT" -> parseUslt(bytes, start, end)?.let { out.putIfAbsent("LYRICS", it) }
            }
            pos = end
        }
        return out
    }

    /** A frame id: 4 of A–Z / 0–9. */
    private fun isFrameId(id: String): Boolean = id.length == 4 && id.all { it in 'A'..'Z' || it in '0'..'9' }

    private fun landsWell(
        b: ByteArray,
        at: Int,
        tagEnd: Int,
    ): Boolean =
        when {
            at == tagEnd -> true
            at > tagEnd || at < 0 -> false
            at + 4 > tagEnd -> b.copyOfRange(at, tagEnd).all { it.toInt() == 0 }
            b[at].toInt() == 0 -> true // padding
            else -> isFrameId(String(b, at, 4, Charsets.ISO_8859_1))
        }

    /** [block] — the body of a FLAC VORBIS_COMMENT block (or an Ogg comment packet without its header). */
    fun parseVorbisComments(block: ByteArray): Map<String, String> {
        val out = linkedMapOf<String, String>()
        runCatching {
            var pos = 0
            val vendor = le32(block, pos)
            pos += 4 + vendor
            val count = le32(block, pos)
            pos += 4
            repeat(count) {
                val len = le32(block, pos)
                pos += 4
                if (len < 0 || pos + len > block.size) return out
                val entry = String(block, pos, len, Charsets.UTF_8)
                pos += len
                val eq = entry.indexOf('=')
                if (eq > 0) {
                    val value = entry.substring(eq + 1).trim()
                    if (value.isNotEmpty()) out.putIfAbsent(entry.substring(0, eq).uppercase(), value)
                }
            }
        }
        return out
    }

    fun syncSafe(
        b: ByteArray,
        at: Int,
    ): Int = ((b[at].toInt() and 0x7f) shl 21) or ((b[at + 1].toInt() and 0x7f) shl 14) or ((b[at + 2].toInt() and 0x7f) shl 7) or (b[at + 3].toInt() and 0x7f)

    private fun int32(
        b: ByteArray,
        at: Int,
    ): Int = ((b[at].toInt() and 0xff) shl 24) or ((b[at + 1].toInt() and 0xff) shl 16) or ((b[at + 2].toInt() and 0xff) shl 8) or (b[at + 3].toInt() and 0xff)

    fun le32(
        b: ByteArray,
        at: Int,
    ): Int = (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8) or ((b[at + 2].toInt() and 0xff) shl 16) or ((b[at + 3].toInt() and 0xff) shl 24)

    private fun parseTxxx(
        b: ByteArray,
        start: Int,
        end: Int,
    ): Pair<String, String>? {
        if (end - start < 2) return null
        val enc = b[start].toInt()
        val descEnd = terminator(b, start + 1, end, enc)
        val key = decode(b, start + 1, descEnd, enc).trim().uppercase()
        val value = decode(b, skipTerminator(descEnd, end, enc), end, enc).trim().replace("\u0000", "\n").trim()
        return if (key.isEmpty() || value.isEmpty()) null else key to value
    }

    private fun parseUslt(
        b: ByteArray,
        start: Int,
        end: Int,
    ): String? {
        if (end - start < 5) return null
        val enc = b[start].toInt()
        val descStart = start + 4 // encoding + 3-letter language
        val descEnd = terminator(b, descStart, end, enc)
        return decode(b, skipTerminator(descEnd, end, enc), end, enc).trim().takeIf { it.isNotEmpty() }
    }

    private fun isWide(enc: Int) = enc == 1 || enc == 2

    private fun terminator(
        b: ByteArray,
        from: Int,
        end: Int,
        enc: Int,
    ): Int {
        if (isWide(enc)) {
            var i = from
            while (i + 1 < end) {
                if (b[i].toInt() == 0 && b[i + 1].toInt() == 0) return i
                i += 2
            }
            return end
        }
        for (i in from until end) if (b[i].toInt() == 0) return i
        return end
    }

    private fun skipTerminator(
        at: Int,
        end: Int,
        enc: Int,
    ): Int = minOf(end, at + if (isWide(enc)) 2 else 1)

    private fun decode(
        b: ByteArray,
        from: Int,
        to: Int,
        enc: Int,
    ): String {
        if (to <= from) return ""
        val charset: Charset =
            when (enc) {
                1 -> Charsets.UTF_16 // with BOM
                2 -> Charsets.UTF_16BE
                3 -> Charsets.UTF_8
                else -> Charsets.ISO_8859_1
            }
        return String(b, from, to - from, charset).trimEnd('\u0000')
    }
}
