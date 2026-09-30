package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.widget.LinearLayout
import kotlin.math.min

/**
 * Cyan/orange HUD panel behind PulseDeck button groups.
 *
 * The geometry mirrors the approved reference blocks: transport has a large
 * center housing with smaller side housings; actions has four equal sockets.
 * Buttons are still real Android Views on top, so touch/accessibility remains
 * native while the rail provides the skin.
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
    private val path = Path()

    private var style =
        Style.TRANSPORT

    init {
        setWillNotDraw(false)
        orientation = HORIZONTAL
        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )
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
                20,
                211,
                248,
            )
        val orange =
            Color.rgb(
                255,
                139,
                18,
            )

        drawOuterPanel(
            canvas,
            w,
            h,
            cyan,
            orange,
        )

        when (style) {
            Style.TRANSPORT ->
                drawTransportSockets(
                    canvas,
                    w,
                    h,
                    cyan,
                    orange,
                )

            Style.ACTIONS ->
                drawActionSockets(
                    canvas,
                    w,
                    h,
                    cyan,
                    orange,
                )
        }

        super.onDraw(canvas)
    }

    private fun drawOuterPanel(
        canvas: Canvas,
        w: Float,
        h: Float,
        cyan: Int,
        orange: Int,
    ) {
        val radius =
            h *
                if (style == Style.TRANSPORT) {
                    0.23f
                } else {
                    0.29f
                }

        rect.set(
            1f,
            h * 0.07f,
            w - 1f,
            h * 0.93f,
        )

        paint.style =
            Paint.Style.FILL
        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                h,
                intArrayOf(
                    Color.argb(
                        190,
                        2,
                        9,
                        14,
                    ),
                    Color.argb(
                        222,
                        0,
                        5,
                        10,
                    ),
                    Color.argb(
                        194,
                        2,
                        12,
                        18,
                    ),
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            paint,
        )

        paint.shader = null
        paint.style =
            Paint.Style.STROKE
        paint.strokeWidth =
            resources.displayMetrics.density *
                1.15f
        paint.shader =
            LinearGradient(
                0f,
                0f,
                w,
                0f,
                intArrayOf(
                    orange,
                    Color.argb(
                        190,
                        255,
                        139,
                        18,
                    ),
                    Color.argb(
                        160,
                        20,
                        211,
                        248,
                    ),
                    cyan,
                ),
                floatArrayOf(
                    0f,
                    0.28f,
                    0.70f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        paint.setShadowLayer(
            resources.displayMetrics.density *
                4.5f,
            0f,
            0f,
            Color.argb(
                95,
                20,
                211,
                248,
            ),
        )

        canvas.drawRoundRect(
            rect,
            radius,
            radius,
            paint,
        )

        paint.clearShadowLayer()
        paint.shader = null

        // Small asymmetric traces copied from the reference rail language.
        paint.strokeWidth =
            resources.displayMetrics.density *
                0.85f
        paint.alpha = 120

        paint.color = orange
        canvas.drawLine(
            w * 0.07f,
            h * 0.16f,
            w * 0.31f,
            h * 0.16f,
            paint,
        )
        canvas.drawLine(
            w * 0.62f,
            h * 0.84f,
            w * 0.91f,
            h * 0.84f,
            paint,
        )

        paint.color = cyan
        canvas.drawLine(
            w * 0.68f,
            h * 0.16f,
            w * 0.93f,
            h * 0.16f,
            paint,
        )
        canvas.drawLine(
            w * 0.08f,
            h * 0.84f,
            w * 0.39f,
            h * 0.84f,
            paint,
        )

        paint.alpha = 255
    }

    private fun drawTransportSockets(
        canvas: Canvas,
        w: Float,
        h: Float,
        cyan: Int,
        orange: Int,
    ) {
        val cy =
            h *
                0.50f

        val centers =
            floatArrayOf(
                0.10f,
                0.28f,
                0.50f,
                0.72f,
                0.90f,
            )

        centers.forEachIndexed {
                index,
                fraction,
            ->
            val primary =
                index == 2

            val radius =
                h *
                    if (primary) {
                        0.40f
                    } else if (
                        index == 1 ||
                        index == 3
                    ) {
                        0.27f
                    } else {
                        0.23f
                    }

            drawSocket(
                canvas,
                w * fraction,
                cy,
                radius,
                if (primary) {
                    orange
                } else {
                    cyan
                },
                if (primary) {
                    cyan
                } else {
                    orange
                },
            )
        }

        paint.style =
            Paint.Style.STROKE
        paint.strokeWidth =
            resources.displayMetrics.density *
                1.1f
        paint.alpha = 185

        val leftEnd =
            w *
                0.50f -
                h *
                0.40f
        val rightStart =
            w *
                0.50f +
                h *
                0.40f

        paint.color = orange
        canvas.drawLine(
            w * 0.11f,
            cy,
            leftEnd,
            cy,
            paint,
        )

        paint.color = cyan
        canvas.drawLine(
            rightStart,
            cy,
            w * 0.89f,
            cy,
            paint,
        )

        paint.alpha = 255
    }

    private fun drawActionSockets(
        canvas: Canvas,
        w: Float,
        h: Float,
        cyan: Int,
        orange: Int,
    ) {
        val cy =
            h *
                0.50f
        val radius =
            h *
                0.29f

        val centers =
            floatArrayOf(
                0.13f,
                0.38f,
                0.63f,
                0.87f,
            )

        centers.forEachIndexed {
                index,
                fraction,
            ->
            drawSocket(
                canvas,
                w * fraction,
                cy,
                radius,
                if (index == 0) {
                    orange
                } else {
                    cyan
                },
                if (index == 0) {
                    cyan
                } else {
                    orange
                },
            )
        }
    }

    private fun drawSocket(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        main: Int,
        secondary: Int,
    ) {
        paint.style =
            Paint.Style.FILL
        paint.shader =
            RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    Color.argb(
                        100,
                        Color.red(main),
                        Color.green(main),
                        Color.blue(main),
                    ),
                    Color.argb(
                        190,
                        2,
                        8,
                        13,
                    ),
                    Color.argb(
                        220,
                        0,
                        4,
                        8,
                    ),
                ),
                floatArrayOf(
                    0f,
                    0.56f,
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

        paint.shader = null
        paint.style =
            Paint.Style.STROKE

        repeat(3) { ring ->
            val r =
                radius *
                    (
                        0.68f +
                            ring *
                            0.13f
                        )

            rect.set(
                cx - r,
                cy - r,
                cx + r,
                cy + r,
            )

            paint.strokeWidth =
                resources.displayMetrics.density *
                    if (ring == 0) {
                        1.3f
                    } else {
                        0.75f
                    }

            paint.color =
                if (ring == 1) {
                    secondary
                } else {
                    main
                }
            paint.alpha =
                if (ring == 0) {
                    205
                } else {
                    115
                }

            canvas.drawArc(
                rect,
                205f +
                    ring * 24f,
                92f +
                    ring * 16f,
                false,
                paint,
            )
            canvas.drawArc(
                rect,
                20f +
                    ring * 16f,
                76f +
                    ring * 12f,
                false,
                paint,
            )
        }

        paint.alpha = 255
    }
}
