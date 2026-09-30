package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Permanent FARIC PulseDeck main-screen skin.
 *
 * This is deliberately independent from visualizer/theme content. Existing
 * visualizers, GF heroes and video layers can later be inserted above or
 * below this shell without forcing the player controls to change style.
 *
 * The visual language follows the approved cyan/orange HUD reference:
 * - dark navy/black field;
 * - orange energy on the left, cyan energy on the right;
 * - large reactive circular F reactor;
 * - fine HUD rings, radial bars, sparks and flowing stereo ribbons.
 */
class PulseDeckMainSkinView(
    context: Context,
) : View(context) {

    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface =
                android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD,
                )
        }

    private val path = Path()
    private val arc = RectF()

    private var amplitude = 0.10f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f
    private var playing = false

    fun updateSignal(
        signal: SceneSignal,
    ) {
        amplitude =
            signal.amplitude.coerceIn(
                0f,
                1f,
            )
        bass =
            signal.bass.coerceIn(
                0f,
                1f,
            )
        mid =
            signal.mid.coerceIn(
                0f,
                1f,
            )
        high =
            signal.high.coerceIn(
                0f,
                1f,
            )
        beat =
            max(
                beat,
                signal.beatStrength.coerceIn(
                    0f,
                    1f,
                ),
            )
        postInvalidateOnAnimation()
    }

    fun setPlaying(
        value: Boolean,
    ) {
        playing = value
        postInvalidateOnAnimation()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        beat *=
            if (playing) {
                0.90f
            } else {
                0.82f
            }

        val t =
            SystemClock.elapsedRealtime() /
                1000f

        drawBackground(
            canvas,
            w,
            h,
        )
        drawEnergyField(
            canvas,
            w,
            h,
            t,
        )
        drawReactor(
            canvas,
            w,
            h,
            t,
        )

        if (
            playing ||
            amplitude > 0.015f
        ) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        fill.shader =
            LinearGradient(
                0f,
                0f,
                0f,
                h,
                intArrayOf(
                    Color.rgb(
                        0,
                        4,
                        8,
                    ),
                    Color.rgb(
                        0,
                        10,
                        17,
                    ),
                    Color.rgb(
                        0,
                        4,
                        8,
                    ),
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        canvas.drawRect(
            0f,
            0f,
            w,
            h,
            fill,
        )
        fill.shader = null

        val cx = w * 0.5f
        val cy = h * 0.29f
        val radius = min(w, h) * 0.58f

        fill.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    Color.argb(
                        90,
                        0,
                        83,
                        116,
                    ),
                    Color.argb(
                        36,
                        0,
                        34,
                        51,
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(
                    0f,
                    0.58f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            radius,
            fill,
        )
        fill.shader = null
    }

    private fun drawEnergyField(
        canvas: Canvas,
        w: Float,
        h: Float,
        t: Float,
    ) {
        val centerY =
            h * 0.285f
        val orange =
            Color.rgb(
                255,
                139,
                18,
            )
        val cyan =
            Color.rgb(
                20,
                211,
                248,
            )

        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )

        repeat(4) { layer ->
            val isOrange =
                layer % 2 == 0

            stroke.color =
                if (isOrange) {
                    orange
                } else {
                    cyan
                }

            stroke.alpha =
                118 -
                    layer * 13
            stroke.strokeWidth =
                resources.displayMetrics.density *
                    (
                        1.0f +
                            layer * 0.34f
                        )
            stroke.maskFilter =
                BlurMaskFilter(
                    resources.displayMetrics.density *
                        (
                            2.2f +
                                layer
                            ),
                    BlurMaskFilter.Blur.NORMAL,
                )

            path.reset()

            val phase =
                t *
                    (
                        0.7f +
                            layer * 0.12f
                        ) +
                    layer *
                    1.17f

            val baseAmp =
                h *
                    (
                        0.026f +
                            layer * 0.008f
                        ) *
                    (
                        0.76f +
                            amplitude * 0.44f
                        )

            val steps = 84

            repeat(
                steps + 1,
            ) { index ->
                val x =
                    w *
                        index /
                        steps.toFloat()

                val normalized =
                    x /
                        w

                val leftWeight =
                    (
                        1f -
                            normalized
                        )
                        .coerceIn(
                            0f,
                            1f,
                        )

                val rightWeight =
                    normalized.coerceIn(
                        0f,
                        1f,
                    )

                val stereoWeight =
                    if (isOrange) {
                        0.68f +
                            leftWeight *
                            0.32f
                    } else {
                        0.68f +
                            rightWeight *
                            0.32f
                    }

                val y =
                    centerY +
                        sin(
                            (
                                normalized *
                                    (
                                        3.1f +
                                            layer * 0.23f
                                        ) *
                                    PI *
                                    2f +
                                    phase
                                )
                                .toDouble(),
                        )
                            .toFloat() *
                        baseAmp *
                        stereoWeight +
                        sin(
                            (
                                normalized *
                                    PI *
                                    6f -
                                    phase *
                                    0.72f
                                )
                                .toDouble(),
                        )
                            .toFloat() *
                        baseAmp *
                        0.22f

                if (index == 0) {
                    path.moveTo(
                        x,
                        y,
                    )
                } else {
                    path.lineTo(
                        x,
                        y,
                    )
                }
            }

            canvas.drawPath(
                path,
                stroke,
            )
        }

        stroke.maskFilter = null

        repeat(92) { index ->
            val f =
                (
                    index *
                        0.61803398875f
                    ) %
                    1f
            val x =
                w *
                    f

            val wave =
                sin(
                    (
                        f *
                            PI *
                            5.2 +
                            index *
                            0.47 +
                            t *
                            0.42
                        )
                        .toDouble(),
                )
                    .toFloat()

            val y =
                centerY +
                    wave *
                    h *
                    0.10f +
                    (
                        (index % 7) -
                            3
                        ) *
                    h *
                    0.008f

            val orangeParticle =
                index % 3 == 0

            fill.color =
                if (orangeParticle) {
                    Color.argb(
                        (
                            70 +
                                bass *
                                80f +
                                beat *
                                65f
                            )
                            .toInt()
                            .coerceIn(
                                50,
                                205,
                            ),
                        255,
                        139,
                        18,
                    )
                } else {
                    Color.argb(
                        (
                            58 +
                                high *
                                95f +
                                beat *
                                45f
                            )
                            .toInt()
                            .coerceIn(
                                45,
                                205,
                            ),
                        20,
                        211,
                        248,
                    )
                }

            val radius =
                resources.displayMetrics.density *
                    (
                        0.7f +
                            (index % 4) *
                            0.36f +
                            beat *
                            0.9f
                        )

            canvas.drawCircle(
                x,
                y,
                radius,
                fill,
            )
        }
    }

    private fun drawReactor(
        canvas: Canvas,
        w: Float,
        h: Float,
        t: Float,
    ) {
        val cx =
            w *
                0.5f
        val cy =
            h *
                0.29f
        val base =
            min(
                w,
                h,
            )
        val beatPulse =
            1f +
                beat *
                0.035f
        val outerRadius =
            base *
                0.315f *
                beatPulse

        val orange =
            Color.rgb(
                255,
                139,
                18,
            )
        val cyan =
            Color.rgb(
                20,
                211,
                248,
            )

        fill.shader =
            RadialGradient(
                cx,
                cy,
                outerRadius,
                intArrayOf(
                    Color.argb(
                        20,
                        20,
                        211,
                        248,
                    ),
                    Color.argb(
                        16,
                        255,
                        139,
                        18,
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(
                    0.36f,
                    0.72f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            outerRadius * 1.10f,
            fill,
        )
        fill.shader = null

        repeat(7) { index ->
            val r =
                outerRadius *
                    (
                        0.58f +
                            index *
                            0.065f
                        )

            arc.set(
                cx - r,
                cy - r,
                cx + r,
                cy + r,
            )

            stroke.strokeWidth =
                resources.displayMetrics.density *
                    (
                        0.8f +
                            index *
                            0.14f
                        )

            stroke.color =
                if (index % 2 == 0) {
                    cyan
                } else {
                    orange
                }

            stroke.alpha =
                72 +
                    index *
                    14

            canvas.drawArc(
                arc,
                (
                    t *
                        (
                            7f +
                                index * 1.7f
                            ) +
                        index * 37f
                    ) %
                    360f,
                66f +
                    index * 7f,
                false,
                stroke,
            )

            canvas.drawArc(
                arc,
                (
                    182f -
                        t *
                        (
                            5f +
                                index * 1.25f
                            ) +
                        index * 29f
                    ) %
                    360f,
                42f +
                    index * 5f,
                false,
                stroke,
            )
        }

        val bars = 72
        val inner =
            outerRadius *
                0.54f
        val maxBar =
            outerRadius *
                0.25f

        repeat(bars) { index ->
            val ratio =
                index /
                    bars.toFloat()

            val angle =
                ratio *
                    PI *
                    2f -
                    PI /
                    2f

            val orangeSide =
                cos(
                    angle,
                ) <
                    0f

            val band =
                if (orangeSide) {
                    bass
                } else {
                    high
                }

            val animation =
                (
                    sin(
                        (
                            t *
                                3.3f +
                                index *
                                0.41f
                            )
                            .toDouble(),
                    ) +
                        1.0
                    )
                    .toFloat() *
                    0.5f

            val length =
                maxBar *
                    (
                        0.32f +
                            amplitude * 0.32f +
                            band * 0.24f +
                            animation * 0.16f +
                            beat * 0.20f
                        )
                        .coerceIn(
                            0.24f,
                            1f,
                        )

            val x1 =
                cx +
                    cos(
                        angle,
                    )
                        .toFloat() *
                    inner
            val y1 =
                cy +
                    sin(
                        angle,
                    )
                        .toFloat() *
                    inner
            val x2 =
                cx +
                    cos(
                        angle,
                    )
                        .toFloat() *
                    (
                        inner +
                            length
                        )
            val y2 =
                cy +
                    sin(
                        angle,
                    )
                        .toFloat() *
                    (
                        inner +
                            length
                        )

            stroke.color =
                if (orangeSide) {
                    orange
                } else {
                    cyan
                }

            stroke.alpha =
                170
            stroke.strokeWidth =
                resources.displayMetrics.density *
                    2.0f

            canvas.drawLine(
                x1,
                y1,
                x2,
                y2,
                stroke,
            )
        }

        val coreRadius =
            outerRadius *
                0.37f

        fill.shader =
            RadialGradient(
                cx,
                cy,
                coreRadius,
                intArrayOf(
                    Color.rgb(
                        8,
                        17,
                        25,
                    ),
                    Color.rgb(
                        2,
                        7,
                        12,
                    ),
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            coreRadius,
            fill,
        )
        fill.shader = null

        stroke.strokeWidth =
            resources.displayMetrics.density *
                2.2f
        stroke.alpha = 235

        arc.set(
            cx - coreRadius,
            cy - coreRadius,
            cx + coreRadius,
            cy + coreRadius,
        )

        stroke.color = orange
        canvas.drawArc(
            arc,
            104f,
            152f,
            false,
            stroke,
        )

        stroke.color = cyan
        canvas.drawArc(
            arc,
            -76f,
            152f,
            false,
            stroke,
        )

        text.textSize =
            base *
                0.105f
        text.color =
            Color.rgb(
                224,
                247,
                255,
            )
        text.setShadowLayer(
            resources.displayMetrics.density *
                9f,
            0f,
            0f,
            cyan,
        )

        val fm =
            text.fontMetrics
        val baseline =
            cy -
                (
                    fm.ascent +
                        fm.descent
                    ) /
                2f

        canvas.drawText(
            "F",
            cx,
            baseline,
            text,
        )

        text.clearShadowLayer()

        repeat(8) { index ->
            val angle =
                PI *
                    2.0 *
                    index /
                    8.0

            val r =
                outerRadius *
                    0.98f

            val x =
                cx +
                    cos(
                        angle,
                    )
                        .toFloat() *
                    r
            val y =
                cy +
                    sin(
                        angle,
                    )
                        .toFloat() *
                    r

            fill.color =
                if (index % 2 == 0) {
                    Color.argb(
                        210,
                        20,
                        211,
                        248,
                    )
                } else {
                    Color.argb(
                        210,
                        255,
                        139,
                        18,
                    )
                }

            canvas.drawCircle(
                x,
                y,
                resources.displayMetrics.density *
                    (
                        1.25f +
                            beat *
                            1.6f
                        ),
                fill,
            )
        }
    }
}
