package com.saney.musicvisualizer.export

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmConvertTest {
    private fun shorts(bytes: ByteArray): ShortArray {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return ShortArray(b.remaining()).also { b.get(it) }
    }

    @Test
    fun pcm16StaysTheSame() {
        val src = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putShort(1234).putShort(-5).flip() as ByteBuffer
        assertArrayEquals(shortArrayOf(1234, -5), shorts(PcmConvert.toPcm16(src, PcmConvert.PCM_16BIT)))
    }

    @Test
    fun pcm24KeepsTheTop16Bits() {
        // 0x123456 → 0x1234; -1 (0xFFFFFF) → -1; 0x800000 (min) → -32768
        val src = ByteBuffer.wrap(byteArrayOf(0x56, 0x34, 0x12, -1, -1, -1, 0, 0, -128))
        assertArrayEquals(shortArrayOf(0x1234, -1, -32768), shorts(PcmConvert.toPcm16(src, PcmConvert.PCM_24BIT_PACKED)))
    }

    @Test
    fun pcm32FloatAnd8BitConvert() {
        val i32 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0x12345678).flip() as ByteBuffer
        assertArrayEquals(shortArrayOf(0x1234), shorts(PcmConvert.toPcm16(i32, PcmConvert.PCM_32BIT)))
        val f = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(1f).putFloat(-2f).flip() as ByteBuffer
        assertArrayEquals(shortArrayOf(32767, -32767), shorts(PcmConvert.toPcm16(f, PcmConvert.PCM_FLOAT)))
        val u8 = ByteBuffer.wrap(byteArrayOf(-128, 0))
        assertArrayEquals(shortArrayOf(0, -32768), shorts(PcmConvert.toPcm16(u8, PcmConvert.PCM_8BIT)))
    }

    @Test
    fun supportedList() {
        assertEquals(true, PcmConvert.isSupported(21))
        assertEquals(false, PcmConvert.isSupported(99))
    }
}
