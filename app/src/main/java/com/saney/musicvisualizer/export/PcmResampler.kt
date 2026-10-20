package com.saney.musicvisualizer.export

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * V89 (faric 20.10 22:44, файл Foiz_PRO_Lil_Jon___Eminem.wav): the WAV is 96 kHz. The phone's AAC
 * encoder does not take 96 kHz and quietly works at 44.1 kHz — 96 000 samples per second were
 * played as 44 100: the sound was 2,18× longer and an octave lower. Audio for the MP4 is now
 * brought to a rate every AAC encoder takes (48 / 44.1 kHz) before encoding.
 *
 * Streaming linear resampler for interleaved 16-bit PCM: chunks may have any size, the position
 * carries over between chunks. Pure Kotlin — tested on JVM.
 */
class PcmResampler(
    val sourceRate: Int,
    val targetRate: Int,
    private val channels: Int,
) {
    private val step = sourceRate.toDouble() / targetRate
    /** Position of the next output frame, in source frames, relative to [previous] (index -1) / chunk start (0). */
    private var position = 0.0
    private val previous = ShortArray(channels)
    private var hasPrevious = false

    val isPassThrough: Boolean get() = sourceRate == targetRate

    fun process(pcm16: ByteArray): ByteArray {
        if (isPassThrough || pcm16.isEmpty()) return pcm16
        val input = ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val frames = input.remaining() / channels
        if (frames == 0) return ByteArray(0)
        // Frame f of this chunk; f = -1 is the last frame of the previous chunk.
        fun sample(frame: Int, ch: Int): Int =
            if (frame < 0) previous[ch].toInt() else input.get(frame * channels + ch).toInt()
        val start = if (hasPrevious) -1.0 else 0.0
        if (position < start) position = start
        val estimated = ((frames - position) / step).toInt() + 2
        val out = ByteBuffer.allocate(estimated.coerceAtLeast(1) * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        // Interpolate between floor(position) and floor(position) + 1; both must be inside [-1, frames - 1].
        while (position <= frames - 1 && out.remaining() >= channels * 2) {
            val base = kotlin.math.floor(position).toInt()
            val frac = position - base
            val next = (base + 1).coerceAtMost(frames - 1)
            for (ch in 0 until channels) {
                val a = sample(base, ch)
                val b = sample(next, ch)
                out.putShort((a + (b - a) * frac).toInt().coerceIn(-32768, 32767).toShort())
            }
            position += step
        }
        for (ch in 0 until channels) previous[ch] = input.get((frames - 1) * channels + ch)
        hasPrevious = true
        // Next chunk: its frame 0 is this chunk's frame `frames`; the last frame becomes -1.
        position -= frames
        out.flip()
        return ByteArray(out.remaining()).also { out.get(it) }
    }

    companion object {
        private val AAC_SAFE_RATES = setOf(8_000, 11_025, 12_000, 16_000, 22_050, 24_000, 32_000, 44_100, 48_000)

        /** A rate every Android AAC encoder accepts: the same when it is one, else 44.1 or 48 kHz. */
        fun aacRateFor(sourceRate: Int): Int =
            when {
                sourceRate in AAC_SAFE_RATES -> sourceRate
                sourceRate % 11_025 == 0 -> 44_100
                else -> 48_000
            }
    }
}
