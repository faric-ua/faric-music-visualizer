package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import kotlin.math.max
import kotlin.math.sin

/**
 * Compact reactive waveform used on the main PulseDeck screen.
 *
 * Many narrow bars are intentional: the approved reference reads as a fine
 * equalizer/waveform, not a row of large round dots.
 */
class PulseMiniView(
    context: Context,
) : View(context) {

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.SQUARE
        }

    private var energy = 0.12f
    private var bass = 0f
    private var high = 0f
    private var beat = 0f

    fun updateSignal(
        signal: SceneSignal,
    ) {
        energy =
            (
                signal.amplitude * 0.45f +
                    signal.mid * 0.25f +
                    signal.high * 0.30f
                )
                .coerceIn(
                    0f,
                    1f,
                )
        bass =
            signal.bass.coerceIn(
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
                signal.beatStrength,
            )

        postInvalidateOnAnimation()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        beat *= 0.88f

        val w = width.toFloat()
        val h = height.toFloat()

        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                0f,
                intArrayOf(
                    Color.rgb(
                        255,
                        145,
                        18,
                    ),
                    Color.rgb(
                        255,
                        183,
                        48,
                    ),
                    Color.rgb(
                        45,
                        198,
                        236,
                    ),
                    Color.rgb(
                        31,
                        218,
                        244,
                    ),
                ),
                floatArrayOf(
                    0f,
                    0.26f,
                    0.58f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )

        paint.strokeWidth =
            (
                resources.displayMetrics.density *
                    1.15f
                )
                .coerceAtLeast(
                    1.6f,
                )

        val bars = 56
        val usable =
            w * 0.94f
        val startX =
            (w - usable) * 0.5f
        val gap =
            usable /
                (
                    bars -
                        1
                    )

        val baseline =
            h * 0.88f

        val t =
            System.nanoTime() /
                1_000_000_000.0

        repeat(bars) { index ->
            val normalized =
                index.toFloat() /
                    (
                        bars -
                            1
                        )

            val waveA =
                (
                    (
                        sin(
                            t *
                                3.1 +
                                index *
                                0.53,
                        ) +
                            1.0
                        ) *
                        0.5
                    )
                    .toFloat()

            val waveB =
                (
                    (
                        sin(
                            t *
                                1.7 +
                                index *
                                0.19 +
                                1.4,
                        ) +
                            1.0
                        ) *
                        0.5
                    )
                    .toFloat()

            val centerLift =
                1f -
                    kotlin.math.abs(
                        normalized -
                            0.52f,
                    ) *
                    0.52f

            val tonalBias =
                if (normalized < 0.48f) {
                    bass
                } else {
                    high
                }

            val level =
                (
                    0.10f +
                        energy * 0.42f +
                        tonalBias * 0.22f +
                        beat * 0.24f +
                        waveA * 0.16f +
                        waveB * 0.08f
                    )
                    .coerceIn(
                        0.08f,
                        1f,
                    )

            val barHeight =
                h *
                    level *
                    (
                        0.28f +
                            centerLift *
                            0.56f
                        )

            val x =
                startX +
                    index *
                    gap

            canvas.drawLine(
                x,
                baseline,
                x,
                baseline -
                    barHeight,
                paint,
            )
        }

        paint.shader = null
        postInvalidateOnAnimation()
    }
}
