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
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.export.ScreenPlacement
import com.saney.musicvisualizer.ui.LayoutAdjust
import com.saney.musicvisualizer.ui.LayoutGroup
import com.saney.musicvisualizer.ui.LayoutGroups
import com.saney.musicvisualizer.ui.LayoutProfileId
import com.saney.musicvisualizer.ui.LayoutProfileStore
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import com.saney.musicvisualizer.ui.UtilityUi
import com.saney.musicvisualizer.ui.UtilityUi.dp
import kotlin.math.max
import kotlin.math.min

/**
 * V104 / V114 / V119 «Утиліти → Розкладка екрана» (макет «Утиліти PulseDeck», екрани 2–4).
 * A small copy of the main screen; faric picks a part (обкладинка, ‹, ⋮, назва, панелі …), moves it
 * (arrows or a finger) and resizes it proportionally (−5 / −1 / +1 / +5 %), then «Зберегти» — the
 * main screen (or the MP4 for «Відео 9:16») uses it from then on. Nothing is saved until «Зберегти».
 */
class PulseDeckLayoutEditorActivity : ComponentActivity() {
    private lateinit var profile: LayoutProfileId
    private lateinit var store: LayoutProfileStore
    private val working = mutableMapOf<String, LayoutAdjust>()
    private var selected: LayoutGroup = LayoutGroups.ALL.first()
    private var stepPx = 4
    private var dirty = false

    private lateinit var skin: PulseDeckMainSkinView
    private lateinit var frameOverlay: CropFrameView
    private lateinit var groupTitle: TextView
    private lateinit var sizeValue: TextView
    private lateinit var stepButton: TextView
    private lateinit var warning: TextView
    private val chips = mutableMapOf<String, TextView>()
    private var virtualW = 1
    private var virtualH = 1
    private var previewScale = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile =
            runCatching { LayoutProfileId.valueOf(intent.getStringExtra(EXTRA_PROFILE).orEmpty()) }
                .getOrDefault(LayoutProfileId.PORTRAIT)
        store = LayoutProfileStore(this)
        working.putAll(store.load(profile))

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
                setBackgroundColor(UtilityUi.BG)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
        UtilityUi.padForSystemBars(root)

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
                if (profile == LayoutProfileId.VIDEO_9X16) {
                    "Лише для MP4. Пунктир — що потрапить у кадр 9:16 «на весь кадр»."
                } else {
                    "Тягни частину пальцем або кнопками нижче."
                },
                12f,
                UtilityUi.MUTED,
            ),
        )
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        root.addView(header)

        // The preview: the real main-screen view at its full size, shown smaller.
        val availableW = metrics.widthPixels - dp(24)
        val availableH = (metrics.heightPixels * if (profile == LayoutProfileId.LANDSCAPE) 0.32f else 0.46f).toInt()
        previewScale = min(availableW.toFloat() / virtualW, availableH.toFloat() / virtualH)
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
                scaleX = previewScale
                scaleY = previewScale
                // Taps go to the editor, not to the player buttons.
                isClickable = false
                isEnabled = false
            }
        previewBox.addView(skin, FrameLayout.LayoutParams(virtualW, virtualH))
        frameOverlay = CropFrameView(this)
        previewBox.addView(frameOverlay, FrameLayout.LayoutParams(-1, -1))
        // The frame view lies on top of the preview and takes the finger (the player view would eat it).
        frameOverlay.setOnTouchListener(DragToMove())
        root.addView(
            previewBox,
            LinearLayout.LayoutParams((virtualW * previewScale).toInt(), (virtualH * previewScale).toInt()).apply {
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

        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        sizeRow.addView(UtilityUi.text(this, "Розмір", 12f, UtilityUi.MUTED), LinearLayout.LayoutParams(dp(54), -2))
        listOf(-5, -1).forEach { step -> sizeRow.addView(UtilityUi.button(this, "$step") { resize(step) }, UtilityUi.weighted(context = this)) }
        sizeValue = UtilityUi.text(this, "100 %", 15f, bold = true).apply {
            gravity = android.view.Gravity.CENTER
            minHeight = dp(48)
            background = UtilityUi.frame(UtilityUi.BG, UtilityUi.CYAN, 1, 10, context)
        }
        sizeRow.addView(sizeValue, UtilityUi.weighted(context = this, weight = 1.3f))
        listOf(1, 5).forEach { step -> sizeRow.addView(UtilityUi.button(this, "+$step") { resize(step) }, UtilityUi.weighted(context = this)) }
        root.addView(sizeRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        val moveRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        moveRow.addView(UtilityUi.text(this, "Місце", 12f, UtilityUi.MUTED), LinearLayout.LayoutParams(dp(54), -2))
        moveRow.addView(UtilityUi.button(this, "←") { move(-stepPx, 0) }.apply { contentDescription = "Вліво" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "↑") { move(0, -stepPx) }.apply { contentDescription = "Вгору" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "↓") { move(0, stepPx) }.apply { contentDescription = "Вниз" }, UtilityUi.weighted(context = this))
        moveRow.addView(UtilityUi.button(this, "→") { move(stepPx, 0) }.apply { contentDescription = "Вправо" }, UtilityUi.weighted(context = this))
        stepButton = UtilityUi.button(this, "") { nextStep() }
        stepButton.textSize = 13f
        moveRow.addView(stepButton, UtilityUi.weighted(context = this, weight = 1.4f))
        root.addView(moveRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(UtilityUi.button(this, "Зберегти", UtilityUi.Kind.MAIN) { save() }, UtilityUi.weighted(context = this))
        actions.addView(UtilityUi.button(this, "Скинути частину") { resetSelected() }, UtilityUi.weighted(context = this))
        if (profile == LayoutProfileId.VIDEO_9X16) {
            actions.addView(UtilityUi.button(this, "Узяти з «Книжки»") { copyFromPortrait() }, UtilityUi.weighted(context = this))
        }
        root.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        setContentView(root)
        // Back asks before dropping unsaved changes.
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = leave()
            },
        )
        select(selected)
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
        chips.forEach { (id, chip) ->
            UtilityUi.style(chip, if (id == group.id) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
            chip.text = (if (id == group.id) "✓ " else "") + (LayoutGroups.byId(id)?.label ?: id)
        }
        refreshLabels()
    }

    private fun current(): LayoutAdjust = working[selected.id] ?: LayoutAdjust()

    private fun update(next: LayoutAdjust) {
        if (next.isIdentity) working.remove(selected.id) else working[selected.id] = next
        dirty = true
        skin.previewLayout = working.toMap()
        refreshLabels()
    }

    private fun resize(percent: Int) = update(current().resized(percent))

    private fun move(
        dxPx: Int,
        dyPx: Int,
    ) = update(current().moved(dxPx.toFloat() / virtualW, dyPx.toFloat() / virtualH))

    private fun nextStep() {
        stepPx =
            when (stepPx) {
                1 -> 4
                4 -> 16
                else -> 1
            }
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
        groupTitle.text = "${selected.label} · ${selected.rule}"
        sizeValue.text = "${Math.round(adjust.scale * 100f)} %"
        stepButton.text = "крок $stepPx"
        // The group's new place is known after the next drawing.
        skin.postDelayed({ checkVideoFrame() }, 80L)
    }

    /** «Відео 9:16»: which parts stick out of the 9:16 frame («на весь кадр») — red warning. */
    private fun checkVideoFrame() {
        if (profile != LayoutProfileId.VIDEO_9X16) {
            frameOverlay.frame = null
            return
        }
        val visible = videoVisibleRect()
        frameOverlay.frame = RectF(visible.left * previewScale, visible.top * previewScale, visible.right * previewScale, visible.bottom * previewScale)
        val outside =
            LayoutGroups.ALL.filter { group ->
                val rect = skin.groupRect(group.id) ?: return@filter false
                rect.width() > 0f && (rect.top < visible.top - 1f || rect.bottom > visible.bottom + 1f || rect.left < visible.left - 1f || rect.right > visible.right + 1f)
            }
        chips.forEach { (id, chip) ->
            val out = outside.any { it.id == id }
            UtilityUi.style(
                chip,
                when {
                    id == selected.id -> UtilityUi.Kind.CHOSEN
                    out -> UtilityUi.Kind.OUTSIDE
                    else -> UtilityUi.Kind.NORMAL
                },
            )
        }
        if (outside.isEmpty()) {
            warning.visibility = View.GONE
        } else {
            warning.visibility = View.VISIBLE
            warning.text = "За кадром: " + outside.joinToString(", ") { it.label } + ". Пересунь їх усередину пунктиру."
        }
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

    /** A finger on the preview moves the chosen part. */
    private inner class DragToMove : View.OnTouchListener {
        private var lastX = 0f
        private var lastY = 0f

        override fun onTouch(
            view: View,
            event: MotionEvent,
        ): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.x - lastX) / previewScale
                    val dy = (event.y - lastY) / previewScale
                    lastX = event.x
                    lastY = event.y
                    update(current().moved(dx / virtualW, dy / virtualH))
                }
                MotionEvent.ACTION_UP -> view.performClick()
            }
            return true
        }
    }

    /** Orange dashed frame of the 9:16 video over the preview (only for «Відео 9:16»). */
    private class CropFrameView(context: Context) : View(context) {
        var frame: RectF? = null
            set(value) {
                field = value
                invalidate()
            }
        private val shade = Paint().apply { color = Color.argb(150, 0, 0, 0) }
        private val line =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = UtilityUi.ORANGE
                strokeWidth = 2f * context.resources.displayMetrics.density
                pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val r = frame ?: return
            canvas.drawRect(0f, 0f, width.toFloat(), r.top, shade)
            canvas.drawRect(0f, r.bottom, width.toFloat(), height.toFloat(), shade)
            canvas.drawRect(r, line)
        }
    }

    companion object {
        const val EXTRA_PROFILE = "profile"
    }
}
