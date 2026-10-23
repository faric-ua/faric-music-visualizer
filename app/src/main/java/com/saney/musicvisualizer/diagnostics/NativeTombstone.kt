package com.saney.musicvisualizer.diagnostics

/**
 * V168 (faric 23.10 19:17–19:19, v0.21.27: «падіння в C++ · код 11», двічі, при переході на центр):
 * the journal said only «crash». For a C++ crash Android 12+ keeps a «tombstone» — a protobuf with the
 * signal and the backtrace of the thread that fell. This reads just enough of it (no library) to write
 * «SIGSEGV · потік RenderThread · #00 libhwui.so …» into the journal, so the next crash names its place.
 *
 * Fields (AOSP system/core/debuggerd/proto/tombstone.proto): Tombstone 6 tid, 10 signal_info (Signal:
 * 2 name, 4 code_name), 11 abort_message, 15 causes (Cause: 1 human_readable), 16 threads (map entry:
 * 1 key, 2 Thread); Thread: 1 id, 2 name, 4 backtrace (BacktraceFrame: 4 function_name,
 * 5 function_offset, 6 file_name). JVM-testable (NativeTombstoneTest).
 */
object NativeTombstone {
    const val MAX_FRAMES = 14

    fun summary(bytes: ByteArray): String? =
        runCatching { summaryOrThrow(bytes) }.getOrNull()

    private class Frame(val function: String, val offset: Long, val file: String)

    private class Thread(val id: Long, val name: String, val frames: List<Frame>)

    private fun summaryOrThrow(bytes: ByteArray): String? {
        var tid = -1L
        var signal = ""
        var code = ""
        var abort = ""
        val causes = mutableListOf<String>()
        val threads = mutableListOf<Thread>()
        Reader(bytes, 0, bytes.size).forEachField { field, wire, r ->
            when {
                field == 6 && wire == 0 -> tid = r.varint()
                field == 10 && wire == 2 -> r.message { f, w, s ->
                    when {
                        f == 2 && w == 2 -> signal = s.string()
                        f == 4 && w == 2 -> code = s.string()
                        else -> s.skip(w)
                    }
                }
                field == 11 && wire == 2 -> abort = r.string()
                field == 15 && wire == 2 -> r.message { f, w, s -> if (f == 1 && w == 2) causes += s.string() else s.skip(w) }
                field == 16 && wire == 2 -> r.message { f, w, s -> if (f == 2 && w == 2) threads += readThread(s.bytesField()) else s.skip(w) }
                else -> r.skip(wire)
            }
        }
        if (signal.isBlank() && threads.isEmpty() && abort.isBlank()) return null
        val crashed = threads.firstOrNull { it.id == tid } ?: threads.firstOrNull()
        return buildString {
            append(listOf(signal, code).filter { it.isNotBlank() }.joinToString(" "))
            crashed?.let { append(" · потік ").append(it.name.ifBlank { it.id.toString() }) }
            append('\n')
            if (abort.isNotBlank()) append("abort: ").append(abort.take(300)).append('\n')
            causes.take(2).forEach { append("причина: ").append(it.take(200)).append('\n') }
            crashed?.frames?.take(MAX_FRAMES)?.forEachIndexed { i, f ->
                append('#').append(i.toString().padStart(2, '0')).append(' ')
                append(f.file.substringAfterLast('/').ifBlank { "?" })
                if (f.function.isNotBlank()) append("  ").append(f.function.take(120)).append('+').append(f.offset)
                append('\n')
            }
        }.trimEnd()
    }

    private fun readThread(r: Reader): Thread {
        var id = -1L
        var name = ""
        val frames = mutableListOf<Frame>()
        r.forEachField { f, w, s ->
            when {
                f == 1 && w == 0 -> id = s.varint()
                f == 2 && w == 2 -> name = s.string()
                f == 4 && w == 2 && frames.size < MAX_FRAMES -> {
                    var function = ""
                    var offset = 0L
                    var file = ""
                    s.message { ff, ww, fs ->
                        when {
                            ff == 4 && ww == 2 -> function = fs.string()
                            ff == 5 && ww == 0 -> offset = fs.varint()
                            ff == 6 && ww == 2 -> file = fs.string()
                            else -> fs.skip(ww)
                        }
                    }
                    frames += Frame(function, offset, file)
                }
                else -> s.skip(w)
            }
        }
        return Thread(id, name, frames)
    }

    /** Minimal protobuf reader over bytes[start, end). */
    private class Reader(val b: ByteArray, var pos: Int, val end: Int) {
        fun varint(): Long {
            var shift = 0
            var result = 0L
            while (true) {
                require(pos < end && shift < 64)
                val v = b[pos++].toInt() and 0xFF
                result = result or ((v and 0x7F).toLong() shl shift)
                if (v and 0x80 == 0) return result
                shift += 7
            }
        }

        fun bytesField(): Reader {
            val len = varint().toInt()
            require(len >= 0 && pos + len <= end)
            val sub = Reader(b, pos, pos + len)
            pos += len
            return sub
        }

        fun string(): String {
            val sub = bytesField()
            return String(b, sub.pos, sub.end - sub.pos, Charsets.UTF_8)
        }

        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> pos += 8
                2 -> bytesField()
                5 -> pos += 4
                else -> throw IllegalArgumentException("wire $wire")
            }
            require(pos <= end)
        }

        fun forEachField(body: (Int, Int, Reader) -> Unit) {
            while (pos < end) {
                val key = varint()
                body((key ushr 3).toInt(), (key and 7).toInt(), this)
            }
        }

        fun <T> message(body: (Int, Int, Reader) -> T) {
            bytesField().forEachField { f, w, s -> body(f, w, s) }
        }
    }
}
