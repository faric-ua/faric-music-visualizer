package com.saney.musicvisualizer.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.scene.SceneSpec
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
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
    val overVisualizationVisible: Boolean,
    val bigEqualizerVisible: Boolean,
    val graphicFiguresVisible: Boolean,
    val cyberSharkConfig: CyberSharkExportConfig,
    val effectsVisible: Boolean,
    val pulseDeckVisible: Boolean,
    val pulseDeckObjectVisibility:
        Map<String, Boolean>,
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
    ) {
        canvas.drawColor(
            Color.BLACK,
        )

        // Layer 0
        if (config.faricReactiveVisible) {
            sceneRenderer
                .renderVisualizer(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )
        }

        // projectM lives in the same Layer 0 slot but is not yet deterministic.
        // The export UI exposes this limitation instead of silently pretending
        // that the native GL preset was rendered.

        // Layer 1
        if (
            config
                .overVisualizationVisible
        ) {
            overVisualizationRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                )
        }

        // Layer 2
        if (
            config
                .bigEqualizerVisible
        ) {
            bigEqualizerRenderer
                .render(
                    canvas = canvas,
                    width = width,
                    height = height,
                    signal = signal,
                )
        }

        // Layer 3
        if (
            config
                .graphicFiguresVisible
        ) {
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
        }

        // Layer 4 is currently an empty GIF/animation slot.

        // Layer 5
        if (
            config
                .effectsVisible
        ) {
            sceneRenderer
                .renderEffects(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeMs = timeMs,
                    signal = signal,
                    scene = config.scene,
                )
        }

        // Layer 6
        pulseDeckView
            ?.let { view ->
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

                view.draw(
                    canvas,
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
