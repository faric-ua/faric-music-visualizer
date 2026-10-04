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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class ReactiveSceneView(
    context: Context,
    private val renderBackground: Boolean = true,
    private val renderVisualizer: Boolean = true,
) : View(context) {

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

    // Background has its own drive. Foreground frequency bands and background
    // travel speed are related to the same track, but are not the same animation.
    private var backgroundDriveTarget = 0.18f
    private var backgroundDrive = 0.18f
    private var backgroundBeat = 0f
    private var warpTravel = 0f

    private var playing = false
    private var lastFrameMs = SystemClock.elapsedRealtime()

    private var currentScene = SceneSpec(
        visualizerType = VisualizerType.RADIAL,
        backgroundType = BackgroundType.WARP_STARFIELD,
        palette = AccentPalette.SUNSET_CYAN,
        intensity = 0.92f,
        changeAfterMs = 30_000L,
    )
    private var previousScene: SceneSpec? = null
    private var transitionStartedMs = 0L

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high

        // Beat foreground reacts immediately.
        beatImpulse = max(beatImpulse, signal.beatStrength)

        // Background has its own response curve: highs make travel sharper,
        // bass/beat accelerate the forward "warp" feeling.
        backgroundDriveTarget = (
            0.22f +
                signal.amplitude * 1.35f +
                signal.high * 0.75f +
                signal.bass * 0.45f +
                signal.beatStrength * 1.4f
            ).coerceIn(0.18f, 3.2f)
        backgroundBeat = max(backgroundBeat, signal.beatStrength)

        // Attack path: do not wait for the render smoothing loop to notice
        // a new frequency spike.
        amplitude = max(amplitude, signal.amplitude * 0.94f)
        bass = max(bass, signal.bass * 0.96f)
        mid = max(mid, signal.mid * 0.96f)
        high = max(high, signal.high * 0.96f)

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

        amplitude = followReactive(amplitude, targetAmplitude, dt)
        bass = followReactive(bass, targetBass, dt)
        mid = followReactive(mid, targetMid, dt)
        high = followReactive(high, targetHigh, dt)

        backgroundDrive = followReactive(backgroundDrive, backgroundDriveTarget, dt, attackHz = 30f, releaseHz = 7f)

        beatImpulse = (beatImpulse - dt * 4.8f).coerceAtLeast(0f)
        backgroundBeat = (backgroundBeat - dt * 3.2f).coerceAtLeast(0f)

        if (!playing) {
            targetAmplitude *= 0.90f
            targetBass *= 0.90f
            targetMid *= 0.90f
            targetHigh *= 0.90f
            backgroundDriveTarget += (0.18f - backgroundDriveTarget) * (dt * 4f).coerceIn(0f, 1f)
        }

        warpTravel += dt * (0.42f + backgroundDrive * 1.25f + backgroundBeat * 1.6f)

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

    private fun followReactive(
        current: Float,
        target: Float,
        dt: Float,
        attackHz: Float = 46f,
        releaseHz: Float = 11f,
    ): Float {
        val rate = if (target > current) attackHz else releaseHz
        val factor = (dt * rate).coerceIn(0f, 1f)
        return current + (target - current) * factor
    }

    private fun drawScene(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        if (renderBackground) {
            drawBackground(canvas, spec, now, alpha)
        }
        if (renderVisualizer) {
            when (spec.visualizerType) {
                VisualizerType.RADIAL -> drawRadial(canvas, spec, now, alpha)
                VisualizerType.WAVE_RIBBON -> drawWaveRibbon(canvas, spec, now, alpha)
                VisualizerType.SPECTRUM_BARS -> drawSpectrumBars(canvas, spec, now, alpha)
            }
        }
    }

    private fun drawBackground(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        when (spec.backgroundType) {
            BackgroundType.WARP_STARFIELD -> drawWarpStarfield(canvas, spec, alpha)
            BackgroundType.AURORA -> drawAurora(canvas, spec, now, alpha)
            BackgroundType.NEON_MIST -> drawNeonMist(canvas, spec, now, alpha)
            BackgroundType.NIGHT_GRID -> drawNightGrid(canvas, spec, now, alpha)
            BackgroundType.EMBER_CLOUD -> drawEmberCloud(canvas, spec, now, alpha)
        }
    }

    private fun drawWarpStarfield(canvas: Canvas, spec: SceneSpec, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.39f
        val maxRadius = kotlin.math.hypot(w.toDouble(), h.toDouble()).toFloat() * 0.72f
        val colors = palette(spec.palette)

        paint.shader = RadialGradient(
            cx,
            cy,
            maxRadius,
            intArrayOf(Color.rgb(8, 13, 24), Color.rgb(2, 5, 11), Color.BLACK),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val speedStretch = (0.12f + backgroundDrive * 0.11f + backgroundBeat * 0.22f).coerceIn(0.08f, 0.62f)

        repeat(STAR_COUNT) { i ->
            val angle = hash01(i * 37 + 11) * (2f * PI.toFloat())
            val baseDepth = hash01(i * 97 + 29)
            var depth = (baseDepth - warpTravel * (0.055f + hash01(i * 17) * 0.035f)) % 1f
            if (depth < 0f) depth += 1f

            val progress = 1f - depth
            val perspective = progress.pow(2.35f)
            val radius = maxRadius * perspective
            val starCx = cx + cos(angle) * radius
            val starCy = cy + sin(angle) * radius * 0.84f

            val tailRadius = maxRadius * (progress - speedStretch * (0.22f + progress * 0.78f)).coerceAtLeast(0f).pow(2.35f)
            val tailX = cx + cos(angle) * tailRadius
            val tailY = cy + sin(angle) * tailRadius * 0.84f

            val tint = if (i % 7 == 0) colors.second else if (i % 11 == 0) colors.first else Color.WHITE
            strokePaint.color = tint
            strokePaint.alpha = ((40 + perspective * 215f) * alpha).toInt().coerceIn(0, 255)
            strokePaint.strokeWidth = (0.7f + perspective * 3.2f + backgroundBeat * 1.2f) * resources.displayMetrics.density
            canvas.drawLine(tailX, tailY, starCx, starCy, strokePaint)
        }

        // Independent background pulse at the vanishing point.
        drawGlow(
            canvas,
            cx,
            cy,
            minOf(w, h) * (0.11f + backgroundBeat * 0.08f),
            colors.second,
            alpha * (0.10f + backgroundBeat * 0.22f),
        )

        paint.alpha = 255
        strokePaint.alpha = 255
    }

    private fun drawAurora(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val phase = now / 1000.0

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(Color.rgb(2, 5, 10), darken(colors.first, 0.32f), darken(colors.second, 0.26f)),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        drawGlow(canvas, w * (0.22f + 0.08f * sin(phase * (0.4 + backgroundDrive * 0.2)).toFloat()), h * 0.30f,
            minOf(w, h) * 0.58f, colors.first, alpha * (0.22f + backgroundBeat * 0.10f))
        drawGlow(canvas, w * (0.78f + 0.07f * cos(phase * (0.35 + backgroundDrive * 0.16)).toFloat()), h * 0.48f,
            minOf(w, h) * 0.64f, colors.second, alpha * (0.20f + backgroundBeat * 0.10f))
    }

    private fun drawNeonMist(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val phase = now / 1000.0

        paint.color = withAlpha(Color.rgb(2, 5, 9), alpha)
        canvas.drawRect(0f, 0f, w, h, paint)

        val minSide = minOf(w, h)
        repeat(6) { i ->
            val x = w * (0.12f + i * 0.16f) +
                sin(phase * (0.25 + backgroundDrive * 0.15 + i * 0.02) + i).toFloat() * w * 0.10f
            val y = h * (0.15f + (i % 3) * 0.23f)
            val color = if (i % 2 == 0) colors.first else colors.second
            drawGlow(canvas, x, y, minSide * (0.30f + i * 0.03f), color, alpha * (0.15f + backgroundBeat * 0.06f))
        }
    }

    private fun drawNightGrid(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.rgb(2, 5, 9), darken(colors.second, 0.16f), Color.rgb(1, 3, 6)),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        strokePaint.alpha = ((55 + backgroundBeat * 90f) * alpha).toInt().coerceIn(0, 255)
        strokePaint.color = colors.second
        strokePaint.strokeWidth = (1f + backgroundBeat) * resources.displayMetrics.density
        val spacing = maxOf(42f, w / 10f)
        val scroll = ((now / (26f / (0.55f + backgroundDrive))).toInt() % spacing.toInt()).toFloat()

        var x = scroll
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, strokePaint)
            x += spacing
        }

        var y = scroll
        while (y < h) {
            canvas.drawLine(0f, y, w, y, strokePaint)
            y += spacing
        }
    }

    private fun drawEmberCloud(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val phase = now / 1000.0

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(Color.rgb(8, 3, 2), darken(colors.first, 0.24f), Color.rgb(2, 4, 8)),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val minSide = minOf(w, h)
        repeat(5) { i ->
            val x = w * (0.12f + i * 0.20f)
            val y = h * (0.26f + 0.10f * sin(phase * (0.45 + backgroundDrive * 0.12) + i).toFloat())
            drawGlow(canvas, x, y, minSide * (0.25f + i * 0.035f), colors.first,
                alpha * (0.16f + backgroundBeat * 0.08f))
        }
        drawGlow(canvas, w * 0.7f, h * 0.45f, minSide * 0.52f, colors.second,
            alpha * (0.10f + backgroundBeat * 0.06f))
    }

    private fun drawRadial(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w * 0.5f
        val cy = h * 0.43f
        val minSide = minOf(w, h)
        val colors = palette(spec.palette)
        val intensity = spec.intensity

        val innerRadius = minSide * (0.115f + beatImpulse * 0.022f * intensity)
        val rayCount = 84

        for (i in 0 until rayCount) {
            val angle = (2.0 * PI * i / rayCount) - PI / 2.0
            val band = when (i % 3) {
                0 -> bass
                1 -> mid
                else -> high
            }

            val shimmer = ((sin(now / 520.0 + i * 0.57) + 1.0) * 0.5).toFloat()
            val length = minSide * (
                0.018f +
                    band * 0.20f * intensity +
                    amplitude * 0.055f +
                    beatImpulse * 0.13f * intensity +
                    shimmer * 0.012f
                )
            val outer = innerRadius + length

            strokePaint.strokeWidth =
                (1.1f + band * 6.2f + beatImpulse * 3f) * resources.displayMetrics.density
            strokePaint.color = blend(colors.first, colors.second, i / rayCount.toFloat())
            strokePaint.alpha =
                ((82 + band * 165f + beatImpulse * 55f) * alpha).toInt().coerceIn(0, 255)

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

        val amp = h * (0.018f + amplitude * 0.14f + bass * 0.05f + beatImpulse * 0.09f) * spec.intensity

        repeat(6) { ribbon ->
            path.reset()
            val offset = (ribbon - 2.5f) * h * 0.016f
            var x = 0f
            while (x <= w) {
                val progress = x / w
                val localBand = when (ribbon % 3) {
                    0 -> bass
                    1 -> mid
                    else -> high
                }
                val y = centerY + offset +
                    sin(progress * PI * (3.4 + ribbon * 0.26) + phase * (2.0 + localBand * 2.4)).toFloat() *
                    amp * (0.52f + ribbon * 0.10f + localBand * 0.24f)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += maxOf(4f, w / 140f)
            }

            strokePaint.color = blend(colors.first, colors.second, ribbon / 5f)
            strokePaint.strokeWidth =
                (1.6f + ribbon * 0.58f + mid * 4f + beatImpulse * 2.2f) * resources.displayMetrics.density
            strokePaint.alpha = ((80 + ribbon * 24 + high * 70f) * alpha).toInt().coerceIn(0, 255)
            canvas.drawPath(path, strokePaint)
        }

        drawPulseCore(canvas, w * 0.5f, centerY, minOf(w, h), colors, spec.intensity, alpha * 0.88f)
    }

    private fun drawSpectrumBars(canvas: Canvas, spec: SceneSpec, now: Long, alpha: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val colors = palette(spec.palette)
        val barCount = 36
        val gap = w * 0.006f
        val usable = w * 0.90f
        val barWidth = (usable - gap * (barCount - 1)) / barCount
        val startX = (w - usable) / 2f
        val baseline = h * 0.50f

        repeat(barCount) { i ->
            val band = when {
                i < barCount / 3 -> bass
                i < barCount * 2 / 3 -> mid
                else -> high
            }
            val oscillation = ((sin(now / 280.0 + i * 0.81) + 1.0) * 0.5).toFloat()
            val height = h * (
                0.018f +
                    band * 0.27f * spec.intensity +
                    amplitude * 0.055f +
                    beatImpulse * 0.12f +
                    oscillation * 0.018f
                )

            val left = startX + i * (barWidth + gap)
            paint.color = blend(colors.first, colors.second, i / (barCount - 1f))
            paint.alpha = ((105 + band * 145f + beatImpulse * 50f) * alpha).toInt().coerceIn(0, 255)

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

        drawPulseCore(canvas, w * 0.5f, h * 0.36f, minOf(w, h), colors, spec.intensity, alpha * 0.80f)
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
        val orbRadius =
            minSide * (0.088f + bass * 0.034f + mid * 0.010f + beatImpulse * 0.027f * intensity)

        drawGlow(canvas, cx, cy, orbRadius * 2.6f, colors.first,
            alpha * (0.18f + beatImpulse * 0.30f + bass * 0.08f))
        drawGlow(canvas, cx, cy, orbRadius * 1.9f, colors.second,
            alpha * (0.12f + high * 0.08f))

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

        strokePaint.strokeWidth =
            (2.2f + beatImpulse * 2.4f) * resources.displayMetrics.density
        strokePaint.color = blend(colors.first, colors.second, 0.5f)
        strokePaint.alpha =
            ((165 + beatImpulse * 90f + high * 35f) * alpha).toInt().coerceIn(0, 255)
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

    private fun hash01(seed: Int): Float {
        val x = sin(seed * 12.9898).toFloat() * 43758.5453f
        return abs(x - kotlin.math.floor(x))
    }

    companion object {
        private const val TRANSITION_MS = 650L
        private const val STAR_COUNT = 96
    }
}
