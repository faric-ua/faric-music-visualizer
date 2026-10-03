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
import androidx.core.graphics.PathParser
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
    private val forceModularMode: Boolean = false,
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
        val cropLeft: Float,
        val cropRight: Float,
        val parentId: String?,
        val localX: Float?,
        val localY: Float?,
        val localWidth: Float?,
        val hitWidth: Float?,
    )

    private data class HitTarget(
        val action: String,
        val z: Int,
        val rect: RectF,
    )

    private data class MasterHitZone(
        val action: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    private data class MasterSkin(
        val nativeWidth: Float,
        val nativeHeight: Float,
        val playingAsset: String,
        val pausedAsset: String,
        val hitZones: List<MasterHitZone>,
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

    private val masterSkin =
        loadMasterSkin()

    private val masterPlayingBitmap =
        masterSkin
            ?.let { skin ->
                loadAssetBitmap(
                    "pulsedeck_master/" +
                        skin.playingAsset,
                )
            }

    private val masterPausedBitmap =
        masterSkin
            ?.let { skin ->
                loadAssetBitmap(
                    "pulsedeck_master/" +
                        skin.pausedAsset,
                )
            }

    private val hitTargets =
        mutableListOf<HitTarget>()

    private val resolvedRects =
        mutableMapOf<String, RectF>()

    private val editorOffsets =
        mutableMapOf<String, Pair<Float, Float>>()

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
        loadApprovedObjectOffsets()
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

    private fun loadApprovedObjectOffsets() {
        runCatching {
            val raw =
                context.assets
                    .open("$SKIN_ROOT/object_templates/PulseDeck_object_template_centered_v1.json")
                    .bufferedReader()
                    .use { it.readText() }
            val items = JSONObject(raw).getJSONArray("items")
            repeat(items.length()) { index ->
                val item = items.getJSONObject(index)
                editorOffsets[item.getString("rendererId")] =
                    item.getDouble("dx").toFloat() to
                        item.getDouble("dy").toFloat()
            }
        }
    }

    fun isMasterPlateMode(): Boolean =
        !forceModularMode &&
            masterSkin != null &&
            masterPlayingBitmap != null &&
            masterPausedBitmap != null

    fun setEditorObjectOffset(
        id: String,
        dxNorm: Float,
        dyNorm: Float,
    ) {
        editorOffsets[id] =
            dxNorm to dyNorm
        invalidate()
    }

    fun setEditorObjectOffsets(
        offsets: Map<String, Pair<Float, Float>>,
    ) {
        editorOffsets.clear()
        editorOffsets.putAll(offsets)
        invalidate()
    }

    fun editorObjectRects(): Map<String, RectF> =
        resolvedRects.mapValues {
            RectF(it.value)
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
        if (isMasterPlateMode()) {
            controlsVisible = true
            controlsAlpha = 1f
            controlsAnimator?.cancel()
            invalidate()
            return
        }

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

        if (isMasterPlateMode()) {
            drawMasterPlate(
                canvas = canvas,
                w = w,
                h = h,
            )
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
        resolvedRects.clear()

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

    private fun drawMasterPlate(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        val bitmap =
            if (playing) {
                masterPlayingBitmap
            } else {
                masterPausedBitmap
            }
                ?: return

        bitmapPaint.alpha = 255
        bitmapPaint.xfermode = null

        val skin =
            masterSkin
                ?: return

        val nativeWidth =
            skin.nativeWidth
                .coerceAtLeast(
                    1f,
                )
        val nativeHeight =
            skin.nativeHeight
                .coerceAtLeast(
                    1f,
                )

        // Never stretch the approved artwork independently on X/Y.
        // Scale uniformly and center-crop only the tiny aspect-ratio mismatch.
        val scale =
            maxOf(
                w /
                    nativeWidth,
                h /
                    nativeHeight,
            )

        val renderedWidth =
            nativeWidth *
                scale
        val renderedHeight =
            nativeHeight *
                scale
        val left =
            (
                w -
                    renderedWidth
                ) *
                0.5f
        val top =
            (
                h -
                    renderedHeight
                ) *
                0.5f

        val masterRect =
            RectF(
                left,
                top,
                left +
                    renderedWidth,
                top +
                    renderedHeight,
            )

        canvas.drawBitmap(
            bitmap,
            null,
            masterRect,
            bitmapPaint,
        )

        hitTargets.clear()

        skin.hitZones
            .forEachIndexed { index, zone ->
                val centerX =
                    masterRect.left +
                        masterRect.width() *
                        zone.x
                val centerY =
                    masterRect.top +
                        masterRect.height() *
                        zone.y
                val zoneWidth =
                    masterRect.width() *
                        zone.width
                val zoneHeight =
                    masterRect.height() *
                        zone.height

                hitTargets.add(
                    HitTarget(
                        action =
                            zone.action,
                        z =
                            10_000 +
                                index,
                        rect =
                            RectF(
                                centerX -
                                    zoneWidth *
                                    0.5f,
                                centerY -
                                    zoneHeight *
                                    0.5f,
                                centerX +
                                    zoneWidth *
                                    0.5f,
                                centerY +
                                    zoneHeight *
                                    0.5f,
                            ),
                    ),
                )
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
        val cropLeft =
            layer.cropLeft
                .coerceIn(
                    0f,
                    0.90f,
                )
        val cropRight =
            layer.cropRight
                .coerceIn(
                    0f,
                    0.90f,
                )

        val visibleHeightFraction =
            (
                1f -
                    cropTop -
                    cropBottom
                )
                .coerceAtLeast(
                    0.05f,
                )
        val visibleWidthFraction =
            (
                1f -
                    cropLeft -
                    cropRight
                )
                .coerceAtLeast(
                    0.05f,
                )

        val sourceLeft =
            (
                bitmap.width *
                    cropLeft
                )
                .toInt()
                .coerceIn(
                    0,
                    bitmap.width - 1,
                )
        val sourceRight =
            (
                bitmap.width *
                    (
                        1f -
                            cropRight
                        )
                )
                .toInt()
                .coerceIn(
                    sourceLeft + 1,
                    bitmap.width,
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
                sourceLeft,
                sourceTop,
                sourceRight,
                sourceBottom,
            )

        val parentRect =
            layer.parentId
                ?.let { parentId ->
                    resolvedRects[parentId]
                }

        val targetWidth =
            (
                if (
                    parentRect != null &&
                    layer.localWidth != null
                ) {
                    parentRect.width() *
                        layer.localWidth
                } else {
                    w *
                        layer.width
                }
            ) *
                reactiveScale

        val aspect =
            (
                bitmap.height.toFloat() *
                    visibleHeightFraction
                ) /
                (
                    bitmap.width.toFloat() *
                        visibleWidthFraction
                    )

        val objectScaleX =
            when (layer.id) {
                "theme", "board", "visualizer", "export" -> 1.50f
                "play_pause" -> if (playing) 1.36f else 1.70f
                else -> 1f
            }
        val objectScaleY =
            when (layer.id) {
                "theme", "board", "visualizer", "export" -> 1.50f
                "play_pause" -> 1.70f
                else -> 1f
            }

        val renderedWidth =
            targetWidth *
                objectScaleX
        val targetHeight =
            targetWidth *
                aspect *
                objectScaleY

        val baseCenterX =
            if (
                parentRect != null &&
                layer.localX != null
            ) {
                parentRect.left +
                    parentRect.width() *
                    layer.localX
            } else {
                w *
                    layer.x
            }

        val baseCenterY =
            if (
                parentRect != null &&
                layer.localY != null
            ) {
                parentRect.top +
                    parentRect.height() *
                    layer.localY
            } else {
                h *
                    layer.y
            }

        val directOffset =
            editorOffsets[layer.id]
                ?: (0f to 0f)
        val groupOffset =
            if (
                layer.id ==
                    "progress_line"
            ) {
                editorOffsets["progress_group"]
                    ?: (0f to 0f)
            } else {
                0f to 0f
            }
        val editorOffset =
            (
                directOffset.first +
                    groupOffset.first
                ) to
                (
                    directOffset.second +
                    groupOffset.second
                )

        val centerX =
            baseCenterX +
                editorOffset.first *
                    w
        val centerY =
            baseCenterY +
                editorOffset.second *
                    h

        val rect =
            RectF(
                centerX -
                    renderedWidth *
                    0.5f,
                centerY -
                    targetHeight *
                    0.5f,
                centerX +
                    renderedWidth *
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

        resolvedRects[layer.id] =
            RectF(rect)

        val action =
            layer.action

        if (
            action != null &&
            controlsVisible &&
            controlsAlpha >= 0.35f
        ) {
            val touchRect =
                when {
                    action == "seek" -> {
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
                    }

                    parentRect != null &&
                        layer.hitWidth != null -> {
                        val size =
                            parentRect.width() *
                                layer.hitWidth

                        RectF(
                            centerX -
                                size *
                                0.5f,
                            centerY -
                                size *
                                0.5f,
                            centerX +
                                size *
                                0.5f,
                            centerY +
                                size *
                                0.5f,
                        )
                    }

                    else ->
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

        val headerOffset =
            editorOffsets["header_title"]
                ?: (0f to 0f)

        val headerX =
            w *
                (0.5f + headerOffset.first)
        val headerY =
            h *
                (0.069f + headerOffset.second)

        drawCenteredText(
            canvas = canvas,
            text = "FARIC PulseDeck",
            x = headerX,
            y = headerY,
            size = w * 0.052f,
            color = Color.WHITE,
            bold = true,
        )

        resolvedRects["header_title"] =
            RectF(
                headerX - w * 0.24f,
                headerY - h * 0.035f,
                headerX + w * 0.24f,
                headerY + h * 0.014f,
            )

        val trackOffset =
            editorOffsets["track_info"]
                ?: (0f to 0f)
        val trackX =
            w *
                (0.5f + trackOffset.first)
        val trackDy =
            h *
                trackOffset.second

        drawCenteredText(
            canvas = canvas,
            text = title,
            x = trackX,
            y = h * 0.485f + trackDy,
            size = w * 0.052f,
            color = Color.WHITE,
            bold = true,
        )

        drawCenteredText(
            canvas = canvas,
            text = artist,
            x = trackX,
            y = h * 0.515f + trackDy,
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
            x = trackX,
            y = h * 0.538f + trackDy,
            size = w * 0.026f,
            color =
                Color.rgb(
                    164,
                    182,
                    194,
                ),
            bold = false,
        )

        resolvedRects["track_info"] =
            RectF(
                trackX - w * 0.30f,
                h * 0.455f + trackDy,
                trackX + w * 0.30f,
                h * 0.548f + trackDy,
            )

        val progressOffset =
            editorOffsets["progress_group"]
                ?: (0f to 0f)
        val progressDx =
            w *
                progressOffset.first
        val progressDy =
            h *
                progressOffset.second

        drawCenteredText(
            canvas = canvas,
            text = elapsed,
            x = w * 0.085f + progressDx,
            y = h * 0.625f + progressDy,
            size = w * 0.031f,
            color = Color.WHITE,
            bold = true,
        )

        drawCenteredText(
            canvas = canvas,
            text = total,
            x = w * 0.915f + progressDx,
            y = h * 0.625f + progressDy,
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
        val progressOffset =
            editorOffsets["progress_group"]
                ?: (0f to 0f)
        val cx =
            left +
                (
                    right -
                        left
                    ) *
                progressFraction +
                progressOffset.first *
                    w
        val cy =
            h *
                0.595f +
                progressOffset.second *
                    h
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

        resolvedRects["progress_group"] =
            RectF(
                w * 0.045f + progressOffset.first * w,
                h * 0.575f + progressOffset.second * h,
                w * 0.955f + progressOffset.first * w,
                h * 0.635f + progressOffset.second * h,
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

    private fun loadMasterSkin(): MasterSkin? =
        runCatching {
            val raw =
                context.assets
                    .open(
                        "pulsedeck_master/manifest.json",
                    )
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(raw)
            val zones =
                json.getJSONArray(
                    "hitZones",
                )

            MasterSkin(
                nativeWidth =
                    json.getDouble(
                        "nativeWidth",
                    )
                        .toFloat(),
                nativeHeight =
                    json.getDouble(
                        "nativeHeight",
                    )
                        .toFloat(),
                playingAsset =
                    json.getString(
                        "playingAsset",
                    ),
                pausedAsset =
                    json.getString(
                        "pausedAsset",
                    ),
                hitZones =
                    buildList {
                        repeat(
                            zones.length(),
                        ) { index ->
                            val item =
                                zones.getJSONObject(
                                    index,
                                )

                            add(
                                MasterHitZone(
                                    action =
                                        item.getString(
                                            "action",
                                        ),
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
                                            "w",
                                        )
                                            .toFloat(),
                                    height =
                                        item.getDouble(
                                            "h",
                                        )
                                            .toFloat(),
                                ),
                            )
                        }
                    },
            )
        }.getOrNull()

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
                            cropLeft =
                                item.optDouble(
                                    "cropLeft",
                                    0.0,
                                )
                                    .toFloat(),
                            cropRight =
                                item.optDouble(
                                    "cropRight",
                                    0.0,
                                )
                                    .toFloat(),
                            parentId =
                                item.optString(
                                    "parent",
                                )
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            localX =
                                if (
                                    item.has(
                                        "localX",
                                    )
                                ) {
                                    item.getDouble(
                                        "localX",
                                    )
                                        .toFloat()
                                } else {
                                    null
                                },
                            localY =
                                if (
                                    item.has(
                                        "localY",
                                    )
                                ) {
                                    item.getDouble(
                                        "localY",
                                    )
                                        .toFloat()
                                } else {
                                    null
                                },
                            localWidth =
                                if (
                                    item.has(
                                        "localWidth",
                                    )
                                ) {
                                    item.getDouble(
                                        "localWidth",
                                    )
                                        .toFloat()
                                } else {
                                    null
                                },
                            hitWidth =
                                if (
                                    item.has(
                                        "hitWidth",
                                    )
                                ) {
                                    item.getDouble(
                                        "hitWidth",
                                    )
                                        .toFloat()
                                } else {
                                    null
                                },
                        ),
                    )
                }
            }
        }.getOrElse {
            emptyList()
        }

    private fun loadAssetBitmap(
        assetPath: String,
    ): Bitmap? =
        runCatching {
            context.assets
                .open(
                    assetPath,
                )
                .use { input ->
                    BitmapFactory.decodeStream(
                        input,
                    )
                }
        }.getOrNull()

    private fun loadBitmap(
        relativePath: String,
    ): Bitmap? {
        bitmaps[relativePath]
            ?.let {
                return it
            }

        val bitmap =
            if (relativePath.endsWith(".svg")) {
                loadSvgBitmap(relativePath)
            } else {
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
            }
                ?: return null

        bitmaps[relativePath] =
            bitmap

        return bitmap
    }

    private fun loadSvgBitmap(
        relativePath: String,
    ): Bitmap? =
        runCatching {
            val raw =
                context.assets
                    .open(
                        "$SKIN_ROOT/$relativePath",
                    )
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val pathData =
                Regex(
                    """<path\s+d="([^"]+)"""",
                )
                    .findAll(raw)
                    .map {
                        it.groupValues[1]
                    }
                    .toList()

            if (pathData.isEmpty()) {
                return@runCatching null
            }

            val size = 512
            val bitmap =
                Bitmap.createBitmap(
                    size,
                    size,
                    Bitmap.Config.ARGB_8888,
                )
            val canvas =
                Canvas(bitmap)
            val scale = 18f
            val inset =
                (
                    size -
                        24f *
                        scale
                    ) *
                    0.5f

            canvas.translate(
                inset,
                inset,
            )
            canvas.scale(
                scale,
                scale,
            )

            val warmAccent =
                relativePath.endsWith(
                    "pause.svg",
                ) ||
                    relativePath.endsWith(
                        "theme.svg",
                    )
            val glowColor =
                if (warmAccent) {
                    Color.rgb(
                        255,
                        126,
                        24,
                    )
                } else {
                    Color.rgb(
                        0,
                        216,
                        255,
                    )
                }
            val coreColor =
                if (warmAccent) {
                    Color.rgb(
                        255,
                        238,
                        204,
                    )
                } else {
                    Color.rgb(
                        232,
                        252,
                        255,
                    )
                }

            val filled =
                raw.contains(
                    """fill="currentColor"""",
                )

            fun drawPass(
                color: Int,
                alpha: Int,
                width: Float,
                style: Paint.Style,
            ) {
                val paint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG,
                    ).apply {
                        this.color = color
                        this.alpha = alpha
                        this.style = style
                        strokeWidth = width
                        strokeCap =
                            Paint.Cap.ROUND
                        strokeJoin =
                            Paint.Join.ROUND
                    }

                pathData.forEach { data ->
                    PathParser
                        .createPathFromPathData(
                            data,
                        )
                        ?.let { vectorPath ->
                            canvas.drawPath(
                                vectorPath,
                                paint,
                            )
                        }
                }
            }

            if (filled) {
                drawPass(
                    glowColor,
                    44,
                    5.2f,
                    Paint.Style.STROKE,
                )
                drawPass(
                    glowColor,
                    92,
                    3.5f,
                    Paint.Style.STROKE,
                )
                drawPass(
                    coreColor,
                    255,
                    0f,
                    Paint.Style.FILL,
                )
            } else {
                drawPass(
                    glowColor,
                    44,
                    5.4f,
                    Paint.Style.STROKE,
                )
                drawPass(
                    glowColor,
                    104,
                    3.6f,
                    Paint.Style.STROKE,
                )
                drawPass(
                    coreColor,
                    255,
                    2f,
                    Paint.Style.STROKE,
                )
            }

            bitmap
        }.getOrNull()

    companion object {
        private const val SKIN_ROOT =
            "pulsedeck_hud"

        private const val CONTROL_Z_MIN =
            40
    }
}
