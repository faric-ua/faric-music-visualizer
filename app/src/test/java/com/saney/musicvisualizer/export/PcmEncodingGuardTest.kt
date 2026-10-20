package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class PcmEncodingGuardTest {
    private fun floatAudio(frames: Int, channels: Int): ByteBuffer {
        val b = ByteBuffer.allocate(frames * channels * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames * channels) b.putFloat((0.4 * sin(i * 0.05)).toFloat())
        b.flip(); return b
    }

    private fun int32Audio(frames: Int, channels: Int): ByteBuffer {
        val b = ByteBuffer.allocate(frames * channels * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames * channels) b.putInt((0.4 * sin(i * 0.05) * Int.MAX_VALUE).toInt())
        b.flip(); return b
    }

    private fun pcm16(frames: Int, channels: Int): ByteBuffer {
        val b = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames * channels) b.putShort((0.4 * sin(i * 0.05) * 32767).toInt().toShort())
        b.flip(); return b
    }

    /** 1024 frames at 44.1 kHz = 23 219 µs. */
    private val frameUs = 1024 * 1_000_000L / 44_100

    @Test
    fun floatWavReportedAs16BitIsFixedFromSecondBuffer() {
        val guard = PcmEncodingGuard()
        val buf = floatAudio(1024, 2)
        assertEquals(PcmConvert.PCM_FLOAT, feed(guard, buf, PcmConvert.PCM_16BIT))
        assertEquals(PcmConvert.PCM_FLOAT, guard.corrected)
        // stays fixed for the rest of the song
        assertEquals(PcmConvert.PCM_FLOAT, guard.resolve(100 * frameUs, buf.remaining(), buf, PcmConvert.PCM_16BIT, 44_100, 2))
    }

    @Test
    fun int32WavReportedAs16BitBecomes32Bit() {
        val guard = PcmEncodingGuard()
        val buf = int32Audio(1024, 2)
        assertEquals(PcmConvert.PCM_32BIT, feed(guard, buf, PcmConvert.PCM_16BIT))
    }

    @Test
    fun correct16BitIsLeftAlone() {
        val guard = PcmEncodingGuard()
        val buf = pcm16(1024, 2)
        assertEquals(PcmConvert.PCM_16BIT, feed(guard, buf, PcmConvert.PCM_16BIT))
        assertNull(guard.corrected)
    }

    @Test
    fun twentyFourBitReportedAs16BitIsFixed() {
        val guard = PcmEncodingGuard()
        val buf = ByteBuffer.allocate(1024 * 2 * 3).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(PcmConvert.PCM_24BIT_PACKED, feed(guard, buf, PcmConvert.PCM_16BIT))
    }

    /** Feeds 8 equal buffers (1024 frames each) and returns the encoding used for the last one. */
    private fun feed(guard: PcmEncodingGuard, buf: ByteBuffer, reported: Int): Int {
        var result = reported
        for (i in 0 until 8) result = guard.resolve(i * frameUs, buf.remaining(), buf, reported, 44_100, 2)
        return result
    }

    @Test
    fun mp3LikeShortFirstBufferDoesNotFoolTheGuard() {
        // V99: an MP3 decoder's first buffer is shorter (encoder delay trimmed) but has a full frame's time.
        val guard = PcmEncodingGuard()
        val full = pcm16(1152, 2)
        val first = pcm16(1152 - 529, 2)
        var used = guard.resolve(0L, first.remaining(), first, PcmConvert.PCM_16BIT, 44_100, 2)
        val step = 1152 * 1_000_000L / 44_100
        for (i in 1 until 10) used = guard.resolve(i * step, full.remaining(), full, PcmConvert.PCM_16BIT, 44_100, 2)
        assertEquals(PcmConvert.PCM_16BIT, used)
        assertNull(guard.corrected)
    }

    @Test
    fun disabledForCompressedSources() {
        val guard = PcmEncodingGuard(enabled = false)
        val buf = floatAudio(1024, 2)
        assertEquals(PcmConvert.PCM_16BIT, feed(guard, buf, PcmConvert.PCM_16BIT))
    }

    @Test
    fun floatDetection() {
        assertEquals(true, PcmConvert.looksLikeFloatAudio(floatAudio(256, 2)))
        assertEquals(false, PcmConvert.looksLikeFloatAudio(int32Audio(256, 2)))
    }
}
