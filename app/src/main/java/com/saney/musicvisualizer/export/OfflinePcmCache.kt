package com.saney.musicvisualizer.export

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException

data class OfflinePcmCacheResult(
    val file: File,
    val sampleRate: Int,
    val sampleCount: Long,
    /** Song time where the cached PCM ends (whole song: its duration). */
    val durationMs: Long,
    /** V60: song time of the first cached sample (0 = whole song from the start). */
    val startMs: Long = 0L,
) {
    fun openReader():
        OfflinePcmReader =
        OfflinePcmReader(
            this,
        )
}

class OfflinePcmReader(
    private val result:
        OfflinePcmCacheResult,
) : AutoCloseable {
    private val input =
        RandomAccessFile(
            result.file,
            "r",
        )

    fun samplesBetween(
        startMs: Long,
        endMs: Long,
    ): ShortArray {
        val safeStartMs =
            startMs.coerceAtLeast(
                0L,
            )
        val safeEndMs =
            endMs
                .coerceAtLeast(
                    safeStartMs +
                        1L,
                )
                .coerceAtMost(
                    result.durationMs
                        .coerceAtLeast(
                            safeStartMs +
                                1L,
                        ),
                )

        val startSample =
            (
                (safeStartMs - result.startMs).coerceAtLeast(0L) *
                    result.sampleRate /
                    1000L
                )
                .coerceIn(
                    0L,
                    result.sampleCount,
                )

        val endSample =
            (
                (safeEndMs - result.startMs).coerceAtLeast(0L) *
                    result.sampleRate /
                    1000L
                )
                .coerceIn(
                    startSample,
                    result.sampleCount,
                )

        val count =
            (
                endSample -
                    startSample
                )
                .toInt()
                .coerceAtLeast(
                    1,
                )

        val bytes =
            ByteArray(
                count *
                    2,
            )

        input.seek(
            startSample *
                2L,
        )

        var offset = 0

        while (
            offset <
            bytes.size
        ) {
            val read =
                input.read(
                    bytes,
                    offset,
                    bytes.size -
                        offset,
                )

            if (read <= 0) {
                break
            }

            offset +=
                read
        }

        val shorts =
            ShortArray(
                count,
            )

        val buffer =
            ByteBuffer
                .wrap(
                    bytes,
                )
                .order(
                    ByteOrder.LITTLE_ENDIAN,
                )
                .asShortBuffer()

        val available =
            minOf(
                count,
                offset /
                    2,
            )

        buffer.get(
            shorts,
            0,
            available,
        )

        return shorts
    }

    override fun close() {
        input.close()
    }
}

object OfflinePcmCache {
    private const val TIMEOUT_US =
        10_000L

    fun build(
        context: Context,
        uri: Uri,
        shouldCancel: () -> Boolean = {
            false
        },
        onProgress: (Int) -> Unit = {},
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
    ): OfflinePcmCacheResult {
        val target =
            File(
                context.cacheDir,
                "faric-offline-pcm-" +
                    System.currentTimeMillis() +
                    ".raw",
            )

        val extractor =
            MediaExtractor()

        try {
            extractor.setDataSource(
                context,
                uri,
                null,
            )

            val trackIndex =
                (
                    0 until
                        extractor.trackCount
                    )
                    .firstOrNull { index ->
                        extractor
                            .getTrackFormat(
                                index,
                            )
                            .getString(
                                MediaFormat.KEY_MIME,
                            )
                            ?.startsWith(
                                "audio/",
                            ) ==
                            true
                    }
                    ?: error(
                        "У файлі немає audio track",
                    )

            extractor.selectTrack(
                trackIndex,
            )
            val range = DecodeRange.of(startMs, endMs)
            if (range.startUs > 0L) {
                extractor.seekTo(range.startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }
            var firstOutputUs = -1L

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

            // V89: a WAV is read by PulseDeck itself (true rate and sample format from its header).
            val wav = openWavPcmOrNull(context, uri)
            wav?.skipToUs(range.startUs)

            val sourceDurationUs =
                if (wav != null && wav.durationUs > 0L) {
                    wav.durationUs
                } else if (
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
                MediaCodec
                    .createDecoderByType(
                        mime,
                    )

            var sampleRate =
                wav?.sampleRate ?: inputFormat
                    .getIntegerOrDefault(
                        MediaFormat.KEY_SAMPLE_RATE,
                        44_100,
                    )

            var channelCount =
                wav?.channels ?: inputFormat
                    .getIntegerOrDefault(
                        MediaFormat.KEY_CHANNEL_COUNT,
                        2,
                    )

            // V89: see OfflineAudioAnalyzer — WAV format from the track + real-size guard.
            var pcmEncoding =
                inputFormat
                    .getIntegerOrDefault(
                        MediaFormat.KEY_PCM_ENCODING,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
            val encodingGuard = PcmEncodingGuard(enabled = mime == "audio/raw")

            var sampleCount =
                0L

            try {
                decoder.configure(
                    inputFormat,
                    null,
                    null,
                    0,
                )
                decoder.start()

                val info =
                    MediaCodec
                        .BufferInfo()

                var inputDone =
                    false
                var outputDone =
                    false
                var lastProgress =
                    -1

                FileOutputStream(
                    target,
                ).use { output ->
                    while (!outputDone) {
                        if (
                            shouldCancel()
                        ) {
                            throw CancellationException(
                                "PCM preparation cancelled",
                            )
                        }

                        if (wav != null) {
                            val chunkPtsUs = wav.positionUs
                            val pcm = if (range.isPastEnd(chunkPtsUs)) null else wav.readPcm16(4096)
                            if (pcm == null) {
                                outputDone = true
                            } else {
                                if (firstOutputUs < 0L) firstOutputUs = chunkPtsUs
                                sampleCount +=
                                    writeMono16(
                                        pcm = java.nio.ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN),
                                        pcmEncoding = AudioFormat.ENCODING_PCM_16BIT,
                                        channelCount = channelCount,
                                        output = output,
                                    )
                                if (sourceDurationUs > 0L) {
                                    val progress = range.progressPercent(chunkPtsUs, sourceDurationUs).coerceIn(0, 99)
                                    if (progress != lastProgress) {
                                        lastProgress = progress
                                        onProgress(progress)
                                    }
                                }
                            }
                            continue
                        }

                        if (!inputDone) {
                            val inputIndex =
                                decoder
                                    .dequeueInputBuffer(
                                        TIMEOUT_US,
                                    )

                            if (
                                inputIndex >=
                                0
                            ) {
                                val inputBuffer =
                                    decoder
                                        .getInputBuffer(
                                            inputIndex,
                                        )
                                        ?: error(
                                            "Decoder input buffer unavailable",
                                        )

                                val size =
                                    extractor
                                        .readSampleData(
                                            inputBuffer,
                                            0,
                                        )

                                if (size < 0 || range.isPastEnd(extractor.sampleTime)) {
                                    decoder
                                        .queueInputBuffer(
                                            inputIndex,
                                            0,
                                            0,
                                            0L,
                                            MediaCodec
                                                .BUFFER_FLAG_END_OF_STREAM,
                                        )
                                    inputDone =
                                        true
                                } else {
                                    val ptsUs =
                                        extractor
                                            .sampleTime
                                            .coerceAtLeast(
                                                0L,
                                            )

                                    decoder
                                        .queueInputBuffer(
                                            inputIndex,
                                            0,
                                            size,
                                            ptsUs,
                                            0,
                                        )

                                    if (
                                        sourceDurationUs >
                                        0L
                                    ) {
                                        val progress =
                                            (
                                                range.progressPercent(ptsUs, sourceDurationUs).toLong()
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

                                    extractor
                                        .advance()
                                }
                            }
                        }

                        when (
                            val outputIndex =
                                decoder
                                    .dequeueOutputBuffer(
                                        info,
                                        TIMEOUT_US,
                                    )
                        ) {
                            MediaCodec
                                .INFO_TRY_AGAIN_LATER -> {
                                // Decoder has no output yet.
                            }

                            MediaCodec
                                .INFO_OUTPUT_FORMAT_CHANGED -> {
                                val outputFormat =
                                    decoder
                                        .outputFormat

                                sampleRate =
                                    outputFormat
                                        .getIntegerOrDefault(
                                            MediaFormat
                                                .KEY_SAMPLE_RATE,
                                            sampleRate,
                                        )

                                channelCount =
                                    outputFormat
                                        .getIntegerOrDefault(
                                            MediaFormat
                                                .KEY_CHANNEL_COUNT,
                                            channelCount,
                                        )

                                pcmEncoding =
                                    outputFormat
                                        .getIntegerOrDefault(
                                            MediaFormat
                                                .KEY_PCM_ENCODING,
                                            pcmEncoding,
                                        )
                            }

                            MediaCodec
                                .INFO_OUTPUT_BUFFERS_CHANGED -> {
                                // Deprecated buffer-array API only.
                            }

                            else -> {
                                if (
                                    outputIndex >=
                                    0
                                ) {
                                    val outputBuffer =
                                        decoder
                                            .getOutputBuffer(
                                                outputIndex,
                                            )

                                    if (
                                        outputBuffer !=
                                        null &&
                                        info.size >
                                        0
                                    ) {
                                        if (firstOutputUs < 0L) firstOutputUs = info.presentationTimeUs
                                        outputBuffer
                                            .position(
                                                info.offset,
                                            )
                                        outputBuffer
                                            .limit(
                                                info.offset +
                                                    info.size,
                                            )

                                        val pcm =
                                            outputBuffer
                                                .slice()
                                                .order(
                                                    ByteOrder
                                                        .LITTLE_ENDIAN,
                                                )

                                        pcmEncoding =
                                            encodingGuard.resolve(
                                                ptsUs = info.presentationTimeUs,
                                                bytes = info.size,
                                                sample = pcm,
                                                reported = pcmEncoding,
                                                sampleRate = sampleRate,
                                                channels = channelCount,
                                            )

                                        sampleCount +=
                                            writeMono16(
                                                pcm =
                                                    pcm,
                                                pcmEncoding =
                                                    pcmEncoding,
                                                channelCount =
                                                    channelCount,
                                                output =
                                                    output,
                                            )
                                    }

                                    outputDone =
                                        info.flags and
                                            MediaCodec
                                                .BUFFER_FLAG_END_OF_STREAM !=
                                            0

                                    decoder
                                        .releaseOutputBuffer(
                                            outputIndex,
                                            false,
                                        )
                                }
                            }
                        }
                    }
                }

                onProgress(
                    100,
                )
            } catch (
                error: Throwable,
            ) {
                target.delete()
                throw error
            } finally {
                runCatching {
                    decoder.stop()
                }
                decoder.release()
                runCatching { wav?.close() }
            }

            val cacheStartMs =
                if (range.isWindow) firstOutputUs.coerceAtLeast(0L) / 1000L else 0L
            val durationMs =
                when {
                    range.isWindow && sampleRate > 0 ->
                        cacheStartMs + sampleCount * 1000L / sampleRate
                    sourceDurationUs >
                        0L ->
                        sourceDurationUs /
                            1000L

                    sampleRate >
                        0 ->
                        sampleCount *
                            1000L /
                            sampleRate

                    else ->
                        0L
                }

            return OfflinePcmCacheResult(
                file = target,
                sampleRate =
                    sampleRate,
                sampleCount =
                    sampleCount,
                durationMs =
                    durationMs,
                startMs = cacheStartMs,
            )
        } finally {
            extractor.release()
        }
    }

    private fun writeMono16(
        pcm: ByteBuffer,
        pcmEncoding: Int,
        channelCount: Int,
        output: FileOutputStream,
    ): Long {
        val channels =
            channelCount
                .coerceAtLeast(
                    1,
                )

        return when (
            pcmEncoding
        ) {
            AudioFormat
                .ENCODING_PCM_FLOAT -> {
                val floats =
                    pcm
                        .asFloatBuffer()

                val frames =
                    floats.remaining() /
                        channels

                val bytes =
                    ByteArray(
                        frames *
                            2,
                    )

                val target =
                    ByteBuffer
                        .wrap(
                            bytes,
                        )
                        .order(
                            ByteOrder
                                .LITTLE_ENDIAN,
                        )

                repeat(
                    frames,
                ) {
                    var mono =
                        0f

                    repeat(
                        channels,
                    ) {
                        mono +=
                            floats.get()
                    }

                    val sample =
                        (
                            mono /
                                channels
                            )
                            .coerceIn(
                                -1f,
                                1f,
                            )

                    target.putShort(
                        (
                            sample *
                                32767f
                            )
                            .toInt()
                            .toShort(),
                    )
                }

                output.write(
                    bytes,
                )

                frames.toLong()
            }

            AudioFormat
                .ENCODING_PCM_16BIT -> {
                val shorts =
                    pcm
                        .asShortBuffer()

                val frames =
                    shorts.remaining() /
                        channels

                val bytes =
                    ByteArray(
                        frames *
                            2,
                    )

                val target =
                    ByteBuffer
                        .wrap(
                            bytes,
                        )
                        .order(
                            ByteOrder
                                .LITTLE_ENDIAN,
                        )

                repeat(
                    frames,
                ) {
                    var sum =
                        0

                    repeat(
                        channels,
                    ) {
                        sum +=
                            shorts
                                .get()
                                .toInt()
                    }

                    target.putShort(
                        (
                            sum /
                                channels
                            )
                            .coerceIn(
                                Short.MIN_VALUE
                                    .toInt(),
                                Short.MAX_VALUE
                                    .toInt(),
                            )
                            .toShort(),
                    )
                }

                output.write(
                    bytes,
                )

                frames.toLong()
            }

            // V56: 24-bit / 32-bit / 8-bit WAV → 16-bit first.
            else ->
                writeMono16(
                    pcm = ByteBuffer.wrap(PcmConvert.toPcm16(pcm, pcmEncoding)).order(ByteOrder.LITTLE_ENDIAN),
                    pcmEncoding = AudioFormat.ENCODING_PCM_16BIT,
                    channelCount = channelCount,
                    output = output,
                )
        }
    }

    private fun MediaFormat
        .getIntegerOrDefault(
            key: String,
            defaultValue: Int,
        ): Int =
        if (
            containsKey(
                key,
            )
        ) {
            getInteger(
                key,
            )
        } else {
            defaultValue
        }
}
