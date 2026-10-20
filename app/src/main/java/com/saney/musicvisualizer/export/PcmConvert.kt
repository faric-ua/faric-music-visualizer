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

/**
 * V89 (faric 20.10 19:24: з WAV тест 3 с дав звук 6,7 с — музика розтягнута й повільна).
 * Some phones report «16-bit» for decoded WAV while the data is 32-bit (float or int) —
 * then every sample is read as two and the sound plays twice as long, an octave lower.
 * The guard measures real bytes per sample from the timestamps of the first two buffers
 * (PTS come from the file, so they are right) and fixes the encoding from then on.
 */
class PcmEncodingGuard(
    /**
     * V99 (faric 21.10 02:10–02:30: у цілій пісні з MP3 візуалізація «стоїть»): only for raw PCM
     * (WAV through Android). A compressed decoder (MP3, AAC …) reports its format correctly, and its
     * first buffers are uneven (encoder delay trimmed), so measuring them used to pick a wrong format —
     * projectM then got broken PCM for the whole song.
     */
    private val enabled: Boolean = true,
) {
    private var lastPtsUs = -1L
    private var lastBytes = 0
    private val measurements = mutableListOf<Double>()
    private var decided = false
    private var override: Int? = null

    /** Encoding to use for this buffer. Call before converting each decoded buffer. */
    fun resolve(
        ptsUs: Long,
        bytes: Int,
        sample: ByteBuffer,
        reported: Int,
        sampleRate: Int,
        channels: Int,
    ): Int {
        if (!enabled) return reported
        if (!decided && lastPtsUs >= 0L && ptsUs > lastPtsUs && sampleRate > 0 && channels > 0) {
            val seconds = (ptsUs - lastPtsUs) / 1_000_000.0
            measurements += lastBytes / (seconds * sampleRate * channels)
            // Decide on the median of several buffers, never on one uneven first buffer.
            if (measurements.size >= MEASUREMENTS) {
                val median = measurements.sorted()[measurements.size / 2]
                val fixed = PcmConvert.resolveEncoding(reported, median, sample)
                if (fixed != reported) override = fixed
                decided = true
            }
        }
        lastPtsUs = ptsUs
        lastBytes = bytes
        return override ?: reported
    }

    /** Non-null once a wrong reported encoding was detected (for the export log). */
    val corrected: Int? get() = override

    private companion object {
        const val MEASUREMENTS = 5
    }
}

/** Bytes per sample for an AudioFormat.ENCODING_PCM_* value, 0 if unknown. */
fun PcmConvert.bytesPerSample(encoding: Int): Int =
    when (encoding) {
        PcmConvert.PCM_16BIT -> 2
        PcmConvert.PCM_8BIT -> 1
        PcmConvert.PCM_FLOAT, PcmConvert.PCM_32BIT -> 4
        PcmConvert.PCM_24BIT_PACKED -> 3
        else -> 0
    }

/**
 * Keeps [reported] when the measured bytes per sample agree with it; otherwise picks the
 * encoding that matches the measurement (4 bytes: float if the data looks like float audio).
 */
fun PcmConvert.resolveEncoding(
    reported: Int,
    measuredBytesPerSample: Double,
    sample: ByteBuffer,
): Int {
    if (!measuredBytesPerSample.isFinite()) return reported
    val expected = bytesPerSample(reported)
    if (expected > 0 && kotlin.math.abs(measuredBytesPerSample - expected) < 0.35) return reported
    return when (kotlin.math.round(measuredBytesPerSample).toInt()) {
        1 -> PcmConvert.PCM_8BIT
        2 -> PcmConvert.PCM_16BIT
        3 -> PcmConvert.PCM_24BIT_PACKED
        4 -> if (looksLikeFloatAudio(sample)) PcmConvert.PCM_FLOAT else PcmConvert.PCM_32BIT
        else -> reported
    }
}

/** Float audio: almost every value is 0 or between 1e-7 and 2 in size; 32-bit ints read as float are not. */
fun PcmConvert.looksLikeFloatAudio(sample: ByteBuffer): Boolean {
    val floats = sample.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    floats.position(sample.position())
    var total = 0
    var plausible = 0
    while (floats.remaining() >= 4 && total < 512) {
        val v = floats.float
        total++
        val a = kotlin.math.abs(v)
        if (v.isFinite() && (a == 0f || (a >= 1e-7f && a <= 2f))) plausible++
    }
    return total > 0 && plausible * 10 >= total * 9
}
