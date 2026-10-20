package com.saney.musicvisualizer.export

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * V93 (faric 20.10 19:34: «теги з MP3 — назва, виконавець, альбом — переносити в MP4»).
 * Android's MediaMuxer cannot write song tags, so after muxing PulseDeck adds the usual
 * iTunes-style tags (moov/udta/meta/ilst: ©nam ©ART ©alb ©day ©gen ©too covr) that galleries,
 * players and YouTube read. MediaMuxer writes «ftyp · mdat · moov» with moov LAST, and the media
 * offsets (stco) point into mdat before it — so moov can be rewritten in place, nothing else moves.
 * If moov is not the last box, the file is left untouched. Pure Kotlin — tested on JVM.
 */
object Mp4Tagger {
    data class Tags(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val year: String? = null,
        val genre: String? = null,
        val cover: ByteArray? = null,
        val tool: String = "PulseDeck",
    ) {
        val isEmpty: Boolean
            get() = listOf(title, artist, album, year, genre).all { it.isNullOrBlank() } && cover == null
    }

    /**
     * Adds the tags to an MP4. [read] and [write] may be the same channel (a RandomAccessFile) or two
     * channels over one file descriptor (Android MediaStore: FileInputStream + FileOutputStream).
     * Returns false (file unchanged) when it cannot.
     */
    fun apply(read: FileChannel, write: FileChannel, tags: Tags): Boolean {
        val channel = read
        val size = channel.size()
        var offset = 0L
        var moovOffset = -1L
        var moovSize = 0L
        val header = ByteBuffer.allocate(16)
        while (offset + 8 <= size) {
            header.clear()
            header.limit(16)
            channel.read(header, offset)
            header.flip()
            if (header.remaining() < 8) return false
            var boxSize = header.int.toLong() and 0xFFFFFFFFL
            val type = String(ByteArray(4).also { header.get(it) }, Charsets.ISO_8859_1)
            if (boxSize == 1L) {
                if (header.remaining() < 8) return false
                boxSize = header.long
            } else if (boxSize == 0L) {
                boxSize = size - offset
            }
            if (boxSize < 8) return false
            if (type == "moov") {
                moovOffset = offset
                moovSize = boxSize
            }
            offset += boxSize
        }
        if (moovOffset < 0 || moovOffset + moovSize != size || moovSize > 64L * 1024 * 1024) return false
        val moov = ByteBuffer.allocate(moovSize.toInt())
        while (moov.hasRemaining()) {
            if (channel.read(moov, moovOffset + moov.position()) < 0) return false
        }
        val rewritten = rewriteMoov(moov.array(), tags) ?: return false
        val out = ByteBuffer.wrap(rewritten)
        var pos = moovOffset
        while (out.hasRemaining()) pos += write.write(out, pos)
        write.truncate(moovOffset + rewritten.size)
        write.force(true)
        return true
    }

    /** New moov bytes with our udta/meta/ilst; null if [moov] is not a plain moov box. */
    fun rewriteMoov(moov: ByteArray, tags: Tags): ByteArray? {
        if (moov.size < 8 || String(moov, 4, 4, Charsets.ISO_8859_1) != "moov") return null
        val children = children(moov, 8, moov.size) ?: return null
        val metaBox = ilstMeta(tags)
        val out = ByteArrayOutputStream()
        var udtaWritten = false
        for ((type, from, to) in children) {
            if (type == "udta") {
                val inner = children(moov, from + 8, to) ?: return null
                val udta = ByteArrayOutputStream()
                // Keep the phone's own udta items (Samsung SDLN / smta …), replace an older iTunes meta.
                for ((t, f, e) in inner) if (t != "meta") udta.write(moov, f, e - f)
                udta.write(metaBox)
                out.write(box("udta", udta.toByteArray()))
                udtaWritten = true
            } else {
                out.write(moov, from, to - from)
                if (type == "mvhd" && !hasChild(children, "udta")) {
                    out.write(box("udta", metaBox))
                    udtaWritten = true
                }
            }
        }
        if (!udtaWritten) out.write(box("udta", metaBox))
        return box("moov", out.toByteArray())
    }

    private data class Child(val type: String, val from: Int, val to: Int)

    private fun hasChild(children: List<Child>, type: String) = children.any { it.type == type }

    private fun children(bytes: ByteArray, start: Int, end: Int): List<Child>? {
        val list = mutableListOf<Child>()
        var off = start
        while (off + 8 <= end) {
            val size = ByteBuffer.wrap(bytes, off, 4).int.toLong() and 0xFFFFFFFFL
            if (size < 8 || off + size > end) return null
            list += Child(String(bytes, off + 4, 4, Charsets.ISO_8859_1), off, (off + size).toInt())
            off += size.toInt()
        }
        return if (off == end) list else null
    }

    private fun ilstMeta(tags: Tags): ByteArray {
        val ilst = ByteArrayOutputStream()
        fun text(code: String, value: String?) {
            val v = value?.trim().orEmpty()
            if (v.isEmpty()) return
            ilst.write(item(code, 1, v.toByteArray(Charsets.UTF_8)))
        }
        text("©nam", tags.title)
        text("©ART", tags.artist)
        text("©alb", tags.album)
        text("©day", tags.year)
        text("©gen", tags.genre)
        text("©too", tags.tool)
        tags.cover?.takeIf { it.size > 8 }?.let { cover ->
            val png = cover[0] == 0x89.toByte() && cover[1] == 'P'.code.toByte()
            ilst.write(item("covr", if (png) 14 else 13, cover))
        }
        val hdlr =
            box(
                "hdlr",
                ByteBuffer.allocate(25).apply {
                    putInt(0) // version + flags
                    putInt(0) // pre_defined
                    put("mdir".toByteArray(Charsets.ISO_8859_1))
                    put("appl".toByteArray(Charsets.ISO_8859_1))
                    putInt(0)
                    putInt(0)
                    put(0) // empty name
                }.array(),
            )
        val metaBody = ByteArrayOutputStream()
        metaBody.write(byteArrayOf(0, 0, 0, 0)) // full box version + flags
        metaBody.write(hdlr)
        metaBody.write(box("ilst", ilst.toByteArray()))
        return box("meta", metaBody.toByteArray())
    }

    private fun item(code: String, dataType: Int, payload: ByteArray): ByteArray {
        val data = ByteBuffer.allocate(8 + payload.size).apply {
            putInt(dataType) // version 0 + type (1 = UTF-8, 13 = JPEG, 14 = PNG)
            putInt(0) // locale
            put(payload)
        }.array()
        return box(code, box("data", data))
    }

    private fun box(type: String, body: ByteArray): ByteArray =
        ByteBuffer.allocate(8 + body.size).apply {
            putInt(8 + body.size)
            put(type.toByteArray(Charsets.ISO_8859_1))
            put(body)
        }.array()
}
