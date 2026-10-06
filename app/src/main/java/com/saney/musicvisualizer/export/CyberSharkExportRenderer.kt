package com.saney.musicvisualizer.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
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
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

data class CyberSharkExportConfig(
    val groupTransform: BoardTransform,
    val groupReaction: BoardGroupReaction,
    val layerTransforms: Map<BoardLayerId, BoardLayerTransform>,
    val visibility: Map<BoardLayerId, Boolean>,
)

data class CyberSharkGpuGlow(
    val centerX: Float,
    val centerY: Float,
    val circleRadius: Float,
    val gradientRadius: Float,
    val centerAlpha: Float,
    val midAlpha: Float,
)

data class CyberSharkGpuFrame(
    val bitmap: Bitmap,
    val centerX: Float,
    val centerY: Float,
    val size: Float,
    val rotationDegrees: Float,
    val alpha: Float,
)

data class CyberSharkStageTiming(
    val backgroundMs: Long,
    val backgroundSetupMs: Long,
    val backgroundGlowMs: Long,
    val backgroundGlowRenderMs: Long,
    val backgroundGlowCompositeMs: Long,
    val backgroundArcsMs: Long,
    val backgroundParticlesMs: Long,
    val backgroundRestoreMs: Long,
    val frameMs: Long,
    val fxMs: Long,
    val creatureMs: Long,
    val wordmarkMs: Long,
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

    // The Cyber Shark background glow is a smooth radial field. Rendering the
    // full ~1490 px diameter software RadialGradient directly into every
    // 1080x1920 export frame is disproportionately expensive. Keep final
    // output geometry unchanged, but rasterize the smooth field into a
    // reusable 512x512 texture and composite it at final size. v0.19.16
    // intentionally avoids FILTER_BITMAP_FLAG because phone profiling showed
    // filtered software scaling dominated the glow cost.
    private val glowBitmap =
        Bitmap.createBitmap(
            GLOW_CACHE_SIZE,
            GLOW_CACHE_SIZE,
            Bitmap.Config.ARGB_8888,
        )
    private val glowCanvas =
        Canvas(
            glowBitmap,
        )
    private val glowFill =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        )
    private val glowBitmapPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG,
        )

    private var backgroundNs =
        0L
    private var backgroundSetupNs =
        0L
    private var backgroundGlowNs =
        0L
    private var backgroundGlowRenderNs =
        0L
    private var backgroundGlowCompositeNs =
        0L
    private var backgroundArcsNs =
        0L
    private var backgroundParticlesNs =
        0L
    private var backgroundRestoreNs =
        0L
    private var frameNs =
        0L
    private var fxNs =
        0L
    private var creatureNs =
        0L
    private var wordmarkNs =
        0L

    private var latestGpuGlow:
        CyberSharkGpuGlow? =
        null

    fun gpuGlowSnapshot():
        CyberSharkGpuGlow? =
        latestGpuGlow

    fun timingSnapshot():
        CyberSharkStageTiming =
        CyberSharkStageTiming(
            backgroundMs =
                backgroundNs /
                    1_000_000L,
            backgroundSetupMs =
                backgroundSetupNs /
                    1_000_000L,
            backgroundGlowMs =
                backgroundGlowNs /
                    1_000_000L,
            backgroundGlowRenderMs =
                backgroundGlowRenderNs /
                    1_000_000L,
            backgroundGlowCompositeMs =
                backgroundGlowCompositeNs /
                    1_000_000L,
            backgroundArcsMs =
                backgroundArcsNs /
                    1_000_000L,
            backgroundParticlesMs =
                backgroundParticlesNs /
                    1_000_000L,
            backgroundRestoreMs =
                backgroundRestoreNs /
                    1_000_000L,
            frameMs =
                frameNs /
                    1_000_000L,
            fxMs =
                fxNs /
                    1_000_000L,
            creatureMs =
                creatureNs /
                    1_000_000L,
            wordmarkMs =
                wordmarkNs /
                    1_000_000L,
        )

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

    fun gpuFrame(
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        config: CyberSharkExportConfig,
    ): CyberSharkGpuFrame? {
        val bitmap =
            frameBitmap
                ?: return null

        if (
            width <= 0 ||
            height <= 0 ||
            config.visibility[
                BoardLayerId.FRAME
            ] == false
        ) {
            return null
        }

        val w =
            width.toFloat()
        val h =
            height.toFloat()
        val minSide =
            min(
                w,
                h,
            )
        val transform =
            config
                .groupTransform
                .sanitized()
        val reaction =
            config
                .groupReaction
                .sanitized()
        val timeSeconds =
            timeMs /
                1000f
        val stereoPan =
            signal.stereoPan
                .coerceIn(
                    -1f,
                    1f,
                )
        val cx =
            w *
                transform.xFraction +
                w *
                    stereoPan *
                    reaction
                        .stereoShiftFraction
        val cy =
            h *
                transform.yFraction +
                minSide *
                    reaction
                        .bassFloatFraction *
                    signal.bass *
                    sin(
                        (
                            timeSeconds *
                                (
                                    2f +
                                        signal.bass *
                                            2.2f
                                    )
                            ).toDouble(),
                    )
                        .toFloat()
        val rotationSway =
            reaction
                .rotationSwayDegrees *
                (
                    0.20f +
                        signal.mid * 0.55f +
                        signal.bass * 0.25f
                    )
                    .coerceIn(
                        0f,
                        1f,
                    ) *
                sin(
                    (
                        timeSeconds *
                            (
                                1.15f +
                                    signal.mid *
                                        1.35f
                                ) +
                            stereoPan *
                                0.75f
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
                amplitude =
                    signal.amplitude,
                bass =
                    signal.bass,
                mid =
                    signal.mid,
                high =
                    signal.high,
                beat =
                    signal.beatStrength,
            )
        val motion =
            BoardLayerMotionEvaluator
                .evaluate(
                    frameReaction,
                    audio,
                    timeSeconds,
                )
        val layerTransform =
            (
                config
                    .layerTransforms[
                        BoardLayerId.FRAME
                    ]
                    ?: BoardLayerTransform
                        .default()
                )
                .sanitized()
        val size =
            baseSize *
                motion.scale *
                layerTransform.scale
        val layerCx =
            cx +
                w *
                    layerTransform
                        .offsetXFraction
        val layerCy =
            cy +
                h *
                    layerTransform
                        .offsetYFraction +
                baseSize *
                    motion
                        .translateYFraction
        val alpha =
            (
                motion.alpha *
                    transform.opacity *
                    layerTransform.opacity
                )
                .coerceIn(
                    0f,
                    1f,
                )

        return CyberSharkGpuFrame(
            bitmap = bitmap,
            centerX = layerCx,
            centerY = layerCy,
            size = size,
            rotationDegrees =
                groupRotation +
                    layerTransform
                        .rotationDegrees +
                    motion
                        .rotationDegrees,
            alpha = alpha,
        )
    }

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        signal: SceneSignal,
        config: CyberSharkExportConfig,
        skipBackgroundGlow:
            Boolean = false,
    ) {
        latestGpuGlow =
            null

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
            val startedNs =
                System.nanoTime()

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
                skipBackgroundGlow,
            )

            backgroundNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config.visibility[
                BoardLayerId.FRAME
            ] != false
        ) {
            val startedNs =
                System.nanoTime()

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

            frameNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config.visibility[
                BoardLayerId.FX
            ] != false
        ) {
            val startedNs =
                System.nanoTime()

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

            fxNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config.visibility[
                BoardLayerId.CREATURE
            ] != false
        ) {
            val startedNs =
                System.nanoTime()

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

            creatureNs +=
                System.nanoTime() -
                    startedNs
        }

        if (
            config.visibility[
                BoardLayerId.WORDMARK
            ] != false
        ) {
            val startedNs =
                System.nanoTime()

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

            wordmarkNs +=
                System.nanoTime() -
                    startedNs
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
        skipGlow: Boolean,
    ) {
        val setupStartedNs =
            System.nanoTime()

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

        backgroundSetupNs +=
            System.nanoTime() -
                setupStartedNs

        val gpuCircleRadius =
            minSide *
                0.69f
        val gpuGradientRadius =
            minSide *
                (
                    0.62f +
                        signal.bass * 0.07f
                    )

        val glowPoints =
            floatArrayOf(
                cx,
                cy,
                cx + gpuCircleRadius,
                cy,
            )
        val glowMatrix =
            Matrix()
        canvas.getMatrix(
            glowMatrix,
        )
        glowMatrix.mapPoints(
            glowPoints,
        )

        val mappedCircleRadius =
            hypot(
                (
                    glowPoints[2] -
                        glowPoints[0]
                    ).toDouble(),
                (
                    glowPoints[3] -
                        glowPoints[1]
                    ).toDouble(),
            ).toFloat()
        val mappedGradientRadius =
            mappedCircleRadius *
                (
                    gpuGradientRadius /
                        gpuCircleRadius
                    )
        val layerOpacity =
            layerAlpha /
                255f

        latestGpuGlow =
            CyberSharkGpuGlow(
                centerX =
                    glowPoints[0],
                centerY =
                    glowPoints[1],
                circleRadius =
                    mappedCircleRadius,
                gradientRadius =
                    mappedGradientRadius,
                centerAlpha =
                    (
                        (
                            75f +
                                signal.bass * 70f +
                                signal.beatStrength * 45f
                            )
                            .coerceIn(
                                0f,
                                190f,
                            ) /
                            255f
                        ) *
                        layerOpacity,
                midAlpha =
                    (
                        (
                            28f +
                                signal.high * 35f
                            )
                            .coerceIn(
                                0f,
                                100f,
                            ) /
                            255f
                        ) *
                        layerOpacity,
            )

        val glowStartedNs =
            System.nanoTime()

        if (!skipGlow) {
            val glowRenderStartedNs =
                glowStartedNs

        glowCanvas.drawColor(
            Color.TRANSPARENT,
            PorterDuff.Mode.CLEAR,
        )

        val glowHalf =
            GLOW_CACHE_SIZE *
                0.5f
        val outputGlowRadius =
            minSide *
                0.69f
        val gradientRadiusRatio =
            (
                0.62f +
                    signal.bass * 0.07f
                ) /
                0.69f

        glowFill.shader =
            RadialGradient(
                glowHalf,
                glowHalf,
                glowHalf *
                    gradientRadiusRatio,
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
        glowCanvas.drawCircle(
            glowHalf,
            glowHalf,
            glowHalf,
            glowFill,
        )
        glowFill.shader =
            null

        backgroundGlowRenderNs +=
            System.nanoTime() -
                glowRenderStartedNs

        val glowCompositeStartedNs =
            System.nanoTime()

        canvas.drawBitmap(
            glowBitmap,
            null,
            RectF(
                cx -
                    outputGlowRadius,
                cy -
                    outputGlowRadius,
                cx +
                    outputGlowRadius,
                cy +
                    outputGlowRadius,
            ),
            glowBitmapPaint,
        )

        backgroundGlowCompositeNs +=
            System.nanoTime() -
                glowCompositeStartedNs

            backgroundGlowNs +=
                System.nanoTime() -
                    glowStartedNs
        }

        val arcsStartedNs =
            System.nanoTime()

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

        backgroundArcsNs +=
            System.nanoTime() -
                arcsStartedNs

        val particlesStartedNs =
            System.nanoTime()

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

        backgroundParticlesNs +=
            System.nanoTime() -
                particlesStartedNs

        val restoreStartedNs =
            System.nanoTime()

        canvas.restoreToCount(
            saveCount,
        )

        backgroundRestoreNs +=
            System.nanoTime() -
                restoreStartedNs
    }

    companion object {
        private const val GLOW_CACHE_SIZE =
            512
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
