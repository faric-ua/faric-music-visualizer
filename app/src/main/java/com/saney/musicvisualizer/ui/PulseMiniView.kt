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

class PulseMiniView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private var energy = 0.12f
    private var beat = 0f

    fun updateSignal(signal: SceneSignal) {
        energy = (signal.amplitude * 0.55f + signal.bass * 0.45f).coerceIn(0f, 1f)
        beat = max(beat, signal.beatStrength)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        beat *= 0.86f
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        paint.shader = LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(Color.rgb(255, 145, 18), Color.rgb(31, 207, 235)),
            null,
            Shader.TileMode.CLAMP,
        )

        val bars = 9
        val gap = w / (bars + 1)
        val t = System.nanoTime() / 1_000_000_000.0

        for (i in 0 until bars) {
            val phase = ((sin(t * 2.6 + i * 0.8) + 1.0) * 0.5).toFloat()
            val barEnergy = (0.2f + energy * 0.65f + beat * 0.55f + phase * 0.18f).coerceIn(0.12f, 1f)
            val barHeight = h * barEnergy * (0.35f + (i % 4) * 0.12f)
            val x = gap * (i + 1)
            paint.strokeWidth = (width / 22f).coerceAtLeast(2f)
            canvas.drawLine(x, h / 2f - barHeight / 2f, x, h / 2f + barHeight / 2f, paint)
        }

        paint.shader = null
        postInvalidateOnAnimation()
    }
}
