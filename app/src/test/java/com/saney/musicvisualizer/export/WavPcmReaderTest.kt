package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavPcmReaderTest {
    /** Builds a WAV like faric's files: optional EXTENSIBLE fmt, a LIST chunk before data. */
    private fun wav(rate: Int, channels: Int, bits: Int, tag: Int, frames: Int, extensible: Boolean, sample: (Int) -> ByteArray): ByteArray {
        val bytesPer = bits / 8
        val data = ByteBuffer.allocate(frames * channels * bytesPer).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) repeat(channels) { data.put(sample(i)) }
        val fmtSize = if (extensible) 40 else 16
        val fmt = ByteBuffer.allocate(fmtSize).order(ByteOrder.LITTLE_ENDIAN)
        fmt.putShort((if (extensible) 0xFFFE else tag).toShort()).putShort(channels.toShort()).putInt(rate)
            .putInt(rate * channels * bytesPer).putShort((channels * bytesPer).toShort()).putShort(bits.toShort())
        if (extensible) {
            fmt.putShort(22).putShort(bits.toShort()).putInt(3).putShort(tag.toShort())
            fmt.put(byteArrayOf(0, 0, 0, 0, 0x10, 0, 0x80.toByte(), 0, 0, 0xAA.toByte(), 0, 0x38, 0x9B.toByte(), 0x71))
        }
        val list = "INFOISFT\u0006\u0000\u0000\u0000Lavf\u0000\u0000".toByteArray(Charsets.ISO_8859_1)
        val out = ByteBuffer.allocate(12 + 8 + fmtSize + 8 + list.size + 8 + data.capacity()).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(out.capacity() - 8).put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(fmtSize).put(fmt.array())
        out.put("LIST".toByteArray()).putInt(list.size).put(list)
        out.put("data".toByteArray()).putInt(data.capacity()).put(data.array())
        return out.array()
    }

    private fun le(v: Int, bytes: Int) = ByteArray(bytes) { (v shr (8 * it)).toByte() }

    @Test
    fun extensible96kHz16BitLikeFaricsFile() {
        val bytes = wav(96_000, 2, 16, 1, 96_000, extensible = true) { le(1000, 2) }
        val r = WavPcmReader.open(ByteArrayInputStream(bytes))!!
        assertEquals(96_000, r.sampleRate)
        assertEquals(2, r.channels)
        assertEquals(PcmConvert.PCM_16BIT, r.encoding)
        assertEquals(1_000_000L, r.durationUs)
        r.skipToUs(500_000L)
        assertEquals(500_000L, r.positionUs)
        var frames = 0
        while (true) {
            val chunk = r.readPcm16(4096) ?: break
            frames += chunk.size / 4
        }
        assertEquals(48_000, frames)
    }

    @Test
    fun float32BecomesQuiet16BitNotNoise() {
        val bytes = wav(44_100, 2, 32, 3, 1000, extensible = false) { ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(0.25f).array() }
        val r = WavPcmReader.open(ByteArrayInputStream(bytes))!!
        assertEquals(PcmConvert.PCM_FLOAT, r.encoding)
        val pcm = r.readPcm16(1000)!!
        assertEquals(1000 * 2 * 2, pcm.size)
        val first = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        assertEquals(8192, first, 2)
    }

    @Test
    fun twentyFourBitExtensible() {
        val bytes = wav(48_000, 2, 24, 1, 100, extensible = true) { le(0x123456, 3) }
        val r = WavPcmReader.open(ByteArrayInputStream(bytes))!!
        assertEquals(PcmConvert.PCM_24BIT_PACKED, r.encoding)
        val pcm = r.readPcm16(100)!!
        assertEquals(100 * 2 * 2, pcm.size)
        assertEquals(0x1234, ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).short.toInt())
    }

    @Test
    fun notAWav() {
        assertNull(WavPcmReader.open(ByteArrayInputStream("ID3\u0004 mp3 data here".toByteArray())))
    }

    private fun assertEquals(expected: Int, actual: Int, delta: Int) =
        org.junit.Assert.assertTrue("$actual vs $expected", kotlin.math.abs(expected - actual) <= delta)
}
