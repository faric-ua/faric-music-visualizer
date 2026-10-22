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
import com.saney.musicvisualizer.ui.SnapshotName
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
    private var stepPx = 5
    private var dirty = false
    private var zoom = 1
    private var snapSet = com.saney.musicvisualizer.ui.SnapSet()
    private var snapPx = 20
    private lateinit var snapDistanceButton: TextView
    private lateinit var chipRow: LinearLayout
    private lateinit var optionsChip: TextView
    private lateinit var controlsPanel: LinearLayout
    private lateinit var objectPanel: LinearLayout
    private lateinit var loadPanel: LinearLayout
    private lateinit var visibleButton: TextView
    private lateinit var alphaLabel: TextView
    private lateinit var alphaBar: android.widget.SeekBar
    private lateinit var loadTitle: TextView
    private lateinit var loadInfo: TextView
    private lateinit var loadDescription: TextView

    /** V127: the name of the layout last saved or applied here — «Зберегти як…» starts from it. */
    private var lastName: String? = null
    private var loadList: List<LayoutProfileStore.Snapshot> = emptyList()
    private var loadIndex = 0
    private var beforeLoad: Map<String, LayoutAdjust>? = null
    private var pulseAnimator: android.animation.ValueAnimator? = null
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
            zoom = savedInstanceState.getInt(STATE_ZOOM, 1).coerceIn(1, ZOOMS.last())
            snapSet = com.saney.musicvisualizer.ui.SnapSet.decode(savedInstanceState.getString(STATE_SNAP))
            snapPx = savedInstanceState.getInt(STATE_SNAP_PX, snapPx)
            lastName = savedInstanceState.getString(STATE_LAST_NAME)
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
                coverMode = com.saney.musicvisualizer.ui.CoverBoxMode.PICTURE_PLACEHOLDER
                // Switched-off parts show faintly here, so they can be found and switched on.
                showHiddenFaint = true
                previewLayout = working.toMap()
                pivotX = 0f
                pivotY = 0f
                // Taps go to the editor, not to the player buttons.
                isClickable = false
                isEnabled = false
            }
        previewBox.addView(skin, FrameLayout.LayoutParams(virtualW, virtualH))
        // V126 (faric 02:23: «у макеті — одне, на головній — інше»): the copy shows the cover of the
        // song that plays, like the main screen (not only the ♪ placeholder).
        kotlin.concurrent.thread(name = "editor-cover") {
            val art =
                runCatching {
                    val session = com.saney.musicvisualizer.playback.LastSessionStore(this)
                    val tracks = session.loadTracks()
                    tracks.getOrNull(session.index().coerceIn(0, (tracks.size - 1).coerceAtLeast(0)))
                        ?.let { com.saney.musicvisualizer.ui.CoverArtLoader.load(this, android.net.Uri.parse(it.uri)) }
                }.getOrNull()
            if (art != null) runOnUiThread { if (!isDestroyed) skin.coverArt = art }
        }
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

        chipRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                // V126: room for the chips' frame (the bottom edge was cut).
                setPadding(0, dp(2), 0, dp(6))
            }
        // V123 (faric 22.10 01:45): «☰» next to the chosen part — its own settings (видимість, прозорість).
        optionsChip =
            UtilityUi.button(this, "☰") { openObjectPanel() }.apply {
                textSize = 16f
                contentDescription = "Налаштування вибраної частини"
            }
        // V126 (faric 02:23): «☰» gently breathes (frame cyan ↔ dark) — «мене можна натиснути».
        pulseAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                val edge =
                    android.graphics.Color.rgb(
                        (42 + (64 - 42) * f).toInt(),
                        (59 + (200 - 59) * f).toInt(),
                        (74 + (255 - 74) * f).toInt(),
                    )
                optionsChip.background = UtilityUi.frame(UtilityUi.PANEL, edge, 2, 12, this@PulseDeckLayoutEditorActivity)
            }
            start()
        }
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

        controlsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // V122 (faric 22.10 01:18): «Розмір» зліва і «Місце» справа — дві колонки по три ряди;
        // «Скинути частину» і «Зберегти» — на всю ширину, унизу.
        val columns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val sizeColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sizeColumn.addView(UtilityUi.text(this, "Розмір", 12f, UtilityUi.MUTED).apply { gravity = android.view.Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
        sizeColumn.addView(pair(UtilityUi.button(this, "−5") { resize(-5) }, UtilityUi.button(this, "−1") { resize(-1) }))
        sizeValue = UtilityUi.text(this, "100 %", 16f, bold = true).apply {
            gravity = android.view.Gravity.CENTER
            minHeight = dp(48)
            background = UtilityUi.frame(UtilityUi.BG, UtilityUi.CYAN, 1, 10, context)
        }
        sizeColumn.addView(sizeValue, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(6), dp(3), 0) })
        sizeColumn.addView(pair(UtilityUi.button(this, "+1") { resize(1) }, UtilityUi.button(this, "+5") { resize(5) }))
        val moveColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        moveColumn.addView(UtilityUi.text(this, "Місце", 12f, UtilityUi.MUTED).apply { gravity = android.view.Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
        moveColumn.addView(
            pair(
                UtilityUi.button(this, "←") { move(-stepPx, 0) }.apply { contentDescription = "Вліво" },
                UtilityUi.button(this, "↑") { move(0, -stepPx) }.apply { contentDescription = "Вгору" },
            ),
        )
        stepButton = UtilityUi.button(this, "") { chooseStep() }
        moveColumn.addView(stepButton, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(6), dp(3), 0) })
        moveColumn.addView(
            pair(
                UtilityUi.button(this, "↓") { move(0, stepPx) }.apply { contentDescription = "Вниз" },
                UtilityUi.button(this, "→") { move(stepPx, 0) }.apply { contentDescription = "Вправо" },
            ),
        )
        columns.addView(sizeColumn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        columns.addView(moveColumn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        controlsPanel.addView(columns, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        val toolsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        zoomButton = UtilityUi.button(this, "") { chooseZoom() }
        snapButton = UtilityUi.button(this, "") { chooseSnap() }
        snapDistanceButton = UtilityUi.button(this, "") { chooseSnapDistance() }
        zoomButton.textSize = 13f
        snapButton.textSize = 13f
        snapDistanceButton.textSize = 13f
        toolsRow.addView(zoomButton, UtilityUi.weighted(context = this))
        toolsRow.addView(snapButton, UtilityUi.weighted(context = this, weight = 1.4f))
        toolsRow.addView(snapDistanceButton, UtilityUi.weighted(context = this, weight = 0.9f))
        controlsPanel.addView(toolsRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        if (profile == LayoutProfileId.VIDEO_9X16) {
            controlsPanel.addView(
                UtilityUi.button(this, "Узяти з «Книжки»") { copyFromPortrait() },
                LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(10), dp(3), 0) },
            )
        }
        controlsPanel.addView(
            UtilityUi.button(this, "Скинути частину", UtilityUi.Kind.WARNING) { resetSelected() },
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(10), dp(3), 0) },
        )
        // V125: named saves — «Зберегти як…» and «Завантажити…» (list with a live preview).
        val filesRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filesRow.addView(UtilityUi.button(this, "Зберегти як…") { saveAs() }, UtilityUi.weighted(context = this))
        filesRow.addView(UtilityUi.button(this, "Завантажити…") { openLoadPanel() }, UtilityUi.weighted(context = this))
        controlsPanel.addView(filesRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        controlsPanel.addView(
            UtilityUi.button(this, "Зберегти", UtilityUi.Kind.MAIN) { save() },
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(10), dp(3), 0) },
        )

        root.addView(controlsPanel, LinearLayout.LayoutParams(-1, -2))

        // V123: the chosen part's own settings (instead of the size / place buttons while open).
        objectPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        visibleButton = UtilityUi.button(this, "") { toggleVisible() }
        objectPanel.addView(visibleButton, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        alphaLabel = UtilityUi.text(this, "", 14f)
        objectPanel.addView(alphaLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        alphaBar =
            android.widget.SeekBar(this).apply {
                max = 90
                progressTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                thumbTintList = android.content.res.ColorStateList.valueOf(UtilityUi.CYAN)
                minimumHeight = dp(48)
                setOnSeekBarChangeListener(
                    object : android.widget.SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) setTransparency(progress)
                        }

                        override fun onStartTrackingTouch(bar: android.widget.SeekBar?) = Unit

                        override fun onStopTrackingTouch(bar: android.widget.SeekBar?) = Unit
                    },
                )
            }
        // V126 (faric 02:23): «−» / «+» at the ends — exactly one percent per tap, no hunting with the finger.
        val alphaRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        alphaRow.addView(
            UtilityUi.button(this, "−") { setTransparency((alphaBar.progress - 1).coerceAtLeast(0)) }.apply { contentDescription = "Менше прозорості на 1 %" },
            LinearLayout.LayoutParams(dp(52), dp(48)),
        )
        alphaRow.addView(alphaBar, LinearLayout.LayoutParams(0, dp(48), 1f))
        alphaRow.addView(
            UtilityUi.button(this, "+") { setTransparency((alphaBar.progress + 1).coerceAtMost(alphaBar.max)) }.apply { contentDescription = "Більше прозорості на 1 %" },
            LinearLayout.LayoutParams(dp(52), dp(48)),
        )
        objectPanel.addView(alphaRow, LinearLayout.LayoutParams(-1, -2))
        objectPanel.addView(
            UtilityUi.text(this, "0 % — видно повністю; більше — крізь частину видно візуалізацію.", 12f, UtilityUi.MUTED),
            LinearLayout.LayoutParams(-1, -2),
        )
        objectPanel.addView(
            UtilityUi.button(this, "Готово", UtilityUi.Kind.MAIN) { closePanels() },
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(14), dp(3), 0) },
        )
        root.addView(objectPanel, LinearLayout.LayoutParams(-1, -2))

        // V125: saved layouts — step through them with ▲ ▼, the copy above shows each one.
        loadPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        loadTitle = UtilityUi.text(this, "", 17f, bold = true)
        loadPanel.addView(loadTitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        loadDescription = UtilityUi.text(this, "", 14f)
        loadPanel.addView(loadDescription, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        loadInfo = UtilityUi.text(this, "", 13f, UtilityUi.MUTED)
        loadPanel.addView(loadInfo, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        val arrows = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        arrows.addView(UtilityUi.button(this, "▲ попереднє") { stepLoad(-1) }, UtilityUi.weighted(context = this))
        arrows.addView(UtilityUi.button(this, "▼ наступне") { stepLoad(1) }, UtilityUi.weighted(context = this))
        loadPanel.addView(arrows, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        loadPanel.addView(
            UtilityUi.button(this, "Видалити це збереження", UtilityUi.Kind.WARNING) { deleteLoaded() },
            LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(3), dp(10), dp(3), 0) },
        )
        val loadActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        loadActions.addView(UtilityUi.button(this, "Закрити") { cancelLoad() }, UtilityUi.weighted(context = this))
        loadActions.addView(UtilityUi.button(this, "Застосувати", UtilityUi.Kind.MAIN) { applyLoaded() }, UtilityUi.weighted(context = this))
        loadPanel.addView(loadActions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        root.addView(loadPanel, LinearLayout.LayoutParams(-1, -2))

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

    override fun onDestroy() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_WORKING, LayoutProfileCodec.encode(working))
        outState.putString(STATE_LAST_NAME, lastName)
        outState.putString(STATE_SELECTED, selected.id)
        outState.putInt(STATE_STEP, stepPx)
        outState.putBoolean(STATE_DIRTY, dirty)
        outState.putInt(STATE_ZOOM, zoom)
        outState.putString(STATE_SNAP, snapSet.encode())
        outState.putInt(STATE_SNAP_PX, snapPx)
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
        if (::objectPanel.isInitialized && objectPanel.visibility == View.VISIBLE) refreshObjectPanel()
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

    /** Two buttons side by side, each half of the column. */
    private fun pair(
        first: TextView,
        second: TextView,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(first, UtilityUi.weighted(context = context))
            addView(second, UtilityUi.weighted(context = context))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
        }

    /** V122: the step — a list (1, 5, 10, 15, 20, 25 px), not a cycling button. */
    private fun chooseStep() {
        val labels = STEPS.map { "$it px" }
        PulseDeckDialogs.showSingleChoice(
            context = this,
            title = "Крок стрілок",
            labels = labels,
            checkedIndex = STEPS.indexOf(stepPx),
        ) { index ->
            stepPx = STEPS[index]
            refreshLabels()
        }
    }

    /** V122: the magnifier — a list (1× … 5×). */
    private fun chooseZoom() {
        val labels = ZOOMS.map { "$it×" }
        PulseDeckDialogs.showSingleChoice(
            context = this,
            title = "Лупа",
            labels = labels,
            checkedIndex = ZOOMS.indexOf(zoom),
        ) { index ->
            zoom = ZOOMS[index]
            refreshLabels()
        }
    }

    /**
     * V126 (faric 22.10 02:23): ticks, several at once — Центр and the edges; «Усі 4 краї» ticks all
     * four and greys the single sides. «Зберегти» / «Скасувати».
     */
    private fun chooseSnap() {
        var draft = snapSet
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rows = mutableListOf<Pair<TextView, () -> Unit>>()
        fun refreshRows() = rows.forEach { (_, refresh) -> refresh() }
        fun row(
            label: String,
            checked: () -> Boolean,
            enabled: () -> Boolean,
            toggle: () -> Unit,
        ) {
            val view = UtilityUi.button(this, "") {}
            view.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            view.setOnClickListener {
                if (enabled()) {
                    toggle()
                    refreshRows()
                }
            }
            val refresh = {
                val on = checked()
                view.text = (if (on) "☑  " else "☐  ") + label
                UtilityUi.style(view, if (on) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
                view.alpha = if (enabled()) 1f else 0.4f
            }
            rows += view to refresh
            body.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }
        row("Центр", { draft.center }, { true }) { draft = draft.copy(center = !draft.center) }
        row("Усі 4 краї", { draft.allEdges }, { true }) { draft = draft.withAllEdges(!draft.allEdges) }
        row("Лівий край", { draft.left }, { !draft.allEdges }) { draft = draft.copy(left = !draft.left) }
        row("Правий край", { draft.right }, { !draft.allEdges }) { draft = draft.copy(right = !draft.right) }
        row("Верхній край", { draft.top }, { !draft.allEdges }) { draft = draft.copy(top = !draft.top) }
        row("Нижній край", { draft.bottom }, { !draft.allEdges }) { draft = draft.copy(bottom = !draft.bottom) }
        refreshRows()
        PulseDeckDialogs.show(
            context = this,
            title = "Прив'язка",
            message = "До чого «прилипає» частина: її центр і / або краї — до центрів і країв екрана та інших частин. Нічого не вибрано — прив'язка вимкнена.",
            body = body,
            actions =
                listOf(
                    PulseDeckDialogs.Action(label = "Зберегти", accent = true) {
                        snapSet = draft
                        guideX = null
                        guideY = null
                        refreshLabels()
                    },
                    PulseDeckDialogs.Action(label = "Скасувати") {},
                ),
        )
    }

    /** V124: from how far (screen px) the part jumps onto the line. */
    private fun chooseSnapDistance() {
        PulseDeckDialogs.showSingleChoice(
            context = this,
            title = "Прив'язка — з якої відстані",
            labels = SNAP_DISTANCES.map { "$it px" },
            checkedIndex = SNAP_DISTANCES.indexOf(snapPx),
        ) { index ->
            snapPx = SNAP_DISTANCES[index]
            refreshLabels()
        }
    }

    private fun showOnly(panel: LinearLayout) {
        listOf(controlsPanel, objectPanel, loadPanel).forEach { it.visibility = if (it === panel) View.VISIBLE else View.GONE }
    }

    private fun closePanels() {
        showOnly(controlsPanel)
        refreshLabels()
    }

    private fun openObjectPanel() {
        showOnly(objectPanel)
        refreshObjectPanel()
    }

    private fun refreshObjectPanel() {
        val adjust = current()
        visibleButton.text = if (adjust.hidden) "Видимість: вимкнено" else "Видимість: увімкнено"
        UtilityUi.style(visibleButton, if (adjust.hidden) UtilityUi.Kind.OUTSIDE else UtilityUi.Kind.CHOSEN)
        val transparency = Math.round((1f - adjust.alpha) * 100f)
        alphaLabel.text = "${selected.label} · прозорість $transparency %"
        if (alphaBar.progress != transparency) alphaBar.progress = transparency
    }

    private fun toggleVisible() {
        update(current().copy(hidden = !current().hidden))
        refreshObjectPanel()
    }

    private fun setTransparency(percent: Int) {
        update(current().copy(alpha = (1f - percent / 100f).coerceIn(0.1f, 1f)))
        refreshObjectPanel()
    }

    private fun saveAs() {
        // V127 (faric 22.10 02:29): the field starts with the last name, so a series of saves is just
        // «дописати» («Обкладинка по центру» → «… + меню знизу 50 %»); ✕ wipes it; long names wrap.
        val start = lastName ?: store.snapshots(profile).firstOrNull()?.name
        PulseDeckDialogs.showTextInput(
            context = this,
            title = "Зберегти розкладку як…",
            hint = "Назва (напр. «Велика обкладинка»)",
            initialValue = start,
            positiveLabel = "Зберегти",
            multiLine = true,
            clearButton = true,
        ) { raw ->
            val name = SnapshotName.clean(raw)
            store.saveSnapshot(profile, name, working)
            lastName = name
            PulseDeckDialogs.showMessage(
                context = this,
                title = "Збережено",
                message = "«$name» — у списку «Завантажити…» для «${profile.label}».",
            )
        }
    }

    private fun openLoadPanel() {
        loadList = store.snapshots(profile)
        if (loadList.isEmpty()) {
            PulseDeckDialogs.showMessage(
                context = this,
                title = "Збережень ще нема",
                message = "Спершу «Зберегти як…» — і розкладка з'явиться тут.",
            )
            return
        }
        beforeLoad = working.toMap()
        loadIndex = 0
        showOnly(loadPanel)
        showLoaded()
    }

    private fun showLoaded() {
        val snapshot = loadList.getOrNull(loadIndex) ?: return
        loadTitle.text = SnapshotName.header(snapshot.name)
        val description = SnapshotName.description(snapshot.name)
        loadDescription.text = description.orEmpty()
        loadDescription.visibility = if (description == null) View.GONE else View.VISIBLE
        val date =
            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.ROOT).format(java.util.Date(snapshot.savedAt))
        loadInfo.text = "${loadIndex + 1} з ${loadList.size} · $date · змінено частин: ${snapshot.adjusts.size}"
        skin.previewLayout = snapshot.adjusts
        refreshLabelsLight()
    }

    private fun stepLoad(delta: Int) {
        if (loadList.isEmpty()) return
        loadIndex = (loadIndex + delta + loadList.size) % loadList.size
        showLoaded()
    }

    private fun cancelLoad() {
        beforeLoad?.let { skin.previewLayout = it }
        beforeLoad = null
        closePanels()
    }

    /** «Застосувати»: the saved layout becomes this position's layout right away (no extra «Зберегти»). */
    private fun applyLoaded() {
        val snapshot = loadList.getOrNull(loadIndex) ?: return
        working.clear()
        working.putAll(snapshot.adjusts)
        store.save(profile, working)
        lastName = snapshot.name
        dirty = false
        beforeLoad = null
        skin.previewLayout = working.toMap()
        closePanels()
        PulseDeckDialogs.showMessage(
            context = this,
            title = "Застосовано",
            message = "«${SnapshotName.header(snapshot.name)}» — тепер розкладка «${profile.label}».",
        )
    }

    private fun deleteLoaded() {
        val snapshot = loadList.getOrNull(loadIndex) ?: return
        PulseDeckDialogs.showConfirm(
            context = this,
            title = "Видалити «${SnapshotName.header(snapshot.name)}»?",
            message = "Збереження зникне зі списку. Поточна розкладка не зміниться.",
            positiveLabel = "Видалити",
            destructive = true,
        ) {
            store.deleteSnapshot(profile, snapshot.id)
            loadList = store.snapshots(profile)
            if (loadList.isEmpty()) {
                cancelLoad()
            } else {
                loadIndex = loadIndex.coerceAtMost(loadList.lastIndex)
                showLoaded()
            }
        }
    }

    /** Keep the view (zoom, video frame) in step without touching the panels. */
    private fun refreshLabelsLight() {
        applyView()
        skin.postDelayed({ applyView() }, 80L)
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
        stepButton.text = "крок $stepPx ▾"
        zoomButton.text = "Лупа ${zoom}× ▾"
        UtilityUi.style(zoomButton, if (zoom > 1) UtilityUi.Kind.CHOSEN else UtilityUi.Kind.NORMAL)
        // V126: just «Прив'язка» — green when on, grey when off; what it sticks to is in the list.
        snapButton.text = "Прив'язка ▾"
        UtilityUi.style(snapButton, if (snapSet.isOff) UtilityUi.Kind.NORMAL else UtilityUi.Kind.CHOSEN)
        snapButton.contentDescription = "Прив'язка: " + snapSet.label
        snapDistanceButton.text = "$snapPx px ▾"
        // «☰» stands right after the chosen part's chip.
        (optionsChip.parent as? android.view.ViewGroup)?.removeView(optionsChip)
        val chosenChip = chips[selected.id]
        val at = chosenChip?.let { chipRow.indexOfChild(it) } ?: -1
        chipRow.addView(optionsChip, at + 1, LinearLayout.LayoutParams(dp(52), -2).apply { marginEnd = dp(6) })
        if (current().hidden) chosenChip?.text = "✓ " + selected.label + " (вимк.)"
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

    private fun otherBoxes(): List<LayoutBox> =
        LayoutGroups.ALL.filter { it.id != selected.id }.mapNotNull { group ->
            boxOf(group.id)?.takeIf { working[group.id]?.hidden != true }
        }

    private fun snapped(raw: LayoutAdjust): LayoutAdjust {
        val baseRect = skin.groupBaseRect(selected.id)
        if (baseRect == null) {
            guideX = null
            guideY = null
            return raw
        }
        val result =
            com.saney.musicvisualizer.ui.LayoutSnapEdges.snap(
                raw = raw,
                base = LayoutBox(baseRect.left, baseRect.top, baseRect.right, baseRect.bottom),
                screenW = virtualW.toFloat(),
                screenH = virtualH.toFloat(),
                others = otherBoxes(),
                set = snapSet,
                threshold = snapPx.toFloat(),
            )
        guideX = result.guideX
        guideY = result.guideY
        return result.adjust
    }

    /** Arrows are exact: the line only lights up when a chosen edge / centre sits on one. */
    private fun showGuidesExact() {
        val baseRect = skin.groupBaseRect(selected.id) ?: return
        val result =
            com.saney.musicvisualizer.ui.LayoutSnapEdges.snap(
                raw = current(),
                base = LayoutBox(baseRect.left, baseRect.top, baseRect.right, baseRect.bottom),
                screenW = virtualW.toFloat(),
                screenH = virtualH.toFloat(),
                others = otherBoxes(),
                set = snapSet,
                threshold = 0.75f,
            )
        guideX = result.guideX
        guideY = result.guideY
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
            if (!snapSet.isOff) {
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
        private val STEPS = listOf(1, 5, 10, 15, 20, 25)
        private val ZOOMS = listOf(1, 2, 3, 4, 5)
        private const val STATE_WORKING = "working"
        private const val STATE_SELECTED = "selected"
        private const val STATE_STEP = "step"
        private const val STATE_DIRTY = "dirty"
        private const val STATE_ZOOM = "zoom"
        private const val STATE_SNAP = "snap"
        private const val STATE_SNAP_PX = "snap_px"
        private const val STATE_LAST_NAME = "last_name"
        private val SNAP_DISTANCES = listOf(5, 10, 20, 30, 40)
    }
}
