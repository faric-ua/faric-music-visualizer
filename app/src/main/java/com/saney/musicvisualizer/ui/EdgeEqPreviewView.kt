package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * V141: the live preview in «Окантовка-еквалайзер» — a tile with pretend text and the same
 * [EdgeEqRenderer] the main screen and the video use, driven by a pretend song (beat + a melody that
 * goes up and down), so every slider shows its effect at once (faric: «хочу бачити й вибирати»).
 */
class EdgeEqPreviewView(context: Context) : View(context) {
    val renderer = EdgeEqRenderer()
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tile = RectF()
    private val spectrum = FloatArray(40)
    private val notes = floatArrayOf(0.25f, 0.45f, 0.35f, 0.6f, 0.5f, 0.3f, 0.55f, 0.4f)
    private var seed = 7

    private fun noise(): Float {
        seed = seed * 1_103_515_245 + 12_345
        return ((seed ushr 16) and 0x7fff) / 32_767f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(16, 18, 52))
        val t = SystemClock.uptimeMillis() / 1000f
        val beat = exp(-((t * 128f / 60f) % 1f) * 5.5f)
        val note = notes[((t * 2.13f).toInt()) % notes.size]
        for (i in spectrum.indices) {
            val k = i.toFloat() / spectrum.size
            spectrum[i] =
                (
                    0.75f * exp(-k * 2.2f) * (0.35f + 0.65f * beat) +
                        0.28f * abs(sin(t * 3f + k * 9f)) * (1f - k * 0.5f) +
                        0.55f * exp(-((k - note) / 0.09f).let { it * it }) +
                        0.12f * noise()
                ).coerceIn(0f, 1f)
        }
        val unit = minOf(w, h * 0.56f)
        tile.set(w * 0.22f, h * 0.18f, w * 0.78f, h * 0.82f)
        val radius = minOf(tile.width(), tile.height()) * 0.08f
        renderer.draw(canvas, tile, radius, spectrum, SystemClock.uptimeMillis(), 255, unit)
        tilePaint.color = Color.argb(215, 8, 14, 20)
        canvas.drawRoundRect(tile, radius, radius, tilePaint)
        linePaint.color = Color.argb(110, 120, 170, 210)
        val lineH = tile.height() * 0.035f
        for (i in 0 until 8) {
            val y = tile.top + tile.height() * (0.12f + i * 0.1f)
            val indent = if (i == 0 || i == 4) tile.width() * 0.08f else 0f
            val right = tile.right - tile.width() * (if (i == 3 || i == 7) 0.35f else 0.1f)
            canvas.drawRoundRect(tile.left + tile.width() * 0.1f + indent, y, right, y + lineH, lineH / 2f, lineH / 2f, linePaint)
        }
        postInvalidateOnAnimation()
    }
}
