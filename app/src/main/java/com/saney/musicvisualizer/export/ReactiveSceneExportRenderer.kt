package com.saney.musicvisualizer.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.scene.AccentPalette
import com.saney.musicvisualizer.scene.SceneSpec
import com.saney.musicvisualizer.scene.VisualizerType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic counterpart of the two ReactiveSceneView roles used by
 * PulseDeck:
 * - Layer 0 FARIC reactive foreground;
 * - Layer 5 transparent atmospheric effects.
 *
 * It intentionally consumes the offline signal + explicit frame time instead
 * of SystemClock so PNG/MP4 output is repeatable.
 */
class ReactiveSceneExportRenderer(
    context: Context,
) {
    private val density =
        context.resources
            .displayMetrics
            .density

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val path =
        Path()

    fun renderVisualizer(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        scene: SceneSpec,
    ) {
        when (scene.visualizerType) {
            VisualizerType.RADIAL ->
                drawRadial(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    timeMs,
                    signal,
                    scene,
                )

            VisualizerType.WAVE_RIBBON ->
                drawWaveRibbon(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    timeMs,
                    signal,
                    scene,
                )

            VisualizerType.SPECTRUM_BARS ->
                drawSpectrumBars(
                    canvas,
                    width.toFloat(),
                    height.toFloat(),
                    timeMs,
                    signal,
                    scene,
                )
        }
    }

    fun renderEffects(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        scene: SceneSpec,
    ) {
        val w =
            width.toFloat()
        val h =
            height.toFloat()
        val colors =
            palette(scene.palette)
        val phase =
            timeMs /
                1000.0
        val minSide =
            minOf(w, h)
        val backgroundBeat =
            signal.beatStrength
                .coerceIn(0f, 1f)

        repeat(5) { index ->
            val progress =
                index / 4f
            val x =
                w *
                    (
                        0.12f +
                            progress *
                            0.76f
                        ) +
                    sin(
                        phase *
                            (
                                0.32 +
                                    index *
                                    0.035
                                ) +
                            index,
                    )
                        .toFloat() *
                    w *
                    0.045f
            val y =
                h *
                    (
                        0.18f +
                            (
                                index %
                                    3
                                ) *
                            0.19f
                        ) +
                    cos(
                        phase *
                            (
                                0.28 +
                                    index *
                                    0.03
                                ) +
                            index,
                    )
                        .toFloat() *
                    h *
                    0.025f

            drawGlow(
                canvas = canvas,
                x = x,
                y = y,
                radius =
                    minSide *
                        (
                            0.16f +
                                backgroundBeat *
                                0.045f
                            ),
                color =
                    if (index % 2 == 0) {
                        colors.first
                    } else {
                        colors.second
                    },
                alpha =
                    (
                        0.035f +
                            signal.amplitude *
                            0.055f +
                            backgroundBeat *
                            0.08f
                        ),
            )
        }
    }

    private fun drawRadial(
        canvas: Canvas,
        w: Float,
        h: Float,
        timeMs: Long,
        signal: SceneSignal,
        scene: SceneSpec,
    ) {
        val cx =
            w * 0.5f
        val cy =
            h * 0.43f
        val minSide =
            minOf(w, h)
        val colors =
            palette(scene.palette)
        val beat =
            signal.beatStrength
                .coerceIn(0f, 1f)
        val intensity =
            scene.intensity
        val innerRadius =
            minSide *
                (
                    0.115f +
                        beat *
                        0.022f *
                        intensity
                    )
        val rayCount =
            84

        repeat(rayCount) { index ->
            val angle =
                (
                    2.0 *
                        PI *
                        index /
                        rayCount
                    ) -
                    PI /
                    2.0

            val band =
                when (index % 3) {
                    0 -> signal.bass
                    1 -> signal.mid
                    else -> signal.high
                }

            val shimmer =
                (
                    (
                        sin(
                            timeMs /
                                520.0 +
                                index *
                                0.57,
                        ) +
                            1.0
                        ) *
                        0.5
                    )
                    .toFloat()

            val length =
                minSide *
                    (
                        0.018f +
                            band *
                            0.20f *
                            intensity +
                            signal.amplitude *
                            0.055f +
                            beat *
                            0.13f *
                            intensity +
                            shimmer *
                            0.012f
                        )

            val outer =
                innerRadius +
                    length

            stroke.strokeWidth =
                (
                    1.1f +
                        band *
                        6.2f +
                        beat *
                        3f
                    ) *
                    density
            stroke.color =
                blend(
                    colors.first,
                    colors.second,
                    index /
                        rayCount.toFloat(),
                )
            stroke.alpha =
                (
                    82 +
                        band *
                        165f +
                        beat *
                        55f
                    )
                    .toInt()
                    .coerceIn(0, 255)

            canvas.drawLine(
                cx +
                    cos(angle)
                        .toFloat() *
                    innerRadius,
                cy +
                    sin(angle)
                        .toFloat() *
                    innerRadius,
                cx +
                    cos(angle)
                        .toFloat() *
                    outer,
                cy +
                    sin(angle)
                        .toFloat() *
                    outer,
                stroke,
            )
        }

        drawPulseCore(
            canvas,
            cx,
            cy,
            minSide,
            colors,
            scene.intensity,
            signal,
        )
    }

    private fun drawWaveRibbon(
        canvas: Canvas,
        w: Float,
        h: Float,
        timeMs: Long,
        signal: SceneSignal,
        scene: SceneSpec,
    ) {
        val colors =
            palette(scene.palette)
        val centerY =
            h * 0.43f
        val phase =
            timeMs /
                1000.0
        val beat =
            signal.beatStrength
                .coerceIn(0f, 1f)
        val amp =
            h *
                (
                    0.018f +
                        signal.amplitude *
                        0.14f +
                        signal.bass *
                        0.05f +
                        beat *
                        0.09f
                    ) *
                scene.intensity

        repeat(6) { ribbon ->
            path.reset()

            val offset =
                (
                    ribbon -
                        2.5f
                    ) *
                    h *
                    0.016f

            var x =
                0f

            while (x <= w) {
                val progress =
                    x / w
                val localBand =
                    when (ribbon % 3) {
                        0 -> signal.bass
                        1 -> signal.mid
                        else -> signal.high
                    }

                val y =
                    centerY +
                        offset +
                        sin(
                            progress *
                                PI *
                                (
                                    3.4 +
                                        ribbon *
                                        0.26
                                    ) +
                                phase *
                                (
                                    2.0 +
                                        localBand *
                                        2.4
                                    ),
                        )
                            .toFloat() *
                        amp *
                        (
                            0.52f +
                                ribbon *
                                0.10f +
                                localBand *
                                0.24f
                            )

                if (x == 0f) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                x +=
                    maxOf(
                        4f,
                        w / 140f,
                    )
            }

            stroke.color =
                blend(
                    colors.first,
                    colors.second,
                    ribbon / 5f,
                )
            stroke.strokeWidth =
                (
                    1.6f +
                        ribbon *
                        0.58f +
                        signal.mid *
                        4f +
                        beat *
                        2.2f
                    ) *
                    density
            stroke.alpha =
                (
                    80 +
                        ribbon *
                        24 +
                        signal.high *
                        70f
                    )
                    .toInt()
                    .coerceIn(0, 255)

            canvas.drawPath(
                path,
                stroke,
            )
        }

        drawPulseCore(
            canvas,
            w * 0.5f,
            centerY,
            minOf(w, h),
            colors,
            scene.intensity,
            signal,
        )
    }

    private fun drawSpectrumBars(
        canvas: Canvas,
        w: Float,
        h: Float,
        timeMs: Long,
        signal: SceneSignal,
        scene: SceneSpec,
    ) {
        val colors =
            palette(scene.palette)
        val beat =
            signal.beatStrength
                .coerceIn(0f, 1f)
        val barCount =
            36
        val gap =
            w * 0.006f
        val usable =
            w * 0.90f
        val barWidth =
            (
                usable -
                    gap *
                    (
                        barCount -
                            1
                        )
                ) /
                barCount
        val startX =
            (
                w -
                    usable
                ) /
                2f
        val baseline =
            h * 0.50f

        repeat(barCount) { index ->
            val band =
                when {
                    index <
                        barCount /
                        3 ->
                        signal.bass

                    index <
                        barCount *
                        2 /
                        3 ->
                        signal.mid

                    else ->
                        signal.high
                }

            val oscillation =
                (
                    (
                        sin(
                            timeMs /
                                280.0 +
                                index *
                                0.81,
                        ) +
                            1.0
                        ) *
                        0.5
                    )
                    .toFloat()

            val barHeight =
                h *
                    (
                        0.018f +
                            band *
                            0.27f *
                            scene.intensity +
                            signal.amplitude *
                            0.055f +
                            beat *
                            0.12f +
                            oscillation *
                            0.018f
                        )

            val left =
                startX +
                    index *
                    (
                        barWidth +
                            gap
                        )

            paint.color =
                blend(
                    colors.first,
                    colors.second,
                    index /
                        (
                            barCount -
                                1f
                            ),
                )
            paint.alpha =
                (
                    105 +
                        band *
                        145f +
                        beat *
                        50f
                    )
                    .toInt()
                    .coerceIn(0, 255)

            canvas.drawRoundRect(
                left,
                baseline -
                    barHeight /
                    2f,
                left +
                    barWidth,
                baseline +
                    barHeight /
                    2f,
                barWidth /
                    2f,
                barWidth /
                    2f,
                paint,
            )
        }

        drawPulseCore(
            canvas,
            w * 0.5f,
            h * 0.36f,
            minOf(w, h),
            colors,
            scene.intensity,
            signal,
        )

        paint.alpha =
            255
    }

    private fun drawPulseCore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        colors: Pair<Int, Int>,
        intensity: Float,
        signal: SceneSignal,
    ) {
        val beat =
            signal.beatStrength
                .coerceIn(0f, 1f)
        val radius =
            minSide *
                (
                    0.088f +
                        signal.bass *
                        0.034f +
                        signal.mid *
                        0.010f +
                        beat *
                        0.027f *
                        intensity
                    )

        drawGlow(
            canvas,
            cx,
            cy,
            radius * 2.6f,
            colors.first,
            (
                0.18f +
                    beat *
                    0.30f +
                    signal.bass *
                    0.08f
                ),
        )
        drawGlow(
            canvas,
            cx,
            cy,
            radius * 1.9f,
            colors.second,
            0.12f +
                signal.high *
                0.08f,
        )

        paint.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    Color.rgb(
                        20,
                        28,
                        34,
                    ),
                    Color.rgb(
                        5,
                        8,
                        12,
                    ),
                    Color.BLACK,
                ),
                floatArrayOf(
                    0f,
                    0.62f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint,
        )
        paint.shader =
            null

        stroke.strokeWidth =
            (
                2.2f +
                    beat *
                    2.4f
                ) *
                density
        stroke.color =
            blend(
                colors.first,
                colors.second,
                0.5f,
            )
        stroke.alpha =
            (
                165 +
                    beat *
                    90f +
                    signal.high *
                    35f
                )
                .toInt()
                .coerceIn(0, 255)
        canvas.drawCircle(
            cx,
            cy,
            radius *
                0.93f,
            stroke,
        )

        paint.textAlign =
            Paint.Align.CENTER
        paint.textSize =
            radius *
                0.25f
        paint.isFakeBoldText =
            true
        paint.color =
            Color.WHITE
        paint.alpha =
            220
        canvas.drawText(
            "PULSE",
            cx,
            cy +
                paint.textSize *
                0.35f,
            paint,
        )
        paint.isFakeBoldText =
            false
        paint.alpha =
            255
        stroke.alpha =
            255
    }

    private fun drawGlow(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Float,
    ) {
        if (
            radius <= 0f ||
            alpha <= 0f
        ) {
            return
        }

        paint.shader =
            RadialGradient(
                x,
                y,
                radius,
                intArrayOf(
                    withAlpha(
                        color,
                        alpha,
                    ),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        paint.alpha =
            255
        canvas.drawCircle(
            x,
            y,
            radius,
            paint,
        )
        paint.shader =
            null
    }

    private fun palette(
        palette: AccentPalette,
    ): Pair<Int, Int> =
        when (palette) {
            AccentPalette.SUNSET_CYAN ->
                Color.rgb(
                    255,
                    145,
                    24,
                ) to
                    Color.rgb(
                        35,
                        211,
                        238,
                    )

            AccentPalette.VIOLET_TEAL ->
                Color.rgb(
                    170,
                    86,
                    255,
                ) to
                    Color.rgb(
                        21,
                        229,
                        197,
                    )

            AccentPalette.LIME_MAGENTA ->
                Color.rgb(
                    188,
                    255,
                    46,
                ) to
                    Color.rgb(
                        255,
                        59,
                        178,
                    )
        }

    private fun blend(
        a: Int,
        b: Int,
        t: Float,
    ): Int {
        val p =
            t.coerceIn(
                0f,
                1f,
            )

        return Color.rgb(
            (
                Color.red(a) +
                    (
                        Color.red(b) -
                            Color.red(a)
                        ) *
                    p
                )
                .toInt(),
            (
                Color.green(a) +
                    (
                        Color.green(b) -
                            Color.green(a)
                        ) *
                    p
                )
                .toInt(),
            (
                Color.blue(a) +
                    (
                        Color.blue(b) -
                            Color.blue(a)
                        ) *
                    p
                )
                .toInt(),
        )
    }

    private fun withAlpha(
        color: Int,
        alpha: Float,
    ): Int =
        Color.argb(
            (
                255 *
                    alpha.coerceIn(
                        0f,
                        1f,
                    )
                )
                .toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
}
