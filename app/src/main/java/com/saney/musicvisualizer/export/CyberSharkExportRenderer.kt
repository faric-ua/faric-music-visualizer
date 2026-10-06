package com.saney.musicvisualizer.export

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
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardAudioState
import com.saney.musicvisualizer.board.BoardGroupReaction
import com.saney.musicvisualizer.board.BoardLayerId
import com.saney.musicvisualizer.board.BoardLayerMotion
import com.saney.musicvisualizer.board.BoardLayerMotionEvaluator
import com.saney.musicvisualizer.board.BoardLayerReaction
import com.saney.musicvisualizer.board.BoardLayerTransform
import com.saney.musicvisualizer.board.BoardTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class CyberSharkExportConfig(
    val groupTransform: BoardTransform,
    val groupReaction: BoardGroupReaction,
    val layerTransforms: Map<BoardLayerId, BoardLayerTransform>,
    val visibility: Map<BoardLayerId, Boolean>,
)

class CyberSharkExportRenderer(
    context: Context,
) {
    private val resources = context.resources
    private val bitmapPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        )
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val path = Path()

    private val frameBitmap =
        decode(R.drawable.cyber_shark_frame)
    private val creatureBitmap =
        decode(R.drawable.cyber_shark_creature)
    private val wordmarkBitmap =
        decode(R.drawable.cyber_shark_wordmark)

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

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        config: CyberSharkExportConfig,
    ) {
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val minSide = min(w, h)
        val transform =
            config.groupTransform.sanitized()
        val reaction =
            config.groupReaction.sanitized()
        val timeSeconds =
            timeMs / 1000f
        val stereoPan =
            signal.stereoPan.coerceIn(-1f, 1f)

        val cx =
            w * transform.xFraction +
                w *
                    stereoPan *
                    reaction.stereoShiftFraction
        val cy =
            h * transform.yFraction +
                minSide *
                    reaction.bassFloatFraction *
                    signal.bass *
                    sin(
                        (
                            timeSeconds *
                                (2f + signal.bass * 2.2f)
                            ).toDouble(),
                    )
                        .toFloat()

        val rotationSway =
            reaction.rotationSwayDegrees *
                (
                    0.20f +
                        signal.mid * 0.55f +
                        signal.bass * 0.25f
                    ).coerceIn(0f, 1f) *
                sin(
                    (
                        timeSeconds *
                            (1.15f + signal.mid * 1.35f) +
                            stereoPan * 0.75f
                        ).toDouble(),
                )
                    .toFloat()

        val groupRotation =
            transform.rotationDegrees +
                rotationSway
        val baseSize =
            minSide *
                transform.sizeFraction

        val audio =
            BoardAudioState(
                amplitude = signal.amplitude,
                bass = signal.bass,
                mid = signal.mid,
                high = signal.high,
                beat = signal.beatStrength,
            )

        fun layerTransform(
            id: BoardLayerId,
        ): BoardLayerTransform =
            (
                config.layerTransforms[id]
                    ?: BoardLayerTransform.default()
                )
                .sanitized()

        if (
            config.visibility[
                BoardLayerId.BACKGROUND
            ] != false
        ) {
            drawBackground(
                canvas,
                w,
                h,
                cx,
                cy,
                minSide,
                timeSeconds,
                signal,
                transform,
                groupRotation,
                layerTransform(
                    BoardLayerId.BACKGROUND,
                ),
            )
        }

        if (
            config.visibility[
                BoardLayerId.FRAME
            ] != false
        ) {
            drawBitmapLayer(
                canvas,
                frameBitmap,
                BoardLayerMotionEvaluator
                    .evaluate(
                        frameReaction,
                        audio,
                        timeSeconds,
                    ),
                cx,
                cy,
                baseSize,
                w,
                h,
                transform,
                layerTransform(
                    BoardLayerId.FRAME,
                ),
                groupRotation,
            )
        }

        if (
            config.visibility[
                BoardLayerId.FX
            ] != false
        ) {
            drawFx(
                canvas,
                BoardLayerMotionEvaluator
                    .evaluate(
                        fxReaction,
                        audio,
                        timeSeconds,
                    ),
                cx,
                cy,
                baseSize,
                w,
                h,
                timeSeconds,
                signal,
                transform,
                layerTransform(
                    BoardLayerId.FX,
                ),
                groupRotation,
            )
        }

        if (
            config.visibility[
                BoardLayerId.CREATURE
            ] != false
        ) {
            drawBitmapLayer(
                canvas,
                creatureBitmap,
                BoardLayerMotionEvaluator
                    .evaluate(
                        creatureReaction,
                        audio,
                        timeSeconds,
                    ),
                cx,
                cy,
                baseSize,
                w,
                h,
                transform,
                layerTransform(
                    BoardLayerId.CREATURE,
                ),
                groupRotation,
            )
        }

        if (
            config.visibility[
                BoardLayerId.WORDMARK
            ] != false
        ) {
            drawBitmapLayer(
                canvas,
                wordmarkBitmap,
                BoardLayerMotionEvaluator
                    .evaluate(
                        wordmarkReaction,
                        audio,
                        timeSeconds,
                    ),
                cx,
                cy,
                baseSize,
                w,
                h,
                transform,
                layerTransform(
                    BoardLayerId.WORDMARK,
                ),
                groupRotation,
            )
        }

        bitmapPaint.alpha = 255
    }

    private fun drawBitmapLayer(
        canvas: Canvas,
        bitmap: Bitmap?,
        motion: BoardLayerMotion,
        cx: Float,
        cy: Float,
        baseSize: Float,
        width: Float,
        height: Float,
        transform: BoardTransform,
        layerTransform: BoardLayerTransform,
        groupRotation: Float,
    ) {
        bitmap ?: return

        bitmapPaint.alpha =
            (
                motion.alpha *
                    transform.opacity *
                    layerTransform.opacity *
                    255f
                )
                .toInt()
                .coerceIn(0, 255)

        val size =
            baseSize *
                motion.scale *
                layerTransform.scale
        val layerCx =
            cx +
                width *
                    layerTransform.offsetXFraction
        val layerCy =
            cy +
                height *
                    layerTransform.offsetYFraction +
                baseSize *
                    motion.translateYFraction

        canvas.save()
        canvas.rotate(
            groupRotation +
                layerTransform.rotationDegrees +
                motion.rotationDegrees,
            layerCx,
            layerCy,
        )
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                layerCx - size * 0.5f,
                layerCy - size * 0.5f,
                layerCx + size * 0.5f,
                layerCy + size * 0.5f,
            ),
            bitmapPaint,
        )
        canvas.restore()
    }

    private fun drawFx(
        canvas: Canvas,
        motion: BoardLayerMotion,
        cx: Float,
        cy: Float,
        baseSize: Float,
        width: Float,
        height: Float,
        timeSeconds: Float,
        signal: SceneSignal,
        transform: BoardTransform,
        layerTransform: BoardLayerTransform,
        groupRotation: Float,
    ) {
        val size =
            baseSize *
                motion.scale *
                layerTransform.scale
        val radius =
            size * 0.43f
        val layerCx =
            cx +
                width *
                    layerTransform.offsetXFraction
        val layerCy =
            cy +
                height *
                    layerTransform.offsetYFraction

        canvas.save()
        canvas.rotate(
            groupRotation +
                layerTransform.rotationDegrees +
                motion.rotationDegrees,
            layerCx,
            layerCy,
        )

        repeat(3) { ring ->
            val phase =
                timeSeconds *
                    (0.8f + ring * 0.16f)

            path.reset()
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
                            )
                                .toFloat() +
                        signal.high *
                            0.025f *
                            sin(
                                a * 13.0 -
                                    phase * 1.8,
                            )
                                .toFloat()

                val r =
                    radius *
                        (
                            0.78f +
                                ring * 0.095f
                            ) *
                        wave

                val x =
                    layerCx +
                        cos(a).toFloat() *
                            r
                val y =
                    layerCy +
                        sin(a).toFloat() *
                            r *
                            0.88f

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            stroke.color =
                if (ring == 1) {
                    Color.rgb(
                        122,
                        224,
                        255,
                    )
                } else {
                    Color.rgb(
                        0,
                        207,
                        255,
                    )
                }
            stroke.alpha =
                (
                    motion.alpha *
                        transform.opacity *
                        layerTransform.opacity *
                        (165 - ring * 28)
                    )
                    .toInt()
                    .coerceIn(0, 230)
            stroke.strokeWidth =
                min(width, height) *
                    (
                        0.0032f +
                            signal.high * 0.0035f +
                            signal.beatStrength * 0.0045f
                        )

            canvas.drawPath(
                path,
                stroke,
            )
        }

        repeat(18) { index ->
            val a =
                index /
                    18f *
                    (PI * 2.0) +
                    timeSeconds *
                    (
                        0.09 +
                            (index % 3) *
                                0.018
                        )
            val r =
                radius *
                    (
                        0.68f +
                            (index % 5) *
                                0.065f +
                            signal.beatStrength *
                                0.04f
                        )

            fill.color =
                Color.argb(
                    (
                        motion.alpha *
                            transform.opacity *
                            layerTransform.opacity *
                            (
                                80 +
                                    signal.high * 130f
                                )
                        )
                        .toInt()
                        .coerceIn(0, 210),
                    80,
                    223,
                    255,
                )

            canvas.drawCircle(
                layerCx +
                    cos(a).toFloat() *
                        r,
                layerCy +
                    sin(a).toFloat() *
                        r *
                        0.88f,
                min(width, height) *
                    (
                        0.0025f +
                            (index % 4) *
                                0.0011f +
                            signal.high *
                                0.0025f
                        ),
                fill,
            )
        }

        canvas.restore()
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        minSide: Float,
        timeSeconds: Float,
        signal: SceneSignal,
        transform: BoardTransform,
        groupRotation: Float,
        layerTransform: BoardLayerTransform,
    ) {
        val layerCx =
            cx +
                w *
                    layerTransform.offsetXFraction
        val layerCy =
            cy +
                h *
                    layerTransform.offsetYFraction

        val layerAlpha =
            (
                255f *
                    transform.opacity *
                    layerTransform.opacity
                )
                .toInt()
                .coerceIn(0, 255)

        val saveCount =
            if (
                layerAlpha >=
                    255
            ) {
                // Avoid allocating/compositing a full-screen offscreen layer
                // when opacity is already fully opaque.
                canvas.save()
            } else {
                canvas.saveLayerAlpha(
                    0f,
                    0f,
                    w,
                    h,
                    layerAlpha,
                )
            }

        canvas.translate(
            layerCx - cx,
            layerCy - cy,
        )
        canvas.rotate(
            groupRotation +
                layerTransform.rotationDegrees,
            cx,
            cy,
        )
        canvas.scale(
            layerTransform.scale,
            layerTransform.scale,
            cx,
            cy,
        )

        fill.shader =
            RadialGradient(
                cx,
                cy,
                minSide *
                    (
                        0.62f +
                            signal.bass * 0.07f
                        ),
                intArrayOf(
                    Color.argb(
                        (
                            75 +
                                signal.bass * 70f +
                                signal.beatStrength * 45f
                            )
                            .toInt()
                            .coerceIn(0, 190),
                        0,
                        136,
                        255,
                    ),
                    Color.argb(
                        (
                            28 +
                                signal.high * 35f
                            )
                            .toInt()
                            .coerceIn(0, 100),
                        0,
                        229,
                        255,
                    ),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(
                    0f,
                    0.46f,
                    1f,
                ),
                Shader.TileMode.CLAMP,
            )
        canvas.drawCircle(
            cx,
            cy,
            minSide * 0.69f,
            fill,
        )
        fill.shader = null

        repeat(3) { index ->
            val inset =
                minSide *
                    (
                        0.49f +
                            index * 0.035f
                        )
            val rect =
                RectF(
                    cx - inset,
                    cy - inset,
                    cx + inset,
                    cy + inset,
                )
            stroke.strokeWidth =
                minSide *
                    (
                        0.004f +
                            index * 0.0012f
                        )
            stroke.color =
                if (index == 1) {
                    Color.argb(
                        (
                            70 +
                                signal.bass * 70f
                            )
                            .toInt()
                            .coerceIn(45, 150),
                        255,
                        139,
                        18,
                    )
                } else {
                    Color.argb(
                        (
                            70 +
                                signal.high * 90f +
                                signal.beatStrength * 35f
                            )
                            .toInt()
                            .coerceIn(45, 175),
                        16,
                        207,
                        245,
                    )
                }

            canvas.drawArc(
                rect,
                (
                    timeSeconds *
                        (12f + index * 5f) +
                        index * 73f
                    ) % 360f,
                42f + index * 18f,
                false,
                stroke,
            )
            canvas.drawArc(
                rect,
                (
                    190f -
                        timeSeconds *
                            (8f + index * 4f) +
                        index * 41f
                    ) % 360f,
                28f + index * 14f,
                false,
                stroke,
            )
        }

        repeat(34) { index ->
            val angle =
                index *
                    2.3999632f +
                    timeSeconds *
                        (
                            0.055f +
                                (index % 5) *
                                    0.008f
                            )
            val radius =
                minSide *
                    (
                        0.31f +
                            (index % 9) *
                                0.036f
                        )

            fill.color =
                if (index % 7 == 0) {
                    Color.argb(
                        65,
                        255,
                        139,
                        18,
                    )
                } else {
                    Color.argb(
                        (
                            35 +
                                signal.high * 55f
                            )
                            .toInt()
                            .coerceIn(28, 95),
                        17,
                        201,
                        245,
                    )
                }

            canvas.drawCircle(
                cx +
                    cos(
                        angle.toDouble(),
                    )
                        .toFloat() *
                        radius,
                cy +
                    sin(
                        angle.toDouble(),
                    )
                        .toFloat() *
                        radius *
                        0.82f,
                minSide *
                    (
                        0.0025f +
                            (index % 3) *
                                0.0014f
                        ),
                fill,
            )
        }

        canvas.restoreToCount(
            saveCount,
        )
    }

    private fun decode(
        drawable: Int,
    ): Bitmap? =
        runCatching {
            BitmapFactory.decodeResource(
                resources,
                drawable,
                BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig =
                        Bitmap.Config.ARGB_8888
                },
            )
        }.getOrNull()
}
