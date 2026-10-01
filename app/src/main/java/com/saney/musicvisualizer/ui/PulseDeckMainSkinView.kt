package com.saney.musicvisualizer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import org.json.JSONObject
import kotlin.math.max

/**
 * Skin Engine v1 for the permanent PulseDeck main page.
 *
 * The visual shell is assembled from the modular PNG pack in:
 *   skin/pulsedeck_hud/
 *
 * Android code owns behavior, text and audio state. The PNG pack owns the
 * visual language, geometry and z-order through manifest.json.
 */
class PulseDeckMainSkinView(
    context: Context,
) : View(context) {

    private data class SkinLayer(
        val id: String,
        val asset: String,
        val assetPlaying: String?,
        val x: Float,
        val y: Float,
        val width: Float,
        val z: Int,
        val action: String?,
        val reactive: String?,
        val opacity: Float,
        val blend: String?,
        val cropTop: Float,
        val cropBottom: Float,
    )

    private data class HitTarget(
        val action: String,
        val z: Int,
        val rect: RectF,
    )

    private val bitmapPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG,
        )

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD,
                )
        }

    private val layers =
        loadManifest()

    private val bitmaps =
        mutableMapOf<String, Bitmap>()

    private val hitTargets =
        mutableListOf<HitTarget>()

    private val progressThumb =
        loadBitmap(
            "progress/progress_thumb.png",
        )

    private var playing = false
    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f

    private var title =
        "FARIC PulseDeck"
    private var artist =
        "Оберіть музику"
    private var status =
        "Готово · PulseDeck HUD"
    private var elapsed =
        "0:00"
    private var total =
        "0:00"
    private var progressFraction = 0f

    private var controlsVisible = true
    private var controlsAlpha = 1f
    private var controlsAnimator: ValueAnimator? = null

    private var pressedAction: String? = null
    private var scrubbing = false

    private var actionListener:
        ((String) -> Unit)? = null

    private var seekListener:
        ((Float) -> Unit)? = null

    private var interactionListener:
        (() -> Unit)? = null

    private var doubleTapListener:
        (() -> Unit)? = null

    private val gestureDetector =
        GestureDetector(
            context,
            object :
                GestureDetector
                    .SimpleOnGestureListener() {

                override fun onDown(
                    event: MotionEvent,
                ): Boolean =
                    true

                override fun onDoubleTap(
                    event: MotionEvent,
                ): Boolean {
                    if (
                        findTarget(
                            event.x,
                            event.y,
                        ) == null
                    ) {
                        doubleTapListener
                            ?.invoke()
                        return true
                    }

                    return false
                }
            },
        )

    init {
        isClickable = true
        isFocusable = true
        contentDescription =
            "PulseDeck HUD"

        layers
            .flatMap { layer ->
                buildList {
                    add(layer.asset)
                    layer.assetPlaying
                        ?.let(::add)
                }
            }
            .distinct()
            .forEach { asset ->
                loadBitmap(asset)
            }
    }

    fun updateSignal(
        signal: SceneSignal,
    ) {
        amplitude =
            signal.amplitude.coerceIn(
                0f,
                1f,
            )
        bass =
            signal.bass.coerceIn(
                0f,
                1f,
            )
        mid =
            signal.mid.coerceIn(
                0f,
                1f,
            )
        high =
            signal.high.coerceIn(
                0f,
                1f,
            )
        beat =
            max(
                beat,
                signal.beatStrength
                    .coerceIn(
                        0f,
                        1f,
                    ),
            )

        postInvalidateOnAnimation()
    }

    fun setPlaying(
        value: Boolean,
    ) {
        if (playing == value) {
            return
        }

        playing = value
        postInvalidateOnAnimation()
    }

    fun setPlaybackContent(
        title: String,
        artist: String,
        status: String,
        elapsed: String,
        total: String,
        progressFraction: Float,
    ) {
        this.title = title
        this.artist = artist
        this.status = status
        this.elapsed = elapsed
        this.total = total
        this.progressFraction =
            progressFraction.coerceIn(
                0f,
                1f,
            )

        postInvalidateOnAnimation()
    }

    fun setActionListener(
        listener: (String) -> Unit,
    ) {
        actionListener = listener
    }

    fun setSeekListener(
        listener: (Float) -> Unit,
    ) {
        seekListener = listener
    }

    fun setInteractionListener(
        listener: () -> Unit,
    ) {
        interactionListener = listener
    }

    fun setDoubleTapListener(
        listener: () -> Unit,
    ) {
        doubleTapListener = listener
    }

    fun setControlsVisible(
        visible: Boolean,
        animate: Boolean,
    ) {
        controlsVisible = visible

        val target =
            if (visible) {
                1f
            } else {
                0f
            }

        controlsAnimator?.cancel()

        if (!animate) {
            controlsAlpha = target
            invalidate()
            return
        }

        controlsAnimator =
            ValueAnimator
                .ofFloat(
                    controlsAlpha,
                    target,
                )
                .apply {
                    duration =
                        if (visible) {
                            150L
                        } else {
                            180L
                        }

                    addUpdateListener { animator ->
                        controlsAlpha =
                            animator.animatedValue
                                as Float
                        invalidate()
                    }

                    start()
                }
    }

    override fun onDetachedFromWindow() {
        controlsAnimator?.cancel()
        controlsAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        val w =
            width.toFloat()
        val h =
            height.toFloat()

        if (
            w <= 0f ||
            h <= 0f
        ) {
            return
        }

        beat *=
            if (playing) {
                0.90f
            } else {
                0.82f
            }

        canvas.drawColor(
            Color.rgb(
                0,
                4,
                8,
            ),
        )

        hitTargets.clear()

        layers
            .sortedBy {
                it.z
            }
            .forEach { layer ->
                drawLayer(
                    canvas = canvas,
                    layer = layer,
                    w = w,
                    h = h,
                )
            }

        if (controlsAlpha > 0.01f) {
            drawDynamicText(
                canvas = canvas,
                w = w,
                h = h,
            )
            drawProgressThumb(
                canvas = canvas,
                w = w,
                h = h,
            )
        }

        if (
            playing ||
            amplitude > 0.01f ||
            beat > 0.01f ||
            controlsAnimator?.isRunning == true
        ) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: SkinLayer,
        w: Float,
        h: Float,
    ) {
        val isChrome =
            layer.z >=
                CONTROL_Z_MIN

        val chromeAlpha =
            if (isChrome) {
                controlsAlpha
            } else {
                1f
            }

        if (chromeAlpha <= 0.01f) {
            return
        }

        val asset =
            if (
                layer.id ==
                    "play_pause" &&
                playing &&
                layer.assetPlaying != null
            ) {
                layer.assetPlaying
            } else {
                layer.asset
            }

        val bitmap =
            bitmaps[asset]
                ?: return

        val reactiveScale =
            when (layer.reactive) {
                "ambient" ->
                    1f +
                        amplitude *
                        0.018f

                "bass" ->
                    1f +
                        bass *
                        0.035f

                "beat" ->
                    1f +
                        beat *
                        0.070f

                "spectrum" ->
                    1f +
                        (
                            mid *
                                0.012f +
                                high *
                                0.018f
                            )

                else ->
                    1f
            }

        val cropTop =
            layer.cropTop
                .coerceIn(
                    0f,
                    0.90f,
                )
        val cropBottom =
            layer.cropBottom
                .coerceIn(
                    0f,
                    0.90f,
                )
        val visibleFraction =
            (
                1f -
                    cropTop -
                    cropBottom
                )
                .coerceAtLeast(
                    0.05f,
                )

        val sourceTop =
            (
                bitmap.height *
                    cropTop
                )
                .toInt()
                .coerceIn(
                    0,
                    bitmap.height - 1,
                )
        val sourceBottom =
            (
                bitmap.height *
                    (
                        1f -
                            cropBottom
                        )
                )
                .toInt()
                .coerceIn(
                    sourceTop + 1,
                    bitmap.height,
                )

        val sourceRect =
            Rect(
                0,
                sourceTop,
                bitmap.width,
                sourceBottom,
            )

        val targetWidth =
            w *
                layer.width *
                reactiveScale

        val aspect =
            (
                bitmap.height.toFloat() *
                    visibleFraction
                ) /
                bitmap.width.toFloat()

        val targetHeight =
            targetWidth *
                aspect

        val centerX =
            w *
                layer.x
        val centerY =
            h *
                layer.y

        val rect =
            RectF(
                centerX -
                    targetWidth *
                    0.5f,
                centerY -
                    targetHeight *
                    0.5f,
                centerX +
                    targetWidth *
                    0.5f,
                centerY +
                    targetHeight *
                    0.5f,
            )

        val reactiveAlpha =
            when (layer.reactive) {
                "ambient" ->
                    (
                        0.78f +
                            amplitude *
                            0.22f
                        )
                        .coerceIn(
                            0f,
                            1f,
                        )

                "spectrum" ->
                    (
                        0.82f +
                            high *
                            0.18f
                        )
                        .coerceIn(
                            0f,
                            1f,
                        )

                else ->
                    1f
            }

        bitmapPaint.alpha =
            (
                255f *
                    chromeAlpha *
                    reactiveAlpha *
                    layer.opacity
                        .coerceIn(
                            0f,
                            1f,
                        )
                )
                .toInt()
                .coerceIn(
                    0,
                    255,
                )

        bitmapPaint.xfermode =
            if (
                layer.blend ==
                    "screen"
            ) {
                PorterDuffXfermode(
                    PorterDuff.Mode.SCREEN,
                )
            } else {
                null
            }

        canvas.drawBitmap(
            bitmap,
            sourceRect,
            rect,
            bitmapPaint,
        )

        bitmapPaint.xfermode = null

        val action =
            layer.action

        if (
            action != null &&
            controlsVisible &&
            controlsAlpha >= 0.35f
        ) {
            val touchRect =
                if (action == "seek") {
                    val extra =
                        resources.displayMetrics
                            .density *
                            18f

                    RectF(
                        rect.left,
                        rect.top -
                            extra,
                        rect.right,
                        rect.bottom +
                            extra,
                    )
                } else {
                    rect
                }

            hitTargets.add(
                HitTarget(
                    action = action,
                    z = layer.z,
                    rect =
                        RectF(touchRect),
                ),
            )
        }
    }

    private fun drawDynamicText(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val alpha =
            (
                controlsAlpha *
                    255f
                )
                .toInt()
                .coerceIn(
                    0,
                    255,
                )

        textPaint.alpha = alpha

        drawCenteredText(
            canvas = canvas,
            text = "FARIC PulseDeck",
            x = w * 0.5f,
            y = h * 0.069f,
            size = w * 0.052f,
            color = Color.WHITE,
            bold = true,
        )

        drawCenteredText(
            canvas = canvas,
            text = title,
            x = w * 0.5f,
            y = h * 0.485f,
            size = w * 0.052f,
            color = Color.WHITE,
            bold = true,
        )

        drawCenteredText(
            canvas = canvas,
            text = artist,
            x = w * 0.5f,
            y = h * 0.515f,
            size = w * 0.037f,
            color =
                Color.rgb(
                    190,
                    202,
                    212,
                ),
            bold = false,
        )

        drawCenteredText(
            canvas = canvas,
            text = status,
            x = w * 0.5f,
            y = h * 0.538f,
            size = w * 0.026f,
            color =
                Color.rgb(
                    164,
                    182,
                    194,
                ),
            bold = false,
        )

        drawCenteredText(
            canvas = canvas,
            text = elapsed,
            x = w * 0.085f,
            y = h * 0.625f,
            size = w * 0.031f,
            color = Color.WHITE,
            bold = true,
        )

        drawCenteredText(
            canvas = canvas,
            text = total,
            x = w * 0.915f,
            y = h * 0.625f,
            size = w * 0.031f,
            color = Color.WHITE,
            bold = true,
        )

        textPaint.alpha = 255
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
    ) {
        textPaint.textSize = size
        textPaint.color = color
        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                if (bold) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                },
            )

        canvas.drawText(
            text,
            x,
            y,
            textPaint,
        )
    }

    private fun drawProgressThumb(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val bitmap =
            progressThumb
                ?: return

        val left =
            w *
                0.055f
        val right =
            w *
                0.945f
        val cx =
            left +
                (
                    right -
                        left
                    ) *
                progressFraction
        val cy =
            h *
                0.595f
        val targetWidth =
            w *
                0.046f
        val targetHeight =
            targetWidth *
                bitmap.height.toFloat() /
                bitmap.width.toFloat()

        bitmapPaint.alpha =
            (
                controlsAlpha *
                    255f
                )
                .toInt()
                .coerceIn(
                    0,
                    255,
                )

        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                cx -
                    targetWidth *
                    0.5f,
                cy -
                    targetHeight *
                    0.5f,
                cx +
                    targetWidth *
                    0.5f,
                cy +
                    targetHeight *
                    0.5f,
            ),
            bitmapPaint,
        )
    }

    override fun onTouchEvent(
        event: MotionEvent,
    ): Boolean {
        gestureDetector
            .onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val target =
                    findTarget(
                        event.x,
                        event.y,
                    )

                pressedAction =
                    target?.action

                scrubbing =
                    target?.action ==
                    "seek"

                if (
                    target != null &&
                    target.action !=
                    "seek"
                ) {
                    interactionListener
                        ?.invoke()
                }

                if (scrubbing) {
                    updateSeek(
                        event.x,
                        commit = false,
                    )
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (scrubbing) {
                    updateSeek(
                        event.x,
                        commit = false,
                    )
                    interactionListener
                        ?.invoke()
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                if (scrubbing) {
                    updateSeek(
                        event.x,
                        commit = true,
                    )
                    interactionListener
                        ?.invoke()
                } else {
                    val target =
                        findTarget(
                            event.x,
                            event.y,
                        )

                    val action =
                        target?.action

                    if (
                        pressedAction != null &&
                        action != null &&
                        action ==
                        pressedAction
                    ) {
                        performClick()
                        actionListener
                            ?.invoke(
                                action,
                            )
                    }
                }

                pressedAction = null
                scrubbing = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedAction = null
                scrubbing = false
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateSeek(
        x: Float,
        commit: Boolean,
    ) {
        val target =
            hitTargets
                .filter {
                    it.action ==
                        "seek"
                }
                .maxByOrNull {
                    it.z
                }
                ?: return

        val fraction =
            (
                (x -
                    target.rect.left) /
                    target.rect.width()
                )
                .coerceIn(
                    0f,
                    1f,
                )

        progressFraction = fraction
        invalidate()

        if (commit) {
            seekListener
                ?.invoke(
                    fraction,
                )
        }
    }

    private fun findTarget(
        x: Float,
        y: Float,
    ): HitTarget? =
        hitTargets
            .asSequence()
            .filter {
                it.rect.contains(
                    x,
                    y,
                )
            }
            .maxByOrNull {
                it.z
            }

    private fun loadManifest(): List<SkinLayer> =
        runCatching {
            val raw =
                context.assets
                    .open(
                        "$SKIN_ROOT/manifest.json",
                    )
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(raw)
            val array =
                json.getJSONArray(
                    "layers",
                )

            buildList {
                repeat(
                    array.length(),
                ) { index ->
                    val item =
                        array.getJSONObject(
                            index,
                        )

                    add(
                        SkinLayer(
                            id =
                                item.getString(
                                    "id",
                                ),
                            asset =
                                item.getString(
                                    "asset",
                                ),
                            assetPlaying =
                                item.optString(
                                    "assetPlaying",
                                )
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            x =
                                item.getDouble(
                                    "x",
                                )
                                    .toFloat(),
                            y =
                                item.getDouble(
                                    "y",
                                )
                                    .toFloat(),
                            width =
                                item.getDouble(
                                    "width",
                                )
                                    .toFloat(),
                            z =
                                item.optInt(
                                    "z",
                                    0,
                                ),
                            action =
                                item.optString(
                                    "action",
                                )
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            reactive =
                                item.optString(
                                    "reactive",
                                )
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            opacity =
                                item.optDouble(
                                    "opacity",
                                    1.0,
                                )
                                    .toFloat(),
                            blend =
                                item.optString(
                                    "blend",
                                )
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            cropTop =
                                item.optDouble(
                                    "cropTop",
                                    0.0,
                                )
                                    .toFloat(),
                            cropBottom =
                                item.optDouble(
                                    "cropBottom",
                                    0.0,
                                )
                                    .toFloat(),
                        ),
                    )
                }
            }
        }.getOrElse {
            emptyList()
        }

    private fun loadBitmap(
        relativePath: String,
    ): Bitmap? {
        bitmaps[relativePath]
            ?.let {
                return it
            }

        val bitmap =
            runCatching {
                context.assets
                    .open(
                        "$SKIN_ROOT/$relativePath",
                    )
                    .use { input ->
                        BitmapFactory.decodeStream(
                            input,
                        )
                    }
            }.getOrNull()
                ?: return null

        bitmaps[relativePath] =
            bitmap

        return bitmap
    }

    companion object {
        private const val SKIN_ROOT =
            "pulsedeck_hud"

        private const val CONTROL_Z_MIN =
            40
    }
}
