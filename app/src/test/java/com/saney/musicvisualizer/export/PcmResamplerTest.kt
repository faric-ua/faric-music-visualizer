package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PcmResamplerTest {
    private fun sine(frames: Int, rate: Int, hz: Double, channels: Int = 2): ByteArray {
        val b = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            val v = (sin(2 * PI * hz * i / rate) * 12000).toInt().toShort()
            repeat(channels) { b.putShort(v) }
        }
        return b.array()
    }

    @Test
    fun rateChoice() {
        assertEquals(48_000, PcmResampler.aacRateFor(96_000))
        assertEquals(44_100, PcmResampler.aacRateFor(88_200))
        assertEquals(48_000, PcmResampler.aacRateFor(192_000))
        assertEquals(44_100, PcmResampler.aacRateFor(44_100))
        assertEquals(48_000, PcmResampler.aacRateFor(48_000))
    }

    @Test
    fun threeSecondsAt96kBecomeThreeSecondsAt48k_inChunks() {
        val r = PcmResampler(96_000, 48_000, 2)
        val all = sine(96_000 * 3, 96_000, 440.0)
        var outBytes = 0
        var offset = 0
        val chunk = 4096 * 4 + 12 // odd-sized chunks, whole frames
        while (offset < all.size) {
            val n = minOf(chunk, all.size - offset)
            outBytes += r.process(all.copyOfRange(offset, offset + n)).size
            offset += n
        }
        val outFrames = outBytes / 4
        assertTrue("frames=$outFrames", abs(outFrames - 48_000 * 3) <= 2)
    }

    @Test
    fun resampledSineKeepsItsPitch() {
        val r = PcmResampler(96_000, 44_100, 2)
        val out = r.process(sine(96_000, 96_000, 1000.0))
        val s = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frames = s.remaining() / 2
        var crossings = 0
        var prev = s.get(0).toInt()
        for (i in 1 until frames) {
            val v = s.get(i * 2).toInt()
            if (prev < 0 && v >= 0) crossings++
            prev = v
        }
        // 1 second of 1 kHz → ~1000 upward zero crossings at the new rate.
        assertTrue("crossings=$crossings", abs(crossings - 1000) <= 3)
    }

    @Test
    fun sameRateIsUntouched() {
        val r = PcmResampler(48_000, 48_000, 2)
        val a = sine(100, 48_000, 440.0)
        assertTrue(r.process(a) === a)
    }
}
