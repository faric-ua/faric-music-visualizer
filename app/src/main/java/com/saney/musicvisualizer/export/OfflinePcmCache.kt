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
    val durationMs: Long,
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
                safeStartMs *
                    result.sampleRate /
                    1000L
                )
                .coerceIn(
                    0L,
                    result.sampleCount,
                )

        val endSample =
            (
                safeEndMs *
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
                MediaCodec
                    .createDecoderByType(
                        mime,
                    )

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
                AudioFormat
                    .ENCODING_PCM_16BIT

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

                                if (size < 0) {
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
                                            AudioFormat
                                                .ENCODING_PCM_16BIT,
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
            }

            val durationMs =
                when {
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

            else ->
                error(
                    "Unsupported PCM encoding: $pcmEncoding",
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
