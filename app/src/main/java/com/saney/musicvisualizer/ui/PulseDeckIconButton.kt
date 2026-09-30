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
import android.view.View
import kotlin.math.min

/**
 * Custom icon-only control used by the PulseDeck Now Playing shell.
 *
 * Icons are drawn locally with Canvas paths instead of font/emoji glyphs so
 * transport and quick actions share one FARIC visual language on every phone.
 */
class PulseDeckIconButton(
    context: Context,
) : View(context) {

    enum class Icon {
        SHUFFLE,
        PREVIOUS,
        PLAY,
        PAUSE,
        NEXT,
        REPEAT,
        THEME,
        BOARD,
        VISUALIZER,
        EXPORT,
        FAVORITE,
        MORE,
        BACK,
    }

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val path = Path()
    private val rect = RectF()

    private var icon =
        Icon.PLAY

    private var primary = false

    fun setIcon(value: Icon) {
        if (icon == value) {
            return
        }

        icon = value
        invalidate()
    }

    fun setPrimary(value: Boolean) {
        if (primary == value) {
            return
        }

        primary = value
        invalidate()
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

        val side =
            min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = side * 0.43f

        val orange =
            Color.rgb(
                255,
                145,
                24,
            )
        val cyan =
            Color.rgb(
                34,
                216,
                244,
            )
        val accent =
            if (primary) {
                orange
            } else {
                cyan
            }

        setLayerType(
            LAYER_TYPE_SOFTWARE,
            null,
        )

        paint.style =
            Paint.Style.FILL
        paint.shader =
            RadialGradient(
                cx,
                cy,
                r,
                intArrayOf(
                    Color.argb(
                        if (primary) {
                            120
                        } else {
                            88
                        },
                        if (primary) {
                            105
                        } else {
                            0
                        },
                        if (primary) {
                            52
                        } else {
                            35
                        },
                        if (primary) {
                            4
                        } else {
                            49
                        },
                    ),
                    Color.argb(
                        235,
                        5,
                        11,
                        18,
                    ),
                    Color.argb(
                        250,
                        1,
                        5,
                        10,
                    ),
                ),
                floatArrayOf(
                    0f,
                    0.58f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        paint.clearShadowLayer()

        canvas.drawCircle(
            cx,
            cy,
            r * 0.88f,
            paint,
        )

        paint.shader = null
        paint.style =
            Paint.Style.STROKE
        paint.strokeWidth =
            side * 0.026f
        paint.color = accent
        paint.alpha =
            if (primary) {
                245
            } else {
                205
            }
        paint.setShadowLayer(
            side * 0.075f,
            0f,
            0f,
            accent,
        )

        canvas.drawCircle(
            cx,
            cy,
            r * 0.82f,
            paint,
        )

        paint.clearShadowLayer()
        paint.alpha = 150
        paint.strokeWidth =
            side * 0.009f

        rect.set(
            cx - r,
            cy - r,
            cx + r,
            cy + r,
        )

        canvas.drawArc(
            rect,
            198f,
            52f,
            false,
            paint,
        )
        canvas.drawArc(
            rect,
            18f,
            42f,
            false,
            paint,
        )

        paint.color =
            if (primary) {
                cyan
            } else {
                orange
            }
        paint.alpha = 110
        canvas.drawArc(
            rect,
            78f,
            36f,
            false,
            paint,
        )
        canvas.drawArc(
            rect,
            278f,
            44f,
            false,
            paint,
        )

        paint.color = Color.WHITE
        paint.alpha = 245
        paint.strokeWidth =
            side * 0.055f
        paint.style =
            Paint.Style.STROKE
        paint.strokeCap =
            Paint.Cap.ROUND
        paint.strokeJoin =
            Paint.Join.ROUND
        paint.setShadowLayer(
            side * 0.035f,
            0f,
            0f,
            accent,
        )

        drawIcon(
            canvas = canvas,
            icon = icon,
            cx = cx,
            cy = cy,
            side = side,
        )

        paint.clearShadowLayer()
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawIcon(
        canvas: Canvas,
        icon: Icon,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        when (icon) {
            Icon.SHUFFLE ->
                drawShuffle(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.PREVIOUS ->
                drawPrevious(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.PLAY ->
                drawPlay(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.PAUSE ->
                drawPause(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.NEXT ->
                drawNext(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.REPEAT ->
                drawRepeat(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.THEME ->
                drawTheme(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.BOARD ->
                drawBoard(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.VISUALIZER ->
                drawVisualizer(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.EXPORT ->
                drawExport(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.FAVORITE ->
                drawFavorite(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.MORE ->
                drawMore(
                    canvas,
                    cx,
                    cy,
                    side,
                )

            Icon.BACK ->
                drawBack(
                    canvas,
                    cx,
                    cy,
                    side,
                )
        }
    }

    private fun drawShuffle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        val left =
            cx - side * 0.18f
        val right =
            cx + side * 0.18f
        val top =
            cy - side * 0.14f
        val bottom =
            cy + side * 0.14f

        path.reset()
        path.moveTo(
            left,
            top,
        )
        path.cubicTo(
            cx - side * 0.02f,
            top,
            cx + side * 0.02f,
            bottom,
            right - side * 0.035f,
            bottom,
        )
        canvas.drawPath(
            path,
            paint,
        )

        path.reset()
        path.moveTo(
            left,
            bottom,
        )
        path.cubicTo(
            cx - side * 0.02f,
            bottom,
            cx + side * 0.02f,
            top,
            right - side * 0.035f,
            top,
        )
        canvas.drawPath(
            path,
            paint,
        )

        drawArrowHead(
            canvas,
            right,
            top,
            side,
            true,
        )
        drawArrowHead(
            canvas,
            right,
            bottom,
            side,
            true,
        )
    }

    private fun drawPrevious(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        paint.style =
            Paint.Style.FILL

        val bar =
            side * 0.055f
        rect.set(
            cx - side * 0.20f,
            cy - side * 0.18f,
            cx - side * 0.20f + bar,
            cy + side * 0.18f,
        )
        canvas.drawRoundRect(
            rect,
            bar,
            bar,
            paint,
        )

        path.reset()
        path.moveTo(
            cx + side * 0.17f,
            cy - side * 0.21f,
        )
        path.lineTo(
            cx - side * 0.10f,
            cy,
        )
        path.lineTo(
            cx + side * 0.17f,
            cy + side * 0.21f,
        )
        path.close()
        canvas.drawPath(
            path,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawPlay(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        paint.style =
            Paint.Style.FILL

        path.reset()
        path.moveTo(
            cx - side * 0.09f,
            cy - side * 0.22f,
        )
        path.lineTo(
            cx + side * 0.20f,
            cy,
        )
        path.lineTo(
            cx - side * 0.09f,
            cy + side * 0.22f,
        )
        path.close()

        canvas.drawPath(
            path,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawPause(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        paint.style =
            Paint.Style.FILL

        val width =
            side * 0.075f
        val gap =
            side * 0.055f

        rect.set(
            cx - gap - width,
            cy - side * 0.22f,
            cx - gap,
            cy + side * 0.22f,
        )
        canvas.drawRoundRect(
            rect,
            width * 0.35f,
            width * 0.35f,
            paint,
        )

        rect.set(
            cx + gap,
            cy - side * 0.22f,
            cx + gap + width,
            cy + side * 0.22f,
        )
        canvas.drawRoundRect(
            rect,
            width * 0.35f,
            width * 0.35f,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawNext(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        paint.style =
            Paint.Style.FILL

        val bar =
            side * 0.055f
        rect.set(
            cx + side * 0.20f - bar,
            cy - side * 0.18f,
            cx + side * 0.20f,
            cy + side * 0.18f,
        )
        canvas.drawRoundRect(
            rect,
            bar,
            bar,
            paint,
        )

        path.reset()
        path.moveTo(
            cx - side * 0.17f,
            cy - side * 0.21f,
        )
        path.lineTo(
            cx + side * 0.10f,
            cy,
        )
        path.lineTo(
            cx - side * 0.17f,
            cy + side * 0.21f,
        )
        path.close()
        canvas.drawPath(
            path,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawRepeat(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        rect.set(
            cx - side * 0.20f,
            cy - side * 0.15f,
            cx + side * 0.20f,
            cy + side * 0.15f,
        )

        canvas.drawArc(
            rect,
            205f,
            185f,
            false,
            paint,
        )
        canvas.drawArc(
            rect,
            25f,
            185f,
            false,
            paint,
        )

        drawArrowHead(
            canvas,
            cx + side * 0.19f,
            cy - side * 0.02f,
            side,
            true,
        )
        drawArrowHead(
            canvas,
            cx - side * 0.19f,
            cy + side * 0.02f,
            side,
            false,
        )
    }

    private fun drawTheme(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        val oldWidth =
            paint.strokeWidth
        paint.strokeWidth =
            side * 0.045f

        path.reset()
        path.moveTo(
            cx + side * 0.18f,
            cy + side * 0.08f,
        )
        path.cubicTo(
            cx + side * 0.12f,
            cy + side * 0.23f,
            cx - side * 0.20f,
            cy + side * 0.18f,
            cx - side * 0.22f,
            cy - side * 0.02f,
        )
        path.cubicTo(
            cx - side * 0.24f,
            cy - side * 0.20f,
            cx + side * 0.02f,
            cy - side * 0.27f,
            cx + side * 0.19f,
            cy - side * 0.15f,
        )
        path.cubicTo(
            cx + side * 0.30f,
            cy - side * 0.07f,
            cx + side * 0.28f,
            cy + side * 0.04f,
            cx + side * 0.18f,
            cy + side * 0.08f,
        )
        canvas.drawPath(
            path,
            paint,
        )

        paint.style =
            Paint.Style.FILL

        val dot =
            side * 0.025f

        listOf(
            -0.09f to -0.10f,
            0.03f to -0.14f,
            0.10f to -0.03f,
        ).forEach { (
                dx,
                dy,
            ) ->
            canvas.drawCircle(
                cx + side * dx,
                cy + side * dy,
                dot,
                paint,
            )
        }

        paint.style =
            Paint.Style.STROKE
        paint.strokeWidth = oldWidth
    }

    private fun drawBoard(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        val d =
            side * 0.15f

        listOf(
            0f to 0f,
            -d to 0f,
            d to 0f,
            0f to -d,
            0f to d,
        ).forEach { (
                dx,
                dy,
            ) ->
            if (
                dx != 0f ||
                dy != 0f
            ) {
                canvas.drawLine(
                    cx,
                    cy,
                    cx + dx,
                    cy + dy,
                    paint,
                )
            }
        }

        paint.style =
            Paint.Style.FILL

        val dot =
            side * 0.045f

        canvas.drawCircle(
            cx,
            cy,
            dot * 1.15f,
            paint,
        )
        canvas.drawCircle(
            cx - d,
            cy,
            dot,
            paint,
        )
        canvas.drawCircle(
            cx + d,
            cy,
            dot,
            paint,
        )
        canvas.drawCircle(
            cx,
            cy - d,
            dot,
            paint,
        )
        canvas.drawCircle(
            cx,
            cy + d,
            dot,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawVisualizer(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        val heights =
            floatArrayOf(
                0.16f,
                0.29f,
                0.42f,
                0.31f,
                0.20f,
            )

        val start =
            cx - side * 0.16f
        val gap =
            side * 0.08f

        heights.forEachIndexed {
                index,
                height,
            ->
            val x =
                start +
                    index *
                    gap

            canvas.drawLine(
                x,
                cy + side * 0.20f,
                x,
                cy + side * 0.20f -
                    side * height,
                paint,
            )
        }
    }

    private fun drawExport(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        rect.set(
            cx - side * 0.19f,
            cy - side * 0.02f,
            cx + side * 0.19f,
            cy + side * 0.22f,
        )

        path.reset()
        path.moveTo(
            rect.left,
            rect.top + side * 0.07f,
        )
        path.lineTo(
            rect.left,
            rect.bottom,
        )
        path.lineTo(
            rect.right,
            rect.bottom,
        )
        path.lineTo(
            rect.right,
            rect.top + side * 0.07f,
        )
        canvas.drawPath(
            path,
            paint,
        )

        canvas.drawLine(
            cx,
            cy + side * 0.07f,
            cx,
            cy - side * 0.22f,
            paint,
        )

        path.reset()
        path.moveTo(
            cx - side * 0.10f,
            cy - side * 0.12f,
        )
        path.lineTo(
            cx,
            cy - side * 0.22f,
        )
        path.lineTo(
            cx + side * 0.10f,
            cy - side * 0.12f,
        )
        canvas.drawPath(
            path,
            paint,
        )
    }

    private fun drawFavorite(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        path.reset()
        path.moveTo(
            cx,
            cy + side * 0.18f,
        )
        path.cubicTo(
            cx - side * 0.28f,
            cy - side * 0.02f,
            cx - side * 0.18f,
            cy - side * 0.24f,
            cx,
            cy - side * 0.10f,
        )
        path.cubicTo(
            cx + side * 0.18f,
            cy - side * 0.24f,
            cx + side * 0.28f,
            cy - side * 0.02f,
            cx,
            cy + side * 0.18f,
        )
        canvas.drawPath(
            path,
            paint,
        )
    }

    private fun drawMore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        paint.style =
            Paint.Style.FILL

        val r =
            side * 0.035f

        canvas.drawCircle(
            cx,
            cy - side * 0.12f,
            r,
            paint,
        )
        canvas.drawCircle(
            cx,
            cy,
            r,
            paint,
        )
        canvas.drawCircle(
            cx,
            cy + side * 0.12f,
            r,
            paint,
        )

        paint.style =
            Paint.Style.STROKE
    }

    private fun drawBack(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        side: Float,
    ) {
        val oldWidth =
            paint.strokeWidth

        paint.strokeWidth =
            side * 0.070f
        paint.strokeCap =
            Paint.Cap.ROUND
        paint.strokeJoin =
            Paint.Join.ROUND

        path.reset()
        path.moveTo(
            cx + side * 0.11f,
            cy - side * 0.20f,
        )
        path.lineTo(
            cx - side * 0.10f,
            cy,
        )
        path.lineTo(
            cx + side * 0.11f,
            cy + side * 0.20f,
        )

        canvas.drawPath(
            path,
            paint,
        )

        paint.strokeWidth =
            oldWidth
    }

    private fun drawArrowHead(
        canvas: Canvas,
        x: Float,
        y: Float,
        side: Float,
        pointsRight: Boolean,
    ) {
        val dir =
            if (pointsRight) {
                1f
            } else {
                -1f
            }

        path.reset()
        path.moveTo(
            x - dir * side * 0.09f,
            y - side * 0.07f,
        )
        path.lineTo(
            x,
            y,
        )
        path.lineTo(
            x - dir * side * 0.09f,
            y + side * 0.07f,
        )

        canvas.drawPath(
            path,
            paint,
        )
    }
}
