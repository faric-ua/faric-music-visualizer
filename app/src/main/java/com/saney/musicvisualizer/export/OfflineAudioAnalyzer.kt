package com.saney.musicvisualizer.export

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.saney.musicvisualizer.analysis.SceneSignal
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln1p
import kotlin.math.sqrt

data class OfflineAnalysisResult(
    val durationMs: Long,
    val frameIntervalMs: Long,
    val signals: List<SceneSignal>,
) {
    fun signalAt(
        timeMs: Long,
    ): SceneSignal {
        if (signals.isEmpty()) {
            return SceneSignal(
                amplitude = 0f,
                bass = 0f,
                mid = 0f,
                high = 0f,
                beatStrength = 0f,
            )
        }

        val index =
            (
                timeMs
                    .coerceAtLeast(0L)
                    .toDouble() /
                    frameIntervalMs
                        .coerceAtLeast(1L)
                        .toDouble()
                )
                .toInt()
                .coerceIn(
                    0,
                    signals.lastIndex,
                )

        return signals[index]
    }
}

object OfflineAudioAnalyzer {
    private const val FFT_SIZE = 2048
    private const val HOP_SIZE = 1024
    private const val TIMEOUT_US = 10_000L

    private data class RawFrame(
        val amplitude: Float,
        val bass: Float,
        val mid: Float,
        val high: Float,
    )

    fun analyze(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit = {},
    ): OfflineAnalysisResult {
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(
                context,
                uri,
                null,
            )

            val trackIndex =
                (0 until extractor.trackCount)
                    .firstOrNull { index ->
                        extractor
                            .getTrackFormat(index)
                            .getString(
                                MediaFormat.KEY_MIME,
                            )
                            ?.startsWith("audio/") ==
                            true
                    }
                    ?: error(
                        "У файлі немає audio track",
                    )

            extractor.selectTrack(trackIndex)

            val inputFormat =
                extractor.getTrackFormat(
                    trackIndex,
                )

            val mime =
                inputFormat.getString(
                    MediaFormat.KEY_MIME,
                )
                    ?: error(
                        "Audio MIME відсутній",
                    )

            val sourceDurationUs =
                if (
                    inputFormat.containsKey(
                        MediaFormat.KEY_DURATION,
                    )
                ) {
                    inputFormat.getLong(
                        MediaFormat.KEY_DURATION,
                    )
                } else {
                    0L
                }

            val decoder =
                MediaCodec.createDecoderByType(
                    mime,
                )

            try {
                decoder.configure(
                    inputFormat,
                    null,
                    null,
                    0,
                )
                decoder.start()

                val info =
                    MediaCodec.BufferInfo()

                var inputDone = false
                var outputDone = false

                var sampleRate =
                    inputFormat
                        .getIntegerOrDefault(
                            MediaFormat.KEY_SAMPLE_RATE,
                            44_100,
                        )

                var channelCount =
                    inputFormat
                        .getIntegerOrDefault(
                            MediaFormat.KEY_CHANNEL_COUNT,
                            2,
                        )

                var pcmEncoding =
                    AudioFormat.ENCODING_PCM_16BIT

                val rawFrames =
                    ArrayList<RawFrame>()

                var accumulator =
                    PcmAccumulator(
                        sampleRate = sampleRate,
                        onFrame = rawFrames::add,
                    )

                var lastProgress = -1

                while (!outputDone) {
                    if (!inputDone) {
                        val inputIndex =
                            decoder.dequeueInputBuffer(
                                TIMEOUT_US,
                            )

                        if (inputIndex >= 0) {
                            val inputBuffer =
                                decoder.getInputBuffer(
                                    inputIndex,
                                )
                                    ?: error(
                                        "Decoder input buffer unavailable",
                                    )

                            val size =
                                extractor.readSampleData(
                                    inputBuffer,
                                    0,
                                )

                            if (size < 0) {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                val ptsUs =
                                    extractor.sampleTime
                                        .coerceAtLeast(
                                            0L,
                                        )

                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    ptsUs,
                                    0,
                                )

                                if (
                                    sourceDurationUs > 0L
                                ) {
                                    val progress =
                                        (
                                            ptsUs *
                                                100L /
                                                sourceDurationUs
                                            )
                                            .toInt()
                                            .coerceIn(
                                                0,
                                                99,
                                            )

                                    if (
                                        progress !=
                                        lastProgress
                                    ) {
                                        lastProgress =
                                            progress
                                        onProgress(
                                            progress,
                                        )
                                    }
                                }

                                extractor.advance()
                            }
                        }
                    }

                    when (
                        val outputIndex =
                            decoder.dequeueOutputBuffer(
                                info,
                                TIMEOUT_US,
                            )
                    ) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // Decoder has no output yet.
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat =
                                decoder.outputFormat

                            sampleRate =
                                outputFormat
                                    .getIntegerOrDefault(
                                        MediaFormat.KEY_SAMPLE_RATE,
                                        sampleRate,
                                    )

                            channelCount =
                                outputFormat
                                    .getIntegerOrDefault(
                                        MediaFormat.KEY_CHANNEL_COUNT,
                                        channelCount,
                                    )

                            pcmEncoding =
                                outputFormat
                                    .getIntegerOrDefault(
                                        MediaFormat.KEY_PCM_ENCODING,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                    )

                            accumulator =
                                PcmAccumulator(
                                    sampleRate =
                                        sampleRate,
                                    onFrame =
                                        rawFrames::add,
                                )
                        }

                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                            // Deprecated buffer-array API only.
                        }

                        else -> {
                            if (outputIndex >= 0) {
                                val outputBuffer =
                                    decoder
                                        .getOutputBuffer(
                                            outputIndex,
                                        )

                                if (
                                    outputBuffer != null &&
                                    info.size > 0
                                ) {
                                    outputBuffer.position(
                                        info.offset,
                                    )
                                    outputBuffer.limit(
                                        info.offset +
                                            info.size,
                                    )

                                    val pcm =
                                        outputBuffer
                                            .slice()
                                            .order(
                                                ByteOrder.LITTLE_ENDIAN,
                                            )

                                    when (pcmEncoding) {
                                        AudioFormat.ENCODING_PCM_FLOAT ->
                                            accumulator
                                                .pushFloatPcm(
                                                    pcm =
                                                        pcm,
                                                    channelCount =
                                                        channelCount,
                                                )

                                        AudioFormat.ENCODING_PCM_16BIT ->
                                            accumulator
                                                .pushShortPcm(
                                                    pcm =
                                                        pcm,
                                                    channelCount =
                                                        channelCount,
                                                )

                                        else ->
                                            error(
                                                "Unsupported PCM encoding: $pcmEncoding",
                                            )
                                    }
                                }

                                outputDone =
                                    info.flags and
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                                        0

                                decoder.releaseOutputBuffer(
                                    outputIndex,
                                    false,
                                )
                            }
                        }
                    }
                }

                onProgress(100)

                return normalize(
                    rawFrames =
                        rawFrames,
                    sampleRate =
                        sampleRate,
                    sourceDurationUs =
                        sourceDurationUs,
                )
            } finally {
                runCatching {
                    decoder.stop()
                }
                decoder.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun normalize(
        rawFrames: List<RawFrame>,
        sampleRate: Int,
        sourceDurationUs: Long,
    ): OfflineAnalysisResult {
        if (rawFrames.isEmpty()) {
            return OfflineAnalysisResult(
                durationMs =
                    sourceDurationUs /
                        1000L,
                frameIntervalMs =
                    (
                        HOP_SIZE *
                            1000L /
                            sampleRate
                                .coerceAtLeast(1)
                        )
                        .coerceAtLeast(1L),
                signals = emptyList(),
            )
        }

        val ampScale =
            percentile95(
                rawFrames.map {
                    it.amplitude
                },
            )
        val bassScale =
            percentile95(
                rawFrames.map {
                    it.bass
                },
            )
        val midScale =
            percentile95(
                rawFrames.map {
                    it.mid
                },
            )
        val highScale =
            percentile95(
                rawFrames.map {
                    it.high
                },
            )

        val result =
            ArrayList<SceneSignal>(
                rawFrames.size,
            )

        var ampSmooth = 0f
        var bassSmooth = 0f
        var midSmooth = 0f
        var highSmooth = 0f
        var beatEnvelope = 0f

        val recentEnergy =
            ArrayDeque<Float>()

        var recentSum = 0f
        var lastBeatIndex = -1000

        rawFrames.forEachIndexed {
                index,
                raw,
            ->

            val amp =
                normalizeValue(
                    raw.amplitude,
                    ampScale,
                )
            val bass =
                normalizeValue(
                    raw.bass,
                    bassScale,
                )
            val mid =
                normalizeValue(
                    raw.mid,
                    midScale,
                )
            val high =
                normalizeValue(
                    raw.high,
                    highScale,
                )

            ampSmooth =
                follow(
                    current = ampSmooth,
                    target = amp,
                    attack = 0.70f,
                    release = 0.16f,
                )
            bassSmooth =
                follow(
                    current = bassSmooth,
                    target = bass,
                    attack = 0.78f,
                    release = 0.14f,
                )
            midSmooth =
                follow(
                    current = midSmooth,
                    target = mid,
                    attack = 0.64f,
                    release = 0.18f,
                )
            highSmooth =
                follow(
                    current = highSmooth,
                    target = high,
                    attack = 0.68f,
                    release = 0.20f,
                )

            val energy =
                bassSmooth * 0.72f +
                    ampSmooth * 0.28f

            val average =
                if (
                    recentEnergy.isEmpty()
                ) {
                    energy
                } else {
                    recentSum /
                        recentEnergy.size
                }

            val threshold =
                average *
                    (
                        1.22f +
                            average * 0.10f
                        )

            val candidate =
                if (
                    recentEnergy.size >=
                    8 &&
                    energy >
                    threshold &&
                    index -
                    lastBeatIndex >=
                    5
                ) {
                    (
                        (energy - threshold) /
                            (
                                0.22f +
                                    average * 0.45f
                                )
                        )
                        .coerceIn(
                            0f,
                            1f,
                        )
                } else {
                    0f
                }

            if (candidate > 0f) {
                lastBeatIndex = index
            }

            beatEnvelope =
                maxOf(
                    candidate,
                    beatEnvelope * 0.68f,
                )

            result +=
                SceneSignal(
                    amplitude =
                        ampSmooth,
                    bass =
                        bassSmooth,
                    mid =
                        midSmooth,
                    high =
                        highSmooth,
                    beatStrength =
                        beatEnvelope,
                )

            recentEnergy.addLast(
                energy,
            )
            recentSum += energy

            if (
                recentEnergy.size >
                24
            ) {
                recentSum -=
                    recentEnergy
                        .removeFirst()
            }
        }

        val frameIntervalMs =
            (
                HOP_SIZE *
                    1000L /
                    sampleRate
                        .coerceAtLeast(1)
                )
                .coerceAtLeast(1L)

        val inferredDurationMs =
            result.size *
                frameIntervalMs

        return OfflineAnalysisResult(
            durationMs =
                if (
                    sourceDurationUs > 0L
                ) {
                    sourceDurationUs /
                        1000L
                } else {
                    inferredDurationMs
                },
            frameIntervalMs =
                frameIntervalMs,
            signals =
                result,
        )
    }

    private fun percentile95(
        values: List<Float>,
    ): Float {
        if (values.isEmpty()) return 1f

        val sorted =
            values
                .filter {
                    it.isFinite() &&
                        it >= 0f
                }
                .sorted()

        if (sorted.isEmpty()) return 1f

        val index =
            (
                sorted.lastIndex *
                    0.95f
                )
                .toInt()
                .coerceIn(
                    0,
                    sorted.lastIndex,
                )

        return sorted[index]
            .coerceAtLeast(
                0.000001f,
            )
    }

    private fun normalizeValue(
        value: Float,
        scale: Float,
    ): Float =
        (
            value /
                scale
            )
            .coerceIn(
                0f,
                1.25f,
            )
            .let {
                (it / 1.25f)
                    .coerceIn(
                        0f,
                        1f,
                    )
            }

    private fun follow(
        current: Float,
        target: Float,
        attack: Float,
        release: Float,
    ): Float {
        val factor =
            if (target > current) {
                attack
            } else {
                release
            }

        return current +
            (target - current) *
            factor
    }

    private class PcmAccumulator(
        private val sampleRate: Int,
        private val onFrame: (RawFrame) -> Unit,
    ) {
        private val window =
            FloatArray(
                FFT_SIZE,
            )

        private val real =
            FloatArray(
                FFT_SIZE,
            )

        private val imaginary =
            FloatArray(
                FFT_SIZE,
            )

        private var filled = 0

        fun pushShortPcm(
            pcm: java.nio.ByteBuffer,
            channelCount: Int,
        ) {
            val shorts =
                pcm.asShortBuffer()

            val channels =
                channelCount
                    .coerceAtLeast(1)

            while (
                shorts.remaining() >=
                channels
            ) {
                var mono = 0f

                repeat(channels) {
                    mono +=
                        shorts.get()
                            .toFloat() /
                            32768f
                }

                push(
                    mono /
                        channels,
                )
            }
        }

        fun pushFloatPcm(
            pcm: java.nio.ByteBuffer,
            channelCount: Int,
        ) {
            val floats =
                pcm.asFloatBuffer()

            val channels =
                channelCount
                    .coerceAtLeast(1)

            while (
                floats.remaining() >=
                channels
            ) {
                var mono = 0f

                repeat(channels) {
                    mono +=
                        floats.get()
                }

                push(
                    mono /
                        channels,
                )
            }
        }

        private fun push(
            sample: Float,
        ) {
            window[filled++] =
                sample
                    .coerceIn(
                        -1f,
                        1f,
                    )

            if (
                filled <
                FFT_SIZE
            ) {
                return
            }

            onFrame(
                analyzeWindow(),
            )

            window.copyInto(
                destination =
                    window,
                destinationOffset =
                    0,
                startIndex =
                    HOP_SIZE,
                endIndex =
                    FFT_SIZE,
            )

            filled =
                FFT_SIZE -
                    HOP_SIZE
        }

        private fun analyzeWindow(): RawFrame {
            var sumSquares = 0.0

            for (
                index in
                0 until FFT_SIZE
            ) {
                val sample =
                    window[index]

                sumSquares +=
                    sample *
                        sample

                val hann =
                    (
                        0.5 -
                            0.5 *
                            cos(
                                2.0 *
                                    PI *
                                    index /
                                    (
                                        FFT_SIZE -
                                            1
                                        )
                            )
                        )
                        .toFloat()

                real[index] =
                    sample *
                        hann

                imaginary[index] =
                    0f
            }

            fft(
                real =
                    real,
                imaginary =
                    imaginary,
            )

            val amplitude =
                sqrt(
                    sumSquares /
                        FFT_SIZE
                    )
                    .toFloat()

            var bass = 0.0
            var mid = 0.0
            var high = 0.0

            var bassBins = 0
            var midBins = 0
            var highBins = 0

            val nyquistBin =
                FFT_SIZE /
                    2

            for (
                bin in
                1 until nyquistBin
            ) {
                val hz =
                    bin *
                        sampleRate
                            .toDouble() /
                        FFT_SIZE

                val re =
                    real[bin]
                        .toDouble()
                val im =
                    imaginary[bin]
                        .toDouble()

                val magnitude =
                    ln1p(
                        sqrt(
                            re * re +
                                im * im
                        ),
                    )

                when {
                    hz in 35.0..180.0 -> {
                        bass += magnitude
                        bassBins++
                    }

                    hz > 180.0 &&
                        hz <= 2_000.0 -> {
                        mid += magnitude
                        midBins++
                    }

                    hz > 2_000.0 &&
                        hz <= 10_000.0 -> {
                        high += magnitude
                        highBins++
                    }
                }
            }

            return RawFrame(
                amplitude =
                    amplitude,
                bass =
                    averageBand(
                        bass,
                        bassBins,
                    ),
                mid =
                    averageBand(
                        mid,
                        midBins,
                    ),
                high =
                    averageBand(
                        high,
                        highBins,
                    ),
            )
        }

        private fun averageBand(
            sum: Double,
            count: Int,
        ): Float =
            if (count <= 0) {
                0f
            } else {
                (
                    sum /
                        count
                    )
                    .toFloat()
            }
    }

    private fun fft(
        real: FloatArray,
        imaginary: FloatArray,
    ) {
        val n = real.size
        var j = 0

        for (
            i in
            1 until n
        ) {
            var bit =
                n shr 1

            while (
                j and bit != 0
            ) {
                j =
                    j xor
                        bit
                bit =
                    bit shr
                        1
            }

            j =
                j xor
                    bit

            if (i < j) {
                val realTemp =
                    real[i]
                real[i] =
                    real[j]
                real[j] =
                    realTemp

                val imaginaryTemp =
                    imaginary[i]
                imaginary[i] =
                    imaginary[j]
                imaginary[j] =
                    imaginaryTemp
            }
        }

        var length = 2

        while (length <= n) {
            val angle =
                -2.0 *
                    PI /
                    length

            val wLengthReal =
                kotlin.math.cos(
                    angle,
                )
                    .toFloat()

            val wLengthImaginary =
                kotlin.math.sin(
                    angle,
                )
                    .toFloat()

            var start = 0

            while (start < n) {
                var wReal = 1f
                var wImaginary = 0f

                for (
                    offset in
                    0 until
                        length /
                        2
                ) {
                    val even =
                        start +
                            offset

                    val odd =
                        even +
                            length /
                            2

                    val oddReal =
                        real[odd] *
                            wReal -
                            imaginary[odd] *
                            wImaginary

                    val oddImaginary =
                        real[odd] *
                            wImaginary +
                            imaginary[odd] *
                            wReal

                    val evenReal =
                        real[even]
                    val evenImaginary =
                        imaginary[even]

                    real[even] =
                        evenReal +
                            oddReal
                    imaginary[even] =
                        evenImaginary +
                            oddImaginary
                    real[odd] =
                        evenReal -
                            oddReal
                    imaginary[odd] =
                        evenImaginary -
                            oddImaginary

                    val nextWReal =
                        wReal *
                            wLengthReal -
                            wImaginary *
                            wLengthImaginary

                    wImaginary =
                        wReal *
                            wLengthImaginary +
                            wImaginary *
                            wLengthReal

                    wReal =
                        nextWReal
                }

                start += length
            }

            length =
                length shl
                    1
        }
    }

    private fun MediaFormat.getIntegerOrDefault(
        key: String,
        defaultValue: Int,
    ): Int =
        if (containsKey(key)) {
            getInteger(key)
        } else {
            defaultValue
        }
}
