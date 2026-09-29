package com.saney.musicvisualizer.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.saney.musicvisualizer.theme.HeroThemeRenderer
import com.saney.musicvisualizer.theme.MusicVideoProject
import com.saney.musicvisualizer.theme.ThemeInput
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.max

object ShortVideoExportProof {
    data class Result(
        val uri: Uri?,
        val frameCount: Int,
        val width: Int,
        val height: Int,
        val durationMs: Long,
    )

    private const val MIME = "video/avc"
    private const val PROOF_FPS = 15
    private const val PROOF_DURATION_MS = 3_000L
    private const val TIMEOUT_US = 10_000L

    fun export(
        context: Context,
        project: MusicVideoProject,
        analysis: OfflineAnalysisResult,
        title: String,
        artist: String,
        startMs: Long,
        onProgress: (Int) -> Unit = {},
    ): Result {
        val (width, height) =
            proofSize(
                project.aspectRatio.width,
                project.aspectRatio.height,
            )

        val availableMs =
            (analysis.durationMs - startMs)
                .coerceAtLeast(0L)

        val durationMs =
            minOf(
                PROOF_DURATION_MS,
                availableMs,
            ).coerceAtLeast(500L)

        val frameCount =
            max(
                1,
                (
                    durationMs *
                        PROOF_FPS /
                        1000L
                    ).toInt(),
            )

        val temp =
            File(
                context.cacheDir,
                "faric-proof-${System.currentTimeMillis()}.mp4",
            )

        val encoderInfo =
            findEncoder()
                ?: error("H.264 encoder недоступний")

        val colorFormat =
            chooseColorFormat(
                encoderInfo,
            )

        val format =
            MediaFormat.createVideoFormat(
                MIME,
                width,
                height,
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    colorFormat,
                )
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    max(
                        1_500_000,
                        width * height * 4,
                    ),
                )
                setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    PROOF_FPS,
                )
                setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL,
                    1,
                )
            }

        val encoder =
            MediaCodec.createByCodecName(
                encoderInfo.name,
            )

        val muxer =
            MediaMuxer(
                temp.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            )

        var muxerStarted = false
        var trackIndex = -1

        val bufferInfo =
            MediaCodec.BufferInfo()

        val bitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888,
            )

        val pixels =
            IntArray(width * height)

        val yuv =
            ByteArray(
                width *
                    height *
                    3 /
                    2,
            )

        try {
            encoder.configure(
                format,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE,
            )
            encoder.start()

            fun drain(
                waitForEos: Boolean,
            ): Boolean {
                var eosReached = false

                while (true) {
                    val outputIndex =
                        encoder.dequeueOutputBuffer(
                            bufferInfo,
                            if (waitForEos) {
                                TIMEOUT_US
                            } else {
                                0L
                            },
                        )

                    when {
                        outputIndex ==
                            MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!waitForEos) {
                                return eosReached
                            }
                        }

                        outputIndex ==
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) {
                                "Encoder output format changed twice"
                            }

                            trackIndex =
                                muxer.addTrack(
                                    encoder.outputFormat,
                                )

                            muxer.start()
                            muxerStarted = true
                        }

                        outputIndex >= 0 -> {
                            val outputBuffer =
                                encoder.getOutputBuffer(
                                    outputIndex,
                                )
                                    ?: error(
                                        "Encoder output buffer unavailable",
                                    )

                            if (
                                bufferInfo.flags and
                                    MediaCodec.BUFFER_FLAG_CODEC_CONFIG !=
                                    0
                            ) {
                                bufferInfo.size = 0
                            }

                            if (
                                bufferInfo.size > 0
                            ) {
                                check(muxerStarted) {
                                    "Muxer not started"
                                }

                                outputBuffer.position(
                                    bufferInfo.offset,
                                )
                                outputBuffer.limit(
                                    bufferInfo.offset +
                                        bufferInfo.size,
                                )

                                muxer.writeSampleData(
                                    trackIndex,
                                    outputBuffer,
                                    bufferInfo,
                                )
                            }

                            eosReached =
                                bufferInfo.flags and
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM !=
                                    0

                            encoder.releaseOutputBuffer(
                                outputIndex,
                                false,
                            )

                            if (eosReached) {
                                return true
                            }
                        }
                    }
                }
            }

            for (
                frameIndex in
                0 until frameCount
            ) {
                val frameTimeMs =
                    startMs +
                        frameIndex *
                        1000L /
                        PROOF_FPS

                val signal =
                    analysis.signalAt(
                        frameTimeMs,
                    )

                bitmap.eraseColor(
                    android.graphics.Color.BLACK,
                )

                HeroThemeRenderer.render(
                    canvas = Canvas(bitmap),
                    width = width,
                    height = height,
                    timeMs = frameTimeMs,
                    themeId = project.themeId,
                    input =
                        ThemeInput(
                            title = title,
                            artist = artist,
                            durationMs =
                                analysis.durationMs,
                            positionMs =
                                frameTimeMs,
                            amplitude =
                                signal.amplitude,
                            bass =
                                signal.bass,
                            mid =
                                signal.mid,
                            high =
                                signal.high,
                            beat =
                                signal.beatStrength,
                        ),
                )

                bitmap.getPixels(
                    pixels,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height,
                )

                convertArgbToYuv420(
                    pixels = pixels,
                    width = width,
                    height = height,
                    output = yuv,
                    semiPlanar =
                        colorFormat ==
                            MediaCodecInfo
                                .CodecCapabilities
                                .COLOR_FormatYUV420SemiPlanar,
                )

                var queued = false

                while (!queued) {
                    val inputIndex =
                        encoder.dequeueInputBuffer(
                            TIMEOUT_US,
                        )

                    if (inputIndex >= 0) {
                        val inputBuffer =
                            encoder.getInputBuffer(
                                inputIndex,
                            )
                                ?: error(
                                    "Encoder input buffer unavailable",
                                )

                        inputBuffer.clear()

                        check(
                            inputBuffer.remaining() >=
                                yuv.size,
                        ) {
                            "Encoder input buffer too small"
                        }

                        inputBuffer.put(yuv)

                        val ptsUs =
                            frameIndex *
                                1_000_000L /
                                PROOF_FPS

                        encoder.queueInputBuffer(
                            inputIndex,
                            0,
                            yuv.size,
                            ptsUs,
                            0,
                        )

                        queued = true
                    }

                    drain(
                        waitForEos = false,
                    )
                }

                drain(
                    waitForEos = false,
                )

                onProgress(
                    (
                        (frameIndex + 1) *
                            100 /
                            frameCount
                        ).coerceIn(
                        0,
                        99,
                    ),
                )
            }

            var eosQueued = false

            while (!eosQueued) {
                val inputIndex =
                    encoder.dequeueInputBuffer(
                        TIMEOUT_US,
                    )

                if (inputIndex >= 0) {
                    encoder.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        frameCount *
                            1_000_000L /
                            PROOF_FPS,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                    eosQueued = true
                }

                drain(
                    waitForEos = false,
                )
            }

            while (
                !drain(
                    waitForEos = true,
                )
            ) {
                // Drain until EOS.
            }

            onProgress(100)
        } finally {
            bitmap.recycle()

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

        val uri =
            publishMp4(
                context = context,
                source = temp,
                displayName =
                    "FARIC-proof-${System.currentTimeMillis()}.mp4",
            )

        temp.delete()

        return Result(
            uri = uri,
            frameCount = frameCount,
            width = width,
            height = height,
            durationMs = durationMs,
        )
    }

    private fun findEncoder():
        MediaCodecInfo? {
        val list =
            MediaCodecList(
                MediaCodecList.REGULAR_CODECS,
            )

        return list.codecInfos
            .asSequence()
            .filter {
                it.isEncoder
            }
            .filter {
                it.supportedTypes.any {
                        type ->
                    type.equals(
                        MIME,
                        ignoreCase = true,
                    )
                }
            }
            .firstOrNull {
                    info ->
                runCatching {
                    val formats =
                        info
                            .getCapabilitiesForType(
                                MIME,
                            )
                            .colorFormats
                            .toSet()

                    formats.any {
                        it in
                            supportedColorFormats
                    }
                }.getOrDefault(false)
            }
    }

    private fun chooseColorFormat(
        info: MediaCodecInfo,
    ): Int {
        val formats =
            info
                .getCapabilitiesForType(
                    MIME,
                )
                .colorFormats
                .toSet()

        return when {
            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420Planar in
                formats ->
                MediaCodecInfo
                    .CodecCapabilities
                    .COLOR_FormatYUV420Planar

            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420SemiPlanar in
                formats ->
                MediaCodecInfo
                    .CodecCapabilities
                    .COLOR_FormatYUV420SemiPlanar

            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420Flexible in
                formats ->
                MediaCodecInfo
                    .CodecCapabilities
                    .COLOR_FormatYUV420Flexible

            else ->
                error(
                    "Encoder does not expose supported YUV420 input",
                )
        }
    }

    private fun proofSize(
        width: Int,
        height: Int,
    ): Pair<Int, Int> {
        val longEdge =
            max(
                width,
                height,
            )

        if (longEdge <= 960) {
            return width.even() to
                height.even()
        }

        val scale =
            960f /
                longEdge

        return (
            width *
                scale
            )
            .toInt()
            .even() to
            (
                height *
                    scale
                )
                .toInt()
                .even()
    }

    private fun convertArgbToYuv420(
        pixels: IntArray,
        width: Int,
        height: Int,
        output: ByteArray,
        semiPlanar: Boolean,
    ) {
        val frameSize =
            width *
                height

        var yIndex = 0

        val uPlane =
            frameSize

        val vPlane =
            if (semiPlanar) {
                frameSize +
                    1
            } else {
                frameSize +
                    frameSize /
                        4
            }

        for (
            y in
            0 until height
        ) {
            for (
                x in
                0 until width
            ) {
                val color =
                    pixels[
                        y *
                            width +
                            x
                    ]

                val r =
                    color shr 16 and
                        0xff

                val g =
                    color shr 8 and
                        0xff

                val b =
                    color and
                        0xff

                val yy =
                    (
                        0.257f *
                            r +
                            0.504f *
                            g +
                            0.098f *
                            b +
                            16f
                        )
                        .toInt()
                        .coerceIn(
                            0,
                            255,
                        )

                output[yIndex++] =
                    yy.toByte()
            }
        }

        var chromaIndex = 0

        for (
            y in
            0 until height
            step 2
        ) {
            for (
                x in
                0 until width
                step 2
            ) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                for (
                    dy in
                    0..1
                ) {
                    for (
                        dx in
                        0..1
                    ) {
                        val px =
                            x +
                                dx
                        val py =
                            y +
                                dy

                        if (
                            px >= width ||
                            py >= height
                        ) {
                            continue
                        }

                        val color =
                            pixels[
                                py *
                                    width +
                                    px
                            ]

                        rSum +=
                            color shr 16 and
                                0xff
                        gSum +=
                            color shr 8 and
                                0xff
                        bSum +=
                            color and
                                0xff
                        count++
                    }
                }

                val r =
                    rSum /
                        count
                val g =
                    gSum /
                        count
                val b =
                    bSum /
                        count

                val u =
                    (
                        -0.148f *
                            r -
                            0.291f *
                            g +
                            0.439f *
                            b +
                            128f
                        )
                        .toInt()
                        .coerceIn(
                            0,
                            255,
                        )

                val v =
                    (
                        0.439f *
                            r -
                            0.368f *
                            g -
                            0.071f *
                            b +
                            128f
                        )
                        .toInt()
                        .coerceIn(
                            0,
                            255,
                        )

                if (semiPlanar) {
                    val index =
                        frameSize +
                            chromaIndex *
                                2

                    output[index] =
                        u.toByte()
                    output[index + 1] =
                        v.toByte()
                } else {
                    output[
                        uPlane +
                            chromaIndex
                    ] =
                        u.toByte()

                    output[
                        vPlane +
                            chromaIndex
                    ] =
                        v.toByte()
                }

                chromaIndex++
            }
        }
    }

    private fun publishMp4(
        context: Context,
        source: File,
        displayName: String,
    ): Uri? {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Video.Media.DISPLAY_NAME,
                        displayName,
                    )
                    put(
                        MediaStore.Video.Media.MIME_TYPE,
                        "video/mp4",
                    )
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/FARIC",
                    )
                    put(
                        MediaStore.Video.Media.IS_PENDING,
                        1,
                    )
                }

            val resolver =
                context.contentResolver

            val uri =
                resolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values,
                )
                    ?: return null

            try {
                resolver
                    .openOutputStream(
                        uri,
                    )
                    ?.use {
                            output ->
                        FileInputStream(
                            source,
                        ).use {
                                input ->
                            input.copyTo(
                                output,
                            )
                        }
                    }

                values.clear()
                values.put(
                    MediaStore.Video.Media.IS_PENDING,
                    0,
                )

                resolver.update(
                    uri,
                    values,
                    null,
                    null,
                )

                uri
            } catch (
                error: Throwable,
            ) {
                resolver.delete(
                    uri,
                    null,
                    null,
                )
                throw error
            }
        } else {
            val directory =
                File(
                    context.getExternalFilesDir(
                        Environment.DIRECTORY_MOVIES,
                    ),
                    "FARIC",
                )
            directory.mkdirs()

            val target =
                File(
                    directory,
                    displayName,
                )

            FileInputStream(
                source,
            ).use {
                    input ->
                FileOutputStream(
                    target,
                ).use {
                        output ->
                    input.copyTo(
                        output,
                    )
                }
            }

            Uri.fromFile(target)
        }
    }

    private val supportedColorFormats =
        setOf(
            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420Planar,
            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo
                .CodecCapabilities
                .COLOR_FormatYUV420Flexible,
        )

    private fun Int.even(): Int =
        if (this % 2 == 0) {
            this
        } else {
            this - 1
        }
}
