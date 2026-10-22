package com.saney.musicvisualizer.export

import android.content.Context
import android.net.Uri
import com.saney.musicvisualizer.ui.SongStoryTags
import java.io.DataInputStream
import java.io.InputStream

/**
 * V135: the story and the words from the song file itself (only reads; the file is never changed).
 * MP3 → the ID3v2 tag at the start; FLAC → its VORBIS_COMMENT block. Other formats → nothing yet.
 */
object SongStoryReader {
    private const val MAX_TAG_BYTES = 24 * 1024 * 1024
    private const val MAX_COMMENT_BYTES = 2 * 1024 * 1024

    fun read(
        context: Context,
        uri: Uri,
    ): SongStoryTags =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream -> SongStoryTags.fromTags(readTags(DataInputStream(stream.buffered()))) }
        }.getOrNull() ?: SongStoryTags.EMPTY

    private fun readTags(input: DataInputStream): Map<String, String> {
        val head = ByteArray(10)
        input.readFully(head, 0, 4)
        return when {
            head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte() -> {
                input.readFully(head, 4, 6)
                val size = StoryTagParser.syncSafe(head, 6)
                if (size <= 0 || size > MAX_TAG_BYTES) return emptyMap()
                val tag = ByteArray(10 + size)
                System.arraycopy(head, 0, tag, 0, 10)
                input.readFully(tag, 10, size)
                StoryTagParser.parseId3(tag)
            }
            String(head, 0, 4, Charsets.ISO_8859_1) == "fLaC" -> readFlac(input)
            else -> emptyMap()
        }
    }

    private fun readFlac(input: DataInputStream): Map<String, String> {
        val header = ByteArray(4)
        repeat(64) {
            input.readFully(header)
            val last = header[0].toInt() and 0x80 != 0
            val type = header[0].toInt() and 0x7f
            val length = ((header[1].toInt() and 0xff) shl 16) or ((header[2].toInt() and 0xff) shl 8) or (header[3].toInt() and 0xff)
            if (type == 4 && length in 8..MAX_COMMENT_BYTES) {
                val block = ByteArray(length)
                input.readFully(block)
                return StoryTagParser.parseVorbisComments(block)
            }
            skipFully(input, length.toLong())
            if (last) return emptyMap()
        }
        return emptyMap()
    }

    private fun skipFully(
        input: InputStream,
        count: Long,
    ) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) {
                if (input.read() < 0) return
                left--
            } else {
                left -= skipped
            }
        }
    }
}
