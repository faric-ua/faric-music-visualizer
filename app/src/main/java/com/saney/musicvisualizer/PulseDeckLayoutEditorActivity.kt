package com.saney.musicvisualizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.export.ScreenPlacement
import com.saney.musicvisualizer.ui.LayoutAdjust
import com.saney.musicvisualizer.ui.LayoutBox
import com.saney.musicvisualizer.ui.LayoutGroup
import com.saney.musicvisualizer.ui.LayoutGroups
import com.saney.musicvisualizer.ui.LayoutProfileCodec
import com.saney.musicvisualizer.ui.LayoutProfileId
import com.saney.musicvisualizer.ui.LayoutProfileStore
import com.saney.musicvisualizer.ui.LayoutSnap
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * V104 / V114 / V119 «Утиліти → Розкладка екрана» (макет «Утиліти PulseDeck», екрани 2–4).
 * A small copy of the main screen; faric picks a part (обкладинка, ‹, ⋮, назва, панелі …), moves it
 * (arrows or a finger) and resizes it proportionally (−5 / −1 / +1 / +5 %), then «Зберегти» — the
 * main screen (or the MP4 for «Відео 9:16») uses it from then on. Nothing is saved until «Зберегти».
 *
 * V121 (faric 22.10 00:45): «Лупа» 1× / 2× / 3× — the copy grows around the chosen part;
 * «Прив'язка» — the part's centre sticks to the screen's centre lines and to other parts' centres,
 * the line lights up; a turned phone keeps the unsaved changes (they used to be lost).
 */
class PulseDeckLayoutEditorActivity : ComponentActivity() {
    private lateinit var profile: LayoutProfileId
    private lateinit var store: LayoutProfileStore
    private val working = mutableMapOf<String, LayoutAdjust>()
    private var selected: LayoutGroup = LayoutGroups.ALL.first()
    private var stepPx = 4
    private var dirty = false
    private var zoom = 1
    private var snapOn = true
    private var guideX: Float? = null
    private var guideY: Float? = null
    private var outsideFrame: Set<String> = emptySet()

    private lateinit var skin: PulseDeckMainSkinView
    private lateinit var overlay: EditorOverlay
    private lateinit var groupTitle: TextView
    private lateinit var sizeValue: TextView
    private lateinit var stepButton: TextView
    private lateinit var zoomButton: TextView
    private lateinit var snapButton: TextView
    private lateinit var warning: TextView
    private val chips = mutableMapOf<String, TextView>()
    private var virtualW = 1
    private var virtualH = 1
    private var previewScale = 1f
    private var boxW = 1
    private var boxH = 1

    /** Virtual (real-screen) px → preview px: x * viewScale + offsetX. */
    private var viewScale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile =
            runCatching { LayoutProfileId.valueOf(intent.getStringExtra(EXTRA_PROFILE).orEmpty()) }
                .getOrDefault(LayoutProfileId.PORTRAIT)
        store = LayoutProfileStore(this)
        if (savedInstanceState != null) {
            // V121: a turned phone keeps what faric did (it used to start from the saved set again).
            working.putAll(LayoutProfileCodec.decode(savedInstanceState.getString(STATE_WORKING)))
            selected = LayoutGroups.byId(savedInstanceState.getString(STATE_SELECTED).orEmpty()) ?: selected
            stepPx = savedInstanceState.getInt(STATE_STEP, stepPx)
            dirty = savedInstanceState.getBoolean(STATE_DIRTY, false)
            zoom = savedInstanceState.getInt(STATE_ZOOM, 1).coerceIn(1, 3)
            snapOn = savedInstanceState.getBoolean(STATE_SNAP, true)
        } else {
            working.putAll(store.load(profile))
        }

        val metrics = resources.displayMetrics
        val shortSide = min(metrics.widthPixels, metrics.heightPixels)
        val longSide = max(metrics.widthPixels, metrics.heightPixels)
        if (profile == LayoutProfileId.LANDSCAPE) {
            virtualW = longSide
            virtualH = shortSide
        } else {
            virtualW = shortSide
            virtualH = longSide
        }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(16))
            }
        val scroll =
            ScrollView(this).apply {
                setBackgroundColor(UtilityUi.BG)
                addView(root)
            }
        UtilityUi.padForSystemBars(scroll)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        header.addView(
            UtilityUi.button(this, "‹") { leave() }.apply { contentDescription = "Назад" },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(UtilityUi.text(this, "Розкладка · ${profile.label}", 18f, bold = true))
        titles.addView(
            UtilityUi.text(
                this,
                when (profile) {
                    LayoutProfileId.VIDEO_9X16 -> "Лише для MP4. Пунктир — що потрапить у кадр 9:16 «на весь кадр»."
                    LayoutProfileId.LANDSCAPE -> "Для лежачого телефона. Розкладка А (платівка зліва) — наступним кроком."
                    LayoutProfileId.PORTRAIT -> "Тягни частину пальцем або кнопками нижче."
                },
                12f,
                UtilityUi.MUTED,
            ),
        )
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        root.addView(header)

        // The preview: the real main-screen view at its full size, shown smaller (and zoomed by «Лупа»).
        val availableW = metrics.widthPixels - dp(24)
        val portraitPhone = metrics.heightPixels > metrics.widthPixels
        val availableH =
            (metrics.heightPixels * if (profile == LayoutProfileId.LANDSCAPE || !portraitPhone) 0.42f else 0.46f).toInt()
        previewScale = min(availableW.toFloat() / virtualW, availableH.toFloat() / virtualH)
        boxW = (virtualW * previewScale).toInt()
        boxH = (virtualH * previewScale).toInt()
        val previewBox =
            FrameLayout(this).apply {
                clipChildren = true
                background = UtilityUi.frame(Color.rgb(7, 12, 17), UtilityUi.PANEL_EDGE, 1, 4, context)
            }
        skin =
            PulseDeckMainSkinView(this, forceModularMode = true).apply {
                setPlaying(true)
                setPlaybackContent(
                    title = "Назва пісні",
                    artist = "Виконавець",
                    status = "Відтворення · reactive ON · PulseDeck HUD",
                    elapsed = "0:39",
                    total = "3:33",
                    progressFraction = 0.18f,
                )
                setControlsVisible(true, false)
                // The frame shows even without a picture, so it can be placed.
                coverFallback = PulseDeckMainSkinView.COVER_FALLBACK_PLACEHOLDER
                previewLayout = working.toMap()
                pivotX = 0f
                pivotY = 0f
                // Taps go to the editor, not to the player buttons.
                isClickable = false
                isEnabled = false
            }
        previewBox.addView(skin, FrameLayout.LayoutParams(virtualW, virtualH))
        overlay = EditorOverlay(this)
        previewBox.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        // The overlay lies on top of the preview and takes the finger (the player view would eat it).
        overlay.setOnTouchListener(DragToMove())
        root.addView(
            previewBox,
            LinearLayout.LayoutParams(boxW, boxH).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = dp(8)
            },
        )

        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        LayoutGroups.ALL.forEach { group ->
            val chip = UtilityUi.button(this, group.label) { select(group) }
            chip.textSize = 13f
            chips[group.id] = chip
            chipRow.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        }
        root.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(chipRow)
            },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) },
        )

        warning = UtilityUi.text(this, "", 13f, Color.rgb(255, 211, 196))
        warning.setPadding(dp(10), dp(6), dp(10), dp(6))
        warning.background = UtilityUi.frame(UtilityUi.RED_FILL, Color.rgb(122, 59, 42), 1, 10, this)
        warning.visibility = View.GONE
        root.addView(warning, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        groupTitle = UtilityUi.text(this, "", 15f, bold = true)
        root.addView(groupTitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        root.addView(UtilityUi.text(this, "Розмір", 12f, UtilityUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        listOf(-5, -1).forEach { step -> sizeRow.addView(UtilityUi.button(this, "$step") { resize(step) }, UtilityUi.weighted(context = this)) }
        sizeValue = UtilityUi.text(this, "100 %", 15f, bold = true).apply {
            gravity = android.view.Gravity.CENTER
            minHeight = dp(48)
            background = UtilityUi.frame(UtilityUi.BG, UtilityUi.CYAN, 1, 10, context)
        }
        sizeRow.addView(sizeValue, UtilityUi.weighted(context = this, weight = 1.3f))
        listOf(1, 5).forEach { step -> sizeRow.addView(UtilityUi.button(this, "+$step") { resize(step) }, UtilityUi.weighted(context = this)) }
        root.addView(sizeRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })

        root.addView(UtilityUi.text(this, "Місце", 12f, UtilityUi.MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        val moveRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        moveRow.addView(UtilityUi.button(this, "←") { move(-stepPx, 0) }.apply { contentDescription = "Вліво" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "↑") { move(0, -stepPx) }.apply { contentDescription = "Вгору" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "↓") { move(0, stepPx) }.apply { contentDescription = "Вниз" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "→") { move(stepPx, 0) }.apply { contentDescription = "Вправо" }, UtilityUi.weighted(context = this))
        stepButton = UtilityUi.button(this, "") { nextStep() }
        stepButton.textSize = 13f
        moveRow.addView(stepButton, UtilityUi.weighted(context = this, weight = 1.4f))
        root.addView(moveRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })

        val toolsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        zoomButton = UtilityUi.button(this, "") { nextZoom() }
        snapButton = UtilityUi.button(this, "") { toggleSnap() }
        toolsRow.addView(zoomButton, UtilityUi.weighted(context = this))
        toolsRow.addView(snapButton, UtilityUi.weighted(context = this))
        root.addView(toolsRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(UtilityUi.button(this, "Зберегти", UtilityUi.Kind.MAIN) { save() }, UtilityUi.weighted(context = this))
        actions.addView(UtilityUi.button(this, "Скинути частину") { resetSelected() }, UtilityUi.weighted(context = this))
        if (profile == LayoutProfileId.VIDEO_9X16) {
            actions.addView(UtilityUi.button(this, "Узяти з «Книжки»") { copyFromPortrait() }, UtilityUi.weighted(context = this))
        }
        root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        setContentView(scroll)
        // Back asks before dropping unsaved changes.
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = leave()
            },
        )
        select(selected)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_WORKING, LayoutProfileCodec.encode(working))
        outState.putString(STATE_SELECTED, selected.id)
        outState.putInt(STATE_STEP, stepPx)
        outState.putBoolean(STATE_DIRTY, dirty)
        outState.putInt(STATE_ZOOM, zoom)
        outState.putBoolean(STATE_SNAP, snapOn)
    }

    private fun leave() {
        if (!dirty) {
            finish()
            return
        }
        PulseDeckDialogs.showConfirm(
            context = this,
            title = "Вийти без збереження?",
            message = "Зміни розкладки «${profile.label}» не збережені.",
            positiveLabel = "Вийти",
            negativeLabel = "Залишитись",
            destructive = true,
        ) { finish() }
    }

    private fun select(group: LayoutGroup) {
        selected = group
        guideX = null
        guideY = null
        refreshLabels()
    }

    private fun current(): LayoutAdjust = working[selected.id] ?: LayoutAdjust()

    private fun update(
        next: LayoutAdjust,
        markDirty: Boolean = true,
    ) {
        if (next.isIdentity) working.remove(selected.id) else working[selected.id] = next
        if (markDirty) dirty = true
        skin.previewLayout = working.toMap()
        refreshLabels()
    }

    private fun resize(percent: Int) = update(current().resized(percent))

    private fun move(
        dxPx: Int,
        dyPx: Int,
    ) {
        update(current().moved(dxPx.toFloat() / virtualW, dyPx.toFloat() / virtualH))
        // Arrows are exact — no pulling; the line only shows when the centre sits on one.
        showGuidesExact()
    }

    private fun nextStep() {
        stepPx =
            when (stepPx) {
                1 -> 4
                4 -> 16
                else -> 1
            }
        refreshLabels()
    }

    private fun nextZoom() {
        zoom = if (zoom >= 3) 1 else zoom + 1
        refreshLabels()
    }

    private fun toggleSnap() {
        snapOn = !snapOn
        guideX = null
        guideY = null
        refreshLabels()
    }

    private fun resetSelected() = update(LayoutAdjust())

    private fun copyFromPortrait() {
        working.clear()
        working.putAll(store.load(LayoutProfileId.PORTRAIT))
        dirty = true
        skin.previewLayout = working.toMap()
        refreshLabels()
    }

    private fun save() {
        store.save(profile, working)
        dirty = false
        PulseDeckDialogs.showMessage(
            context = this,
            title = "Збережено",
            message =
                if (profile == LayoutProfileId.VIDEO_9X16) {
                    "Розкладка «Відео 9:16» збережена — наступний MP4 її візьме."
                } else {
                    "Розкладка «${profile.label}» збережена — головний екран її вже показує."
                },
            primaryLabel = "OK",
        )
    }

    private fun refreshLabels() {
        val adjust = current()
        chips.forEach { (id, chip) ->
            UtilityUi.style(
                chip,
                when {
                    id == selected.id -> UtilityUi.Kind.CHOSEN
                    id in outsideFrame -> UtilityUi.Kind.OUTSIDE
                    else -> UtilityUi.Kind.NORMAL
                },
            )
            chip.text = (if (id == selected.id) "✓ " else "") + (LayoutGroups.byId(id)?.label ?: id)
        }
        groupTitle.text = "${selected.label} · ${selected.rule}"
        sizeValue.text = "${Math.round(adjust.scale * 100f)} %"
        stepButton.text = "крок $stepPx"
        zoomButton.text = "Лупа ${zoom}×"
        UtilityUi.style(zoomButton, if (zoom > 1) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
        snapButton.text = if (snapOn) "Прив'язка: так" else "Прив'язка: ні"
        UtilityUi.style(snapButton, if (snapOn) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
        applyView()
        // The parts' template places are known after the next drawing.
        skin.postDelayed({
            applyView()
            checkVideoFrame()
        }, 80L)
    }

    /** Where the chosen part is now (template place + faric's move / size), in screen px. */
    private fun boxOf(groupId: String): LayoutBox? {
        val base = skin.groupBaseRect(groupId) ?: return null
        return LayoutBox(base.left, base.top, base.right, base.bottom)
            .adjusted(working[groupId] ?: LayoutAdjust(), virtualW.toFloat(), virtualH.toFloat())
    }

    /** «Лупа»: the copy grows around the chosen part and stays inside the preview frame. */
    private fun applyView() {
        viewScale = previewScale * zoom
        val focus = boxOf(selected.id)
        val fx = focus?.centerX ?: (virtualW / 2f)
        val fy = focus?.centerY ?: (virtualH / 2f)
        offsetX = (boxW / 2f - fx * viewScale).coerceIn(boxW - virtualW * viewScale, 0f)
        offsetY = (boxH / 2f - fy * viewScale).coerceIn(boxH - virtualH * viewScale, 0f)
        skin.scaleX = viewScale
        skin.scaleY = viewScale
        skin.translationX = offsetX
        skin.translationY = offsetY
        overlay.invalidate()
    }

    private fun guideTargets(): Pair<List<Float>, List<Float>> {
        val xs = mutableListOf(virtualW / 2f)
        val ys = mutableListOf(virtualH / 2f)
        LayoutGroups.ALL.filter { it.id != selected.id }.forEach { group ->
            boxOf(group.id)?.let { box ->
                xs += box.centerX
                ys += box.centerY
            }
        }
        return xs to ys
    }

    private fun snapped(raw: LayoutAdjust): LayoutAdjust {
        val baseRect = skin.groupBaseRect(selected.id)
        if (!snapOn || baseRect == null) {
            guideX = null
            guideY = null
            return raw
        }
        val (xs, ys) = guideTargets()
        val result =
            LayoutSnap.snap(
                raw = raw,
                base = LayoutBox(baseRect.left, baseRect.top, baseRect.right, baseRect.bottom),
                screenW = virtualW.toFloat(),
                screenH = virtualH.toFloat(),
                targetsX = xs,
                targetsY = ys,
                threshold = dp(SNAP_DP) / viewScale,
            )
        guideX = result.guideX
        guideY = result.guideY
        return result.adjust
    }

    private fun showGuidesExact() {
        if (!snapOn) return
        val box = boxOf(selected.id) ?: return
        val (xs, ys) = guideTargets()
        guideX = xs.firstOrNull { abs(it - box.centerX) < 0.75f }
        guideY = ys.firstOrNull { abs(it - box.centerY) < 0.75f }
        overlay.invalidate()
    }

    /** «Відео 9:16»: which parts stick out of the 9:16 frame («на весь кадр») — red. */
    private fun checkVideoFrame() {
        if (profile != LayoutProfileId.VIDEO_9X16) return
        val visible = videoVisibleRect()
        val outside =
            LayoutGroups.ALL.filter { group ->
                val box = boxOf(group.id) ?: return@filter false
                box.right > box.left &&
                    (box.top < visible.top - 1f || box.bottom > visible.bottom + 1f || box.left < visible.left - 1f || box.right > visible.right + 1f)
            }.map { it.id }.toSet()
        if (outside != outsideFrame) {
            outsideFrame = outside
            refreshLabels()
        }
        if (outside.isEmpty()) {
            warning.visibility = View.GONE
        } else {
            warning.visibility = View.VISIBLE
            warning.text =
                "За кадром: " + outside.mapNotNull { LayoutGroups.byId(it)?.label }.joinToString(", ") +
                    ". Пересунь їх усередину пунктиру."
        }
        overlay.invalidate()
    }

    /** The part of the phone screen a 1080×1920 video shows in «на весь кадр» (same maths as the export). */
    private fun videoVisibleRect(): RectF {
        val placement = ScreenPlacement.compute(1080, 1920, virtualH.toFloat() / virtualW, false)
        val toView = virtualW.toFloat() / placement.virtualWidth
        val left = (-placement.dx / placement.scale) * toView
        val top = (-placement.dy / placement.scale) * toView
        val right = left + (1080f / placement.scale) * toView
        val bottom = top + (1920f / placement.scale) * toView
        return RectF(max(0f, left), max(0f, top), min(virtualW.toFloat(), right), min(virtualH.toFloat(), bottom))
    }

    /** A finger on the preview moves the chosen part (and sticks to centres with «Прив'язка»). */
    private inner class DragToMove : View.OnTouchListener {
        private var lastX = 0f
        private var lastY = 0f
        private var raw = LayoutAdjust()

        override fun onTouch(
            view: View,
            event: MotionEvent,
        ): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    raw = current()
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.x - lastX) / viewScale
                    val dy = (event.y - lastY) / viewScale
                    lastX = event.x
                    lastY = event.y
                    raw = raw.moved(dx / virtualW, dy / virtualH)
                    // With «Лупа» the view follows the part only after the finger lifts — it would run away.
                    updateDuringDrag(snapped(raw))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.performClick()
                    refreshLabels()
                }
            }
            return true
        }
    }

    private fun updateDuringDrag(next: LayoutAdjust) {
        if (next.isIdentity) working.remove(selected.id) else working[selected.id] = next
        dirty = true
        skin.previewLayout = working.toMap()
        sizeValue.text = "${Math.round(next.scale * 100f)} %"
        overlay.invalidate()
    }

    /** Guides, the chosen part's outline and the video frame, drawn over the preview. */
    private inner class EditorOverlay(context: Context) : View(context) {
        private val centerLine =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.argb(90, 64, 200, 255)
                strokeWidth = context.resources.displayMetrics.density
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            }
        private val guideLine =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = UtilityUi.GREEN
                strokeWidth = 2f * context.resources.displayMetrics.density
            }
        private val outline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = UtilityUi.GREEN
                strokeWidth = 1.5f * context.resources.displayMetrics.density
                pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
            }
        private val shade = Paint().apply { color = Color.argb(150, 0, 0, 0) }
        private val frameLine =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = UtilityUi.ORANGE
                strokeWidth = 2f * context.resources.displayMetrics.density
                pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }

        private fun px(x: Float) = x * viewScale + offsetX

        private fun py(y: Float) = y * viewScale + offsetY

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (profile == LayoutProfileId.VIDEO_9X16) {
                val r = videoVisibleRect()
                val top = py(r.top)
                val bottom = py(r.bottom)
                canvas.drawRect(0f, 0f, w, top, shade)
                canvas.drawRect(0f, bottom, w, h, shade)
                canvas.drawRect(px(r.left), top, px(r.right), bottom, frameLine)
            }
            if (snapOn) {
                canvas.drawLine(px(virtualW / 2f), 0f, px(virtualW / 2f), h, centerLine)
                canvas.drawLine(0f, py(virtualH / 2f), w, py(virtualH / 2f), centerLine)
            }
            guideX?.let { canvas.drawLine(px(it), 0f, px(it), h, guideLine) }
            guideY?.let { canvas.drawLine(0f, py(it), w, py(it), guideLine) }
            boxOf(selected.id)?.let { box ->
                canvas.drawRect(px(box.left), py(box.top), px(box.right), py(box.bottom), outline)
            }
        }
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
        private const val SNAP_DP = 8
        private const val STATE_WORKING = "working"
        private const val STATE_SELECTED = "selected"
        private const val STATE_STEP = "step"
        private const val STATE_DIRTY = "dirty"
        private const val STATE_ZOOM = "zoom"
        private const val STATE_SNAP = "snap"
    }
}
