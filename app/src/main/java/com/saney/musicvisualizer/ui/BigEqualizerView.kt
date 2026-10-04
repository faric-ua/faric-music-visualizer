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

/** Layer 1: independent large rounded-bar equalizer. */
class BigEqualizerView(
    context: Context,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var targetBass = 0f
    private var targetMid = 0f
    private var targetHigh = 0f
    private var targetAmplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var amplitude = 0f
    private var playing = false

    fun updateSignal(signal: SceneSignal) {
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high
        targetAmplitude = signal.amplitude
        bass = max(bass, targetBass)
        mid = max(mid, targetMid)
        high = max(high, targetHigh)
        amplitude = max(amplitude, targetAmplitude)
        postInvalidateOnAnimation()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val attack = 0.58f
        val release = if (playing) 0.16f else 0.08f
        bass += (targetBass - bass) * if (targetBass > bass) attack else release
        mid += (targetMid - mid) * if (targetMid > mid) attack else release
        high += (targetHigh - high) * if (targetHigh > high) attack else release
        amplitude += (targetAmplitude - amplitude) * if (targetAmplitude > amplitude) attack else release

        val count = 34
        val usable = w * 0.86f
        val gap = w * 0.008f
        val barWidth = (usable - gap * (count - 1)) / count
        val startX = (w - usable) * 0.5f
        val baseline = h * 0.39f
        val minHeight = h * 0.018f
        val maxHeight = h * 0.12f

        repeat(count) { index ->
            val normalized = index / (count - 1f)
            val band = when {
                normalized < 0.34f -> bass
                normalized < 0.68f -> mid
                else -> high
            }
            val centerWeight = 1f - kotlin.math.abs(normalized - 0.5f) * 0.32f
            val value = (band * 0.82f + amplitude * 0.18f).coerceIn(0f, 1f)
            val barHeight = minHeight + maxHeight * value * centerWeight
            val left = startX + index * (barWidth + gap)
            val top = baseline - barHeight * 0.5f
            val bottom = baseline + barHeight * 0.5f

            paint.shader = LinearGradient(
                left, top, left + barWidth, bottom,
                Color.rgb(150, 72, 220),
                Color.rgb(0, 214, 205),
                Shader.TileMode.CLAMP,
            )
            paint.alpha = (125 + value * 120f).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(left, top, left + barWidth, bottom, barWidth * 0.5f, barWidth * 0.5f, paint)
        }

        paint.shader = null
        paint.alpha = 255
        if (playing) postInvalidateOnAnimation()
    }
}
