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
import android.util.Log
import com.saney.musicvisualizer.board.GraphicFigureCatalog
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
        /** V115: whole-frame time per visualization (slowest first) — which ones make the export slow. */
        val presetTimings: List<PresetTiming> = emptyList(),
    )

    /**
     * V130: the export was cancelled (or stopped) part way — [partial] holds the numbers up to that
     * frame, [plannedFrames] how many the whole video would have had. Still a CancellationException,
     * so every «cancelled» check keeps working.
     */
    class ExportCancelledException(
        val partial: Result,
        val plannedFrames: Int,
    ) : CancellationException("Export cancelled")

    /** V115: one visualization in the MP4 — [frames] frames, [totalMs] of whole-frame time. */
    data class PresetTiming(
        val path: String,
        val frames: Int,
        val totalMs: Long,
    ) {
        val msPerFrame: Long get() = if (frames > 0) totalMs / frames else 0L
    }

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
        /** V94: visualization for a time of THIS video (ms from its start); null = keep the one loaded. */
        presetForVideoTime:
            ((Long) -> String?)? = null,
        /** V94, path B (projectM on its own view): load a visualization there (V198: + blend seconds or null). */
        loadPresetOnView:
            ((String, Double?) -> Unit)? = null,
        /** V198: how long one visualization flows into the next (0 = hard cut, as before). */
        presetBlendMs: Int = 0,
        requestedDurationMs: Long =
            DEFAULT_DURATION_MS,
        fps: Int =
            DEFAULT_FPS,
        displayNamePrefix: String =
            "PulseDeck-proof",
        // Batch export (v0.19.73): exact file name and Movies/PulseDeck/<folder> (Movies/FARIC before v0.19.87).
        exactDisplayName: String? = null,
        relativePath: String = Mp4AvMuxer.DEFAULT_RELATIVE_PATH,
        /** V93: song tags for the MP4 (title, artist, album, year, genre, cover). */
        tags: Mp4Tagger.Tags? = null,
        /** V109: a small copy of the MP4 frame about once a second (progress window). */
        onPreviewFrame: ((android.graphics.Bitmap) -> Unit)? = null,
        shouldCancel: () -> Boolean = {
            false
        },
        /** V172: 0–100 with tenths — [ExportProgress] shares (picture · sound · file). */
        onProgress: (Float) -> Unit = {},
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

        if (durationMs >= 20_000L) {
            // Temp H264 and the MediaStore destination coexist during muxing.
            // Check BEFORE a long render, not after hundreds of MB were written.
            val estimate = ExportStorageBudget.estimateRequiredBytes(
                durationMs = durationMs,
                width = width,
                height = height,
                needsPcm = compositionConfig?.projectMVisible == true,
            )
            val available = context.cacheDir.usableSpace
            Log.i("FARIC-export", "Storage preflight requiredMiB=" +
                ExportStorageBudget.formatMiB(estimate) + " availableMiB=" +
                ExportStorageBudget.formatMiB(available))
            check(ExportStorageBudget.hasCapacity(available, estimate)) {
                "Недостатньо місця для повного MP4: потрібно приблизно " +
                    ExportStorageBudget.formatMiB(estimate) + " МБ, доступно " +
                    ExportStorageBudget.formatMiB(available) + " МБ. " +
                    "Звільни місце та повтори експорт."
            }
        }

        val videoTemp =
            File(
                context.cacheDir,
                "faric-proof-${System.currentTimeMillis()}.mp4",
            )

        try {
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
                // V113 (faric 21.10 14:49: «кнопки у відео моросять»): on a busy projectM background the
                // fine HUD details ran out of bits (blocky «mosquito» noise). More bits, keyframe every 2 s.
                setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    ExportStorageBudget.videoBitRate(width, height).toInt(),
                )
                setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    safeFps,
                )
                setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL,
                    2,
                )
                // High profile packs the same picture into fewer bits (when the encoder has it).
                highProfileLevel(encoderInfo)?.let { (profile, level) ->
                    setInteger(MediaFormat.KEY_PROFILE, profile)
                    setInteger(MediaFormat.KEY_LEVEL, level)
                }
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

        val directGpuCompositionCandidate =
            compositionConfig != null &&
                compositionConfig.projectMVisible &&
                directGpuProjectMConfig != null

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

        val useGpuCyberSharkFrame =
            compositionConfig
                ?.let { config ->
                    config
                        .graphicFiguresVisible &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.FRAME
                            ] != false
                } ==
                true

        val useGpuCyberSharkCreature =
            compositionConfig
                ?.let { config ->
                    config
                        .graphicFiguresVisible &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.CREATURE
                            ] != false
                } ==
                true

        val useGpuCyberSharkWordmark =
            compositionConfig
                ?.let { config ->
                    config
                        .graphicFiguresVisible &&
                        config
                            .cyberSharkConfig
                            .visibility[
                                com.saney.musicvisualizer.board
                                    .BoardLayerId.WORDMARK
                            ] != false
                } ==
                true

        val overlayBitmap =
            if (
                useGpuCyberSharkGlow ||
                directGpuCompositionCandidate
            ) {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            } else {
                null
            }

        val postFrameOverlayBitmap =
            if (
                useGpuCyberSharkFrame ||
                directGpuCompositionCandidate
            ) {
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            } else {
                null
            }

        val postWordmarkOverlayBitmap =
            if (
                useGpuCyberSharkCreature ||
                useGpuCyberSharkWordmark ||
                directGpuCompositionCandidate
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
                project.themeId in
                    GraphicFigureCatalog.ids &&
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

        val presetFrameStats = HashMap<String, LongArray>()
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
            val configured =
                runCatching {
                    encoder.configure(
                        format,
                        null,
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE,
                    )
                }
            if (configured.isFailure && format.containsKey(MediaFormat.KEY_PROFILE)) {
                // V113: an encoder that refuses High profile — the old settings, as before.
                Log.w("FARIC-export", "High profile refused; default profile", configured.exceptionOrNull())
                encoder.reset()
                format.removeKey(MediaFormat.KEY_PROFILE)
                format.removeKey(MediaFormat.KEY_LEVEL)
                encoder.configure(
                    format,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE,
                )
            } else {
                configured.getOrThrow()
            }

            val codecInputSurface =
                encoder.createInputSurface()

            encoder.start()

            encoderSurface =
                EglBitmapEncoderSurface(
                    surface = codecInputSurface,
                    width = width,
                    height = height,
                ).also { it.previewSink = onPreviewFrame }

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

            // V94: AUTO in the video — which visualization is loaded in the offline projectM now.
            var loadedPresetPath: String? = directGpuProjectMConfig?.presetPath
            // V115: whole-frame time is booked to the visualization that drew the frame.
            var timedPreset: String? = null
            var frameStartedNs = 0L
            fun bookFrameTime(now: Long) {
                val key = timedPreset ?: return
                val slot = presetFrameStats.getOrPut(key) { LongArray(2) }
                slot[0] += now - frameStartedNs
                slot[1] += 1
            }
            // V130 (faric 22.10 02:58: «скасував на 47 % — дані загубимо»): a cancel brings the numbers so far.
            var framesDone = 0
            try {
            for (
                frameIndex in
                0 until frameCount
            ) {
                framesDone = frameIndex
                if (
                    shouldCancel()
                ) {
                    throw CancellationException(
                        "Export cancelled",
                    )
                }
                System.nanoTime().let { now ->
                    if (frameIndex > 0) bookFrameTime(now)
                    frameStartedNs = now
                }

                presetForVideoTime
                    ?.invoke(frameIndex * 1000L / safeFps)
                    ?.let { wanted ->
                        if (wanted != loadedPresetPath) {
                            loadedPresetPath = wanted
                            // V198: the first frame always starts clean; later switches may blend.
                            val blend =
                                com.saney.musicvisualizer.projectm.PresetBlend.secondsFor(presetBlendMs, firstFrame = frameIndex == 0)
                            if (projectMGpuDirect) {
                                encoderSurface?.loadOfflinePreset(wanted, blend)
                            } else {
                                loadPresetOnView?.invoke(wanted, blend)
                            }
                        }
                    }

                timedPreset = loadedPresetPath ?: "—"

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
                        directGpuProjectMForFrame &&
                        directGpuProjectMBase &&
                        overlayBitmap != null &&
                        postFrameOverlayBitmap !=
                            null &&
                        postWordmarkOverlayBitmap !=
                            null
                    ) {
                        overlayBitmap.eraseColor(
                            Color.TRANSPARENT,
                        )

                        if (useGpuCyberSharkGlow) {
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
                        }

                        gpuFrame =
                            if (useGpuCyberSharkFrame) {
                                compositionRenderer
                                    .gpuCyberSharkFrame(
                                        width = width,
                                        height = height,
                                        timeMs =
                                            frameTimeMs,
                                        signal = signal,
                                    )
                            } else {
                                null
                            }

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

                        gpuCreature =
                            if (useGpuCyberSharkCreature) {
                                compositionRenderer
                                    .gpuCyberSharkCreature(
                                        width = width,
                                        height = height,
                                        timeMs =
                                            frameTimeMs,
                                        signal = signal,
                                    )
                            } else {
                                null
                            }

                        gpuWordmark =
                            if (useGpuCyberSharkWordmark) {
                                compositionRenderer
                                    .gpuCyberSharkWordmark(
                                        width = width,
                                        height = height,
                                        timeMs =
                                            frameTimeMs,
                                        signal = signal,
                                    )
                            } else {
                                null
                            }

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
                    } else if (
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
                                    (useGpuCyberSharkCreature && useGpuCyberSharkWordmark) &&
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
                    directGpuProjectMForFrame &&
                    directGpuProjectMBase &&
                    overlayBitmap != null &&
                    postFrameOverlayBitmap !=
                        null &&
                    postWordmarkOverlayBitmap !=
                        null
                ) {
                    val gpuTiming =
                        activeEncoderSurface
                            .drawGpuProjectMGraphicFigureComposite(
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
                } else if (
                    gpuGlow != null &&
                    overlayBitmap != null
                ) {
                    val gpuTiming =
                        if (
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

                onProgress(ExportProgress.render(frameIndex + 1, frameCount))
            }
            } catch (cancel: CancellationException) {
                bookFrameTime(System.nanoTime())
                throw ExportCancelledException(
                    partial =
                        Result(
                            uri = null,
                            frameCount = framesDone,
                            width = width,
                            height = height,
                            durationMs = framesDone * 1000L / safeFps,
                            hasAudio = false,
                            projectMFrameMs = projectMFrameNs / 1_000_000L,
                            compositionMs = compositionNs / 1_000_000L,
                            encoderSubmitMs = encoderSubmitNs / 1_000_000L,
                            gpuProjectMMs = gpuProjectMNs / 1_000_000L,
                            gpuGlowMs = gpuGlowNs / 1_000_000L,
                            gpuFrameMs = gpuFrameNs / 1_000_000L,
                            gpuCreatureMs = gpuCreatureNs / 1_000_000L,
                            gpuWordmarkMs = gpuWordmarkNs / 1_000_000L,
                            gpuOverlayMs = gpuOverlayNs / 1_000_000L,
                            audioTranscodeMs = 0L,
                            muxMs = 0L,
                            publishMs = 0L,
                            totalMs = (System.nanoTime() - totalStartedNs) / 1_000_000L,
                            compositionStages = compositionRenderer?.timingSnapshot(),
                            projectMDirectBgra = projectMDirectBgra,
                            projectMGpuDirect = projectMGpuDirect,
                            projectMOfflineStages = projectMOfflineTimingProvider?.invoke(),
                            presetTimings =
                                presetFrameStats
                                    .map { (path, slot) -> PresetTiming(path, slot[1].toInt(), slot[0] / 1_000_000L) }
                                    .sortedByDescending { it.msPerFrame },
                        ),
                    plannedFrames = frameCount,
                )
            }
            bookFrameTime(System.nanoTime())

            encoder.signalEndOfInputStream()

            while (
                !drain(
                    waitForEos = true,
                )
            ) {
                // Drain until EOS.
            }

            onProgress(ExportProgress.RENDER_END)
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

            Log.i("FARIC-export", "AAC transcoding start durationMs=$durationMs")
            AudioClipTranscoder.transcodeToAacMp4(
                context = context,
                sourceUri = sourceAudioUri,
                startMs = startMs,
                durationMs = durationMs,
                outputFile = audioTemp,
                shouldCancel = shouldCancel,
                onProgress = { fraction -> onProgress(ExportProgress.audio(fraction)) },
            )
            check(audioTemp.length() > 0L) { "AAC transcode produced an empty file" }
            Log.i("FARIC-export", "AAC transcoding finished bytes=" + audioTemp.length())

            val audioTranscodeMs =
                (
                    System.nanoTime() -
                        audioStartedNs
                    ) /
                    1_000_000L

            onProgress(ExportProgress.AUDIO_END)

            val finalizingStartedNs = System.nanoTime()
            val displayName = exactDisplayName ?: "$displayNamePrefix-${System.currentTimeMillis()}.mp4"
            Log.i("FARIC-export", "Final MP4 mux started: videoBytes=" +
                videoTemp.length() + " audioBytes=" + audioTemp.length() +
                " directMediaStore=" + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q))
            // On Android 10+ mux directly to pending Movies/PulseDeck. A second
            // complete MP4 in cache used to exhaust device storage at the end.
            val uri: Uri?
            val muxMs: Long
            val publishMs: Long
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = Mp4AvMuxer.muxToMediaStore(
                    context = context.applicationContext,
                    videoFile = videoTemp,
                    audioFile = audioTemp,
                    displayName = displayName,
                    shouldCancel = shouldCancel,
                    relativePath = relativePath,
                    tags = tags,
                    onProgress = { fraction -> onProgress(ExportProgress.mux(fraction)) },
                )
                muxMs = (System.nanoTime() - finalizingStartedNs) / 1_000_000L
                publishMs = 0L // Direct FD mux also commits the MediaStore row.
            } else {
                Mp4AvMuxer.mux(
                    videoFile = videoTemp,
                    audioFile = audioTemp,
                    outputFile = muxedTemp,
                    shouldCancel = shouldCancel,
                    tags = tags,
                    onProgress = { fraction -> onProgress(ExportProgress.mux(fraction) * 0.995f) },
                )
                muxMs = (System.nanoTime() - finalizingStartedNs) / 1_000_000L
                if (shouldCancel()) throw CancellationException("Export cancelled")
                onProgress(ExportProgress.MUX_END)
                val publishingStartedNs = System.nanoTime()
                uri = publishMp4(
                    context = context,
                    source = muxedTemp,
                    displayName = displayName,
                )
                publishMs = (System.nanoTime() - publishingStartedNs) / 1_000_000L
            }
            check(uri != null) { "MP4 was not published to ${ExportFolders.VIDEO}" }
            Log.i("FARIC-export", "Final MP4 published: uri=$uri elapsedMs=" +
                ((System.nanoTime() - finalizingStartedNs) / 1_000_000L))
            onProgress(100f)

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
                presetTimings =
                    presetFrameStats
                        .map { (path, slot) -> PresetTiming(path, slot[1].toInt(), slot[0] / 1_000_000L) }
                        .sortedByDescending { it.msPerFrame },
            )
        } finally {
            videoTemp.delete()
            audioTemp.delete()
            muxedTemp.delete()
        }
        } finally {
            // Also clean up if video rendering throws BEFORE the AAC/mux phase.
            videoTemp.delete()
        }
    }

    /** V113: AVC High profile at the best level this encoder offers, or null. */
    private fun highProfileLevel(info: MediaCodecInfo): Pair<Int, Int>? =
        runCatching {
            info.getCapabilitiesForType(MIME).profileLevels
                .filter { it.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh }
                .maxByOrNull { it.level }
                ?.let { it.profile to it.level }
        }.getOrNull()

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
                        ExportFolders.VIDEO,
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
                val output = resolver.openOutputStream(uri)
                    ?: error("Could not open MediaStore MP4 output")
                output.use { stream ->
                    FileInputStream(source).use { input ->
                        input.copyTo(stream)
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
                    ExportFolders.NAME,
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
