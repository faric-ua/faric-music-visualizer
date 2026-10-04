package com.saney.musicvisualizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardGroupReaction
import com.saney.musicvisualizer.board.BoardGroupReactionStore
import com.saney.musicvisualizer.board.BoardLayerId
import com.saney.musicvisualizer.board.BoardLayerTransform
import com.saney.musicvisualizer.board.BoardLayerTransformStore
import com.saney.musicvisualizer.board.BoardTransform
import com.saney.musicvisualizer.board.BoardTransformStore
import com.saney.musicvisualizer.export.ExportFrameProof
import com.saney.musicvisualizer.export.OfflineAnalysisResult
import com.saney.musicvisualizer.export.OfflineAudioAnalyzer
import com.saney.musicvisualizer.export.ShortVideoExportProof
import com.saney.musicvisualizer.playback.PlaybackController
import com.saney.musicvisualizer.playback.PlaybackSnapshot
import com.saney.musicvisualizer.playback.QueueTrack
import com.saney.musicvisualizer.scene.SceneOrchestrator
import com.saney.musicvisualizer.scene.SceneSpec
import com.saney.musicvisualizer.theme.ExportAspectRatio
import com.saney.musicvisualizer.theme.MusicVideoProject
import com.saney.musicvisualizer.theme.PlaybackThemeId
import com.saney.musicvisualizer.theme.PlaybackThemeRegistry
import com.saney.musicvisualizer.theme.PlaybackThemeStore
import com.saney.musicvisualizer.theme.ThemeInput
import com.saney.musicvisualizer.ui.HeroBoardView
import com.saney.musicvisualizer.ui.HeroThemeView
import com.saney.musicvisualizer.ui.PulseMiniView
import com.saney.musicvisualizer.ui.PulseDeckControlRail
import com.saney.musicvisualizer.ui.PulseDeckIconButton
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import com.saney.musicvisualizer.ui.ReactiveSceneView
import java.util.Locale
import kotlin.concurrent.thread

@UnstableApi
class MainActivity : ComponentActivity(), PlaybackController.Listener {

    private enum class ControlsAutoHideMode {
        NEVER,
        TRANSPORT_ONLY,
        TRANSPORT_AND_QUICK,
    }

    private enum class Screen {
        LIBRARY,
        NOW_PLAYING,
        THEME_PICKER,
        BOARD_TRANSFORM,
        EXPORT_LAB,
    }

    private lateinit var controller: PlaybackController
    private lateinit var sceneOrchestrator: SceneOrchestrator
    private lateinit var currentScene: SceneSpec
    private lateinit var themeStore: PlaybackThemeStore
    private lateinit var boardTransformStore: BoardTransformStore
    private lateinit var boardGroupReactionStore: BoardGroupReactionStore
    private lateinit var boardLayerTransformStore: BoardLayerTransformStore

    private var activeVerticalScroll: ScrollView? = null
    private var activeHorizontalScroll: HorizontalScrollView? = null
    private var pendingRestoreScrollY: Int? = null
    private var pendingRestoreScrollX: Int? = null
    private var boardEditorLayer: BoardLayerId? = null
    private var selectedThemeId = PlaybackThemeId.VISUALIZER
    private var screen = Screen.LIBRARY
    private var latestSnapshot = PlaybackSnapshot()
    private var latestSignal =
        SceneSignal(
            amplitude = 0f,
            bass = 0f,
            mid = 0f,
            high = 0f,
            beatStrength = 0f,
        )
    private var exportAspectRatio =
        ExportAspectRatio.VERTICAL_9_16

    private var offlineAnalysis: OfflineAnalysisResult? = null
    private var offlineAnalysisUri: String? = null
    private var offlineAnalysisRunning = false

    private var sceneView: ReactiveSceneView? = null
    private var heroBoardView: HeroBoardView? = null
    private var heroThemeView: HeroThemeView? = null
    private var pulseDeckMainSkinView: PulseDeckMainSkinView? = null
    private var miniPulseView: PulseMiniView? = null
    private var dock: View? = null
    private var dockTitle: TextView? = null
    private var dockSubtitle: TextView? = null
    private var dockPlay: TextView? = null
    private var dockProgress: ProgressBar? = null

    private var nowTitle: TextView? = null
    private var nowArtist: TextView? = null
    private var nowStatus: TextView? = null
    private var nowElapsed: TextView? = null
    private var nowTotal: TextView? = null
    private var nowSeek: SeekBar? = null
    private var nowPlay: PulseDeckIconButton? = null
    private var nowControlsLayer: View? = null
    private var nowControlsHidden = false
    private var controlsAutoHideMode = ControlsAutoHideMode.NEVER
    private val nowControlsAutoHideRunnable =
        Runnable {
            if (screen == Screen.NOW_PLAYING) {
                setNowControlsVisible(
                    visible = false,
                    animate = true,
                )
            }
        }

    private var pendingOpenAfterPermission = false

    private val openAudio =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
                offlineAnalysis = null
                offlineAnalysisUri = null
                offlineAnalysisRunning = false

                controller.loadQueue(
                    uris.map { uri ->
                        QueueTrack(
                            uri = uri,
                            displayName = resolveDisplayName(uri),
                        )
                    },
                )
                controller.play()
                showNowPlaying()
            }
        }

    private val requestAudioAnalysisPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            controller.setAnalysisPermissionGranted(granted)
            if (pendingOpenAfterPermission) {
                pendingOpenAfterPermission = false
                openAudio.launch(arrayOf("audio/*"))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        controlsAutoHideMode =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_CONTROLS_AUTO_HIDE_MODE, null)
                ?.let { runCatching { ControlsAutoHideMode.valueOf(it) }.getOrNull() }
                ?: ControlsAutoHideMode.NEVER

                controller = ViewModelProvider(this)[PlaybackController::class.java]
        controller.setAnalysisPermissionGranted(hasAnalysisPermission())

        themeStore = PlaybackThemeStore(this)
        boardTransformStore = BoardTransformStore(this)
        boardGroupReactionStore = BoardGroupReactionStore(this)
        boardLayerTransformStore =
            BoardLayerTransformStore(this)
        selectedThemeId =
            savedInstanceState
                ?.getString(KEY_SELECTED_THEME)
                ?.let { name ->
                    runCatching {
                        PlaybackThemeId.valueOf(name)
                    }.getOrNull()
                }
                ?: themeStore.selectedThemeId

        exportAspectRatio =
            savedInstanceState
                ?.getString(KEY_EXPORT_ASPECT_RATIO)
                ?.let { name ->
                    runCatching {
                        ExportAspectRatio.valueOf(name)
                    }.getOrNull()
                }
                ?: exportAspectRatio

        pendingRestoreScrollY =
            savedInstanceState
                ?.takeIf {
                    it.getBoolean(
                        KEY_HAS_VERTICAL_SCROLL,
                        false,
                    )
                }
                ?.getInt(
                    KEY_VERTICAL_SCROLL_Y,
                    0,
                )

        pendingRestoreScrollX =
            savedInstanceState
                ?.takeIf {
                    it.getBoolean(
                        KEY_HAS_HORIZONTAL_SCROLL,
                        false,
                    )
                }
                ?.getInt(
                    KEY_HORIZONTAL_SCROLL_X,
                    0,
                )

        nowControlsHidden =
            savedInstanceState
                ?.getBoolean(
                    KEY_NOW_CONTROLS_HIDDEN,
                    false,
                )
                ?: false

        boardEditorLayer =
            savedInstanceState
                ?.getString(
                    KEY_BOARD_EDITOR_LAYER,
                )
                ?.let { name ->
                    runCatching {
                        BoardLayerId.valueOf(name)
                    }.getOrNull()
                }

        sceneOrchestrator = SceneOrchestrator { spec ->
            currentScene = spec
            sceneView?.setScene(spec)
        }
        currentScene = sceneOrchestrator.currentScene()

        onBackPressedDispatcher.addCallback(this) {
            when (screen) {
                Screen.NOW_PLAYING -> showLibrary()
                Screen.THEME_PICKER,
                Screen.BOARD_TRANSFORM,
                Screen.EXPORT_LAB,
                -> {
                    if (latestSnapshot.trackName != null) {
                        showNowPlaying()
                    } else {
                        showLibrary()
                    }
                }
                Screen.LIBRARY -> finish()
            }
        }

        val restoredScreen =
            savedInstanceState
                ?.getString(KEY_SCREEN)
                ?.let { name ->
                    runCatching {
                        Screen.valueOf(name)
                    }.getOrNull()
                }
                ?: Screen.LIBRARY

        restoreScreen(restoredScreen)
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putString(
            KEY_SCREEN,
            screen.name,
        )
        outState.putString(
            KEY_SELECTED_THEME,
            selectedThemeId.name,
        )
        outState.putString(
            KEY_EXPORT_ASPECT_RATIO,
            exportAspectRatio.name,
        )
        outState.putBoolean(
            KEY_NOW_CONTROLS_HIDDEN,
            nowControlsHidden,
        )
        boardEditorLayer?.let { layerId ->
            outState.putString(
                KEY_BOARD_EDITOR_LAYER,
                layerId.name,
            )
        }

        activeVerticalScroll?.let { scroll ->
            outState.putBoolean(
                KEY_HAS_VERTICAL_SCROLL,
                true,
            )
            outState.putInt(
                KEY_VERTICAL_SCROLL_Y,
                scroll.scrollY,
            )
        }

        activeHorizontalScroll?.let { scroll ->
            outState.putBoolean(
                KEY_HAS_HORIZONTAL_SCROLL,
                true,
            )
            outState.putInt(
                KEY_HORIZONTAL_SCROLL_X,
                scroll.scrollX,
            )
        }

        super.onSaveInstanceState(
            outState,
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    override fun onStart() {
        super.onStart()
        controller.listener = this
        controller.emitCurrentState()
    }

    override fun onStop() {
        // Do not pause playback here. Screen lock and Home both stop the Activity,
        // but a music player must keep playing. A MediaSessionService migration is
        // tracked separately for full long-lived background playback/notification.
        sceneOrchestrator.stop()
        controller.listener = null
        super.onStop()
    }

    override fun onDestroy() {
        sceneOrchestrator.close()
        super.onDestroy()
    }

    override fun onPlaybackSnapshot(snapshot: PlaybackSnapshot) {
        latestSnapshot = snapshot

        dock?.visibility = if (snapshot.trackName == null) View.GONE else View.VISIBLE
        dockTitle?.text = snapshot.trackName ?: "Нічого не грає"
        dockSubtitle?.text = if (snapshot.analysisActive) {
            "Локальний файл · reactive ON"
        } else {
            "Локальний файл"
        }
        dockPlay?.text = if (snapshot.isPlaying) "Ⅱ" else "▶"

        val ratio = if (snapshot.durationMs > 0L) {
            ((snapshot.positionMs.toDouble() / snapshot.durationMs) * 1000.0).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        dockProgress?.progress = ratio

        nowTitle?.text = snapshot.trackName ?: "FARIC PulseDeck"
        nowArtist?.text = if (snapshot.trackName == null) "Оберіть музику" else "Невідомий виконавець"
        nowStatus?.text = buildString {
            append(snapshot.status)
            if (snapshot.trackName != null) {
                append(
                    if (snapshot.analysisActive) {
                        " · reactive ON"
                    } else {
                        " · reactive OFF"
                    },
                )
            }
            append(" · PulseDeck HUD")
        }
        nowElapsed?.text = formatTime(snapshot.positionMs)
        nowTotal?.text = formatTime(snapshot.durationMs)
        nowPlay?.setIcon(
            if (snapshot.isPlaying) {
                PulseDeckIconButton.Icon.PAUSE
            } else {
                PulseDeckIconButton.Icon.PLAY
            },
        )
        sceneView?.setPlaying(snapshot.isPlaying)
        heroBoardView?.setPlaying(snapshot.isPlaying)
        heroThemeView?.setPlaying(snapshot.isPlaying)
        pulseDeckMainSkinView?.setPlaying(snapshot.isPlaying)
        pulseDeckMainSkinView?.setPlaybackContent(
            title =
                snapshot.trackName
                    ?: "FARIC PulseDeck",
            artist =
                if (snapshot.trackName == null) {
                    "Оберіть музику"
                } else {
                    "Невідомий виконавець"
                },
            status =
                buildString {
                    append(snapshot.status)
                    if (snapshot.trackName != null) {
                        append(
                            if (snapshot.analysisActive) {
                                " · reactive ON"
                            } else {
                                " · reactive OFF"
                            },
                        )
                    }
                    append(" · PulseDeck HUD")
                },
            elapsed =
                formatTime(
                    snapshot.positionMs,
                ),
            total =
                formatTime(
                    snapshot.durationMs,
                ),
            progressFraction =
                ratio /
                    1000f,
        )
        heroThemeView?.setMetadata(
            title = snapshot.trackName,
            artist = if (snapshot.trackName == null) null else "Невідомий виконавець",
        )

        if (
            screen == Screen.NOW_PLAYING &&
            snapshot.isPlaying &&
            selectedThemeId == PlaybackThemeId.VISUALIZER
        ) {
            sceneOrchestrator.start()
        } else {
            sceneOrchestrator.stop()
        }

        nowSeek?.let { seek ->
            if (!seek.isPressed) {
                seek.progress = ratio
            }
            seek.isEnabled = snapshot.durationMs > 0L
        }
    }

    override fun onSceneSignal(signal: SceneSignal) {
        latestSignal = signal
        sceneView?.updateSignal(signal)
        heroBoardView?.updateSignal(signal)
        heroThemeView?.updateSignal(signal)
        pulseDeckMainSkinView?.updateSignal(signal)
        miniPulseView?.updateSignal(signal)
    }

    private fun showLibrary() {
        screen = Screen.LIBRARY
        sceneOrchestrator.stop()
        clearScreenRefs()

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }
        applySafeArea(root)

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setPadding(dp(18), dp(10), dp(18), dp(24))
        }
        activeVerticalScroll = scroll

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("FARIC", 28f, Color.WHITE, true))
        header.addView(label("  PulseDeck", 22f, COLOR_MUTED, false))
        header.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(iconButton("⌕") { toast("Пошук з'явиться разом з локальним індексом") })
        header.addView(iconButton("⋮") { toast("Меню PulseDeck — наступна хвиля") })

        content.addView(header)

        val modeScroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        activeHorizontalScroll = modeScroller
        val modes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, dp(8))
        }
        modes.addView(modeChip("♫", "Бібліотека", "Моя музика", true) { })
        modes.addView(modeChip("▥", "Tone Lab", "Звук і ефекти", false) { toast("Tone Lab — наступний етап") })
        modes.addView(modeChip("◉", "Scene Lab", "Візуальні сцени", false) { showThemePicker() })
        modeScroller.addView(modes)
        content.addView(modeScroller)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = panelDrawable(COLOR_PANEL, 28, COLOR_ACCENT_ORANGE, 1)
        }
        hero.addView(label("Твоя музика. Твоя сцена.", 25f, Color.WHITE, true))
        hero.addView(label(
            "PulseDeck об'єднує локальний плеєр і музичний visualizer. Почнемо з файлів на телефоні.",
            15f,
            COLOR_MUTED,
            false,
        ))
        hero.addView(
            actionPill("＋  Обрати музику", true) { chooseTrack() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(18) },
        )
        content.addView(
            hero,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
            },
        )

        content.addView(
            label("Library Worlds", 20f, Color.WHITE, true),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24)
                bottomMargin = dp(10)
            },
        )

        val categories = listOf(
            Triple("♫", "Усі треки", "локальна бібліотека"),
            Triple("▰", "Теки", "папки й каталоги"),
            Triple("◉", "Альбоми", "обкладинки й релізи"),
            Triple("●", "Виконавці", "артисти"),
            Triple("✦", "Жанри", "стилі музики"),
            Triple("20", "Роки", "хронологія"),
            Triple("♡", "Улюблені", "твоя добірка"),
            Triple("↺", "Нещодавні", "останні треки"),
        )

        categories.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { index, item ->
                row.addView(
                    libraryCard(item.first, item.second, item.third) {
                        toast("${item.second}: запрацює після media scanner")
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index == 0) marginEnd = dp(6) else marginStart = dp(6)
                        bottomMargin = dp(12)
                    },
                )
            }
            content.addView(row)
        }

        scroll.addView(content)
        root.addView(scroll)

        val bottomStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), dp(10))
        }

        bottomStack.addView(
            buildPulseDock(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(8)
            },
        )
        bottomStack.addView(
            buildBottomNav(active = "library"),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        bottomStack.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val requiredBottom = (bottom - top) + dp(18)
            if (scroll.paddingBottom != requiredBottom) {
                scroll.setPadding(dp(18), dp(10), dp(18), requiredBottom)
            }
        }

        root.addView(
            bottomStack,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM),
        )

        setContentView(root)
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
        onPlaybackSnapshot(latestSnapshot)
    }

    private fun showNowPlaying() {
        screen = Screen.NOW_PLAYING
        clearScreenRefs()

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(COLOR_BG)
            }

        val layerStack =
            PulseDeckLayerStack(this)

        root.addView(
            layerStack,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val liveScene =
            ReactiveSceneView(this).also { view ->
                view.setScene(currentScene)
                view.updateSignal(latestSignal)
                view.setPlaying(latestSnapshot.isPlaying)
            }
        sceneView = liveScene

        layerStack.setContent(
            PulseDeckLayerStack.Layer.VISUALIZER,
            liveScene,
        )

        val skinView =
            PulseDeckMainSkinView(
                this,
                forceModularMode = true,
                transparentBackground = true,
            ).also { view ->
                view.updateSignal(
                    latestSignal,
                )
                view.setPlaying(
                    latestSnapshot.isPlaying,
                )

                view.setInteractionListener {
                    scheduleNowControlsAutoHide()
                }

                view.setDoubleTapListener {
                    setNowControlsVisible(
                        visible =
                            nowControlsHidden,
                        animate = true,
                    )
                }

                view.setSeekListener { fraction ->
                    val duration =
                        latestSnapshot.durationMs

                    if (duration > 0L) {
                        controller.seekTo(
                            (
                                duration *
                                    fraction
                                )
                                .toLong(),
                        )
                    }

                    scheduleNowControlsAutoHide()
                }

                view.setActionListener { action ->
                    when (action) {
                        "back" ->
                            showLibrary()

                        "menu" ->
                            android.app.AlertDialog.Builder(this)
                                .setTitle("PulseDeck tools")
                                .setItems(
                                    arrayOf(
                                        "Center Calibration",
                                        "Template Constructor",
                                        "Object Constructor",
                                        "Автоприховування",
                                    ),
                                ) { _, which ->
                                    when (which) {
                                        0 ->
                                            startActivity(
                                                Intent(
                                                    this,
                                                    PulseDeckCenterCalibrationActivity::class.java,
                                                ),
                                            )

                                        1 ->
                                            startActivity(
                                                Intent(
                                                    this,
                                                    PulseDeckTemplateConstructorActivity::class.java,
                                                ),
                                            )

                                        2 ->
                                            startActivity(
                                                Intent(
                                                    this,
                                                    PulseDeckObjectConstructorActivity::class.java,
                                                ),
                                            )

                                        3 ->
                                            showControlsAutoHideDialog()
                                    }
                                }
                                .show()

                        "favorite" ->
                            toast(
                                "Обране — наступний етап",
                            )

                        "track_more" ->
                            toast(
                                "Дії треку — наступний етап",
                            )

                        "shuffle" -> {
                            val enabled = controller.toggleShuffle()
                            toast(if (enabled) "Shuffle · увімкнено" else "Shuffle · вимкнено")
                        }

                        "previous" ->
                            controller.previous()

                        "play_pause" ->
                            controller
                                .togglePlayPause()

                        "next" ->
                            controller.next()

                        "repeat" -> {
                            val enabled =
                                controller.toggleRepeatOne()
                            toast(
                                if (enabled) {
                                    "Repeat one · увімкнено"
                                } else {
                                    "Repeat one · вимкнено"
                                },
                            )
                        }

                        "theme" ->
                            showThemePicker()

                        "board" ->
                            if (
                                isLayeredBoardTheme(
                                    selectedThemeId,
                                )
                            ) {
                                showBoardTransform()
                            } else if (
                                selectedThemeId ==
                                PlaybackThemeId.VISUALIZER
                            ) {
                                val nextScene =
                                    sceneOrchestrator
                                        .shuffleNow()

                                toast(
                                    "Сцена: " +
                                        nextScene
                                            .visualizerType
                                            .name
                                            .lowercase()
                                            .replace(
                                                '_',
                                                ' ',
                                            ),
                                )
                            } else {
                                toast(
                                    "Board для цього шару — наступний етап",
                                )
                            }

                        "visualizer" ->
                            startActivity(
                                Intent(
                                    this,
                                    com.saney.musicvisualizer
                                        .projectm
                                        .ProjectMActivity::class.java,
                                ),
                            )

                        "export" ->
                            showExportLab()
                    }

                    scheduleNowControlsAutoHide()
                }
            }

        pulseDeckMainSkinView =
            skinView

        // The master plate is authored for the full physical viewport.
        // Do not squeeze it into a padded safe-area rectangle.
        if (!skinView.isMasterPlateMode()) {
            applySafeArea(root)
        }

        layerStack.setContent(
            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
            skinView,
        )

        setContentView(root)
        enableImmersiveFullscreen()

        onPlaybackSnapshot(
            latestSnapshot,
        )

        setNowControlsVisible(
            visible =
                !nowControlsHidden,
            animate = false,
        )

        if (!nowControlsHidden) {
            scheduleNowControlsAutoHide()
        }
    }

    private fun showBoardTransform() {
        if (!isLayeredBoardTheme(selectedThemeId)) {
            toast("Board Transform доступний для layered GF")
            return
        }

        screen = Screen.BOARD_TRANSFORM
        sceneOrchestrator.stop()
        clearScreenRefs()

        var transform =
            boardTransformStore.load(selectedThemeId)

        var groupReaction =
            boardGroupReactionStore.load(
                selectedThemeId,
            )

        val selectedLayer =
            boardEditorLayer

        var selectedLayerTransform =
            selectedLayer?.let { layerId ->
                boardLayerTransformStore.load(
                    selectedThemeId,
                    layerId,
                )
            }

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(COLOR_BG)
            }
        applySafeArea(root)

        val boardView =
            HeroBoardView(this).also { view ->
                view.setGroupTransform(transform)
                view.setGroupReaction(groupReaction)
                view.setLayerTransforms(
                    boardLayerTransformStore.loadAll(
                        selectedThemeId,
                    ),
                )
                view.setPlaying(latestSnapshot.isPlaying)
                view.updateSignal(latestSignal)
            }

        heroBoardView = boardView

        root.addView(
            boardView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        fun layerTitle(
            layerId: BoardLayerId?,
        ): String =
            when (layerId) {
                null -> "Усе GF"
                BoardLayerId.FRAME -> "Frame"
                BoardLayerId.FX -> "FX"
                BoardLayerId.CREATURE -> "Shark"
                BoardLayerId.WORDMARK -> "FARIC"
            }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(14),
                    dp(8),
                    dp(14),
                    dp(8),
                )
                background =
                    panelDrawable(
                        Color.argb(
                            150,
                            4,
                            8,
                            12,
                        ),
                        24,
                        Color.TRANSPARENT,
                        0,
                    )
            }

        header.addView(
            iconButton("‹") {
                showNowPlaying()
            },
        )
        header.addView(
            label(
                "Board · ${layerTitle(selectedLayer)}",
                18f,
                Color.WHITE,
                true,
            ),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart = dp(8)
            },
        )
        header.addView(
            label(
                "LIVE",
                11f,
                COLOR_ACCENT_CYAN,
                true,
            ),
        )

        root.addView(
            header,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64),
                Gravity.TOP,
            ).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
                topMargin = dp(8)
            },
        )

        val panelContent =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(14),
                )
                background =
                    panelDrawable(
                        Color.argb(
                            235,
                            6,
                            10,
                            15,
                        ),
                        28,
                        Color.argb(
                            110,
                            54,
                            202,
                            255,
                        ),
                        1,
                    )
            }

        panelContent.addView(
            label(
                "Редагувати",
                14f,
                Color.WHITE,
                true,
            ),
        )

        val selectorScroll =
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }

        val selectorRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(8),
                )
            }

        val selectorItems =
            listOf(
                null to "Усе",
                BoardLayerId.FRAME to "Frame",
                BoardLayerId.CREATURE to "Shark",
                BoardLayerId.WORDMARK to "FARIC",
                BoardLayerId.FX to "FX",
            )

        selectorItems.forEach { (layerId, title) ->
            val active =
                selectedLayer == layerId

            selectorRow.addView(
                actionPill(
                    text = title,
                    accent = active,
                ) {
                    pendingRestoreScrollY =
                        activeVerticalScroll
                            ?.scrollY
                    pendingRestoreScrollX =
                        activeHorizontalScroll
                            ?.scrollX

                    boardEditorLayer =
                        layerId

                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    dp(84),
                    dp(42),
                ).apply {
                    marginEnd = dp(6)
                },
            )
        }

        selectorScroll.addView(selectorRow)
        activeHorizontalScroll =
            selectorScroll
        panelContent.addView(
            selectorScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58),
            ),
        )

        panelContent.addView(
            label(
                if (selectedLayer == null) {
                    "Жести: 1 палець — рухати весь GF · pinch — розмір · 2 пальці — поворот."
                } else {
                    "Жести зараз редагують тільки ${layerTitle(selectedLayer)}. Реакція на музику при цьому залишається."
                },
                11f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(7)
            },
        )

        fun persistTransform(
            value: BoardTransform,
        ) {
            transform =
                value.sanitized()

            boardTransformStore.save(
                selectedThemeId,
                transform,
            )

            boardView.setGroupTransform(
                transform,
            )
        }

        fun persistReaction(
            value: BoardGroupReaction,
        ) {
            groupReaction =
                value.sanitized()

            boardGroupReactionStore.save(
                selectedThemeId,
                groupReaction,
            )

            boardView.setGroupReaction(
                groupReaction,
            )
        }

        fun persistLayerTransform(
            layerId: BoardLayerId,
            value: BoardLayerTransform,
        ) {
            val safe =
                value.sanitized()

            selectedLayerTransform =
                safe

            boardLayerTransformStore.save(
                selectedThemeId,
                layerId,
                safe,
            )

            boardView.setLayerTransform(
                layerId,
                safe,
            )
        }

        fun addSlider(
            title: String,
            max: Int,
            initial: Int,
            valueText: (Int) -> String,
            onChanged: (Int) -> Unit,
        ): SeekBar {
            val valueLabel =
                label(
                    "$title · ${valueText(initial)}",
                    12f,
                    Color.WHITE,
                    true,
                )

            val seek =
                SeekBar(this).apply {
                    this.max = max
                    progress =
                        initial.coerceIn(
                            0,
                            max,
                        )

                    progressTintList =
                        android.content.res
                            .ColorStateList
                            .valueOf(
                                COLOR_ACCENT_CYAN,
                            )

                    thumbTintList =
                        android.content.res
                            .ColorStateList
                            .valueOf(
                                COLOR_ACCENT_ORANGE,
                            )

                    setOnSeekBarChangeListener(
                        object :
                            SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean,
                            ) {
                                valueLabel.text =
                                    "$title · ${valueText(progress)}"

                                if (fromUser) {
                                    onChanged(progress)
                                }
                            }

                            override fun onStartTrackingTouch(
                                seekBar: SeekBar?,
                            ) = Unit

                            override fun onStopTrackingTouch(
                                seekBar: SeekBar?,
                            ) = Unit
                        },
                    )
                }

            panelContent.addView(valueLabel)
            panelContent.addView(
                seek,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(36),
                ),
            )

            return seek
        }

        if (selectedLayer == null) {
            panelContent.addView(
                label(
                    "Положення всього GF",
                    14f,
                    COLOR_ACCENT_CYAN,
                    true,
                ),
            )

            var xSeek: SeekBar? = null
            var ySeek: SeekBar? = null
            var sizeSeek: SeekBar? = null
            var rotationSeek: SeekBar? = null
            var opacitySeek: SeekBar? = null

            xSeek =
                addSlider(
                    title = "X",
                    max = 100,
                    initial =
                        (
                            transform.xFraction *
                                100f
                            )
                            .toInt(),
                    valueText = {
                        "$it%"
                    },
                ) { progress ->
                    persistTransform(
                        transform.copy(
                            xFraction =
                                progress /
                                    100f,
                        ),
                    )
                }

            ySeek =
                addSlider(
                    title = "Y",
                    max = 100,
                    initial =
                        (
                            transform.yFraction *
                                100f
                            )
                            .toInt(),
                    valueText = {
                        "$it%"
                    },
                ) { progress ->
                    persistTransform(
                        transform.copy(
                            yFraction =
                                progress /
                                    100f,
                        ),
                    )
                }

            sizeSeek =
                addSlider(
                    title = "Розмір",
                    max = 80,
                    initial =
                        (
                            transform.sizeFraction *
                                100f -
                                30f
                            )
                            .toInt(),
                    valueText = {
                        "${it + 30}%"
                    },
                ) { progress ->
                    persistTransform(
                        transform.copy(
                            sizeFraction =
                                (
                                    progress +
                                        30
                                    ) /
                                    100f,
                        ),
                    )
                }

            rotationSeek =
                addSlider(
                    title = "Поворот",
                    max = 90,
                    initial =
                        (
                            transform
                                .rotationDegrees +
                                45f
                            )
                            .toInt(),
                    valueText = {
                        "${it - 45}°"
                    },
                ) { progress ->
                    persistTransform(
                        transform.copy(
                            rotationDegrees =
                                (
                                    progress -
                                        45
                                    )
                                    .toFloat(),
                        ),
                    )
                }

            opacitySeek =
                addSlider(
                    title = "Прозорість",
                    max = 80,
                    initial =
                        (
                            transform.opacity *
                                100f -
                                20f
                            )
                            .toInt(),
                    valueText = {
                        "${it + 20}%"
                    },
                ) { progress ->
                    persistTransform(
                        transform.copy(
                            opacity =
                                (
                                    progress +
                                        20
                                    ) /
                                    100f,
                        ),
                    )
                }

            boardView.setGestureEditing(
                enabled = true,
                layerId = null,
                onTransformChanged = { value ->
                    persistTransform(value)

                    xSeek?.progress =
                        (
                            transform.xFraction *
                                100f
                            )
                            .toInt()
                    ySeek?.progress =
                        (
                            transform.yFraction *
                                100f
                            )
                            .toInt()
                    sizeSeek?.progress =
                        (
                            transform.sizeFraction *
                                100f -
                                30f
                            )
                            .toInt()
                    rotationSeek?.progress =
                        (
                            transform.rotationDegrees +
                                45f
                            )
                            .toInt()
                    opacitySeek?.progress =
                        (
                            transform.opacity *
                                100f -
                                20f
                            )
                            .toInt()
                },
            )

            panelContent.addView(
                label(
                    "Реакція всього GF на музику",
                    14f,
                    COLOR_ACCENT_CYAN,
                    true,
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(8)
                    bottomMargin = dp(4)
                },
            )

            addSlider(
                title = "Плавний поворот",
                max = 60,
                initial =
                    (
                        groupReaction
                            .rotationSwayDegrees *
                            10f
                        )
                        .toInt(),
                valueText = {
                    String.format(
                        Locale.US,
                        "%.1f°",
                        it / 10f,
                    )
                },
            ) { progress ->
                persistReaction(
                    groupReaction.copy(
                        rotationSwayDegrees =
                            progress /
                                10f,
                    ),
                )
            }

            addSlider(
                title = "Stereo L/R",
                max = 100,
                initial =
                    (
                        groupReaction
                            .stereoShiftFraction *
                            1000f
                        )
                        .toInt(),
                valueText = {
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        it / 10f,
                    )
                },
            ) { progress ->
                persistReaction(
                    groupReaction.copy(
                        stereoShiftFraction =
                            progress /
                                1000f,
                    ),
                )
            }

            addSlider(
                title = "Bass ↑↓",
                max = 60,
                initial =
                    (
                        groupReaction
                            .bassFloatFraction *
                            1000f
                        )
                        .toInt(),
                valueText = {
                    String.format(
                        Locale.US,
                        "%.1f%%",
                        it / 10f,
                    )
                },
            ) { progress ->
                persistReaction(
                    groupReaction.copy(
                        bassFloatFraction =
                            progress /
                                1000f,
                    ),
                )
            }

            val presetRow =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }

            presetRow.addView(
                actionPill(
                    text = "Fit Safe Area",
                    accent = false,
                ) {
                    boardTransformStore
                        .fitSafeArea(
                            selectedThemeId,
                        )

                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f,
                ).apply {
                    marginEnd = dp(5)
                },
            )

            presetRow.addView(
                actionPill(
                    text = "Reset усе",
                    accent = false,
                ) {
                    boardTransformStore.reset(
                        selectedThemeId,
                    )
                    boardGroupReactionStore.reset(
                        selectedThemeId,
                    )
                    boardLayerTransformStore
                        .resetAll(
                            selectedThemeId,
                        )
                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f,
                ).apply {
                    marginStart = dp(5)
                },
            )

            panelContent.addView(
                presetRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(7)
                },
            )
        } else {
            val layerId =
                selectedLayer

            var layerValue =
                selectedLayerTransform
                    ?: BoardLayerTransform.default()

            panelContent.addView(
                label(
                    "${layerTitle(layerId)} · окремий шар",
                    14f,
                    COLOR_ACCENT_CYAN,
                    true,
                ),
            )

            var xSeek: SeekBar? = null
            var ySeek: SeekBar? = null
            var sizeSeek: SeekBar? = null
            var rotationSeek: SeekBar? = null
            var opacitySeek: SeekBar? = null

            xSeek =
                addSlider(
                    title = "Зсув X",
                    max = 70,
                    initial =
                        (
                            layerValue
                                .offsetXFraction *
                                100f +
                                35f
                            )
                            .toInt(),
                    valueText = {
                        "${it - 35}%"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            offsetXFraction =
                                (
                                    progress -
                                        35
                                    ) /
                                    100f,
                        )

                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }

            ySeek =
                addSlider(
                    title = "Зсув Y",
                    max = 70,
                    initial =
                        (
                            layerValue
                                .offsetYFraction *
                                100f +
                                35f
                            )
                            .toInt(),
                    valueText = {
                        "${it - 35}%"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            offsetYFraction =
                                (
                                    progress -
                                        35
                                    ) /
                                    100f,
                        )

                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }

            sizeSeek =
                addSlider(
                    title = "Розмір шару",
                    max = 140,
                    initial =
                        (
                            layerValue.scale *
                                100f -
                                40f
                            )
                            .toInt(),
                    valueText = {
                        "${it + 40}%"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            scale =
                                (
                                    progress +
                                        40
                                    ) /
                                    100f,
                        )

                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }

            rotationSeek =
                addSlider(
                    title = "Поворот шару",
                    max = 180,
                    initial =
                        (
                            layerValue
                                .rotationDegrees +
                                90f
                            )
                            .toInt(),
                    valueText = {
                        "${it - 90}°"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            rotationDegrees =
                                (
                                    progress -
                                        90
                                    )
                                    .toFloat(),
                        )

                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }

            opacitySeek =
                addSlider(
                    title = "Прозорість шару",
                    max = 100,
                    initial =
                        (
                            layerValue.opacity *
                                100f
                            )
                            .toInt(),
                    valueText = {
                        "$it%"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            opacity =
                                progress /
                                    100f,
                        )

                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }

            boardView.setGestureEditing(
                enabled = true,
                layerId = layerId,
                onLayerTransformChanged = {
                        changedLayer,
                        value,
                    ->
                    if (changedLayer == layerId) {
                        layerValue = value
                        persistLayerTransform(
                            layerId,
                            value,
                        )

                        xSeek?.progress =
                            (
                                value
                                    .offsetXFraction *
                                    100f +
                                    35f
                                )
                                .toInt()
                        ySeek?.progress =
                            (
                                value
                                    .offsetYFraction *
                                    100f +
                                    35f
                                )
                                .toInt()
                        sizeSeek?.progress =
                            (
                                value.scale *
                                    100f -
                                    40f
                                )
                                .toInt()
                        rotationSeek?.progress =
                            (
                                value
                                    .rotationDegrees +
                                    90f
                                )
                                .toInt()
                        opacitySeek?.progress =
                            (
                                value.opacity *
                                    100f
                                )
                                .toInt()
                    }
                },
            )

            panelContent.addView(
                label(
                    "Значення шару накладаються поверх налаштувань усього GF. Аудіореакція шару лишається активною.",
                    11f,
                    COLOR_MUTED,
                    false,
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(7)
                },
            )

            panelContent.addView(
                actionPill(
                    text = "Скинути ${layerTitle(layerId)}",
                    accent = false,
                ) {
                    boardLayerTransformStore.reset(
                        selectedThemeId,
                        layerId,
                    )
                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(48),
                ).apply {
                    topMargin = dp(8)
                },
            )
        }

        panelContent.addView(
            actionPill(
                text = "Готово",
                accent = true,
            ) {
                showNowPlaying()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
            ).apply {
                topMargin = dp(8)
            },
        )

        val panelScroll =
            ScrollView(this).apply {
                isFillViewport = false
                addView(panelContent)
            }

        activeVerticalScroll =
            panelScroll

        val boardPanelHeight =
            (
                resources.displayMetrics.heightPixels *
                    0.40f
                )
                .toInt()

        root.addView(
            panelScroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                boardPanelHeight,
                Gravity.BOTTOM,
            ).apply {
                leftMargin = dp(10)
                rightMargin = dp(10)
                bottomMargin = dp(8)
            },
        )

        setContentView(root)
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun showExportLab() {
        screen = Screen.EXPORT_LAB
        sceneOrchestrator.stop()
        clearScreenRefs()

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }
        applySafeArea(root)

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setPadding(dp(16), dp(10), dp(16), dp(30))
        }

        activeVerticalScroll =
            scroll

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            iconButton("‹") {
                if (latestSnapshot.trackName != null) {
                    showNowPlaying()
                } else {
                    showLibrary()
                }
            },
        )
        header.addView(
            label(
                "Export Lab",
                24f,
                Color.WHITE,
                true,
            ),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart = dp(10)
            },
        )
        content.addView(header)

        val themeTitle =
            PlaybackThemeRegistry
                .byId(selectedThemeId)
                .title

        content.addView(
            label(
                "Тема: $themeTitle\nТрек: ${latestSnapshot.trackName ?: "—"}",
                15f,
                Color.WHITE,
                true,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            },
        )

        content.addView(
            label(
                "Перший export proof: FARIC рендерить кадр не через screenshot UI, а через детермінований renderer. Це той самий шлях, на якому далі буде MP4.",
                13f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(18)
            },
        )

        val currentTrackUri =
            controller
                .currentTrackUri()
                ?.toString()

        val analysisReady =
            offlineAnalysis != null &&
                offlineAnalysisUri ==
                currentTrackUri

        val analysisText =
            when {
                offlineAnalysisRunning ->
                    "Offline analysis · обробка…"

                analysisReady ->
                    "✓ Offline analysis · ${offlineAnalysis?.signals?.size ?: 0} кадрів сигналу"

                currentTrackUri == null ->
                    "Offline analysis · спочатку обери трек"

                else ->
                    "Offline analysis · ще не виконано"
            }

        content.addView(
            label(
                analysisText,
                13f,
                if (analysisReady) {
                    COLOR_ACCENT_CYAN
                } else {
                    COLOR_MUTED
                },
                analysisReady,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(8)
            },
        )

        content.addView(
            actionPill(
                text =
                    when {
                        offlineAnalysisRunning ->
                            "Аналізую трек…"

                        analysisReady ->
                            "Повторити offline analysis"

                        else ->
                            "Проаналізувати трек офлайн"
                    },
                accent =
                    currentTrackUri != null &&
                        !offlineAnalysisRunning,
            ) {
                if (!offlineAnalysisRunning) {
                    runOfflineAnalysis()
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54),
            ).apply {
                bottomMargin = dp(20)
            },
        )

        content.addView(
            label("Формат", 18f, Color.WHITE, true),
        )

        val formats =
            listOf(
                ExportAspectRatio.VERTICAL_9_16 to "9:16 · TikTok / Shorts",
                ExportAspectRatio.LANDSCAPE_16_9 to "16:9 · YouTube",
                ExportAspectRatio.SQUARE_1_1 to "1:1 · Square",
                ExportAspectRatio.PORTRAIT_4_5 to "4:5 · Feed",
            )

        formats.forEach { (ratio, title) ->
            content.addView(
                actionPill(
                    text =
                        if (ratio == exportAspectRatio) {
                            "✓  $title"
                        } else {
                            title
                        },
                    accent =
                        ratio == exportAspectRatio,
                ) {
                    exportAspectRatio = ratio
                    showExportLab()
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52),
                ).apply {
                    topMargin = dp(8)
                },
            )
        }

        val exportReady =
            isDeterministicExportReady(
                selectedThemeId,
            )

        content.addView(
            actionPill(
                text =
                    if (exportReady) {
                        "Зберегти тестовий кадр PNG"
                    } else {
                        "Ця тема ще не готова до export proof"
                    },
                accent = exportReady,
            ) {
                if (exportReady) {
                    exportProofFrame()
                } else {
                    toast("Обери Hero, Vinyl або Cassette")
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58),
            ).apply {
                topMargin = dp(22)
            },
        )

        content.addView(
            actionPill(
                text =
                    when {
                        !exportReady ->
                            "MP4 proof недоступний для цієї теми"

                        !analysisReady ->
                            "Спочатку виконай offline analysis"

                        else ->
                            "Експортувати 3 с MP4 зі звуком"
                    },
                accent =
                    exportReady &&
                        analysisReady &&
                        !offlineAnalysisRunning,
            ) {
                if (
                    exportReady &&
                    analysisReady &&
                    !offlineAnalysisRunning
                ) {
                    exportProofVideo()
                } else if (!analysisReady) {
                    toast(
                        "Спочатку проаналізуй трек офлайн",
                    )
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58),
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            label(
                "PNG proof перевіряє кадр. MP4 proof рендерить 3 секунди відео з offline timeline, кодує фрагмент музики в AAC і mux-ить звук із H.264.",
                12f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(14)
            },
        )

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun runOfflineAnalysis() {
        val uri =
            controller.currentTrackUri()

        if (uri == null) {
            toast("Спочатку обери локальний трек")
            return
        }

        if (offlineAnalysisRunning) {
            return
        }

        offlineAnalysisRunning = true
        offlineAnalysis = null
        offlineAnalysisUri = null

        if (screen == Screen.EXPORT_LAB) {
            showExportLab()
        }

        toast("Offline analysis запущено")

        thread(name = "faric-offline-audio-analysis") {
            runCatching {
                OfflineAudioAnalyzer.analyze(
                    context = this,
                    uri = uri,
                )
            }.onSuccess { result ->
                offlineAnalysis = result
                offlineAnalysisUri =
                    uri.toString()
                offlineAnalysisRunning = false

                runOnUiThread {
                    toast(
                        "Аналіз готовий · ${result.signals.size} сигналів",
                    )

                    if (screen == Screen.EXPORT_LAB) {
                        showExportLab()
                    }
                }
            }.onFailure { error ->
                offlineAnalysisRunning = false

                runOnUiThread {
                    toast(
                        "Offline analysis: ${error.message ?: error.javaClass.simpleName}",
                    )

                    if (screen == Screen.EXPORT_LAB) {
                        showExportLab()
                    }
                }
            }
        }
    }

    private fun exportProofVideo() {
        val analysis =
            offlineAnalysis
                ?: run {
                    toast("Спочатку виконай offline analysis")
                    return
                }

        val currentUriKey =
            controller
                .currentTrackUri()
                ?.toString()

        if (
            offlineAnalysisUri !=
            currentUriKey
        ) {
            toast("Offline analysis не відповідає поточному треку")
            return
        }

        val snapshot = latestSnapshot
        val theme = selectedThemeId
        val ratio = exportAspectRatio

        if (!isDeterministicExportReady(theme)) {
            toast("Ця тема ще не підтримує H.264 proof")
            return
        }

        toast("Рендерю 3 с H.264 + AAC proof…")

        thread(name = "faric-h264-proof") {
            runCatching {
                ShortVideoExportProof.export(
                    context = this,
                    sourceAudioUri =
                        controller.currentTrackUri()
                            ?: error("Current track URI missing"),
                    project =
                        MusicVideoProject(
                            themeId = theme,
                            aspectRatio = ratio,
                            frameRate = 15,
                        ),
                    analysis = analysis,
                    title =
                        snapshot.trackName.orEmpty(),
                    artist =
                        if (snapshot.trackName == null) {
                            ""
                        } else {
                            "Невідомий виконавець"
                        },
                    startMs =
                        snapshot.positionMs
                            .coerceAtMost(
                                (analysis.durationMs - 500L)
                                    .coerceAtLeast(0L),
                            ),
                )
            }.onSuccess { result ->
                runOnUiThread {
                    if (result.uri != null) {
                        toast(
                            "Готово · ${result.width}×${result.height} · ${result.frameCount} кадрів · зі звуком · Movies/FARIC",
                        )
                    } else {
                        toast("Не вдалося зберегти MP4 proof")
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    toast(
                        "MP4 proof: ${error.message ?: error.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    private fun exportProofFrame() {
        val snapshot = latestSnapshot
        val currentUriKey =
            controller
                .currentTrackUri()
                ?.toString()

        val signal =
            offlineAnalysis
                ?.takeIf {
                    offlineAnalysisUri ==
                        currentUriKey
                }
                ?.signalAt(
                    snapshot.positionMs,
                )
                ?: latestSignal
        val theme = selectedThemeId
        val ratio = exportAspectRatio

        if (!isDeterministicExportReady(theme)) {
            toast("Ця тема ще не підтримує export proof")
            return
        }

        toast("Рендерю ${ratio.width}×${ratio.height}…")

        thread(name = "faric-export-frame") {
            runCatching {
                val project =
                    MusicVideoProject(
                        themeId = theme,
                        aspectRatio = ratio,
                        frameRate = 30,
                    )

                val input =
                    ThemeInput(
                        title = snapshot.trackName.orEmpty(),
                        artist =
                            if (snapshot.trackName == null) {
                                ""
                            } else {
                                "Невідомий виконавець"
                            },
                        durationMs = snapshot.durationMs,
                        positionMs = snapshot.positionMs,
                        amplitude = signal.amplitude,
                        bass = signal.bass,
                        mid = signal.mid,
                        high = signal.high,
                        beat = signal.beatStrength,
                    )

                val bitmap =
                    ExportFrameProof.render(
                        project = project,
                        input = input,
                        timeMs = snapshot.positionMs,
                    )

                val safeTheme =
                    PlaybackThemeRegistry
                        .byId(theme)
                        .title
                        .replace(" ", "-")

                val uri =
                    ExportFrameProof.savePng(
                        context = this,
                        bitmap = bitmap,
                        displayName =
                            "FARIC-$safeTheme-${System.currentTimeMillis()}.png",
                    )

                bitmap.recycle()
                uri
            }.onSuccess { uri ->
                runOnUiThread {
                    if (uri != null) {
                        toast("Готово · Pictures/FARIC")
                    } else {
                        toast("Не вдалося зберегти кадр")
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    toast(
                        "Export proof: ${error.message ?: error.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    private fun isDeterministicExportReady(
        id: PlaybackThemeId,
    ): Boolean =
        id in setOf(
            PlaybackThemeId.NEON_EMBLEM,
            PlaybackThemeId.ENERGY_CORE,
            PlaybackThemeId.ORBITAL_CROWN,
            PlaybackThemeId.STAR_SEED,
            PlaybackThemeId.WAVE_IDOL,
            PlaybackThemeId.VINYL,
            PlaybackThemeId.CASSETTE,
        )

    private fun showThemePicker() {
        screen = Screen.THEME_PICKER
        sceneOrchestrator.stop()
        clearScreenRefs()

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }
        applySafeArea(root)

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(30),
            )
        }

        activeVerticalScroll =
            scroll

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            iconButton("‹") {
                if (latestSnapshot.trackName != null) {
                    showNowPlaying()
                } else {
                    showLibrary()
                }
            },
        )

        header.addView(
            label(
                "Playback Themes",
                24f,
                Color.WHITE,
                true,
            ),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ).apply {
                marginStart = dp(10)
            },
        )

        content.addView(header)

        content.addView(
            label(
                "Обери центральну сцену. Hero Themes вже працюють окремо від projectM.",
                14f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            },
        )

        PlaybackThemeRegistry.all.forEach { spec ->
            val implemented =
                spec.id == PlaybackThemeId.VISUALIZER ||
                    isLayeredBoardTheme(spec.id) ||
                    isStandaloneHeroTheme(spec.id)

            val selected =
                spec.id == selectedThemeId

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(16),
                    dp(14),
                    dp(16),
                    dp(14),
                )

                background =
                    panelDrawable(
                        if (selected) {
                            Color.rgb(25, 38, 45)
                        } else {
                            COLOR_PANEL
                        },
                        22,
                        if (selected) {
                            COLOR_ACCENT_ORANGE
                        } else if (implemented) {
                            COLOR_ACCENT_CYAN
                        } else {
                            Color.argb(
                                70,
                                255,
                                255,
                                255,
                            )
                        },
                        1,
                    )

                addView(
                    label(
                        buildString {
                            if (selected) append("✓  ")
                            append(spec.title)
                            if (!implemented) append("  · СКОРО")
                        },
                        18f,
                        Color.WHITE,
                        true,
                    ),
                )

                addView(
                    label(
                        spec.subtitle,
                        12f,
                        COLOR_MUTED,
                        false,
                    ),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(5)
                    },
                )

                addView(
                    label(
                        when {
                            spec.id == PlaybackThemeId.VISUALIZER ->
                                "FARIC / projectM layered visualizer"

                            isLayeredBoardTheme(spec.id) ->
                                "Layered Board · frame / FX / creature / wordmark"

                            implemented ->
                                "Standalone · bass / mid / high / beat reactive"

                            else ->
                                "Заплановано в Theme Engine"
                        },
                        11f,
                        if (implemented) {
                            COLOR_ACCENT_CYAN
                        } else {
                            COLOR_MUTED
                        },
                        false,
                    ),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        topMargin = dp(7)
                    },
                )

                setOnClickListener {
                    if (!implemented) {
                        toast("${spec.title}: ще будуємо")
                        return@setOnClickListener
                    }

                    selectedThemeId = spec.id
                    themeStore.selectedThemeId = spec.id

                    if (latestSnapshot.trackName != null) {
                        showNowPlaying()
                    } else {
                        showThemePicker()
                    }
                }
            }

            content.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(10)
                },
            )
        }

        scroll.addView(content)
        root.addView(scroll)

        setContentView(root)
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun isLayeredBoardTheme(
        id: PlaybackThemeId,
    ): Boolean =
        id == PlaybackThemeId.CYBER_SHARK

    private fun isStandaloneHeroTheme(
        id: PlaybackThemeId,
    ): Boolean =
        when (id) {
            PlaybackThemeId.NEON_EMBLEM,
            PlaybackThemeId.ENERGY_CORE,
            PlaybackThemeId.ORBITAL_CROWN,
            PlaybackThemeId.STAR_SEED,
            PlaybackThemeId.WAVE_IDOL,
            PlaybackThemeId.VINYL,
            PlaybackThemeId.CASSETTE,
            -> true

            else -> false
        }

    private fun buildPulseDock(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(84)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = panelDrawable(COLOR_PANEL, 26, Color.argb(110, 43, 170, 221), 1)
            visibility = View.GONE
            setOnClickListener { showNowPlaying() }
        }
        dock = card

        miniPulseView = PulseMiniView(this)
        card.addView(miniPulseView, LinearLayout.LayoutParams(dp(58), dp(58)))

        val textArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(6), 0)
        }
        dockTitle = label("Нічого не грає", 16f, Color.WHITE, true).apply { maxLines = 1 }
        dockSubtitle = label("Локальний файл", 12f, COLOR_MUTED, false).apply { maxLines = 1 }
        dockProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(COLOR_ACCENT_CYAN)
        }

        textArea.addView(dockTitle)
        textArea.addView(dockSubtitle)
        textArea.addView(
            dockProgress,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)).apply { topMargin = dp(5) },
        )
        card.addView(textArea, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        dockPlay = roundControl("▶", 52, false) {
            controller.togglePlayPause()
        }
        card.addView(dockPlay)

        return card
    }

    private fun buildBottomNav(active: String): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = dp(64)
            setPadding(0, dp(4), 0, dp(4))
            background = panelDrawable(Color.rgb(15, 22, 29), 26, Color.argb(80, 255, 255, 255), 1)
        }

        nav.addView(
            navItem("⌂", "Бібліотека", active == "library") { showLibrary() },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )
        nav.addView(
            navItem("▥", "Візуалізатор", active == "visualizer") {
                if (latestSnapshot.trackName != null) showNowPlaying() else toast("Спочатку оберіть музику")
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )
        nav.addView(
            navItem("⌕", "Пошук", false) { toast("Пошук — після media scanner") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )
        nav.addView(
            navItem("≡", "Черга", false) { toast("Черга — наступний етап") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )

        return nav
    }

    private fun modeChip(icon: String, title: String, subtitle: String, active: Boolean, action: () -> Unit): View {
        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = panelDrawable(
                if (active) Color.rgb(40, 29, 15) else Color.rgb(8, 25, 31),
                28,
                if (active) COLOR_ACCENT_ORANGE else COLOR_ACCENT_CYAN,
                1,
            )
            setOnClickListener { action() }
        }
        chip.addView(label(icon, 23f, if (active) COLOR_ACCENT_ORANGE else COLOR_ACCENT_CYAN, true))

        val text = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        text.addView(label(title, 15f, Color.WHITE, true))
        text.addView(label(subtitle, 11f, COLOR_MUTED, false))
        chip.addView(text)

        chip.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            marginEnd = dp(10)
        }
        return chip
    }

    private fun libraryCard(icon: String, title: String, subtitle: String, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(122)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = panelDrawable(Color.rgb(14, 21, 28), 24, Color.argb(80, 255, 255, 255), 1)
            addView(label(icon, 28f, COLOR_ACCENT_CYAN, true))
            addView(label(title, 17f, Color.WHITE, true))
            addView(label(subtitle, 11f, COLOR_MUTED, false))
            setOnClickListener { action() }
        }

    private fun actionPill(text: String, accent: Boolean, action: () -> Unit): TextView =
        label(text, 16f, if (accent) Color.BLACK else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = panelDrawable(
                if (accent) COLOR_ACCENT_ORANGE else COLOR_PANEL_2,
                24,
                if (accent) COLOR_ACCENT_ORANGE else Color.argb(100, 255, 255, 255),
                1,
            )
            setOnClickListener { action() }
        }

    private fun iconButton(text: String, action: () -> Unit): TextView =
        label(text, 27f, Color.WHITE, false).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            background = panelDrawable(Color.argb(120, 16, 22, 30), 26, Color.argb(80, 255, 255, 255), 1)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(7) }
        }

    private fun roundControl(text: String, sizeDp: Int, accent: Boolean, action: () -> Unit): TextView =
        label(text, if (sizeDp >= 70) 31f else 20f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            background = panelDrawable(
                if (accent) Color.rgb(42, 29, 16) else Color.rgb(12, 20, 27),
                sizeDp / 2,
                if (accent) COLOR_ACCENT_ORANGE else Color.argb(150, 73, 142, 173),
                if (accent) 2 else 1,
            )
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        }

    private fun actionTile(icon: String, title: String, action: () -> Unit): TextView =
        label("$icon\n$title", 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            minimumHeight = dp(62)
            background = panelDrawable(Color.rgb(12, 20, 27), 18, Color.argb(90, 68, 175, 211), 1)
            setOnClickListener { action() }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

    private fun navItem(icon: String, title: String, active: Boolean, action: () -> Unit): TextView =
        label("$icon\n$title", 11f, if (active) COLOR_ACCENT_ORANGE else COLOR_MUTED, active).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            setOnClickListener { action() }
        }

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            includeFontPadding = false
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun panelDrawable(fill: Int, radiusDp: Int, stroke: Int, strokeDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fill)
            if (strokeDp > 0 && Color.alpha(stroke) > 0) {
                setStroke(dp(strokeDp), stroke)
            }
        }

    private fun chooseTrack() {
        if (hasAnalysisPermission()) {
            openAudio.launch(arrayOf("audio/*"))
        } else {
            pendingOpenAfterPermission = true
            requestAudioAnalysisPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasAnalysisPermission(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun resolveDisplayName(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else uri.lastPathSegment.orEmpty()
            } else {
                uri.lastPathSegment.orEmpty()
            }
        } catch (_: Throwable) {
            uri.lastPathSegment ?: "Аудіофайл"
        } finally {
            cursor?.close()
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val seconds = ms / 1000L
        return String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)
    }

    private fun setNowControlsVisible(
        visible: Boolean,
        animate: Boolean,
    ) {
        val skin =
            pulseDeckMainSkinView
                ?: return

        skin.removeCallbacks(
            nowControlsAutoHideRunnable,
        )

        if (skin.isMasterPlateMode()) {
            nowControlsHidden = false
            skin.setControlsVisible(
                visible = true,
                animate = false,
            )
            return
        }

        if (!visible && controlsAutoHideMode == ControlsAutoHideMode.NEVER) {
            nowControlsHidden = false
            skin.setControlsVisible(visible = true, animate = false)
            skin.setChromeVisibility(
                transportVisible = true,
                quickActionsVisible = true,
            )
            return
        }

        nowControlsHidden = !visible

        skin.setControlsVisible(
            visible = true,
            animate = false,
        )
        skin.setChromeVisibility(
            transportVisible = visible || controlsAutoHideMode == ControlsAutoHideMode.NEVER,
            quickActionsVisible =
                visible ||
                    controlsAutoHideMode != ControlsAutoHideMode.TRANSPORT_AND_QUICK,
        )

        if (visible) {
            scheduleNowControlsAutoHide()
        }
    }

    private fun scheduleNowControlsAutoHide() {
        val skin =
            pulseDeckMainSkinView
                ?: return

        skin.removeCallbacks(
            nowControlsAutoHideRunnable,
        )

        if (skin.isMasterPlateMode() || controlsAutoHideMode == ControlsAutoHideMode.NEVER) {
            return
        }

        if (!nowControlsHidden) {
            skin.postDelayed(
                nowControlsAutoHideRunnable,
                NOW_CONTROLS_AUTO_HIDE_MS,
            )
        }
    }

    private fun showControlsAutoHideDialog() {
        val modes =
            arrayOf(
                ControlsAutoHideMode.NEVER,
                ControlsAutoHideMode.TRANSPORT_ONLY,
                ControlsAutoHideMode.TRANSPORT_AND_QUICK,
            )
        val labels =
            arrayOf(
                "Не ховати",
                "Ховати тільки керування",
                "Ховати керування + нижню панель",
            )
        val checked = modes.indexOf(controlsAutoHideMode).coerceAtLeast(0)

        android.app.AlertDialog.Builder(this)
            .setTitle("Автоприховування")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                controlsAutoHideMode = modes[which]
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CONTROLS_AUTO_HIDE_MODE, controlsAutoHideMode.name)
                    .apply()
                nowControlsHidden = false
                setNowControlsVisible(visible = true, animate = false)
                dialog.dismiss()
            }
            .show()
    }

    private fun clearScreenRefs() {
        nowControlsLayer?.removeCallbacks(
            nowControlsAutoHideRunnable,
        )
        pulseDeckMainSkinView
            ?.removeCallbacks(
                nowControlsAutoHideRunnable,
            )
        sceneView = null
        heroBoardView = null
        heroThemeView = null
        pulseDeckMainSkinView = null
        miniPulseView = null
        dock = null
        dockTitle = null
        dockSubtitle = null
        dockPlay = null
        dockProgress = null
        nowTitle = null
        nowArtist = null
        nowStatus = null
        nowElapsed = null
        nowTotal = null
        nowSeek = null
        nowPlay = null
        nowControlsLayer = null
        activeVerticalScroll = null
        activeHorizontalScroll = null
    }

    private fun restoreScreen(
        target: Screen,
    ) {
        when (target) {
            Screen.LIBRARY ->
                showLibrary()

            Screen.NOW_PLAYING ->
                showNowPlaying()

            Screen.THEME_PICKER ->
                showThemePicker()

            Screen.BOARD_TRANSFORM ->
                if (
                    isLayeredBoardTheme(
                        selectedThemeId,
                    )
                ) {
                    showBoardTransform()
                } else {
                    showNowPlaying()
                }

            Screen.EXPORT_LAB ->
                showExportLab()
        }
    }

    private fun restorePendingScrollPositions() {
        val vertical =
            pendingRestoreScrollY

        if (
            vertical != null &&
            activeVerticalScroll != null
        ) {
            activeVerticalScroll?.post {
                activeVerticalScroll?.scrollTo(
                    0,
                    vertical,
                )
            }
            pendingRestoreScrollY = null
        }

        val horizontal =
            pendingRestoreScrollX

        if (
            horizontal != null &&
            activeHorizontalScroll != null
        ) {
            activeHorizontalScroll?.post {
                activeHorizontalScroll?.scrollTo(
                    horizontal,
                    0,
                )
            }
            pendingRestoreScrollX = null
        }
    }

    private fun enableImmersiveFullscreen() {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun applySafeArea(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            runCatching {
                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())

                view.setPadding(
                    cutout.left,
                    cutout.top,
                    cutout.right,
                    maxOf(cutout.bottom, gestures.bottom),
                )
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS_NAME =
            "faric.preferences"
        private const val KEY_CONTROLS_AUTO_HIDE_MODE =
            "faric.controls_auto_hide_mode"
        private const val KEY_SCREEN =
            "faric.screen"
        private const val KEY_SELECTED_THEME =
            "faric.selected_theme"
        private const val KEY_EXPORT_ASPECT_RATIO =
            "faric.export_aspect_ratio"
        private const val KEY_BOARD_EDITOR_LAYER =
            "faric.board_editor_layer"
        private const val KEY_NOW_CONTROLS_HIDDEN =
            "faric.now_controls_hidden"
        private const val NOW_CONTROLS_AUTO_HIDE_MS =
            6000L
        private const val KEY_HAS_VERTICAL_SCROLL =
            "faric.has_vertical_scroll"
        private const val KEY_VERTICAL_SCROLL_Y =
            "faric.vertical_scroll_y"
        private const val KEY_HAS_HORIZONTAL_SCROLL =
            "faric.has_horizontal_scroll"
        private const val KEY_HORIZONTAL_SCROLL_X =
            "faric.horizontal_scroll_x"

        private val COLOR_BG = Color.rgb(2, 6, 10)
        private val COLOR_PANEL = Color.rgb(13, 20, 27)
        private val COLOR_PANEL_2 = Color.rgb(22, 28, 35)
        private val COLOR_MUTED = Color.rgb(165, 178, 190)
        private val COLOR_ACCENT_ORANGE = Color.rgb(255, 153, 24)
        private val COLOR_ACCENT_CYAN = Color.rgb(35, 211, 238)
    }
}
