package com.saney.musicvisualizer.export

import java.io.Closeable
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * V89 (faric 20.10 22:44–22:45: один WAV тягнеться у 2 рази, інший — шипить). Android's own WAV
 * reading on the phone gave wrong data for some WAV files (96 kHz, 24 / 32-bit, float,
 * WAVE_FORMAT_EXTENSIBLE). PulseDeck now reads WAV itself: the RIFF header says exactly how the
 * samples are stored, and every format is turned into 16-bit PCM by [PcmConvert].
 * Not a WAV, or a kind we do not know (ADPCM, 64-bit float …) → null, Android reads it as before.
 * Pure Kotlin over an InputStream — tested on JVM.
 */
class WavPcmReader private constructor(
    private val input: InputStream,
    val sampleRate: Int,
    val channels: Int,
    /** PcmConvert.PCM_* of the samples in the file. */
    val encoding: Int,
    private val dataBytes: Long,
) : Closeable {
    private val bytesPerSample = PcmConvert.bytesPerSample(encoding)
    private val frameBytes = bytesPerSample * channels
    private var bytesRead = 0L

    /** Song length by the header (0 when the header does not say). */
    val durationUs: Long
        get() = if (dataBytes > 0) dataBytes / frameBytes * 1_000_000L / sampleRate else 0L

    /** Time of the next frame [read] returns. */
    val positionUs: Long
        get() = bytesRead / frameBytes * 1_000_000L / sampleRate

    /** Skips forward to [us] (whole frames). */
    fun skipToUs(us: Long) {
        val targetFrames = us.coerceAtLeast(0L) * sampleRate / 1_000_000L
        var toSkip = targetFrames * frameBytes - bytesRead
        if (dataBytes > 0) toSkip = toSkip.coerceAtMost(dataBytes - bytesRead)
        while (toSkip > 0) {
            val skipped = input.skip(toSkip)
            if (skipped <= 0) {
                if (input.read() < 0) break
                bytesRead += 1
                toSkip -= 1
            } else {
                bytesRead += skipped
                toSkip -= skipped
            }
        }
    }

    /**
     * Up to [maxFrames] frames as interleaved 16-bit little-endian PCM, or null at the end.
     */
    fun readPcm16(maxFrames: Int = 4096): ByteArray? {
        var want = maxFrames.toLong() * frameBytes
        if (dataBytes > 0) want = want.coerceAtMost(dataBytes - bytesRead)
        if (want <= 0) return null
        val buffer = ByteArray(want.toInt())
        var filled = 0
        while (filled < buffer.size) {
            val n = input.read(buffer, filled, buffer.size - filled)
            if (n < 0) break
            filled += n
        }
        val whole = filled / frameBytes * frameBytes
        if (whole <= 0) return null
        bytesRead += whole
        val raw = ByteBuffer.wrap(buffer, 0, whole).order(ByteOrder.LITTLE_ENDIAN)
        return if (encoding == PcmConvert.PCM_16BIT) buffer.copyOf(whole) else PcmConvert.toPcm16(raw, encoding)
    }

    override fun close() = input.close()

    companion object {
        private const val FORMAT_PCM = 1
        private const val FORMAT_FLOAT = 3
        private const val FORMAT_EXTENSIBLE = 0xFFFE

        /** Reads the header. Returns null (and does NOT close [input]) when this is not a WAV we can read. */
        fun open(input: InputStream): WavPcmReader? {
            val riff = ByteArray(12)
            if (!readFully(input, riff)) return null
            if (String(riff, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                String(riff, 8, 4, Charsets.US_ASCII) != "WAVE"
            ) return null
            var formatTag = -1
            var channels = 0
            var sampleRate = 0
            var bits = 0
            val header = ByteArray(8)
            while (true) {
                if (!readFully(input, header)) return null
                val id = String(header, 0, 4, Charsets.US_ASCII)
                val size = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                when (id) {
                    "fmt " -> {
                        if (size < 16 || size > 1024) return null
                        val fmt = ByteArray(size.toInt())
                        if (!readFully(input, fmt)) return null
                        val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        formatTag = b.getShort(0).toInt() and 0xFFFF
                        channels = b.getShort(2).toInt() and 0xFFFF
                        sampleRate = b.getInt(4)
                        bits = b.getShort(14).toInt() and 0xFFFF
                        if (formatTag == FORMAT_EXTENSIBLE) {
                            if (size < 40) return null
                            formatTag = b.getShort(24).toInt() and 0xFFFF // first bytes of the sub-format GUID
                        }
                        if (size % 2 == 1L) input.read()
                    }
                    "data" -> {
                        val encoding =
                            when {
                                formatTag == FORMAT_PCM && bits == 8 -> PcmConvert.PCM_8BIT
                                formatTag == FORMAT_PCM && bits == 16 -> PcmConvert.PCM_16BIT
                                formatTag == FORMAT_PCM && bits == 24 -> PcmConvert.PCM_24BIT_PACKED
                                formatTag == FORMAT_PCM && bits == 32 -> PcmConvert.PCM_32BIT
                                formatTag == FORMAT_FLOAT && bits == 32 -> PcmConvert.PCM_FLOAT
                                else -> return null
                            }
                        if (channels !in 1..8 || sampleRate !in 4_000..768_000) return null
                        // 0 or 0xFFFFFFFF = streamed / unknown length → read to the end of the file.
                        val dataBytes = if (size == 0L || size == 0xFFFFFFFFL) 0L else size
                        return WavPcmReader(input, sampleRate, channels, encoding, dataBytes)
                    }
                    else -> {
                        if (!skipFully(input, size + (size and 1L))) return null
                    }
                }
            }
        }

        private fun readFully(input: InputStream, target: ByteArray): Boolean {
            var filled = 0
            while (filled < target.size) {
                val n = input.read(target, filled, target.size - filled)
                if (n < 0) return false
                filled += n
            }
            return true
        }

        private fun skipFully(input: InputStream, bytes: Long): Boolean {
            var left = bytes
            while (left > 0) {
                val s = input.skip(left)
                if (s <= 0) {
                    if (input.read() < 0) return false
                    left -= 1
                } else {
                    left -= s
                }
            }
            return true
        }
    }
}

/** Opens [uri] as a WAV that PulseDeck reads itself (V89); null → let Android decode it. */
fun openWavPcmOrNull(
    context: android.content.Context,
    uri: android.net.Uri,
): WavPcmReader? {
    val stream =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            ?: return null
    val buffered = java.io.BufferedInputStream(stream, 256 * 1024)
    val reader = runCatching { WavPcmReader.open(buffered) }.getOrNull()
    if (reader == null) runCatching { buffered.close() }
    return reader
}
