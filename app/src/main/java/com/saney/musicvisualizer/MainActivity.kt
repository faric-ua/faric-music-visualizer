package com.saney.musicvisualizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.DocumentsContract
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
import com.saney.musicvisualizer.board.GraphicFigureCatalog
import com.saney.musicvisualizer.board.BoardLayerId
import com.saney.musicvisualizer.board.BoardLayerTransform
import com.saney.musicvisualizer.board.BoardLayerTransformStore
import com.saney.musicvisualizer.board.BoardTransform
import com.saney.musicvisualizer.board.BoardTransformStore
import com.saney.musicvisualizer.export.CompositionExportConfig
import com.saney.musicvisualizer.export.CyberSharkExportConfig
import com.saney.musicvisualizer.export.ExportFrameProof
import com.saney.musicvisualizer.export.OfflineAnalysisResult
import com.saney.musicvisualizer.export.OfflineAudioAnalyzer
import com.saney.musicvisualizer.export.OfflinePcmCache
import com.saney.musicvisualizer.export.OfflinePcmCacheResult
import com.saney.musicvisualizer.export.ShortVideoExportProof
import com.saney.musicvisualizer.playback.PlaybackController
import com.saney.musicvisualizer.playback.PlaybackSnapshot
import com.saney.musicvisualizer.playback.QueueTrack
import com.saney.musicvisualizer.projectm.ProjectMBridge
import com.saney.musicvisualizer.projectm.ProjectMLibraryManager
import com.saney.musicvisualizer.projectm.ProjectMOfflineFrameRequest
import com.saney.musicvisualizer.projectm.ProjectMPerformanceProfile
import com.saney.musicvisualizer.projectm.ProjectMStateStore
import com.saney.musicvisualizer.projectm.ProjectMView
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
import com.saney.musicvisualizer.ui.BigEqualizerView
import com.saney.musicvisualizer.ui.OverVisualizationView
import com.saney.musicvisualizer.ui.PulseDeckLayerStack
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.ReactiveSceneView
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.roundToInt

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

    private var offlinePcmCache: OfflinePcmCacheResult? = null
    private var offlinePcmCacheUri: String? = null

    private var sceneView: ReactiveSceneView? = null
    private var projectMMainView: ProjectMView? = null
    private var projectMMainResumed = false
    private var projectMExportSnapshot: Bitmap? = null
    private var projectMExportLiveView: ProjectMView? = null
    private var projectMExportLiveResumed = false
    private var effectsView: ReactiveSceneView? = null
    private var bigEqualizerView: BigEqualizerView? = null
    private var pulseDeckLayerStack: PulseDeckLayerStack? = null
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

    private val emergencyRecoveryHandler =
        android.os.Handler(
            android.os.Looper.getMainLooper(),
        )
    private var emergencyRecoveryArmed = false
    private var emergencyRecoveryDownX = 0f
    private var emergencyRecoveryDownY = 0f
    private val emergencyRecoveryRunnable =
        Runnable {
            if (
                emergencyRecoveryArmed &&
                screen == Screen.NOW_PLAYING
            ) {
                emergencyRecoveryArmed = false
                window.decorView
                    .performHapticFeedback(
                        android.view.HapticFeedbackConstants.LONG_PRESS,
                    )
                showEmergencyRecoveryDialog()
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
                offlinePcmCache
                    ?.file
                    ?.delete()
                offlinePcmCache = null
                offlinePcmCacheUri = null

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


    private val openProjectMVisualizer =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            if (
                ProjectMStateStore(this)
                    .lastPresetFileOrNull() != null
            ) {
                layerPrefs()
                    .edit()
                    .putBoolean(
                        layerPreferenceKey(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                        ),
                        true,
                    )
                    .apply()
            }

            if (screen == Screen.NOW_PLAYING) {
                showNowPlaying()
            }
        }

    private val exportLayerConfiguration =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json",
            ),
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            runCatching {
                contentResolver
                    .openOutputStream(
                        uri,
                    )
                    ?.bufferedWriter()
                    ?.use { writer ->
                        writer.write(
                            buildLayerConfigurationJson(),
                        )
                    }
                    ?: error(
                        "Не вдалося відкрити файл",
                    )
            }.onSuccess {
                toast(
                    "Конфігурацію шарів збережено",
                )
            }.onFailure { error ->
                toast(
                    "Експорт: " +
                        (
                            error.message
                                ?: error.javaClass.simpleName
                            ),
                )
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

        ensureVisualizerLayerMigration()
        ensureVisualizerObjectModelMigration()

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
            effectsView?.setScene(spec)
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

    override fun dispatchTouchEvent(
        event: MotionEvent,
    ): Boolean {
        if (screen == Screen.NOW_PLAYING) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    emergencyRecoveryArmed = true
                    emergencyRecoveryDownX = event.rawX
                    emergencyRecoveryDownY = event.rawY
                    emergencyRecoveryHandler
                        .removeCallbacks(
                            emergencyRecoveryRunnable,
                        )
                    emergencyRecoveryHandler
                        .postDelayed(
                            emergencyRecoveryRunnable,
                            EMERGENCY_RECOVERY_HOLD_MS,
                        )
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    emergencyRecoveryArmed = false
                    emergencyRecoveryHandler
                        .removeCallbacks(
                            emergencyRecoveryRunnable,
                        )
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx =
                        event.rawX -
                            emergencyRecoveryDownX
                    val dy =
                        event.rawY -
                            emergencyRecoveryDownY
                    val limit =
                        dp(
                            24,
                        )
                            .toFloat()

                    if (
                        dx * dx +
                            dy * dy >
                        limit * limit
                    ) {
                        emergencyRecoveryArmed = false
                        emergencyRecoveryHandler
                            .removeCallbacks(
                                emergencyRecoveryRunnable,
                            )
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    emergencyRecoveryArmed = false
                    emergencyRecoveryHandler
                        .removeCallbacks(
                            emergencyRecoveryRunnable,
                        )
                }
            }
        } else {
            emergencyRecoveryArmed = false
            emergencyRecoveryHandler
                .removeCallbacks(
                    emergencyRecoveryRunnable,
                )
        }

        return super.dispatchTouchEvent(
            event,
        )
    }

    private fun showEmergencyRecoveryDialog() {
        PulseDeckDialogs.show(
            context = this,
            title = "Аварійне відновлення PulseDeck",
            message =
                "Меню можна повернути навіть якщо всі кнопки керування були приховані.",
            actions =
                listOf(
                    PulseDeckDialogs.Action(
                        label =
                            "Відновити доступ до меню",
                        accent = true,
                    ) {
                        restoreMenuAccess()
                    },
                    PulseDeckDialogs.Action(
                        label =
                            "Скинути всі кнопки HUD",
                    ) {
                        resetHudControlsVisibility()
                    },
                    PulseDeckDialogs.Action(
                        label =
                            "Скасувати",
                    ) {},
                ),
        )
    }

    private fun restoreMenuAccess() {
        val essential =
            listOf(
                "menu",
                "back",
                "quick_rail",
                "theme",
                "board",
                "visualizer",
                "export",
            )

        val editor =
            layerPrefs()
                .edit()

        essential.forEach { objectId ->
            editor.putBoolean(
                layerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                    objectId,
                ),
                true,
            )
        }

        editor.apply()

        nowControlsHidden = false
        showNowPlaying()
        toast(
            "Доступ до меню відновлено",
        )
    }

    private fun resetHudControlsVisibility() {
        val editor =
            layerPrefs()
                .edit()

        PULSEDECK_EXPORT_OBJECT_IDS
            .forEach { objectId ->
                editor.putBoolean(
                    layerObjectPreferenceKey(
                        PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                        objectId,
                    ),
                    true,
                )
            }

        editor.apply()

        controlsAutoHideMode =
            ControlsAutoHideMode.NEVER
        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE,
        )
            .edit()
            .putString(
                KEY_CONTROLS_AUTO_HIDE_MODE,
                ControlsAutoHideMode.NEVER.name,
            )
            .apply()

        nowControlsHidden = false
        showNowPlaying()
        toast(
            "HUD і меню відновлено",
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
        updateProjectMRenderState()

        if (
            screen == Screen.EXPORT_LAB &&
            !projectMExportLiveResumed
        ) {
            projectMExportLiveView
                ?.onResume()
            projectMExportLiveResumed =
                projectMExportLiveView !=
                null
        }
    }

    override fun onStop() {
        // Do not pause playback here. Screen lock and Home both stop the Activity,
        // but a music player must keep playing. A MediaSessionService migration is
        // tracked separately for full long-lived background playback/notification.
        sceneOrchestrator.stop()
        if (projectMMainResumed) {
            projectMMainView?.onPause()
            projectMMainResumed = false
        }
        if (projectMExportLiveResumed) {
            projectMExportLiveView?.onPause()
            projectMExportLiveResumed = false
        }
        controller.listener = null
        super.onStop()
    }

    override fun onDestroy() {
        sceneOrchestrator.close()
        ProjectMBridge
            .endOfflineExport()
        offlinePcmCache
            ?.file
            ?.delete()
        offlinePcmCache =
            null
        offlinePcmCacheUri =
            null
        projectMExportSnapshot =
            null
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
        effectsView?.setPlaying(snapshot.isPlaying)
        bigEqualizerView?.setPlaying(snapshot.isPlaying)
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

        updateSceneOrchestratorState()
        updateProjectMRenderState()

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
        effectsView?.updateSignal(signal)
        bigEqualizerView?.updateSignal(signal)
        heroBoardView?.updateSignal(signal)
        heroThemeView?.updateSignal(signal)
        pulseDeckMainSkinView?.updateSignal(signal)
        miniPulseView?.updateSignal(signal)
    }

    private fun updateSceneOrchestratorState() {
        val active =
            (
                screen == Screen.NOW_PLAYING ||
                    screen == Screen.BOARD_TRANSFORM
                ) &&
                latestSnapshot.isPlaying &&
                layerVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                ) &&
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "faric_reactive",
                )

        if (active) {
            sceneOrchestrator.start()
        } else {
            sceneOrchestrator.stop()
        }
    }

    private fun updateProjectMRenderState() {
        val shouldRender =
            (
                screen == Screen.NOW_PLAYING ||
                    screen == Screen.BOARD_TRANSFORM
                ) &&
                layerVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                ) &&
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                ) &&
                projectMMainView !=
                    null

        if (
            shouldRender &&
            !projectMMainResumed
        ) {
            projectMMainView?.onResume()
            projectMMainResumed = true
        } else if (
            !shouldRender &&
            projectMMainResumed
        ) {
            projectMMainView?.onPause()
            projectMMainResumed = false
        }
    }

    private fun showLibrary() {
        screen = Screen.LIBRARY
        sceneOrchestrator.stop()
        clearScreenRefs()

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }

        attachExportProjectMPreview(
            root,
        )
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

        content.addView(
            label(
                "v${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
                11f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(2)
                bottomMargin = dp(2)
            },
        )

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

    private fun attachLayer0Visualizer(
        stack: PulseDeckLayerStack,
    ) {
        val projectMState =
            ProjectMStateStore(
                this,
            )
        val projectMPreset =
            projectMState
                .lastPresetFileOrNull()

        val projectM =
            if (
                projectMPreset != null &&
                ProjectMLibraryManager
                    .textureDir(this)
                    .isDirectory
            ) {
                ProjectMView(
                    context = this,
                    initialPreset = projectMPreset,
                    textureDirectory =
                        ProjectMLibraryManager
                            .textureDir(this),
                    profile =
                        ProjectMPerformanceProfile
                            .BALANCED_BACKGROUND,
                    foregroundSample =
                        projectMState
                            .foregroundSample,
                    onTapNext = {},
                )
            } else {
                null
            }

        projectMMainView =
            projectM

        stack.setContent(
            PulseDeckLayerStack.Layer.VISUALIZER,
            projectM,
        )

        val liveScene =
            ReactiveSceneView(
                this,
                renderBackground = false,
                renderVisualizer = true,
            ).also { view ->
                view.setScene(
                    currentScene,
                )
                view.updateSignal(
                    latestSignal,
                )
                view.setPlaying(
                    latestSnapshot.isPlaying,
                )
            }

        sceneView =
            liveScene

        stack.setContent(
            PulseDeckLayerStack.Layer.FARIC_REACTIVE,
            liveScene,
        )
    }

    private fun currentGraphicFigureThemeId():
        PlaybackThemeId {
        val saved =
            layerPrefs()
                .getString(
                    KEY_GF_THEME_ID,
                    null,
                )
                ?.let { name ->
                    runCatching {
                        PlaybackThemeId
                            .valueOf(
                                name,
                            )
                    }.getOrNull()
                }

        return GraphicFigureCatalog
            .normalize(
                saved
                    ?: PlaybackThemeId
                        .CYBER_SHARK,
            )
    }

    private fun setGraphicFigureThemeId(
        themeId: PlaybackThemeId,
    ) {
        val safe =
            GraphicFigureCatalog
                .normalize(
                    themeId,
                )

        layerPrefs()
            .edit()
            .putString(
                KEY_GF_THEME_ID,
                safe.name,
            )
            .apply()

        selectedThemeId =
            safe
        themeStore.selectedThemeId =
            safe

        setLayerVisible(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            true,
        )

        val selectedEditorLayer =
            boardEditorLayer

        if (
            selectedEditorLayer != null &&
            !GraphicFigureCatalog
                .supports(
                    safe,
                    selectedEditorLayer,
                )
        ) {
            boardEditorLayer =
                null
        }
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
        pulseDeckLayerStack = layerStack

        root.addView(
            layerStack,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        attachLayer0Visualizer(
            layerStack,
        )

        val overVisualization =
            OverVisualizationView(this)

        layerStack.setContent(
            PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
            overVisualization,
        )

        val bigEqualizer =
            BigEqualizerView(this).also { view ->
                view.updateSignal(latestSignal)
                view.setPlaying(latestSnapshot.isPlaying)
            }
        bigEqualizerView = bigEqualizer
        layerStack.setContent(
            PulseDeckLayerStack.Layer.BIG_EQUALIZER,
            bigEqualizer,
        )

        val effects =
            ReactiveSceneView(
                this,
                renderBackground = true,
                renderVisualizer = false,
                transparentBackground = true,
            ).also { view ->
                view.setScene(currentScene)
                view.updateSignal(latestSignal)
                view.setPlaying(latestSnapshot.isPlaying)
            }
        effectsView = effects
        layerStack.setContent(
            PulseDeckLayerStack.Layer.EFFECTS,
            effects,
        )

        val gfThemeId =
            currentGraphicFigureThemeId()

        val boardView =
            HeroBoardView(
                this,
                gfThemeId,
            ).also { view ->
                view.setGroupTransform(
                    boardTransformStore.load(
                        gfThemeId,
                    ),
                )
                view.setGroupReaction(
                    boardGroupReactionStore.load(
                        gfThemeId,
                    ),
                )
                view.setLayerTransforms(
                    boardLayerTransformStore.loadAll(
                        gfThemeId,
                    ),
                )
                HeroBoardView.ObjectId.entries
                    .forEach { objectId ->
                        view.setObjectVisible(
                            objectId,
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                objectId.name.lowercase(),
                            ),
                        )
                    }
                view.setPlaying(
                    latestSnapshot.isPlaying,
                )
                view.updateSignal(
                    latestSignal,
                )
            }

        heroBoardView =
            boardView
        layerStack.setContent(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            boardView,
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
                view.objectVisibilityItems()
                    .forEach { item ->
                        view.setObjectVisible(
                            item.id,
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                                item.id,
                            ),
                        )
                    }
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
                            PulseDeckDialogs.showActionList(
                                context = this,
                                title = "PulseDeck tools",
                                items =
                                    listOf(
                                        "Center Calibration",
                                        "Template Constructor",
                                        "Object Constructor",
                                        "Шари / Layers",
                                        "Сети / Sets",
                                        "Автоприховування",
                                    ),
                                cancelLabel = "Закрити",
                            ) { which ->
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
                                        showPulseDeckLayersDialog()

                                    4 ->
                                        showCompositionSetsDialog()

                                    5 ->
                                        showControlsAutoHideDialog()
                                }
                            }

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
                            showBoardTransform()

                        "visualizer" -> {
                            if (projectMMainResumed) {
                                projectMMainView?.onPause()
                                projectMMainResumed = false
                            }
                            projectMMainView
                                ?.releaseProjectM()
                            projectMMainView = null

                            openProjectMVisualizer.launch(
                                Intent(
                                    this,
                                    com.saney.musicvisualizer
                                        .projectm
                                        .ProjectMActivity::class.java,
                                ),
                            )
                        }

                        "export" ->
                            openExportLabWithProjectMCapture()
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

        applyPulseDeckLayerVisibility(layerStack)

        setContentView(root)
        updateProjectMRenderState()
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

    private fun ensureVisualizerLayerMigration() {
        val prefs =
            layerPrefs()

        if (
            prefs.getBoolean(
                KEY_VISUALIZER_SPLIT_MIGRATED,
                false,
            )
        ) {
            return
        }

        val oldParentVisible =
            when {
                prefs.contains(
                    "layer_visualizer",
                ) ->
                    prefs.getBoolean(
                        "layer_visualizer",
                        true,
                    )

                prefs.contains(
                    "layer_0",
                ) ->
                    prefs.getBoolean(
                        "layer_0",
                        true,
                    )

                else ->
                    true
            }

        val oldProjectMVisible =
            prefs.getBoolean(
                "object_visualizer_projectm",
                true,
            )
        val oldFaricVisible =
            prefs.getBoolean(
                "object_visualizer_faric_reactive",
                true,
            )

        prefs.edit()
            .putBoolean(
                layerPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                ),
                oldParentVisible &&
                    oldProjectMVisible,
            )
            .putBoolean(
                layerPreferenceKey(
                    PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                ),
                oldParentVisible &&
                    oldFaricVisible,
            )
            .putBoolean(
                KEY_VISUALIZER_SPLIT_MIGRATED,
                true,
            )
            .apply()
    }

    private fun ensureVisualizerObjectModelMigration() {
        val prefs =
            layerPrefs()

        if (
            prefs.getBoolean(
                KEY_VISUALIZER_OBJECTS_MIGRATED,
                false,
            )
        ) {
            return
        }

        val projectMVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            )
        val faricVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.FARIC_REACTIVE,
            )

        prefs.edit()
            .putBoolean(
                layerPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                ),
                true,
            )
            .putBoolean(
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                ),
                projectMVisible,
            )
            .putBoolean(
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "faric_reactive",
                ),
                faricVisible,
            )
            .putBoolean(
                KEY_VISUALIZER_OBJECTS_MIGRATED,
                true,
            )
            .apply()
    }

    private fun layerPrefs() =
        getSharedPreferences("pulsedeck_layers", MODE_PRIVATE)

    private fun layerPreferenceKey(
        layer: PulseDeckLayerStack.Layer,
    ): String =
        "layer_" +
            layer.name
                .lowercase()

    private fun layerVisible(
        layer: PulseDeckLayerStack.Layer,
        defaultValue: Boolean = true,
    ): Boolean {
        val prefs =
            layerPrefs()
        val stableKey =
            layerPreferenceKey(layer)

        if (prefs.contains(stableKey)) {
            return prefs.getBoolean(
                stableKey,
                defaultValue,
            )
        }

        // One-time compatibility for the old 0..4 stack.
        val oldZ =
            when (layer) {
                PulseDeckLayerStack.Layer.VISUALIZER -> 0
                PulseDeckLayerStack.Layer.BIG_EQUALIZER -> 1
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES -> 2
                PulseDeckLayerStack.Layer.GIF_ANIMATION -> 3
                PulseDeckLayerStack.Layer.EFFECTS -> 4
                else -> null
            }

        return oldZ
            ?.let { z ->
                val oldKey =
                    "layer_" +
                        z
                if (prefs.contains(oldKey)) {
                    prefs.getBoolean(
                        oldKey,
                        defaultValue,
                    )
                } else {
                    defaultValue
                }
            }
            ?: defaultValue
    }

    private fun setLayerVisible(
        layer: PulseDeckLayerStack.Layer,
        visible: Boolean,
    ) {
        layerPrefs()
            .edit()
            .putBoolean(
                layerPreferenceKey(layer),
                visible,
            )
            .apply()
        if (
            layer ==
                PulseDeckLayerStack.Layer.VISUALIZER
        ) {
            pulseDeckLayerStack
                ?.setLayerVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    visible &&
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "projectm",
                        ),
                )
            pulseDeckLayerStack
                ?.setLayerVisible(
                    PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                    visible &&
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "faric_reactive",
                        ),
                )
            updateProjectMRenderState()
            updateSceneOrchestratorState()
        } else {
            pulseDeckLayerStack
                ?.setLayerVisible(
                    layer,
                    visible,
                )
        }
    }


    private fun legacyLayerObjectPreferenceKey(
        layer: PulseDeckLayerStack.Layer,
        objectId: String,
    ): String =
        "object_" +
            layer.name.lowercase() +
            "_" +
            objectId.lowercase()

    private fun layerObjectPreferenceKey(
        layer: PulseDeckLayerStack.Layer,
        objectId: String,
    ): String =
        if (
            layer ==
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES
        ) {
            "object_" +
                layer.name.lowercase() +
                "_" +
                currentGraphicFigureThemeId()
                    .name
                    .lowercase() +
                "_" +
                objectId.lowercase()
        } else {
            legacyLayerObjectPreferenceKey(
                layer,
                objectId,
            )
        }

    private fun layerObjectVisible(
        layer: PulseDeckLayerStack.Layer,
        objectId: String,
        defaultValue: Boolean = true,
    ): Boolean {
        val prefs =
            layerPrefs()
        val key =
            layerObjectPreferenceKey(
                layer,
                objectId,
            )

        if (prefs.contains(key)) {
            return prefs.getBoolean(
                key,
                defaultValue,
            )
        }

        // Preserve legacy Cyber Shark visibility, but do not let old Shark
        // toggles leak into a newly selected Graphic Figure such as Panther.
        if (
            layer ==
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES &&
            currentGraphicFigureThemeId() ==
                PlaybackThemeId.CYBER_SHARK
        ) {
            val legacyKey =
                legacyLayerObjectPreferenceKey(
                    layer,
                    objectId,
                )

            if (prefs.contains(legacyKey)) {
                return prefs.getBoolean(
                    legacyKey,
                    defaultValue,
                )
            }
        }

        return defaultValue
    }

    private fun setLayerObjectVisible(
        layer: PulseDeckLayerStack.Layer,
        objectId: String,
        visible: Boolean,
    ) {
        layerPrefs()
            .edit()
            .putBoolean(
                layerObjectPreferenceKey(
                    layer,
                    objectId,
                ),
                visible,
            )
            .apply()

        when (layer) {
            PulseDeckLayerStack.Layer.VISUALIZER ->
                when (objectId) {
                    "projectm" -> {
                        pulseDeckLayerStack
                            ?.setLayerVisible(
                                PulseDeckLayerStack.Layer.VISUALIZER,
                                layerVisible(
                                    PulseDeckLayerStack.Layer.VISUALIZER,
                                ) &&
                                    visible,
                            )
                        updateProjectMRenderState()
                    }

                    "faric_reactive" -> {
                        pulseDeckLayerStack
                            ?.setLayerVisible(
                                PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                                layerVisible(
                                    PulseDeckLayerStack.Layer.VISUALIZER,
                                ) &&
                                    visible,
                            )
                        updateSceneOrchestratorState()
                    }
                }

            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES ->
                HeroBoardView.ObjectId.entries
                    .firstOrNull { candidate ->
                        candidate.name.equals(
                            objectId,
                            ignoreCase = true,
                        )
                    }
                    ?.let { objectIdValue ->
                        heroBoardView
                            ?.setObjectVisible(
                                objectIdValue,
                                visible,
                            )
                    }

            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED ->
                pulseDeckMainSkinView
                    ?.setObjectVisible(
                        objectId,
                        visible,
                    )

            else -> Unit
        }
    }

    private data class LayerMenuObject(
        val id: String,
        val label: String,
        val groupCode: String? = null,
        val groupLabel: String? = null,
    )

    private fun layerMenuObjects(
        layer: PulseDeckLayerStack.Layer,
    ): List<LayerMenuObject> =
        when (layer) {
            PulseDeckLayerStack.Layer.VISUALIZER ->
                listOf(
                    LayerMenuObject(
                        "projectm",
                        "projectM",
                    ),
                    LayerMenuObject(
                        "faric_reactive",
                        "FARIC Reactive",
                    ),
                )

            PulseDeckLayerStack.Layer.FARIC_REACTIVE ->
                emptyList()

            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES -> {
                val gfThemeId =
                    currentGraphicFigureThemeId()
                val creatureLabel =
                    GraphicFigureCatalog
                        .creatureLabel(
                            gfThemeId,
                        )

                listOf(
                    BoardLayerId.BACKGROUND to
                        LayerMenuObject(
                            "background",
                            "GF background / glow",
                        ),
                    BoardLayerId.FRAME to
                        LayerMenuObject(
                            "frame",
                            "Frame",
                        ),
                    BoardLayerId.FX to
                        LayerMenuObject(
                            "fx",
                            "FX",
                        ),
                    BoardLayerId.CREATURE to
                        LayerMenuObject(
                            "creature",
                            creatureLabel,
                        ),
                    BoardLayerId.WORDMARK to
                        LayerMenuObject(
                            "wordmark",
                            "Wordmark",
                        ),
                )
                    .filter { (layerId, _) ->
                        GraphicFigureCatalog
                            .supports(
                                gfThemeId,
                                layerId,
                            )
                    }
                    .map { (_, item) ->
                        item
                    }
            }

            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED ->
                pulseDeckMainSkinView
                    ?.objectVisibilityItems()
                    .orEmpty()
                    .map { item ->
                        LayerMenuObject(
                            id = item.id,
                            label = item.label,
                            groupCode = item.groupCode,
                            groupLabel = item.groupLabel,
                        )
                    }

            else -> emptyList()
        }

    private fun controllablePulseDeckLayers() =
        listOf(
            PulseDeckLayerStack.Layer.VISUALIZER,
            PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
            PulseDeckLayerStack.Layer.BIG_EQUALIZER,
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            PulseDeckLayerStack.Layer.GIF_ANIMATION,
            PulseDeckLayerStack.Layer.EFFECTS,
        )

    private fun applyPulseDeckLayerVisibility(
        stack: PulseDeckLayerStack,
    ) {
        controllablePulseDeckLayers()
            .forEach { layer ->
                if (
                    layer ==
                        PulseDeckLayerStack.Layer.VISUALIZER
                ) {
                    val parentVisible =
                        layerVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                        )

                    stack.setLayerVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        parentVisible &&
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.VISUALIZER,
                                "projectm",
                            ),
                    )
                    stack.setLayerVisible(
                        PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                        parentVisible &&
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.VISUALIZER,
                                "faric_reactive",
                            ),
                    )
                } else {
                    stack.setLayerVisible(
                        layer,
                        layerVisible(layer),
                    )
                }
            }

        // Layer 7 is the locked PulseDeck HUD and is never user-toggleable.
        stack.setLayerVisible(
            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
            true,
        )
    }

    private fun buildLayerConfigurationJson(): String {
        val root =
            JSONObject().apply {
                put(
                    "schema",
                    "faric-layer-config-v1",
                )
                put(
                    "appVersion",
                    BuildConfig.VERSION_NAME,
                )
            }

        val layersJson =
            JSONObject()

        PulseDeckLayerStack.Layer.entries
            .filter { layer ->
                layer !=
                    PulseDeckLayerStack.Layer.FARIC_REACTIVE
            }
            .sortedBy { layer ->
                layer.z
            }
            .forEach { layer ->
                val visible =
                    when (layer) {
                        PulseDeckLayerStack.Layer.PULSEDECK_LOCKED ->
                            true

                        PulseDeckLayerStack.Layer.SERVICE_OVERLAY ->
                            pulseDeckLayerStack
                                ?.isLayerVisible(
                                    layer,
                                )
                                ?: true

                        else ->
                            layerVisible(
                                layer,
                            )
                    }

                layersJson.put(
                    layer.name,
                    visible,
                )
            }

        root.put(
            "layers",
            layersJson,
        )

        val objectsJson =
            JSONObject()

        PulseDeckLayerStack.Layer.entries
            .forEach { layer ->
                val items =
                    layerMenuObjects(
                        layer,
                    )

                if (items.isNotEmpty()) {
                    val layerObjects =
                        JSONObject()

                    items.forEach { item ->
                        layerObjects.put(
                            item.id,
                            layerObjectVisible(
                                layer,
                                item.id,
                            ),
                        )
                    }

                    objectsJson.put(
                        layer.name,
                        layerObjects,
                    )
                }
            }

        root.put(
            "objects",
            objectsJson,
        )

        val gfThemeId =
            currentGraphicFigureThemeId()
        val group =
            boardTransformStore.load(
                gfThemeId,
            )
        val reaction =
            boardGroupReactionStore.load(
                gfThemeId,
            )

        val gfJson =
            JSONObject().apply {
                put(
                    "theme",
                    gfThemeId.name,
                )
                put(
                    "group",
                    JSONObject().apply {
                        put(
                            "x",
                            group.xFraction,
                        )
                        put(
                            "y",
                            group.yFraction,
                        )
                        put(
                            "scale",
                            group.sizeFraction,
                        )
                        put(
                            "rotation",
                            group.rotationDegrees,
                        )
                        put(
                            "opacity",
                            group.opacity,
                        )
                    },
                )
                put(
                    "reaction",
                    JSONObject().apply {
                        put(
                            "rotationSwayDegrees",
                            reaction.rotationSwayDegrees,
                        )
                        put(
                            "stereoShiftFraction",
                            reaction.stereoShiftFraction,
                        )
                        put(
                            "bassFloatFraction",
                            reaction.bassFloatFraction,
                        )
                    },
                )
            }

        val gfLayers =
            JSONObject()

        BoardLayerId.entries
            .forEach { layerId ->
                val value =
                    boardLayerTransformStore.load(
                        gfThemeId,
                        layerId,
                    )

                gfLayers.put(
                    layerId.name,
                    JSONObject().apply {
                        put(
                            "x",
                            value.offsetXFraction,
                        )
                        put(
                            "y",
                            value.offsetYFraction,
                        )
                        put(
                            "scale",
                            value.scale,
                        )
                        put(
                            "rotation",
                            value.rotationDegrees,
                        )
                        put(
                            "opacity",
                            value.opacity,
                        )
                    },
                )
            }

        gfJson.put(
            "layers",
            gfLayers,
        )
        root.put(
            "gf",
            gfJson,
        )

        ProjectMStateStore(this)
            .lastPresetFileOrNull()
            ?.let { preset ->
                root.put(
                    "projectM",
                    JSONObject().apply {
                        put(
                            "presetId",
                            ProjectMLibraryManager
                                .presetId(
                                    preset,
                                ),
                        )
                        put(
                            "presetPath",
                            preset.absolutePath,
                        )
                        put(
                            "foreground",
                            ProjectMStateStore(
                                this@MainActivity,
                            )
                                .foregroundSample
                                .name,
                        )
                    },
                )
            }

        return root.toString(
            2,
        )
    }

    private fun compositionSetsPrefs() =
        getSharedPreferences(
            "pulsedeck_composition_sets",
            MODE_PRIVATE,
        )

    private fun compositionSetNames(): List<String> {
        val raw =
            compositionSetsPrefs()
                .getString(
                    KEY_COMPOSITION_SET_NAMES,
                    "[]",
                )
                ?: "[]"

        return runCatching {
            val array =
                JSONArray(
                    raw,
                )
            buildList {
                repeat(
                    array.length(),
                ) { index ->
                    val name =
                        array.optString(
                            index,
                        )
                            .trim()
                    if (
                        name.isNotBlank() &&
                        name !in this
                    ) {
                        add(
                            name,
                        )
                    }
                }
            }
        }.getOrDefault(
            emptyList(),
        )
    }

    private fun persistCompositionSetNames(
        names: List<String>,
    ) {
        val array =
            JSONArray()
        names.forEach { name ->
            array.put(
                name,
            )
        }

        compositionSetsPrefs()
            .edit()
            .putString(
                KEY_COMPOSITION_SET_NAMES,
                array.toString(),
            )
            .apply()
    }

    private fun saveCurrentCompositionSet(
        name: String,
    ) {
        val safeName =
            name.trim()

        if (safeName.isBlank()) {
            toast(
                "Назва сету порожня",
            )
            return
        }

        val prefs =
            compositionSetsPrefs()
        val names =
            compositionSetNames()
                .toMutableList()

        if (safeName !in names) {
            names.add(
                safeName,
            )
        }

        prefs.edit()
            .putString(
                compositionSetKey(
                    safeName,
                ),
                buildLayerConfigurationJson(),
            )
            .apply()

        persistCompositionSetNames(
            names,
        )

        toast(
            "Сет «$safeName» збережено",
        )
    }

    private fun compositionSetKey(
        name: String,
    ): String =
        "set:" +
            name

    private fun deleteCompositionSet(
        name: String,
    ) {
        val names =
            compositionSetNames()
                .filterNot {
                    it == name
                }

        compositionSetsPrefs()
            .edit()
            .remove(
                compositionSetKey(
                    name,
                ),
            )
            .apply()

        persistCompositionSetNames(
            names,
        )

        toast(
            "Сет «$name» видалено",
        )
    }

    private fun renameCompositionSet(
        oldName: String,
        newName: String,
    ) {
        val safe =
            newName.trim()

        if (
            safe.isBlank() ||
            safe == oldName
        ) {
            return
        }

        val prefs =
            compositionSetsPrefs()
        val raw =
            prefs.getString(
                compositionSetKey(
                    oldName,
                ),
                null,
            )
                ?: return

        val names =
            compositionSetNames()
                .map { current ->
                    if (
                        current ==
                            oldName
                    ) {
                        safe
                    } else {
                        current
                    }
                }
                .distinct()

        prefs.edit()
            .remove(
                compositionSetKey(
                    oldName,
                ),
            )
            .putString(
                compositionSetKey(
                    safe,
                ),
                raw,
            )
            .apply()

        persistCompositionSetNames(
            names,
        )

        toast(
            "Сет перейменовано",
        )
    }

    private fun promptSaveCompositionSet() {
        PulseDeckDialogs.showTextInput(
            context = this,
            title = "Зберегти поточний сет",
            hint = "Наприклад: Panther Neon",
            positiveLabel = "Зберегти",
            negativeLabel = "Скасувати",
        ) { value ->
            saveCurrentCompositionSet(
                value,
            )
        }
    }

    private fun promptRenameCompositionSet(
        oldName: String,
    ) {
        PulseDeckDialogs.showTextInput(
            context = this,
            title = "Перейменувати сет",
            initialValue = oldName,
            positiveLabel = "Зберегти",
            negativeLabel = "Скасувати",
        ) { value ->
            renameCompositionSet(
                oldName,
                value,
            )
        }
    }

    private fun showCompositionSetActions(
        name: String,
    ) {
        PulseDeckDialogs.showActionList(
            context = this,
            title = name,
            items =
                listOf(
                    "Завантажити",
                    "Перезаписати поточним",
                    "Перейменувати",
                    "Видалити",
                ),
            cancelLabel = "Закрити",
        ) { which ->
            when (which) {
                0 ->
                    loadCompositionSet(
                        name,
                    )

                1 ->
                    saveCurrentCompositionSet(
                        name,
                    )

                2 ->
                    promptRenameCompositionSet(
                        name,
                    )

                3 ->
                    PulseDeckDialogs.showConfirm(
                        context = this,
                        title = "Видалити «$name»?",
                        positiveLabel = "Видалити",
                        negativeLabel = "Скасувати",
                        destructive = true,
                    ) {
                        deleteCompositionSet(
                            name,
                        )
                    }
            }
        }
    }

    private fun showCompositionSetsDialog() {
        val names =
            compositionSetNames()
        val items =
            buildList {
                add(
                    "＋ Зберегти поточний як сет",
                )
                addAll(
                    names,
                )
            }

        PulseDeckDialogs.showActionList(
            context = this,
            title = "Composition Sets",
            items = items,
            cancelLabel = "Закрити",
            accentFirst = true,
        ) { which ->
            if (which == 0) {
                promptSaveCompositionSet()
            } else {
                showCompositionSetActions(
                    names[
                        which -
                            1
                    ],
                )
            }
        }
    }

    private fun loadCompositionSet(
        name: String,
    ) {
        val raw =
            compositionSetsPrefs()
                .getString(
                    compositionSetKey(
                        name,
                    ),
                    null,
                )

        if (raw == null) {
            toast(
                "Сет не знайдено",
            )
            return
        }

        runCatching {
            applyLayerConfigurationJson(
                raw,
            )
        }.onSuccess {
            toast(
                "Сет «$name» завантажено",
            )
        }.onFailure { error ->
            toast(
                "Сет: " +
                    (
                        error.message
                            ?: error.javaClass
                                .simpleName
                        ),
            )
        }
    }

    private fun applyLayerConfigurationJson(
        raw: String,
    ) {
        val root =
            JSONObject(
                raw,
            )
        val schema =
            root.optString(
                "schema",
                "",
            )

        require(
            schema.startsWith(
                "faric-layer-config-v",
            ),
        ) {
            "Невідомий формат сету"
        }

        val gfJson =
            root.optJSONObject(
                "gf",
            )
        val gfTheme =
            gfJson
                ?.optString(
                    "theme",
                    "",
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let { name ->
                    runCatching {
                        PlaybackThemeId
                            .valueOf(
                                name,
                            )
                    }.getOrNull()
                }
                ?.let(
                    GraphicFigureCatalog::normalize,
                )
                ?: currentGraphicFigureThemeId()

        setGraphicFigureThemeId(
            gfTheme,
        )

        val layerEditor =
            layerPrefs()
                .edit()

        root.optJSONObject(
            "layers",
        )
            ?.let { layers ->
                controllablePulseDeckLayers()
                    .forEach { layer ->
                        if (
                            layers.has(
                                layer.name,
                            )
                        ) {
                            layerEditor.putBoolean(
                                layerPreferenceKey(
                                    layer,
                                ),
                                layers.optBoolean(
                                    layer.name,
                                    true,
                                ),
                            )
                        }
                    }
            }

        root.optJSONObject(
            "objects",
        )
            ?.let { objects ->
                PulseDeckLayerStack
                    .Layer
                    .entries
                    .forEach { layer ->
                        objects.optJSONObject(
                            layer.name,
                        )
                            ?.let { layerObjects ->
                                val keys =
                                    layerObjects.keys()
                                while (
                                    keys.hasNext()
                                ) {
                                    val objectId =
                                        keys.next()
                                    layerEditor.putBoolean(
                                        layerObjectPreferenceKey(
                                            layer,
                                            objectId,
                                        ),
                                        layerObjects.optBoolean(
                                            objectId,
                                            true,
                                        ),
                                    )
                                }
                            }
                    }
            }

        layerEditor.apply()

        gfJson
            ?.optJSONObject(
                "group",
            )
            ?.let { group ->
                val current =
                    boardTransformStore.load(
                        gfTheme,
                    )
                boardTransformStore.save(
                    gfTheme,
                    BoardTransform(
                        xFraction =
                            group.optDouble(
                                "x",
                                current.xFraction
                                    .toDouble(),
                            )
                                .toFloat(),
                        yFraction =
                            group.optDouble(
                                "y",
                                current.yFraction
                                    .toDouble(),
                            )
                                .toFloat(),
                        sizeFraction =
                            group.optDouble(
                                "scale",
                                current.sizeFraction
                                    .toDouble(),
                            )
                                .toFloat(),
                        rotationDegrees =
                            group.optDouble(
                                "rotation",
                                current.rotationDegrees
                                    .toDouble(),
                            )
                                .toFloat(),
                        opacity =
                            group.optDouble(
                                "opacity",
                                current.opacity
                                    .toDouble(),
                            )
                                .toFloat(),
                    ),
                )
            }

        gfJson
            ?.optJSONObject(
                "reaction",
            )
            ?.let { reaction ->
                val current =
                    boardGroupReactionStore.load(
                        gfTheme,
                    )
                boardGroupReactionStore.save(
                    gfTheme,
                    BoardGroupReaction(
                        rotationSwayDegrees =
                            reaction.optDouble(
                                "rotationSwayDegrees",
                                current.rotationSwayDegrees
                                    .toDouble(),
                            )
                                .toFloat(),
                        stereoShiftFraction =
                            reaction.optDouble(
                                "stereoShiftFraction",
                                current.stereoShiftFraction
                                    .toDouble(),
                            )
                                .toFloat(),
                        bassFloatFraction =
                            reaction.optDouble(
                                "bassFloatFraction",
                                current.bassFloatFraction
                                    .toDouble(),
                            )
                                .toFloat(),
                    ),
                )
            }

        gfJson
            ?.optJSONObject(
                "layers",
            )
            ?.let { layerTransforms ->
                BoardLayerId.entries
                    .forEach { layerId ->
                        layerTransforms
                            .optJSONObject(
                                layerId.name,
                            )
                            ?.let { item ->
                                val current =
                                    boardLayerTransformStore
                                        .load(
                                            gfTheme,
                                            layerId,
                                        )
                                boardLayerTransformStore
                                    .save(
                                        gfTheme,
                                        layerId,
                                        BoardLayerTransform(
                                            offsetXFraction =
                                                item.optDouble(
                                                    "x",
                                                    current.offsetXFraction
                                                        .toDouble(),
                                                )
                                                    .toFloat(),
                                            offsetYFraction =
                                                item.optDouble(
                                                    "y",
                                                    current.offsetYFraction
                                                        .toDouble(),
                                                )
                                                    .toFloat(),
                                            scale =
                                                item.optDouble(
                                                    "scale",
                                                    current.scale
                                                        .toDouble(),
                                                )
                                                    .toFloat(),
                                            rotationDegrees =
                                                item.optDouble(
                                                    "rotation",
                                                    current.rotationDegrees
                                                        .toDouble(),
                                                )
                                                    .toFloat(),
                                            opacity =
                                                item.optDouble(
                                                    "opacity",
                                                    current.opacity
                                                        .toDouble(),
                                                )
                                                    .toFloat(),
                                        ),
                                    )
                            }
                    }
            }

        root.optJSONObject(
            "projectM",
        )
            ?.let { projectM ->
                val state =
                    ProjectMStateStore(
                        this,
                    )
                val path =
                    projectM.optString(
                        "presetPath",
                        "",
                    )

                if (
                    path.isNotBlank() &&
                    java.io.File(
                        path,
                    ).isFile
                ) {
                    state.lastPresetPath =
                        path
                }

                projectM
                    .optString(
                        "foreground",
                        "",
                    )
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let { value ->
                        runCatching {
                            com.saney.musicvisualizer
                                .projectm
                                .FaricForegroundSample
                                .valueOf(
                                    value,
                                )
                        }.getOrNull()
                    }
                    ?.let { sample ->
                        state.foregroundSample =
                            sample
                    }
            }

        projectMExportSnapshot =
            null
        showNowPlaying()
    }

    private fun showPulseDeckLayersDialog() {
        val metrics =
            resources.displayMetrics
        val screenWidth =
            metrics.widthPixels
        val screenHeight =
            metrics.heightPixels

        // Keep the panel readable, but make it occupy only ~30% of the
        // physical screen height so the composition remains visible while
        // layers/objects are toggled.
        val panelWidth =
            (screenWidth * 0.88f)
                .toInt()
                .coerceAtLeast(
                    dp(280),
                )
                .coerceAtMost(
                    screenWidth,
                )
        val panelHeight =
            (screenHeight * 0.30f)
                .toInt()
                .coerceAtLeast(
                    dp(220),
                )
                .coerceAtMost(
                    screenHeight,
                )

        val dialog =
            android.app.Dialog(
                this,
            ).apply {
                requestWindowFeature(
                    android.view.Window
                        .FEATURE_NO_TITLE,
                )
                setCanceledOnTouchOutside(
                    false,
                )
            }

        val panel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                background =
                    panelDrawable(
                        Color.argb(
                            250,
                            22,
                            25,
                            29,
                        ),
                        24,
                        Color.argb(
                            95,
                            255,
                            255,
                            255,
                        ),
                        1,
                    )
                setPadding(
                    dp(10),
                    dp(6),
                    dp(10),
                    dp(8),
                )
            }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(8),
                    dp(2),
                    dp(2),
                    dp(2),
                )
            }

        val title =
            TextView(this).apply {
                text =
                    "☰  Шари та об'єкти · тягни панель"
                textSize = 15f
                setTextColor(
                    Color.WHITE,
                )
                setTypeface(
                    typeface,
                    Typeface.BOLD,
                )
            }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        val exportConfig =
            TextView(this).apply {
                text = "⇩"
                textSize = 21f
                gravity = Gravity.CENTER
                contentDescription =
                    "Експортувати конфігурацію шарів"
                setPadding(
                    dp(9),
                    dp(4),
                    dp(9),
                    dp(4),
                )
                setTextColor(
                    Color.WHITE,
                )
                background =
                    panelDrawable(
                        Color.argb(
                            120,
                            16,
                            22,
                            30,
                        ),
                        18,
                        Color.argb(
                            100,
                            35,
                            211,
                            238,
                        ),
                        1,
                    )
                setOnClickListener {
                    exportLayerConfiguration.launch(
                        "FARIC-layers-v" +
                            BuildConfig.VERSION_NAME +
                            ".json",
                    )
                }
            }

        val resetPosition =
            TextView(this).apply {
                text = "◎"
                textSize = 22f
                gravity = Gravity.CENTER
                contentDescription =
                    "Повернути панель у центр"
                setPadding(
                    dp(10),
                    dp(4),
                    dp(10),
                    dp(4),
                )
                setTextColor(
                    Color.WHITE,
                )
                background =
                    panelDrawable(
                        Color.argb(
                            120,
                            16,
                            22,
                            30,
                        ),
                        18,
                        Color.argb(
                            100,
                            35,
                            211,
                            238,
                        ),
                        1,
                    )
            }

        val close =
            TextView(this).apply {
                text = "✕"
                textSize = 20f
                gravity = Gravity.CENTER
                contentDescription =
                    "Закрити панель"
                setPadding(
                    dp(10),
                    dp(4),
                    dp(8),
                    dp(4),
                )
                setTextColor(
                    Color.WHITE,
                )
                background =
                    panelDrawable(
                        Color.argb(
                            120,
                            16,
                            22,
                            30,
                        ),
                        18,
                        Color.argb(
                            100,
                            35,
                            211,
                            238,
                        ),
                        1,
                    )
                setOnClickListener {
                    dialog.dismiss()
                }
            }

        header.addView(
            exportConfig,
        )
        header.addView(
            resetPosition,
        )
        header.addView(
            close,
        )
        panel.addView(
            header,
        )

        val container =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(4),
                    0,
                    dp(4),
                    dp(10),
                )
            }

        val scroll =
            ScrollView(this).apply {
                isFillViewport = false
                isVerticalScrollBarEnabled = true
                addView(
                    container,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }

        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        fun addSectionLabel(
            text: String,
            leftPaddingDp: Int = 0,
        ) {
            container.addView(
                TextView(this).apply {
                    this.text = text
                    textSize = 12f
                    setTextColor(
                        COLOR_MUTED,
                    )
                    setPadding(
                        dp(leftPaddingDp),
                        dp(5),
                        0,
                        dp(1),
                    )
                },
            )
        }

        val objectControlsByLayer =
            mutableMapOf<
                PulseDeckLayerStack.Layer,
                MutableList<View>
                >()

        fun setObjectControlsEnabled(
            layer: PulseDeckLayerStack.Layer,
            enabled: Boolean,
        ) {
            objectControlsByLayer[layer]
                ?.forEach { control ->
                    control.isEnabled = enabled
                    control.alpha =
                        if (enabled) {
                            1f
                        } else {
                            0.38f
                        }
                }
        }

        fun styleLayerCheckBox(
            box: android.widget.CheckBox,
        ) {
            box.setTextColor(
                Color.WHITE,
            )
            box.buttonTintList =
                android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(
                            android.R.attr.state_checked,
                        ),
                        intArrayOf(),
                    ),
                    intArrayOf(
                        COLOR_ACCENT_CYAN,
                        Color.rgb(
                            115,
                            126,
                            136,
                        ),
                    ),
                )
        }

        fun addObjectGroup(
            layer: PulseDeckLayerStack.Layer,
            groupLabel: String,
            objects: List<LayerMenuObject>,
        ) {
            var updatingGroup = false
            val childBoxes =
                mutableListOf<android.widget.CheckBox>()

            val groupBox =
                android.widget.CheckBox(this).apply {
                    text =
                        "↳ $groupLabel · ${objects.size}"
                    textSize = 13f
                    setPadding(
                        dp(12),
                        0,
                        0,
                        0,
                    )
                }

            styleLayerCheckBox(
                groupBox,
            )

            container.addView(
                groupBox,
            )
            objectControlsByLayer
                .getOrPut(
                    layer,
                ) {
                    mutableListOf()
                }
                .add(
                    groupBox,
                )

            fun refreshGroupState() {
                updatingGroup = true
                groupBox.isChecked =
                    childBoxes.isNotEmpty() &&
                        childBoxes.all { child ->
                            child.isChecked
                        }
                updatingGroup = false
            }

            objects.forEach { item ->
                val child =
                    android.widget.CheckBox(this).apply {
                        text = item.label
                        textSize = 12f
                        isChecked =
                            layerObjectVisible(
                                layer,
                                item.id,
                            )
                        setPadding(
                            dp(30),
                            0,
                            0,
                            0,
                        )
                    }

                styleLayerCheckBox(
                    child,
                )

                child.setOnCheckedChangeListener { _, checked ->
                    setLayerObjectVisible(
                        layer,
                        item.id,
                        checked,
                    )
                    if (!updatingGroup) {
                        refreshGroupState()
                    }
                }

                childBoxes += child
                container.addView(
                    child,
                )
                objectControlsByLayer
                    .getOrPut(
                        layer,
                    ) {
                        mutableListOf()
                    }
                    .add(
                        child,
                    )
            }

            refreshGroupState()

            groupBox.setOnCheckedChangeListener { _, checked ->
                if (updatingGroup) {
                    return@setOnCheckedChangeListener
                }

                updatingGroup = true
                childBoxes.forEach { child ->
                    if (
                        child.isChecked !=
                            checked
                    ) {
                        child.isChecked =
                            checked
                    }
                }
                updatingGroup = false

                objects.forEach { item ->
                    setLayerObjectVisible(
                        layer,
                        item.id,
                        checked,
                    )
                }
            }
        }

        PulseDeckLayerStack.Layer.entries
            .filter { layer ->
                layer !=
                    PulseDeckLayerStack.Layer.FARIC_REACTIVE
            }
            .sortedBy { layer ->
                layer.z
            }
            .forEach { layer ->
                val objects =
                    layerMenuObjects(
                        layer,
                    )

                val layerName =
                    when (layer) {
                        PulseDeckLayerStack.Layer.VISUALIZER ->
                            "Visualizer"

                        PulseDeckLayerStack.Layer.FARIC_REACTIVE ->
                            "Visualizer / FARIC Reactive (internal)"

                        PulseDeckLayerStack.Layer.OVER_VISUALIZATION ->
                            "Надвізуалізація · 447504"

                        PulseDeckLayerStack.Layer.BIG_EQUALIZER ->
                            "Big Equalizer"

                        PulseDeckLayerStack.Layer.GRAPHIC_FIGURES ->
                            "GF / Graphic Figures"

                        PulseDeckLayerStack.Layer.GIF_ANIMATION ->
                            "GIF / Animation"

                        PulseDeckLayerStack.Layer.EFFECTS ->
                            "Effects"

                        PulseDeckLayerStack.Layer.PULSEDECK_LOCKED ->
                            "PulseDeck 🔒"

                        PulseDeckLayerStack.Layer.SERVICE_OVERLAY ->
                            "Service Overlay"
                    }

                val suffix =
                    when {
                        objects.isNotEmpty() ->
                            " · ${objects.size}"

                        layer ==
                            PulseDeckLayerStack.Layer.GIF_ANIMATION ->
                            " · порожньо"

                        layer ==
                            PulseDeckLayerStack.Layer.SERVICE_OVERLAY ->
                            " · service"

                        else -> ""
                    }

                val layerBox =
                    android.widget.CheckBox(this).apply {
                        text =
                            "L${layer.z} · $layerName$suffix"
                        textSize = 14f
                        setPadding(
                            0,
                            dp(4),
                            0,
                            0,
                        )

                        when (layer) {
                            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED -> {
                                isChecked = true
                                isEnabled = false
                            }

                            PulseDeckLayerStack.Layer.SERVICE_OVERLAY -> {
                                isChecked =
                                    pulseDeckLayerStack
                                        ?.isLayerVisible(
                                            layer,
                                        )
                                        ?: true
                                isEnabled = false
                            }

                            else -> {
                                isChecked =
                                    layerVisible(
                                        layer,
                                    )
                            }
                        }
                    }

                styleLayerCheckBox(
                    layerBox,
                )

                if (
                    layer !=
                        PulseDeckLayerStack.Layer.PULSEDECK_LOCKED &&
                    layer !=
                        PulseDeckLayerStack.Layer.SERVICE_OVERLAY
                ) {
                    layerBox.setOnCheckedChangeListener { _, checked ->
                        setLayerVisible(
                            layer,
                            checked,
                        )
                        setObjectControlsEnabled(
                            layer,
                            checked,
                        )
                    }
                }

                if (
                    layer ==
                        PulseDeckLayerStack.Layer.GRAPHIC_FIGURES
                ) {
                    val layerRow =
                        LinearLayout(this).apply {
                            orientation =
                                LinearLayout.HORIZONTAL
                            gravity =
                                Gravity.CENTER_VERTICAL
                        }

                    layerRow.addView(
                        layerBox,
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f,
                        ),
                    )

                    layerRow.addView(
                        TextView(this).apply {
                            text =
                                GraphicFigureCatalog
                                    .title(
                                        currentGraphicFigureThemeId(),
                                    )
                            textSize = 11f
                            gravity =
                                Gravity.CENTER
                            contentDescription =
                                "Обрати Graphic Figure"
                            setTextColor(
                                COLOR_ACCENT_CYAN,
                            )
                            background =
                                panelDrawable(
                                    Color.argb(
                                        105,
                                        8,
                                        18,
                                        24,
                                    ),
                                    18,
                                    Color.argb(
                                        95,
                                        80,
                                        220,
                                        255,
                                    ),
                                    1,
                                )
                            setOnClickListener {
                                val ids =
                                    GraphicFigureCatalog
                                        .ids
                                val labels =
                                    ids
                                        .map { id ->
                                            GraphicFigureCatalog
                                                .title(
                                                    id,
                                                )
                                        }
                                        .toTypedArray()
                                val checked =
                                    ids.indexOf(
                                        currentGraphicFigureThemeId(),
                                    )

                                PulseDeckDialogs.showSingleChoice(
                                    context =
                                        this@MainActivity,
                                    title =
                                        "Graphic Figure",
                                    labels =
                                        labels.toList(),
                                    checkedIndex =
                                        checked,
                                    cancelLabel =
                                        "Скасувати",
                                ) { which ->
                                    setGraphicFigureThemeId(
                                        ids[which],
                                    )
                                    dialog.dismiss()
                                    showNowPlaying()
                                }
                            }
                        },
                        LinearLayout.LayoutParams(
                            dp(112),
                            dp(42),
                        ).apply {
                            marginStart =
                                dp(6)
                        },
                    )

                    layerRow.addView(
                        TextView(this).apply {
                            text = "⚙"
                            textSize = 20f
                            gravity = Gravity.CENTER
                            contentDescription =
                                "Налаштувати GF Background / Glow"
                            setTextColor(
                                COLOR_ACCENT_CYAN,
                            )
                            background =
                                panelDrawable(
                                    Color.argb(
                                        105,
                                        8,
                                        18,
                                        24,
                                    ),
                                    18,
                                    Color.argb(
                                        95,
                                        80,
                                        220,
                                        255,
                                    ),
                                    1,
                                )
                            setOnClickListener {
                                dialog.dismiss()
                                boardEditorLayer =
                                    BoardLayerId.BACKGROUND
                                showBoardTransform()
                            }
                        },
                        LinearLayout.LayoutParams(
                            dp(42),
                            dp(42),
                        ).apply {
                            marginStart = dp(6)
                        },
                    )

                    container.addView(
                        layerRow,
                    )
                } else {
                    container.addView(
                        layerBox,
                    )
                }

                if (objects.isNotEmpty()) {
                    if (
                        layer ==
                            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED
                    ) {
                        objects
                            .groupBy { item ->
                                item.groupCode
                                    ?: "6.x"
                            }
                            .toSortedMap()
                            .forEach { (_, groupObjects) ->
                                addObjectGroup(
                                    layer = layer,
                                    groupLabel =
                                        groupObjects
                                            .firstOrNull()
                                            ?.groupLabel
                                            ?: "PulseDeck",
                                    objects =
                                        groupObjects,
                                )
                            }
                    } else {
                        addObjectGroup(
                            layer = layer,
                            groupLabel =
                                when (layer) {
                                    PulseDeckLayerStack.Layer.VISUALIZER ->
                                        "Visualizer objects"

                                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES ->
                                        "GF objects"

                                    else ->
                                        "Objects"
                                },
                            objects = objects,
                        )

                    }
                } else if (
                    layer ==
                        PulseDeckLayerStack.Layer.GIF_ANIMATION
                ) {
                    addSectionLabel(
                        "↳ Немає активних GIF",
                        leftPaddingDp = 14,
                    )
                }

                if (objects.isNotEmpty()) {
                    setObjectControlsEnabled(
                        layer,
                        layerBox.isChecked,
                    )
                }
            }

        dialog.setContentView(
            panel,
        )
        dialog.show()

        val window =
            dialog.window
                ?: return

        window.setBackgroundDrawable(
            android.graphics.drawable
                .ColorDrawable(
                    Color.TRANSPARENT,
                ),
        )
        window.clearFlags(
            android.view.WindowManager
                .LayoutParams
                .FLAG_DIM_BEHIND,
        )
        window.addFlags(
            android.view.WindowManager
                .LayoutParams
                .FLAG_NOT_TOUCH_MODAL,
        )
        window.setGravity(
            Gravity.TOP or
                Gravity.START,
        )
        window.setLayout(
            panelWidth,
            panelHeight,
        )

        val positionPrefs =
            layerPrefs()
        val maxX =
            (
                screenWidth -
                    panelWidth
                )
                .coerceAtLeast(
                    0,
                )
        val maxY =
            (
                screenHeight -
                    panelHeight
                )
                .coerceAtLeast(
                    0,
                )

        fun setPanelPosition(
            x: Int,
            y: Int,
            persist: Boolean,
        ) {
            val safeX =
                x.coerceIn(
                    0,
                    maxX,
                )
            val safeY =
                y.coerceIn(
                    0,
                    maxY,
                )

            val attrs =
                window.attributes
            attrs.x = safeX
            attrs.y = safeY
            window.attributes = attrs

            if (persist) {
                positionPrefs
                    .edit()
                    .putInt(
                        KEY_LAYER_PANEL_X,
                        safeX,
                    )
                    .putInt(
                        KEY_LAYER_PANEL_Y,
                        safeY,
                    )
                    .apply()
            }
        }

        val defaultX =
            (
                (screenWidth -
                    panelWidth) /
                    2
                )
        val defaultY =
            (
                (screenHeight -
                    panelHeight) /
                    2
                )

        setPanelPosition(
            x =
                positionPrefs.getInt(
                    KEY_LAYER_PANEL_X,
                    defaultX,
                ),
            y =
                positionPrefs.getInt(
                    KEY_LAYER_PANEL_Y,
                    defaultY,
                ),
            persist = false,
        )

        resetPosition.setOnClickListener {
            setPanelPosition(
                x = defaultX,
                y = defaultY,
                persist = true,
            )
        }

        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var dragStartWindowX = 0
        var dragStartWindowY = 0

        header.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX =
                        event.rawX
                    dragStartRawY =
                        event.rawY
                    val attrs =
                        window.attributes
                    dragStartWindowX =
                        attrs.x
                    dragStartWindowY =
                        attrs.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val nextX =
                        dragStartWindowX +
                            (
                                event.rawX -
                                    dragStartRawX
                                )
                                .toInt()
                    val nextY =
                        dragStartWindowY +
                            (
                                event.rawY -
                                    dragStartRawY
                                )
                                .toInt()

                    setPanelPosition(
                        x = nextX,
                        y = nextY,
                        persist = false,
                    )
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    val attrs =
                        window.attributes
                    setPanelPosition(
                        x = attrs.x,
                        y = attrs.y,
                        persist = true,
                    )
                    true
                }

                else -> false
            }
        }
    }

    private fun showBoardTransform() {
        val gfThemeId =
            currentGraphicFigureThemeId()

        screen = Screen.BOARD_TRANSFORM
        sceneOrchestrator.stop()
        clearScreenRefs()

        var transform =
            boardTransformStore.load(gfThemeId)

        var groupReaction =
            boardGroupReactionStore.load(
                gfThemeId,
            )

        val selectedLayer =
            boardEditorLayer
                ?.takeIf { layerId ->
                    GraphicFigureCatalog
                        .supports(
                            gfThemeId,
                            layerId,
                        )
                }

        var selectedLayerTransform =
            selectedLayer?.let { layerId ->
                boardLayerTransformStore.load(
                    gfThemeId,
                    layerId,
                )
            }

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(COLOR_BG)
            }

        // Keep the preview on the same full physical viewport as Now Playing.
        // Applying safe-area padding to the root would change GF↔Visualizer
        // registration while the user is editing it.
        // Board Transform is a composition editor, not an isolated GF screen.
        // Keep the real lower layers visible so GF position/scale is adjusted
        // against the same visualizer/background the user sees in Now Playing.
        val previewStack =
            PulseDeckLayerStack(this)

        root.addView(
            previewStack,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        attachLayer0Visualizer(
            previewStack,
        )

        previewStack.setContent(
            PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
            OverVisualizationView(this),
        )

        val previewEqualizer =
            BigEqualizerView(this).also { view ->
                view.updateSignal(
                    latestSignal,
                )
                view.setPlaying(
                    latestSnapshot.isPlaying,
                )
            }
        bigEqualizerView =
            previewEqualizer
        previewStack.setContent(
            PulseDeckLayerStack.Layer.BIG_EQUALIZER,
            previewEqualizer,
        )

        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.VISUALIZER,
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            ),
        )
        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.FARIC_REACTIVE,
            layerVisible(
                PulseDeckLayerStack.Layer.FARIC_REACTIVE,
            ),
        )
        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
            layerVisible(
                PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
            ),
        )
        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.BIG_EQUALIZER,
            layerVisible(
                PulseDeckLayerStack.Layer.BIG_EQUALIZER,
            ),
        )

        val boardView =
            HeroBoardView(
                this,
                gfThemeId,
            ).also { view ->
                view.setGroupTransform(
                    transform,
                )
                view.setGroupReaction(
                    groupReaction,
                )
                view.setLayerTransforms(
                    boardLayerTransformStore.loadAll(
                        gfThemeId,
                    ),
                )
                HeroBoardView.ObjectId.entries
                    .forEach { objectId ->
                        view.setObjectVisible(
                            objectId,
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                objectId.name.lowercase(),
                            ),
                        )
                    }
                view.setPlaying(
                    latestSnapshot.isPlaying,
                )
                view.updateSignal(
                    latestSignal,
                )
            }

        heroBoardView =
            boardView

        previewStack.setContent(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            boardView,
        )

        // Force only the edited GF container visible in the editor. Its child
        // visibility still mirrors the live composition.
        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            true,
        )

        fun layerTitle(
            layerId: BoardLayerId?,
        ): String =
            when (layerId) {
                null -> "Усе GF"
                BoardLayerId.BACKGROUND -> "Background / Glow"
                BoardLayerId.FRAME -> "Frame"
                BoardLayerId.FX -> "FX"
                BoardLayerId.CREATURE ->
                    GraphicFigureCatalog
                        .creatureLabel(
                            gfThemeId,
                        )
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
            (
                listOf(
                    null to "Усе",
                ) +
                    listOf(
                        BoardLayerId.BACKGROUND to
                            "BG/Glow",
                        BoardLayerId.FRAME to
                            "Frame",
                        BoardLayerId.CREATURE to
                            GraphicFigureCatalog
                                .creatureLabel(
                                    gfThemeId,
                                ),
                        BoardLayerId.WORDMARK to
                            "FARIC",
                        BoardLayerId.FX to
                            "FX",
                    )
                        .filter { (layerId, _) ->
                            GraphicFigureCatalog
                                .supports(
                                    gfThemeId,
                                    layerId,
                                )
                        }
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
                gfThemeId,
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
                gfThemeId,
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
                gfThemeId,
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
                            gfThemeId,
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
                        gfThemeId,
                    )
                    boardGroupReactionStore.reset(
                        gfThemeId,
                    )
                    boardLayerTransformStore
                        .resetAll(
                            gfThemeId,
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

            val layerQuickRow =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        Gravity.CENTER
                }

            layerQuickRow.addView(
                actionPill(
                    text = "Центр",
                    accent = false,
                ) {
                    layerValue =
                        layerValue.copy(
                            offsetXFraction = 0f,
                            offsetYFraction = 0f,
                        )
                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    marginEnd = dp(4)
                },
            )

            layerQuickRow.addView(
                actionPill(
                    text = "100%",
                    accent = false,
                ) {
                    layerValue =
                        layerValue.copy(
                            scale = 1f,
                        )
                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                },
            )

            layerQuickRow.addView(
                actionPill(
                    text = "Reset",
                    accent = false,
                ) {
                    boardLayerTransformStore.reset(
                        gfThemeId,
                        layerId,
                    )
                    showBoardTransform()
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1f,
                ).apply {
                    marginStart = dp(4)
                },
            )

            panelContent.addView(
                layerQuickRow,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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
        updateProjectMRenderState()
        updateSceneOrchestratorState()
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun openExportLabWithProjectMCapture() {
        val projectMVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            ) &&
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                )

        val liveView =
            projectMMainView

        if (
            !projectMVisible ||
            liveView == null
        ) {
            projectMExportSnapshot =
                null
            showExportLab()
            return
        }

        toast(
            "Знімаю projectM кадр…",
        )

        liveView.captureFrame { bitmap ->
            projectMExportSnapshot =
                bitmap

            if (
                projectMVisible &&
                bitmap == null
            ) {
                toast(
                    "projectM capture не вдався — експорт без нього",
                )
            }

            showExportLab()
        }
    }

    private fun exportProofThemeId():
        PlaybackThemeId =
        if (
            layerVisible(
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            )
        ) {
            currentGraphicFigureThemeId()
        } else {
            selectedThemeId
        }

    private fun attachExportProjectMPreview(
        root: FrameLayout,
    ) {
        val projectMVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            ) &&
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                )

        if (!projectMVisible) {
            return
        }

        val state =
            ProjectMStateStore(
                this,
            )
        val preset =
            state.lastPresetFileOrNull()
                ?: return
        val textureDirectory =
            ProjectMLibraryManager
                .textureDir(
                    this,
                )

        if (!textureDirectory.isDirectory) {
            return
        }

        ProjectMBridge
            .beginOfflineExport()

        val view =
            ProjectMView(
                context = this,
                initialPreset = preset,
                textureDirectory =
                    textureDirectory,
                profile =
                    ProjectMPerformanceProfile
                        .BALANCED_BACKGROUND,
                foregroundSample =
                    state.foregroundSample,
                onTapNext = {},
                manualFrameMode = true,
                manualRenderWidth =
                    exportAspectRatio.width,
                manualRenderHeight =
                    exportAspectRatio.height,
            )

        projectMExportLiveView =
            view

        root.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        root.addView(
            View(this).apply {
                setBackgroundColor(
                    Color.argb(
                        218,
                        0,
                        0,
                        0,
                    ),
                )
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun showExportLab() {
        screen = Screen.EXPORT_LAB
        sceneOrchestrator.stop()
        clearScreenRefs()

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }

        attachExportProjectMPreview(
            root,
        )
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

        val compositionConfig =
            currentCompositionExportConfig()

        val activeLayers =
            buildList {
                if (
                    compositionConfig
                        .faricReactiveVisible
                ) {
                    add("L1 FARIC")
                }
                if (
                    compositionConfig
                        .projectMVisible
                ) {
                    add(
                        when {
                            projectMExportLiveView !=
                                null ->
                                "L0 projectM offline"

                            compositionConfig
                                .projectMFrame !=
                                null ->
                                "L0 projectM snapshot"

                            else ->
                                "L0 projectM*"
                        },
                    )
                }
                if (
                    compositionConfig
                        .overVisualizationVisible
                ) {
                    add("L1")
                }
                if (
                    compositionConfig
                        .bigEqualizerVisible
                ) {
                    add("L2")
                }
                if (
                    compositionConfig
                        .graphicFiguresVisible
                ) {
                    add("L3 GF")
                }
                if (
                    compositionConfig
                        .effectsVisible
                ) {
                    add("L5 FX")
                }
                if (
                    compositionConfig
                        .pulseDeckVisible
                ) {
                    add("L6 HUD")
                }
            }
                .joinToString(
                    " + ",
                )

        content.addView(
            label(
                "Композиція: " +
                    (
                        activeLayers
                            .takeIf {
                                it.isNotBlank()
                            }
                            ?: "порожньо"
                        ) +
                    "\nТрек: ${latestSnapshot.trackName ?: "—"}",
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
                "Composition export рендерить активні шари в їхньому Z-порядку: projectM → FARIC Reactive → Over → Big EQ → GF → GIF → Effects → HUD. PNG і MP4 використовують один deterministic renderer.",
                13f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
                bottomMargin =
                    if (
                        compositionConfig
                            .projectMVisible
                    ) {
                        dp(8)
                    } else {
                        dp(18)
                    }
            },
        )

        if (
            compositionConfig
                .projectMVisible
        ) {
            val hasLiveCapture =
                projectMExportLiveView !=
                    null
            val hasSnapshot =
                compositionConfig
                    .projectMFrame !=
                null

            content.addView(
                label(
                    when {
                        hasLiveCapture ->
                            "✓ projectM offline clock готовий. MP4 може рендерити кадри з власного timeline без очікування реального часу та без залежності від головного плеєра."

                        hasSnapshot ->
                            "✓ projectM GL snapshot є. PNG включить його точно; MP4 використає snapshot як fallback, якщо live capture недоступний."

                        else ->
                            "⚠ projectM увімкнений, але GL capture недоступний. Повернись у Now Playing і відкрий Export ще раз."
                    },
                    12f,
                    if (
                        hasLiveCapture ||
                        hasSnapshot
                    ) {
                        COLOR_ACCENT_CYAN
                    } else {
                        COLOR_ACCENT_ORANGE
                    },
                    true,
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(18)
                },
            )
        }

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
            true

        content.addView(
            actionPill(
                text =
                    if (exportReady) {
                        "Зберегти кадр композиції PNG"
                    } else {
                        "Ця тема ще не готова до export proof"
                    },
                accent = exportReady,
            ) {
                if (exportReady) {
                    exportProofFrame()
                } else {
                    toast("Ця сцена ще не має export renderer")
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
                            "Тест · експортувати 3 с MP4"
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
            actionPill(
                text =
                    when {
                        !analysisReady ->
                            "Спочатку виконай offline analysis"

                        else ->
                            "Експортувати всю пісню MP4"
                    },
                accent =
                    analysisReady &&
                        !offlineAnalysisRunning,
            ) {
                if (
                    analysisReady &&
                    !offlineAnalysisRunning
                ) {
                    exportFullSongVideo()
                } else {
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
            actionPill(
                text = "Відкрити папку експорту",
                accent = false,
            ) {
                showExportFolderChooser()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54),
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            label(
                "Тестовий MP4 рендерить 3 секунди. «Експортувати всю пісню» проходить offline timeline від 0:00 до кінця, незалежно від позиції та стану головного плеєра.",
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

        projectMExportLiveView
            ?.onResume()
        projectMExportLiveResumed =
            projectMExportLiveView !=
            null

        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun showExportFolderChooser() {
        val dialog =
            android.app.Dialog(
                this,
            ).apply {
                requestWindowFeature(
                    android.view.Window
                        .FEATURE_NO_TITLE,
                )
                setCanceledOnTouchOutside(
                    true,
                )
            }

        val panel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(14),
                )
                background =
                    panelDrawable(
                        Color.argb(
                            248,
                            24,
                            27,
                            31,
                        ),
                        26,
                        Color.argb(
                            95,
                            255,
                            255,
                            255,
                        ),
                        1,
                    )
            }

        val titleRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
            }

        titleRow.addView(
            label(
                "Папка експорту",
                21f,
                Color.WHITE,
                true,
            ),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        titleRow.addView(
            TextView(this).apply {
                text = "✕"
                textSize = 22f
                gravity = Gravity.CENTER
                contentDescription =
                    "Закрити"
                setTextColor(
                    Color.WHITE,
                )
                setOnClickListener {
                    dialog.dismiss()
                }
            },
            LinearLayout.LayoutParams(
                dp(44),
                dp(44),
            ),
        )

        panel.addView(
            titleRow,
        )

        panel.addView(
            label(
                "Куди перейти?",
                12f,
                COLOR_MUTED,
                false,
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            },
        )

        panel.addView(
            actionPill(
                text =
                    "▣  Кадри PNG\nPictures/FARIC",
                accent = false,
            ) {
                dialog.dismiss()
                openExportFolder(
                    "Pictures/FARIC",
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(66),
            ).apply {
                bottomMargin = dp(8)
            },
        )

        panel.addView(
            actionPill(
                text =
                    "▶  Відео зі звуком\nMovies/FARIC",
                accent = false,
            ) {
                dialog.dismiss()
                openExportFolder(
                    "Movies/FARIC",
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(66),
            ),
        )

        panel.addView(
            actionPill(
                text = "Скасувати",
                accent = false,
            ) {
                dialog.dismiss()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44),
            ).apply {
                topMargin = dp(10)
            },
        )

        dialog.setContentView(
            panel,
        )
        dialog.show()

        dialog.window
            ?.apply {
                setBackgroundDrawable(
                    android.graphics.drawable
                        .ColorDrawable(
                            Color.TRANSPARENT,
                        ),
                )
                clearFlags(
                    android.view.WindowManager
                        .LayoutParams
                        .FLAG_DIM_BEHIND,
                )
                setLayout(
                    (
                        resources
                            .displayMetrics
                            .widthPixels *
                            0.86f
                        )
                        .toInt(),
                    ViewGroup.LayoutParams
                        .WRAP_CONTENT,
                )
                setGravity(
                    Gravity.CENTER,
                )
            }
    }

    private fun openExportFolder(
        relativePath: String,
    ) {
        val documentUri =
            DocumentsContract
                .buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:$relativePath",
                )

        val viewIntent =
            Intent(
                Intent.ACTION_VIEW,
            ).apply {
                setDataAndType(
                    documentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

        val opened =
            runCatching {
                startActivity(
                    viewIntent,
                )
            }.isSuccess

        if (opened) {
            return
        }

        val picker =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE,
            ).apply {
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    documentUri,
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

        runCatching {
            startActivity(
                picker,
            )
        }.onFailure {
            toast(
                "Не вдалося відкрити $relativePath",
            )
        }
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

    private fun currentCyberSharkExportConfig():
        CyberSharkExportConfig {
        val gfThemeId =
            currentGraphicFigureThemeId()
        val parentVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            )

        return CyberSharkExportConfig(
            figureThemeId =
                gfThemeId,
            groupTransform =
                boardTransformStore.load(
                    gfThemeId,
                ),
            groupReaction =
                boardGroupReactionStore.load(
                    gfThemeId,
                ),
            layerTransforms =
                boardLayerTransformStore.loadAll(
                    gfThemeId,
                ),
            visibility =
                BoardLayerId.entries
                    .associateWith { layerId ->
                        parentVisible &&
                            GraphicFigureCatalog
                                .supports(
                                    gfThemeId,
                                    layerId,
                                ) &&
                            layerObjectVisible(
                                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                layerId.name.lowercase(),
                            )
                    },
        )
    }


    private fun currentCompositionExportConfig():
        CompositionExportConfig {
        val visualizerVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            )
        val projectMVisible =
            visualizerVisible &&
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                ) &&
                ProjectMStateStore(this)
                    .lastPresetFileOrNull() !=
                null

        return CompositionExportConfig(
            scene =
                currentScene,
            faricReactiveVisible =
                visualizerVisible &&
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "faric_reactive",
                    ),
            projectMVisible =
                projectMVisible,
            projectMFrame =
                if (projectMVisible) {
                    projectMExportSnapshot
                } else {
                    null
                },
            overVisualizationVisible =
                layerVisible(
                    PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
                ),
            bigEqualizerVisible =
                layerVisible(
                    PulseDeckLayerStack.Layer.BIG_EQUALIZER,
                ),
            graphicFiguresVisible =
                layerVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                ),
            cyberSharkConfig =
                currentCyberSharkExportConfig(),
            effectsVisible =
                layerVisible(
                    PulseDeckLayerStack.Layer.EFFECTS,
                ),
            pulseDeckVisible =
                true,
            pulseDeckObjectVisibility =
                PULSEDECK_EXPORT_OBJECT_IDS
                    .associateWith { objectId ->
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                            objectId,
                        )
                    },
        )
    }

    private fun ensureOfflinePcmCache(
        sourceUri: Uri,
        cancelled: AtomicBoolean,
        onProgress: (Int) -> Unit,
    ): OfflinePcmCacheResult {
        val key =
            sourceUri.toString()

        offlinePcmCache
            ?.takeIf {
                offlinePcmCacheUri ==
                    key &&
                    it.file.isFile
            }
            ?.let {
                return it
            }

        offlinePcmCache
            ?.file
            ?.delete()

        offlinePcmCache =
            null
        offlinePcmCacheUri =
            null

        val result =
            OfflinePcmCache.build(
                context = this,
                uri = sourceUri,
                shouldCancel = {
                    cancelled.get()
                },
                onProgress =
                    onProgress,
            )

        offlinePcmCache =
            result
        offlinePcmCacheUri =
            key

        return result
    }

    private fun exportProofVideo() {
        exportCompositionVideo(
            fullSong = false,
        )
    }

    private fun exportFullSongVideo() {
        exportCompositionVideo(
            fullSong = true,
        )
    }

    private fun exportCompositionVideo(
        fullSong: Boolean,
    ) {
        val analysis =
            offlineAnalysis
                ?: run {
                    toast(
                        "Спочатку виконай offline analysis",
                    )
                    return
                }

        val sourceUri =
            controller
                .currentTrackUri()
                ?: run {
                    toast(
                        "Поточний трек відсутній",
                    )
                    return
                }

        val currentUriKey =
            sourceUri.toString()

        if (
            offlineAnalysisUri !=
            currentUriKey
        ) {
            toast(
                "Offline analysis не відповідає поточному треку",
            )
            return
        }

        val snapshot =
            latestSnapshot
        val ratio =
            exportAspectRatio
        val compositionConfig =
            currentCompositionExportConfig()

        val exportStartMs =
            if (fullSong) {
                0L
            } else {
                snapshot.positionMs
                    .coerceAtMost(
                        (
                            analysis.durationMs -
                                500L
                            )
                            .coerceAtLeast(
                                0L,
                            ),
                    )
            }

        val requestedDurationMs =
            if (fullSong) {
                analysis.durationMs
                    .coerceAtLeast(
                        500L,
                    )
            } else {
                minOf(
                    PROJECTM_EXPORT_DURATION_MS,
                    (
                        analysis.durationMs -
                            exportStartMs
                        )
                        .coerceAtLeast(
                            0L,
                        ),
                )
                    .coerceAtLeast(
                        500L,
                    )
            }

        val projectMView =
            projectMExportLiveView
                ?.takeIf {
                    compositionConfig
                        .projectMVisible
                }

        val directGpuProjectMEligible =
            projectMView !=
                null &&
                compositionConfig
                    .projectMVisible &&
                !compositionConfig
                    .faricReactiveVisible &&
                !compositionConfig
                    .overVisualizationVisible &&
                !compositionConfig
                    .bigEqualizerVisible &&
                compositionConfig
                    .graphicFiguresVisible &&
                compositionConfig
                    .cyberSharkConfig
                    .visibility[
                        BoardLayerId.BACKGROUND
                    ] !=
                    false

        val cancelled =
            AtomicBoolean(
                false,
            )

        val dialog =
            android.app.Dialog(
                this,
            ).apply {
                requestWindowFeature(
                    android.view.Window
                        .FEATURE_NO_TITLE,
                )
                setCancelable(
                    true,
                )
                setCanceledOnTouchOutside(
                    false,
                )
                setOnCancelListener {
                    cancelled.set(
                        true,
                    )
                }
            }

        val panel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    dp(18),
                    dp(16),
                    dp(18),
                    dp(16),
                )
                background =
                    panelDrawable(
                        Color.argb(
                            250,
                            22,
                            25,
                            29,
                        ),
                        24,
                        Color.argb(
                            90,
                            255,
                            255,
                            255,
                        ),
                        1,
                    )
            }

        panel.addView(
            label(
                if (fullSong) {
                    "Експорт усієї пісні"
                } else {
                    "Тестовий MP4 · 3 с"
                },
                20f,
                Color.WHITE,
                true,
            ),
        )

        val progressStatus =
            label(
                "Підготовка…",
                13f,
                COLOR_MUTED,
                false,
            )

        panel.addView(
            progressStatus,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin =
                    dp(8)
            },
        )

        val progressBar =
            ProgressBar(
                this,
                null,
                android.R.attr
                    .progressBarStyleHorizontal,
            ).apply {
                max =
                    100
                progress =
                    0
            }

        panel.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(12),
            ).apply {
                topMargin =
                    dp(14)
            },
        )

        panel.addView(
            actionPill(
                text =
                    "Скасувати",
                accent =
                    false,
            ) {
                cancelled.set(
                    true,
                )
                progressStatus.text =
                    "Скасування…"
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46),
            ).apply {
                topMargin =
                    dp(14)
            },
        )

        dialog.setContentView(
            panel,
        )
        dialog.show()

        dialog.window
            ?.apply {
                setBackgroundDrawable(
                    android.graphics.drawable
                        .ColorDrawable(
                            Color.TRANSPARENT,
                        ),
                )
                setLayout(
                    (
                        resources
                            .displayMetrics
                            .widthPixels *
                            0.88f
                        )
                        .toInt(),
                    ViewGroup.LayoutParams
                        .WRAP_CONTENT,
                )
                setGravity(
                    Gravity.CENTER,
                )
            }

        window.addFlags(
            android.view.WindowManager
                .LayoutParams
                .FLAG_KEEP_SCREEN_ON,
        )

        fun updateProgress(
            progress: Int,
            status: String,
        ) {
            runOnUiThread {
                if (!isFinishing) {
                    progressBar.progress =
                        progress
                            .coerceIn(
                                0,
                                100,
                            )
                    progressStatus.text =
                        status
                }
            }
        }

        thread(
            name =
                if (fullSong) {
                    "faric-full-song-export"
                } else {
                    "faric-preview-export"
                },
        ) {
            var pcmReader:
                com.saney.musicvisualizer.export
                    .OfflinePcmReader? =
                null

            var offlineProjectM =
                false

            runCatching {
                val needsProjectMPcm =
                    projectMView !=
                        null

                val pcmPrepWeight =
                    if (
                        needsProjectMPcm &&
                        (
                            offlinePcmCacheUri !=
                                currentUriKey ||
                                offlinePcmCache
                                    ?.file
                                    ?.isFile !=
                                true
                            )
                    ) {
                        10
                    } else {
                        0
                    }

                if (needsProjectMPcm) {
                    updateProgress(
                        0,
                        "Готую PCM для projectM…",
                    )

                    val pcmCache =
                        ensureOfflinePcmCache(
                            sourceUri =
                                sourceUri,
                            cancelled =
                                cancelled,
                        ) { progress ->
                            updateProgress(
                                progress *
                                    pcmPrepWeight /
                                    100,
                                "Готую PCM для projectM · " +
                                    progress +
                                    "%",
                            )
                        }

                    pcmReader =
                        pcmCache
                            .openReader()

                    check(
                        projectMView
                            .awaitReadyBlocking()
                    ) {
                        "projectM export surface не готовий"
                    }

                    if (
                        directGpuProjectMEligible
                    ) {
                        check(
                            projectMView
                                .releaseProjectMBlocking()
                        ) {
                            "projectM export renderer не звільнив GL-контекст"
                        }
                    } else {
                        check(
                            projectMView
                                .resetOfflineRendererBlocking()
                        ) {
                            "projectM offline renderer не скинувся"
                        }
                    }

                    offlineProjectM =
                        true
                }

                updateProgress(
                    pcmPrepWeight,
                    if (fullSong) {
                        "Рендерю від 0:00 до кінця…"
                    } else {
                        "Рендерю 3 секунди…"
                    },
                )

                val renderSpan =
                    100 -
                        pcmPrepWeight

                val projectMAvailableMs =
                    (
                        analysis.durationMs -
                            exportStartMs
                        )
                        .coerceAtLeast(
                            0L,
                        )
                val projectMRenderDurationMs =
                    minOf(
                        requestedDurationMs
                            .coerceAtLeast(
                                500L,
                            ),
                        projectMAvailableMs,
                    ).coerceAtLeast(
                        500L,
                    )
                val projectMFrameCount =
                    (
                        projectMRenderDurationMs *
                            PROJECTM_EXPORT_FPS /
                            1000L
                        )
                        .toInt()
                        .coerceAtLeast(
                            1,
                        )

                val directGpuProjectMConfig =
                    if (
                        directGpuProjectMEligible &&
                        pcmReader !=
                            null
                    ) {
                        val profile =
                            ProjectMPerformanceProfile
                                .BALANCED_BACKGROUND
                        val state =
                            ProjectMStateStore(
                                this,
                            )
                        val preset =
                            state
                                .lastPresetFileOrNull()
                                ?: error(
                                    "projectM preset missing for direct GPU export",
                                )
                        val textureDirectory =
                            ProjectMLibraryManager
                                .textureDir(
                                    this,
                                )

                        check(
                            textureDirectory
                                .isDirectory
                        ) {
                            "projectM texture directory missing"
                        }

                        ShortVideoExportProof
                            .DirectGpuProjectMConfig(
                                renderWidth =
                                    (
                                        ratio.width *
                                            profile.renderScale
                                        )
                                        .roundToInt()
                                        .coerceAtLeast(
                                            1,
                                        ),
                                renderHeight =
                                    (
                                        ratio.height *
                                            profile.renderScale
                                        )
                                        .roundToInt()
                                        .coerceAtLeast(
                                            1,
                                        ),
                                presetPath =
                                    preset.absolutePath,
                                texturePath =
                                    textureDirectory
                                        .absolutePath,
                                profile =
                                    profile,
                                foregroundSample =
                                    state
                                        .foregroundSample,
                                pcmProvider = {
                                        frameIndex,
                                        frameTimeMs,
                                    ->
                                    if (
                                        cancelled.get()
                                    ) {
                                        throw CancellationException(
                                            "Export cancelled",
                                        )
                                    }

                                    val nextFrameTimeMs =
                                        exportStartMs +
                                            (
                                                frameIndex +
                                                    1L
                                                ) *
                                            1000L /
                                            PROJECTM_EXPORT_FPS

                                    pcmReader
                                        ?.samplesBetween(
                                            startMs =
                                                frameTimeMs,
                                            endMs =
                                                nextFrameTimeMs,
                                        )
                                        ?: error(
                                            "projectM PCM reader unavailable",
                                        )
                                },
                            )
                    } else {
                        null
                    }

                var pendingProjectMFrame:
                    ProjectMOfflineFrameRequest? =
                    null
                var pendingProjectMFrameIndex =
                    -1

                fun queueProjectMFrame(
                    frameIndex: Int,
                    frameTimeMs: Long,
                ): ProjectMOfflineFrameRequest {
                    val activeView =
                        projectMView
                            ?: error(
                                "projectM export view unavailable",
                            )
                    val activePcmReader =
                        pcmReader
                            ?: error(
                                "projectM PCM reader unavailable",
                            )

                    val nextFrameTimeMs =
                        exportStartMs +
                            (
                                frameIndex +
                                    1L
                                ) *
                            1000L /
                            PROJECTM_EXPORT_FPS

                    val pcm =
                        activePcmReader
                            .samplesBetween(
                                startMs =
                                    frameTimeMs,
                                endMs =
                                    nextFrameTimeMs,
                            )

                    val signal =
                        analysis
                            .signalAt(
                                frameTimeMs,
                            )

                    return activeView
                        .queueOfflineFrame(
                            frameIndex =
                                frameIndex,
                            frameTimeSeconds =
                                frameIndex
                                    .toDouble() /
                                    PROJECTM_EXPORT_FPS,
                            pcm =
                                pcm,
                            signal =
                                signal,
                        )
                        ?: error(
                            "projectM offline frame " +
                                frameIndex +
                                " не поставлено в чергу",
                        )
                }

                ShortVideoExportProof.export(
                    context = this,
                    sourceAudioUri =
                        sourceUri,
                    project =
                        MusicVideoProject(
                            themeId =
                                PlaybackThemeId.CYBER_SHARK,
                            aspectRatio =
                                ratio,
                            frameRate =
                                PROJECTM_EXPORT_FPS,
                        ),
                    analysis =
                        analysis,
                    title =
                        snapshot.trackName
                            .orEmpty(),
                    artist =
                        if (
                            snapshot.trackName ==
                                null
                        ) {
                            ""
                        } else {
                            "Невідомий виконавець"
                        },
                    startMs =
                        exportStartMs,
                    compositionConfig =
                        compositionConfig,
                    directGpuProjectMConfig =
                        directGpuProjectMConfig,
                    projectMFrameProvider =
                        if (
                            directGpuProjectMConfig ==
                                null &&
                            projectMView !=
                                null &&
                            pcmReader !=
                                null
                        ) {
                            {
                                frameIndex,
                                frameTimeMs,
                            ->
                                if (
                                    cancelled.get()
                                ) {
                                    throw CancellationException(
                                        "Export cancelled",
                                    )
                                }

                                val queued =
                                    pendingProjectMFrame

                                if (
                                    queued != null &&
                                    pendingProjectMFrameIndex !=
                                    frameIndex
                                ) {
                                    error(
                                        "projectM pipeline desync: " +
                                            pendingProjectMFrameIndex +
                                            " != " +
                                            frameIndex,
                                    )
                                }

                                val request =
                                    queued
                                        ?: queueProjectMFrame(
                                            frameIndex =
                                                frameIndex,
                                            frameTimeMs =
                                                frameTimeMs,
                                        )

                                pendingProjectMFrame =
                                    null
                                pendingProjectMFrameIndex =
                                    -1

                                val frame =
                                    projectMView
                                        .awaitOfflineFrame(
                                            request,
                                        )
                                        ?: error(
                                            "projectM offline frame " +
                                                frameIndex +
                                                " не відрендерився",
                                        )

                                val nextFrameIndex =
                                    frameIndex +
                                        1

                                if (
                                    !cancelled.get() &&
                                    nextFrameIndex <
                                    projectMFrameCount
                                ) {
                                    val nextFrameTimeMs =
                                        exportStartMs +
                                            nextFrameIndex *
                                            1000L /
                                            PROJECTM_EXPORT_FPS

                                    pendingProjectMFrame =
                                        queueProjectMFrame(
                                            frameIndex =
                                                nextFrameIndex,
                                            frameTimeMs =
                                                nextFrameTimeMs,
                                        )
                                    pendingProjectMFrameIndex =
                                        nextFrameIndex
                                }

                                frame
                            }
                        } else {
                            null
                        },
                    projectMRawChannelsCorrectProvider =
                        if (
                            directGpuProjectMConfig ==
                                null
                        ) {
                            {
                                projectMView
                                    ?.offlineReadbackChannelsAreCorrect() ==
                                    true
                            }
                        } else {
                            null
                        },
                    projectMOfflineTimingProvider =
                        if (
                            directGpuProjectMConfig ==
                                null
                        ) {
                            {
                                projectMView
                                    ?.offlineTimingSnapshot()
                            }
                        } else {
                            null
                        },
                    requestedDurationMs =
                        requestedDurationMs,
                    fps =
                        PROJECTM_EXPORT_FPS,
                    displayNamePrefix =
                        if (fullSong) {
                            "FARIC-Full"
                        } else {
                            "FARIC-preview"
                        },
                    shouldCancel = {
                        cancelled.get()
                    },
                    onProgress = {
                            progress ->
                        val mapped =
                            pcmPrepWeight +
                                progress *
                                    renderSpan /
                                    100

                        updateProgress(
                            mapped,
                            (
                                if (fullSong) {
                                    "Експорт усієї пісні · "
                                } else {
                                    "Тестовий MP4 · "
                                }
                                ) +
                                mapped +
                                "%",
                        )
                    },
                )
            }.onSuccess { result ->
                runOnUiThread {
                    dialog.dismiss()
                    window.clearFlags(
                        android.view.WindowManager
                            .LayoutParams
                            .FLAG_KEEP_SCREEN_ON,
                    )

                    if (
                        result.uri !=
                        null
                    ) {
                        if (fullSong) {
                            toast(
                                "Готово · уся пісня · " +
                                    result.width +
                                    "×" +
                                    result.height +
                                    " · Movies/FARIC",
                            )
                        } else {
                            showExportTimingResult(
                                result,
                            )
                        }
                    } else {
                        toast(
                            "Не вдалося зберегти MP4",
                        )
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    dialog.dismiss()
                    window.clearFlags(
                        android.view.WindowManager
                            .LayoutParams
                            .FLAG_KEEP_SCREEN_ON,
                    )

                    if (
                        error is
                            CancellationException
                    ) {
                        toast(
                            "Експорт скасовано",
                        )
                    } else {
                        toast(
                            "MP4 export: " +
                                (
                                    error.message
                                        ?: error
                                            .javaClass
                                            .simpleName
                                    ),
                        )
                    }
                }
            }

            runCatching {
                pcmReader
                    ?.close()
            }

            if (
                offlineProjectM &&
                directGpuProjectMEligible
            ) {
                runCatching {
                    projectMView
                        ?.resetOfflineRendererBlocking()
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
        val ratio =
            exportAspectRatio
        val compositionConfig =
            currentCompositionExportConfig()

        toast(
            "Рендерю композицію ${ratio.width}×${ratio.height}…",
        )

        thread(name = "faric-export-frame") {
            runCatching {
                val project =
                    MusicVideoProject(
                        themeId =
                            PlaybackThemeId.CYBER_SHARK,
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
                        context = this,
                        project = project,
                        input = input,
                        timeMs = snapshot.positionMs,
                        compositionConfig =
                            compositionConfig,
                        title =
                            snapshot.trackName.orEmpty(),
                        artist =
                            if (
                                snapshot.trackName ==
                                    null
                            ) {
                                ""
                            } else {
                                "Невідомий виконавець"
                            },
                        durationMs =
                            snapshot.durationMs,
                    )

                val uri =
                    ExportFrameProof.savePng(
                        context = this,
                        bitmap = bitmap,
                        displayName =
                            "FARIC-Composition-${System.currentTimeMillis()}.png",
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
            PlaybackThemeId.CYBER_SHARK,
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

                    if (
                        isLayeredBoardTheme(
                            spec.id,
                        )
                    ) {
                        setGraphicFigureThemeId(
                            spec.id,
                        )
                    }

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
        id ==
            PlaybackThemeId.CYBER_SHARK ||
            id ==
            PlaybackThemeId.CYBER_PANTHER

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

        PulseDeckDialogs.showSingleChoice(
            context = this,
            title = "Автоприховування",
            labels = labels.toList(),
            checkedIndex = checked,
            cancelLabel = "Скасувати",
        ) { which ->
            controlsAutoHideMode =
                modes[which]
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE,
            )
                .edit()
                .putString(
                    KEY_CONTROLS_AUTO_HIDE_MODE,
                    controlsAutoHideMode.name,
                )
                .apply()
            nowControlsHidden =
                false
            setNowControlsVisible(
                visible = true,
                animate = false,
            )
        }
    }

    private fun clearScreenRefs() {
        ProjectMBridge
            .endOfflineExport()
        projectMExportLiveView
            ?.releaseProjectMBlocking()
        if (projectMExportLiveResumed) {
            projectMExportLiveView
                ?.onPause()
            projectMExportLiveResumed =
                false
        }
        projectMExportLiveView =
            null

        projectMMainView
            ?.releaseProjectMBlocking()
        if (projectMMainResumed) {
            projectMMainView?.onPause()
            projectMMainResumed = false
        }
        projectMMainView = null

        nowControlsLayer?.removeCallbacks(
            nowControlsAutoHideRunnable,
        )
        pulseDeckMainSkinView
            ?.removeCallbacks(
                nowControlsAutoHideRunnable,
            )
        sceneView = null
        effectsView = null
        bigEqualizerView = null
        pulseDeckLayerStack = null
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
                showBoardTransform()

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

    private fun showExportTimingResult(
        result: ShortVideoExportProof.Result,
    ) {
        val message =
            buildString {
                append(
                    result.width,
                )
                append("×")
                append(
                    result.height,
                )
                append(" · ")
                append(
                    result.frameCount,
                )
                append(" кадрів")
                append("\n\nprojectM: ")
                append(
                    result.projectMFrameMs,
                )
                append(" ms")
                append("\ncomposition: ")
                append(
                    result.compositionMs,
                )
                append(" ms")
                append("\nprojectM BGRA: ")
                append(
                    when {
                        result
                            .projectMGpuDirect ->
                            "GPU direct"

                        result
                            .projectMDirectBgra ->
                            "yes"

                        else ->
                            "fallback"
                    },
                )

                result.projectMOfflineStages
                    ?.let { stages ->
                        append("\nprojectM internals")
                        append("\nqueue wait: ")
                        append(
                            stages.queueWaitMs,
                        )
                        append(" ms")
                        append("\nnative render: ")
                        append(
                            stages.nativeRenderMs,
                        )
                        append(" ms")
                        append("\nglReadPixels: ")
                        append(
                            stages.readPixelsMs,
                        )
                        append(" ms")
                        append("\nbitmap copy: ")
                        append(
                            stages.bitmapCopyMs,
                        )
                        append(" ms")
                    }

                append("\nencoder: ")
                append(
                    result.encoderSubmitMs,
                )
                append(" ms")
                if (
                    result.gpuProjectMMs >
                        0L ||
                    result.gpuGlowMs >
                        0L ||
                    result.gpuFrameMs >
                        0L ||
                    result.gpuCreatureMs >
                        0L ||
                    result.gpuWordmarkMs >
                        0L ||
                    result.gpuOverlayMs >
                        0L
                ) {
                    if (
                        result.gpuProjectMMs >
                            0L
                    ) {
                        append("\nGPU projectM: ")
                        append(
                            result.gpuProjectMMs,
                        )
                        append(" ms")
                    }
                    append("\nGPU glow: ")
                    append(
                        result.gpuGlowMs,
                    )
                    append(" ms")
                    if (
                        result.gpuFrameMs >
                            0L
                    ) {
                        append("\nGPU frame: ")
                        append(
                            result.gpuFrameMs,
                        )
                        append(" ms")
                    }
                    if (
                        result.gpuCreatureMs >
                            0L
                    ) {
                        append("\nGPU creature: ")
                        append(
                            result.gpuCreatureMs,
                        )
                        append(" ms")
                    }
                    if (
                        result.gpuWordmarkMs >
                            0L
                    ) {
                        append("\nGPU wordmark: ")
                        append(
                            result.gpuWordmarkMs,
                        )
                        append(" ms")
                    }
                    append("\nGPU overlay: ")
                    append(
                        result.gpuOverlayMs,
                    )
                    append(" ms")
                }
                append("\naudio: ")
                append(
                    result.audioTranscodeMs,
                )
                append(" ms")
                append("\nmux: ")
                append(
                    result.muxMs,
                )
                append(" ms")
                append("\nsave: ")
                append(
                    result.publishMs,
                )
                append(" ms")
                append("\n\ntotal: ")
                append(
                    result.totalMs,
                )
                append(" ms")

                result.compositionStages
                    ?.let { stages ->
                        append("\n\ncomposition layers")
                        append("\nclear: ")
                        append(
                            stages.clearMs,
                        )
                        append(" ms")
                        append("\nprojectM draw: ")
                        append(
                            stages.projectMDrawMs,
                        )
                        append(" ms")
                        append("\nreactive: ")
                        append(
                            stages.faricReactiveMs,
                        )
                        append(" ms")
                        append("\noverlay: ")
                        append(
                            stages.overVisualizationMs,
                        )
                        append(" ms")
                        append("\nbig EQ: ")
                        append(
                            stages.bigEqualizerMs,
                        )
                        append(" ms")
                        append("\nGraphic Figure · ")
                        append(
                            GraphicFigureCatalog
                                .title(
                                    currentGraphicFigureThemeId(),
                                ),
                        )
                        append(": ")
                        append(
                            stages.cyberSharkMs,
                        )
                        append(" ms")

                        stages.cyberSharkStages
                            ?.let {
                                    cyber ->
                                append("\nGraphic Figure internals")
                                append("\nbackground: ")
                                append(
                                    cyber.backgroundMs,
                                )
                                append(" ms")
                                append("\nbackground internals")
                                append("\nsetup/save: ")
                                append(
                                    cyber.backgroundSetupMs,
                                )
                                append(" ms")
                                append("\nglow: ")
                                append(
                                    cyber.backgroundGlowMs,
                                )
                                append(" ms")
                                append("\nglow render: ")
                                append(
                                    cyber.backgroundGlowRenderMs,
                                )
                                append(" ms")
                                append("\nglow composite: ")
                                append(
                                    cyber.backgroundGlowCompositeMs,
                                )
                                append(" ms")
                                append("\narcs: ")
                                append(
                                    cyber.backgroundArcsMs,
                                )
                                append(" ms")
                                append("\nparticles: ")
                                append(
                                    cyber.backgroundParticlesMs,
                                )
                                append(" ms")
                                append("\nrestore: ")
                                append(
                                    cyber.backgroundRestoreMs,
                                )
                                append(" ms")
                                append("\nframe: ")
                                append(
                                    cyber.frameMs,
                                )
                                append(" ms")
                                append("\nFX: ")
                                append(
                                    cyber.fxMs,
                                )
                                append(" ms")
                                append("\ncreature: ")
                                append(
                                    cyber.creatureMs,
                                )
                                append(" ms")
                                append("\nwordmark: ")
                                append(
                                    cyber.wordmarkMs,
                                )
                                append(" ms")
                            }

                        append("\neffects: ")
                        append(
                            stages.effectsMs,
                        )
                        append(" ms")
                        append("\nHUD update: ")
                        append(
                            stages.pulseDeckUpdateMs,
                        )
                        append(" ms")
                        append("\nHUD draw: ")
                        append(
                            stages.pulseDeckDrawMs,
                        )
                        append(" ms")
                    }

                append("\n\nMovies/FARIC")
            }

        PulseDeckDialogs.showMessage(
            context = this,
            title = "Експорт завершено",
            message = message,
            primaryLabel = "OK",
            secondaryLabel = "Копіювати текст",
        ) {
            val clipboard =
                getSystemService(
                    android.content.Context.CLIPBOARD_SERVICE,
                ) as
                    android.content.ClipboardManager

            clipboard.setPrimaryClip(
                android.content.ClipData
                    .newPlainText(
                        "FARIC export timing",
                        message,
                    ),
            )

            toast(
                "Текст скопійовано",
            )
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PROJECTM_EXPORT_FPS =
            30
        private const val PROJECTM_EXPORT_DURATION_MS =
            3_000L

        private val PULSEDECK_EXPORT_OBJECT_IDS =
            listOf(
                "energy_waves",
                "particles_orange_left",
                "particles_blue_left",
                "particles_blue_right",
                "particles_orange_right",
                "reactor_energy_ring",
                "hero_frame",
                "hero_core",
                "favorite",
                "track_more",
                "waveform",
                "progress_line",
                "transport_rail",
                "shuffle",
                "previous",
                "play_pause",
                "next",
                "repeat",
                "quick_rail",
                "theme",
                "board",
                "visualizer",
                "export",
                "back",
                "menu",
                "header_title",
                "track_info",
                "progress_time",
                "progress_thumb",
            )

        private const val PREFS_NAME =
            "faric.preferences"
        private const val KEY_CONTROLS_AUTO_HIDE_MODE =
            "faric.controls_auto_hide_mode"
        private const val KEY_SCREEN =
            "faric.screen"
        private const val KEY_SELECTED_THEME =
            "faric.selected_theme"
        private const val KEY_GF_THEME_ID =
            "faric.gf_theme_id"
        private const val KEY_EXPORT_ASPECT_RATIO =
            "faric.export_aspect_ratio"
        private const val KEY_BOARD_EDITOR_LAYER =
            "faric.board_editor_layer"
        private const val KEY_NOW_CONTROLS_HIDDEN =
            "faric.now_controls_hidden"
        private const val NOW_CONTROLS_AUTO_HIDE_MS =
            6000L
        private const val EMERGENCY_RECOVERY_HOLD_MS =
            10_000L
        private const val KEY_VISUALIZER_SPLIT_MIGRATED =
            "faric.visualizer_split_migrated_v1"
        private const val KEY_VISUALIZER_OBJECTS_MIGRATED =
            "faric.visualizer_objects_migrated_v2"
        private const val KEY_COMPOSITION_SET_NAMES =
            "faric.composition_set_names"
        private const val KEY_HAS_VERTICAL_SCROLL =
            "faric.has_vertical_scroll"
        private const val KEY_VERTICAL_SCROLL_Y =
            "faric.vertical_scroll_y"
        private const val KEY_HAS_HORIZONTAL_SCROLL =
            "faric.has_horizontal_scroll"
        private const val KEY_HORIZONTAL_SCROLL_X =
            "faric.horizontal_scroll_x"
        private const val KEY_LAYER_PANEL_X =
            "faric.layer_panel_x"
        private const val KEY_LAYER_PANEL_Y =
            "faric.layer_panel_y"

        private val COLOR_BG = Color.rgb(2, 6, 10)
        private val COLOR_PANEL = Color.rgb(13, 20, 27)
        private val COLOR_PANEL_2 = Color.rgb(22, 28, 35)
        private val COLOR_MUTED = Color.rgb(165, 178, 190)
        private val COLOR_ACCENT_ORANGE = Color.rgb(255, 153, 24)
        private val COLOR_ACCENT_CYAN = Color.rgb(35, 211, 238)
    }
}
