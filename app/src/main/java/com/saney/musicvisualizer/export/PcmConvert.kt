package com.saney.musicvisualizer.export

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Any decoded PCM → 16-bit little-endian (V56). WAV files are often 24-bit or 32-bit;
 * before this, analysis / projectM PCM / AAC knew only 16-bit and float and stopped
 * with «Unsupported PCM encoding». Pure java.nio — tested on JVM.
 * Encoding numbers are android.media.AudioFormat.ENCODING_PCM_*.
 */
object PcmConvert {
    const val PCM_16BIT = 2
    const val PCM_8BIT = 3
    const val PCM_FLOAT = 4
    const val PCM_24BIT_PACKED = 21
    const val PCM_32BIT = 22

    fun isSupported(encoding: Int): Boolean =
        encoding == PCM_16BIT || encoding == PCM_8BIT || encoding == PCM_FLOAT ||
            encoding == PCM_24BIT_PACKED || encoding == PCM_32BIT

    fun toPcm16(
        source: ByteBuffer,
        encoding: Int,
    ): ByteArray {
        val input = source.slice().order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample =
            when (encoding) {
                PCM_16BIT -> 2
                PCM_8BIT -> 1
                PCM_FLOAT, PCM_32BIT -> 4
                PCM_24BIT_PACKED -> 3
                else -> error("Unsupported PCM encoding: $encoding")
            }
        val samples = input.remaining() / bytesPerSample
        val out = ByteArray(samples * 2)
        val target = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        repeat(samples) {
            val value: Int =
                when (encoding) {
                    PCM_16BIT -> input.short.toInt()
                    PCM_8BIT -> ((input.get().toInt() and 0xff) - 128) shl 8
                    PCM_FLOAT -> (input.float.coerceIn(-1f, 1f) * 32767f).roundToInt()
                    PCM_32BIT -> input.int shr 16
                    else -> {
                        val b0 = input.get().toInt() and 0xff
                        val b1 = input.get().toInt() and 0xff
                        val b2 = input.get().toInt()
                        ((b2 shl 16) or (b1 shl 8) or b0) shr 8
                    }
                }
            target.putShort(value.coerceIn(-32768, 32767).toShort())
        }
        return out
    }
}
