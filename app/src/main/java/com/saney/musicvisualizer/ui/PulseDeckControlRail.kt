package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.LinearLayout

/**
 * Lightweight cyber-neon rail used behind PulseDeck control groups.
 * It deliberately stays translucent so the Board/Hero remains visible.
 */
class PulseDeckControlRail(
    context: Context,
) : LinearLayout(context) {

    enum class Style {
        TRANSPORT,
        ACTIONS,
    }

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val rect = RectF()

    private var style =
        Style.TRANSPORT

    init {
        setWillNotDraw(false)
        orientation = HORIZONTAL
    }

    fun setStyle(value: Style) {
        style = value
        invalidate()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        if (
            w <= 0f ||
            h <= 0f
        ) {
            super.onDraw(canvas)
            return
        }

        val cyan =
            Color.rgb(
                34,
                216,
                244,
            )
        val orange =
            Color.rgb(
                255,
                145,
                24,
            )

        rect.set(
            1f,
            1f,
            w - 1f,
            h - 1f,
        )

        paint.style =
            Paint.Style.FILL
        paint.shader = null
        paint.color =
            Color.argb(
                if (style == Style.TRANSPORT) {
                    118
                } else {
                    148
                },
                2,
                8,
                14,
            )

        canvas.drawRoundRect(
            rect,
            h * 0.34f,
            h * 0.34f,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
        paint.strokeWidth =
            (resources.displayMetrics.density * 1.15f)
                .coerceAtLeast(
                    1f,
                )
        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                0f,
                intArrayOf(
                    orange,
                    Color.argb(
                        125,
                        255,
                        145,
                        24,
                    ),
                    Color.argb(
                        80,
                        34,
                        216,
                        244,
                    ),
                    cyan,
                ),
                floatArrayOf(
                    0f,
                    0.28f,
                    0.72f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        paint.alpha =
            if (style == Style.TRANSPORT) {
                165
            } else {
                120
            }

        canvas.drawRoundRect(
            rect,
            h * 0.34f,
            h * 0.34f,
            paint,
        )

        paint.shader = null

        if (style == Style.TRANSPORT) {
            val cy =
                h * 0.54f

            paint.strokeWidth =
                resources.displayMetrics.density * 1.25f
            paint.alpha = 150
            paint.color = orange
            canvas.drawLine(
                w * 0.06f,
                cy,
                w * 0.45f,
                cy,
                paint,
            )

            paint.color = cyan
            canvas.drawLine(
                w * 0.55f,
                cy,
                w * 0.94f,
                cy,
                paint,
            )
        } else {
            paint.strokeWidth =
                resources.displayMetrics.density
            paint.alpha = 90
            paint.color = cyan

            canvas.drawLine(
                w * 0.08f,
                h * 0.14f,
                w * 0.42f,
                h * 0.14f,
                paint,
            )
            paint.color = orange
            canvas.drawLine(
                w * 0.58f,
                h * 0.86f,
                w * 0.92f,
                h * 0.86f,
                paint,
            )
        }

        paint.alpha = 255
        paint.shader = null

        super.onDraw(canvas)
    }
}
