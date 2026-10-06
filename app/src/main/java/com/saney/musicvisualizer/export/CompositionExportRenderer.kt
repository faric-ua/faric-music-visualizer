package com.saney.musicvisualizer.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardLayerId
import com.saney.musicvisualizer.scene.SceneSpec
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Snapshot of the current PulseDeck composition used by deterministic export.
 *
 * projectM is intentionally declared separately: the native GLSurfaceView path
 * cannot yet be reproduced by the offline Canvas renderer. The export UI warns
 * when projectM is visible so the user knows the only current gap.
 */
data class CompositionExportConfig(
    val scene: SceneSpec,
    val faricReactiveVisible: Boolean,
    val projectMVisible: Boolean,
    val projectMFrame: Bitmap?,
    val overVisualizationVisible: Boolean,
    val bigEqualizerVisible: Boolean,
    val graphicFiguresVisible: Boolean,
    val cyberSharkConfig: CyberSharkExportConfig,
    val effectsVisible: Boolean,
    val pulseDeckVisible: Boolean,
    val pulseDeckObjectVisibility:
        Map<String, Boolean>,
)

data class CompositionStageTiming(
    val clearMs: Long,
    val projectMDrawMs: Long,
    val faricReactiveMs: Long,
    val overVisualizationMs: Long,
    val bigEqualizerMs: Long,
    val cyberSharkMs: Long,
    val cyberSharkStages:
        CyberSharkStageTiming?,
    val effectsMs: Long,
    val pulseDeckUpdateMs: Long,
    val pulseDeckDrawMs: Long,
)

class CompositionExportRenderer(
    context: Context,
    private val config:
        CompositionExportConfig,
) {
    private val sceneRenderer =
        ReactiveSceneExportRenderer(
            context,
        )

    private val overVisualizationRenderer =
        OverVisualizationExportRenderer(
            context,
        )

    private val bigEqualizerRenderer =
        BigEqualizerExportRenderer()

    private val cyberSharkRenderer =
        CyberSharkExportRenderer(
            context,
        )

    private val projectMPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        )

    private val projectMRawGlPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        ).apply {
            // ProjectM offline fast readback copies GL_RGBA bytes directly
            // into an Android ARGB_8888 bitmap on arm64 little-endian.
            // Swap red/blue here instead of doing a CPU pass over every pixel.
            colorFilter =
                ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            0f, 0f, 1f, 0f, 0f,
                            0f, 1f, 0f, 0f, 0f,
                            1f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
        }

    private val pulseDeckView =
        if (config.pulseDeckVisible) {
            PulseDeckMainSkinView(
                context,
                forceModularMode = true,
                transparentBackground = true,
            ).also { view ->
                config
                    .pulseDeckObjectVisibility
                    .forEach { (id, visible) ->
                        view.setObjectVisible(
                            id,
                            visible,
                        )
                    }

                view.setControlsVisible(
                    visible = true,
                    animate = false,
                )
                view.setChromeVisibility(
                    transportVisible = true,
                    quickActionsVisible = true,
                )
                view.setHudLayersVisible(
                    true,
                )
                view.setEqualizerVisible(
                    true,
                )
            }
        } else {
            null
        }

    private var measuredWidth =
        -1

    private var measuredHeight =
        -1

    private var clearNs =
        0L
    private var projectMDrawNs =
        0L
    private var faricReactiveNs =
        0L
    private var overVisualizationNs =
        0L
    private var bigEqualizerNs =
        0L
    private var cyberSharkNs =
        0L
    private var effectsNs =
        0L
    private var pulseDeckUpdateNs =
        0L
    private var pulseDeckDrawNs =
        0L

    fun timingSnapshot():
        CompositionStageTiming =
        CompositionStageTiming(
            clearMs = clearNs / 1_000_000L,
            projectMDrawMs =
                projectMDrawNs / 1_000_000L,
            faricReactiveMs =
                faricReactiveNs / 1_000_000L,
            overVisualizationMs =
                overVisualizationNs / 1_000_000L,
            bigEqualizerMs =
                bigEqualizerNs / 1_000_000L,
            cyberSharkMs =
                cyberSharkNs / 1_000_000L,
            cyberSharkStages =
                cyberSharkRenderer
                    .timingSnapshot(),
            effectsMs =
                effectsNs / 1_000_000L,
            pulseDeckUpdateMs =
                pulseDeckUpdateNs / 1_000_000L,
            pulseDeckDrawMs =
                pulseDeckDrawNs / 1_000_000L,
        )

    fun gpuCyberSharkGlow():
        CyberSharkGpuGlow? =
        cyberSharkRenderer
            .gpuGlowSnapshot()

    fun renderBeforeCyberShark(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        projectMFrameOverride:
            Bitmap? = null,
        projectMRawChannelsCorrect:
            Boolean = false,
    ) {
        val clearStartedNs =
            System.nanoTime()

        canvas.drawColor(
            Color.BLACK,
        )

        clearNs +=
            System.nanoTime() -
                clearStartedNs

        val projectMFrame =
            projectMFrameOverride
                ?: config.projectMFrame

        if (
            config.projectMVisible &&
            projectMFrame != null
        ) {
            val startedNs =
                System.nanoTime()

            drawProjectMFrame(
                canvas = canvas,
                width = width,
                height = height,
                bitmap =
                    projectMFrame,
                rawGlFrame =
                    projectMFrameOverride !=
                        null,
                rawChannelsCorrect =
                    projectMRawChannelsCorrect,
            )

            projectMDrawNs +=
                System.nanoTime() -
                    startedNs
        }

        if (config.faricReactiveVisible) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderVisualizer(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            faricReactiveNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config
                .overVisualizationVisible
        ) {
            val startedNs =
                System.nanoTime()

            overVisualizationRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                )

            overVisualizationNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config
                .bigEqualizerVisible
        ) {
            val startedNs =
                System.nanoTime()

            bigEqualizerRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    signal = signal,
                )

            bigEqualizerNs +=
                System.nanoTime() -
                    startedNs
        }
    }

    fun gpuCyberSharkFrame(
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
    ): CyberSharkGpuFrame? =
        if (
            config
                .graphicFiguresVisible
        ) {
            cyberSharkRenderer
                .gpuFrame(
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig,
                )
        } else {
            null
        }

    fun gpuCyberSharkCreature(
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
    ): CyberSharkGpuFrame? =
        if (
            config
                .graphicFiguresVisible
        ) {
            cyberSharkRenderer
                .gpuCreature(
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig,
                )
        } else {
            null
        }

    fun gpuCyberSharkWordmark(
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
    ): CyberSharkGpuFrame? =
        if (
            config
                .graphicFiguresVisible
        ) {
            cyberSharkRenderer
                .gpuWordmark(
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig,
                )
        } else {
            null
        }

    fun renderCyberSharkBackgroundAfterGlow(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
    ) {
        if (
            !config
                .graphicFiguresVisible
        ) {
            return
        }

        val visibility =
            config
                .cyberSharkConfig
                .visibility
                .toMutableMap()
                .apply {
                    this[
                        BoardLayerId.FRAME
                    ] =
                        false
                    this[
                        BoardLayerId.FX
                    ] =
                        false
                    this[
                        BoardLayerId.CREATURE
                    ] =
                        false
                    this[
                        BoardLayerId.WORDMARK
                    ] =
                        false
                }

        val startedNs =
            System.nanoTime()

        cyberSharkRenderer
            .render(
                canvas = canvas,
                width = width,
                height = height,
                timeMs = timeMs,
                signal = signal,
                config =
                    config
                        .cyberSharkConfig
                        .copy(
                            visibility =
                                visibility,
                        ),
                skipBackgroundGlow =
                    true,
            )

        cyberSharkNs +=
            System.nanoTime() -
                startedNs
    }

    fun renderCyberSharkFxAfterFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
    ) {
        if (
            !config
                .graphicFiguresVisible
        ) {
            return
        }

        val visibility =
            config
                .cyberSharkConfig
                .visibility
                .toMutableMap()
                .apply {
                    this[
                        BoardLayerId.BACKGROUND
                    ] =
                        false
                    this[
                        BoardLayerId.FRAME
                    ] =
                        false
                    this[
                        BoardLayerId.CREATURE
                    ] =
                        false
                    this[
                        BoardLayerId.WORDMARK
                    ] =
                        false
                }

        val startedNs =
            System.nanoTime()

        cyberSharkRenderer
            .render(
                canvas = canvas,
                width = width,
                height = height,
                timeMs = timeMs,
                signal = signal,
                config =
                    config
                        .cyberSharkConfig
                        .copy(
                            visibility =
                                visibility,
                        ),
                skipBackgroundGlow =
                    true,
            )

        cyberSharkNs +=
            System.nanoTime() -
                startedNs
    }

    fun renderAfterCyberSharkFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        title: String,
        artist: String,
        durationMs: Long,
        playing: Boolean = true,
    ) {
        if (
            config
                .graphicFiguresVisible
        ) {
            val visibility =
                config
                    .cyberSharkConfig
                    .visibility
                    .toMutableMap()
                    .apply {
                        this[
                            BoardLayerId.BACKGROUND
                        ] =
                            false
                        this[
                            BoardLayerId.FRAME
                        ] =
                            false
                    }

            val startedNs =
                System.nanoTime()

            cyberSharkRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig
                            .copy(
                                visibility =
                                    visibility,
                            ),
                    skipBackgroundGlow =
                        true,
                )

            cyberSharkNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config
                .effectsVisible
        ) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderEffects(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            effectsNs +=
                System.nanoTime() -
                    startedNs
        }

        pulseDeckView
            ?.let { view ->
                val updateStartedNs =
                    System.nanoTime()

                ensurePulseDeckSize(
                    view = view,
                    width = width,
                    height = height,
                )

                view.updateSignal(
                    signalForHud(
                        signal = signal,
                        timeMs = timeMs,
                    ),
                )
                view.setPlaying(
                    playing,
                )

                val safeDuration =
                    durationMs
                        .coerceAtLeast(
                            1L,
                        )

                val safePosition =
                    timeMs.coerceIn(
                        0L,
                        safeDuration,
                    )

                view.setPlaybackContent(
                    title =
                        title.ifBlank {
                            "FARIC"
                        },
                    artist = artist,
                    status =
                        if (playing) {
                            "Playing"
                        } else {
                            "Paused"
                        },
                    elapsed =
                        formatTime(
                            safePosition,
                        ),
                    total =
                        formatTime(
                            safeDuration,
                        ),
                    progressFraction =
                        (
                            safePosition
                                .toDouble() /
                                safeDuration
                                    .toDouble()
                            )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f,
                            ),
                )

                pulseDeckUpdateNs +=
                    System.nanoTime() -
                        updateStartedNs

                val drawStartedNs =
                    System.nanoTime()

                view.draw(
                    canvas,
                )

                pulseDeckDrawNs +=
                    System.nanoTime() -
                        drawStartedNs
            }
    }

    fun renderAfterCyberSharkWordmark(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        title: String,
        artist: String,
        durationMs: Long,
        playing: Boolean = true,
    ) {
        if (
            config
                .effectsVisible
        ) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderEffects(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            effectsNs +=
                System.nanoTime() -
                    startedNs
        }

        pulseDeckView
            ?.let { view ->
                val updateStartedNs =
                    System.nanoTime()

                ensurePulseDeckSize(
                    view = view,
                    width = width,
                    height = height,
                )

                view.updateSignal(
                    signalForHud(
                        signal = signal,
                        timeMs = timeMs,
                    ),
                )
                view.setPlaying(
                    playing,
                )

                val safeDuration =
                    durationMs
                        .coerceAtLeast(
                            1L,
                        )

                val safePosition =
                    timeMs.coerceIn(
                        0L,
                        safeDuration,
                    )

                view.setPlaybackContent(
                    title =
                        title.ifBlank {
                            "FARIC"
                        },
                    artist = artist,
                    status =
                        if (playing) {
                            "Playing"
                        } else {
                            "Paused"
                        },
                    elapsed =
                        formatTime(
                            safePosition,
                        ),
                    total =
                        formatTime(
                            safeDuration,
                        ),
                    progressFraction =
                        (
                            safePosition
                                .toDouble() /
                                safeDuration
                                    .toDouble()
                            )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f,
                            ),
                )

                pulseDeckUpdateNs +=
                    System.nanoTime() -
                        updateStartedNs

                val drawStartedNs =
                    System.nanoTime()

                view.draw(
                    canvas,
                )

                pulseDeckDrawNs +=
                    System.nanoTime() -
                        drawStartedNs
            }
    }

    fun renderAfterCyberSharkGlow(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        title: String,
        artist: String,
        durationMs: Long,
        playing: Boolean = true,
    ) {
        if (
            config
                .graphicFiguresVisible
        ) {
            val startedNs =
                System.nanoTime()

            cyberSharkRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig,
                    skipBackgroundGlow =
                        true,
                )

            cyberSharkNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config
                .effectsVisible
        ) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderEffects(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            effectsNs +=
                System.nanoTime() -
                    startedNs
        }

        pulseDeckView
            ?.let { view ->
                val updateStartedNs =
                    System.nanoTime()

                ensurePulseDeckSize(
                    view = view,
                    width = width,
                    height = height,
                )

                view.updateSignal(
                    signalForHud(
                        signal = signal,
                        timeMs = timeMs,
                    ),
                )
                view.setPlaying(
                    playing,
                )

                val safeDuration =
                    durationMs
                        .coerceAtLeast(
                            1L,
                        )

                val safePosition =
                    timeMs.coerceIn(
                        0L,
                        safeDuration,
                    )

                view.setPlaybackContent(
                    title =
                        title.ifBlank {
                            "FARIC"
                        },
                    artist = artist,
                    status =
                        if (playing) {
                            "Playing"
                        } else {
                            "Paused"
                        },
                    elapsed =
                        formatTime(
                            safePosition,
                        ),
                    total =
                        formatTime(
                            safeDuration,
                        ),
                    progressFraction =
                        (
                            safePosition
                                .toDouble() /
                                safeDuration
                                    .toDouble()
                            )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f,
                            ),
                )

                pulseDeckUpdateNs +=
                    System.nanoTime() -
                        updateStartedNs

                val drawStartedNs =
                    System.nanoTime()

                view.draw(
                    canvas,
                )

                pulseDeckDrawNs +=
                    System.nanoTime() -
                        drawStartedNs
            }
    }

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        title: String,
        artist: String,
        durationMs: Long,
        playing: Boolean = true,
        projectMFrameOverride:
            Bitmap? = null,
        projectMRawChannelsCorrect:
            Boolean = false,
    ) {
        val clearStartedNs =
            System.nanoTime()

        canvas.drawColor(
            Color.BLACK,
        )

        clearNs +=
            System.nanoTime() -
                clearStartedNs

        // Layer 0 · projectM snapshot sits below FARIC reactive content.
        val projectMFrame =
            projectMFrameOverride
                ?: config.projectMFrame

        if (
            config.projectMVisible &&
            projectMFrame != null
        ) {
            val startedNs =
                System.nanoTime()

            drawProjectMFrame(
                canvas = canvas,
                width = width,
                height = height,
                bitmap =
                    projectMFrame,
                rawGlFrame =
                    projectMFrameOverride !=
                        null,
                rawChannelsCorrect =
                    projectMRawChannelsCorrect,
            )

            projectMDrawNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 0 · FARIC reactive foreground.
        if (config.faricReactiveVisible) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderVisualizer(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            faricReactiveNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 1
        if (
            config
                .overVisualizationVisible
        ) {
            val startedNs =
                System.nanoTime()

            overVisualizationRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                )

            overVisualizationNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 2
        if (
            config
                .bigEqualizerVisible
        ) {
            val startedNs =
                System.nanoTime()

            bigEqualizerRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    signal = signal,
                )

            bigEqualizerNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 3
        if (
            config
                .graphicFiguresVisible
        ) {
            val startedNs =
                System.nanoTime()

            cyberSharkRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    config =
                        config
                            .cyberSharkConfig,
                )

            cyberSharkNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 4 is currently an empty GIF/animation slot.

        // Layer 5
        if (
            config
                .effectsVisible
        ) {
            val startedNs =
                System.nanoTime()

            sceneRenderer
                .renderEffects(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )

            effectsNs +=
                System.nanoTime() -
                    startedNs
        }

        // Layer 6
        pulseDeckView
            ?.let { view ->
                val updateStartedNs =
                    System.nanoTime()

                ensurePulseDeckSize(
                    view = view,
                    width = width,
                    height = height,
                )

                view.updateSignal(
                    signalForHud(
                        signal = signal,
                        timeMs = timeMs,
                    ),
                )
                view.setPlaying(
                    playing,
                )

                val safeDuration =
                    durationMs
                        .coerceAtLeast(
                            1L,
                        )

                val safePosition =
                    timeMs.coerceIn(
                        0L,
                        safeDuration,
                    )

                view.setPlaybackContent(
                    title =
                        title.ifBlank {
                            "FARIC"
                        },
                    artist = artist,
                    status =
                        if (playing) {
                            "Playing"
                        } else {
                            "Paused"
                        },
                    elapsed =
                        formatTime(
                            safePosition,
                        ),
                    total =
                        formatTime(
                            safeDuration,
                        ),
                    progressFraction =
                        (
                            safePosition
                                .toDouble() /
                                safeDuration
                                    .toDouble()
                            )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f,
                            ),
                )

                pulseDeckUpdateNs +=
                    System.nanoTime() -
                        updateStartedNs

                val drawStartedNs =
                    System.nanoTime()

                view.draw(
                    canvas,
                )

                pulseDeckDrawNs +=
                    System.nanoTime() -
                        drawStartedNs
            }
    }

    private fun drawProjectMFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        bitmap: Bitmap,
        rawGlFrame: Boolean,
        rawChannelsCorrect: Boolean,
    ) {
        if (
            bitmap.width <= 0 ||
            bitmap.height <= 0 ||
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val scale =
            max(
                width.toFloat() /
                    bitmap.width,
                height.toFloat() /
                    bitmap.height,
            )

        val renderedWidth =
            bitmap.width *
                scale
        val renderedHeight =
            bitmap.height *
                scale

        val left =
            (
                width -
                    renderedWidth
                ) *
                0.5f
        val top =
            (
                height -
                    renderedHeight
                ) *
                0.5f

        val destination =
            RectF(
                left,
                top,
                left +
                    renderedWidth,
                top +
                    renderedHeight,
            )

        if (rawGlFrame) {
            // glReadPixels returns framebuffer rows bottom-up. Flip only the
            // dynamic offline frame at draw time, avoiding another CPU copy.
            canvas.save()
            canvas.scale(
                1f,
                -1f,
                width *
                    0.5f,
                height *
                    0.5f,
            )
            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                if (rawChannelsCorrect) {
                    projectMPaint
                } else {
                    projectMRawGlPaint
                },
            )
            canvas.restore()
        } else {
            canvas.drawBitmap(
                bitmap,
                null,
                destination,
                projectMPaint,
            )
        }
    }

    private fun signalForHud(
        signal: SceneSignal,
        timeMs: Long,
    ): SceneSignal {
        if (signal.spectrum.isNotEmpty()) {
            return signal
        }

        val bins =
            FloatArray(
                48,
            ) { index ->
                val normalized =
                    index /
                        47f
                val band =
                    when {
                        normalized < 0.34f ->
                            signal.bass

                        normalized < 0.68f ->
                            signal.mid

                        else ->
                            signal.high
                    }

                val phase =
                    timeMs /
                        180.0 +
                        index *
                        0.73

                val oscillation =
                    kotlin.math.abs(
                        kotlin.math.sin(
                            phase,
                        ),
                    ).toFloat()

                (
                    band *
                        (
                            0.72f +
                                0.28f *
                                oscillation
                            ) +
                        signal.amplitude *
                        0.08f +
                        signal.beatStrength *
                        0.10f
                    )
                    .coerceIn(
                        0f,
                        1f,
                    )
            }

        return signal.copy(
            spectrum = bins,
        )
    }

    private fun ensurePulseDeckSize(
        view: PulseDeckMainSkinView,
        width: Int,
        height: Int,
    ) {
        if (
            measuredWidth ==
                width &&
            measuredHeight ==
                height
        ) {
            return
        }

        view.measure(
            View.MeasureSpec
                .makeMeasureSpec(
                    width,
                    View.MeasureSpec.EXACTLY,
                ),
            View.MeasureSpec
                .makeMeasureSpec(
                    height,
                    View.MeasureSpec.EXACTLY,
                ),
        )

        view.layout(
            0,
            0,
            width,
            height,
        )

        measuredWidth =
            width
        measuredHeight =
            height
    }

    private fun formatTime(
        millis: Long,
    ): String {
        val totalSeconds =
            (
                millis /
                    1000.0
                )
                .roundToLong()
                .coerceAtLeast(
                    0L,
                )

        val minutes =
            totalSeconds /
                60L
        val seconds =
            totalSeconds %
                60L

        return "%d:%02d".format(
            minutes,
            seconds,
        )
    }
}
