package com.saney.musicvisualizer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.PathParser
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.saney.musicvisualizer.R
import com.saney.musicvisualizer.analysis.SceneSignal
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

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
    private val transparentBackground: Boolean = false,
) : View(context) {

    data class ObjectInfo(
        val id: String,
        val label: String,
        val groupCode: String,
        val groupLabel: String,
    )

    private enum class PulseDeckSubsystem(
        val code: String,
    ) {
        ATMOSPHERE("6.0"),
        REACTOR("6.1"),
        TRACK_UI("6.2"),
        TRANSPORT("6.3"),
        QUICK_ACTIONS("6.4"),
        NAVIGATION("6.5"),
    }

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

    private val objectVisibility =
        mutableMapOf<String, Boolean>()

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

    // V104 «Утиліти»: faric's own moves / sizes per position, on top of the approved template.
    /** The MP4 forces «Відео 9:16»; null = by the view's shape (wide → «Альбом»). */
    var forcedLayoutProfile: LayoutProfileId? = null
        set(value) {
            field = value
            invalidate()
        }

    /** The layout editor shows its unsaved set here; null = the saved one. */
    var previewLayout: Map<String, LayoutAdjust>? = null
        set(value) {
            field = value
            invalidate()
        }
    private var savedLayouts: Map<LayoutProfileId, Map<String, LayoutAdjust>> =
        runCatching { LayoutProfileStore(context).loadAll() }.getOrDefault(emptyMap())
    private var activeLayout: Map<String, LayoutAdjust> = emptyMap()

    /** Each group's place in the template (no faric moves) — the centre a size change grows from. */
    private val groupBounds = HashMap<String, RectF>()
    private val groupMatrix = android.graphics.Matrix()

    /** After «Зберегти» in Утиліти: the main screen is shown again → read the saved sets anew. */
    fun reloadLayoutProfiles() {
        savedLayouts = runCatching { LayoutProfileStore(context).loadAll() }.getOrDefault(emptyMap())
        invalidate()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && previewLayout == null) reloadLayoutProfiles()
    }

    private fun matrixFor(
        groupId: String?,
        w: Float,
        h: Float,
    ): android.graphics.Matrix? {
        val adjust = groupId?.let { activeLayout[it] } ?: return null
        if (adjust.isPlaceIdentity) return null
        val bounds = groupBounds[groupId]
        val cx = bounds?.centerX() ?: (w / 2f)
        val cy = bounds?.centerY() ?: (h / 2f)
        groupMatrix.reset()
        groupMatrix.postScale(adjust.scale, adjust.scale, cx, cy)
        groupMatrix.postTranslate(adjust.dx * w, adjust.dy * h)
        return groupMatrix
    }

    /** V123: the layout editor shows switched-off parts faintly (so they can be found and switched on). */
    var showHiddenFaint: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    private val layerBounds = RectF()

    /** V132: the see-through of the part being drawn now (1 = solid). */
    private var groupAlphaMul = 1f

    /**
     * V136 (запис faric 22.10 20:02: ▶ ⏮ ⏸ на головному не реагують, у шторці — грають): with a part
     * made see-through in Утиліти (V126), [withGroup] multiplies [controlsAlpha] for drawing, and the
     * «are the controls shown?» check (≥ 0.35) then threw the buttons out of the touch list — a part
     * 70 % transparent could not be pressed at all. Touch now looks at the controls' own visibility,
     * kept here while a group is drawn (−1 = not inside a group).
     */
    private var controlsTouchAlpha = -1f

    /**
     * Draw [block] moved / resized as faric set the group; taps follow the drawing.
     * V123: a part can also be see-through ([LayoutAdjust.alpha]) or switched off ([LayoutAdjust.hidden]).
     */
    /**
     * V174 (замір 23.10 20:10: HUD draw у відео 38,8 мс/кадр, було 14–18): where the HUD's drawing time
     * goes, part by part — added up over all frames, read by the export report.
     */
    private val drawPartNs = LongArray(DRAW_PART_NAMES.size)

    private inline fun timedPart(
        index: Int,
        block: () -> Unit,
    ) {
        val started = System.nanoTime()
        block()
        drawPartNs[index] += System.nanoTime() - started
    }

    /** V174: (name, ms) of each HUD part since the view was made. */
    fun drawPartsMs(): List<Pair<String, Long>> = DRAW_PART_NAMES.mapIndexed { i, name -> name to drawPartNs[i] / 1_000_000L }

    private inline fun withGroup(
        canvas: Canvas,
        groupId: String?,
        w: Float,
        h: Float,
        block: () -> Unit,
    ) {
        val adjust = groupId?.let { activeLayout[it] }
        if (adjust != null && adjust.hidden && !showHiddenFaint) return
        val alpha =
            when {
                adjust == null -> 1f
                adjust.hidden -> 0.25f
                else -> adjust.alpha.coerceIn(0f, 1f)
            }
        val matrix = matrixFor(groupId, w, h)
        // V127: the cover paints its own alpha. V132 (замір 22.10 03:27: HUD 71 мс/кадр з прозорими
        // панелями): every other part too — no extra layer any more; the alpha rides on the paints
        // (bitmaps, text, lines) through [groupAlphaMul] and the controls alpha.
        val selfAlpha = groupId == "cover"
        coverAlpha = if (selfAlpha) (alpha * 255f).toInt().coerceIn(0, 255) else 255
        val paintAlpha = if (selfAlpha) 1f else alpha
        if (matrix == null && paintAlpha >= 0.999f) {
            block()
            return
        }
        val firstTarget = hitTargets.size
        val savedControlsAlpha = controlsAlpha
        val savedGroupAlpha = groupAlphaMul
        val savedTouchAlpha = controlsTouchAlpha
        controlsTouchAlpha = savedControlsAlpha
        groupAlphaMul = paintAlpha
        controlsAlpha = savedControlsAlpha * paintAlpha
        val saveCount = canvas.save()
        if (matrix != null) canvas.concat(matrix)
        try {
            block()
        } finally {
            canvas.restoreToCount(saveCount)
            controlsAlpha = savedControlsAlpha
            controlsTouchAlpha = savedTouchAlpha
            groupAlphaMul = savedGroupAlpha
        }
        if (adjust?.hidden == true) {
            // A faint switched-off part is only a hint in the editor — not a button.
            while (hitTargets.size > firstTarget) hitTargets.removeAt(hitTargets.size - 1)
            return
        }
        if (matrix != null) {
            for (index in firstTarget until hitTargets.size) {
                val target = hitTargets[index]
                val mapped = RectF(target.rect)
                matrix.mapRect(mapped)
                hitTargets[index] = target.copy(rect = mapped)
            }
        }
    }

    private fun rememberGroupBounds() {
        LayoutGroups.ALL.forEach { group ->
            var union: RectF? = null
            group.members.forEach { id ->
                resolvedRects[id]?.let { rect ->
                    union = (union ?: RectF(rect)).apply { union(rect) }
                }
            }
            union?.let { groupBounds[group.id] = it }
        }
    }

    /** V121: the group's place in the template (before faric's move / size) — for guides and snapping. */
    fun groupBaseRect(groupId: String): RectF? = groupBounds[groupId]?.let { RectF(it) }

    /** Where the group is on screen now (with faric's move / size) — the editor checks the video frame. */
    fun groupRect(groupId: String): RectF? {
        val bounds = groupBounds[groupId] ?: return null
        val rect = RectF(bounds)
        matrixFor(groupId, width.toFloat(), height.toFloat())?.mapRect(rect)
        return rect
    }

    private val resolvedRects =
        mutableMapOf<String, RectF>()

    private val editorOffsets =
        mutableMapOf<String, Pair<Float, Float>>()
    private var playEditorOffset = 0f to 0f
    private var pauseEditorOffset = 0f to 0f
    private var editorPlayPausePreviewPlaying: Boolean? = null

    private var playing = false
    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f
    private var spectrum = FloatArray(0)

    private var title =
        "PulseDeck"

    // V86: app logo next to the header title (same picture as the launcher icon).
    private val headerLogo by lazy { context.getDrawable(R.drawable.ic_pulsedeck_logo) }
    private var artist =
        "Оберіть музику"
    private var status =
        "Готово · PulseDeck HUD"
    private var elapsed =
        "0:00"
    private var total =
        "0:00"
    private var progressFraction = 0f

    private var equalizerVisible = true

    private var controlsVisible = true
    private var controlsAlpha = 1f
    private var transportAlpha = 1f
    private var quickActionsAlpha = 1f
    private var controlsAnimator: ValueAnimator? = null
    private var pressFeedbackAnimator: ValueAnimator? = null
    private var feedbackAction: String? = null
    private var feedbackScale = 1f
    private var feedbackGlow = 0f

    /**
     * V114: the song's embedded picture for the frame at the top («Обкладинка вгорі»); null = none.
     * V135: [coverMode] decides what the frame shows — the picture, the song's story or words as
     * slow credits ([coverStory], [coverLyrics]), or nothing (see [CoverBoxContent]).
     */
    var coverArt: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }
    var coverMode: CoverBoxMode = CoverBoxMode.AUTO
        set(value) {
            field = value
            invalidate()
        }
    var coverStory: String? = null
        set(value) {
            field = value
            invalidate()
        }
    var coverLyrics: String? = null
        set(value) {
            field = value
            invalidate()
        }

    /** V142: the ticks of the «Разом» mode (picture / story / words). */
    var coverMix: CoverMix = CoverMix()
        set(value) {
            field = value
            invalidate()
        }

    /** V141: the equalizer round the frame (bars / dots / wave), set in «Окантовка-еквалайзер». */
    var edgeEq: EdgeEqSettings
        get() = edgeRenderer.settings
        set(value) {
            edgeRenderer.settings = value
            invalidate()
        }
    private val edgeRenderer = EdgeEqRenderer()
    private var edgeSpectrum = FloatArray(0)

    /** The spectrum for the edge; without one (no analyser) — made from bass / middle / highs. */
    private fun spectrumForEdge(): FloatArray {
        if (spectrum.size >= 8) return spectrum
        if (edgeSpectrum.size != 24) edgeSpectrum = FloatArray(24)
        for (i in edgeSpectrum.indices) {
            val k = i / 23f
            edgeSpectrum[i] =
                when {
                    k < 0.5f -> bass + (mid - bass) * (k / 0.5f)
                    else -> mid + (high - mid) * ((k - 0.5f) / 0.5f)
                }
        }
        return edgeSpectrum
    }

    /** V135: neon light from under the frame, pulsing with this part of the music. */
    var coverGlowBand: CoverGlowBand = CoverGlowBand.BASS
        set(value) {
            field = value
            invalidate()
        }
    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var coverShader: android.graphics.BitmapShader? = null
    private var coverShaderBitmap: Bitmap? = null
    private val coverShaderMatrix = android.graphics.Matrix()

    /** V127: the cover's see-through (0–255), set by withGroup — the cover needs no extra layer. */
    private var coverAlpha = 255
    private val coverFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val coverGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val coverGlowRect = RectF()
    private val coverTextPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var coverLayout: android.text.StaticLayout? = null
    private var coverLayoutKey: String? = null

    /** The light level the neon follows, smoothed so it breathes instead of flickering. */
    private var coverGlowLevel = 0f

    /**
     * V114: frame size and place — a square between the header and the hero, centred.
     * Everything inside is laid out on the screen from this frame — see drawCoverBox (V137 / V138).
     */
    private fun coverBoxRect(
        w: Float,
        h: Float,
    ): RectF {
        val side = w * 0.2f
        val offset = editorOffsets[COVER_BOX_ID] ?: (0f to 0f)
        val width = side
        val left = (w - width) / 2f + offset.first * w
        val top = h * 0.137f + offset.second * h
        return RectF(left, top, left + width, top + side)
    }

    /**
     * V114 / V135 / V137 / V138: the frame at the top. Everything is drawn on the SCREEN: the cover's
     * «Розкладка» size only says where the frame is and how big ([CoverBoxContent.framePanel]) — the
     * neon, the edge, the letters and the small buttons inside keep their screen size, so a frame made
     * 5× bigger does not get 5× letters (faric 22.10 20:24, 22:50).
     */
    private fun drawCoverBox(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        if (!isObjectVisible(COVER_BOX_ID)) return
        val art = coverArt?.takeIf { !it.isRecycled }
        val choice = CoverBoxContent.resolve(coverMode, art != null, coverStory, coverLyrics, coverMix)
        val square = coverBoxRect(w, h)
        resolvedRects[COVER_BOX_ID] = RectF(square)
        val group = matrixFor("cover", w, h)
        coverScreenMatrix.reset()
        if (group != null) coverScreenMatrix.set(group)
        if (group != null && !coverScreenMatrix.invert(coverInverse)) return
        val onScreen = RectF(square)
        coverScreenMatrix.mapRect(onScreen)
        val box =
            CoverBoxContent.framePanel(
                LayoutBox(onScreen.left, onScreen.top, onScreen.right, onScreen.bottom),
                w,
                h,
                text = choice.show == CoverBoxShow.TEXT,
            )
        val frame = RectF(box.left, box.top, box.right, box.bottom)
        // V154: the equalizer goes inside the frame — the tile gives up its width (only if it stays a tile).
        val eqInset = EdgeEqMath.inset(edgeRenderer.settings, w).coerceAtMost(minOf(frame.width(), frame.height()) * 0.2f)
        val panel = RectF(frame).apply { inset(eqInset, eqInset) }
        // V158 (faric 23.10 17:33: «хочу змахнути від центру на праву сторінку, а воно відкриває
        // меню»): the frame itself takes no taps any more — only its ⋮ / 👍 / 👎 do, so a swipe that
        // starts on the frame works like anywhere else. (Its menu is also in «PulseDeck tools».)
        if (choice.show == CoverBoxShow.HIDDEN) return

        val save = canvas.save()
        if (group != null) canvas.concat(coverInverse)
        val alpha = coverAlpha
        val radius = minOf(panel.width(), panel.height()) * if (choice.show == CoverBoxShow.TEXT) 0.06f else 0.09f
        timedPart(PART_GLOW) { drawCoverGlow(canvas, panel, radius, alpha) }
        // V141: the equalizer round the tile (under the tile — it grows outward from the edge).
        timedPart(PART_EDGE) {
            edgeRenderer.draw(canvas, panel, radius, spectrumForEdge(), marqueeClockMs ?: SystemClock.uptimeMillis(), alpha, w)
        }
        when (choice.show) {
            CoverBoxShow.PICTURE -> art?.let { timedPart(PART_PICTURE) { drawCoverPicture(canvas, panel, radius, it, alpha) } }
            CoverBoxShow.TEXT -> {
                // V167 (скрін faric 23.10 18:48: «внутрішня частина з прямими кутами… закруглити й
                // маленьку окантовку»): a thin dark border ring, and everything inside — picture, veil,
                // text — in a rounded inner tile.
                val border = (resources.displayMetrics.density * 5f).coerceAtMost(minOf(panel.width(), panel.height()) * 0.05f)
                val inner = RectF(panel).apply { inset(border, border) }
                val innerRadius = (radius - border).coerceAtLeast(0f)
                coverRingPath.reset()
                coverRingPath.fillType = android.graphics.Path.FillType.EVEN_ODD
                coverRingPath.addRoundRect(panel, radius, radius, android.graphics.Path.Direction.CW)
                coverRingPath.addRoundRect(inner, innerRadius, innerRadius, android.graphics.Path.Direction.CW)
                coverPaint.shader = null
                coverPaint.color = Color.rgb(8, 14, 20)
                coverPaint.alpha = (maxOf(200f, 255 * coverText.veil / 100f) * alpha / 255f).toInt().coerceIn(0, 255)
                canvas.drawPath(coverRingPath, coverPaint)
                // V142: «Разом» — the picture underneath, the text going over it on a lighter veil.
                val under = if (choice.pictureUnder) art else null
                // V174: with the picture under the text the veil is mixed into the picture itself (one pass
                // over the tile instead of two) — the same colours while the tile is fully visible.
                val veilAlpha = (255 * coverText.veil / 100f).toInt().coerceIn(0, 255)
                val veilInPicture = under != null && alpha >= 255 && veilAlpha > 0
                if (under != null) {
                    timedPart(PART_PICTURE) {
                        drawCoverPicture(canvas, inner, innerRadius, under, alpha, if (veilInPicture) veilAlpha else 0)
                    }
                }
                timedPart(PART_TEXT) { drawCoverText(canvas, inner, innerRadius, w, choice, alpha, veilDone = veilInPicture) }
            }
            else -> {
                coverPaint.shader = null
                coverPaint.color = Color.rgb(10, 18, 26)
                coverPaint.alpha = alpha
                canvas.drawRoundRect(panel, radius, radius, coverPaint)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = minOf(panel.width(), panel.height()) * 0.5f
                textPaint.color = Color.rgb(64, 200, 255)
                textPaint.alpha = alpha
                canvas.drawText("♪", panel.centerX(), panel.centerY() + panel.height() * 0.18f, textPaint)
                textPaint.alpha = 255
            }
        }
        // V135 (faric 22.10 17:17: «края дуже товсті… границі описані жорстоко»): a thin, quiet edge;
        // the light comes from the neon under the tile.
        coverFramePaint.strokeWidth = resources.displayMetrics.density * 0.8f
        coverFramePaint.color = Color.argb((70 + 120 * coverGlowLevel).toInt() * alpha / 255, 120, 220, 255)
        canvas.drawRoundRect(panel, radius, radius, coverFramePaint)
        if (isObjectVisible(COVER_MENU_ID)) drawCoverMenu(canvas, panel, w, alpha, group != null)
        canvas.restoreToCount(save)
    }

    /** Centre-crop: the picture keeps its proportions; V127: a shader in a rounded rectangle, no layer. */
    private fun drawCoverPicture(
        canvas: Canvas,
        panel: RectF,
        radius: Float,
        picture: Bitmap,
        alpha: Int,
        /** V174: 0–255 of the dark veil mixed straight into the picture (instead of a second pass). */
        veil: Int = 0,
    ) {
        if (drawCoverPictureOnPixels(canvas, panel, radius, picture, alpha, veil)) return
        drawCoverPictureDirect(canvas, panel, radius, picture, alpha, veil)
    }

    /**
     * V174 крок 3: the video (software canvas) gets the cover already cut round, veiled and at the video's
     * pixel size — made once, then copied 1:1 every frame instead of stretching it through a rounded mask.
     */
    private fun drawCoverPictureOnPixels(
        canvas: Canvas,
        panel: RectF,
        radius: Float,
        picture: Bitmap,
        alpha: Int,
        veil: Int,
    ): Boolean {
        val values = softwareAxisValues(canvas) ?: return false
        // A round corner stays round only when both sides are stretched alike.
        if (kotlin.math.abs(values[0] - values[4]) > 1e-3f) return false
        val px = SoftwareBlitMath.deviceRect(values, panel.left, panel.top, panel.right, panel.bottom) ?: return false
        if (SoftwareBlitMath.bytesOf(px.width, px.height, 4) > SoftwareBlitMath.MAX_ENTRY_BYTES) return false
        val pixelRadius = radius * values[0]
        val key = CoverCopyKey(picture, px.width, px.height, (pixelRadius * 4f).roundToInt(), veil)
        val copy =
            coverCopy?.takeIf { coverCopyKey == key && !it.isRecycled }
                ?: runCatching {
                    Bitmap.createBitmap(px.width, px.height, Bitmap.Config.ARGB_8888).also { out ->
                        out.eraseColor(Color.TRANSPARENT)
                        val tile = RectF(0f, 0f, px.width.toFloat(), px.height.toFloat())
                        drawCoverPictureDirect(Canvas(out), tile, pixelRadius, picture, 255, veil)
                    }
                }.getOrNull()?.also {
                    coverCopy = it
                    coverCopyKey = key
                }
                ?: return false
        pixelCopyPaint.alpha = alpha
        drawOnPixels(canvas) { canvas.drawBitmap(copy, px.left.toFloat(), px.top.toFloat(), pixelCopyPaint) }
        pixelCopyPaint.alpha = 255
        return true
    }

    private data class CoverCopyKey(
        val picture: Bitmap,
        val width: Int,
        val height: Int,
        val radiusQuarterPx: Int,
        val veil: Int,
    )

    private var coverCopy: Bitmap? = null
    private var coverCopyKey: CoverCopyKey? = null

    private fun drawCoverPictureDirect(
        canvas: Canvas,
        panel: RectF,
        radius: Float,
        picture: Bitmap,
        alpha: Int,
        veil: Int,
    ) {
        val fullScale = max(panel.width() / picture.width, panel.height() / picture.height)
        // V174 (замір 23.10 21:08: «рамка: картинка» 4,8 мс на кадр відео): a big cover squeezed into the
        // tile on every frame. Squeeze it once to the tile's size and keep that copy.
        val source =
            if (fullScale < 0.95f) {
                val tw = (picture.width * fullScale).roundToInt().coerceAtLeast(1)
                val th = (picture.height * fullScale).roundToInt().coerceAtLeast(1)
                coverScaled?.takeIf { coverScaledFrom === picture && it.width == tw && it.height == th && !it.isRecycled }
                    ?: runCatching { Bitmap.createScaledBitmap(picture, tw, th, true) }.getOrNull()?.also {
                        coverScaled = it
                        coverScaledFrom = picture
                    }
                    ?: picture
            } else {
                picture
            }
        val shader =
            coverShader?.takeIf { coverShaderBitmap === source }
                ?: android.graphics.BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
                    coverShader = it
                    coverShaderBitmap = source
                }
        val scale = max(panel.width() / source.width, panel.height() / source.height)
        coverShaderMatrix.reset()
        coverShaderMatrix.setScale(scale, scale)
        coverShaderMatrix.postTranslate(
            panel.centerX() - source.width * scale / 2f,
            panel.centerY() - source.height * scale / 2f,
        )
        shader.setLocalMatrix(coverShaderMatrix)
        coverPaint.shader = shader
        coverPaint.color = Color.WHITE
        coverPaint.alpha = alpha
        if (veil > 0) {
            // picture × (1 − v) + veil colour × v — exactly the old veil drawn over the picture.
            coverPaint.colorFilter =
                coverVeilFilter?.takeIf { coverVeilFilterAlpha == veil }
                    ?: run {
                        val keep = 1f - veil / 255f
                        val k = (255 * keep).roundToInt().coerceIn(0, 255)
                        val v = veil / 255f
                        android.graphics.LightingColorFilter(
                            Color.rgb(k, k, k),
                            Color.rgb((8 * v).roundToInt(), (14 * v).roundToInt(), (20 * v).roundToInt()),
                        ).also {
                            coverVeilFilter = it
                            coverVeilFilterAlpha = veil
                        }
                    }
        }
        canvas.drawRoundRect(panel, radius, radius, coverPaint)
        coverPaint.colorFilter = null
        coverPaint.shader = null
    }

    private var coverScaled: Bitmap? = null
    private var coverScaledFrom: Bitmap? = null
    private var coverVeilFilter: android.graphics.ColorFilter? = null
    private var coverVeilFilterAlpha = -1

    private fun addCoverTarget(
        action: String,
        z: Int,
        screenRect: RectF,
        inGroup: Boolean,
    ) {
        val rect = RectF(screenRect)
        if (inGroup) coverInverse.mapRect(rect)
        hitTargets.add(HitTarget(action = action, z = z, rect = rect))
    }

    /**
     * V135 (faric 22.10 17:17–17:18: «як неонове свічення з-під плитки… реагувати на музику»):
     * soft light around the tile — a few wide, faint strokes, brighter and wider with the chosen part
     * of the music. Plain strokes only (no blur, no layer) — cheap enough for every video frame.
     */
    private fun drawCoverGlow(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        alpha: Int,
    ) {
        val raw =
            when (coverGlowBand) {
                CoverGlowBand.OFF -> 0f
                CoverGlowBand.BASS -> bass
                CoverGlowBand.MID -> mid
                CoverGlowBand.HIGH -> high
                CoverGlowBand.BEAT -> beat
            }.coerceIn(0f, 1f)
        // Quick up, slower down: a hit flashes, then the light fades out like a neon tube.
        coverGlowLevel = if (raw > coverGlowLevel) coverGlowLevel + (raw - coverGlowLevel) * 0.6f else coverGlowLevel * 0.86f + raw * 0.14f
        if (coverGlowBand == CoverGlowBand.OFF) {
            coverGlowLevel = 0f
            return
        }
        val level = coverGlowLevel
        val density = resources.displayMetrics.density
        val rings = 4
        for (i in rings downTo 1) {
            val spread = density * (2f + i * (2.2f + 3.5f * level))
            coverGlowRect.set(rect)
            coverGlowRect.inset(-spread / 2f, -spread / 2f)
            coverGlowPaint.strokeWidth = spread
            val a = (alpha * (0.05f + 0.16f * level) * (rings + 1 - i) / rings).toInt().coerceIn(0, 255)
            coverGlowPaint.color = Color.argb(a, 40 + (140 * level).toInt(), 190, 255)
            canvas.drawRoundRect(coverGlowRect, radius + spread / 2f, radius + spread / 2f, coverGlowPaint)
        }
    }

    private val coverInverse = android.graphics.Matrix()
    private val coverScreenMatrix = android.graphics.Matrix()

    /**
     * V138 (faric 22.10 22:50, скрін Poweramp): rating of the playing song — 1 👍, −1 👎, 0 none.
     * Shown on the small see-through buttons at the bottom of the frame, with ⋮ for the frame's menu.
     */
    var coverRating: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    private val coverMenuPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPath: android.graphics.Path by lazy {
        // Material «thumb_up» (24 × 24) — the same shape as Poweramp's, drawn in our soft blue.
        PathParser.createPathFromPathData(
            "M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 " +
                "7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z",
        )
    }
    private val iconMatrix = android.graphics.Matrix()
    private val iconPath = android.graphics.Path()

    private fun drawCoverMenu(
        canvas: Canvas,
        panel: RectF,
        w: Float,
        alpha: Int,
        inGroup: Boolean,
    ) {
        val d = (w * 0.085f).coerceAtMost(minOf(panel.width(), panel.height()) * 0.3f)
        if (d < w * 0.04f) return
        val gap = d * 0.18f
        val cy = panel.bottom - gap - d / 2f
        val buttons =
            listOf(
                Triple(COVER_LIKE_ID, panel.left + gap + d / 2f, coverRating > 0),
                Triple(COVER_DISLIKE_ID, panel.left + gap * 2f + d * 1.5f, coverRating < 0),
                Triple(COVER_MORE_ID, panel.right - gap - d / 2f, false),
            )
        for ((id, cx, active) in buttons) {
            val pressed = feedbackAction == id
            val r = d / 2f * (if (pressed) feedbackScale else 1f)
            coverMenuPaint.style = Paint.Style.FILL
            coverMenuPaint.color = if (active) Color.rgb(20, 60, 90) else Color.rgb(6, 12, 18)
            coverMenuPaint.alpha = (alpha * 0.55f).toInt()
            canvas.drawCircle(cx, cy, r, coverMenuPaint)
            coverMenuPaint.style = Paint.Style.STROKE
            coverMenuPaint.strokeWidth = resources.displayMetrics.density
            coverMenuPaint.color = Color.rgb(120, 200, 240)
            coverMenuPaint.alpha = (alpha * if (active) 0.9f else 0.35f).toInt()
            canvas.drawCircle(cx, cy, r, coverMenuPaint)
            coverMenuPaint.style = Paint.Style.FILL
            // Soft blue, not white (faric: white on black is hard for his eyes); chosen = bright cyan / orange.
            coverMenuPaint.color =
                when {
                    active && id == COVER_LIKE_ID -> Color.rgb(90, 220, 255)
                    active && id == COVER_DISLIKE_ID -> Color.rgb(255, 160, 80)
                    else -> Color.rgb(150, 200, 235)
                }
            coverMenuPaint.alpha = (alpha * if (active) 1f else 0.8f).toInt()
            if (id == COVER_MORE_ID) {
                val dot = r * 0.11f
                for (k in -1..1) canvas.drawCircle(cx, cy + k * r * 0.34f, dot, coverMenuPaint)
            } else {
                val icon = r * 1.0f
                iconMatrix.reset()
                iconMatrix.setScale(icon / 24f, icon / 24f)
                iconMatrix.postTranslate(-icon / 2f, -icon / 2f)
                if (id == COVER_DISLIKE_ID) iconMatrix.postRotate(180f)
                iconMatrix.postTranslate(cx, cy)
                iconPath.reset()
                thumbPath.transform(iconMatrix, iconPath)
                canvas.drawPath(iconPath, coverMenuPaint)
            }
            addCoverTarget(id, 950, RectF(cx - d / 2f, cy - d / 2f, cx + d / 2f, cy + d / 2f), inGroup)
        }
    }

    /**
     * V138 (faric 22.10 22:50: «текст ривками йде»): the live screen gets the song position only
     * 4 times a second; between those the credits move on by the clock, so they glide. The MP4 gives
     * every frame its exact time ([marqueeClockMs] set) — no guessing there.
     */
    private var clockPositionMs = 0L
    private var clockDurationMs = 0L
    private var clockPlaying = false
    private var clockStampMs = 0L

    fun setPlaybackClock(
        positionMs: Long,
        durationMs: Long,
        playing: Boolean,
    ) {
        val now = SystemClock.uptimeMillis()
        val predicted = if (clockPlaying) clockPositionMs + (now - clockStampMs) else clockPositionMs
        val sameSong = durationMs == clockDurationMs
        // V150: a new position nudges the running clock instead of pulling it back.
        clockPositionMs = CoverBoxContent.smoothClock(predicted, positionMs, clockPlaying && sameSong, playing)
        clockDurationMs = durationMs
        clockPlaying = playing
        clockStampMs = now
    }

    private var crawlShown = -1f

    private fun crawlFraction(): Float {
        if (marqueeClockMs != null || clockDurationMs <= 0L) return progressFraction
        val moved = if (clockPlaying) SystemClock.uptimeMillis() - clockStampMs else 0L
        val target = ((clockPositionMs + moved).toFloat() / clockDurationMs).coerceIn(0f, 1f)
        // V148: never step back a hair — the text waits a moment instead. V150: only ≤ 0,12 s (the clock
        // itself is smoothed now; 0,6 s made the text stop for ~0,2 s every few seconds).
        crawlShown = if (crawlShown < 0f) target else CoverBoxContent.steadyFraction(crawlShown, target, 120f / clockDurationMs)
        return crawlShown
    }

    /** V148: the text's own look (opacity, veil, size) — «Текст і окантовка». */
    var coverText: CoverTextSettings = CoverTextSettings()
        set(value) {
            field = value
            invalidate()
        }

    /** V148: the laid-out text as a soft (ALPHA_8) picture — moved by fractions of a pixel, smoothly. */
    private var coverTextBitmap: Bitmap? = null
    private var coverTextBitmapKey: String? = null
    private val coverTextBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val coverRingPath = android.graphics.Path()

    /**
     * V135 / V137: the story or the song's words as slow credits — enter at the bottom of the panel,
     * leave over its top exactly when the song ends. Story: justified paragraphs with a first-line
     * indent; headings and song words centred. V138: letters a bit bigger than the song title, soft blue.
     */
    private fun drawCoverText(
        canvas: Canvas,
        panel: RectF,
        radius: Float,
        w: Float,
        choice: CoverBoxChoice,
        alpha: Int,
        veilDone: Boolean = false,
    ) {
        val text = choice.text.orEmpty()
        val look = coverText
        if (!veilDone) {
            coverPaint.shader = null
            coverPaint.color = Color.rgb(8, 14, 20)
            // V148: the veil and the letters have their own see-through, not the tile's.
            coverPaint.alpha = (255 * look.veil / 100f).toInt().coerceIn(0, 255)
            if (coverPaint.alpha > 0) canvas.drawRoundRect(panel, radius, radius, coverPaint)
        }
        val padX = w * 0.04f
        val padY = w * 0.03f
        val innerWidth = (panel.width() - padX * 2f).toInt().coerceAtLeast(1)
        // V141 (скрін faric 23.10 00:24: 👍 👎 ⋮ лежали поверх нижніх рядків): with the buttons on, the
        // text ends above them.
        // V170 (faric 23.10 20:10, скрін: «текст раніше виходив аж знизу, а зараз йде над кружечками…
        // хай аж із самого низу виходить, як вверх заходить; а пальці наверху будуть»): the crawl uses the
        // whole tile again — it comes in at the very bottom, under 👍 👎 ⋮ (they are drawn over it).
        val padBottom = padY
        val innerHeight = (panel.height() - padY - padBottom).coerceAtLeast(1f)
        // The song title's size (w × 0.031, with its own «Розкладка» size) and «a couple of points» more.
        val size = w * 0.031f * 1.15f * (activeLayout["track"]?.scale ?: 1f).coerceIn(0.8f, 1.6f) * look.size / 100f
        val key = text.hashCode().toString() + "|" + choice.lyrics + "|" + innerWidth + "|" + size
        val layout =
            coverLayout?.takeIf { coverLayoutKey == key }
                ?: buildCoverLayout(text, choice.lyrics, innerWidth, size).also {
                    coverLayout = it
                    coverLayoutKey = key
                }
        val textAlpha = (255 * look.opacity / 100f).toInt().coerceIn(0, 255)
        val top = panel.top + padY + CoverBoxContent.crawlTop(crawlFraction(), innerHeight, layout.height.toFloat())
        // V174: the video (a software canvas) gets the light one-byte picture of the text — 4× less to read
        // per pixel; the screen keeps the coloured one (V167: no grey box on the phone's graphics).
        val textClipLeft = panel.left + padX * 0.5f
        val textClipTop = panel.top + padY * 0.5f
        val textClipRight = panel.right - padX * 0.5f
        val textClipBottom = panel.bottom - padBottom
        if (drawCoverTextOnPixels(canvas, layout, key, panel.left + padX, top, textAlpha, textClipLeft, textClipTop, textClipRight, textClipBottom)) {
            if (marqueeClockMs == null && clockPlaying) postInvalidateOnAnimation()
            return
        }
        val bitmap = coverTextBitmapFor(layout, key, alphaOnly = !canvas.isHardwareAccelerated)
        if (bitmap != null) {
            // V148 (faric 23.10 02:54: «інколи ривками»): letters drawn straight snap to whole pixels, so a
            // slow crawl moved in 1-pixel hops; a filtered picture of the text glides between pixels.
            // V168 (падіння v0.21.27 при переході на центр): back to the plain drawBitmap of V148 (the V167
            // pattern-in-a-rounded-window was the only new drawing there). The text window lies well inside
            // the rounded inner tile, so its straight edges never reach the corners.
            val clip = canvas.save()
            canvas.clipRect(textClipLeft, textClipTop, textClipRight, textClipBottom)
            coverTextBitmapPaint.color = Color.rgb(176, 216, 242)
            coverTextBitmapPaint.alpha = textAlpha
            canvas.drawBitmap(bitmap, panel.left + padX - TEXT_BITMAP_PAD, top - TEXT_BITMAP_PAD, coverTextBitmapPaint)
            canvas.restoreToCount(clip)
        } else {
            val clip = canvas.save()
            canvas.clipRect(panel.left + padX, panel.top + padY, panel.right - padX, panel.bottom - padBottom)
            coverTextPaint.color = Color.rgb(176, 216, 242)
            coverTextPaint.alpha = textAlpha
            canvas.translate(panel.left + padX, top)
            layout.draw(canvas)
            canvas.restoreToCount(clip)
        }
        if (marqueeClockMs == null && clockPlaying) postInvalidateOnAnimation()
    }

    /**
     * V174 крок 3 (замір: «рамка: текст» 10–12 мс на кадр відео — найдорожча частина): the video gets the
     * text drawn once at the video's own pixel size, in 4 (long texts — 2) copies shifted by a quarter
     * (half) pixel, and each frame copies the right one 1:1 onto whole pixels. The crawl still glides
     * (V148: no 1-pixel hops) and the letters are drawn sharp at the video's size instead of stretched.
     * Too long a text (or a turned frame) keeps the old way.
     */
    private fun drawCoverTextOnPixels(
        canvas: Canvas,
        layout: android.text.StaticLayout,
        layoutKey: String,
        left: Float,
        top: Float,
        textAlpha: Int,
        clipLeft: Float,
        clipTop: Float,
        clipRight: Float,
        clipBottom: Float,
    ): Boolean {
        val values = softwareAxisValues(canvas) ?: return false
        if (layout.width <= 0 || layout.height <= 0) return false
        val sx = values[0]
        val sy = values[4]
        val pad = TEXT_BITMAP_PAD
        val width = kotlin.math.ceil(layout.width * sx).toInt() + pad * 2
        val height = kotlin.math.ceil(layout.height * sy).toInt() + pad * 2 + 1
        val phases = SoftwareBlitMath.textPhases(SoftwareBlitMath.bytesOf(width, height, 1))
        if (phases == 0) return false
        val key = layoutKey + "|px|" + sx + "|" + sy + "|" + phases
        val copies =
            coverTextCopies?.takeIf { coverTextCopiesKey == key }
                ?: run {
                    coverTextCopies = null
                    runCatching {
                        List(phases) { phase ->
                            Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8).also { bmp ->
                                bmp.eraseColor(Color.TRANSPARENT)
                                coverTextPaint.color = Color.WHITE
                                coverTextPaint.alpha = 255
                                val c = Canvas(bmp)
                                c.translate(pad.toFloat(), pad + phase.toFloat() / phases)
                                c.scale(sx, sy)
                                layout.draw(c)
                            }
                        }
                    }.getOrNull()?.also {
                        coverTextCopies = it
                        coverTextCopiesKey = key
                    }
                }
                ?: return false
        val x = (left * sx + values[2] - pad).roundToInt()
        val rowPhase = SoftwareBlitMath.rowPhase(top * sy + values[5] - pad, phases)
        val clip = canvas.save()
        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom)
        coverTextBitmapPaint.color = Color.rgb(176, 216, 242)
        coverTextBitmapPaint.alpha = textAlpha
        val copy = copies[rowPhase.phase]
        drawOnPixels(canvas) { canvas.drawBitmap(copy, x.toFloat(), rowPhase.row.toFloat(), coverTextBitmapPaint) }
        canvas.restoreToCount(clip)
        return true
    }

    private var coverTextCopies: List<Bitmap>? = null
    private var coverTextCopiesKey: String? = null

    private fun coverTextBitmapFor(
        layout: android.text.StaticLayout,
        layoutKey: String,
        alphaOnly: Boolean = false,
    ): Bitmap? {
        val key = layoutKey + if (alphaOnly) "|a8" else "|argb"
        coverTextBitmap?.takeIf { coverTextBitmapKey == key && !it.isRecycled }?.let { return it }
        // V168: no recycle() — the screen's drawing thread may still hold the old picture; the memory
        // is freed by itself once nothing uses it.
        coverTextBitmap = null
        coverTextBitmapKey = key
        if (layout.width <= 0 || layout.height <= 0 || layout.height > 8_000) return null
        // V167: coloured letters on a fully transparent picture (no grey box on any phone), with a
        // transparent edge of TEXT_BITMAP_PAD px so the stretched edge of the pattern stays empty. Very
        // long texts keep the lighter alpha-only picture, tinted by the paint.
        val full = !alphaOnly && layout.height <= 4_000
        return runCatching {
            Bitmap
                .createBitmap(
                    layout.width + TEXT_BITMAP_PAD * 2,
                    layout.height + TEXT_BITMAP_PAD * 2,
                    if (full) Bitmap.Config.ARGB_8888 else Bitmap.Config.ALPHA_8,
                ).also { bmp ->
                    bmp.eraseColor(Color.TRANSPARENT)
                    coverTextPaint.color = if (full) Color.rgb(176, 216, 242) else Color.WHITE
                    coverTextPaint.alpha = 255
                    val c = Canvas(bmp)
                    c.translate(TEXT_BITMAP_PAD.toFloat(), TEXT_BITMAP_PAD.toFloat())
                    layout.draw(c)
                    coverTextBitmap = bmp
                }
        }.getOrNull()
    }

    private fun buildCoverLayout(
        text: String,
        lyrics: Boolean,
        width: Int,
        size: Float,
    ): android.text.StaticLayout {
        coverTextPaint.textSize = size
        coverTextPaint.typeface = Typeface.DEFAULT
        val styled = android.text.SpannableStringBuilder()
        val paragraphs = if (lyrics) listOf(text.trim()) else CoverBoxContent.paragraphs(text)
        paragraphs.forEachIndexed { index, paragraph ->
            if (index > 0) {
                // A half-height empty line between paragraphs.
                styled.append('\n')
                val gap = styled.length
                styled.append('\n')
                styled.setSpan(android.text.style.RelativeSizeSpan(0.5f), gap, gap + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val start = styled.length
            styled.append(paragraph)
            val end = styled.length
            when {
                lyrics || CoverBoxContent.isHeading(paragraph) -> {
                    styled.setSpan(
                        android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                        start,
                        end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    if (!lyrics) styled.setSpan(android.text.style.StyleSpan(Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                CoverBoxContent.isLineBlock(paragraph) -> {
                    // V194: «TRACK 01 — 4:24–4:44» / «IGNITION POINT» — centred; the song's name (last line) bold.
                    styled.setSpan(
                        android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                        start,
                        end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    val lastLine = paragraph.lastIndexOf('\n') + 1
                    styled.setSpan(android.text.style.StyleSpan(Typeface.BOLD), start + lastLine, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else ->
                    styled.setSpan(
                        android.text.style.LeadingMarginSpan.Standard((size * 1.6f).toInt(), 0),
                        start,
                        end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
            }
        }
        val builder =
            android.text.StaticLayout.Builder
                .obtain(styled, 0, styled.length, coverTextPaint, width)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.22f)
                .setIncludePad(false)
        if (!lyrics) builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
        return builder.build()
    }

    private var pressedAction: String? = null
    private var pressedOriginAction: String? = null
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
            Handler(
                Looper.getMainLooper(),
            ),
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
            playEditorOffset =
                editorOffsets["play"]
                    ?: editorOffsets["play_pause"]
                    ?: (0f to 0f)
            pauseEditorOffset =
                editorOffsets["pause"]
                    ?: editorOffsets["play_pause"]
                    ?: (0f to 0f)
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
        playEditorOffset = offsets["play"] ?: offsets["play_pause"] ?: (0f to 0f)
        pauseEditorOffset = offsets["pause"] ?: offsets["play_pause"] ?: (0f to 0f)
        invalidate()
    }

    fun setEditorPlayPausePreview(playing: Boolean?) {
        editorPlayPausePreviewPlaying = playing
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
        spectrum = signal.spectrum.copyOf()

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


    fun setObjectVisible(
        objectId: String,
        visible: Boolean,
    ) {
        objectVisibility[objectId] = visible
        postInvalidateOnAnimation()
    }

    fun isObjectVisible(
        objectId: String,
    ): Boolean = objectVisibility[objectId] != false

    fun objectVisibilityItems(): List<ObjectInfo> =
        buildList {
            layers.forEach { layer ->
                val subsystem =
                    subsystemFor(layer.id)
                add(
                    ObjectInfo(
                        id = layer.id,
                        label = objectLabel(layer.id),
                        groupCode = subsystem.code,
                        groupLabel = subsystemLabel(subsystem),
                    ),
                )
            }

            add(
                ObjectInfo(
                    id = "header_title",
                    label = "Header title",
                    groupCode = PulseDeckSubsystem.NAVIGATION.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.NAVIGATION),
                ),
            )
            add(
                ObjectInfo(
                    id = "track_info",
                    label = "Track info",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            // V114 (faric 21.10 18:35): the song's cover in a frame at the top of the screen.
            add(
                ObjectInfo(
                    id = COVER_BOX_ID,
                    label = "Обкладинка вгорі",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            // V138 (faric 22.10 22:50): 👍 👎 ⋮ at the bottom of the frame — can be switched off.
            add(
                ObjectInfo(
                    id = COVER_MENU_ID,
                    label = "Кнопки в рамці (👍 👎 ⋮)",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            // V138: two small lines — the song file and the video PulseDeck makes.
            add(
                ObjectInfo(
                    id = FILE_INFO_ID,
                    label = "Інфо про файл (пісня і відео)",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            // V108 (faric 21.10 03:46): the artist line can be hidden too.
            add(
                ObjectInfo(
                    id = "track_artist",
                    label = "Виконавець (під назвою пісні) · Artist line",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            // V101 (faric 21.10 01:40): the «Відтворення · reactive ON · PulseDeck HUD» line can be hidden.
            add(
                ObjectInfo(
                    id = "track_status",
                    label = "Рядок стану (Відтворення · reactive · HUD) · Status line",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            add(
                ObjectInfo(
                    id = "progress_time",
                    label = "Elapsed / total time",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
            add(
                ObjectInfo(
                    id = "progress_thumb",
                    label = "Progress thumb",
                    groupCode = PulseDeckSubsystem.TRACK_UI.code,
                    groupLabel = subsystemLabel(PulseDeckSubsystem.TRACK_UI),
                ),
            )
        }
            .distinctBy { item ->
                item.id
            }
            .sortedWith(
                compareBy<ObjectInfo> { item ->
                    item.groupCode
                }.thenBy { item ->
                    item.label
                },
            )

    private fun subsystemLabel(
        subsystem: PulseDeckSubsystem,
    ): String =
        when (subsystem) {
            PulseDeckSubsystem.ATMOSPHERE -> "6.0 · Atmosphere"
            PulseDeckSubsystem.REACTOR -> "6.1 · Reactor"
            PulseDeckSubsystem.TRACK_UI -> "6.2 · Track UI"
            PulseDeckSubsystem.TRANSPORT -> "6.3 · Transport"
            PulseDeckSubsystem.QUICK_ACTIONS -> "6.4 · Quick Actions"
            PulseDeckSubsystem.NAVIGATION -> "6.5 · Navigation"
        }

    private fun objectLabel(
        id: String,
    ): String =
        id
            .replace('_', ' ')
            .replaceFirstChar { ch ->
                ch.uppercaseChar()
            }

    /**
     * Clock for the scrolling («стрічка») title lines. Live screen: null → real time.
     * MP4 export sets the frame time so every export of the same clip looks the same.
     */
    var marqueeClockMs: Long? = null

    /**
     * V78 (faric 19.10 23:40: «червоне сердечко класне — на головній теж»): the song is in
     * favourites → a red heart sits in the ♡ button, the same ❤ as in the track list.
     */
    /** Where an object (e.g. «back») was last drawn, in this view's coordinates. */
    fun objectRect(id: String): RectF? = resolvedRects[id]?.let { RectF(it) }

    /** V184: where a part really is on screen — with faric's move / size from «Розкладка». */
    fun objectScreenRect(id: String): RectF? {
        val rect = objectRect(id) ?: return null
        val group = LayoutGroups.ALL.firstOrNull { id in it.members } ?: return rect
        matrixFor(group.id, width.toFloat(), height.toFloat())?.mapRect(rect)
        return rect
    }

    var favoriteActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            postInvalidateOnAnimation()
        }

    private val favoritePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
    private var marqueeRunning = false

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

    fun setChromeVisibility(
        transportVisible: Boolean,
        quickActionsVisible: Boolean,
    ) {
        transportAlpha = if (transportVisible) 1f else 0f
        quickActionsAlpha = if (quickActionsVisible) 1f else 0f
        invalidate()
    }

    fun setHudLayersVisible(
        visible: Boolean,
    ) {
        alpha =
            if (visible) {
                1f
            } else {
                0f
            }
        isEnabled = visible
        isClickable = visible
    }

    fun setEqualizerVisible(
        visible: Boolean,
    ) {
        equalizerVisible = visible
        invalidate()
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
        pressFeedbackAnimator?.cancel()
        pressFeedbackAnimator = null
        super.onDetachedFromWindow()
    }

    private fun animatePressFeedback(
        action: String?,
        targetScale: Float,
        durationMs: Long,
        clearWhenDone: Boolean,
    ) {
        if (action == null || action == "seek") {
            return
        }

        pressFeedbackAnimator?.cancel()
        feedbackAction = action

        pressFeedbackAnimator =
            ValueAnimator
                .ofFloat(
                    feedbackScale,
                    targetScale,
                )
                .apply {
                    duration = durationMs
                    addUpdateListener { animator ->
                        feedbackScale =
                            animator.animatedValue
                                as Float
                        feedbackGlow =
                            when {
                                feedbackScale < 1f ->
                                    (
                                        (1f - feedbackScale) /
                                            0.20f
                                        )
                                        .coerceIn(0f, 1f)

                                else ->
                                    (
                                        (feedbackScale - 1f) /
                                            0.50f
                                        )
                                        .coerceIn(0f, 1f)
                            }
                        invalidate()
                    }
                    if (clearWhenDone) {
                        addListener(
                            object :
                                android.animation
                                    .AnimatorListenerAdapter() {
                                override fun onAnimationEnd(
                                    animation: android.animation.Animator,
                                ) {
                                    if (feedbackAction == action) {
                                        feedbackAction = null
                                        feedbackScale = 1f
                                        feedbackGlow = 0f
                                        invalidate()
                                    }
                                }
                            },
                        )
                    }
                    start()
                }
    }

    private fun startPressFeedback(
        action: String?,
    ) {
        animatePressFeedback(
            action = action,
            targetScale = 0.80f,
            durationMs = 90L,
            clearWhenDone = false,
        )
    }

    private fun cancelPressFeedback(
        action: String?,
    ) {
        animatePressFeedback(
            action = action,
            targetScale = 1.00f,
            durationMs = 90L,
            clearWhenDone = true,
        )
    }

    private fun startReleaseFeedback(
        action: String?,
    ) {
        if (action == null || action == "seek") {
            return
        }

        pressFeedbackAnimator?.cancel()
        feedbackAction = action

        pressFeedbackAnimator =
            ValueAnimator
                .ofFloat(
                    feedbackScale,
                    1.50f,
                    1.00f,
                )
                .apply {
                    duration = 290L
                    addUpdateListener { animator ->
                        feedbackScale =
                            animator.animatedValue
                                as Float
                        feedbackGlow =
                            when {
                                feedbackScale < 1f ->
                                    (
                                        (1f - feedbackScale) /
                                            0.20f
                                        )
                                        .coerceIn(0f, 1f)

                                else ->
                                    (
                                        (feedbackScale - 1f) /
                                            0.50f
                                        )
                                        .coerceIn(0f, 1f)
                            }
                        invalidate()
                    }
                    addListener(
                        object :
                            android.animation
                                .AnimatorListenerAdapter() {
                            override fun onAnimationEnd(
                                animation: android.animation.Animator,
                            ) {
                                if (feedbackAction == action) {
                                    feedbackAction = null
                                    feedbackScale = 1f
                                    feedbackGlow = 0f
                                    invalidate()
                                }
                            }
                        },
                    )
                    start()
                }
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)
        marqueeRunning = false
        activeLayout =
            previewLayout
                ?: LayoutProfileChoice.forView(width, height, forcedLayoutProfile, savedLayouts)

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

        // V179 (відео faric 23.10 21:47: «на центрі кубик спершу смужкою вгорі, потім розтягується»): a part
        // moved / resized in «Розкладка» is scaled round its own place, and that place is only known after
        // one frame has been drawn. The very first frame scaled the frame round the screen centre — it flew
        // off the top and got squeezed into a strip — and stayed until the next redraw (~0,6 s). Now the
        // first frame is worked out once on an empty canvas (nothing is shown), then drawn for real.
        if (groupBounds.isEmpty()) {
            dryPass = true
            try {
                drawFrame(Canvas(), w, h)
            } finally {
                dryPass = false
            }
        }
        drawFrame(canvas, w, h)
    }

    private fun drawFrame(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        beat *=
            if (playing) {
                0.90f
            } else {
                0.82f
            }

        if (!transparentBackground) {
            canvas.drawColor(
                Color.rgb(
                    0,
                    4,
                    8,
                ),
            )
        }

        hitTargets.clear()
        resolvedRects.clear()

        timedPart(PART_BACK) {
            drawSubsystemLayers(
                canvas = canvas,
                subsystem = PulseDeckSubsystem.ATMOSPHERE,
                w = w,
                h = h,
            )
            drawSubsystemLayers(
                canvas = canvas,
                subsystem = PulseDeckSubsystem.REACTOR,
                w = w,
                h = h,
            )
        }

        val coverParts = drawPartNs[PART_GLOW] + drawPartNs[PART_EDGE] + drawPartNs[PART_PICTURE] + drawPartNs[PART_TEXT]
        val coverStarted = System.nanoTime()
        withGroup(canvas, "cover", w, h) {
            drawCoverBox(
                canvas = canvas,
                w = w,
                h = h,
            )
        }
        val coverInside = drawPartNs[PART_GLOW] + drawPartNs[PART_EDGE] + drawPartNs[PART_PICTURE] + drawPartNs[PART_TEXT] - coverParts
        drawPartNs[PART_COVER_REST] += (System.nanoTime() - coverStarted - coverInside).coerceAtLeast(0L)

        val panelsStarted = System.nanoTime()
        drawSubsystemLayers(
            canvas = canvas,
            subsystem = PulseDeckSubsystem.TRACK_UI,
            w = w,
            h = h,
        )
        drawSubsystemLayers(
            canvas = canvas,
            subsystem = PulseDeckSubsystem.TRANSPORT,
            w = w,
            h = h,
        )
        drawSubsystemLayers(
            canvas = canvas,
            subsystem = PulseDeckSubsystem.QUICK_ACTIONS,
            w = w,
            h = h,
        )
        drawSubsystemLayers(
            canvas = canvas,
            subsystem = PulseDeckSubsystem.NAVIGATION,
            w = w,
            h = h,
        )
        drawPartNs[PART_PANELS] += System.nanoTime() - panelsStarted

        if (controlsAlpha > 0.01f) {
            timedPart(PART_TITLE) {
                drawDynamicText(
                    canvas = canvas,
                    w = w,
                    h = h,
                )
            }
            timedPart(PART_PROGRESS) {
                withGroup(canvas, "progress", w, h) {
                    if (equalizerVisible) {
                        drawFrequencySpectrum(
                            canvas = canvas,
                            w = w,
                            h = h,
                        )
                    }
                    drawProgressThumb(
                        canvas = canvas,
                        w = w,
                        h = h,
                    )
                }
            }
            timedPart(PART_INFO) { withGroup(canvas, "info", w, h) { drawFileInfo(canvas, w, h) } }
        }
        rememberGroupBounds()

        if (
            playing ||
            amplitude > 0.01f ||
            beat > 0.01f ||
            marqueeRunning ||
            favoriteActive ||
            controlsAnimator?.isRunning == true
        ) {
            postInvalidateOnAnimation()
        }
    }

    private fun subsystemFor(
        layerId: String,
    ): PulseDeckSubsystem =
        when (layerId) {
            "energy_waves",
            "particles_orange_left",
            "particles_blue_left",
            "particles_blue_right",
            "particles_orange_right",
            -> PulseDeckSubsystem.ATMOSPHERE

            "reactor_energy_ring",
            "hero_frame",
            "hero_core",
            -> PulseDeckSubsystem.REACTOR

            "favorite",
            "track_more",
            "waveform",
            "progress_line",
            -> PulseDeckSubsystem.TRACK_UI

            "transport_rail",
            "shuffle",
            "previous",
            "play_pause",
            "next",
            "repeat",
            -> PulseDeckSubsystem.TRANSPORT

            "quick_rail",
            "theme",
            "board",
            "visualizer",
            "export",
            -> PulseDeckSubsystem.QUICK_ACTIONS

            "back",
            "menu",
            -> PulseDeckSubsystem.NAVIGATION

            else -> PulseDeckSubsystem.TRACK_UI
        }

    private fun drawSubsystemLayers(
        canvas: Canvas,
        subsystem: PulseDeckSubsystem,
        w: Float,
        h: Float,
    ) {
        layers
            .asSequence()
            .filter { layer ->
                subsystemFor(layer.id) == subsystem
            }
            .sortedBy { layer ->
                layer.z
            }
            .forEach { layer ->
                withGroup(canvas, LayoutGroups.groupOf(layer.id), w, h) {
                    drawLayer(
                        canvas = canvas,
                        layer = layer,
                        w = w,
                        h = h,
                    )
                }
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

    // V174 крок 3 — ready pixel copies for the video (software canvas); see [SoftwareBlitMath].
    private val blitMatrix = android.graphics.Matrix()
    private val blitValues = FloatArray(9)
    private val pixelCopyPaint = Paint()
    private val panelCopyMakePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * V174 крок 3: set only by the video export ([com.saney.musicvisualizer.export.CompositionExportRenderer]).
     * The screen never keeps these copies (memory), even when something draws it on a software canvas.
     */
    var videoPixelCopies = false

    private var dryPass = false

    /** The software canvas's whole stretch-and-move, or null on the screen / when the picture is turned. */
    private fun softwareAxisValues(canvas: Canvas): FloatArray? {
        if (!videoPixelCopies || dryPass || canvas.isHardwareAccelerated) return null
        @Suppress("DEPRECATION")
        canvas.getMatrix(blitMatrix)
        blitMatrix.getValues(blitValues)
        return if (SoftwareBlitMath.isAxisAligned(blitValues)) blitValues else null
    }

    /** Draws in the picture's own pixels (the clip already set stays). */
    private inline fun drawOnPixels(
        canvas: Canvas,
        block: () -> Unit,
    ) {
        val save = canvas.save()
        canvas.setMatrix(null)
        try {
            block()
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private data class PanelCopyKey(
        val from: Bitmap,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val width: Int,
        val height: Int,
    )

    private val panelCopies = LinkedHashMap<PanelCopyKey, Bitmap>(32, 0.75f, true)
    private var panelCopiesBytes = 0L
    private val panelLastSize = HashMap<String, Long>()

    /**
     * V174 крок 3 (замір: «рамка: панелі» 7–8 мс на кадр відео): a PNG panel / button is stretched to its
     * place once and then copied 1:1. Only when its size held still for two frames — a part that pulses
     * with the beat keeps the old (smooth) way.
     */
    private fun drawLayerOnPixels(
        canvas: Canvas,
        layerId: String,
        bitmap: Bitmap,
        sourceRect: Rect,
        drawRect: RectF,
    ): Boolean {
        val values = softwareAxisValues(canvas) ?: return false
        val px = SoftwareBlitMath.deviceRect(values, drawRect.left, drawRect.top, drawRect.right, drawRect.bottom) ?: return false
        val bytes = SoftwareBlitMath.bytesOf(px.width, px.height, 4)
        if (bytes > SoftwareBlitMath.MAX_ENTRY_BYTES) return false
        val size = (px.width.toLong() shl 32) or px.height.toLong()
        val steady = panelLastSize.put(layerId, size) == size
        val key = PanelCopyKey(bitmap, sourceRect.left, sourceRect.top, sourceRect.right, sourceRect.bottom, px.width, px.height)
        var copy = panelCopies[key]?.takeIf { !it.isRecycled }
        if (copy == null) {
            if (!steady) return false
            copy =
                runCatching {
                    Bitmap.createBitmap(px.width, px.height, Bitmap.Config.ARGB_8888).also { out ->
                        out.eraseColor(Color.TRANSPARENT)
                        Canvas(out).drawBitmap(bitmap, sourceRect, Rect(0, 0, px.width, px.height), panelCopyMakePaint)
                    }
                }.getOrNull() ?: return false
            panelCopies[key] = copy
            panelCopiesBytes += bytes
            val oldest = panelCopies.entries.iterator()
            while (panelCopiesBytes > SoftwareBlitMath.PANEL_BUDGET_BYTES && panelCopies.size > 1 && oldest.hasNext()) {
                val entry = oldest.next()
                if (entry.key == key) continue
                panelCopiesBytes -= SoftwareBlitMath.bytesOf(entry.value.width, entry.value.height, 4)
                oldest.remove()
            }
        }
        val drawn: Bitmap = copy
        drawOnPixels(canvas) { canvas.drawBitmap(drawn, px.left.toFloat(), px.top.toFloat(), bitmapPaint) }
        return true
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: SkinLayer,
        w: Float,
        h: Float,
    ) {
        val objectVisible =
            isObjectVisible(
                layer.id,
            )

        val isChrome =
            layer.z >=
                CONTROL_Z_MIN

        val sectionAlpha =
            when (layer.id) {
                "transport_rail",
                "shuffle",
                "previous",
                "play_pause",
                "next",
                "repeat",
                -> transportAlpha

                "quick_rail",
                "theme",
                "board",
                "visualizer",
                "export",
                -> quickActionsAlpha

                else -> 1f
            }

        val chromeAlpha =
            if (isChrome) {
                controlsAlpha * sectionAlpha
            } else {
                1f
            }

        if (chromeAlpha <= 0.01f) {
            return
        }

        if (layer.id == "waveform") return
        if (layer.id == "progress_line") {
            if (objectVisible) {
                drawProgressLine(canvas, w, h, layer)
            }
            return
        }

        val effectivePlaying =
            if (layer.id == "play_pause") {
                editorPlayPausePreviewPlaying ?: playing
            } else {
                playing
            }

        val asset =
            if (
                layer.id ==
                    "play_pause" &&
                effectivePlaying &&
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
            if (layer.id == "play_pause") {
                if (effectivePlaying) pauseEditorOffset else playEditorOffset
            } else {
                editorOffsets[layer.id] ?: (0f to 0f)
            }
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

        // Keep geometry stable even when an individual object is hidden.
        // Child controls use their parent rect for local coordinates, so a hidden
        // rail must still resolve its layout or its children would visibly jump.
        resolvedRects[layer.id] =
            RectF(rect)
        if (layer.id == "play_pause") {
            resolvedRects[
                if (effectivePlaying) {
                    "pause"
                } else {
                    "play"
                }
            ] = RectF(rect)
        }

        if (!objectVisible) {
            return
        }

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
                    // V132: chrome layers get it through controlsAlpha; the rest — here.
                    (if (isChrome) 1f else groupAlphaMul) *
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

        val hasPressFeedback =
            layer.action != null &&
                layer.action == feedbackAction &&
                layer.action != "seek"

        val objectScale =
            if (hasPressFeedback) {
                feedbackScale
            } else {
                1f
            }

        val drawRect =
            if (hasPressFeedback) {
                val cx = rect.centerX()
                val cy = rect.centerY()
                val halfWidth =
                    rect.width() *
                        objectScale *
                        0.5f
                val halfHeight =
                    rect.height() *
                        objectScale *
                        0.5f

                RectF(
                    cx - halfWidth,
                    cy - halfHeight,
                    cx + halfWidth,
                    cy + halfHeight,
                )
            } else {
                rect
            }

        if (hasPressFeedback) {
            val glowPad =
                maxOf(
                    drawRect.width(),
                    drawRect.height(),
                ) *
                    (
                        0.12f +
                            feedbackGlow *
                            0.10f
                        )

            val neonGlowRect =
                RectF(
                    drawRect.left - glowPad,
                    drawRect.top - glowPad,
                    drawRect.right + glowPad,
                    drawRect.bottom + glowPad,
                )

            val neonGlowPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        Color.rgb(
                            44,
                            226,
                            255,
                        )
                    alpha =
                        (
                            28f +
                                feedbackGlow *
                                60f
                            )
                            .toInt()
                            .coerceIn(
                                0,
                                100,
                            )
                    style = Paint.Style.FILL
                }

            canvas.drawOval(
                neonGlowRect,
                neonGlowPaint,
            )
        }

        if (!drawLayerOnPixels(canvas, layer.id, bitmap, sourceRect, drawRect)) {
            canvas.drawBitmap(
                bitmap,
                sourceRect,
                drawRect,
                bitmapPaint,
            )
        }

        if (hasPressFeedback) {
            // Keep the existing ring and add only the requested stronger spring/glow.
            val feedbackPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(90, 232, 255)
                    alpha =
                        (
                            72f +
                                feedbackGlow *
                                65f
                            )
                            .toInt()
                            .coerceIn(
                                0,
                                150,
                            )
                    style = Paint.Style.STROKE
                    strokeWidth =
                        maxOf(
                            2f,
                            w *
                                0.0035f,
                        )
                }
            val glowPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha =
                        (
                            28f +
                                feedbackGlow *
                                20f
                            )
                            .toInt()
                            .coerceIn(
                                0,
                                64,
                            )
                    style = Paint.Style.FILL
                }

            canvas.drawOval(
                drawRect,
                glowPaint,
            )
            canvas.drawOval(
                drawRect,
                feedbackPaint,
            )
        }

        bitmapPaint.xfermode = null

        val action =
            layer.action

        if (
            action != null &&
            controlsVisible &&
            (if (controlsTouchAlpha >= 0f) controlsTouchAlpha else controlsAlpha) >= 0.35f
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

        withGroup(canvas, "header", w, h) {
        if (isObjectVisible("header_title")) {
            // V86 / V76: logo + "PulseDeck", the pair centred where the title was.
            val headerSize = w * 0.052f
            textPaint.textSize = headerSize
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerText = "PulseDeck"
            val textWidth = textPaint.measureText(headerText)
            val logoSize = headerSize * 1.45f
            val logoGap = headerSize * 0.35f
            val groupLeft = headerX - (logoSize + logoGap + textWidth) / 2f
            val logoTop = headerY - headerSize * 0.36f - logoSize / 2f
            headerLogo?.let { logo ->
                logo.setBounds(
                    groupLeft.toInt(),
                    logoTop.toInt(),
                    (groupLeft + logoSize).toInt(),
                    (logoTop + logoSize).toInt(),
                )
                logo.alpha = (alpha * groupAlphaMul).toInt().coerceIn(0, 255)
                logo.draw(canvas)
            }
            drawCenteredText(
                canvas = canvas,
                text = headerText,
                x = groupLeft + logoSize + logoGap + textWidth / 2f,
                y = headerY,
                size = headerSize,
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
        }
        }

        val trackOffset =
            editorOffsets["track_info"]
                ?: (0f to 0f)
        val trackX =
            w *
                (0.5f + trackOffset.first)
        val trackDy =
            h *
                trackOffset.second

        withGroup(canvas, "track", w, h) {
        drawFavoriteHeart(canvas)

        if (isObjectVisible("track_info")) {
            // V64 (faric 19.10 23:17): the track lines live between ♡ and ⋮ with a
            // 20 dp gap on each side; a line that does not fit scrolls like a ribbon.
            marqueeRunning = false
            val gap = 20f * resources.displayMetrics.density
            val shiftX = trackX - w * 0.5f
            val favoriteRight = resolvedRects["favorite"]?.right ?: (w * 0.141f)
            val moreLeft = resolvedRects["track_more"]?.left ?: (w * 0.859f)
            val lineLeft = favoriteRight + gap + shiftX
            val lineRight = (moreLeft - gap + shiftX).coerceAtLeast(lineLeft + w * 0.2f)

            // V69: the three lines take turns — only one moves at a time (TrackMarquee).
            val lines =
                listOfNotNull(
                    TrackLine(title, h * 0.485f + trackDy, w * 0.052f, Color.WHITE, true),
                    TrackLine(artist, h * 0.515f + trackDy, w * 0.037f, Color.rgb(190, 202, 212), false)
                        .takeIf { isObjectVisible("track_artist") },
                    TrackLine(status, h * 0.538f + trackDy, w * 0.026f, Color.rgb(164, 182, 194), false)
                        .takeIf { isObjectVisible("track_status") },
                )
            val widths =
                lines.map { line ->
                    applyTrackFont(line)
                    textPaint.measureText(line.text)
                }
            val offsets =
                TrackMarquee.offsets(
                    textWidthsPx = widths,
                    availablePx = lineRight - lineLeft,
                    density = resources.displayMetrics.density,
                    clockMs = marqueeClockMs ?: SystemClock.uptimeMillis(),
                )
            lines.forEachIndexed { index, line ->
                drawTrackLine(canvas, line, lineLeft, lineRight, offsets[index])
            }

            resolvedRects["track_info"] =
                RectF(
                    trackX - w * 0.30f,
                    h * 0.455f + trackDy,
                    trackX + w * 0.30f,
                    h * 0.548f + trackDy,
                )
        }
        }

        val progressOffset =
            editorOffsets["progress_group"]
                ?: (0f to 0f)
        val progressDx =
            w *
                progressOffset.first
        val progressDy =
            h *
                progressOffset.second

        withGroup(canvas, "progress", w, h) {
        if (isObjectVisible("progress_time")) {
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
        }
        }

        textPaint.alpha = 255
    }

    /**
     * V138 (faric 22.10 22:50, як у Poweramp «48 КГЦ 320 КБ/С MP3»): the song file and the video —
     * two small soft-blue lines between the two times. Own part in «Розкладка» («Інфо про файл»).
     */
    var fileInfoLines: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private fun drawFileInfo(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        if (!isObjectVisible(FILE_INFO_ID)) return
        val lines = fileInfoLines.filter { it.isNotBlank() }.take(2)
        if (lines.isEmpty()) return
        val size = w * 0.023f
        val step = size * 1.3f
        val firstY = h * 0.618f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = size
        var widest = 0f
        lines.forEachIndexed { index, line ->
            widest = max(widest, textPaint.measureText(line))
            drawCenteredText(canvas, line, w / 2f, firstY + index * step, size, Color.argb(200, 150, 200, 235), false)
        }
        resolvedRects[FILE_INFO_ID] =
            RectF(w / 2f - widest / 2f, firstY - size, w / 2f + widest / 2f, firstY + (lines.size - 1) * step + size * 0.3f)
    }

    private fun drawFavoriteHeart(canvas: Canvas) {
        if (!favoriteActive || !isObjectVisible("favorite")) return
        val rect = resolvedRects["favorite"] ?: return
        // «Живеньке» (faric 20.10 05:36): breathes softly and gives a small kick on the bass.
        val clock = marqueeClockMs ?: SystemClock.uptimeMillis()
        val breath = 0.5f + 0.5f * kotlin.math.sin(clock / 1_100.0 * 2.0 * Math.PI).toFloat()
        val pulse = 1f + 0.06f * breath + 0.08f * bass.coerceIn(0f, 1f)
        val size = minOf(rect.width(), rect.height()) * 0.42f * pulse
        favoritePaint.textSize = size
        favoritePaint.alpha = (controlsAlpha * 255f).toInt().coerceIn(0, 255)
        val baseline = rect.centerY() - (favoritePaint.descent() + favoritePaint.ascent()) / 2f
        canvas.drawText("\u2764\uFE0F", rect.centerX(), baseline, favoritePaint)
    }

    private data class TrackLine(
        val text: String,
        val y: Float,
        val size: Float,
        val color: Int,
        val bold: Boolean,
    )

    private fun applyTrackFont(line: TrackLine) {
        textPaint.textSize = line.size
        textPaint.typeface =
            Typeface.create(
                Typeface.DEFAULT,
                if (line.bold) Typeface.BOLD else Typeface.NORMAL,
            )
    }

    /** One track line: centred when it fits; otherwise drawn from [left] shifted by [offset], clipped. */
    private fun drawTrackLine(
        canvas: Canvas,
        line: TrackLine,
        left: Float,
        right: Float,
        offset: Float?,
    ) {
        if (offset == null) {
            drawCenteredText(canvas, line.text, (left + right) / 2f, line.y, line.size, line.color, line.bold)
            return
        }
        marqueeRunning = true
        applyTrackFont(line)
        textPaint.color = line.color
        textPaint.alpha = (Color.alpha(line.color) * groupAlphaMul).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.clipRect(left, line.y - line.size * 1.25f, right, line.y + line.size * 0.45f)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(line.text, left - offset, line.y, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.restore()
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
        textPaint.alpha = (Color.alpha(color) * groupAlphaMul).toInt().coerceIn(0, 255)
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

    private fun drawFrequencySpectrum(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        if (!isObjectVisible("waveform")) return
        if (spectrum.isEmpty()) return

        val waveformOffset = editorOffsets["waveform"] ?: (0f to 0f)
        val centerX = w * 0.5f + waveformOffset.first * w
        val baseline = h * 0.585f + waveformOffset.second * h
        val halfWidth = w * 0.445f
        val maxHeight = h * 0.058f
        val sideBands = 36
        val slot = halfWidth / sideBands
        val barWidth = (slot * 0.28f).coerceAtLeast(1f)
        val sourceCount = spectrum.size

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 72, 40)
            alpha = (controlsAlpha * 42f).toInt().coerceIn(0, 255)
        }

        fun sourceValue(sideIndex: Int): Float {
            if (sourceCount == 0) return 0f
            val normalized = sideIndex.toFloat() / (sideBands - 1).coerceAtLeast(1)
            val sourceIndex =
                (normalized * (sourceCount - 1))
                    .toInt()
                    .coerceIn(0, sourceCount - 1)
            return spectrum[sourceIndex].coerceIn(0f, 1f)
        }

        for (index in 0 until sideBands) {
            val value = sourceValue(index)
            val shaped = value * value * 0.72f + value * 0.28f
            val barHeight = (maxHeight * shaped).coerceAtLeast(h * 0.0025f)
            val top = baseline - barHeight
            val distance = slot * (index + 0.55f)
            val gradient =
                LinearGradient(
                    0f,
                    baseline,
                    0f,
                    top,
                    intArrayOf(
                        Color.rgb(245, 252, 255),
                        Color.rgb(255, 190, 105),
                        Color.rgb(255, 58, 42),
                    ),
                    floatArrayOf(0f, 0.58f, 1f),
                    Shader.TileMode.CLAMP,
                )
            val corePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradient
                    alpha = (controlsAlpha * 245f).toInt().coerceIn(0, 255)
                }

            for (direction in intArrayOf(-1, 1)) {
                val cx = centerX + distance * direction
                canvas.drawRoundRect(
                    cx - barWidth * 1.2f,
                    top - h * 0.0015f,
                    cx + barWidth * 1.2f,
                    baseline,
                    barWidth,
                    barWidth,
                    glowPaint,
                )
                canvas.drawRoundRect(
                    cx - barWidth * 0.5f,
                    top,
                    cx + barWidth * 0.5f,
                    baseline,
                    barWidth * 0.5f,
                    barWidth * 0.5f,
                    corePaint,
                )
            }
        }

        resolvedRects["waveform"] =
            RectF(
                centerX - halfWidth,
                baseline - maxHeight,
                centerX + halfWidth,
                baseline,
            )
    }

    private fun drawProgressLine(
        canvas: Canvas,
        w: Float,
        h: Float,
        layer: SkinLayer,
    ) {
        val progressOffset = editorOffsets["progress_group"] ?: (0f to 0f)
        val left = w * 0.055f + progressOffset.first * w
        val right = w * 0.945f + progressOffset.first * w
        val cy = h * 0.595f + progressOffset.second * h
        val stroke = w * 0.007f

        val glowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    LinearGradient(
                        left,
                        cy,
                        right,
                        cy,
                        Color.rgb(0, 216, 255),
                        Color.rgb(255, 145, 24),
                        Shader.TileMode.CLAMP,
                    )
                alpha = (controlsAlpha * 70f).toInt().coerceIn(0, 255)
                strokeWidth = stroke * 2.2f
                strokeCap = Paint.Cap.ROUND
            }
        val corePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    LinearGradient(
                        left,
                        cy,
                        right,
                        cy,
                        Color.rgb(125, 245, 255),
                        Color.rgb(255, 154, 28),
                        Shader.TileMode.CLAMP,
                    )
                alpha = (controlsAlpha * 255f).toInt().coerceIn(0, 255)
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }

        canvas.drawLine(left, cy, right, cy, glowPaint)
        canvas.drawLine(left, cy, right, cy, corePaint)

        val rect =
            RectF(
                left,
                cy - stroke * 0.5f,
                right,
                cy + stroke * 0.5f,
            )
        resolvedRects[layer.id] = RectF(rect)

        val extra = resources.displayMetrics.density * 18f
        hitTargets.add(
            HitTarget(
                action = "seek",
                z = layer.z,
                rect = RectF(rect.left, rect.top - extra, rect.right, rect.bottom + extra),
            ),
        )
    }

    private fun drawProgressThumb(
        canvas: Canvas,
        w: Float,
        h: Float,
    ) {
        if (!isObjectVisible("progress_thumb")) return

        val left = w * 0.055f
        val right = w * 0.945f
        val progressOffset = editorOffsets["progress_group"] ?: (0f to 0f)
        val cx =
            left +
                (right - left) * progressFraction +
                progressOffset.first * w
        val cy =
            h * 0.595f +
                progressOffset.second * h
        val radius = w * 0.014f

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 216, 255)
            alpha = (controlsAlpha * 90f).toInt().coerceIn(0, 255)
        }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 216, 255)
            alpha = (controlsAlpha * 255f).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = w * 0.004f
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (controlsAlpha * 255f).toInt().coerceIn(0, 255)
        }

        canvas.drawCircle(cx, cy, radius * 1.65f, outerPaint)
        canvas.drawCircle(cx, cy, radius, corePaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)

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
                // V120: a tap just beside a button (between ⏮ and ⏯) still counts for the nearest one.
                val target =
                    findTarget(
                        event.x,
                        event.y,
                    ) ?: findNearTarget(event.x, event.y)

                pressedTargetRect = target?.rect?.let { RectF(it) }
                pressedAction =
                    target?.action
                pressedOriginAction =
                    target?.action
                startPressFeedback(
                    target?.action,
                )
                invalidate()

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
                } else if (pressedOriginAction != null) {
                    val hoveredAction =
                        if (stillOnPressed(event.x, event.y)) {
                            pressedOriginAction
                        } else {
                            findTarget(
                                event.x,
                                event.y,
                            )?.action
                        }
                    val nextPressed =
                        pressedOriginAction
                            .takeIf { it == hoveredAction }

                    if (nextPressed != pressedAction) {
                        val previousPressed =
                            pressedAction
                        pressedAction =
                            nextPressed

                        if (nextPressed != null) {
                            startPressFeedback(
                                nextPressed,
                            )
                        } else {
                            cancelPressFeedback(
                                previousPressed
                                    ?: pressedOriginAction,
                            )
                        }
                        invalidate()
                    }
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
                    val action =
                        if (stillOnPressed(event.x, event.y)) {
                            pressedOriginAction
                        } else {
                            findTarget(
                                event.x,
                                event.y,
                            )?.action
                        }

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

                val releasedAction =
                    pressedOriginAction
                pressedAction = null
                pressedOriginAction = null
                scrubbing = false
                startReleaseFeedback(
                    releasedAction,
                )
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                val cancelledAction =
                    pressedOriginAction
                pressedAction = null
                pressedOriginAction = null
                scrubbing = false
                cancelPressFeedback(
                    cancelledAction,
                )
                invalidate()
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

    /**
     * V120 (faric 22.10 00:34, запис: кнопки «то працюють, то зависають на 2–5 с»). Розбір кадрів:
     * палець тиснув край ⏮ і за 0,1–0,2 с з'їжджав на кілька міліметрів убік — з кнопки — і натиск
     * скасовувався (кнопка світилась, але пісня не мінялась). Тепер натиснута кнопка «тримає» палець
     * у межах [PRESS_TOLERANCE_DP] навколо себе, а дотик поруч з кнопкою дістається найближчій.
     */
    private var pressedTargetRect: RectF? = null

    private fun stillOnPressed(
        x: Float,
        y: Float,
    ): Boolean {
        val rect = pressedTargetRect ?: return false
        if (pressedOriginAction == null || pressedOriginAction == "seek") return false
        val slack = PRESS_TOLERANCE_DP * resources.displayMetrics.density
        return x >= rect.left - slack && x <= rect.right + slack && y >= rect.top - slack && y <= rect.bottom + slack
    }

    private fun findNearTarget(
        x: Float,
        y: Float,
    ): HitTarget? {
        val reach = NEAR_TAP_DP * resources.displayMetrics.density
        return hitTargets
            .asSequence()
            .filter { it.action != "seek" }
            .map { target ->
                val dx = maxOf(target.rect.left - x, 0f, x - target.rect.right)
                val dy = maxOf(target.rect.top - y, 0f, y - target.rect.bottom)
                target to (dx * dx + dy * dy)
            }
            .filter { (_, distance) -> distance <= reach * reach }
            .minByOrNull { (_, distance) -> distance }
            ?.first
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
        /** V120: a pressed button keeps the finger this far around it. */
        const val PRESS_TOLERANCE_DP = 28f

        /** V120: a tap this close to a button (but not on it) goes to that button. */
        const val NEAR_TAP_DP = 14f

        const val COVER_MENU_ID = "cover_menu"
        const val FILE_INFO_ID = "file_info"
        const val COVER_LIKE_ID = "cover_like"
        const val COVER_DISLIKE_ID = "cover_dislike"
        const val COVER_MORE_ID = "cover_more"
        const val COVER_BOX_ID = "cover_box"

        /** V174: the HUD parts timed for the export report (index = PART_*). */
        val DRAW_PART_NAMES =
            listOf(
                "фон і реактор",
                "рамка: світіння",
                "рамка: окантовка-EQ",
                "рамка: картинка",
                "рамка: текст і затемнення",
                "рамка: решта (бортик, 👍 👎 ⋮)",
                "панелі й кнопки",
                "назва й виконавець",
                "смуга, час, спектр",
                "інфо про файл",
            )
        private const val PART_BACK = 0
        private const val PART_GLOW = 1
        private const val PART_EDGE = 2
        private const val PART_PICTURE = 3
        private const val PART_TEXT = 4
        private const val PART_COVER_REST = 5
        private const val PART_PANELS = 6
        private const val PART_TITLE = 7
        private const val PART_PROGRESS = 8
        private const val PART_INFO = 9

        /** V167: the transparent edge round the picture of the story text. */
        private const val TEXT_BITMAP_PAD = 2

        private const val SKIN_ROOT =
            "pulsedeck_hud"

        private const val CONTROL_Z_MIN =
            40
    }
}
