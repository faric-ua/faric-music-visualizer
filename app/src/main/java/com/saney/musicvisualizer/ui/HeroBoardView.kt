package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardAudioState
import com.saney.musicvisualizer.board.BoardLayerMotion
import com.saney.musicvisualizer.board.BoardLayerMotionEvaluator
import com.saney.musicvisualizer.board.BoardLayerReaction
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * First real layered Board Hero/GF renderer.
 *
 * Cyber Shark uses four independently reactive layers:
 * frame -> FX -> creature -> wordmark.
 *
 * Frame / creature / wordmark use optimized transparent image resources.
 * FX is procedural in this first app proof so it can react strongly without
 * growing the APK; the full master FX remains in the companion asset repository.
 */
class HeroBoardView(context: Context) : View(context) {
    private val bitmapPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val fxPath = Path()

    private val frameBitmap =
        decodeSafely(R.drawable.cyber_shark_frame)
    private val creatureBitmap =
        decodeSafely(R.drawable.cyber_shark_creature)
    private val wordmarkBitmap =
        decodeSafely(R.drawable.cyber_shark_wordmark)

    private var renderErrorLogged = false

    private val frameReaction =
        BoardLayerReaction(
            baseScale = 0.92f,
            bassScale = 0.055f,
            amplitudeScale = 0.012f,
            beatScale = 0.018f,
            baseAlpha = 0.98f,
            rotationDegPerSecond = 1.15f,
            beatRotationDeg = 0.8f,
        )

    private val fxReaction =
        BoardLayerReaction(
            baseScale = 0.94f,
            bassScale = 0.020f,
            highScale = 0.045f,
            beatScale = 0.050f,
            baseAlpha = 0.18f,
            highAlpha = 0.42f,
            beatAlpha = 0.34f,
            rotationDegPerSecond = -0.65f,
            beatRotationDeg = -1.5f,
        )

    private val creatureReaction =
        BoardLayerReaction(
            baseScale = 0.94f,
            amplitudeScale = 0.010f,
            bassScale = 0.025f,
            midScale = 0.010f,
            beatScale = 0.070f,
            baseAlpha = 1f,
            beatTranslateYFraction = -0.010f,
        )

    private val wordmarkReaction =
        BoardLayerReaction(
            baseScale = 0.94f,
            bassScale = 0.010f,
            highScale = 0.010f,
            beatScale = 0.035f,
            baseAlpha = 1f,
            beatTranslateYFraction = 0.004f,
        )

    private var targetAmplitude = 0f
    private var targetBass = 0f
    private var targetMid = 0f
    private var targetHigh = 0f

    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f

    private var playing = false
    private var lastFrameMs = SystemClock.elapsedRealtime()

    fun setPlaying(value: Boolean) {
        playing = value
        postInvalidateOnAnimation()
    }

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high

        amplitude = max(amplitude, signal.amplitude * 0.96f)
        bass = max(bass, signal.bass * 0.98f)
        mid = max(mid, signal.mid * 0.96f)
        high = max(high, signal.high * 0.96f)
        beat = max(beat, signal.beatStrength)

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = SystemClock.elapsedRealtime()
        val dt =
            ((now - lastFrameMs).coerceIn(1L, 50L) / 1000f)
        lastFrameMs = now

        amplitude = follow(amplitude, targetAmplitude, dt, 58f, 12f)
        bass = follow(bass, targetBass, dt, 74f, 15f)
        mid = follow(mid, targetMid, dt, 54f, 13f)
        high = follow(high, targetHigh, dt, 62f, 14f)
        beat = (beat - dt * 3.4f).coerceAtLeast(0f)

        if (!playing) {
            targetAmplitude *= 0.90f
            targetBass *= 0.90f
            targetMid *= 0.90f
            targetHigh *= 0.90f
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val minSide = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.39f
        val timeSeconds = now / 1000f
        val baseSize = minSide * 0.92f

        drawBoardBackground(
            canvas = canvas,
            w = w,
            h = h,
            cx = cx,
            cy = cy,
            minSide = minSide,
        )

        val audio =
            BoardAudioState(
                amplitude = amplitude,
                bass = bass,
                mid = mid,
                high = high,
                beat = beat,
            )

        val frameMotion =
            BoardLayerMotionEvaluator.evaluate(
                frameReaction,
                audio,
                timeSeconds,
            )
        drawBitmapLayer(
            canvas,
            frameBitmap,
            frameMotion,
            cx,
            cy,
            baseSize,
        )

        val fxMotion =
            BoardLayerMotionEvaluator.evaluate(
                fxReaction,
                audio,
                timeSeconds,
            )
        drawFxLayer(
            canvas,
            fxMotion,
            cx,
            cy,
            baseSize,
            timeSeconds,
        )

        val creatureMotion =
            BoardLayerMotionEvaluator.evaluate(
                creatureReaction,
                audio,
                timeSeconds,
            )
        drawBitmapLayer(
            canvas,
            creatureBitmap,
            creatureMotion,
            cx,
            cy,
            baseSize,
        )

        val wordmarkMotion =
            BoardLayerMotionEvaluator.evaluate(
                wordmarkReaction,
                audio,
                timeSeconds,
            )
        drawBitmapLayer(
            canvas,
            wordmarkBitmap,
            wordmarkMotion,
            cx,
            cy,
            baseSize,
        )

        bitmapPaint.alpha = 255
        postInvalidateOnAnimation()
    }

    private fun drawBitmapLayer(
        canvas: Canvas,
        bitmap: Bitmap?,
        motion: BoardLayerMotion,
        cx: Float,
        cy: Float,
        baseSize: Float,
    ) {
        if (bitmap == null) {
            drawMissingLayerFallback(
                canvas = canvas,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                motion = motion,
            )
            return
        }

        bitmapPaint.alpha =
            (motion.alpha * 255f)
                .toInt()
                .coerceIn(0, 255)

        val size = baseSize * motion.scale
        val layerCy =
            cy +
                baseSize *
                motion.translateYFraction

        canvas.save()
        canvas.rotate(
            motion.rotationDegrees,
            cx,
            layerCy,
        )
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                cx - size * 0.5f,
                layerCy - size * 0.5f,
                cx + size * 0.5f,
                layerCy + size * 0.5f,
            ),
            bitmapPaint,
        )
        canvas.restore()
    }

    private fun drawFxLayer(
        canvas: Canvas,
        motion: BoardLayerMotion,
        cx: Float,
        cy: Float,
        baseSize: Float,
        timeSeconds: Float,
    ) {
        val size = baseSize * motion.scale
        val radius = size * 0.43f

        canvas.save()
        canvas.rotate(
            motion.rotationDegrees,
            cx,
            cy,
        )

        repeat(3) { ring ->
            val phase =
                timeSeconds *
                    (0.8f + ring * 0.16f)

            fxPath.reset()
            val points = 96

            for (i in 0..points) {
                val a =
                    i.toFloat() /
                        points *
                        (PI * 2.0)

                val wave =
                    1f +
                        0.035f *
                        sin(
                            a * (5 + ring) +
                                phase,
                        ).toFloat() +
                        high * 0.025f *
                        sin(
                            a * 13.0 -
                                phase * 1.8,
                        ).toFloat()

                val r =
                    radius *
                        (0.78f + ring * 0.095f) *
                        wave

                val x =
                    cx +
                        cos(a).toFloat() *
                        r

                val y =
                    cy +
                        sin(a).toFloat() *
                        r *
                        0.88f

                if (i == 0) {
                    fxPath.moveTo(x, y)
                } else {
                    fxPath.lineTo(x, y)
                }
            }

            stroke.color =
                if (ring == 1) {
                    Color.rgb(122, 224, 255)
                } else {
                    Color.rgb(0, 207, 255)
                }

            stroke.alpha =
                (motion.alpha * (165 - ring * 28))
                    .toInt()
                    .coerceIn(0, 230)

            stroke.strokeWidth =
                min(width, height) *
                    (0.0032f +
                        high * 0.0035f +
                        beat * 0.0045f)

            canvas.drawPath(
                fxPath,
                stroke,
            )
        }

        repeat(18) { i ->
            val a =
                i / 18f *
                    (PI * 2.0) +
                    timeSeconds *
                    (0.09 + (i % 3) * 0.018)

            val r =
                radius *
                    (0.68f +
                        (i % 5) * 0.065f +
                        beat * 0.04f)

            fill.color =
                Color.argb(
                    (motion.alpha * (80 + high * 130f))
                        .toInt()
                        .coerceIn(0, 210),
                    80,
                    223,
                    255,
                )

            val dot =
                min(width, height) *
                    (0.0025f +
                        (i % 4) * 0.0011f +
                        high * 0.0025f)

            canvas.drawCircle(
                cx + cos(a).toFloat() * r,
                cy + sin(a).toFloat() * r * 0.88f,
                dot,
                fill,
            )
        }

        canvas.restore()
    }

    private fun drawBoardBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        minSide: Float,
    ) {
        fill.color = Color.rgb(1, 5, 10)
        canvas.drawRect(0f, 0f, w, h, fill)

        fill.shader =
            RadialGradient(
                cx,
                cy,
                minSide * (0.62f + bass * 0.07f),
                intArrayOf(
                    Color.argb(
                        (75 + bass * 70f + beat * 45f)
                            .toInt()
                            .coerceIn(0, 190),
                        0,
                        136,
                        255,
                    ),
                    Color.argb(
                        (28 + high * 35f)
                            .toInt()
                            .coerceIn(0, 100),
                        0,
                        229,
                        255,
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.46f, 1f),
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            minSide * 0.69f,
            fill,
        )
        fill.shader = null
    }

    private fun decodeSafely(drawable: Int): Bitmap? =
        runCatching {
            BitmapFactory.decodeResource(
                resources,
                drawable,
                BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull()

    private fun drawMissingLayerFallback(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseSize: Float,
        motion: BoardLayerMotion,
    ) {
        if (!renderErrorLogged) {
            android.util.Log.e(
                "HeroBoardView",
                "Cyber Shark bitmap resource could not be decoded; using fallback rendering.",
            )
            renderErrorLogged = true
        }

        fill.color =
            Color.argb(
                (motion.alpha * 120f)
                    .toInt()
                    .coerceIn(0, 180),
                0,
                190,
                255,
            )

        val r =
            baseSize *
                0.16f *
                motion.scale

        canvas.drawCircle(
            cx,
            cy + baseSize * motion.translateYFraction,
            r,
            fill,
        )
    }

    private fun follow(
        current: Float,
        target: Float,
        dt: Float,
        attackHz: Float,
        releaseHz: Float,
    ): Float {
        val rate =
            if (target > current) attackHz
            else releaseHz

        val factor =
            (dt * rate).coerceIn(0f, 1f)

        return current +
            (target - current) * factor
    }
}
