package com.saney.musicvisualizer.export

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

object AudioClipTranscoder {
    private const val AAC_MIME = "audio/mp4a-latm"
    private const val AUDIO_BITRATE = 160_000
    private const val TIMEOUT_US = 10_000L
    private const val STALL_LIMIT_MS = 20_000L

    data class Result(
        val sampleRate: Int,
        val channelCount: Int,
        val durationMs: Long,
    )

    fun transcodeToAacMp4(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        durationMs: Long,
        outputFile: File,
        shouldCancel: () -> Boolean = { false },
    ): Result {
        val extractor = MediaExtractor()
        extractor.setDataSource(
            context,
            sourceUri,
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
                ?: run {
                    extractor.release()
                    error("У файлі немає audio track")
                }

        extractor.selectTrack(trackIndex)

        val sourceFormat =
            extractor.getTrackFormat(trackIndex)

        val sourceMime =
            sourceFormat.getString(
                MediaFormat.KEY_MIME,
            )
                ?: run {
                    extractor.release()
                    error("Audio MIME відсутній")
                }

        // V89: a WAV is read by PulseDeck itself — its header says the true rate and sample format.
        val wav = openWavPcmOrNull(context, sourceUri)

        val sampleRate =
            wav?.sampleRate
                ?: sourceFormat.getInteger(
                    MediaFormat.KEY_SAMPLE_RATE,
                )

        val channelCount =
            wav?.channels
                ?: sourceFormat.getInteger(
                    MediaFormat.KEY_CHANNEL_COUNT,
                )

        val startUs =
            startMs
                .coerceAtLeast(0L) *
                1000L

        val durationUs =
            durationMs
                .coerceAtLeast(1L) *
                1000L

        val endUs =
            startUs +
                durationUs

        extractor.seekTo(
            startUs,
            MediaExtractor.SEEK_TO_CLOSEST_SYNC,
        )

        val decoder =
            MediaCodec.createDecoderByType(
                sourceMime,
            )

        val encoder =
            MediaCodec.createEncoderByType(
                AAC_MIME,
            )

        // V89: AAC encoders on phones take at most 48 kHz; a 96 kHz WAV used to be encoded as if it
        // were 44.1 kHz (2,18× longer, an octave lower). Encode at a safe rate, resample before.
        var encoderRate =
            PcmResampler.aacRateFor(sampleRate)

        val encoderFormat =
            MediaFormat.createAudioFormat(
                AAC_MIME,
                encoderRate,
                channelCount,
            ).apply {
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo
                        .CodecProfileLevel
                        .AACObjectLC,
                )
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    AUDIO_BITRATE,
                )
                setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE,
                    64 * 1024,
                )
            }

        val muxer =
            MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )

        var muxerStarted = false
        var muxerTrack = -1

        val decoderInfo =
            MediaCodec.BufferInfo()

        val encoderInfo =
            MediaCodec.BufferInfo()

        var decoderInputDone = false
        var decoderOutputDone = false
        var encoderInputDone = false
        var encoderOutputDone = false

        // V89 (faric 20.10 19:24: WAV → звук у 2 рази довший і повільніший): the WAV track
        // says its own sample format; keep it unless the decoder reports another one, and
        // let the guard check the real bytes per sample from the timestamps.
        var pcmEncoding =
            if (sourceFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                sourceFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
        val encodingGuard = PcmEncodingGuard()

        try {
            decoder.configure(
                sourceFormat,
                null,
                null,
                0,
            )

            encoder.configure(
                encoderFormat,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE,
            )
            // Trust the rate the encoder really took (some quietly fall back to another one).
            runCatching { encoder.inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) }
                .getOrNull()
                ?.takeIf { it > 0 }
                ?.let { encoderRate = it }
            val resampler = PcmResampler(sampleRate, encoderRate, channelCount)

            decoder.start()
            encoder.start()

            // V56 (faric 19.10 23:04, WAV hung at «Кодую аудіо · 80%»): drain the AAC encoder
            // also while waiting for its input. A WAV decoder hands over big PCM buffers that
            // need several encoder inputs; if nobody empties the encoder output meanwhile, the
            // encoder never frees an input buffer and the export waits forever.
            fun drainEncoderOutput() {
                while (true) {
                    when (
                        val outputIndex =
                            encoder.dequeueOutputBuffer(
                                encoderInfo,
                                0L,
                            )
                    ) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            return
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) {
                                "AAC output format changed twice"
                            }

                            muxerTrack =
                                muxer.addTrack(
                                    encoder.outputFormat,
                                )

                            muxer.start()
                            muxerStarted = true
                        }

                        else -> {
                            if (outputIndex >= 0) {
                                val outputBuffer =
                                    encoder.getOutputBuffer(
                                        outputIndex,
                                    )
                                        ?: error(
                                            "AAC encoder output buffer unavailable",
                                        )

                                if (
                                    encoderInfo.flags and
                                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG !=
                                        0
                                ) {
                                    encoderInfo.size = 0
                                }

                                if (
                                    encoderInfo.size > 0
                                ) {
                                    check(muxerStarted) {
                                        "AAC muxer not started"
                                    }

                                    outputBuffer.position(
                                        encoderInfo.offset,
                                    )
                                    outputBuffer.limit(
                                        encoderInfo.offset +
                                            encoderInfo.size,
                                    )

                                    muxer.writeSampleData(
                                        muxerTrack,
                                        outputBuffer,
                                        encoderInfo,
                                    )
                                }

                                encoderOutputDone =
                                    encoderInfo.flags and
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                                        0

                                encoder.releaseOutputBuffer(
                                    outputIndex,
                                    false,
                                )
                            }
                        }
                    }
                }
            }

            wav?.skipToUs(startUs)
            while (!encoderOutputDone) {
                if (shouldCancel()) throw CancellationException("Export cancelled during AAC")
                if (wav != null) {
                    // V89: PCM straight from the WAV file, cut exactly at the end of the clip.
                    if (!decoderOutputDone) {
                        val chunkPtsUs = wav.positionUs
                        val pcm = if (chunkPtsUs < endUs) wav.readPcm16(4096) else null
                        if (pcm == null) {
                            decoderOutputDone = true
                        } else {
                            val frameBytes = 2 * channelCount
                            val allowedFrames = ((endUs - chunkPtsUs) * sampleRate / 1_000_000L).toInt()
                            val clip =
                                if (pcm.size / frameBytes > allowedFrames) pcm.copyOf(allowedFrames * frameBytes) else pcm
                            if (clip.isNotEmpty()) {
                                queuePcm(
                                    shouldCancel = shouldCancel,
                                    onStall = ::drainEncoderOutput,
                                    encoder = encoder,
                                    pcm16 = resampler.process(clip),
                                    ptsUs = chunkPtsUs - startUs,
                                    sampleRate = encoderRate,
                                    channelCount = channelCount,
                                )
                            }
                            if (wav.positionUs >= endUs) decoderOutputDone = true
                        }
                    }
                } else {
                if (!decoderInputDone) {
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
                                    "Audio decoder input buffer unavailable",
                                )

                        inputBuffer.clear()

                        val sampleTimeUs =
                            extractor.sampleTime

                        if (
                            sampleTimeUs < 0L ||
                            sampleTimeUs >=
                            endUs
                        ) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                durationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            decoderInputDone = true
                        } else {
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
                                    durationUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                decoderInputDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    sampleTimeUs,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                var keepDrainingDecoder = true

                while (keepDrainingDecoder) {
                    when (
                        val outputIndex =
                            decoder.dequeueOutputBuffer(
                                decoderInfo,
                                0L,
                            )
                    ) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            keepDrainingDecoder = false
                        }

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outputFormat =
                                decoder.outputFormat

                            if (
                                outputFormat.containsKey(
                                    MediaFormat.KEY_PCM_ENCODING,
                                )
                            ) {
                                pcmEncoding =
                                    outputFormat.getInteger(
                                        MediaFormat.KEY_PCM_ENCODING,
                                    )
                            }

                            val outputSampleRate =
                                outputFormat.getInteger(
                                    MediaFormat.KEY_SAMPLE_RATE,
                                )

                            val outputChannels =
                                outputFormat.getInteger(
                                    MediaFormat.KEY_CHANNEL_COUNT,
                                )

                            check(
                                outputSampleRate ==
                                    sampleRate &&
                                    outputChannels ==
                                    channelCount,
                            ) {
                                "Audio format changed during decode"
                            }
                        }

                        else -> {
                            if (outputIndex >= 0) {
                                val isEos =
                                    decoderInfo.flags and
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                                        0

                                val sourcePtsUs =
                                    decoderInfo.presentationTimeUs

                                if (
                                    decoderInfo.size > 0 &&
                                    sourcePtsUs <
                                    endUs
                                ) {
                                    val outputBuffer =
                                        decoder.getOutputBuffer(
                                            outputIndex,
                                        )
                                            ?: error(
                                                "Audio decoder output buffer unavailable",
                                            )

                                    outputBuffer.position(
                                        decoderInfo.offset,
                                    )
                                    outputBuffer.limit(
                                        decoderInfo.offset +
                                            decoderInfo.size,
                                    )

                                    pcmEncoding =
                                        encodingGuard.resolve(
                                            ptsUs = sourcePtsUs,
                                            bytes = decoderInfo.size,
                                            sample = outputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN),
                                            reported = pcmEncoding,
                                            sampleRate = sampleRate,
                                            channels = channelCount,
                                        )

                                    if (
                                        sourcePtsUs >=
                                        startUs
                                    ) {
                                        val pcm16 =
                                            toPcm16(
                                                outputBuffer =
                                                    outputBuffer.slice()
                                                        .order(
                                                            ByteOrder.LITTLE_ENDIAN,
                                                        ),
                                                pcmEncoding =
                                                    pcmEncoding,
                                            )

                                        queuePcm(
                                            shouldCancel = shouldCancel,
                                            onStall = ::drainEncoderOutput,
                                            encoder =
                                                encoder,
                                            pcm16 =
                                                resampler.process(pcm16),
                                            ptsUs =
                                                sourcePtsUs -
                                                    startUs,
                                            sampleRate =
                                                encoderRate,
                                            channelCount =
                                                channelCount,
                                        )
                                    }
                                }

                                decoder.releaseOutputBuffer(
                                    outputIndex,
                                    false,
                                )

                                if (
                                    isEos ||
                                    sourcePtsUs >=
                                    endUs
                                ) {
                                    decoderOutputDone = true
                                }

                                // Give the AAC encoder a chance to drain after each
                                // decoded PCM buffer instead of filling all input
                                // buffers in one decoder burst.
                                keepDrainingDecoder = false
                            }
                        }
                    }
                }

                } // end of Android decoder path

                if (
                    decoderOutputDone &&
                    !encoderInputDone
                ) {
                    queueEncoderEos(
                        shouldCancel = shouldCancel,
                        onStall = ::drainEncoderOutput,
                        encoder = encoder,
                        ptsUs = durationUs,
                    )
                    encoderInputDone = true
                }

                drainEncoderOutput()
            }
        } finally {
            extractor.release()
            runCatching { wav?.close() }

            runCatching {
                decoder.stop()
            }
            decoder.release()

            runCatching {
                encoder.stop()
            }
            encoder.release()

            if (muxerStarted) {
                runCatching {
                    muxer.stop()
                }
            }

            muxer.release()
        }

        return Result(
            sampleRate = encoderRate,
            channelCount = channelCount,
            durationMs = durationMs,
        )
    }

    private fun queuePcm(
        shouldCancel: () -> Boolean,
        onStall: () -> Unit,
        encoder: MediaCodec,
        pcm16: ByteArray,
        ptsUs: Long,
        sampleRate: Int,
        channelCount: Int,
    ) {
        var offset = 0

        while (offset < pcm16.size) {
            val inputIndex =
                waitForEncoderInput(encoder, shouldCancel, onStall)

            val inputBuffer =
                encoder.getInputBuffer(
                    inputIndex,
                )
                    ?: error(
                        "AAC encoder input buffer unavailable",
                    )

            inputBuffer.clear()

            val bytes =
                minOf(
                    inputBuffer.remaining(),
                    pcm16.size -
                        offset,
                )

            inputBuffer.put(
                pcm16,
                offset,
                bytes,
            )

            val bytesPerFrame =
                channelCount *
                    2

            val framesBefore =
                offset /
                    bytesPerFrame

            val chunkPtsUs =
                ptsUs +
                    framesBefore *
                    1_000_000L /
                    sampleRate

            encoder.queueInputBuffer(
                inputIndex,
                0,
                bytes,
                chunkPtsUs,
                0,
            )

            offset += bytes
        }
    }

    private fun queueEncoderEos(
        shouldCancel: () -> Boolean,
        onStall: () -> Unit,
        encoder: MediaCodec,
        ptsUs: Long,
    ) {
        val inputIndex =
            waitForEncoderInput(encoder, shouldCancel, onStall)

        encoder.queueInputBuffer(
            inputIndex,
            0,
            0,
            ptsUs,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
        )
    }

    private fun waitForEncoderInput(
        encoder: MediaCodec,
        shouldCancel: () -> Boolean,
        onStall: () -> Unit,
    ): Int {
        val startedMs = System.currentTimeMillis()
        while (true) {
            if (shouldCancel()) throw CancellationException("Export cancelled during AAC")
            val index =
                encoder.dequeueInputBuffer(
                    TIMEOUT_US,
                )

            if (index >= 0) {
                return index
            }
            // No free input: empty the encoder output so it can take more PCM.
            onStall()
            // Safety net: never wait forever — the batch moves on to the next song.
            check(System.currentTimeMillis() - startedMs < STALL_LIMIT_MS) {
                "Кодування звуку зависло: кодувальник AAC не бере дані " + STALL_LIMIT_MS / 1000 + " с"
            }
        }
    }

    private fun toPcm16(
        outputBuffer: ByteBuffer,
        pcmEncoding: Int,
    ): ByteArray {
        return when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val bytes =
                    ByteArray(
                        outputBuffer.remaining(),
                    )

                outputBuffer.get(bytes)
                bytes
            }

            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floats =
                    outputBuffer
                        .order(
                            ByteOrder.LITTLE_ENDIAN,
                        )
                        .asFloatBuffer()

                val bytes =
                    ByteArray(
                        floats.remaining() *
                            2,
                    )

                var index = 0

                while (floats.hasRemaining()) {
                    val sample =
                        (
                            floats.get()
                                .coerceIn(
                                    -1f,
                                    1f,
                                ) *
                                32767f
                            )
                            .roundToInt()
                            .toShort()

                    bytes[index++] =
                        (
                            sample.toInt() and
                                0xff
                            )
                            .toByte()

                    bytes[index++] =
                        (
                            sample.toInt() shr
                                8 and
                                0xff
                            )
                            .toByte()
                }

                bytes
            }

            // V56: 24-bit / 32-bit / 8-bit WAV.
            else ->
                PcmConvert.toPcm16(outputBuffer, pcmEncoding)
        }
    }
}
