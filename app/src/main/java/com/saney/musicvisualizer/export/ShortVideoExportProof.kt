package com.saney.musicvisualizer.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.saney.musicvisualizer.projectm.FaricForegroundSample
import com.saney.musicvisualizer.projectm.ProjectMOfflineTiming
import com.saney.musicvisualizer.projectm.ProjectMPerformanceProfile
import com.saney.musicvisualizer.theme.HeroThemeRenderer
import com.saney.musicvisualizer.theme.MusicVideoProject
import com.saney.musicvisualizer.theme.ThemeInput
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CancellationException
import kotlin.math.max

object ShortVideoExportProof {
    data class DirectGpuProjectMConfig(
        val renderWidth: Int,
        val renderHeight: Int,
        val presetPath: String,
        val texturePath: String,
        val profile:
            ProjectMPerformanceProfile,
        val foregroundSample:
            FaricForegroundSample,
        val pcmProvider:
            (Int, Long) -> ShortArray,
    )

    data class Result(
        val uri: Uri?,
        val frameCount: Int,
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val hasAudio: Boolean,
        val projectMFrameMs: Long,
        val compositionMs: Long,
        val encoderSubmitMs: Long,
        val gpuProjectMMs: Long,
        val gpuGlowMs: Long,
        val gpuFrameMs: Long,
        val gpuCreatureMs: Long,
        val gpuWordmarkMs: Long,
        val gpuOverlayMs: Long,
        val audioTranscodeMs: Long,
        val muxMs: Long,
        val publishMs: Long,
        val totalMs: Long,
        val compositionStages:
            CompositionStageTiming?,
        val projectMDirectBgra:
            Boolean,
        val projectMGpuDirect:
            Boolean,
        val projectMOfflineStages:
            ProjectMOfflineTiming?,
    )

    private const val MIME = "video/avc"
    private const val DEFAULT_FPS = 30
    private const val DEFAULT_DURATION_MS = 3_000L
    private const val TIMEOUT_US = 10_000L

    fun export(
        context: Context,
        sourceAudioUri: Uri,
        project: MusicVideoProject,
        analysis: OfflineAnalysisResult,
        title: String,
        artist: String,
        startMs: Long,
        cyberSharkConfig:
            CyberSharkExportConfig? = null,
        compositionConfig:
            CompositionExportConfig? = null,
        directGpuProjectMConfig:
            DirectGpuProjectMConfig? = null,
        projectMFrameProvider:
            ((Int, Long) -> Bitmap?)? = null,
        projectMRawChannelsCorrectProvider:
            (() -> Boolean)? = null,
        projectMOfflineTimingProvider:
            (() -> ProjectMOfflineTiming?)? = null,
        requestedDurationMs: Long =
            DEFAULT_DURATION_MS,
        fps: Int =
            DEFAULT_FPS,
        displayNamePrefix: String =
            "FARIC-proof",
        shouldCancel: () -> Boolean = {
            false
        },
        onProgress: (Int) -> Unit = {},
    ): Result {
        val totalStartedNs =
            System.nanoTime()

        val (width, height) =
            proofSize(
                project.aspectRatio.width,
                project.aspectRatio.height,
            )

        val availableMs =
            (analysis.durationMs - startMs)
                .coerceAtLeast(0L)

        val safeFps =
            fps.coerceIn(
                1,
                60,
            )

        val durationMs =
            minOf(
                requestedDurationMs
                    .coerceAtLeast(
                        500L,
                    ),
                availableMs,
            ).coerceAtLeast(500L)

        val frameCount =
            max(
                1,
                (
                    durationMs *
                        safeFps /
                        1000L
                    ).toInt(),
            )

        val videoTemp =
            File(
                context.cacheDir,
                "faric-proof-${System.currentTimeMillis()}.mp4",
            )

        val encoderInfo =
            findSurfaceEncoder()
                ?: error(
                    "H.264 Surface encoder недоступний",
                )

        val format =
            MediaFormat.createVideoFormat(
                MIME,
                width,
                height,
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo
                        .CodecCapabilities
                        .COLOR_FormatSurface,
                )
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    max(
                        8_000_000,
                        width * height * 6,
                    ),
                )
                setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    safeFps,
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
                videoTemp.absolutePath,
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

        val useGpuCyberSharkGlow =
            compositionConfig
                ?.let { config ->
                    config
                        .graphicFiguresVisible &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.BACKGROUND
                            ] != false
                } ==
                true

        val overlayBitmap =
            if (useGpuCyberSharkGlow) {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            } else {
                null
            }

        val useGpuCyberSharkFrame =
            compositionConfig
                ?.let { config ->
                    useGpuCyberSharkGlow &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.FRAME
                            ] != false
                } ==
                true

        val postFrameOverlayBitmap =
            if (useGpuCyberSharkFrame) {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            } else {
                null
            }

        val useGpuCyberSharkCreatureWordmark =
            compositionConfig
                ?.let { config ->
                    useGpuCyberSharkFrame &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.CREATURE
                            ] != false &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.WORDMARK
                            ] != false
                } ==
                true

        val postWordmarkOverlayBitmap =
            if (
                useGpuCyberSharkCreatureWordmark
            ) {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            } else {
                null
            }

        val cyberSharkRenderer =
            if (
                project.themeId ==
                    com.saney.musicvisualizer.theme
                        .PlaybackThemeId.CYBER_SHARK &&
                cyberSharkConfig != null
            ) {
                CyberSharkExportRenderer(
                    context,
                )
            } else {
                null
            }

        val compositionRenderer =
            compositionConfig
                ?.let { config ->
                    CompositionExportRenderer(
                        context = context,
                        config = config,
                    )
                }

        var encoderSurface:
            EglBitmapEncoderSurface? =
            null

        var projectMFrameNs =
            0L
        var compositionNs =
            0L
        var encoderSubmitNs =
            0L
        var gpuProjectMNs =
            0L
        var gpuGlowNs =
            0L
        var gpuFrameNs =
            0L
        var gpuCreatureNs =
            0L
        var gpuWordmarkNs =
            0L
        var gpuOverlayNs =
            0L
        var projectMDirectBgra =
            false
        var projectMGpuDirect =
            false

        try {
            encoder.configure(
                format,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE,
            )

            val codecInputSurface =
                encoder.createInputSurface()

            encoder.start()

            encoderSurface =
                EglBitmapEncoderSurface(
                    surface = codecInputSurface,
                    width = width,
                    height = height,
                )

            directGpuProjectMConfig
                ?.let { gpuConfig ->
                    encoderSurface
                        ?.initializeOfflineProjectM(
                            renderWidth =
                                gpuConfig
                                    .renderWidth,
                            renderHeight =
                                gpuConfig
                                    .renderHeight,
                            presetPath =
                                gpuConfig
                                    .presetPath,
                            texturePath =
                                gpuConfig
                                    .texturePath,
                            profile =
                                gpuConfig
                                    .profile,
                            foregroundSample =
                                gpuConfig
                                    .foregroundSample,
                        )
                    projectMGpuDirect =
                        encoderSurface
                            ?.hasDirectProjectM() ==
                            true
                }

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
                if (
                    shouldCancel()
                ) {
                    throw CancellationException(
                        "Export cancelled",
                    )
                }

                val frameTimeMs =
                    startMs +
                        frameIndex *
                        1000L /
                        safeFps

                val signal =
                    analysis.signalAt(
                        frameTimeMs,
                    )

                val projectMStartedNs =
                    System.nanoTime()

                val directGpuProjectMForFrame =
                    projectMGpuDirect &&
                        compositionRenderer !=
                            null &&
                        compositionConfig
                            ?.projectMVisible ==
                            true

                val dynamicProjectMFrame =
                    if (
                        directGpuProjectMForFrame
                    ) {
                        val gpuConfig =
                            directGpuProjectMConfig
                                ?: error(
                                    "Direct GPU projectM config missing",
                                )
                        val pcm =
                            gpuConfig
                                .pcmProvider(
                                    frameIndex,
                                    frameTimeMs,
                                )

                        encoderSurface
                            ?.renderOfflineProjectM(
                                frameTimeSeconds =
                                    frameIndex
                                        .toDouble() /
                                        safeFps,
                                pcm = pcm,
                                signal = signal,
                            )
                            ?: error(
                                "Direct GPU projectM surface unavailable",
                            )

                        null
                    } else if (
                        compositionRenderer !=
                            null &&
                        compositionConfig
                            ?.projectMVisible ==
                            true
                    ) {
                        projectMFrameProvider
                            ?.invoke(
                                frameIndex,
                                frameTimeMs,
                            )
                    } else {
                        null
                    }

                projectMFrameNs +=
                    System.nanoTime() -
                        projectMStartedNs

                projectMDirectBgra =
                    if (
                        directGpuProjectMForFrame
                    ) {
                        true
                    } else {
                        projectMRawChannelsCorrectProvider
                            ?.invoke() ==
                            true
                    }

                val directGpuProjectMBase =
                    useGpuCyberSharkGlow &&
                        overlayBitmap != null &&
                        (
                            directGpuProjectMForFrame ||
                                (
                                    dynamicProjectMFrame !=
                                        null &&
                                        projectMDirectBgra &&
                                        dynamicProjectMFrame
                                            .width >
                                            0 &&
                                        dynamicProjectMFrame
                                            .height >
                                            0
                                    )
                            ) &&
                        compositionConfig
                            ?.faricReactiveVisible ==
                            false &&
                        compositionConfig
                            .overVisualizationVisible ==
                            false &&
                        compositionConfig
                            .bigEqualizerVisible ==
                            false

                if (!directGpuProjectMBase) {
                    bitmap.eraseColor(
                        Color.BLACK,
                    )
                }

                val canvas =
                    Canvas(
                        bitmap,
                    )

                val compositionStartedNs =
                    System.nanoTime()

                var gpuGlow:
                    CyberSharkGpuGlow? =
                    null
                var gpuFrame:
                    CyberSharkGpuFrame? =
                    null
                var gpuCreature:
                    CyberSharkGpuFrame? =
                    null
                var gpuWordmark:
                    CyberSharkGpuFrame? =
                    null

                if (
                    compositionRenderer != null
                ) {
                    if (
                        useGpuCyberSharkGlow &&
                        overlayBitmap != null
                    ) {
                        if (!directGpuProjectMBase) {
                            compositionRenderer
                                .renderBeforeCyberShark(
                                    canvas = canvas,
                                    width = width,
                                    height = height,
                                    timeMs =
                                        frameTimeMs,
                                    signal = signal,
                                    projectMFrameOverride =
                                        dynamicProjectMFrame,
                                    projectMRawChannelsCorrect =
                                        projectMDirectBgra,
                                )
                        }

                        if (
                            directGpuProjectMBase &&
                            useGpuCyberSharkFrame &&
                            postFrameOverlayBitmap !=
                                null
                        ) {
                            gpuFrame =
                                compositionRenderer
                                    .gpuCyberSharkFrame(
                                        width = width,
                                        height = height,
                                        timeMs =
                                            frameTimeMs,
                                        signal = signal,
                                    )

                            if (gpuFrame != null) {
                                overlayBitmap
                                    .eraseColor(
                                        Color.TRANSPARENT,
                                    )

                                compositionRenderer
                                    .renderCyberSharkBackgroundAfterGlow(
                                        canvas =
                                            Canvas(
                                                overlayBitmap,
                                            ),
                                        width = width,
                                        height = height,
                                        timeMs =
                                            frameTimeMs,
                                        signal = signal,
                                    )

                                gpuGlow =
                                    compositionRenderer
                                        .gpuCyberSharkGlow()

                                if (
                                    directGpuProjectMForFrame &&
                                    useGpuCyberSharkCreatureWordmark &&
                                    postWordmarkOverlayBitmap !=
                                        null
                                ) {
                                    gpuCreature =
                                        compositionRenderer
                                            .gpuCyberSharkCreature(
                                                width = width,
                                                height = height,
                                                timeMs =
                                                    frameTimeMs,
                                                signal = signal,
                                            )
                                    gpuWordmark =
                                        compositionRenderer
                                            .gpuCyberSharkWordmark(
                                                width = width,
                                                height = height,
                                                timeMs =
                                                    frameTimeMs,
                                                signal = signal,
                                            )
                                }

                                if (
                                    gpuCreature != null &&
                                    gpuWordmark != null &&
                                    postWordmarkOverlayBitmap !=
                                        null
                                ) {
                                    postFrameOverlayBitmap
                                        .eraseColor(
                                            Color.TRANSPARENT,
                                        )

                                    compositionRenderer
                                        .renderCyberSharkFxAfterFrame(
                                            canvas =
                                                Canvas(
                                                    postFrameOverlayBitmap,
                                                ),
                                            width = width,
                                            height = height,
                                            timeMs =
                                                frameTimeMs,
                                            signal = signal,
                                        )

                                    postWordmarkOverlayBitmap
                                        .eraseColor(
                                            Color.TRANSPARENT,
                                        )

                                    compositionRenderer
                                        .renderAfterCyberSharkWordmark(
                                            canvas =
                                                Canvas(
                                                    postWordmarkOverlayBitmap,
                                                ),
                                            width = width,
                                            height = height,
                                            timeMs =
                                                frameTimeMs,
                                            signal = signal,
                                            title = title,
                                            artist = artist,
                                            durationMs =
                                                analysis.durationMs,
                                            playing = true,
                                        )
                                } else {
                                    postFrameOverlayBitmap
                                        .eraseColor(
                                            Color.TRANSPARENT,
                                        )

                                    compositionRenderer
                                        .renderAfterCyberSharkFrame(
                                            canvas =
                                                Canvas(
                                                    postFrameOverlayBitmap,
                                                ),
                                            width = width,
                                            height = height,
                                            timeMs =
                                                frameTimeMs,
                                            signal = signal,
                                            title = title,
                                            artist = artist,
                                            durationMs =
                                                analysis.durationMs,
                                            playing = true,
                                        )
                                }
                            }
                        }

                        if (gpuFrame == null) {
                            overlayBitmap.eraseColor(
                                Color.TRANSPARENT,
                            )

                            compositionRenderer
                                .renderAfterCyberSharkGlow(
                                    canvas =
                                        Canvas(
                                            overlayBitmap,
                                        ),
                                    width = width,
                                    height = height,
                                    timeMs =
                                        frameTimeMs,
                                    signal = signal,
                                    title = title,
                                    artist = artist,
                                    durationMs =
                                        analysis.durationMs,
                                    playing = true,
                                )

                            gpuGlow =
                                compositionRenderer
                                    .gpuCyberSharkGlow()
                        }

                        if (gpuGlow == null) {
                            bitmap.eraseColor(
                                Color.BLACK,
                            )
                            compositionRenderer
                                .render(
                                    canvas =
                                        Canvas(
                                            bitmap,
                                        ),
                                    width = width,
                                    height = height,
                                    timeMs =
                                        frameTimeMs,
                                    signal = signal,
                                    title = title,
                                    artist = artist,
                                    durationMs =
                                        analysis.durationMs,
                                    playing = true,
                                    projectMFrameOverride =
                                        dynamicProjectMFrame,
                                    projectMRawChannelsCorrect =
                                        projectMDirectBgra,
                                )
                        }
                    } else {
                        compositionRenderer.render(
                            canvas = canvas,
                            width = width,
                            height = height,
                            timeMs = frameTimeMs,
                            signal = signal,
                            title = title,
                            artist = artist,
                            durationMs =
                                analysis.durationMs,
                            playing = true,
                            projectMFrameOverride =
                                dynamicProjectMFrame,
                            projectMRawChannelsCorrect =
                                projectMDirectBgra,
                        )
                    }
                } else if (
                    cyberSharkRenderer != null &&
                    cyberSharkConfig != null
                ) {
                    cyberSharkRenderer.render(
                        canvas = canvas,
                        width = width,
                        height = height,
                        timeMs = frameTimeMs,
                        signal = signal,
                        config = cyberSharkConfig,
                    )
                } else {
                    HeroThemeRenderer.render(
                        canvas = canvas,
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
                }

                compositionNs +=
                    System.nanoTime() -
                        compositionStartedNs

                // Offline projectM returns a reusable framebuffer bitmap.
                // Do not recycle it here; the GL view owns and reuses it.
                val encoderStartedNs =
                    System.nanoTime()

                val presentationTimeNs =
                    frameIndex *
                        1_000_000_000L /
                        safeFps

                val activeEncoderSurface =
                    encoderSurface
                        ?: error(
                            "Encoder EGL surface unavailable",
                        )

                if (
                    gpuGlow != null &&
                    overlayBitmap != null
                ) {
                    val gpuTiming =
                        if (
                            directGpuProjectMForFrame &&
                            gpuFrame != null &&
                            gpuCreature != null &&
                            gpuWordmark != null &&
                            postFrameOverlayBitmap !=
                                null &&
                            postWordmarkOverlayBitmap !=
                                null
                        ) {
                            activeEncoderSurface
                                .drawGpuProjectMCyberSharkComposite(
                                    lowerOverlayBitmap =
                                        overlayBitmap,
                                    frame =
                                        gpuFrame,
                                    fxOverlayBitmap =
                                        postFrameOverlayBitmap,
                                    creature =
                                        gpuCreature,
                                    wordmark =
                                        gpuWordmark,
                                    topOverlayBitmap =
                                        postWordmarkOverlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        } else if (
                            directGpuProjectMForFrame &&
                            gpuFrame != null &&
                            postFrameOverlayBitmap !=
                                null
                        ) {
                            activeEncoderSurface
                                .drawGpuProjectMFrameComposite(
                                    lowerOverlayBitmap =
                                        overlayBitmap,
                                    frame =
                                        gpuFrame,
                                    upperOverlayBitmap =
                                        postFrameOverlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        } else if (
                            directGpuProjectMForFrame
                        ) {
                            activeEncoderSurface
                                .drawGpuProjectMComposite(
                                    overlayBitmap =
                                        overlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        } else if (
                            directGpuProjectMBase &&
                            dynamicProjectMFrame !=
                                null &&
                            gpuFrame != null &&
                            postFrameOverlayBitmap !=
                                null
                        ) {
                            activeEncoderSurface
                                .drawProjectMFrameComposite(
                                    projectMBitmap =
                                        dynamicProjectMFrame,
                                    lowerOverlayBitmap =
                                        overlayBitmap,
                                    frame =
                                        gpuFrame,
                                    upperOverlayBitmap =
                                        postFrameOverlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        } else if (
                            directGpuProjectMBase &&
                            dynamicProjectMFrame !=
                                null
                        ) {
                            activeEncoderSurface
                                .drawProjectMComposite(
                                    projectMBitmap =
                                        dynamicProjectMFrame,
                                    overlayBitmap =
                                        overlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        } else {
                            activeEncoderSurface
                                .drawComposite(
                                    baseBitmap =
                                        bitmap,
                                    overlayBitmap =
                                        overlayBitmap,
                                    glow =
                                        gpuGlow,
                                    presentationTimeNs =
                                        presentationTimeNs,
                                )
                        }

                    gpuProjectMNs +=
                        gpuTiming.projectMNs
                    gpuGlowNs +=
                        gpuTiming.glowNs
                    gpuFrameNs +=
                        gpuTiming.frameNs
                    gpuCreatureNs +=
                        gpuTiming.creatureNs
                    gpuWordmarkNs +=
                        gpuTiming.wordmarkNs
                    gpuOverlayNs +=
                        gpuTiming.overlayNs
                } else {
                    activeEncoderSurface
                        .draw(
                            bitmap = bitmap,
                            presentationTimeNs =
                                presentationTimeNs,
                        )
                }

                drain(
                    waitForEos = false,
                )

                encoderSubmitNs +=
                    System.nanoTime() -
                        encoderStartedNs

                onProgress(
                    (
                        (frameIndex + 1) *
                            75 /
                            frameCount
                        ).coerceIn(
                        0,
                        99,
                    ),
                )
            }

            encoder.signalEndOfInputStream()

            while (
                !drain(
                    waitForEos = true,
                )
            ) {
                // Drain until EOS.
            }

            onProgress(78)
        } finally {
            bitmap.recycle()
            overlayBitmap
                ?.recycle()
            postFrameOverlayBitmap
                ?.recycle()
            postWordmarkOverlayBitmap
                ?.recycle()

            runCatching {
                encoderSurface
                    ?.close()
            }

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

        val audioTemp =
            File(
                context.cacheDir,
                "faric-proof-audio-${System.currentTimeMillis()}.m4a",
            )

        val muxedTemp =
            File(
                context.cacheDir,
                "faric-proof-av-${System.currentTimeMillis()}.mp4",
            )

        try {
            if (
                shouldCancel()
            ) {
                throw CancellationException(
                    "Export cancelled",
                )
            }

            val audioStartedNs =
                System.nanoTime()

            AudioClipTranscoder.transcodeToAacMp4(
                context = context,
                sourceUri = sourceAudioUri,
                startMs = startMs,
                durationMs = durationMs,
                outputFile = audioTemp,
            )

            val audioTranscodeMs =
                (
                    System.nanoTime() -
                        audioStartedNs
                    ) /
                    1_000_000L

            onProgress(92)

            val muxStartedNs =
                System.nanoTime()

            Mp4AvMuxer.mux(
                videoFile = videoTemp,
                audioFile = audioTemp,
                outputFile = muxedTemp,
            )

            val muxMs =
                (
                    System.nanoTime() -
                        muxStartedNs
                    ) /
                    1_000_000L

            onProgress(100)

            val publishStartedNs =
                System.nanoTime()

            val uri =
                publishMp4(
                    context = context,
                    source = muxedTemp,
                    displayName =
                        "$displayNamePrefix-${System.currentTimeMillis()}.mp4",
                )

            val publishMs =
                (
                    System.nanoTime() -
                        publishStartedNs
                    ) /
                    1_000_000L

            return Result(
                uri = uri,
                frameCount = frameCount,
                width = width,
                height = height,
                durationMs = durationMs,
                hasAudio = true,
                projectMFrameMs =
                    projectMFrameNs /
                        1_000_000L,
                compositionMs =
                    compositionNs /
                        1_000_000L,
                encoderSubmitMs =
                    encoderSubmitNs /
                        1_000_000L,
                gpuProjectMMs =
                    gpuProjectMNs /
                        1_000_000L,
                gpuGlowMs =
                    gpuGlowNs /
                        1_000_000L,
                gpuFrameMs =
                    gpuFrameNs /
                        1_000_000L,
                gpuCreatureMs =
                    gpuCreatureNs /
                        1_000_000L,
                gpuWordmarkMs =
                    gpuWordmarkNs /
                        1_000_000L,
                gpuOverlayMs =
                    gpuOverlayNs /
                        1_000_000L,
                audioTranscodeMs =
                    audioTranscodeMs,
                muxMs =
                    muxMs,
                publishMs =
                    publishMs,
                totalMs =
                    (
                        System.nanoTime() -
                            totalStartedNs
                        ) /
                        1_000_000L,
                compositionStages =
                    compositionRenderer
                        ?.timingSnapshot(),
                projectMDirectBgra =
                    projectMDirectBgra,
                projectMGpuDirect =
                    projectMGpuDirect,
                projectMOfflineStages =
                    projectMOfflineTimingProvider
                        ?.invoke(),
            )
        } finally {
            videoTemp.delete()
            audioTemp.delete()
            muxedTemp.delete()
        }
    }

    private fun findSurfaceEncoder():
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
                    info
                        .getCapabilitiesForType(
                            MIME,
                        )
                        .colorFormats
                        .contains(
                            MediaCodecInfo
                                .CodecCapabilities
                                .COLOR_FormatSurface,
                        )
                }.getOrDefault(false)
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

        if (longEdge <= 1920) {
            return width.even() to
                height.even()
        }

        val scale =
            1920f /
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

    private fun Int.even(): Int =
        if (this % 2 == 0) {
            this
        } else {
            this - 1
        }
}
