package com.saney.musicvisualizer.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class NativeTombstoneTest {
    private class Proto {
        val out = ByteArrayOutputStream()

        fun varint(v: Long) {
            var x = v
            while (true) {
                if (x and 0x7FL.inv() == 0L) {
                    out.write(x.toInt())
                    return
                }
                out.write(((x and 0x7F) or 0x80).toInt())
                x = x ushr 7
            }
        }

        fun int(field: Int, v: Long) = apply {
            varint((field shl 3 or 0).toLong())
            varint(v)
        }

        fun bytes(field: Int, b: ByteArray) = apply {
            varint((field shl 3 or 2).toLong())
            varint(b.size.toLong())
            out.write(b)
        }

        fun str(field: Int, s: String) = bytes(field, s.toByteArray())

        fun msg(field: Int, p: Proto) = bytes(field, p.out.toByteArray())

        fun fixed64(field: Int) = apply {
            varint((field shl 3 or 1).toLong())
            repeat(8) { out.write(0) }
        }
    }

    private fun frame(fn: String, off: Long, file: String) =
        Proto().int(1, 0x1234).int(2, 0x7000001234).str(4, fn).int(5, off).str(6, file).str(8, "abcdef0123456789")

    private fun thread(id: Long, name: String, vararg frames: Proto) =
        Proto().int(1, id).str(2, name).also { t -> frames.forEach { t.msg(4, it) } }

    @Test fun crashingThreadWithSignalAndFrames() {
        val tomb =
            Proto()
                .int(1, 3)
                .str(2, "samsung/a26/…")
                .int(5, 100)
                .int(6, 222)
                .fixed64(4)
                .msg(10, Proto().int(1, 11).str(2, "SIGSEGV").int(3, 1).str(4, "SEGV_MAPERR"))
                .msg(15, Proto().str(1, "null pointer dereference"))
                .msg(16, Proto().int(1, 100).msg(2, thread(100, "main", frame("epoll_wait", 8, "/apex/libc.so"))))
                .msg(
                    16,
                    Proto().int(1, 222).msg(
                        2,
                        thread(
                            222,
                            "RenderThread",
                            frame("SkImageShader::onAsFragmentProcessor", 40, "/system/lib64/libhwui.so"),
                            frame("", 0, "/vendor/lib64/egl/libGLES_mali.so"),
                        ),
                    ),
                ).str(17, "memory mappings are ignored")
        val text = NativeTombstone.summary(tomb.out.toByteArray())!!
        val lines = text.lines()
        assertEquals("SIGSEGV SEGV_MAPERR · потік RenderThread", lines[0])
        assertEquals("причина: null pointer dereference", lines[1])
        assertEquals("#00 libhwui.so  SkImageShader::onAsFragmentProcessor+40", lines[2])
        assertEquals("#01 libGLES_mali.so", lines[3])
        assertTrue(!text.contains("epoll_wait"))
    }

    @Test fun garbageGivesNull() {
        assertNull(NativeTombstone.summary(byteArrayOf(0x7F, 0x01, 0x02)))
        assertNull(NativeTombstone.summary(ByteArray(0)))
    }
}
