package com.saney.musicvisualizer.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * V235 (faric 05.11 23:50: «стрілочки різні — не треба стандартні символи клавіатури»): one arrow drawn with lines,
 * the same length and thickness for ↑ ↓ ← →. [dx], [dy] — where it points (one of them ±1, the other 0).
 */
class ArrowLineDrawable(
    private val dx: Int,
    private val dy: Int,
    color: Int,
    strokePx: Float,
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
        val half = minOf(b.width(), b.height()) * 0.28f
        val head = half * 0.55f
        val tipX = cx + dx * half
        val tipY = cy + dy * half
        canvas.drawLine(cx - dx * half, cy - dy * half, tipX, tipY, paint)
        // the two wings go back from the tip
        val px = -dy
        val py = dx
        canvas.drawLine(tipX, tipY, tipX - dx * head + px * head, tipY - dy * head + py * head, paint)
        canvas.drawLine(tipX, tipY, tipX - dx * head - px * head, tipY - dy * head - py * head, paint)
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
