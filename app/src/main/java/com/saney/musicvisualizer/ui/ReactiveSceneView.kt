package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class ReactiveSceneView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var targetAmplitude = 0f
    private var targetBass = 0f
    private var targetMid = 0f
    private var targetHigh = 0f
    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beatImpulse = 0f
    private var playing = false
    private var lastFrameMs = SystemClock.elapsedRealtime()

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high
        beatImpulse = max(beatImpulse, signal.beatStrength)
        postInvalidateOnAnimation()
    }
    fun setPlaying(value: Boolean) { playing = value; postInvalidateOnAnimation() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.elapsedRealtime()
        val dt = ((now - lastFrameMs).coerceIn(1L, 50L) / 1000f)
        lastFrameMs = now
        val follow = (dt * 8f).coerceIn(0f, 1f)
        amplitude += (targetAmplitude - amplitude) * follow
        bass += (targetBass - bass) * follow
        mid += (targetMid - mid) * follow
        high += (targetHigh - high) * follow
        beatImpulse = (beatImpulse - dt * 2.8f).coerceAtLeast(0f)
        if (!playing) {
            targetAmplitude *= 0.96f; targetBass *= 0.96f; targetMid *= 0.96f; targetHigh *= 0.96f
        }

        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val pulse = (0.18f + amplitude * 0.5f + beatImpulse * 0.7f).coerceIn(0f, 1.4f)
        val cx = w * 0.5f; val cy = h * 0.45f; val minSide = minOf(w, h)

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.rgb(8, 9, 18),
                Color.rgb((18 + bass * 35).toInt(), 12, (38 + mid * 48).toInt()),
                Color.rgb(6, (16 + high * 30).toInt(), 24),
            ),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val ambientRadius = minSide * (0.24f + pulse * 0.05f)
        for (ring in 4 downTo 1) {
            paint.color = Color.argb((18 + beatImpulse * 42f).toInt().coerceIn(0, 70), 255, 133 + ring * 16, 76)
            canvas.drawCircle(cx, cy, ambientRadius * (1f + ring * 0.08f), paint)
        }

        val rayCount = 64
        val innerRadius = minSide * (0.14f + beatImpulse * 0.012f)
        val phase = now / 1000.0
        for (i in 0 until rayCount) {
            val angle = (2.0 * PI * i / rayCount) - PI / 2.0
            val band = when (i % 3) { 0 -> bass; 1 -> mid; else -> high }
            val shimmer = ((sin(phase * 2.0 + i * 0.61) + 1.0) * 0.5).toFloat()
            val length = minSide * (0.025f + band * 0.09f + amplitude * 0.04f + beatImpulse * 0.08f + shimmer * 0.008f)
            val outer = innerRadius + length
            rayPaint.strokeWidth = (1.5f + band * 4f + beatImpulse * 2f) * resources.displayMetrics.density
            rayPaint.color = Color.argb(
                (85 + band * 135f + beatImpulse * 35f).toInt().coerceIn(0, 255),
                255,
                (210 + high * 35f).toInt().coerceIn(0, 255),
                (170 + high * 70f).toInt().coerceIn(0, 255),
            )
            canvas.drawLine(
                cx + cos(angle).toFloat() * innerRadius,
                cy + sin(angle).toFloat() * innerRadius,
                cx + cos(angle).toFloat() * outer,
                cy + sin(angle).toFloat() * outer,
                rayPaint,
            )
        }

        val orbRadius = minSide * (0.105f + bass * 0.016f + beatImpulse * 0.012f)
        paint.color = Color.argb((55 + beatImpulse * 90f).toInt().coerceIn(0, 180), 255, 122, 52)
        canvas.drawCircle(cx, cy, orbRadius * 1.18f, paint)
        paint.color = Color.rgb(10, 10, 14)
        canvas.drawCircle(cx, cy, orbRadius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f * resources.displayMetrics.density
        paint.color = Color.argb((150 + beatImpulse * 100f).toInt().coerceIn(0, 255), 255, 192, 116)
        canvas.drawCircle(cx, cy, orbRadius * 0.92f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = orbRadius * 0.28f
        paint.isFakeBoldText = true
        paint.color = Color.argb(220, 245, 245, 250)
        canvas.drawText("FARIC", cx, cy + paint.textSize * 0.34f, paint)
        paint.isFakeBoldText = false
        postInvalidateOnAnimation()
    }
}
