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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
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
import kotlin.math.atan2
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
 * Manual Board transforms establish the base pose. Audio reactions are then
 * added on top of that pose, so layout editing never replaces the existing
 * per-layer music response.
 */
class HeroBoardView(context: Context) : View(context) {

    enum class ObjectId {
        BACKGROUND,
        FRAME,
        FX,
        CREATURE,
        WORDMARK,
    }

    private val objectVisibility =
        ObjectId.entries
            .associateWith { true }
            .toMutableMap()

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

    private var groupTransform =
        BoardTransform.default()

    private var groupReaction =
        BoardGroupReaction.default()

    private var presentationScale = 1f
    private var presentationYOffsetFraction = 0f

    private val layerTransforms =
        BoardLayerId.entries
            .associateWith {
                BoardLayerTransform.default()
            }
            .toMutableMap()

    private var gesturesEnabled = false
    private var gestureLayerId:
        BoardLayerId? = null
    private var gestureTransformListener:
        ((BoardTransform) -> Unit)? = null
    private var gestureLayerTransformListener:
        ((BoardLayerId, BoardLayerTransform) -> Unit)? = null

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastRotationAngle = 0f
    private var rotatingGesture = false

    private val scaleGestureDetector =
        ScaleGestureDetector(
            context,
            object :
                ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(
                    detector: ScaleGestureDetector,
                ): Boolean {
                    if (!gesturesEnabled) {
                        return false
                    }

                    val layerId =
                        gestureLayerId

                    if (layerId == null) {
                        updateTransformFromGesture(
                            groupTransform.copy(
                                sizeFraction =
                                    groupTransform.sizeFraction *
                                        detector.scaleFactor,
                            ),
                        )
                    } else {
                        val current =
                            layerTransform(layerId)

                        updateLayerTransformFromGesture(
                            layerId,
                            current.copy(
                                scale =
                                    current.scale *
                                        detector.scaleFactor,
                            ),
                        )
                    }
                    return true
                }
            },
        )

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
    private var targetStereoPan = 0f

    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f
    private var stereoPan = 0f

    private var playing = false
    private var lastFrameMs =
        SystemClock.elapsedRealtime()

    fun setPlaying(value: Boolean) {
        playing = value
        postInvalidateOnAnimation()
    }

    fun setObjectVisible(
        objectId: ObjectId,
        visible: Boolean,
    ) {
        objectVisibility[objectId] = visible
        postInvalidateOnAnimation()
    }

    fun isObjectVisible(
        objectId: ObjectId,
    ): Boolean = objectVisibility[objectId] != false

    fun setGroupTransform(
        value: BoardTransform,
    ) {
        groupTransform = value.sanitized()
        postInvalidateOnAnimation()
    }

    fun setGroupReaction(
        value: BoardGroupReaction,
    ) {
        groupReaction = value.sanitized()
        postInvalidateOnAnimation()
    }

    /**
     * Visual-only shell tuning for Now Playing.
     *
     * This never mutates the persisted Board transform. It lets the permanent
     * PulseDeck shell give the swappable Hero/GF a larger standardized slot,
     * while the Board editor continues to work with the user's real values.
     */
    fun setPresentationTuning(
        scale: Float = 1f,
        yOffsetFraction: Float = 0f,
    ) {
        presentationScale =
            scale.coerceIn(
                0.80f,
                1.45f,
            )
        presentationYOffsetFraction =
            yOffsetFraction.coerceIn(
                -0.10f,
                0.10f,
            )
        postInvalidateOnAnimation()
    }

    fun setLayerTransforms(
        values:
            Map<
                BoardLayerId,
                BoardLayerTransform,
            >,
    ) {
        BoardLayerId.entries
            .forEach { layerId ->
                layerTransforms[layerId] =
                    (
                        values[layerId]
                            ?: BoardLayerTransform.default()
                        )
                        .sanitized()
            }

        postInvalidateOnAnimation()
    }

    fun setLayerTransform(
        layerId: BoardLayerId,
        value: BoardLayerTransform,
    ) {
        layerTransforms[layerId] =
            value.sanitized()
        postInvalidateOnAnimation()
    }

    fun setGestureEditing(
        enabled: Boolean,
        layerId: BoardLayerId? = null,
        onTransformChanged:
            ((BoardTransform) -> Unit)? = null,
        onLayerTransformChanged:
            ((
                BoardLayerId,
                BoardLayerTransform,
            ) -> Unit)? = null,
    ) {
        gesturesEnabled = enabled
        gestureLayerId =
            if (enabled) {
                layerId
            } else {
                null
            }
        gestureTransformListener =
            if (enabled) {
                onTransformChanged
            } else {
                null
            }
        gestureLayerTransformListener =
            if (enabled) {
                onLayerTransformChanged
            } else {
                null
            }

        isClickable = enabled
    }

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high
        targetStereoPan =
            signal.stereoPan.coerceIn(
                -1f,
                1f,
            )

        amplitude =
            max(
                amplitude,
                signal.amplitude * 0.96f,
            )
        bass =
            max(
                bass,
                signal.bass * 0.98f,
            )
        mid =
            max(
                mid,
                signal.mid * 0.96f,
            )
        high =
            max(
                high,
                signal.high * 0.96f,
            )
        beat =
            max(
                beat,
                signal.beatStrength,
            )

        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(
        event: MotionEvent,
    ): Boolean {
        if (!gesturesEnabled) {
            return super.onTouchEvent(event)
        }

        parent?.requestDisallowInterceptTouchEvent(
            true,
        )

        scaleGestureDetector.onTouchEvent(
            event,
        )

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                rotatingGesture = false
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastRotationAngle =
                        pointerAngle(event)
                    rotatingGesture = true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (
                    event.pointerCount >= 2 &&
                    rotatingGesture
                ) {
                    val angle =
                        pointerAngle(event)

                    val delta =
                        normalizeDegrees(
                            angle -
                                lastRotationAngle,
                        )

                    lastRotationAngle = angle

                    val layerId =
                        gestureLayerId

                    if (layerId == null) {
                        updateTransformFromGesture(
                            groupTransform.copy(
                                rotationDegrees =
                                    groupTransform
                                        .rotationDegrees +
                                        delta,
                            ),
                        )
                    } else {
                        val current =
                            layerTransform(layerId)

                        updateLayerTransformFromGesture(
                            layerId,
                            current.copy(
                                rotationDegrees =
                                    current.rotationDegrees +
                                        delta,
                            ),
                        )
                    }
                } else if (
                    event.pointerCount == 1 &&
                    !scaleGestureDetector
                        .isInProgress
                ) {
                    val dx =
                        event.x -
                            lastTouchX
                    val dy =
                        event.y -
                            lastTouchY

                    lastTouchX = event.x
                    lastTouchY = event.y

                    if (
                        width > 0 &&
                        height > 0
                    ) {
                        val layerId =
                            gestureLayerId

                        if (layerId == null) {
                            updateTransformFromGesture(
                                groupTransform.copy(
                                    xFraction =
                                        groupTransform
                                            .xFraction +
                                            dx /
                                            width,
                                    yFraction =
                                        groupTransform
                                            .yFraction +
                                            dy /
                                            height,
                                ),
                            )
                        } else {
                            val current =
                                layerTransform(layerId)

                            updateLayerTransformFromGesture(
                                layerId,
                                current.copy(
                                    offsetXFraction =
                                        current
                                            .offsetXFraction +
                                            dx /
                                            width,
                                    offsetYFraction =
                                        current
                                            .offsetYFraction +
                                            dy /
                                            height,
                                ),
                            )
                        }
                    }
                }

                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                rotatingGesture = false
                if (event.pointerCount >= 2) {
                    lastTouchX =
                        event.getX(0)
                    lastTouchY =
                        event.getY(0)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                rotatingGesture = false
                parent?.requestDisallowInterceptTouchEvent(
                    false,
                )
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                rotatingGesture = false
                parent?.requestDisallowInterceptTouchEvent(
                    false,
                )
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now =
            SystemClock.elapsedRealtime()
        val dt =
            (
                (now - lastFrameMs)
                    .coerceIn(
                        1L,
                        50L,
                    ) /
                    1000f
                )
        lastFrameMs = now

        amplitude =
            follow(
                amplitude,
                targetAmplitude,
                dt,
                58f,
                12f,
            )
        bass =
            follow(
                bass,
                targetBass,
                dt,
                74f,
                15f,
            )
        mid =
            follow(
                mid,
                targetMid,
                dt,
                54f,
                13f,
            )
        high =
            follow(
                high,
                targetHigh,
                dt,
                62f,
                14f,
            )
        stereoPan =
            follow(
                stereoPan,
                targetStereoPan,
                dt,
                18f,
                8f,
            )

        beat =
            (
                beat -
                    dt * 3.4f
                )
                .coerceAtLeast(
                    0f,
                )

        if (!playing) {
            targetAmplitude *= 0.90f
            targetBass *= 0.90f
            targetMid *= 0.90f
            targetHigh *= 0.90f
            targetStereoPan *= 0.88f
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        val minSide =
            min(w, h)

        val transform =
            groupTransform

        val reaction =
            groupReaction

        val timeSeconds =
            now / 1000f

        val stereoOffsetX =
            w *
                stereoPan *
                reaction
                    .stereoShiftFraction

        val bassFloatPhase =
            timeSeconds *
                (
                    2.0f +
                        bass * 2.2f
                    )

        val bassFloatY =
            minSide *
                reaction
                    .bassFloatFraction *
                bass *
                sin(
                    bassFloatPhase
                        .toDouble(),
                )
                    .toFloat()

        val rotationSwayPhase =
            timeSeconds *
                (
                    1.15f +
                        mid * 1.35f
                    ) +
                stereoPan * 0.75f

        val rotationSway =
            reaction
                .rotationSwayDegrees *
                (
                    0.20f +
                        mid * 0.55f +
                        bass * 0.25f
                    )
                    .coerceIn(
                        0f,
                        1f,
                    ) *
                sin(
                    rotationSwayPhase
                        .toDouble(),
                )
                    .toFloat()

        val cx =
            w *
                transform.xFraction +
                stereoOffsetX

        val cy =
            h *
                (
                    transform.yFraction +
                        presentationYOffsetFraction
                    ) +
                bassFloatY

        val groupRotationDegrees =
            transform.rotationDegrees +
                rotationSway

        val baseSize =
            minSide *
                transform.sizeFraction *
                presentationScale

        if (isObjectVisible(ObjectId.BACKGROUND)) {
            drawBoardBackground(
                canvas = canvas,
                w = w,
                h = h,
                cx = cx,
                cy = cy,
                minSide = minSide,
            )
        }

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
        if (isObjectVisible(ObjectId.FRAME)) {
            drawBitmapLayer(
                canvas = canvas,
                bitmap = frameBitmap,
                motion = frameMotion,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                transform = transform,
                layerTransform =
                    layerTransform(
                        BoardLayerId.FRAME,
                    ),
                groupRotationDegrees =
                    groupRotationDegrees,
            )
        }

        val fxMotion =
            BoardLayerMotionEvaluator.evaluate(
                fxReaction,
                audio,
                timeSeconds,
            )
        if (isObjectVisible(ObjectId.FX)) {
            drawFxLayer(
                canvas = canvas,
                motion = fxMotion,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                timeSeconds = timeSeconds,
                transform = transform,
                layerTransform =
                    layerTransform(
                        BoardLayerId.FX,
                    ),
                groupRotationDegrees =
                    groupRotationDegrees,
            )
        }

        val creatureMotion =
            BoardLayerMotionEvaluator.evaluate(
                creatureReaction,
                audio,
                timeSeconds,
            )
        if (isObjectVisible(ObjectId.CREATURE)) {
            drawBitmapLayer(
                canvas = canvas,
                bitmap = creatureBitmap,
                motion = creatureMotion,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                transform = transform,
                layerTransform =
                    layerTransform(
                        BoardLayerId.CREATURE,
                    ),
                groupRotationDegrees =
                    groupRotationDegrees,
            )
        }

        val wordmarkMotion =
            BoardLayerMotionEvaluator.evaluate(
                wordmarkReaction,
                audio,
                timeSeconds,
            )
        if (isObjectVisible(ObjectId.WORDMARK)) {
            drawBitmapLayer(
                canvas = canvas,
                bitmap = wordmarkBitmap,
                motion = wordmarkMotion,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                transform = transform,
                layerTransform =
                    layerTransform(
                        BoardLayerId.WORDMARK,
                    ),
                groupRotationDegrees =
                    groupRotationDegrees,
            )
        }

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
        transform: BoardTransform,
        layerTransform: BoardLayerTransform,
        groupRotationDegrees: Float,
    ) {
        if (bitmap == null) {
            drawMissingLayerFallback(
                canvas = canvas,
                cx = cx,
                cy = cy,
                baseSize = baseSize,
                motion = motion,
                transform = transform,
                layerTransform = layerTransform,
            )
            return
        }

        bitmapPaint.alpha =
            (
                motion.alpha *
                    transform.opacity *
                    layerTransform.opacity *
                    255f
                )
                .toInt()
                .coerceIn(
                    0,
                    255,
                )

        val size =
            baseSize *
                motion.scale *
                layerTransform.scale

        val layerCx =
            cx +
                width *
                layerTransform
                    .offsetXFraction

        val layerCy =
            cy +
                height *
                layerTransform
                    .offsetYFraction +
                baseSize *
                motion.translateYFraction

        canvas.save()
        canvas.rotate(
            groupRotationDegrees +
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

    private fun drawFxLayer(
        canvas: Canvas,
        motion: BoardLayerMotion,
        cx: Float,
        cy: Float,
        baseSize: Float,
        timeSeconds: Float,
        transform: BoardTransform,
        layerTransform: BoardLayerTransform,
        groupRotationDegrees: Float,
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
                layerTransform
                    .offsetXFraction
        val layerCy =
            cy +
                height *
                layerTransform
                    .offsetYFraction

        canvas.save()
        canvas.rotate(
            groupRotationDegrees +
                layerTransform.rotationDegrees +
                motion.rotationDegrees,
            layerCx,
            layerCy,
        )

        repeat(3) { ring ->
            val phase =
                timeSeconds *
                    (
                        0.8f +
                            ring * 0.16f
                        )

            fxPath.reset()
            val points = 96

            for (i in 0..points) {
                val a =
                    i.toFloat() /
                        points *
                        (
                            PI *
                                2.0
                            )

                val wave =
                    1f +
                        0.035f *
                        sin(
                            a *
                                (5 + ring) +
                                phase,
                        )
                            .toFloat() +
                        high *
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
                                ring *
                                0.095f
                            ) *
                        wave

                val x =
                    layerCx +
                        cos(a)
                            .toFloat() *
                        r

                val y =
                    layerCy +
                        sin(a)
                            .toFloat() *
                        r *
                        0.88f

                if (i == 0) {
                    fxPath.moveTo(
                        x,
                        y,
                    )
                } else {
                    fxPath.lineTo(
                        x,
                        y,
                    )
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
                        (
                            165 -
                                ring *
                                28
                            )
                    )
                    .toInt()
                    .coerceIn(
                        0,
                        230,
                    )

            stroke.strokeWidth =
                min(
                    width,
                    height,
                ) *
                    (
                        0.0032f +
                            high *
                            0.0035f +
                            beat *
                            0.0045f
                        )

            canvas.drawPath(
                fxPath,
                stroke,
            )
        }

        repeat(18) { i ->
            val a =
                i / 18f *
                    (
                        PI *
                            2.0
                        ) +
                    timeSeconds *
                    (
                        0.09 +
                            (
                                i %
                                    3
                                ) *
                            0.018
                        )

            val r =
                radius *
                    (
                        0.68f +
                            (
                                i %
                                    5
                                ) *
                            0.065f +
                            beat *
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
                                    high *
                                    130f
                                )
                        )
                        .toInt()
                        .coerceIn(
                            0,
                            210,
                        ),
                    80,
                    223,
                    255,
                )

            val dot =
                min(
                    width,
                    height,
                ) *
                    (
                        0.0025f +
                            (
                                i %
                                    4
                                ) *
                            0.0011f +
                            high *
                            0.0025f
                        )

            canvas.drawCircle(
                cx +
                    cos(a)
                        .toFloat() *
                    r,
                cy +
                    sin(a)
                        .toFloat() *
                    r *
                    0.88f,
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
        // Layer 3 must stay compositing-friendly: never paint an opaque
        // full-screen background over Layers 0–2. Keep only local glow/HUD
        // decoration around the GF object.
        fill.shader =
            RadialGradient(
                cx,
                cy,
                minSide *
                    (
                        0.62f +
                            bass *
                            0.07f
                        ),
                intArrayOf(
                    Color.argb(
                        (
                            75 +
                                bass *
                                70f +
                                beat *
                                45f
                            )
                            .toInt()
                            .coerceIn(
                                0,
                                190,
                            ),
                        0,
                        136,
                        255,
                    ),
                    Color.argb(
                        (
                            28 +
                                high *
                                35f
                            )
                            .toInt()
                            .coerceIn(
                                0,
                                100,
                            ),
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
            minSide *
                0.69f *
                presentationScale,
            fill,
        )
        fill.shader = null

        // Lightweight HUD energy around the hero. This gives the real app
        // some of the reference's cyan/orange activity without baking the
        // decoration into the swappable Hero/GF artwork.
        val timeSeconds =
            SystemClock.elapsedRealtime() /
                1000f

        stroke.style = Paint.Style.STROKE
        stroke.strokeCap = Paint.Cap.ROUND

        val arcRadius =
            minSide *
                0.49f *
                presentationScale

        repeat(3) { index ->
            val inset =
                arcRadius +
                    minSide *
                    index *
                    0.035f

            val arcRect =
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
                                bass *
                                70f
                            )
                            .toInt()
                            .coerceIn(
                                45,
                                150,
                            ),
                        255,
                        139,
                        18,
                    )
                } else {
                    Color.argb(
                        (
                            70 +
                                high *
                                90f +
                                beat *
                                35f
                            )
                            .toInt()
                            .coerceIn(
                                45,
                                175,
                            ),
                        16,
                        207,
                        245,
                    )
                }

            canvas.drawArc(
                arcRect,
                (
                    timeSeconds *
                        (
                            12f +
                                index * 5f
                            ) +
                        index * 73f
                    ) %
                    360f,
                42f +
                    index * 18f,
                false,
                stroke,
            )

            canvas.drawArc(
                arcRect,
                (
                    190f -
                        timeSeconds *
                        (
                            8f +
                                index * 4f
                            ) +
                        index * 41f
                    ) %
                    360f,
                28f +
                    index * 14f,
                false,
                stroke,
            )
        }

        fill.shader = null

        repeat(34) { index ->
            val angle =
                (
                    index *
                        2.3999632f +
                        timeSeconds *
                        (
                            0.055f +
                                (index % 5) *
                                0.008f
                            )
                    )

            val radius =
                minSide *
                    (
                        0.31f +
                            (index % 9) *
                            0.036f
                        ) *
                    presentationScale

            val px =
                cx +
                    cos(
                        angle.toDouble(),
                    )
                        .toFloat() *
                    radius

            val py =
                cy +
                    sin(
                        angle.toDouble(),
                    )
                        .toFloat() *
                    radius *
                    0.82f

            val orange =
                index % 7 == 0

            fill.color =
                if (orange) {
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
                                high *
                                55f
                            )
                            .toInt()
                            .coerceIn(
                                28,
                                95,
                            ),
                        17,
                        201,
                        245,
                    )
                }

            canvas.drawCircle(
                px,
                py,
                minSide *
                    (
                        0.0025f +
                            (index % 3) *
                            0.0014f
                        ),
                fill,
            )
        }
    }

    private fun decodeSafely(
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

    private fun drawMissingLayerFallback(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        baseSize: Float,
        motion: BoardLayerMotion,
        transform: BoardTransform,
        layerTransform: BoardLayerTransform,
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
                (
                    motion.alpha *
                        transform.opacity *
                        layerTransform.opacity *
                        120f
                    )
                    .toInt()
                    .coerceIn(
                        0,
                        180,
                    ),
                0,
                190,
                255,
            )

        val r =
            baseSize *
                0.16f *
                motion.scale *
                layerTransform.scale

        canvas.drawCircle(
            cx +
                width *
                layerTransform
                    .offsetXFraction,
            cy +
                height *
                layerTransform
                    .offsetYFraction +
                baseSize *
                motion.translateYFraction,
            r,
            fill,
        )
    }

    private fun layerTransform(
        layerId: BoardLayerId,
    ): BoardLayerTransform =
        layerTransforms[layerId]
            ?: BoardLayerTransform.default()

    private fun updateLayerTransformFromGesture(
        layerId: BoardLayerId,
        value: BoardLayerTransform,
    ) {
        val safe =
            value.sanitized()

        layerTransforms[layerId] = safe

        gestureLayerTransformListener
            ?.invoke(
                layerId,
                safe,
            )

        postInvalidateOnAnimation()
    }

    private fun updateTransformFromGesture(
        value: BoardTransform,
    ) {
        groupTransform =
            value.sanitized()

        gestureTransformListener
            ?.invoke(
                groupTransform,
            )

        postInvalidateOnAnimation()
    }

    private fun pointerAngle(
        event: MotionEvent,
    ): Float {
        if (event.pointerCount < 2) {
            return 0f
        }

        val dx =
            event.getX(1) -
                event.getX(0)
        val dy =
            event.getY(1) -
                event.getY(0)

        return Math.toDegrees(
            atan2(
                dy.toDouble(),
                dx.toDouble(),
            ),
        ).toFloat()
    }

    private fun normalizeDegrees(
        value: Float,
    ): Float {
        var result = value

        while (result > 180f) {
            result -= 360f
        }

        while (result < -180f) {
            result += 360f
        }

        return result
    }

    private fun follow(
        current: Float,
        target: Float,
        dt: Float,
        attackHz: Float,
        releaseHz: Float,
    ): Float {
        val rate =
            if (target > current) {
                attackHz
            } else {
                releaseHz
            }

        val factor =
            (
                dt *
                    rate
                )
                .coerceIn(
                    0f,
                    1f,
                )

        return current +
            (
                target -
                    current
                ) *
            factor
    }
}
