package com.saney.musicvisualizer.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.saney.musicvisualizer.analysis.SceneSignal
import kotlin.math.abs

/** Deterministic Layer 2 renderer matching BigEqualizerView geometry. */
class BigEqualizerExportRenderer {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        signal: SceneSignal,
    ) {
        val w =
            width.toFloat()
        val h =
            height.toFloat()

        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        val count =
            34
        val usable =
            w * 0.86f
        val gap =
            w * 0.008f
        val barWidth =
            (
                usable -
                    gap *
                    (
                        count -
                            1
                        )
                ) /
                count
        val startX =
            (
                w -
                    usable
                ) *
                0.5f
        val baseline =
            h * 0.39f
        val minHeight =
            h * 0.018f
        val maxHeight =
            h * 0.12f

        repeat(count) { index ->
            val normalized =
                index /
                    (
                        count -
                            1f
                        )

            val band =
                when {
                    normalized < 0.34f ->
                        signal.bass

                    normalized < 0.68f ->
                        signal.mid

                    else ->
                        signal.high
                }

            val centerWeight =
                1f -
                    abs(
                        normalized -
                            0.5f,
                    ) *
                    0.32f

            val value =
                (
                    band *
                        0.82f +
                        signal.amplitude *
                        0.18f
                    )
                    .coerceIn(
                        0f,
                        1f,
                    )

            val barHeight =
                minHeight +
                    maxHeight *
                    value *
                    centerWeight

            val left =
                startX +
                    index *
                    (
                        barWidth +
                            gap
                        )
            val top =
                baseline -
                    barHeight *
                    0.5f
            val bottom =
                baseline +
                    barHeight *
                    0.5f

            paint.shader =
                LinearGradient(
                    left,
                    top,
                    left +
                        barWidth,
                    bottom,
                    Color.rgb(
                        150,
                        72,
                        220,
                    ),
                    Color.rgb(
                        0,
                        214,
                        205,
                    ),
                    Shader.TileMode.CLAMP,
                )

            paint.alpha =
                (
                    125 +
                        value *
                        120f
                    )
                    .toInt()
                    .coerceIn(
                        0,
                        255,
                    )

            canvas.drawRoundRect(
                left,
                top,
                left +
                    barWidth,
                bottom,
                barWidth *
                    0.5f,
                barWidth *
                    0.5f,
                paint,
            )
        }

        paint.shader =
            null
        paint.alpha =
            255
    }
}
