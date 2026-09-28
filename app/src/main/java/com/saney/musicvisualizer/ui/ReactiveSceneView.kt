package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.scene.AccentPalette
import com.saney.musicvisualizer.scene.BackgroundType
import com.saney.musicvisualizer.scene.SceneSpec
import com.saney.musicvisualizer.scene.VisualizerType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class ReactiveSceneView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

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

    private var currentScene = SceneSpec(
        visualizerType = VisualizerType.RADIAL,
        backgroundType = BackgroundType.AURORA,
        palette = AccentPalette.SUNSET_CYAN,
        intensity = 0.86f,
        changeAfterMs = 30_000L,
    )
    private var previousScene: SceneSpec? = null
    private var transitionStartedMs = 0L

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high
        beatImpulse = max(beatImpulse, signal.beatStrength)
        postInvalidateOnAnimation()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        postInvalidateOnAnimation()
    }

    fun setScene(spec: SceneSpec) {
        if (spec == currentScene) return
        previousScene = currentScene
        currentScene = spec
        transitionStartedMs = SystemClock.elapsedRealtime()
        postInvalidateOnAnimation()
    }

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
            targetAmplitude *= 0.96f
            targetBass *= 0.96f
            targetMid *= 0.96f
            targetHigh *= 0.96f
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val transition = if (transitionStartedMs == 0L) {
            1f
        } else {
            ((now - transitionStartedMs) / TRANSITION_MS.toFloat()).coerceIn(0f, 1f)
        }

        previousScene?.let { old ->
            if (transition < 1f) {
                drawScene(canvas, old, now, 1f - transition)
            } else {
                previousScene = null
            }
        }
        drawScene(canvas, currentScene, now, transition)

        postInvalidateOnAnimation()
    }

    private fun drawScene(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        drawBackground(canvas, spec, now, alpha)
        when (spec.visualizerType) {
            VisualizerType.RADIAL -> drawRadial(canvas, spec, now, alpha)
            VisualizerType.WAVE_RIBBON -> drawWaveRibbon(canvas, spec, now, alpha)
            VisualizerType.SPECTRUM_BARS -> drawSpectrumBars(canvas, spec, now, alpha)
        }
    }

    private fun drawBackground(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val phase = now / 1000.0

        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        paint.style = Paint.Style.FILL

        when (spec.backgroundType) {
            BackgroundType.AURORA -> {
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(Color.rgb(2, 5, 10), darken(colors.first, 0.32f), darken(colors.second, 0.26f)),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                drawGlow(
                    canvas,
                    w * (0.22f + 0.06f * sin(phase * 0.31).toFloat()),
                    h * 0.30f,
                    minOf(w, h) * 0.56f,
                    colors.first,
                    alpha * 0.34f,
                )
                drawGlow(
                    canvas,
                    w * (0.78f + 0.05f * cos(phase * 0.27).toFloat()),
                    h * 0.48f,
                    minOf(w, h) * 0.62f,
                    colors.second,
                    alpha * 0.28f,
                )
            }

            BackgroundType.NEON_MIST -> {
                paint.color = withAlpha(Color.rgb(2, 5, 9), alpha)
                canvas.drawRect(0f, 0f, w, h, paint)
                val minSide = minOf(w, h)
                repeat(5) { i ->
                    val x = w * (0.15f + i * 0.18f) +
                        sin(phase * (0.18 + i * 0.025) + i).toFloat() * w * 0.08f
                    val y = h * (0.18f + (i % 3) * 0.22f)
                    val color = if (i % 2 == 0) colors.first else colors.second
                    drawGlow(canvas, x, y, minSide * (0.32f + i * 0.035f), color, alpha * 0.22f)
                }
            }

            BackgroundType.NIGHT_GRID -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.rgb(2, 5, 9), darken(colors.second, 0.16f), Color.rgb(1, 3, 6)),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                strokePaint.alpha = (70 * alpha).toInt().coerceIn(0, 255)
                strokePaint.color = colors.second
                strokePaint.strokeWidth = 1f * resources.displayMetrics.density
                val spacing = maxOf(42f, w / 10f)
                var x = (now / 24L % spacing.toLong()).toFloat()
                while (x < w) {
                    canvas.drawLine(x, 0f, x, h, strokePaint)
                    x += spacing
                }
                var y = 0f
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, strokePaint)
                    y += spacing
                }
            }

            BackgroundType.EMBER_CLOUD -> {
                paint.shader = LinearGradient(
                    0f, 0f, w, h,
                    intArrayOf(Color.rgb(8, 3, 2), darken(colors.first, 0.24f), Color.rgb(2, 4, 8)),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null

                val minSide = minOf(w, h)
                repeat(4) { i ->
                    val x = w * (0.18f + i * 0.22f)
                    val y = h * (0.28f + 0.08f * sin(phase * 0.35 + i).toFloat())
                    drawGlow(canvas, x, y, minSide * (0.28f + i * 0.03f), colors.first, alpha * 0.22f)
                }
                drawGlow(canvas, w * 0.7f, h * 0.45f, minSide * 0.52f, colors.second, alpha * 0.14f)
            }
        }

        paint.alpha = 255
        strokePaint.alpha = 255
    }

    private fun drawRadial(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.43f
        val minSide = minOf(w, h)
        val colors = palette(spec.palette)
        val intensity = spec.intensity
        val innerRadius = minSide * (0.13f + beatImpulse * 0.014f * intensity)
        val rayCount = 72

        for (i in 0 until rayCount) {
            val angle = (2.0 * PI * i / rayCount) - PI / 2.0
            val band = when (i % 3) {
                0 -> bass
                1 -> mid
                else -> high
            }
            val shimmer = ((sin(now / 760.0 + i * 0.51) + 1.0) * 0.5).toFloat()
            val length = minSide * (
                0.025f +
                    band * 0.12f * intensity +
                    amplitude * 0.035f +
                    beatImpulse * 0.085f * intensity +
                    shimmer * 0.009f
                )
            val outer = innerRadius + length

            strokePaint.strokeWidth = (1.4f + band * 4.5f + beatImpulse * 2f) * resources.displayMetrics.density
            strokePaint.color = blend(colors.first, colors.second, i / rayCount.toFloat())
            strokePaint.alpha = ((95 + band * 130f + beatImpulse * 30f) * alpha).toInt().coerceIn(0, 255)

            canvas.drawLine(
                cx + cos(angle).toFloat() * innerRadius,
                cy + sin(angle).toFloat() * innerRadius,
                cx + cos(angle).toFloat() * outer,
                cy + sin(angle).toFloat() * outer,
                strokePaint,
            )
        }

        drawPulseCore(canvas, cx, cy, minSide, colors, intensity, alpha)
    }

    private fun drawWaveRibbon(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val centerY = h * 0.43f
        val phase = now / 1000.0
        val amp = h * (0.025f + amplitude * 0.08f + beatImpulse * 0.05f) * spec.intensity

        repeat(5) { ribbon ->
            path.reset()
            val offset = (ribbon - 2) * h * 0.018f
            var x = 0f
            while (x <= w) {
                val progress = x / w
                val y = centerY + offset +
                    sin(progress * PI * (3.0 + ribbon * 0.2) + phase * (1.5 + ribbon * 0.07)).toFloat() *
                    amp * (0.58f + ribbon * 0.10f)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += maxOf(5f, w / 120f)
            }

            strokePaint.color = blend(colors.first, colors.second, ribbon / 4f)
            strokePaint.strokeWidth = (2f + ribbon * 0.65f + bass * 3f) * resources.displayMetrics.density
            strokePaint.alpha = ((95 + ribbon * 22) * alpha).toInt().coerceIn(0, 255)
            canvas.drawPath(path, strokePaint)
        }

        drawPulseCore(canvas, w * 0.5f, centerY, minOf(w, h), colors, spec.intensity, alpha * 0.86f)
    }

    private fun drawSpectrumBars(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val barCount = 32
        val gap = w * 0.008f
        val usable = w * 0.86f
        val barWidth = (usable - gap * (barCount - 1)) / barCount
        val startX = (w - usable) / 2f
        val baseline = h * 0.50f

        repeat(barCount) { i ->
            val band = when {
                i < barCount / 3 -> bass
                i < barCount * 2 / 3 -> mid
                else -> high
            }
            val oscillation = ((sin(now / 430.0 + i * 0.73) + 1.0) * 0.5).toFloat()
            val height = h * (
                0.035f +
                    band * 0.15f * spec.intensity +
                    beatImpulse * 0.075f +
                    oscillation * 0.025f
                )
            val left = startX + i * (barWidth + gap)
            paint.color = blend(colors.first, colors.second, i / (barCount - 1f))
            paint.alpha = ((125 + band * 110f) * alpha).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(
                left,
                baseline - height / 2f,
                left + barWidth,
                baseline + height / 2f,
                barWidth / 2f,
                barWidth / 2f,
                paint,
            )
        }

        drawPulseCore(canvas, w * 0.5f, h * 0.37f, minOf(w, h), colors, spec.intensity, alpha * 0.82f)
        paint.alpha = 255
    }

    private fun drawPulseCore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        colors: Pair<Int, Int>,
        intensity: Float,
        alpha: Float,
    ) {
        val orbRadius = minSide * (0.095f + bass * 0.018f + beatImpulse * 0.014f * intensity)

        drawGlow(canvas, cx, cy, orbRadius * 2.3f, colors.first, alpha * (0.20f + beatImpulse * 0.18f))
        drawGlow(canvas, cx, cy, orbRadius * 1.75f, colors.second, alpha * 0.14f)

        paint.shader = RadialGradient(
            cx,
            cy,
            orbRadius,
            intArrayOf(Color.rgb(20, 28, 34), Color.rgb(5, 8, 12), Color.BLACK),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, orbRadius, paint)
        paint.shader = null

        strokePaint.strokeWidth = 2.2f * resources.displayMetrics.density
        strokePaint.color = blend(colors.first, colors.second, 0.5f)
        strokePaint.alpha = ((170 + beatImpulse * 70f) * alpha).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, orbRadius * 0.93f, strokePaint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = orbRadius * 0.25f
        paint.isFakeBoldText = true
        paint.color = Color.WHITE
        paint.alpha = (220 * alpha).toInt().coerceIn(0, 255)
        canvas.drawText("PULSE", cx, cy + paint.textSize * 0.35f, paint)
        paint.isFakeBoldText = false
        paint.alpha = 255
        strokePaint.alpha = 255
    }

    private fun drawGlow(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, alpha: Float) {
        if (radius <= 0f || alpha <= 0f) return
        paint.shader = RadialGradient(
            x,
            y,
            radius,
            intArrayOf(withAlpha(color, alpha), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.alpha = 255
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }

    private fun palette(palette: AccentPalette): Pair<Int, Int> = when (palette) {
        AccentPalette.SUNSET_CYAN -> Color.rgb(255, 145, 24) to Color.rgb(35, 211, 238)
        AccentPalette.VIOLET_TEAL -> Color.rgb(170, 86, 255) to Color.rgb(21, 229, 197)
        AccentPalette.LIME_MAGENTA -> Color.rgb(188, 255, 46) to Color.rgb(255, 59, 178)
    }

    private fun darken(color: Int, amount: Float): Int =
        Color.rgb(
            (Color.red(color) * amount).toInt().coerceIn(0, 255),
            (Color.green(color) * amount).toInt().coerceIn(0, 255),
            (Color.blue(color) * amount).toInt().coerceIn(0, 255),
        )

    private fun blend(a: Int, b: Int, t: Float): Int {
        val p = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * p).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * p).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * p).toInt(),
        )
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb(
            (255 * alpha.coerceIn(0f, 1f)).toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    companion object {
        private const val TRANSITION_MS = 850L
    }
}
