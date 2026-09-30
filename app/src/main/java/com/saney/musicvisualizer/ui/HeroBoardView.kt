package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import androidx.annotation.DrawableRes
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardAudioState
import com.saney.musicvisualizer.board.BoardLayerMotionEvaluator
import com.saney.musicvisualizer.board.BoardLayerReaction
import kotlin.math.max
import kotlin.math.min

/**
 * First real layered Board Hero/GF renderer.
 *
 * Cyber Shark is intentionally rendered as four independent transparent layers:
 * frame -> FX -> creature -> wordmark.
 *
 * Every layer consumes the same SceneSignal but uses a different reaction map.
 * This is the proof for the generic Board model described in
 * docs/architecture/FARIC_LAYERED_BOARD_VISION.md.
 */
class HeroBoardView(context: Context) : View(context) {
    private data class Layer(
        @DrawableRes val drawableRes: Int,
        val reaction: BoardLayerReaction,
    )

    private val bitmapPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    private val layers =
        listOf(
            Layer(
                drawableRes = R.drawable.cyber_shark_frame,
                reaction =
                    BoardLayerReaction(
                        baseScale = 0.92f,
                        bassScale = 0.055f,
                        amplitudeScale = 0.012f,
                        beatScale = 0.018f,
                        baseAlpha = 0.98f,
                        rotationDegPerSecond = 1.15f,
                        beatRotationDeg = 0.8f,
                    ),
            ),
            Layer(
                drawableRes = R.drawable.cyber_shark_fx,
                reaction =
                    BoardLayerReaction(
                        baseScale = 0.94f,
                        bassScale = 0.020f,
                        highScale = 0.045f,
                        beatScale = 0.050f,
                        baseAlpha = 0.20f,
                        highAlpha = 0.42f,
                        beatAlpha = 0.30f,
                        rotationDegPerSecond = -0.65f,
                        beatRotationDeg = -1.5f,
                    ),
            ),
            Layer(
                drawableRes = R.drawable.cyber_shark_creature,
                reaction =
                    BoardLayerReaction(
                        baseScale = 0.94f,
                        amplitudeScale = 0.010f,
                        bassScale = 0.025f,
                        midScale = 0.010f,
                        beatScale = 0.070f,
                        baseAlpha = 1f,
                        beatTranslateYFraction = -0.010f,
                    ),
            ),
            Layer(
                drawableRes = R.drawable.cyber_shark_wordmark,
                reaction =
                    BoardLayerReaction(
                        baseScale = 0.94f,
                        bassScale = 0.010f,
                        highScale = 0.010f,
                        beatScale = 0.035f,
                        baseAlpha = 1f,
                        highAlpha = 0f,
                        beatTranslateYFraction = 0.004f,
                    ),
            ),
        )

    private val bitmaps: List<Bitmap> =
        layers.map { layer ->
            requireNotNull(
                BitmapFactory.decodeResource(
                    resources,
                    layer.drawableRes,
                ),
            )
        }

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

        val baseSize = minSide * 0.92f

        layers.forEachIndexed { index, layer ->
            val bitmap = bitmaps[index]
            val motion =
                BoardLayerMotionEvaluator.evaluate(
                    reaction = layer.reaction,
                    audio = audio,
                    timeSeconds = timeSeconds,
                )

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
                android.graphics.RectF(
                    cx - size * 0.5f,
                    layerCy - size * 0.5f,
                    cx + size * 0.5f,
                    layerCy + size * 0.5f,
                ),
                bitmapPaint,
            )
            canvas.restore()
        }

        bitmapPaint.alpha = 255
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Resource bitmaps are immutable and scoped to this short-lived view.
        // Do not recycle here: Canvas can still hold display-list references.
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
