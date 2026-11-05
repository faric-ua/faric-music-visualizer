package com.saney.musicvisualizer.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * V226 (faric 04.11 22:54: «у кружечок з усіх сторін — маленькі стрілочки в центр, щоб одразу було зрозуміло, що це
 * центровка»). Drawn with lines (not a font glyph): a small ring and four short arrows pointing at it.
 */
class CenterArrowsDrawable(
    color: Int,
    private val strokePx: Float,
) : Drawable() {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val size = minOf(b.width(), b.height()).toFloat()
        val ring = size * 0.10f
        val outer = size * 0.36f
        val inner = size * 0.19f
        val head = size * 0.07f
        canvas.drawCircle(cx, cy, ring, paint)
        // (dx, dy) — direction from the centre to the arrow's tail
        listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f).forEach { (dx, dy) ->
            val tailX = cx + dx * outer
            val tailY = cy + dy * outer
            val tipX = cx + dx * inner
            val tipY = cy + dy * inner
            canvas.drawLine(tailX, tailY, tipX, tipY, paint)
            // the arrow head points to the centre
            val px = -dy
            val py = dx
            canvas.drawLine(tipX, tipY, tipX + dx * head + px * head, tipY + dy * head + py * head, paint)
            canvas.drawLine(tipX, tipY, tipX + dx * head - px * head, tipY + dy * head - py * head, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
