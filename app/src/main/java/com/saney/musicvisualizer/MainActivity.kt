package com.saney.musicvisualizer

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.os.Build
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
import android.widget.ImageView
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
import androidx.media3.common.util.UnstableApi
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.board.BoardGroupReaction
import com.saney.musicvisualizer.board.BoardGroupReactionStore
import com.saney.musicvisualizer.board.BoardLayerReactionTuning
import com.saney.musicvisualizer.board.BoardLayerReactionTuningStore
import com.saney.musicvisualizer.board.GraphicFigureCatalog
import com.saney.musicvisualizer.board.GraphicFigureAssets
import com.saney.musicvisualizer.board.GfAssetLibrary
import com.saney.musicvisualizer.board.GfArtType
import com.saney.musicvisualizer.board.GfMixController
import com.saney.musicvisualizer.board.ExternalPngHeroLibrary
import com.saney.musicvisualizer.board.UserHeroPack
import com.saney.musicvisualizer.board.BoardLayerId
import com.saney.musicvisualizer.board.BoardLayerTransform
import com.saney.musicvisualizer.board.BoardLayerTransformStore
import com.saney.musicvisualizer.board.BoardTransform
import com.saney.musicvisualizer.board.BoardTransformStore
import com.saney.musicvisualizer.diagnostics.CrashJournal
import com.saney.musicvisualizer.export.CompositionExportConfig
import com.saney.musicvisualizer.export.CyberSharkExportConfig
import com.saney.musicvisualizer.export.ExportFrameProof
import com.saney.musicvisualizer.export.OfflineAnalysisResult
import com.saney.musicvisualizer.export.OfflineAudioAnalyzer
import com.saney.musicvisualizer.export.OfflinePcmCache
import com.saney.musicvisualizer.export.OfflinePcmCacheResult
import com.saney.musicvisualizer.export.ShortVideoExportProof
import com.saney.musicvisualizer.playback.PlaybackController
import com.saney.musicvisualizer.playback.PlaybackControllerHolder
import com.saney.musicvisualizer.playback.PlaybackSnapshot
import com.saney.musicvisualizer.playback.QueueTrack
import com.saney.musicvisualizer.projectm.ProjectMBridge
import com.saney.musicvisualizer.projectm.ProjectMLibraryManager
import com.saney.musicvisualizer.projectm.ProjectMOfflineFrameRequest
import com.saney.musicvisualizer.projectm.ProjectMPerformanceProfile
import com.saney.musicvisualizer.projectm.ProjectMSettingsPanel
import com.saney.musicvisualizer.projectm.ProjectMStateStore
import com.saney.musicvisualizer.projectm.ProjectMPresetRatingsStore
import com.saney.musicvisualizer.projectm.ProjectMPresetRating
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
import com.saney.musicvisualizer.ui.HeroGalleryPolicy
import com.saney.musicvisualizer.ui.PlayerSwipeNavigation
import com.saney.musicvisualizer.ui.HeroThemeView
import com.saney.musicvisualizer.ui.LocalMusicBrowser
import com.saney.musicvisualizer.playback.LocalMusicCatalog
import com.saney.musicvisualizer.playback.LocalMusicCategory
import com.saney.musicvisualizer.playback.LocalMusicLibrary
import com.saney.musicvisualizer.playback.LocalMusicUserState
import com.saney.musicvisualizer.ui.PulseMiniView
import com.saney.musicvisualizer.ui.PulseDeckControlRail
import com.saney.musicvisualizer.ui.PulseDeckIconButton
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView
import com.saney.musicvisualizer.ui.BigEqualizerView
import com.saney.musicvisualizer.ui.OverVisualizationView
import com.saney.musicvisualizer.ui.PulseDeckLayerStack
import com.saney.musicvisualizer.ui.PulseDeckDialogs
import com.saney.musicvisualizer.ui.ReactiveSceneView
import java.io.File
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import com.saney.musicvisualizer.export.ExportRunGate
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
        TRACKS,
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
    private lateinit var boardLayerReactionTuningStore: BoardLayerReactionTuningStore
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

    private val exportRunGate = ExportRunGate()
    private var orientationBeforeFullSongExport: Int? = null

    /** Cancel flag of the running MP4 export, so onDestroy can stop it cleanly (V25). */
    @Volatile private var activeExportCancel: AtomicBoolean? = null

    private var offlineAnalysis: OfflineAnalysisResult? = null
    private var offlineAnalysisUri: String? = null
    private var offlineAnalysisRunning = false

    private var offlinePcmCache: OfflinePcmCacheResult? = null
    private var offlinePcmCacheUri: String? = null

    private var sceneView: ReactiveSceneView? = null
    private var projectMMainView: ProjectMView? = null
    private var projectMMainResumed = false
    private var projectMOpenPending = false

    // One live render stack per player screen; projectM authoring is a menu
    // above that stack, never a second Activity/native bridge.
    private var persistentSceneRoot: FrameLayout? = null

    // V3 (faric 19.10 23:31): Export Lab is an overlay above the running scene, like the
    // library — opening / closing it rebuilds nothing; projectM keeps playing.
    private var exportLabOverlay: FrameLayout? = null
    private var boardMenuOverlay: FrameLayout? = null
    private var themeMenuOverlay: FrameLayout? = null
    private var mediaMenuOverlay: FrameLayout? = null
    private var localMusicBrowser: LocalMusicBrowser? = null
    private var musicBrowseCategory = LocalMusicCategory.ALL
    private var musicBrowseGroup: String? = null
    private var musicBrowseQuery = ""
    private var lastRecordedMusicUri: String? = null
    private var themeMenuOriginalTheme: PlaybackThemeId? = null
    private var themeMenuLiveHeroSwitch = false
    private var heroAssetLoadGeneration = 0
    private var projectMFocusOverlay: FrameLayout? = null
    private var projectMFocusSavedVisibility:
        Map<PulseDeckLayerStack.Layer, Boolean>? = null
    private var projectMFocusPresets: List<File> = emptyList()
    private var projectMFocusCatalogGeneration = 0
    private var projectMFocusStatus: TextView? = null
    private var projectMFocusAutoControl: TextView? = null
    private var projectMFocusBackgroundControl: TextView? = null
    private var projectMFocusCenterControl: TextView? = null
    private var projectMFocusEdgeControl: TextView? = null
    private val projectMFocusHandler = Handler(Looper.getMainLooper())
    private val projectMFocusAutoTick = object : Runnable {
        override fun run() {
            if (projectMFocusOverlay == null) return
            val state = ProjectMStateStore(this@MainActivity)
            if (!state.autoEnabled) return
            advanceProjectMFocusPreset(manual = false)
            projectMFocusHandler.postDelayed(this, state.autoSwitchSeconds * 1_000L)
        }
    }
    private var projectMRebuildPending = false
    private var projectMRebuildTarget: Screen? = null
    private var projectMExportSnapshot: Bitmap? = null
    private var projectMExportLiveView: ProjectMView? = null
    private var projectMExportLiveResumed = false
    private var effectsView: ReactiveSceneView? = null
    private var bigEqualizerView: BigEqualizerView? = null
    private var pulseDeckLayerStack: PulseDeckLayerStack? = null
    private var heroBoardView: HeroBoardView? = null
    private val gfMixController by lazy { GfMixController(this) }
    private var gfMixLoadGeneration = 0

    // V3 (faric 19.10 23:15): the last decoded GF artwork, kept while the app lives.
    // When a screen comes back (Export Lab → player, Board), the hero is applied at once
    // instead of showing Cyber Shark for ~0.8 s while faric's PNGs are decoded again.
    private var gfArtworkCache: Pair<String, GraphicFigureAssets>? = null
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
    private var playerSwipeEligible = false
    private var playerSwipeDownX = 0f
    private var playerSwipeDownY = 0f
    private val emergencyRecoveryRunnable =
        Runnable {
            if (
                emergencyRecoveryArmed &&
                screen == Screen.NOW_PLAYING &&
                projectMFocusOverlay == null
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

    private var heroCatalogExpanded = false

    // v0.19.73 folder batch: read-only access to a folder of songs.
    private val selectBatchFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            // Mode is persisted before launch: the SAF picker may recreate MainActivity.
            val prefs = getSharedPreferences(BATCH_REQUEST_PREFS, MODE_PRIVATE)
            val testMode = prefs.getBoolean(KEY_BATCH_TEST_MODE, false)
            prefs.edit().remove(KEY_BATCH_TEST_MODE).apply()
            if (uri != null) onBatchFolderPicked(uri, testMode)
        }

    // The SAF picker may recreate MainActivity while it is open.
    // A field defaulting to CREATURE used to silently misfile FRAME/FX/WORDMARK
    // imports under Heroes. Persist an explicit pending role *before* launch.
    private val pendingPngImportPrefs by lazy {
        getSharedPreferences("faric_external_png_import_request_v1", MODE_PRIVATE)
    }

    private val selectExternalPngFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val selectedType = pendingPngImportPrefs
                .getString("pending_type", null)
                ?.let { name -> runCatching { GfArtType.valueOf(name) }.getOrNull() }
            pendingPngImportPrefs.edit().remove("pending_type").apply()
            if (uri != null && selectedType == null) {
                PulseDeckDialogs.showMessage(
                    this, "Не вдалося визначити тип PNG",
                    "Папку не імпортовано, щоб випадково не додати рамки до героїв. " +
                        "Повтори вибір через потрібний шар GF Мікс.",
                )
            }
            if (uri != null && selectedType != null) {
                val access = runCatching {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                if (access.isFailure) {
                    toast("Не вдалося зберегти доступ до папки PNG")
                } else {
                    toast("Перевіряю PNG у вибраній папці…")
                    thread(name = "faric-saf-png-index") {
                        val result = runCatching {
                            ExternalPngHeroLibrary(applicationContext)
                                .scanFolder(uri, selectedType)
                        }
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                result.onSuccess { scan ->
                                    toast(
                                        "PNG ${selectedType.fileStem}: ${scan.accepted} · пропущено: " +
                                            "${scan.rejected} · повтори: ${scan.duplicates}",
                                    )
                                    if (selectedType == GfArtType.CREATURE) {
                                        heroCatalogExpanded = true
                                        showExternalPngHeroes()
                                    } else {
                                        toast(
                                            "Додано ${scan.accepted} PNG у ${selectedType.fileStem}. " +
                                                "Тепер вибери потрібний об'єкт."
                                        )
                                        showGfMixSourcePicker(selectedType)
                                    }
                                }.onFailure { e ->
                                    PulseDeckDialogs.showMessage(
                                        this, "Помилка PNG",
                                        e.message ?: "Не вдалося прочитати папку",
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    private val openHeroArchive =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                toast("Імпорт героїв…")
                thread(name = "faric-import-user-heroes") {
                    val result = runCatching { UserHeroPack.importPreparedZip(applicationContext, uri) }
                    runOnUiThread {
                        result.onSuccess { count ->
                            toast("Імпортовано героїв: $count")
                            if (screen == Screen.THEME_PICKER) {
                                heroCatalogExpanded = true
                                if (themeMenuOverlay != null && persistentSceneRoot != null) {
                                    (themeMenuOverlay?.parent as? ViewGroup)
                                        ?.removeView(themeMenuOverlay)
                                    themeMenuOverlay = null
                                    screen = Screen.NOW_PLAYING
                                }
                                showThemePicker()
                            }
                        }.onFailure { error ->
                            toast("ZIP: ${error.message ?: "Помилка імпорту"}")
                        }
                    }
                }
            }
        }

    private val requestMusicLibraryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (screen == Screen.TRACKS) {
                showAllTracks(category = musicBrowseCategory, selectedGroup = musicBrowseGroup, restoredQuery = musicBrowseQuery)
                if (!granted) {
                    toast("Доступ до музики не надано. Оберіть файли вручну.")
                }
            }
        }

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
                // The player and its HUD stayed mounted underneath projectM.
                // Restore only the native visualizer slot after the other
                // Activity released our shared ProjectMBridge.
                val stack = pulseDeckLayerStack
                if (stack != null) {
                    val startNs = System.nanoTime()
                    val restoredView = createLayer0ProjectMView()
                    projectMMainView = restoredView
                    stack.setContent(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        restoredView,
                    )
                    applyVisualizerRenderSlots()
                    updateProjectMRenderState()
                    Log.i(
                        "FARIC-nav",
                        "projectM return: restored only visualizer slot in " +
                            ((System.nanoTime() - startNs) / 1_000_000L) + " ms",
                    )
                } else {
                    showNowPlaying()
                }
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
        PulseDeckSystemBars.apply(this)

        controlsAutoHideMode =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_CONTROLS_AUTO_HIDE_MODE, null)
                ?.let { runCatching { ControlsAutoHideMode.valueOf(it) }.getOrNull() }
                ?: ControlsAutoHideMode.NEVER

        controller = PlaybackControllerHolder.obtain(application)
        controller.setAnalysisPermissionGranted(hasAnalysisPermission())

        themeStore = PlaybackThemeStore(this)
        boardTransformStore = BoardTransformStore(this)
        boardGroupReactionStore = BoardGroupReactionStore(this)
        boardLayerReactionTuningStore = BoardLayerReactionTuningStore(this)
        boardLayerTransformStore =
            BoardLayerTransformStore(this)

        ensureVisualizerLayerMigration()
        ensureVisualizerObjectModelMigration()
        ensureProjectMComponentVisibilityMigration()

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
            if (projectMFocusOverlay != null) {
                closeProjectMFocusOverlay()
                return@addCallback
            }
            when (screen) {
                Screen.NOW_PLAYING -> showLibrary()
                Screen.TRACKS -> {
                    if (localMusicBrowser?.navigateBack() != true) showLibrary()
                }
                // V70 (faric 19.10 23:26): «назад» from Board / Export Lab / themes always
                // returns to the centre (the player works without a song too), never to
                // the library on the left.
                Screen.THEME_PICKER,
                Screen.BOARD_TRANSFORM,
                Screen.EXPORT_LAB,
                -> showNowPlaying()
                Screen.LIBRARY -> {
                    if (mediaMenuOverlay != null) showNowPlaying() else finish()
                }
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

        musicBrowseCategory = savedInstanceState?.getString("local_music_category")?.let {
            runCatching { LocalMusicCategory.valueOf(it) }.getOrNull()
        } ?: LocalMusicCategory.ALL
        musicBrowseGroup = savedInstanceState?.getString("local_music_group")
        musicBrowseQuery = savedInstanceState?.getString("local_music_query").orEmpty()
        restoreScreen(restoredScreen)
        // V49: the window that was open before the rotation comes back.
        savedInstanceState?.getString(KEY_RESTORE_WINDOW_KIND)?.let { kind ->
            val window =
                RestorableWindow(
                    kind = kind,
                    title = savedInstanceState.getString(KEY_RESTORE_WINDOW_TITLE).orEmpty(),
                    message = savedInstanceState.getString(KEY_RESTORE_WINDOW_MESSAGE).orEmpty(),
                    extra = savedInstanceState.getString(KEY_RESTORE_WINDOW_EXTRA).orEmpty(),
                )
            this.window.decorView.postDelayed({
                if (!isFinishing && !isDestroyed) reopenWindow(window)
            }, 350L)
        }
        // Notification media-card tap returns to the requested app screen.
        openFromNotification(intent)
        // V24: after a crash / killed process, say why — once, on a cold start.
        if (savedInstanceState == null) {
            window.decorView.postDelayed({ maybeShowCrashReport() }, 900L)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromNotification(intent)
    }

    private fun openFromNotification(request: Intent?) {
        val destination = request?.getStringExtra(EXTRA_OPEN_SCREEN) ?: return
        request.removeExtra(EXTRA_OPEN_SCREEN)
        when (destination) {
            "NOW_PLAYING" -> showNowPlaying()
            "THEME_PICKER" -> showThemePicker()
            "BOARD_TRANSFORM" -> showBoardTransform()
            "EXPORT_LAB" -> showExportLab()
            "LIBRARY" -> showLibrary()
            else -> Unit
        }
    }

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putString(KEY_SCREEN, screen.name)
        restorableWindow?.let { w ->
            outState.putString(KEY_RESTORE_WINDOW_KIND, w.kind)
            outState.putString(KEY_RESTORE_WINDOW_TITLE, w.title)
            outState.putString(KEY_RESTORE_WINDOW_MESSAGE, w.message)
            outState.putString(KEY_RESTORE_WINDOW_EXTRA, w.extra)
        }
        outState.putString("local_music_category", musicBrowseCategory.name)
        outState.putString("local_music_group", musicBrowseGroup)
        outState.putString("local_music_query", musicBrowseQuery)
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

    /**
     * Emergency HUD recovery is driven by the Activity touch stream (not the
     * optional/hidden skin buttons). Horizontal navigation only accepts a
     * single-finger swipe beginning in the central artwork zone, away from the
     * player seek and transport rails and Android's system Back edge.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val active = screen == Screen.NOW_PLAYING && projectMFocusOverlay == null
        var navigation: PlayerSwipeNavigation.Destination? = null
        if (active) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    emergencyRecoveryArmed = true
                    emergencyRecoveryDownX = event.rawX
                    emergencyRecoveryDownY = event.rawY
                    emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
                    emergencyRecoveryHandler.postDelayed(
                        emergencyRecoveryRunnable, EMERGENCY_RECOVERY_HOLD_MS,
                    )

                    val width = resources.displayMetrics.widthPixels.toFloat()
                    val height = resources.displayMetrics.heightPixels.toFloat()
                    val insideArtwork = event.rawY >= height * 0.12f &&
                        event.rawY <= height * 0.54f
                    // Do not compete with OS edge-back, seek, HUD transport,
                    // or controls located at the top/bottom of the player.
                    playerSwipeEligible = insideArtwork &&
                        event.rawX >= dp(32) &&
                        event.rawX <= width - dp(32)
                    playerSwipeDownX = event.rawX
                    playerSwipeDownY = event.rawY
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    emergencyRecoveryArmed = false
                    playerSwipeEligible = false
                    emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - emergencyRecoveryDownX
                    val dy = event.rawY - emergencyRecoveryDownY
                    val slop = dp(44).toFloat()
                    if (dx * dx + dy * dy > slop * slop) {
                        emergencyRecoveryArmed = false
                        emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (playerSwipeEligible && event.pointerCount == 1) {
                        navigation = PlayerSwipeNavigation.resolve(
                            deltaX = event.rawX - playerSwipeDownX,
                            deltaY = event.rawY - playerSwipeDownY,
                            minimumDistance = dp(104).toFloat(),
                            maximumVerticalDrift = dp(64).toFloat(),
                        )
                    }
                    playerSwipeEligible = false
                    emergencyRecoveryArmed = false
                    emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
                }
                MotionEvent.ACTION_CANCEL -> {
                    playerSwipeEligible = false
                    emergencyRecoveryArmed = false
                    emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
                }
            }
        } else {
            playerSwipeEligible = false
            emergencyRecoveryArmed = false
            emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
        }

        // First release the child touch sequence, then navigate. In
        // particular, a swipe cannot be mistaken for a new seek position.
        val handled = super.dispatchTouchEvent(event)
        when (navigation) {
            PlayerSwipeNavigation.Destination.PROJECTM -> openProjectMFocusOverlay()
            PlayerSwipeNavigation.Destination.LIBRARY -> showLibrary()
            // V83: swipe down from the centre — Export Lab, even with every HUD layer hidden.
            PlayerSwipeNavigation.Destination.EXPORT -> openExportLabWithProjectMCapture()
            null -> Unit
        }
        return navigation != null || handled
    }

    private fun showEmergencyRecoveryDialog() {
        PulseDeckDialogs.show(
            context = this,
            title = "Аварійне відновлення PulseDeck",
            message =
                "Утримання 10 секунд: тут можна налаштувати потрібні кнопки або відновити приховане меню.",
            actions =
                listOf(
                    PulseDeckDialogs.Action(
                        label = "Налаштувати шари та кнопки",
                        accent = true,
                    ) {
                        showPulseDeckLayersDialog()
                    },
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
            when {
                PulseDeckSystemBars.alwaysVisible(this) -> PulseDeckSystemBars.apply(this)
                projectMFocusOverlay != null -> showProjectMSystemBars()
                screen == Screen.LIBRARY || screen == Screen.TRACKS ->
                    showMediaLibrarySystemBars()
                else -> enableImmersiveFullscreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (projectMFocusOverlay != null) scheduleProjectMFocusAuto()
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
        emergencyRecoveryArmed = false
        playerSwipeEligible = false
        emergencyRecoveryHandler.removeCallbacks(emergencyRecoveryRunnable)
        projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        // Do not pause playback here. Screen lock and Home both stop the Activity,
        // but a music player must keep playing. A MediaSessionService migration is
        // tracked separately for full long-lived background playback/notification.
        sceneOrchestrator.stop()
        if (projectMMainResumed) {
            projectMMainView?.onPause()
            projectMMainResumed = false
        }
        if (exportRunGate.isActive()) {
            // V25: pausing the GLSurfaceView would destroy the GL context the
            // export may be rendering projectM with. Keep it; endGuardedExport()
            // pauses it when the render is over.
            CrashJournal.exportNote(this, "екран сховано під час експорту (onStop)")
        } else if (projectMExportLiveResumed) {
            projectMExportLiveView?.onPause()
            projectMExportLiveResumed = false
        }
        controller.listener = null
        super.onStop()
    }

    override fun onDestroy() {
        projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        sceneOrchestrator.close()
        if (exportRunGate.isActive()) {
            // V25: the export worker still reads the PCM cache and drives the
            // offline projectM. Stop it at the next frame; its own finally-block
            // cleans up. Ending native export / deleting the PCM file here used
            // to pull them out from under the running render.
            activeExportCancel?.set(true)
            CrashJournal.exportNote(this, "екран знищено під час експорту (onDestroy) — експорт зупинено")
        } else {
            ProjectMBridge
                .endOfflineExport()
            offlinePcmCache
                ?.file
                ?.delete()
        }
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
        // Record actual playing items, including queue Next/Previous, not just taps.
        if (snapshot.isPlaying && snapshot.trackName != null) {
            val uri = controller.currentTrackUri()?.toString()
            if (uri != null && uri != lastRecordedMusicUri) {
                lastRecordedMusicUri = uri
                LocalMusicUserState(this).recordPlay(uri)
            }
        }

        dock?.visibility = if (snapshot.trackName == null) View.GONE else View.VISIBLE
        dockTitle?.text = snapshot.trackName ?: "Нічого не грає"
        dockSubtitle?.text = buildString {
            append(snapshot.trackArtist ?: "Локальний файл")
            if (snapshot.analysisActive) append(" · reactive ON")
        }
        dockPlay?.text = if (snapshot.isPlaying) "Ⅱ" else "▶"

        val ratio = if (snapshot.durationMs > 0L) {
            ((snapshot.positionMs.toDouble() / snapshot.durationMs) * 1000.0).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        dockProgress?.progress = ratio

        nowTitle?.text = snapshot.trackName ?: "FARIC PulseDeck"
        nowArtist?.text = if (snapshot.trackName == null) "Оберіть музику" else
            snapshot.trackArtist ?: "Невідомий виконавець"
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
        pulseDeckMainSkinView?.let { skin ->
            // V78: red heart when this song is in favourites (also changed from the library).
            skin.favoriteActive =
                controller.currentTrackUri()?.let { LocalMusicUserState(this).isFavorite(it.toString()) } == true
        }
        pulseDeckMainSkinView?.setPlaybackContent(
            title =
                snapshot.trackName
                    ?: "FARIC PulseDeck",
            artist =
                if (snapshot.trackName == null) {
                    "Оберіть музику"
                } else {
                    snapshot.trackArtist ?: "Невідомий виконавець"
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
            artist = snapshot.trackArtist,
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
                    screen == Screen.BOARD_TRANSFORM ||
                    (screen == Screen.THEME_PICKER && themeMenuOverlay != null) ||
                    (screen == Screen.EXPORT_LAB && exportLabOverlay != null) ||
                    ((screen == Screen.LIBRARY || screen == Screen.TRACKS) &&
                        mediaMenuOverlay != null)
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

    private fun projectMSurfaceVisible(): Boolean =
        layerVisible(
            PulseDeckLayerStack.Layer.VISUALIZER,
        ) &&
            (
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                ) ||
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "fg_center",
                    ) ||
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "fg_edge_fx",
                    )
                )

    private fun applyVisualizerRenderSlots() {
        val parentVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            )

        pulseDeckLayerStack
            ?.setLayerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
                projectMSurfaceVisible(),
            )

        pulseDeckLayerStack
            ?.setLayerVisible(
                PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                parentVisible &&
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "faric_reactive",
                    ),
            )
    }

    private fun updateProjectMRenderState() {
        val shouldRender =
            (
                screen == Screen.NOW_PLAYING ||
                    screen == Screen.BOARD_TRANSFORM ||
                    (screen == Screen.THEME_PICKER && themeMenuOverlay != null) ||
                    (screen == Screen.EXPORT_LAB && exportLabOverlay != null) ||
                    ((screen == Screen.LIBRARY || screen == Screen.TRACKS) &&
                        mediaMenuOverlay != null)
                ) &&
                (projectMSurfaceVisible() || projectMFocusOverlay != null) &&
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

    /** Library and Tracks menus have no ownership of projectM or the scene. */
    private fun hasRetainedMediaScene(): Boolean =
        persistentSceneRoot != null &&
            pulseDeckLayerStack != null &&
            (screen == Screen.NOW_PLAYING ||
                ((screen == Screen.LIBRARY || screen == Screen.TRACKS) &&
                    mediaMenuOverlay != null))

    private fun installMediaMenu(root: FrameLayout, retainScene: Boolean) {
        val host = persistentSceneRoot
        if (retainScene && host != null) {
            mediaMenuOverlay?.let { old ->
                (old.parent as? ViewGroup)?.removeView(old)
            }
            host.addView(
                root,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            mediaMenuOverlay = root
            Log.i(
                "FARIC-nav",
                "Media overlay retained sceneRoot=" + System.identityHashCode(host) +
                    " projectM=" + System.identityHashCode(projectMMainView) +
                    " stack=" + System.identityHashCode(pulseDeckLayerStack),
            )
            updateSceneOrchestratorState()
            updateProjectMRenderState()
        } else {
            setContentView(root)
        }
    }

    private fun showLibrary() {
        if (blockedByActiveExport("showLibrary")) return
        localMusicBrowser = null
        val retainScene = hasRetainedMediaScene()
        screen = Screen.LIBRARY
        if (!retainScene) {
            sceneOrchestrator.stop()
            clearScreenRefs()
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }

        if (!retainScene) {
            attachExportProjectMPreview(root)
        }
        applySafeArea(root, includeSystemBars = true)

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
        header.addView(iconButton("⌕") { showAllTracks(focusSearch = true) })
        header.addView(
            iconButton("⋮") {
                // V83: Export Lab from the library menu too.
                PulseDeckDialogs.show(
                    context = this,
                    title = "Меню PulseDeck",
                    actions =
                        listOf(
                            PulseDeckDialogs.Action(label = "Export Lab", subtitle = "Відео MP4 і кадри PNG", icon = "⇧", accent = true) {
                                showNowPlaying()
                                window.decorView.post { openExportLabWithProjectMCapture() }
                            },
                            PulseDeckDialogs.Action(label = "Закрити") {},
                        ),
                )
            },
        )

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
        modes.addView(modeChip("◉", "Scene Lab", "Візуальні сцени", false) {
            if (mediaMenuOverlay != null) showNowPlaying()
            showThemePicker()
        })
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
            label("Моя медіатека", 20f, Color.WHITE, true),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24)
                bottomMargin = dp(10)
            },
        )

        val categories = listOf(
            Triple("♫", "Усі треки", "Вся локальна музика"),
            Triple("▰", "Теки", "Папки пристрою"),
            Triple("◉", "Альбоми", "Релізи та трек-листи"),
            Triple("●", "Виконавці", "Усі виконавці"),
            Triple("✦", "Жанри", "Стилі музики"),
            Triple("20", "Роки", "Музика за роками"),
            Triple("♡", "Улюблені", "Твої позначки ♥"),
            Triple("↺", "Нещодавні", "Прослухані у FARIC"),
            Triple("▤", "Мої добірки", "Власні плейлисти"),
        )

        categories.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEachIndexed { index, item ->
                row.addView(
                    libraryCard(item.first, item.second, item.third) {
                        val mode = when (item.second) {
                            "Теки" -> LocalMusicCategory.FOLDERS
                            "Альбоми" -> LocalMusicCategory.ALBUMS
                            "Виконавці" -> LocalMusicCategory.ARTISTS
                            "Жанри" -> LocalMusicCategory.GENRES
                            "Роки" -> LocalMusicCategory.YEARS
                            "Улюблені" -> LocalMusicCategory.FAVORITES
                            "Нещодавні" -> LocalMusicCategory.RECENT
                            "Мої добірки" -> LocalMusicCategory.PLAYLISTS
                            else -> LocalMusicCategory.ALL
                        }
                        showAllTracks(category = mode)
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index == 0) marginEnd = dp(6) else marginStart = dp(6)
                        bottomMargin = dp(12)
                    },
                )
            }
            content.addView(row)
        }

        val summary = label(
            if (hasMusicLibraryPermission()) "Підрахунок локальної музики…" else "Відкрий «Усі треки» та дозволь доступ до музики",
            13f,
            COLOR_MUTED,
            false,
        )
        content.addView(summary)
        if (hasMusicLibraryPermission()) {
            thread(name = "faric-library-home-stats") {
                runCatching {
                    val tracks = LocalMusicLibrary.scan(applicationContext)
                    val albums = LocalMusicCatalog.groups(LocalMusicCategory.ALBUMS, tracks).size
                    val artists = LocalMusicCatalog.groups(LocalMusicCategory.ARTISTS, tracks).size
                    "${tracks.size} треків · $albums альбомів · $artists виконавців"
                }.onSuccess { countText ->
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && root.isAttachedToWindow) {
                            summary.text = countText
                        }
                    }
                }
            }
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

        installMediaMenu(root, retainScene)
        restorePendingScrollPositions()
        showMediaLibrarySystemBars()
        onPlaybackSnapshot(latestSnapshot)
    }

    private fun hasMusicLibraryPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun askMusicLibraryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestMusicLibraryPermission.launch(permission)
    }

    private fun showAllTracks(
        focusSearch: Boolean = false,
        category: LocalMusicCategory = LocalMusicCategory.ALL,
        selectedGroup: String? = null,
        restoredQuery: String = "",
    ) {
        if (blockedByActiveExport("showAllTracks")) return
        musicBrowseCategory = category
        musicBrowseGroup = selectedGroup
        musicBrowseQuery = restoredQuery
        val retainScene = hasRetainedMediaScene()
        screen = Screen.TRACKS
        if (!retainScene) {
            sceneOrchestrator.stop()
            clearScreenRefs()
        }

        val browser = LocalMusicBrowser(
            activity = this,
            canReadMusic = hasMusicLibraryPermission(),
            category = category,
            onBack = { showLibrary() },
            onRequestPermission = { askMusicLibraryPermission() },
            onPickFiles = { chooseTrack() },
            onSelect = { tracks, selectedIndex ->
                controller.loadQueue(
                    tracks.map { item ->
                        QueueTrack(item.uri, item.title, item.artist, item.album)
                    },
                    selectedIndex,
                )
                controller.play()
                showNowPlaying()
            },
            initialGroup = selectedGroup,
            initialQuery = restoredQuery,
            onStateChange = { group, query ->
                musicBrowseGroup = group
                musicBrowseQuery = query
            },
        )
        localMusicBrowser = browser
        val root = browser.create(
            focusSearch = focusSearch,
            footer = buildPulseDock(),
        )
        applySafeArea(root, includeSystemBars = true)
        installMediaMenu(root, retainScene)
        showMediaLibrarySystemBars()
        onPlaybackSnapshot(latestSnapshot)
    }

    private fun createLayer0ProjectMView(): ProjectMView? {
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
                    backgroundVisible =
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "projectm",
                        ),
                    foregroundCenterVisible =
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "fg_center",
                        ),
                    foregroundEdgeFxVisible =
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "fg_edge_fx",
                        ),
                    foregroundTuning =
                        projectMState.foregroundTuning(),
                    onTapNext = {},
                )
            } else {
                null
            }

        return projectM
    }

    private fun attachLayer0Visualizer(
        stack: PulseDeckLayerStack,
    ) {
        val projectM = createLayer0ProjectMView()
        projectMMainView = projectM

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

    private fun activeGfLayerTransforms(
        id: PlaybackThemeId,
    ): Map<BoardLayerId, BoardLayerTransform> =
        gfMixController.transforms(boardLayerTransformStore.loadAll(id))

    /**
     * Replaces only GF bitmaps. No GL, projectM, playback or HUD recreation.
     * On a theme switch/reset, a newer request invalidates older decode jobs.
     */
    private fun refreshGfMixArtwork(forceOriginal: Boolean = false) {
        val target = heroBoardView ?: return
        val theme = currentGraphicFigureThemeId()
        val mix = gfMixController.active()
        if (mix == null && !forceOriginal) return
        val request = ++gfMixLoadGeneration
        val cacheKey =
            theme.name + "|" + (mix?.instances?.joinToString(",") { it.asset.assetId } ?: "original")
        // Explicit changes (forceOriginal) always re-read the PNGs; only re-created screens
        // reuse what is already decoded.
        val cached = gfArtworkCache?.takeIf { !forceOriginal && it.first == cacheKey }?.second
        if (cached != null) {
            applyGfMixArtwork(target, theme, cached)
            return
        }
        thread(name = "faric-gf-mix-load") {
            val images = if (mix == null) {
                GraphicFigureCatalog.loadAssets(this, theme)
            } else {
                val refs = mix.instances.associate { it.asset.type to it.asset }
                GraphicFigureAssets(
                    frame = refs[GfArtType.FRAME]?.let { GfAssetLibrary.load(this, it) },
                    fx = refs[GfArtType.FX]?.let { GfAssetLibrary.load(this, it) },
                    creature = refs[GfArtType.CREATURE]?.let { GfAssetLibrary.load(this, it) },
                    wordmark = refs[GfArtType.WORDMARK]?.let { GfAssetLibrary.load(this, it) },
                )
            }
            runOnUiThread {
                if (isFinishing || isDestroyed ||
                    request != gfMixLoadGeneration ||
                    target !== heroBoardView ||
                    theme != currentGraphicFigureThemeId()
                ) return@runOnUiThread
                gfArtworkCache = cacheKey to images
                applyGfMixArtwork(target, theme, images)
            }
        }
    }

    private fun applyGfMixArtwork(
        target: HeroBoardView,
        theme: PlaybackThemeId,
        images: GraphicFigureAssets,
    ) {
        target.applyPreloadedFigureTheme(theme, images)
        target.setLayerTransforms(activeGfLayerTransforms(theme))
        HeroBoardView.ObjectId.entries.forEach { id ->
            val layer = BoardLayerId.valueOf(id.name)
            target.setObjectVisible(
                id,
                gfMixController.visible(
                    layer,
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                        id.name.lowercase(),
                    ),
                ),
            )
        }
    }

    private fun activateExternalPngHero(id: String) {
        val selected = ExternalPngHeroLibrary(this).find(id)
        if (selected == null) {
            toast("PNG недоступний. Обери папку повторно.")
            return
        }
        val theme = currentGraphicFigureThemeId()
        val ok = gfMixController.showStandalonePng(
            sourceId = selected.id,
            themeId = theme,
            baseline = boardLayerTransformStore.loadAll(theme),
            visible = { layer ->
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layer.name.lowercase(),
                )
            },
            onChanged = {
                setLayerVisible(PulseDeckLayerStack.Layer.GRAPHIC_FIGURES, true)
                refreshGfMixArtwork(forceOriginal = true)
            },
        )
        if (ok) {
            toast("PNG-герой: ${selected.title}")
            if (screen == Screen.THEME_PICKER) showNowPlaying()
        } else {
            toast("Не вдалося застосувати PNG-героя")
        }
    }

    private fun showExternalPngHeroes() {
        val library = ExternalPngHeroLibrary(this)
        val items = library.list().filter { HeroGalleryPolicy.visibleExternalHero(it.title) }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lateinit var dialog: android.app.Dialog
        list.addView(
            actionPill(text = "📁 Обрати або змінити папку героїв", accent = false) {
                dialog.dismiss()
                requestExternalPngFolder(GfArtType.CREATURE)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48),
            ).apply { bottomMargin = dp(9) },
        )
        val thumbs = mutableListOf<Pair<ExternalPngHeroLibrary.Hero, ImageView>>()
        items.forEachIndexed { index, hero ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(COLOR_PANEL)
            }
            row.addView(
                image,
                LinearLayout.LayoutParams(dp(78), dp(78)).apply {
                    marginEnd = dp(8)
                },
            )
            row.addView(
                actionPill(
                    text = "${index + 1}. ${hero.title}",
                    accent = gfMixController.overrides()[BoardLayerId.CREATURE]?.sourcePackId == hero.id,
                ) {
                    dialog.dismiss()
                    activateExternalPngHero(hero.id)
                },
                LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
                ),
            )
            list.addView(
                row, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(8) },
            )
            thumbs.add(hero to image)
        }
        if (items.isEmpty()) {
            list.addView(label(
                "Знайдені PNG з'являться тут із мініатюрами після вибору папки.",
                13f, COLOR_MUTED, false,
            ))
        }
        val scroll = ScrollView(this).apply { addView(list) }
        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.54f).toInt(),
        )
        dialog = PulseDeckDialogs.show(
            context = this,
            title = "Мої PNG-герої · ${items.size}",
            body = scroll,
            actions = listOf(PulseDeckDialogs.Action("Закрити") {}),
        )
        // Decode only 128px previews while this dialog remains on screen.
        thread(name = "faric-external-png-thumbnails") {
            thumbs.forEach { (hero, image) ->
                if (!dialog.isShowing) return@thread
                val bitmap = library.decode(hero.id, preferredDimension = 128)
                runOnUiThread {
                    if (dialog.isShowing && image.parent != null) {
                        image.setImageBitmap(bitmap)
                    } else bitmap?.recycle()
                }
            }
        }
    }

    private fun requestExternalPngFolder(type: GfArtType) {
        // Persist BEFORE Android's document picker takes over; callbacks will
        // restore the same layer role even after rotation / process recreation.
        val stored = pendingPngImportPrefs.edit()
            .putString("pending_type", type.name)
            .commit()
        if (!stored) {
            toast("Не вдалося зберегти тип шару. Імпорт не запущено.")
            return
        }
        selectExternalPngFolder.launch(
            ExternalPngHeroLibrary(this).activeFolder(type),
        )
    }

    /**
     * Direct, role-specific import actions shared by Board and Theme Picker.
     * These NEVER route FRAME/FX/WORDMARK to the Creature hero catalog.
     * The SAF callback uses the saved role and immediately opens that role's
     * source picker, without changing the other GF layers.
     */
    private fun addGfPngImportControls(parent: LinearLayout) {
        parent.addView(
            label("Додати свої PNG-шари", 14f, COLOR_ACCENT_CYAN, true),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16); bottomMargin = dp(10) },
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val choices = listOf(
            GfArtType.FRAME to "Рамки",
            GfArtType.WORDMARK to "Написи",
            GfArtType.FX to "Ефекти",
        )
        choices.forEachIndexed { index, (type, title) ->
            val button = actionPill("＋\n$title", accent = false) {
                requestExternalPngFolder(type)
            }.apply {
                textSize = 14f
                gravity = Gravity.CENTER
                minimumHeight = dp(100)
                minLines = 2
                maxLines = 2
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                // One prominent plus above one label; all tiles use identical
                // content and minimum-height measurement in the weighted row.
                text = android.text.SpannableString("＋\n$title").apply {
                    setSpan(
                        android.text.style.RelativeSizeSpan(1.55f), 0, 1,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
                contentDescription = "Вибрати PNG-папку: $title"
            }
            row.addView(
                button,
                LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
                ).apply { if (index < choices.lastIndex) marginEnd = dp(8) },
            )
        }
        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(12) },
        )
    }

    private fun showGfMixSourcePicker(type: GfArtType) {
        val theme = currentGraphicFigureThemeId()
        gfMixController.showSourcesFor(
            type = type,
            themeId = theme,
            baseline = boardLayerTransformStore.loadAll(theme),
            visible = { layerId ->
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layerId.name.lowercase(),
                )
            },
            onChanged = { refreshGfMixArtwork(forceOriginal = true) },
            onImportExternalPng = { requested -> requestExternalPngFolder(requested) },
        )
    }

    private fun showGfMixPicker() {
        val theme = currentGraphicFigureThemeId()
        gfMixController.show(
            themeId = theme,
            baseline = boardLayerTransformStore.loadAll(theme),
            visible = { layerId ->
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layerId.name.lowercase(),
                )
            },
            onChanged = {
                refreshGfMixArtwork(forceOriginal = true)
            },
            onImportExternalPng = { type ->
                requestExternalPngFolder(type)
            },
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
        readyAssets: GraphicFigureAssets? = null,
    ) {
        val safe =
            GraphicFigureCatalog
                .normalize(
                    themeId,
                )

        val wasMixed = gfMixController.active() != null
        gfMixController.clear()
        gfMixLoadGeneration++

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

        val supportedLayers =
            GraphicFigureCatalog
                .supportedLayers(
                    safe,
                )
        val hasVisibleObject =
            supportedLayers.any { layerId ->
                layerObjectVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layerId.name.lowercase(),
                )
            }

        if (!hasVisibleObject) {
            supportedLayers.forEach { layerId ->
                setLayerObjectVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layerId.name.lowercase(),
                    true,
                )
            }
        }

        heroBoardView
            ?.apply {
                if (readyAssets != null) {
                    applyPreloadedFigureTheme(safe, readyAssets)
                } else {
                    setFigureTheme(safe)
                }
                setGroupTransform(
                    boardTransformStore.load(
                        safe,
                    ),
                )
                // Parent settings are inherited by children, not applied
                // a second time as a whole-GF physical animation.
                setGroupReaction(BoardGroupReaction.neutral())
                setLayerReactionTunings(
                    boardLayerReactionTuningStore.loadAll(
                        safe, boardGroupReactionStore.load(safe),
                    ),
                )
                setLayerTransforms(
                    boardLayerTransformStore.loadAll(
                        safe,
                    ),
                )
                HeroBoardView.ObjectId.entries
                    .forEach { objectId ->
                        setObjectVisible(
                            objectId,
                            gfMixController.visible(
                                BoardLayerId.valueOf(objectId.name),
                                layerObjectVisible(
                                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                    objectId.name.lowercase(),
                                ),
                            ),
                        )
                    }
            }

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
        if (wasMixed) refreshGfMixArtwork(forceOriginal = true)
    }

    /**
     * Every implemented Playback Theme shares one live Scene Host. A theme
     * switches the visible central Hero only, never the projectM SurfaceView,
     * HUD, Player Activity, or playback state.
     */
    private fun applyLiveThemeVisibility() {
        val standalone = isStandaloneHeroTheme(selectedThemeId)
        heroBoardView?.visibility =
            if (standalone) View.GONE else View.VISIBLE
        heroThemeView?.visibility =
            if (standalone) View.VISIBLE else View.GONE
        if (standalone) {
            heroThemeView?.setTheme(selectedThemeId)
        }
        pulseDeckLayerStack?.setLayerVisible(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            if (standalone) true else
                layerVisible(PulseDeckLayerStack.Layer.GRAPHIC_FIGURES),
        )
    }

    private fun activateLivePlaybackTheme(
        theme: PlaybackThemeId,
        preloaded: GraphicFigureAssets? = null,
    ) {
        if (isLayeredBoardTheme(theme)) {
            setGraphicFigureThemeId(theme, preloaded)
        } else {
            selectedThemeId = theme
            themeStore.selectedThemeId = theme
        }
        applyLiveThemeVisibility()
    }

    private fun showNowPlaying() {
        if (blockedByActiveExport("showNowPlaying")) return
        // Menu-only return from Board: the underlying composition and native
        // projectM are the exact same instances as before the editor opened.
        if (screen == Screen.BOARD_TRANSFORM && boardMenuOverlay != null) {
            val panel = boardMenuOverlay
            (panel?.parent as? ViewGroup)?.removeView(panel)
            boardMenuOverlay = null
            screen = Screen.NOW_PLAYING
            pulseDeckLayerStack?.let { stack ->
                stack.setLayerVisible(
                    PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                    layerVisible(PulseDeckLayerStack.Layer.PULSEDECK_LOCKED),
                )
                stack.setLayerVisible(
                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                    layerVisible(PulseDeckLayerStack.Layer.GRAPHIC_FIGURES),
                )
            }
            applyLiveThemeVisibility()
            applyVisualizerRenderSlots()
            updateProjectMRenderState()
            updateSceneOrchestratorState()
            enableImmersiveFullscreen()
            Log.i("FARIC-nav", "Board overlay returned; GL/scene retained")
            return
        }
        if (
            screen == Screen.THEME_PICKER &&
            themeMenuOverlay != null &&
            (themeMenuOriginalTheme == selectedThemeId ||
                themeMenuLiveHeroSwitch)
        ) {
            (themeMenuOverlay?.parent as? ViewGroup)?.removeView(themeMenuOverlay)
            themeMenuOverlay = null
            themeMenuOriginalTheme = null
            themeMenuLiveHeroSwitch = false
            heroAssetLoadGeneration++
            screen = Screen.NOW_PLAYING
            updateProjectMRenderState()
            updateSceneOrchestratorState()
            enableImmersiveFullscreen()
            Log.i("FARIC-nav", "Themes browser closed; native GL retained")
            return
        }
        if (screen == Screen.EXPORT_LAB &&
            exportLabOverlay != null &&
            persistentSceneRoot != null &&
            projectMExportLiveView == null
        ) {
            // V3: leave Export Lab — remove only the overlay; scene, HUD and projectM
            // were never stopped.
            (exportLabOverlay?.parent as? ViewGroup)?.removeView(exportLabOverlay)
            exportLabOverlay = null
            screen = Screen.NOW_PLAYING
            activeVerticalScroll = null
            updateProjectMRenderState()
            updateSceneOrchestratorState()
            enableImmersiveFullscreen()
            onPlaybackSnapshot(latestSnapshot)
            Log.i("FARIC-nav", "Export Lab overlay closed; scene and projectM retained")
            return
        }
        if ((screen == Screen.LIBRARY || screen == Screen.TRACKS) &&
            mediaMenuOverlay != null &&
            persistentSceneRoot != null
        ) {
            val host = persistentSceneRoot
            (mediaMenuOverlay?.parent as? ViewGroup)?.removeView(mediaMenuOverlay)
            mediaMenuOverlay = null
            screen = Screen.NOW_PLAYING
            // Dock belongs to the dismissed menu, not to the persistent skin.
            miniPulseView = null
            dock = null
            dockTitle = null
            dockSubtitle = null
            dockPlay = null
            dockProgress = null
            activeVerticalScroll = null
            activeHorizontalScroll = null
            updateProjectMRenderState()
            updateSceneOrchestratorState()
            enableImmersiveFullscreen()
            onPlaybackSnapshot(latestSnapshot)
            Log.i(
                "FARIC-nav",
                "Media overlay returned; same sceneRoot=" + System.identityHashCode(host) +
                    " projectM=" + System.identityHashCode(projectMMainView) +
                    " stack=" + System.identityHashCode(pulseDeckLayerStack),
            )
            return
        }
        if (
            deferMainProjectMRebuild(
                Screen.NOW_PLAYING,
            )
        ) {
            return
        }
        screen = Screen.NOW_PLAYING
        clearScreenRefs()

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(COLOR_BG)
            }

        persistentSceneRoot = root

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
                deferArtwork = gfMixController.active() != null,
            ).also { view ->
                view.setGroupTransform(
                    boardTransformStore.load(
                        gfThemeId,
                    ),
                )
                view.setGroupReaction(BoardGroupReaction.neutral())
                view.setLayerReactionTunings(
                    boardLayerReactionTuningStore.loadAll(
                        gfThemeId, boardGroupReactionStore.load(gfThemeId),
                    ),
                )
                view.setLayerTransforms(
                    activeGfLayerTransforms(gfThemeId),
                )
                HeroBoardView.ObjectId.entries
                    .forEach { objectId ->
                        view.setObjectVisible(
                            objectId,
                            gfMixController.visible(
                                BoardLayerId.valueOf(objectId.name),
                                layerObjectVisible(
                                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                    objectId.name.lowercase(),
                                ),
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

        heroBoardView = boardView
        refreshGfMixArtwork()
        layerStack.setContent(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            boardView,
        )

        // A single central Hero slot supports all implemented themes.
        // The Canvas-based themes stay mounted and reuse this same scene
        // rather than calling showNowPlaying() for each selection.
        val standaloneHero = HeroThemeView(this).also { view ->
            view.setTheme(selectedThemeId)
            view.updateSignal(latestSignal)
            view.setPlaying(latestSnapshot.isPlaying)
            view.setMetadata(latestSnapshot.trackName, null)
        }
        heroThemeView = standaloneHero
        layerStack.slot(PulseDeckLayerStack.Layer.GRAPHIC_FIGURES).addView(
            standaloneHero,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
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
                                        "Системні панелі Android",
                                        "Журнал збоїв",
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

                                    6 ->
                                        showSystemBarsSettingsDialog()

                                    7 ->
                                        showDiagnosticsReport(
                                            title = "Журнал збоїв",
                                            report = CrashJournal.fullReport(this),
                                        )
                                }
                            }

                        // V78: ♡ really adds / removes the song from favourites.
                        "favorite" -> {
                            val uri = controller.currentTrackUri()
                            if (uri == null) {
                                toast("Спочатку обери пісню")
                            } else {
                                val added = LocalMusicUserState(this).toggleFavorite(uri.toString())
                                pulseDeckMainSkinView?.favoriteActive = added
                                toast(if (added) "❤ Додано в улюблені" else "Прибрано з улюблених")
                            }
                        }

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

                        "visualizer" ->
                            openProjectMFocusOverlay()

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
        applyLiveThemeVisibility()

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
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "fg_center",
                ),
                ProjectMStateStore(this)
                    .foregroundCenterVisible,
            )
            .putBoolean(
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "fg_edge_fx",
                ),
                ProjectMStateStore(this)
                    .foregroundEdgeFxVisible,
            )
            .putBoolean(
                KEY_VISUALIZER_OBJECTS_MIGRATED,
                true,
            )
            .apply()
    }

    private fun ensureProjectMComponentVisibilityMigration() {
        val prefs =
            layerPrefs()

        if (
            prefs.getBoolean(
                KEY_PROJECTM_COMPONENTS_MIGRATED,
                false,
            )
        ) {
            return
        }

        val state =
            ProjectMStateStore(
                this,
            )
        val backgroundKey =
            legacyLayerObjectPreferenceKey(
                PulseDeckLayerStack.Layer.VISUALIZER,
                "projectm",
            )
        val centerKey =
            legacyLayerObjectPreferenceKey(
                PulseDeckLayerStack.Layer.VISUALIZER,
                "fg_center",
            )
        val edgeKey =
            legacyLayerObjectPreferenceKey(
                PulseDeckLayerStack.Layer.VISUALIZER,
                "fg_edge_fx",
            )

        state.backgroundVisible =
            if (
                prefs.contains(
                    backgroundKey,
                )
            ) {
                prefs.getBoolean(
                    backgroundKey,
                    true,
                )
            } else {
                layerVisible(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                )
            }

        if (
            prefs.contains(
                centerKey,
            )
        ) {
            state.foregroundCenterVisible =
                prefs.getBoolean(
                    centerKey,
                    true,
                )
        }

        if (
            prefs.contains(
                edgeKey,
            )
        ) {
            state.foregroundEdgeFxVisible =
                prefs.getBoolean(
                    edgeKey,
                    true,
                )
        }

        prefs.edit()
            .putBoolean(
                KEY_PROJECTM_COMPONENTS_MIGRATED,
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
                layerPreferenceKey(
                    layer,
                ),
                visible,
            )
            .apply()

        if (
            layer ==
                PulseDeckLayerStack.Layer.VISUALIZER
        ) {
            applyVisualizerRenderSlots()
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
        if (
            layer ==
                PulseDeckLayerStack.Layer.VISUALIZER
        ) {
            val projectMState =
                ProjectMStateStore(
                    this,
                )

            when (objectId) {
                "projectm" ->
                    return projectMState
                        .backgroundVisible

                "fg_center" ->
                    return projectMState
                        .foregroundCenterVisible

                "fg_edge_fx" ->
                    return projectMState
                        .foregroundEdgeFxVisible
            }
        }

        val prefs =
            layerPrefs()
        val key =
            layerObjectPreferenceKey(
                layer,
                objectId,
            )

        if (layer == PulseDeckLayerStack.Layer.GRAPHIC_FIGURES) {
            val layerId = BoardLayerId.entries.firstOrNull {
                it.name.equals(objectId, ignoreCase = true)
            }
            if (layerId != null && gfMixController.active() != null) {
                return gfMixController.visible(
                    layerId,
                    prefs.getBoolean(key, defaultValue),
                )
            }
        }

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
        val layerId = if (layer == PulseDeckLayerStack.Layer.GRAPHIC_FIGURES) {
            BoardLayerId.entries.firstOrNull {
                it.name.equals(objectId, ignoreCase = true)
            }
        } else null
        if (layerId == null || !gfMixController.editVisibility(layerId, visible)) {
            layerPrefs()
                .edit()
                .putBoolean(layerObjectPreferenceKey(layer, objectId), visible)
                .apply()
        }

        when (layer) {
            PulseDeckLayerStack.Layer.VISUALIZER ->
                when (objectId) {
                    "projectm" -> {
                        val state =
                            ProjectMStateStore(
                                this,
                            )
                        state.backgroundVisible =
                            visible
                        projectMMainView
                            ?.setBackgroundVisible(
                                visible,
                            )
                        applyVisualizerRenderSlots()
                        updateProjectMRenderState()
                    }

                    "faric_reactive" -> {
                        applyVisualizerRenderSlots()
                        updateSceneOrchestratorState()
                    }

                    "fg_center",
                    "fg_edge_fx",
                    -> {
                        val state =
                            ProjectMStateStore(
                                this,
                            )
                        val centerVisible =
                            if (
                                objectId ==
                                    "fg_center"
                            ) {
                                visible
                            } else {
                                state.foregroundCenterVisible
                            }
                        val edgeVisible =
                            if (
                                objectId ==
                                    "fg_edge_fx"
                            ) {
                                visible
                            } else {
                                state.foregroundEdgeFxVisible
                            }

                        state.foregroundCenterVisible =
                            centerVisible
                        state.foregroundEdgeFxVisible =
                            edgeVisible

                        projectMMainView
                            ?.setForegroundVisibility(
                                centerVisible =
                                    centerVisible,
                                edgeFxVisible =
                                    edgeVisible,
                            )

                        applyVisualizerRenderSlots()
                        updateProjectMRenderState()
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
                    LayerMenuObject(
                        "fg_center",
                        "FG Center",
                    ),
                    LayerMenuObject(
                        "fg_edge_fx",
                        "FG Edge FX",
                    ),
                )

            PulseDeckLayerStack.Layer.FARIC_REACTIVE ->
                emptyList()

            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES -> {
                val gfThemeId =
                    currentGraphicFigureThemeId()
                val creatureLabel =
                    "Creature"

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
            .filter { layer ->
                layer !=
                    PulseDeckLayerStack.Layer.VISUALIZER
            }
            .forEach { layer ->
                stack.setLayerVisible(
                    layer,
                    layerVisible(
                        layer,
                    ),
                )
            }

        val parentVisible =
            layerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
            )

        stack.setLayerVisible(
            PulseDeckLayerStack.Layer.VISUALIZER,
            parentVisible &&
                (
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "projectm",
                    ) ||
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "fg_center",
                        ) ||
                        layerObjectVisible(
                            PulseDeckLayerStack.Layer.VISUALIZER,
                            "fg_edge_fx",
                        )
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

        // Locked PulseDeck HUD is never user-toggleable as a top-level layer.
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
                            "spinSpeed",
                            value.autoRotationDegreesPerSecond,
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

        val projectMState =
            ProjectMStateStore(
                this,
            )
        val projectMTuning =
            projectMState.foregroundTuning()

        projectMState
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
                            projectMState
                                .foregroundSample
                                .name,
                        )
                        put(
                            "autoSwitchSeconds",
                            projectMState
                                .autoSwitchSeconds,
                        )
                        put(
                            "tuning",
                            JSONObject().apply {
                                put(
                                    "centerScale",
                                    projectMTuning
                                        .centerScale,
                                )
                                put(
                                    "centerRotation",
                                    projectMTuning
                                        .centerRotationDegrees,
                                )
                                put(
                                    "centerSpinSpeed",
                                    projectMTuning
                                        .centerSpinDegreesPerSecond,
                                )
                                put(
                                    "centerOpacity",
                                    projectMTuning
                                        .centerOpacity,
                                )
                                put(
                                    "centerBass",
                                    projectMTuning
                                        .centerBassGain,
                                )
                                put(
                                    "centerMid",
                                    projectMTuning
                                        .centerMidGain,
                                )
                                put(
                                    "centerHigh",
                                    projectMTuning
                                        .centerHighGain,
                                )
                                put(
                                    "centerBeat",
                                    projectMTuning
                                        .centerBeatGain,
                                )
                                put(
                                    "edgeOpacity",
                                    projectMTuning
                                        .edgeOpacity,
                                )
                                put(
                                    "edgeBass",
                                    projectMTuning
                                        .edgeBassGain,
                                )
                                put(
                                    "edgeHigh",
                                    projectMTuning
                                        .edgeHighGain,
                                )
                                put(
                                    "edgeBeat",
                                    projectMTuning
                                        .edgeBeatGain,
                                )
                            },
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

        val layersForMigration =
            root.optJSONObject(
                "layers",
            )
        val objectsForMigration =
            root.optJSONObject(
                "objects",
            )
        val visualizerObjectsForMigration =
            objectsForMigration
                ?.optJSONObject(
                    PulseDeckLayerStack.Layer.VISUALIZER.name,
                )

        if (
            visualizerObjectsForMigration == null ||
            !visualizerObjectsForMigration.has(
                "projectm",
            )
        ) {
            layerEditor.putBoolean(
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "projectm",
                ),
                layersForMigration
                    ?.optBoolean(
                        PulseDeckLayerStack.Layer.VISUALIZER.name,
                        true,
                    )
                    ?: true,
            )
        }

        if (
            visualizerObjectsForMigration == null ||
            !visualizerObjectsForMigration.has(
                "faric_reactive",
            )
        ) {
            layerEditor.putBoolean(
                legacyLayerObjectPreferenceKey(
                    PulseDeckLayerStack.Layer.VISUALIZER,
                    "faric_reactive",
                ),
                layersForMigration
                    ?.optBoolean(
                        PulseDeckLayerStack.Layer.FARIC_REACTIVE.name,
                        true,
                    )
                    ?: true,
            )
        }

        if (
            visualizerObjectsForMigration == null ||
            !visualizerObjectsForMigration.has(
                "projectm",
            )
        ) {
            ProjectMStateStore(this)
                .backgroundVisible =
                layersForMigration
                    ?.optBoolean(
                        PulseDeckLayerStack.Layer.VISUALIZER.name,
                        true,
                    )
                    ?: true
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

        root.optJSONObject(
            "objects",
        )
            ?.optJSONObject(
                PulseDeckLayerStack.Layer.VISUALIZER.name,
            )
            ?.let { visualizerObjects ->
                val state =
                    ProjectMStateStore(
                        this,
                    )

                if (
                    visualizerObjects.has(
                        "projectm",
                    )
                ) {
                    state.backgroundVisible =
                        visualizerObjects.optBoolean(
                            "projectm",
                            true,
                        )
                }

                if (
                    visualizerObjects.has(
                        "fg_center",
                    )
                ) {
                    state.foregroundCenterVisible =
                        visualizerObjects.optBoolean(
                            "fg_center",
                            true,
                        )
                }

                if (
                    visualizerObjects.has(
                        "fg_edge_fx",
                    )
                ) {
                    state.foregroundEdgeFxVisible =
                        visualizerObjects.optBoolean(
                            "fg_edge_fx",
                            true,
                        )
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
                boardLayerReactionTuningStore.applyParentToAll(
                    gfTheme, boardGroupReactionStore.load(gfTheme),
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
                                            autoRotationDegreesPerSecond =
                                                item.optDouble(
                                                    "spinSpeed",
                                                    current.autoRotationDegreesPerSecond
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

                val autoSwitchSeconds =
                    projectM.optInt(
                        "autoSwitchSeconds",
                        state.autoSwitchSeconds,
                    )
                if (
                    autoSwitchSeconds in
                        com.saney.musicvisualizer
                            .projectm
                            .ProjectMStateStore
                            .AUTO_SWITCH_OPTIONS
                ) {
                    state.autoSwitchSeconds =
                        autoSwitchSeconds
                }

                projectM
                    .optJSONObject(
                        "tuning",
                    )
                    ?.let { tuning ->
                        val current =
                            state.foregroundTuning()

                        state.saveForegroundTuning(
                            com.saney.musicvisualizer
                                .projectm
                                .ProjectMForegroundTuning(
                                    centerScale =
                                        tuning.optDouble(
                                            "centerScale",
                                            current.centerScale
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerRotationDegrees =
                                        tuning.optDouble(
                                            "centerRotation",
                                            current.centerRotationDegrees
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerSpinDegreesPerSecond =
                                        tuning.optDouble(
                                            "centerSpinSpeed",
                                            current.centerSpinDegreesPerSecond
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerOpacity =
                                        tuning.optDouble(
                                            "centerOpacity",
                                            current.centerOpacity
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerBassGain =
                                        tuning.optDouble(
                                            "centerBass",
                                            current.centerBassGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerMidGain =
                                        tuning.optDouble(
                                            "centerMid",
                                            current.centerMidGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerHighGain =
                                        tuning.optDouble(
                                            "centerHigh",
                                            current.centerHighGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    centerBeatGain =
                                        tuning.optDouble(
                                            "centerBeat",
                                            current.centerBeatGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    edgeOpacity =
                                        tuning.optDouble(
                                            "edgeOpacity",
                                            current.edgeOpacity
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    edgeBassGain =
                                        tuning.optDouble(
                                            "edgeBass",
                                            current.edgeBassGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    edgeHighGain =
                                        tuning.optDouble(
                                            "edgeHigh",
                                            current.edgeHighGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                    edgeBeatGain =
                                        tuning.optDouble(
                                            "edgeBeat",
                                            current.edgeBeatGain
                                                .toDouble(),
                                        )
                                            .toFloat(),
                                ),
                        )
                    }
            }

        projectMExportSnapshot =
            null
        showNowPlaying()
    }

    /**
     * The foreground projectM controls are an overlay on the current player
     * scene, not another Activity. Never destroy/recreate native projectM
     * on normal enter/exit; temporarily mask unrelated render layers only.
     */
    private fun openProjectMFocusOverlay() {
        if (projectMFocusOverlay != null) return
        val host = persistentSceneRoot ?: return
        val stack = pulseDeckLayerStack ?: return
        if (screen != Screen.NOW_PLAYING) return
        if (projectMMainView == null) {
            // First-time projectM library installation still uses its legacy
            // setup Activity. Normal installed playback never launches it.
            openProjectMVisualizer.launch(
                Intent(
                    this,
                    com.saney.musicvisualizer.projectm.ProjectMActivity::class.java,
                ),
            )
            return
        }

        val previousVisibility =
            PulseDeckLayerStack.Layer.entries
                .associateWith { stack.isLayerVisible(it) }
        projectMFocusSavedVisibility = previousVisibility

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        projectMFocusOverlay = overlay
        PulseDeckLayerStack.Layer.entries.forEach { layer ->
            stack.setLayerVisible(
                layer,
                layer == PulseDeckLayerStack.Layer.VISUALIZER,
            )
        }
        // The exact underlying GLSurfaceView and native renderer survive.
        host.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        // ProjectM's original screen showed both Android system bars. Honor
        // the same behavior inside the persistent player Activity, with
        // inset padding limited to the controls (not the underlying GL view).
        ViewCompat.setOnApplyWindowInsetsListener(overlay) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        showProjectMSystemBars()
        updateProjectMRenderState()
        Log.i("FARIC-nav", "projectM overlay opened; live GL retained")

        // Restore the exact legacy four-row control hierarchy and back/status
        // header. The native projectM renderer stays owned by MainActivity.
        fun legacyControl(
            title: String,
            onClick: () -> Unit,
        ): TextView =
            TextView(this).apply {
                text = title
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                includeFontPadding = false
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setBackgroundColor(Color.argb(150, 15, 22, 31))
                setOnClickListener { onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    0, dp(48), 1f,
                ).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
            }

        val status = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(145, 0, 0, 0))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
        }
        projectMFocusStatus = status

        val back = ImageView(this).apply {
            contentDescription = "Назад"
            isClickable = true
            isFocusable = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(2), dp(2), dp(2), dp(2))
            setImageBitmap(
                runCatching {
                    assets.open("pulsedeck_hud/utility/back.png")
                        .use { input -> BitmapFactory.decodeStream(input) }
                }.getOrNull(),
            )
            setOnClickListener { closeProjectMFocusOverlay() }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), 0)
        }
        // V84 (faric 20.10 05:36 / 06:03): the same size as the centre's «‹» (≈ 10 % of the
        // width) AND in the same place — centred on where the centre screen draws its «back».
        val backSize = (resources.displayMetrics.widthPixels * 0.10f).toInt().coerceAtLeast(dp(40))
        val skin = pulseDeckMainSkinView
        val centreBack = skin?.objectRect("back")
        if (skin != null && centreBack != null) {
            // 20.10 06:25 (video): the same picture as the centre draws, in exactly the
            // rectangle it uses (10.5 % of the width, no padding) — was 10 % minus padding.
            overlay.clipToPadding = false
            back.setPadding(0, 0, 0, 0)
            back.scaleType = ImageView.ScaleType.FIT_XY
            overlay.addView(
                back,
                FrameLayout.LayoutParams(centreBack.width().toInt(), centreBack.height().toInt()),
            )
            back.visibility = View.INVISIBLE
            overlay.post {
                val overlayAt = IntArray(2).also(overlay::getLocationInWindow)
                val skinAt = IntArray(2).also(skin::getLocationInWindow)
                back.x = skinAt[0] - overlayAt[0] + centreBack.left
                back.y = skinAt[1] - overlayAt[1] + centreBack.top
                back.visibility = View.VISIBLE
                back.bringToFront()
            }
            // The status text starts right of the button.
            top.setPadding(centreBack.right.toInt() + dp(6), dp(4), dp(8), 0)
        } else {
            top.addView(back, LinearLayout.LayoutParams(backSize, backSize))
        }
        top.addView(
            status,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
            ).apply { marginStart = dp(6) },
        )
        overlay.addView(
            top,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            ),
        )

        fun controlRow(
            topPadding: Int,
            bottomPadding: Int,
            vararg buttons: TextView,
        ): LinearLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(topPadding), dp(8), dp(bottomPadding))
                setBackgroundColor(Color.argb(155, 0, 0, 0))
                buttons.forEach { addView(it) }
            }

        val modes = controlRow(
            6, 4,
            // V44: ТОП / ВСІ only choose the list; they no longer switch AUTO on by themselves.
            legacyControl("ТОП") {
                loadProjectMFocusPresets(
                    com.saney.musicvisualizer.projectm.ProjectMBackgroundMode.TOP,
                )
                scheduleProjectMFocusAuto()
            },
            legacyControl("ВСІ") {
                loadProjectMFocusPresets(
                    com.saney.musicvisualizer.projectm.ProjectMBackgroundMode.ALL,
                )
                scheduleProjectMFocusAuto()
            },
            legacyControl("NEXT") { advanceProjectMFocusPreset(manual = true) },
            legacyControl("FG") {
                val state = ProjectMStateStore(this)
                state.foregroundSample = state.foregroundSample.next()
                projectMMainView?.setForegroundSample(state.foregroundSample)
                updateProjectMFocusStatus()
            },
        )
        projectMFocusBackgroundControl = legacyControl("") {
            val state = ProjectMStateStore(this)
            state.backgroundVisible = !state.backgroundVisible
            projectMMainView?.setBackgroundVisible(state.backgroundVisible)
            updateProjectMFocusStatus()
        }
        projectMFocusCenterControl = legacyControl("") {
            val state = ProjectMStateStore(this)
            state.foregroundCenterVisible = !state.foregroundCenterVisible
            projectMMainView?.setForegroundVisibility(
                state.foregroundCenterVisible,
                state.foregroundEdgeFxVisible,
            )
            updateProjectMFocusStatus()
        }
        projectMFocusEdgeControl = legacyControl("") {
            val state = ProjectMStateStore(this)
            state.foregroundEdgeFxVisible = !state.foregroundEdgeFxVisible
            projectMMainView?.setForegroundVisibility(
                state.foregroundCenterVisible,
                state.foregroundEdgeFxVisible,
            )
            updateProjectMFocusStatus()
        }
        val foreground = controlRow(
            2, 4,
            requireNotNull(projectMFocusBackgroundControl),
            requireNotNull(projectMFocusCenterControl),
            requireNotNull(projectMFocusEdgeControl),
        )
        projectMFocusAutoControl = legacyControl("") {
            // V44 (faric 19.10 15:57: «AUTO не вимикається»): AUTO OFF → 5 с → 10 с → 15 с → AUTO OFF.
            val state = ProjectMStateStore(this)
            val options = intArrayOf(5, 10, 15)
            val current = options.indexOf(state.autoSwitchSeconds)
            when {
                !state.autoEnabled -> {
                    state.autoEnabled = true
                    state.autoSwitchSeconds = options.first()
                }
                current == options.lastIndex || current < 0 -> state.autoEnabled = false
                else -> state.autoSwitchSeconds = options[current + 1]
            }
            scheduleProjectMFocusAuto()
            updateProjectMFocusStatus()
        }
        val settings = controlRow(
            2, 4,
            requireNotNull(projectMFocusAutoControl),
            legacyControl("⚙ FG") { showProjectMSettingsPanel() },
            // V83: Export Lab is reachable from here even when the bottom menu is hidden.
            legacyControl("⇧ EXPORT") {
                closeProjectMFocusOverlay()
                openExportLabWithProjectMCapture()
            },
        )
        val ratings = controlRow(
            2, 10,
            legacyControl("👍") {
                rateProjectMFocusPreset(ProjectMPresetRating.UP)
            },
            legacyControl("👎") {
                rateProjectMFocusPreset(ProjectMPresetRating.DOWN)
            },
            legacyControl("−") {
                rateProjectMFocusPreset(ProjectMPresetRating.HIDDEN)
            },
        )
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(modes)
            addView(foreground)
            addView(settings)
            addView(ratings)
        }
        overlay.addView(
            bottom,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )
        updateProjectMFocusStatus()
        loadProjectMFocusPresets(ProjectMStateStore(this).backgroundMode)
        scheduleProjectMFocusAuto()
    }

    private fun updateProjectMFocusStatus() {
        if (projectMFocusOverlay == null) return
        val state = ProjectMStateStore(this)
        val current = state.lastPresetFileOrNull()
        val rating = current?.let { ProjectMPresetRatingsStore(this).ratingFor(it) }
            ?: ProjectMPresetRating.NONE
        val ratingIcon = when (rating) {
            ProjectMPresetRating.UP -> "👍"
            ProjectMPresetRating.DOWN -> "👎"
            ProjectMPresetRating.HIDDEN -> "−"
            ProjectMPresetRating.NONE -> "○"
        }
        val auto = if (state.autoEnabled) {
            "AUTO ${state.autoSwitchSeconds}s"
        } else {
            "MANUAL"
        }
        val onOff = { active: Boolean -> if (active) "ON" else "OFF" }
        projectMFocusStatus?.text =
            "$auto · ${state.backgroundMode.name} ${projectMFocusPresets.size} · " +
                "$ratingIcon ${current?.nameWithoutExtension?.take(28) ?: "—"} · " +
                "FG ${state.foregroundSample.label} · " +
                "BG ${onOff(state.backgroundVisible)} · " +
                "CENTER ${onOff(state.foregroundCenterVisible)} · " +
                "EDGE ${onOff(state.foregroundEdgeFxVisible)}"
        projectMFocusAutoControl?.text =
            if (state.autoEnabled) "AUTO ${state.autoSwitchSeconds}s" else "AUTO OFF"
        projectMFocusBackgroundControl?.text =
            (if (state.backgroundVisible) "● " else "○ ") + "BG"
        projectMFocusCenterControl?.text =
            (if (state.foregroundCenterVisible) "● " else "○ ") + "CENTER"
        projectMFocusEdgeControl?.text =
            (if (state.foregroundEdgeFxVisible) "● " else "○ ") + "EDGE FX"
    }

    private fun rateProjectMFocusPreset(rating: ProjectMPresetRating) {
        val state = ProjectMStateStore(this)
        val current = state.lastPresetFileOrNull() ?: return
        ProjectMPresetRatingsStore(this).setRating(current, rating)
        if (rating == ProjectMPresetRating.HIDDEN) {
            projectMFocusPresets =
                projectMFocusPresets.filterNot { it.absolutePath == current.absolutePath }
            advanceProjectMFocusPreset(manual = false)
        }
        updateProjectMFocusStatus()
    }

    private fun loadProjectMFocusPresets(
        mode: com.saney.musicvisualizer.projectm.ProjectMBackgroundMode,
    ) {
        val state = ProjectMStateStore(this)
        state.backgroundMode = mode
        val generation = ++projectMFocusCatalogGeneration
        thread(name = "projectm-focus-catalog") {
            val pool = runCatching {
                when (mode) {
                    com.saney.musicvisualizer.projectm.ProjectMBackgroundMode.TOP ->
                        ProjectMLibraryManager.fastPresetFiles(this)
                    com.saney.musicvisualizer.projectm.ProjectMBackgroundMode.ALL ->
                        ProjectMLibraryManager.allPresetFiles(this)
                }
            }.getOrDefault(emptyList())
            runOnUiThread {
                if (projectMFocusOverlay == null ||
                    generation != projectMFocusCatalogGeneration
                ) return@runOnUiThread
                val ratings = ProjectMPresetRatingsStore(this)
                projectMFocusPresets = pool.filter {
                    it.isFile &&
                        ratings.ratingFor(it) != ProjectMPresetRating.HIDDEN
                }
                if (projectMFocusPresets.isEmpty()) {
                    toast("Для ${mode.name} немає доступних preset")
                }
                updateProjectMFocusStatus()
            }
        }
    }

    private fun advanceProjectMFocusPreset(manual: Boolean) {
        val state = ProjectMStateStore(this)
        if (manual) {
            state.autoEnabled = false
            projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        }
        val pool = projectMFocusPresets
        if (pool.isEmpty()) {
            updateProjectMFocusStatus()
            return
        }
        val current = state.lastPresetPath
        val index = pool.indexOfFirst { it.absolutePath == current }
        val next = pool[(index + 1) % pool.size]
        if (next.absolutePath != current) {
            state.lastPresetPath = next.absolutePath
            projectMMainView?.loadPreset(next, smoothTransition = false)
        }
        updateProjectMFocusStatus()
    }

    private fun scheduleProjectMFocusAuto() {
        projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        if (projectMFocusOverlay == null) return
        val state = ProjectMStateStore(this)
        if (state.autoEnabled) {
            projectMFocusHandler.postDelayed(
                projectMFocusAutoTick,
                state.autoSwitchSeconds * 1_000L,
            )
        }
    }

    private fun closeProjectMFocusOverlay() {
        val overlay = projectMFocusOverlay ?: return
        projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        projectMFocusCatalogGeneration++
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        projectMFocusOverlay = null
        projectMFocusStatus = null
        projectMFocusAutoControl = null
        projectMFocusBackgroundControl = null
        projectMFocusCenterControl = null
        projectMFocusEdgeControl = null
        val saved = projectMFocusSavedVisibility
        projectMFocusSavedVisibility = null
        projectMFocusPresets = emptyList()
        val stack = pulseDeckLayerStack
        if (saved != null && stack != null) {
            saved.forEach { (layer, visible) ->
                stack.setLayerVisible(layer, visible)
            }
        }
        updateProjectMRenderState()
        enableImmersiveFullscreen()
        Log.i("FARIC-nav", "projectM overlay closed; live GL retained")
    }

    private fun showProjectMSettingsPanel() {
        val state =
            ProjectMStateStore(
                this,
            )

        ProjectMSettingsPanel.show(
            context = this,
            stateStore = state,
            initialSection =
                ProjectMSettingsPanel
                    .Section
                    .CENTER,
            onAutoChanged = { _, _ ->
                scheduleProjectMFocusAuto()
                updateProjectMFocusStatus()
            },
            onTuningChanged = { tuning ->
                projectMMainView
                    ?.setForegroundTuning(
                        tuning,
                    )
                projectMExportSnapshot =
                    null
            },
        )
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
                            setOnClickListener { buttonView ->
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
                                    dismissOnSelect =
                                        true,
                                ) { which ->
                                    val selectedId =
                                        ids[which]
                                    val selectedTitle =
                                        GraphicFigureCatalog
                                            .title(
                                                selectedId,
                                            )

                                    setGraphicFigureThemeId(
                                        selectedId,
                                    )

                                    (
                                        buttonView as?
                                            TextView
                                        )
                                        ?.text =
                                        selectedTitle

                                    toast(
                                        selectedTitle +
                                            " · застосовано",
                                    )
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
                } else if (
                    layer ==
                        PulseDeckLayerStack.Layer.VISUALIZER
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
                                "⚙"
                            textSize =
                                20f
                            gravity =
                                Gravity.CENTER
                            contentDescription =
                                "Налаштувати projectM / FG"
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
                                // Preserve the Layers parent dialog under
                                // the movable child editor. Closing the
                                // editor returns to this same Layers panel.
                                showProjectMSettingsPanel()
                            }
                        },
                        LinearLayout.LayoutParams(
                            dp(42),
                            dp(42),
                        ).apply {
                            marginStart =
                                dp(6)
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
        if (blockedByActiveExport("showBoardTransform")) return
        val retainedHost =
            persistentSceneRoot?.takeIf {
                pulseDeckLayerStack != null &&
                    (screen == Screen.NOW_PLAYING ||
                        (screen == Screen.BOARD_TRANSFORM && boardMenuOverlay != null))
            }
        if (
            retainedHost == null &&
            deferMainProjectMRebuild(Screen.BOARD_TRANSFORM)
        ) {
            return
        }
        val gfThemeId =
            currentGraphicFigureThemeId()

        screen = Screen.BOARD_TRANSFORM
        if (retainedHost == null) {
            sceneOrchestrator.stop()
            clearScreenRefs()
        } else {
            boardMenuOverlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
            boardMenuOverlay = null
        }

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
                activeGfLayerTransforms(gfThemeId)[layerId] ?: BoardLayerTransform.default()
            }

        val root =
            FrameLayout(this).apply {
                setBackgroundColor(
                    if (retainedHost != null) Color.TRANSPARENT else COLOR_BG,
                )
            }

        // Board edits the actual live scene when entered from the player.
        // No second PulseDeckLayerStack, native bridge, or SurfaceView exists.
        val previewStack =
            if (retainedHost != null) {
                requireNotNull(pulseDeckLayerStack)
            } else {
                PulseDeckLayerStack(this).also { stack ->
                    root.addView(
                        stack,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    attachLayer0Visualizer(stack)
                    stack.setContent(
                        PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
                        OverVisualizationView(this),
                    )
                    val previewEqualizer =
                        BigEqualizerView(this).also { view ->
                            view.updateSignal(latestSignal)
                            view.setPlaying(latestSnapshot.isPlaying)
                        }
                    bigEqualizerView = previewEqualizer
                    stack.setContent(
                        PulseDeckLayerStack.Layer.BIG_EQUALIZER,
                        previewEqualizer,
                    )
                }
            }

        if (retainedHost == null) {
            previewStack.setLayerVisible(
                PulseDeckLayerStack.Layer.VISUALIZER,
                layerVisible(PulseDeckLayerStack.Layer.VISUALIZER),
            )
            previewStack.setLayerVisible(
                PulseDeckLayerStack.Layer.FARIC_REACTIVE,
                layerVisible(PulseDeckLayerStack.Layer.FARIC_REACTIVE),
            )
            previewStack.setLayerVisible(
                PulseDeckLayerStack.Layer.OVER_VISUALIZATION,
                layerVisible(PulseDeckLayerStack.Layer.OVER_VISUALIZATION),
            )
            previewStack.setLayerVisible(
                PulseDeckLayerStack.Layer.BIG_EQUALIZER,
                layerVisible(PulseDeckLayerStack.Layer.BIG_EQUALIZER),
            )
        }

        val boardView =
            if (retainedHost != null) {
                requireNotNull(heroBoardView)
            } else {
                HeroBoardView(this, gfThemeId, deferArtwork = gfMixController.active() != null)
            }
        boardView.setGroupTransform(transform)
        boardView.setGroupReaction(BoardGroupReaction.neutral())
        boardView.setLayerReactionTunings(
            boardLayerReactionTuningStore.loadAll(gfThemeId, groupReaction),
        )
        boardView.setLayerTransforms(activeGfLayerTransforms(gfThemeId))
        HeroBoardView.ObjectId.entries.forEach { objectId ->
            boardView.setObjectVisible(
                objectId,
                            gfMixController.visible(
                                BoardLayerId.valueOf(objectId.name),
                                layerObjectVisible(
                                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                    objectId.name.lowercase(),
                                ),
                            ),
            )
        }
        boardView.setPlaying(latestSnapshot.isPlaying)
        boardView.updateSignal(latestSignal)
        heroBoardView = boardView
        refreshGfMixArtwork()

        if (retainedHost == null) {
            previewStack.setContent(
                PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                boardView,
            )
        }
        previewStack.setLayerVisible(
            PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
            true,
        )
        if (retainedHost != null) {
            // Board is always an editor of the actual GF artwork, even when a
            // standalone theme happens to be currently selected.
            heroBoardView?.visibility = View.VISIBLE
            heroThemeView?.visibility = View.GONE
            // The HUD belongs to Player UI, not to the Board editor.
            previewStack.setLayerVisible(
                PulseDeckLayerStack.Layer.PULSEDECK_LOCKED,
                false,
            )
        }

        fun layerTitle(
            layerId: BoardLayerId?,
        ): String =
            when (layerId) {
                null -> "Усе GF"
                BoardLayerId.BACKGROUND -> "Background / Glow"
                BoardLayerId.FRAME -> "Frame"
                BoardLayerId.FX -> "FX"
                // The selected Creature can be a scorpion, griffin or
                // any external PNG regardless of the backing GF theme ID.
                BoardLayerId.CREATURE -> "Герой"
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
        panelContent.addView(
            actionPill(
                text = if (gfMixController.active() != null)
                    "◆ GF Мікс · активний" else "◆ GF Мікс · обрати свої PNG",
                accent = gfMixController.active() != null,
            ) { showGfMixPicker() }.apply { minimumHeight = dp(52) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(10); bottomMargin = dp(10) },
        )
        panelContent.addView(
            actionPill(
                text = "📁 Зовнішні PNG-герої",
                accent = false,
            ) { showExternalPngHeroes() }.apply { minimumHeight = dp(52) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(4) },
        )

        // Import entry points must be visible without entering the hero-only chooser.
        addGfPngImportControls(panelContent)

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
                            "Фон",
                        BoardLayerId.FRAME to
                            "Рамка",
                        BoardLayerId.CREATURE to
                            GraphicFigureCatalog
                                .creatureLabel(
                                    gfThemeId,
                                ),
                        BoardLayerId.WORDMARK to
                            "Напис",
                        BoardLayerId.FX to
                            "Ефект",
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
                }.apply {
                    // V71 (faric 19.10 23:26: «Fra… · Shar…»): the chip is as wide as its
                    // name — never cut; the row scrolls sideways when there are many.
                    setSingleLine(true)
                    ellipsize = null
                    includeFontPadding = false
                    minimumWidth = dp(72)
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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

            // Editing ANY parent slider replaces all three values for EVERY
            // child. The live layer view updates immediately and the exported
            // video uses those same persisted values.
            boardLayerReactionTuningStore.applyParentToAll(gfThemeId, groupReaction)
            boardView.setGroupReaction(BoardGroupReaction.neutral())
            boardView.setLayerReactionTunings(
                boardLayerReactionTuningStore.loadAll(gfThemeId, groupReaction),
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

            if (!gfMixController.editTransform(layerId, safe)) {
                boardLayerTransformStore.save(gfThemeId, layerId, safe)
            }

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
                    val resetParent = boardGroupReactionStore.reset(gfThemeId)
                    boardLayerReactionTuningStore.resetAll(gfThemeId, resetParent)
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

            if (layerId == BoardLayerId.FRAME) {
                panelContent.addView(
                    label(
                        "Автообертання Frame · навколо власної осі; 0°/с — вимкнено",
                        11f,
                        COLOR_MUTED,
                        false,
                    ),
                )
                addSlider(
                    title = "Автообертання Frame",
                    max = 360,
                    initial =
                        (layerValue.autoRotationDegreesPerSecond + 180f).toInt(),
                    valueText = {
                        "${it - 180}°/с"
                    },
                ) { progress ->
                    layerValue =
                        layerValue.copy(
                            autoRotationDegreesPerSecond =
                                (progress - 180).toFloat(),
                        )
                    persistLayerTransform(
                        layerId,
                        layerValue,
                    )
                }
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


            // Values are absolute for this object; no additive parent motion.
            // An untouched layer inherits the current all-GF control values.
            var tuning = boardLayerReactionTuningStore.load(gfThemeId, layerId, groupReaction)
            fun persistLayerReaction(value: BoardLayerReactionTuning) {
                tuning = value.sanitized()
                boardLayerReactionTuningStore.save(gfThemeId, layerId, tuning)
                boardView.setLayerReactionTuning(layerId, tuning)
            }
            panelContent.addView(
                label("Реакція шару на музику · " + layerTitle(layerId),
                    14f, COLOR_ACCENT_CYAN, true),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(12); bottomMargin = dp(4) },
            )
            addSlider(
                title = "Плавний поворот шару",
                max = 60,
                initial = (tuning.rotationSwayDegrees * 10f).toInt(),
                valueText = { String.format(Locale.US, "%.1f°", it / 10f) },
            ) { n -> persistLayerReaction(tuning.copy(rotationSwayDegrees = n / 10f)) }
            addSlider(
                title = "Stereo L/R шару",
                max = 100,
                initial = (tuning.stereoShiftFraction * 1000f).toInt(),
                valueText = { String.format(Locale.US, "%.1f%%", it / 10f) },
            ) { n -> persistLayerReaction(tuning.copy(stereoShiftFraction = n / 1000f)) }
            addSlider(
                title = "Bass ↑↓ шару",
                max = 60,
                initial = (tuning.bassFloatFraction * 1000f).toInt(),
                valueText = { String.format(Locale.US, "%.1f%%", it / 10f) },
            ) { n -> persistLayerReaction(tuning.copy(bassFloatFraction = n / 1000f)) }

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
                    "Окремі значення шару. Зміна «Усе GF» встановить нові значення для всіх шарів. Нуль вимикає відповідну реакцію.",
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
                    persistLayerTransform(layerId, BoardLayerTransform.default())
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

        if (retainedHost != null) {
            retainedHost.addView(
                root,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            boardMenuOverlay = root
            Log.i("FARIC-nav", "Board editor opened over retained scene")
        } else {
            setContentView(root)
        }
        updateProjectMRenderState()
        updateSceneOrchestratorState()
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun openExportLabWithProjectMCapture() {
        // V3: with the scene kept under Export Lab there is nothing to freeze —
        // open at once; a projectM frame is captured only when a PNG is saved.
        if (hasRetainedExportScene()) {
            showExportLab()
            return
        }
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
        val projectMSurfaceVisible =
            projectMSurfaceVisible()

        if (!projectMSurfaceVisible) {
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
                backgroundVisible =
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "projectm",
                    ),
                foregroundCenterVisible =
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "fg_center",
                    ),
                foregroundEdgeFxVisible =
                    layerObjectVisible(
                        PulseDeckLayerStack.Layer.VISUALIZER,
                        "fg_edge_fx",
                    ),
                foregroundTuning =
                    state.foregroundTuning(),
                onTapNext = {},
                manualFrameMode = true,
                manualRenderWidth =
                    exportAspectRatio.width,
                manualRenderHeight =
                    exportAspectRatio.height,
            )

        projectMExportLiveView =
            view
        // V3: always at the bottom of the root, so in the Export Lab overlay it sits
        // under the buttons (index 0 / 1; the classic screen's root is still empty here).
        root.addView(
            view,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        root.addView(
            View(this).apply {
                tag = EXPORT_PREVIEW_DIM_TAG
                setBackgroundColor(
                    Color.argb(
                        218,
                        0,
                        0,
                        0,
                    ),
                )
            },
            1,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun exportPrefs() =
        getSharedPreferences("faric_export_options_v1", android.content.Context.MODE_PRIVATE)

    /** V38: «Кнопки у відео» — the PulseDeck HUD drawn into the MP4 or not. */
    private fun hudInVideoEnabled(): Boolean =
        exportPrefs().getBoolean(KEY_EXPORT_HUD_IN_VIDEO, true)

    /** V3: Export Lab can sit on top of the running PulseDeck scene. */
    private fun hasRetainedExportScene(): Boolean =
        persistentSceneRoot != null &&
            pulseDeckLayerStack != null &&
            (screen == Screen.NOW_PLAYING ||
                (screen == Screen.EXPORT_LAB && exportLabOverlay != null))

    private fun showExportLab() {
        if (blockedByActiveExport("showExportLab")) return
        val retainScene = hasRetainedExportScene()
        screen = Screen.EXPORT_LAB
        if (!retainScene) {
            sceneOrchestrator.stop()
            clearScreenRefs()
        }
        val root = FrameLayout(this).apply {
            // Over the live scene: dark, but the visualisation still shows through.
            setBackgroundColor(if (retainScene) Color.argb(228, 5, 8, 12) else COLOR_BG)
            isClickable = true
        }
        if (!retainScene) {
            attachExportProjectMPreview(
                root,
            )
        }
        // projectM for the MP4 is made only when an export starts (withExportProjectM).
        val projectMOnDemand =
            retainScene && projectMMainView != null
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
                // V70: always back to the centre.
                showNowPlaying()
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
                                null || projectMOnDemand ->
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
                "Відео складається з усіх увімкнених шарів — так само, як на екрані: візуалізація projectM, реакції, еквалайзер, герой, ефекти, кнопки (HUD). Кадр PNG і MP4 малюються однаково.",
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
                    null || projectMOnDemand
            val hasSnapshot =
                compositionConfig
                    .projectMFrame !=
                null

            content.addView(
                label(
                    when {
                        hasLiveCapture ->
                            "✓ Візуалізація projectM готова до запису у відео (швидше, ніж грає пісня, плеєр не заважає)."

                        hasSnapshot ->
                            "✓ Є знімок projectM — він піде в PNG; у відео — якщо живий запис недоступний."

                        else ->
                            "⚠ projectM увімкнено, але запис недоступний. Повернись на центр і відкрий Export Lab ще раз."
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

        // V59 / V60 (faric 19.10 23:10–23:12): no separate «Проаналізувати трек офлайн»
        // step — the export buttons analyse the sound themselves, only as much as needed.
        val hasTrack =
            controller.currentTrackUri() != null

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
                exportPill(
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(8)
                },
            )
        }

        // V38: buttons (HUD) in the video — yes / no, remembered.
        content.addView(
            exportPill(
                text = if (hudInVideoEnabled()) "Кнопки у відео: так" else "Кнопки у відео: ні",
                accent = false,
            ) {
                exportPrefs().edit().putBoolean(KEY_EXPORT_HUD_IN_VIDEO, !hudInVideoEnabled()).apply()
                showExportLab()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(14)
            },
        )

        val exportReady =
            true

        content.addView(
            exportPill(
                text =
                    if (exportReady) {
                        "Зберегти кадр композиції PNG"
                    } else {
                        "Ця тема ще не готова до export proof"
                    },
                accent = exportReady,
            ) {
                if (exportReady) {
                    captureLiveProjectMThen { exportProofFrame() }
                } else {
                    toast("Ця сцена ще не має export renderer")
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(22)
            },
        )

        // V59: buttons always keep their own name; unavailable → grey + a hint on tap.
        content.addView(
            exportPill(
                text = "Тест 3 с → MP4",
                accent = exportReady && hasTrack,
            ) {
                when {
                    !exportReady -> toast("Ця тема ще не вміє експорт MP4")
                    !hasTrack -> longToast("Спочатку обери пісню")
                    else -> exportProofVideo()
                }
            }.apply {
                if (!(exportReady && hasTrack)) alpha = 0.5f
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            exportPill(
                text = "Уся пісня → MP4",
                accent = false,
            ) {
                if (!hasTrack) longToast("Спочатку обери пісню") else exportFullSongVideo()
            }.apply {
                if (!hasTrack) alpha = 0.5f
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            exportPill(
                text = "Пакет: усі пісні з папки → MP4",
                accent = false,
            ) {
                startBatchFolderPicker()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            exportPill(
                text = "Пакет-тест: по 3 с з кожної пісні",
                accent = false,
            ) {
                startBatchFolderPicker(testMode = true)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            exportPill(
                text = "Відкрити папку експорту",
                accent = false,
            ) {
                showExportFolderChooser()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            },
        )

        content.addView(
            label(
                "«Тест 3 с» — 3 секунди з того місця, де зараз пісня. «Уся пісня» — від 0:00 до кінця. Звук аналізується сам, рівно стільки, скільки треба для відео.",
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
        val host = persistentSceneRoot
        if (retainScene && host != null) {
            exportLabOverlay?.let { old -> (old.parent as? ViewGroup)?.removeView(old) }
            host.addView(
                root,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            exportLabOverlay = root
            updateSceneOrchestratorState()
            updateProjectMRenderState()
            Log.i("FARIC-nav", "Export Lab overlay over retained scene; projectM=" + System.identityHashCode(projectMMainView))
        } else {
            setContentView(root)
            projectMExportLiveView
                ?.onResume()
            projectMExportLiveResumed =
                projectMExportLiveView !=
                null
        }

        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    /**
     * V3: the offline projectM for an MP4 exists only while an export runs. projectM is a
     * single native instance, so the live one is released first, the export one is put
     * under the Export Lab overlay, and [finishExportProjectM] gives the live one back.
     */
    private fun withExportProjectM(onReady: () -> Unit) {
        val overlay = exportLabOverlay
        if (overlay == null || projectMExportLiveView != null || !projectMSurfaceVisible()) {
            onReady()
            return
        }
        val live = projectMMainView
        val attach: () -> Unit = {
            if (!isFinishing && !isDestroyed && exportLabOverlay === overlay) {
                if (live != null && projectMMainView === live) {
                    if (projectMMainResumed) {
                        live.onPause()
                        projectMMainResumed = false
                    }
                    pulseDeckLayerStack?.setContent(PulseDeckLayerStack.Layer.VISUALIZER, null)
                    projectMMainView = null
                }
                attachExportProjectMPreview(overlay)
                projectMExportLiveView?.onResume()
                projectMExportLiveResumed = projectMExportLiveView != null
                Log.i("FARIC-export", "Export projectM attached for this export only")
            }
            onReady()
        }
        if (live != null) live.releaseProjectMThen { runOnUiThread(attach) } else attach()
    }

    /** V3: after the MP4 — export projectM away, the live visualiser back in its slot. */
    private fun finishExportProjectM() {
        val exportView = projectMExportLiveView ?: return
        if (exportLabOverlay == null) return // classic full-screen Export Lab keeps its view
        ProjectMBridge.endOfflineExport()
        exportView.releaseProjectMThen {
            runOnUiThread {
                if (projectMExportLiveResumed) {
                    exportView.onPause()
                    projectMExportLiveResumed = false
                }
                (exportView.parent as? ViewGroup)?.let { parent ->
                    parent.findViewWithTag<View>(EXPORT_PREVIEW_DIM_TAG)?.let(parent::removeView)
                    parent.removeView(exportView)
                }
                if (projectMExportLiveView === exportView) projectMExportLiveView = null
                val stack = pulseDeckLayerStack
                if (!isFinishing && !isDestroyed && stack != null && projectMMainView == null) {
                    val restored = createLayer0ProjectMView()
                    projectMMainView = restored
                    stack.setContent(PulseDeckLayerStack.Layer.VISUALIZER, restored)
                    applyVisualizerRenderSlots()
                    updateProjectMRenderState()
                    Log.i("FARIC-export", "Live projectM restored after export (only the visualizer slot)")
                }
            }
        }
    }

    /** V3: PNG needs a projectM frame — take it from the live view only when asked. */
    private fun captureLiveProjectMThen(next: () -> Unit) {
        val live = projectMMainView
        if (exportLabOverlay == null || live == null || !projectMSurfaceVisible()) {
            next()
            return
        }
        live.captureFrame { bitmap ->
            if (bitmap != null) projectMExportSnapshot = bitmap
            next()
        }
    }


    private fun showExportFolderChooser() {
        // V61 / V62 (faric 19.10 23:13): canonical window; icon tiles — the icon in its own
        // area, the text centred and wrapping, the tile grows.
        PulseDeckDialogs.show(
            context = this,
            title = "Папка експорту",
            message = "Куди перейти?",
            actions =
                listOf(
                    PulseDeckDialogs.Action(label = "Кадри PNG", subtitle = "Pictures/FARIC", icon = "▣") {
                        openExportFolder("Pictures/FARIC")
                    },
                    PulseDeckDialogs.Action(label = "Відео зі звуком", subtitle = "Movies/FARIC", icon = "▶") {
                        openExportFolder("Movies/FARIC")
                    },
                    PulseDeckDialogs.Action(label = "Скасувати") {},
                ),
        )
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
            groupReaction = BoardGroupReaction.neutral(),
            layerTransforms =
                activeGfLayerTransforms(gfThemeId),
            assetOverrides = gfMixController.overrides(),
            layerReactionTunings = boardLayerReactionTuningStore.loadAll(
                gfThemeId, boardGroupReactionStore.load(gfThemeId),
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
                            gfMixController.visible(
                                layerId,
                                layerObjectVisible(
                                    PulseDeckLayerStack.Layer.GRAPHIC_FIGURES,
                                    layerId.name.lowercase(),
                                ),
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
            projectMSurfaceVisible() &&
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
            // V38 (faric 19.10 16:07: «без кнопок — так і пишеться»): Export Lab switch.
            pulseDeckVisible =
                hudInVideoEnabled(),
            pulseDeckFavorite =
                controller.currentTrackUri()?.let { LocalMusicUserState(this).isFavorite(it.toString()) } == true,
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
        range: Pair<Long, Long>? = null,
        onProgress: (Int) -> Unit,
    ): OfflinePcmCacheResult {
        val key =
            pcmCacheKey(sourceUri, range)

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
                startMs = range?.first ?: 0L,
                endMs = range?.second ?: Long.MAX_VALUE,
            )

        offlinePcmCache =
            result
        offlinePcmCacheUri =
            key

        return result
    }

    /** V60: a «3 s» cache covers only its piece, so it must never be reused for the whole song. */
    private fun pcmCacheKey(
        sourceUri: Uri,
        range: Pair<Long, Long>?,
    ): String =
        sourceUri.toString() + (range?.let { "#" + it.first + "-" + it.second } ?: "")

    /**
     * V60 (faric 19.10 23:12): analysis runs by itself when an export starts, and only for
     * what the video needs — a 3 s clip analyses its piece, the whole song the whole song
     * (that one is kept and reused for the next whole-song export of the same track).
     */
    private fun analysisForExport(
        sourceUri: Uri,
        fullSong: Boolean,
        clipStartMs: Long,
        clipLengthMs: Long,
        onProgress: (Int) -> Unit,
    ): OfflineAnalysisResult {
        val key = sourceUri.toString()
        if (fullSong) {
            offlineAnalysis
                ?.takeIf { offlineAnalysisUri == key && it.startMs == 0L && it.signals.isNotEmpty() }
                ?.let { return it }
            val whole = OfflineAudioAnalyzer.analyze(context = this, uri = sourceUri, onProgress = onProgress)
            offlineAnalysis = whole
            offlineAnalysisUri = key
            return whole
        }
        val (from, to) = com.saney.musicvisualizer.export.DecodeRange.forClip(clipStartMs, clipLengthMs)
        return OfflineAudioAnalyzer.analyze(
            context = this,
            uri = sourceUri,
            onProgress = onProgress,
            startMs = from,
            endMs = to,
        )
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

    /**
     * Temporary rotation safety guard while the existing export depends on
     * MainActivity-owned projectM/GL. Background recovery is a separate task.
     */
    /**
     * V25: while an MP4 export runs, leaving the export screen would call
     * clearScreenRefs(), which ends the offline projectM and releases the export
     * view mid-render. The progress dialog blocks taps, but a notification tap
     * (openFromNotification) or any programmatic navigation could still do it.
     */
    private fun blockedByActiveExport(target: String): Boolean {
        if (!exportRunGate.isActive()) return false
        CrashJournal.exportNote(this, "перехід $target заблоковано")
        toast("Йде експорт MP4 — дочекайся кінця або натисни «Скасувати»")
        return true
    }

    private fun maybeShowCrashReport() {
        if (isFinishing || isDestroyed) return
        val report = runCatching { CrashJournal.pendingReport(this) }.getOrNull() ?: return
        CrashJournal.markReported(this)
        showDiagnosticsReport(title = "Минулого разу застосунок закрився", report = report)
    }

    private fun showDiagnosticsReport(
        title: String,
        report: String,
    ) {
        PulseDeckDialogs.showMessage(
            context = this,
            title = title,
            message = report + "\n\nНатисни «Копіювати» і надішли текст Claude.",
            primaryLabel = "OK",
            secondaryLabel = "Копіювати",
            scrollableMessage = true,
            keepOpenOnSecondary = true,
        ) {
            val clipboard =
                getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("FARIC crash journal", report))
            toast("Текст скопійовано")
        }
    }

    private fun beginGuardedExport(fullSong: Boolean): Boolean {
        if (!exportRunGate.tryStart()) {
            toast("Експорт уже виконується")
            return false
        }
        if (fullSong) {
            orientationBeforeFullSongExport = requestedOrientation
            val lock = runCatching {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            }
            if (lock.isFailure) {
                Log.e("FARIC-export", "Orientation lock failed", lock.exceptionOrNull())
                orientationBeforeFullSongExport = null
                exportRunGate.finish()
                toast("Не вдалося захистити експорт від повороту")
                return false
            }
        }
        Log.i("FARIC-export", "Export started; fullSong=$fullSong")
        return true
    }

    private fun endGuardedExport() {
        if (!exportRunGate.isActive()) return
        exportRunGate.finish()
        activeExportCancel = null
        // onStop() kept the export GL view alive during the render (V25);
        // pause it now if the screen is no longer visible.
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) && projectMExportLiveResumed) {
            projectMExportLiveView?.onPause()
            projectMExportLiveResumed = false
        }
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // V3: the live visualiser goes back into its slot (Export Lab overlay only).
        finishExportProjectM()
        val previous = orientationBeforeFullSongExport
        orientationBeforeFullSongExport = null
        if (previous != null && !isFinishing && !isDestroyed) {
            requestedOrientation = previous
        }
        Log.i("FARIC-export", "Export guard released")
    }

    private fun exportCompositionVideo(
        fullSong: Boolean,
    ) {
        if (exportRunGate.isActive()) {
            toast("Експорт уже виконується")
            return
        }
        if (controller.currentTrackUri() == null) {
            longToast("Спочатку обери пісню")
            return
        }
        if (latestSnapshot.durationMs <= 0L) {
            longToast("Пісня ще відкривається — спробуй за секунду")
            return
        }
        // V3: the export projectM is made now (and given back after), not on opening Export Lab.
        withExportProjectM { startCompositionVideoExport(fullSong) }
    }

    private fun startCompositionVideoExport(
        fullSong: Boolean,
    ) {
        val sourceUri =
            controller
                .currentTrackUri()
                ?: run {
                    longToast("Спочатку обери пісню")
                    return
                }
        val snapshot =
            latestSnapshot
        // V60: the song length comes from the player; analysis runs inside the export.
        val songDurationMs =
            snapshot.durationMs
        if (songDurationMs <= 0L) {
            longToast("Пісня ще відкривається — спробуй за секунду")
            return
        }
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
                        (songDurationMs - 500L).coerceAtLeast(0L),
                    )
            }
        val requestedDurationMs =
            if (fullSong) {
                songDurationMs.coerceAtLeast(500L)
            } else {
                minOf(
                    PROJECTM_EXPORT_DURATION_MS,
                    (songDurationMs - exportStartMs).coerceAtLeast(0L),
                ).coerceAtLeast(500L)
            }
        val projectMView =
            projectMExportLiveView
                ?.takeIf {
                    compositionConfig
                        .projectMVisible
                }

        val directGpuProjectMEligible =
            isDirectGpuExportEligible(
                compositionConfig = compositionConfig,
                projectMView = projectMView,
            )

        if (!beginGuardedExport(fullSong)) {
            finishExportProjectM()
            return
        }
        val cancelled =
            AtomicBoolean(
                false,
            )
        activeExportCancel = cancelled
        // V24: leave a trail on disk, so a process death mid-export is explained next start.
        CrashJournal.exportStarted(
            context = this,
            fullSong = fullSong,
            path =
                when {
                    directGpuProjectMEligible -> "А (повний GPU)"
                    projectMView != null -> "Б (projectM через копію)"
                    else -> "В (без projectM)"
                },
            track = snapshot.trackName.orEmpty(),
            durationMs = requestedDurationMs,
        )

        // V62: canonical window (title · body · buttons always visible); Back does not
        // close it — the export is stopped only with «Скасувати».
        val progressStatus =
            label(
                "Підготовка…",
                13f,
                COLOR_MUTED,
                false,
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
        val progressBody =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    progressStatus,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    progressBar,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(12),
                    ).apply {
                        topMargin = dp(14)
                    },
                )
                if (fullSong) {
                    addView(
                        label(
                            "Під час експорту поворот екрана тимчасово заблоковано.",
                            12f,
                            COLOR_MUTED,
                            false,
                        ),
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topMargin = dp(12)
                        },
                    )
                }
            }
        val dialog =
            PulseDeckDialogs.show(
                context = this,
                title = if (fullSong) "Експорт усієї пісні" else "Тестовий MP4 · 3 с",
                body = progressBody,
                cancelable = false,
                actions =
                    listOf(
                        PulseDeckDialogs.Action(label = "Скасувати", dismissOnClick = false) {
                            PulseDeckDialogs.showConfirm(
                                context = this,
                                title = "Скасувати експорт?",
                                message = "Поточний рендер буде зупинено. Готовий MP4 не буде створено.",
                                positiveLabel = "Так, скасувати",
                                negativeLabel = "Продовжити",
                                destructive = true,
                            ) {
                                cancelled.set(true)
                                progressStatus.text = "Скасування…"
                                Log.i("FARIC-export", "Explicit cancellation requested")
                            }
                        },
                    ),
            )
        window.addFlags(
            android.view.WindowManager
                .LayoutParams
                .FLAG_KEEP_SCREEN_ON,
        )

        var lastLoggedDecile = -1
        fun updateProgress(
            progress: Int,
            status: String,
        ) {
            CrashJournal.exportProgress(this, progress, status)
            val decile = progress.coerceIn(0, 100) / 10
            if (decile != lastLoggedDecile) {
                lastLoggedDecile = decile
                Log.i("FARIC-export", "Render progress " + decile * 10 + "%")
            }
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
            runCatching {
                updateProgress(0, "Аналізую звук…")
                val analysis =
                    analysisForExport(
                        sourceUri = sourceUri,
                        fullSong = fullSong,
                        clipStartMs = exportStartMs,
                        clipLengthMs = requestedDurationMs,
                    ) { p -> updateProgress(0, "Аналізую звук · $p%") }
                if (cancelled.get()) throw java.util.concurrent.CancellationException("Export cancelled")
                runCompositionExportBlocking(
                    fullSong = fullSong,
                    sourceUri = sourceUri,
                    analysis = analysis,
                    exportStartMs = exportStartMs,
                    requestedDurationMs = requestedDurationMs,
                    ratio = ratio,
                    compositionConfig = compositionConfig,
                    projectMView = projectMView,
                    directGpuProjectMEligible = directGpuProjectMEligible,
                    title = snapshot.trackName.orEmpty(),
                    artist = snapshot.trackArtist.orEmpty(),
                    cancelled = cancelled,
                    updateProgress = { progress, status -> updateProgress(progress, status) },
                )
            }.onSuccess { result ->
                Log.i("FARIC-export", "Render finished; published=" + (result.uri != null))
                CrashJournal.exportFinished(
                    this,
                    if (result.uri != null) {
                        "готово · " + result.width + "×" + result.height + " · " + result.frameCount + " кадрів · " + result.totalMs + " мс"
                    } else {
                        "MP4 не збережено"
                    },
                )
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) dialog.dismiss()

                    if (
                        result.uri !=
                        null
                    ) {
                        // v0.19.73: the full song also shows the timing report (was a 2 s toast),
                        // so the whole-song speed can be measured against EXPORT_ETALON.md.
                        showExportTimingResult(
                            result,
                        )
                    } else {
                        toast(
                            "Не вдалося зберегти MP4",
                        )
                    }
                }
            }.onFailure { error ->
                Log.e("FARIC-export", "Export failed", error)
                val cancelledByUser = error is CancellationException
                val errorLine = error.javaClass.simpleName + ": " + (error.message ?: "—")
                CrashJournal.exportFinished(
                    this,
                    if (cancelledByUser) "скасовано" else "помилка · " + errorLine.take(200),
                )
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) dialog.dismiss()

                    if (cancelledByUser) {
                        toast(
                            "Експорт скасовано",
                        )
                    } else if (!isDestroyed && !isFinishing) {
                        // V24: a toast vanished in 2 s; keep the reason readable and copyable.
                        val trace =
                            error.stackTrace
                                .take(12)
                                .joinToString("\n") { "  at " + it }
                        showDiagnosticsReport(
                            title = "Експорт MP4 не вдався",
                            report =
                                errorLine + "\n" + trace +
                                    "\n\n" + CrashJournal.fullReport(this),
                        )
                    }
                }
            }

            // Keep orientation locked until native GL and PCM cleanup is done.
            runOnUiThread { endGuardedExport() }
        }
    }

    /**
     * Path A (projectM straight into the encoder on the GPU) is possible when the
     * composition has projectM + GF and none of the CPU-only visualizer layers.
     *
     * V43 (v0.19.73): the GF *background / glow* is no longer required. Until now
     * a hidden GF background silently switched the whole export to the slower
     * path B (owner's 3 s test 19.10: 7074 ms, glReadPixels 4.8 s). The renderers
     * already skip a hidden background (CyberSharkExportRenderer checks
     * BoardLayerId.BACKGROUND), and the GPU glow is optional since v0.19.23.
     */
    private fun isDirectGpuExportEligible(
        compositionConfig: CompositionExportConfig,
        projectMView: ProjectMView?,
    ): Boolean =
        projectMView != null &&
            compositionConfig.projectMVisible &&
            !compositionConfig.faricReactiveVisible &&
            !compositionConfig.overVisualizationVisible &&
            !compositionConfig.bigEqualizerVisible &&
            compositionConfig.graphicFiguresVisible

    /**
     * Renders ONE song to MP4 on the calling (worker) thread and returns the result.
     * Shared by the single export (exportCompositionVideo) and the folder batch
     * (v0.19.73). The body is the former inline worker of exportCompositionVideo,
     * unchanged (see docs/export/EXPORT_ETALON.md): PCM cache → projectM offline
     * setup (path A / B) → ShortVideoExportProof.export → cleanup.
     */
    private fun runCompositionExportBlocking(
        fullSong: Boolean,
        sourceUri: Uri,
        analysis: OfflineAnalysisResult,
        exportStartMs: Long,
        requestedDurationMs: Long,
        ratio: ExportAspectRatio,
        compositionConfig: CompositionExportConfig,
        projectMView: ProjectMView?,
        directGpuProjectMEligible: Boolean,
        title: String,
        artist: String,
        cancelled: AtomicBoolean,
        updateProgress: (Int, String) -> Unit,
        exactDisplayName: String? = null,
        relativePath: String = com.saney.musicvisualizer.export.Mp4AvMuxer.DEFAULT_RELATIVE_PATH,
    ): ShortVideoExportProof.Result {
        val currentUriKey =
            sourceUri.toString()
        var pcmReader:
            com.saney.musicvisualizer.export
                .OfflinePcmReader? =
            null

        var offlineProjectM =
            false

        return try {
                val needsProjectMPcm =
                    projectMView !=
                        null

                // V60: a 3 s clip decodes only its piece (+ lead-in) for projectM.
                val pcmRange =
                    if (fullSong) {
                        null
                    } else {
                        com.saney.musicvisualizer.export.DecodeRange.forClip(exportStartMs, requestedDurationMs)
                    }
                val pcmKey =
                    pcmCacheKey(sourceUri, pcmRange)
                val pcmPrepWeight =
                    if (
                        needsProjectMPcm &&
                        (
                            offlinePcmCacheUri !=
                                pcmKey ||
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
                            range = pcmRange,
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
                        title,
                    artist = artist,
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
                    exactDisplayName =
                        exactDisplayName,
                    relativePath =
                        relativePath,
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
                        val stage = when {
                            progress < 78 -> "Рендерю відео"
                            progress < 92 -> "Кодую аудіо"
                            progress < 100 -> "Збираю та зберігаю MP4"
                            else -> "MP4 успішно збережено"
                        }
                        updateProgress(
                            mapped,
                            stage + " · " + mapped + "%",
                        )
                    },
                )
        } finally {
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

    // ---- Folder batch export (TODO.md V22, v0.19.73) ----------------------------
    // faric 19.10 04:16: «щоб я міг указати папку, і він прогнав із папки всі пісні».
    // MVP: runs on the Export Lab screen with the screen on (same engine as
    // «Експортувати всю пісню MP4»). Background service is V2 (next stage).

    private data class BatchSong(
        val uri: Uri,
        val name: String,
        val durationMs: Long,
    )

    private fun startBatchFolderPicker(testMode: Boolean = false) {
        if (exportRunGate.isActive()) {
            toast("Йде експорт MP4 — дочекайся кінця")
            return
        }
        if (screen != Screen.EXPORT_LAB) {
            toast("Відкрий Export Lab")
            return
        }
        getSharedPreferences(BATCH_REQUEST_PREFS, MODE_PRIVATE).edit().putBoolean(KEY_BATCH_TEST_MODE, testMode).commit()
        selectBatchFolder.launch(null)
    }

    private fun onBatchFolderPicked(
        treeUri: Uri,
        testMode: Boolean,
    ) {
        lastBatchTreeUri = treeUri
        runCatching {
            contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        toast("Шукаю пісні в папці…")
        thread(name = "faric-batch-scan") {
            val scan = runCatching { scanBatchFolder(treeUri) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                scan
                    .onSuccess { (folderName, songs) -> confirmBatch(folderName, songs, testMode) }
                    .onFailure { error ->
                        PulseDeckDialogs.showMessage(
                            this,
                            "Папку не прочитано",
                            (error.javaClass.simpleName + ": " + (error.message ?: "—")).take(300),
                        )
                    }
            }
        }
    }

    private fun scanBatchFolder(treeUri: Uri): Pair<String, List<BatchSong>> {
        val treeDocId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
        val folderDoc = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val folderName =
            runCatching {
                contentResolver.query(
                    folderDoc,
                    arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull()
                ?: treeDocId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Batch" }
        val children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        val found = mutableListOf<Pair<Uri, String>>()
        contentResolver.query(
            children,
            arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                if (mime == android.provider.DocumentsContract.Document.MIME_TYPE_DIR) continue
                if (!com.saney.musicvisualizer.export.BatchPlan.isAudio(name, mime)) continue
                found += android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId) to name
            }
        }
        val songs =
            com.saney.musicvisualizer.export.BatchPlan.ordered(found) { it.second }.map { (uri, name) ->
                val duration =
                    runCatching {
                        val retriever = android.media.MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(this, uri)
                            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        } finally {
                            retriever.release()
                        }
                    }.getOrDefault(0L)
                BatchSong(uri = uri, name = name, durationMs = duration)
            }
        return folderName to songs
    }

    private fun confirmBatch(
        folderName: String,
        songs: List<BatchSong>,
        testMode: Boolean = false,
    ) {
        if (songs.isEmpty()) {
            PulseDeckDialogs.showMessage(
                this,
                "У папці нема пісень",
                "У «$folderName» не знайдено аудіофайлів (mp3, m4a, flac, wav, ogg…). Підпапки поки не переглядаються.",
            )
            return
        }
        val plan = com.saney.musicvisualizer.export.BatchPlan
        val totalMs = songs.sumOf { it.durationMs }
        if (testMode) {
            val message =
                buildString {
                    append("Папка: ").append(folderName).append('\n')
                    append("Пісень: ").append(songs.size).append(" · з кожної — 3 с із середини пісні\n")
                    append("Орієнтовно: по пів хвилини на пісню (аналіз звуку + 3 с відео)\n")
                    append("Відео: ").append(plan.relativePath(folderName)).append("Тест 3с <дата час>/ — кожен запуск в окрему папку\n")
                    append("Шари й формат — як зараз на екрані (").append(exportAspectRatio.width).append('×').append(exportAspectRatio.height).append(")\n")
                    append("У підсумку — час кожної пісні, можна порівняти з наступним запуском.\n")
                    append("Не вимикай екран і не виходь із застосунку.\n\n")
                    songs.take(12).forEachIndexed { i, s -> append(i + 1).append(". ").append(s.name).append('\n') }
                    if (songs.size > 12) append("… і ще ").append(songs.size - 12)
                }.trimEnd()
            val confirm =
                PulseDeckDialogs.showConfirm(
                    context = this,
                    title = "Пакет-тест: ${songs.size} × 3 с",
                    message = message,
                    positiveLabel = "Почати тест",
                    negativeLabel = "Скасувати",
                ) {
                    withExportProjectM { runBatchExport(folderName, songs, testMode = true) }
                }
            rememberBatchConfirm(confirm, testMode = true)
            return
        }
        val message =
            buildString {
                append("Папка: ").append(folderName).append('\n')
                append("Пісень: ").append(songs.size).append(" · разом ").append(plan.formatMinutes(totalMs)).append('\n')
                append("Орієнтовно: ~").append(plan.estimateMinutes(totalMs)).append(" хв (телефон рахує приблизно вдвічі довше за музику)\n")
                append("Відео: ").append(plan.relativePath(folderName)).append(" · назва = назва пісні\n")
                append("Шари й формат — як зараз на екрані (").append(exportAspectRatio.width).append('×').append(exportAspectRatio.height).append(")\n")
                append("Готові відео пропускаються — можна продовжити пізніше.\n")
                append("Не вимикай екран і не виходь із застосунку; краще на зарядці.\n\n")
                songs.take(12).forEachIndexed { i, s ->
                    append(i + 1).append(". ").append(s.name)
                    if (s.durationMs > 0) append(" · ").append(plan.formatMinutes(s.durationMs))
                    append('\n')
                }
                if (songs.size > 12) append("… і ще ").append(songs.size - 12)
            }.trimEnd()
        val confirm =
            PulseDeckDialogs.showConfirm(
                context = this,
                title = "Пакет: ${songs.size} пісень → MP4",
                message = message,
                positiveLabel = "Почати",
                negativeLabel = "Скасувати",
            ) {
                withExportProjectM { runBatchExport(folderName, songs) }
            }
        rememberBatchConfirm(confirm, testMode = false)
    }

    /** V49: after a rotation the same folder is read again and the confirm comes back. */
    private fun rememberBatchConfirm(
        dialog: android.app.Dialog,
        testMode: Boolean,
    ) {
        val uri = lastBatchTreeUri ?: return
        rememberWindow(dialog, RestorableWindow(WINDOW_BATCH_CONFIRM, "", "", uri.toString() + "|" + testMode))
    }

    private fun mediaStoreHasVideo(
        relativePath: String,
        displayName: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.Video.Media._ID),
                android.provider.MediaStore.Video.Media.RELATIVE_PATH + "=? AND " +
                    android.provider.MediaStore.Video.Media.DISPLAY_NAME + "=?",
                arrayOf(relativePath, displayName),
                null,
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }

    private fun runBatchExport(
        folderName: String,
        songs: List<BatchSong>,
        testMode: Boolean = false,
    ) {
        if (screen != Screen.EXPORT_LAB) {
            toast("Відкрий Export Lab")
            return
        }
        val plan = com.saney.musicvisualizer.export.BatchPlan
        val compositionConfig = currentCompositionExportConfig()
        val ratio = exportAspectRatio
        val projectMView = projectMExportLiveView?.takeIf { compositionConfig.projectMVisible }
        val directGpuEligible = isDirectGpuExportEligible(compositionConfig = compositionConfig, projectMView = projectMView)
        val relativePath =
            if (testMode) {
                plan.testRelativePath(
                    folderName,
                    java.text.SimpleDateFormat("dd.MM HH-mm", java.util.Locale.ROOT).format(java.util.Date()),
                )
            } else {
                plan.relativePath(folderName)
            }

        if (!beginGuardedExport(fullSong = true)) {
            finishExportProjectM()
            return
        }
        val cancelled = AtomicBoolean(false)
        activeExportCancel = cancelled

        // V62: canonical window; Back does not close it — «Зупинити пакет» does.
        val songLabel = label("Підготовка…", 15f, Color.WHITE, true)
        val stageLabel = label("", 13f, COLOR_MUTED, false)
        val songBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        val overallLabel = label("Усього: 0 з ${songs.size}", 13f, COLOR_MUTED, false)
        val overallBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = songs.size * 100 }
        val batchBody =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(songLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(stageLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
                addView(songBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)).apply { topMargin = dp(12) })
                addView(overallLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
                addView(overallBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14)).apply { topMargin = dp(6) })
            }
        val dialog =
            PulseDeckDialogs.show(
                context = this,
                title = if (testMode) "Пакет-тест · ${songs.size} × 3 с" else "Пакет · ${songs.size} пісень → MP4",
                body = batchBody,
                cancelable = false,
                actions =
                    listOf(
                        PulseDeckDialogs.Action(label = "Зупинити пакет", dismissOnClick = false) {
                            PulseDeckDialogs.showConfirm(
                                context = this,
                                title = "Зупинити пакет?",
                                message = "Поточна пісня не збережеться. Готові відео лишаться; наступного разу вони пропустяться.",
                                positiveLabel = "Так, зупинити",
                                negativeLabel = "Продовжити",
                                destructive = true,
                            ) {
                                cancelled.set(true)
                                stageLabel.text = "Зупиняю…"
                            }
                        },
                    ),
            )
        dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        thread(name = "faric-batch-export") {
            val results = mutableListOf<com.saney.musicvisualizer.export.BatchPlan.SongResult>()
            for ((index, song) in songs.withIndex()) {
                if (cancelled.get()) break
                val fileName = plan.videoFileName(song.name)
                val title = plan.songTitle(song.name)
                val position = "Пісня ${index + 1} з ${songs.size}"
                fun ui(
                    songProgress: Int,
                    stage: String,
                ) {
                    val p = songProgress.coerceIn(0, 100)
                    // V67: the crash journal follows the batch too (was stuck at «0 % — підготовка»).
                    CrashJournal.exportProgress(this, p, stage)
                    runOnUiThread {
                        if (isFinishing) return@runOnUiThread
                        songLabel.text = "$position · $title"
                        stageLabel.text = stage
                        songBar.progress = p
                        overallBar.progress = index * 100 + p
                        overallLabel.text = "Усього: ${results.size} з ${songs.size} · зроблено ${results.count { it.outcome == com.saney.musicvisualizer.export.BatchPlan.Outcome.DONE }}"
                    }
                }
                if (!testMode && mediaStoreHasVideo(relativePath, fileName)) {
                    results += com.saney.musicvisualizer.export.BatchPlan.SongResult(title, com.saney.musicvisualizer.export.BatchPlan.Outcome.SKIPPED_EXISTS, "відео вже є")
                    ui(100, "Уже є — пропускаю")
                    continue
                }
                val startedAt = System.currentTimeMillis()
                CrashJournal.exportStarted(
                    context = this,
                    fullSong = !testMode,
                    path =
                        when {
                            directGpuEligible -> "А (повний GPU)"
                            projectMView != null -> "Б (projectM через копію)"
                            else -> "В (без projectM)"
                        } + (if (testMode) " · пакет-тест " else " · пакет ") + "${index + 1}/${songs.size}",
                    track = title,
                    durationMs =
                        if (testMode) {
                            com.saney.musicvisualizer.export.BatchPlan.TEST_CLIP_MS.coerceAtMost(song.durationMs.coerceAtLeast(500L))
                        } else {
                            song.durationMs
                        },
                )
                val outcome =
                    runCatching {
                        ui(0, "Аналізую звук…")
                        // V60: the test analyses only its 3 s piece (+ lead-in), not the whole song.
                        val knownDurationMs = song.durationMs
                        val testClip =
                            if (testMode && knownDurationMs > 0L) plan.testWindow(knownDurationMs) else null
                        val analysis =
                            if (testClip != null) {
                                val (from, to) =
                                    com.saney.musicvisualizer.export.DecodeRange.forClip(testClip.first, testClip.second)
                                OfflineAudioAnalyzer.analyze(
                                    context = this,
                                    uri = song.uri,
                                    onProgress = { p -> ui(p / 20, "Аналізую звук · $p%") },
                                    startMs = from,
                                    endMs = to,
                                )
                            } else {
                                OfflineAudioAnalyzer.analyze(
                                    context = this,
                                    uri = song.uri,
                                    onProgress = { p -> ui(p / 20, "Аналізую звук · $p%") },
                                )
                            }
                        if (cancelled.get()) throw CancellationException("Batch cancelled")
                        val (clipStartMs, clipLengthMs) =
                            when {
                                testClip != null -> testClip
                                testMode -> plan.testWindow(analysis.durationMs)
                                else -> 0L to analysis.durationMs.coerceAtLeast(500L)
                            }
                        runCompositionExportBlocking(
                            fullSong = !testMode,
                            sourceUri = song.uri,
                            analysis = analysis,
                            exportStartMs = clipStartMs,
                            requestedDurationMs = clipLengthMs,
                            ratio = ratio,
                            compositionConfig = compositionConfig,
                            projectMView = projectMView,
                            directGpuProjectMEligible = directGpuEligible,
                            title = title,
                            artist = "",
                            cancelled = cancelled,
                            updateProgress = { progress, status -> ui(5 + progress * 95 / 100, status) },
                            exactDisplayName = fileName,
                            relativePath = relativePath,
                        )
                    }
                val elapsed = System.currentTimeMillis() - startedAt
                outcome
                    .onSuccess { result ->
                        CrashJournal.exportFinished(this, "готово · пакет · " + result.frameCount + " кадрів · " + result.totalMs + " мс")
                        // Render time of the clip itself (same number as the 3 s test window) — to compare runs.
                        results += com.saney.musicvisualizer.export.BatchPlan.SongResult(title, com.saney.musicvisualizer.export.BatchPlan.Outcome.DONE, "відео " + result.totalMs + " мс", elapsed)
                    }.onFailure { error ->
                        Log.e("FARIC-export", "Batch song failed: $title", error)
                        if (error is CancellationException || cancelled.get()) {
                            CrashJournal.exportFinished(this, "скасовано (пакет)")
                            results += com.saney.musicvisualizer.export.BatchPlan.SongResult(title, com.saney.musicvisualizer.export.BatchPlan.Outcome.CANCELLED, "зупинено")
                        } else {
                            val line = (error.javaClass.simpleName + ": " + (error.message ?: "—")).take(200)
                            CrashJournal.exportFinished(this, "помилка (пакет) · $line")
                            results += com.saney.musicvisualizer.export.BatchPlan.SongResult(title, com.saney.musicvisualizer.export.BatchPlan.Outcome.FAILED, line, elapsed)
                        }
                    }
                if (cancelled.get()) break
            }
            val summary =
                plan.summary(folder = folderName, results = results, total = songs.size)
                    .replace("Відео: " + plan.relativePath(folderName), "Відео: " + relativePath)
                    .let { if (testMode) "Пакет-тест · по 3 с з середини кожної пісні\n" + it else it }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    dialog.dismiss()
                    showBatchSummaryWindow(
                        title =
                            when {
                                cancelled.get() -> if (testMode) "Пакет-тест зупинено" else "Пакет зупинено"
                                testMode -> "Пакет-тест готовий"
                                else -> "Пакет готовий"
                            },
                        summary = summary,
                        relativePath = relativePath,
                    )
                }
                endGuardedExport()
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

    /**
     * Cyber Shark's numbered gallery tile uses the same canonical live-switch
     * semantics as the former built-in theme tile, with bitmap decoding
     * outside the UI thread and no projectM or audio recreation.
     */
    private fun selectCyberSharkFromGallery() {
        val requestedTheme = PlaybackThemeId.CYBER_SHARK
        if (screen != Screen.THEME_PICKER) return
        if (themeMenuOverlay != null && latestSnapshot.trackName != null) {
            if (selectedThemeId == requestedTheme) {
                showNowPlaying()
                return
            }
            val generation = ++heroAssetLoadGeneration
            val requestedOverlay = themeMenuOverlay
            thread(name = "faric-shark-gallery-assets") {
                val assets = runCatching {
                    GraphicFigureCatalog.loadAssets(applicationContext, requestedTheme)
                }.getOrNull()
                runOnUiThread {
                    if (isFinishing || isDestroyed ||
                        generation != heroAssetLoadGeneration ||
                        screen != Screen.THEME_PICKER ||
                        themeMenuOverlay !== requestedOverlay
                    ) return@runOnUiThread
                    if (assets == null) {
                        toast("Не вдалося завантажити Cyber Shark")
                        return@runOnUiThread
                    }
                    activateLivePlaybackTheme(requestedTheme, preloaded = assets)
                    themeMenuLiveHeroSwitch = true
                    showNowPlaying()
                }
            }
        } else {
            selectedThemeId = requestedTheme
            themeStore.selectedThemeId = requestedTheme
            setGraphicFigureThemeId(requestedTheme)
            if (latestSnapshot.trackName != null) showNowPlaying()
            else showThemePicker()
        }
    }

    private fun showThemePicker() {
        if (blockedByActiveExport("showThemePicker")) return
        val retainedHost =
            persistentSceneRoot?.takeIf {
                screen == Screen.NOW_PLAYING &&
                    pulseDeckLayerStack != null
            }
        screen = Screen.THEME_PICKER
        if (retainedHost == null) {
            sceneOrchestrator.stop()
            clearScreenRefs()
        } else {
            themeMenuOriginalTheme = selectedThemeId
            themeMenuLiveHeroSwitch = false
        }

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

        // One expandable hero family, with a compact two-column visual grid.
        // All other themes remain first-class separate choices below it.
        val heroFamily = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = panelDrawable(COLOR_PANEL, 20, COLOR_ACCENT_CYAN, 1)
        }
        val heroTiles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (heroCatalogExpanded) View.VISIBLE else View.GONE
        }
        // Count only artwork actually available on this device. Future
        // prepared hero slots are definitions, not installed graphics.
        val visibleExternalHeroes = ExternalPngHeroLibrary(this).list()
            .filter { HeroGalleryPolicy.visibleExternalHero(it.title) }
        val visibleBuiltinCount = GraphicFigureCatalog.ids.count {
            it != PlaybackThemeId.CYBER_PANTHER &&
                GraphicFigureCatalog.isInstalled(this, it)
        }
        val availableHeroCount = visibleBuiltinCount + visibleExternalHeroes.size
        val toggleLabel = label(
            (if (heroCatalogExpanded) "▾ " else "▸ ") +
                "Герої · $availableHeroCount",
            20f, Color.WHITE, true,
        )
        heroFamily.addView(toggleLabel)
        heroFamily.addView(label(
            "Кібергерої FARIC / FMV / FVMP · натисни для вибору",
            12f, COLOR_MUTED, false,
        ))
        heroFamily.setOnClickListener {
            heroCatalogExpanded = !heroCatalogExpanded
            heroTiles.visibility = if (heroCatalogExpanded) View.VISIBLE else View.GONE
            toggleLabel.text =
                (if (heroCatalogExpanded) "▾ " else "▸ ") +
                "Герої · $availableHeroCount"
        }
        content.addView(
            actionPill(
                text = "📁 Зовнішні PNG-герої",
                accent = false,
            ) { showExternalPngHeroes() }.apply { minimumHeight = dp(52) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12); bottomMargin = dp(14) },
        )

        // Themes offers direct independent Frame/Wordmark/FX folder import too.
        addGfPngImportControls(content)

        content.addView(
            heroFamily,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(4); bottomMargin = dp(12) },
        )
        val importArchiveButton = label(
            "＋ Додати підготовлений пакет героїв",
            14f, COLOR_ACCENT_CYAN, true,
        ).apply {
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setOnClickListener {
                openHeroArchive.launch(arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream",
                ))
            }
        }
        if (UserHeroPack.heroes.any { !UserHeroPack.isInstalled(this, it.id) }) {
            heroTiles.addView(importArchiveButton)
        }
        val heroGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        heroTiles.addView(heroGrid)
        content.addView(heroTiles)
        content.addView(label(
            "Інші теми", 17f, Color.WHITE, true,
        ), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(18); bottomMargin = dp(12) })
        var heroRow: LinearLayout? = null
        var heroCount = 0

        val externalHeroes = visibleExternalHeroes
        // A built-in Shark replaces the hidden Scorpion B slot. A null source
        // is intentionally the built-in theme and never an external PNG URI.
        val galleryEntries = buildList<ExternalPngHeroLibrary.Hero?> {
            addAll(externalHeroes)
            if (GraphicFigureCatalog.isInstalled(this@MainActivity, PlaybackThemeId.CYBER_SHARK)) {
                add(null)
            }
        }
        val externalThumbs = mutableListOf<Pair<ExternalPngHeroLibrary.Hero, ImageView>>()
        val sharkThumbs = mutableListOf<ImageView>()
        if (galleryEntries.isNotEmpty()) {
            heroGrid.addView(label(
                "Мої PNG та Cyber Shark · ${galleryEntries.size}",
                16f, COLOR_ACCENT_CYAN, true,
            ))
            // Fixed-height cards use the same structure in every row:
            // number -> large transparent hero preview -> readable caption.
            // Do not use variable filename length to size individual tiles.
            val fontScale = resources.configuration.fontScale.coerceAtLeast(1f)
            val screenWidthDp =
                resources.displayMetrics.widthPixels / resources.displayMetrics.density
            val columns = if (screenWidthDp < 350f || fontScale >= 1.6f) 1 else 2
            val cardHeight = dp(if (columns == 1) 300 else 258) +
                dp(((fontScale - 1f).coerceAtMost(0.8f) * 48f).toInt())
            val captionHeight = dp((62f * fontScale.coerceAtMost(1.8f)).toInt())

            galleryEntries.withIndex().toList().chunked(columns).forEach { pair ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }
                heroGrid.addView(row)
                pair.forEach { (index, hero) ->
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(9), dp(8), dp(9), dp(10))
                        background = panelDrawable(COLOR_PANEL, 22, COLOR_ACCENT_CYAN, 1)
                        isClickable = true
                        isFocusable = true
                        contentDescription = "Герой ${index + 1}: ${hero?.title ?: "Cyber Shark"}"
                    }
                    val number = label(
                        com.saney.musicvisualizer.ui.ExternalHeroTileLabel.number(index),
                        16f, COLOR_ACCENT_CYAN, true,
                    ).apply {
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                    tile.addView(
                        number,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(26),
                        ),
                    )
                    val preview = ImageView(this).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = false
                        contentDescription = null // parent announces number and full name
                    }
                    tile.addView(
                        preview,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
                        ).apply { topMargin = dp(3); bottomMargin = dp(6) },
                    )
                    val caption = label(
                        com.saney.musicvisualizer.ui.ExternalHeroTileLabel.caption(hero?.title ?: "Cyber Shark"),
                        15f, Color.WHITE, true,
                    ).apply {
                        gravity = Gravity.CENTER
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        maxLines = 3
                        breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
                        // All cards stay the same height. Auto-fit only long
                        // labels; preserve the full source name for TalkBack.
                        setAutoSizeTextTypeUniformWithConfiguration(
                            11, 16, 1, android.util.TypedValue.COMPLEX_UNIT_SP,
                        )
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    tile.addView(
                        caption,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, captionHeight,
                        ),
                    )
                    tile.setOnClickListener {
                        if (hero == null) selectCyberSharkFromGallery()
                        else activateExternalPngHero(hero.id)
                    }
                    row.addView(
                        tile,
                        LinearLayout.LayoutParams(0, cardHeight, 1f).apply {
                            setMargins(dp(6), dp(6), dp(6), dp(6))
                        },
                    )
                    if (hero == null) sharkThumbs.add(preview)
                    else externalThumbs.add(hero to preview)
                }
                if (pair.size < columns) {
                    row.addView(
                        View(this),
                        LinearLayout.LayoutParams(0, cardHeight, 1f).apply {
                            setMargins(dp(6), dp(6), dp(6), dp(6))
                        },
                    )
                }
            }
            thread(name = "faric-theme-external-previews") {
                val library = ExternalPngHeroLibrary(applicationContext)
                externalThumbs.forEach { (hero, image) ->
                    val preview = library.decode(hero.id, preferredDimension = 192)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && image.parent != null) {
                            image.setImageBitmap(preview)
                        } else preview?.recycle()
                    }
                }
                sharkThumbs.forEach { image ->
                    val bitmap = GraphicFigureCatalog.loadPreview(
                        applicationContext, PlaybackThemeId.CYBER_SHARK,
                    )
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && image.parent != null) {
                            image.setImageBitmap(bitmap)
                        } else bitmap?.recycle()
                    }
                }
            }
        }

        PlaybackThemeRegistry.all.forEach { spec ->
            // Never delete saved Panther source data. It is only hidden from
            // the selectable gallery while its built-in graphic is blank.
            // Cyber Shark now has the same numbered tile as external Heroes.
            if (spec.id == PlaybackThemeId.CYBER_PANTHER ||
                spec.id == PlaybackThemeId.CYBER_SHARK
            ) return@forEach
            val installed = GraphicFigureCatalog.isInstalled(this, spec.id)
            val figure = isLayeredBoardTheme(spec.id)
            // Hidden from the selector ONLY. Never delete metadata, sources,
            // saved presets or existing imported external PNGs.
            // This also means installing a prepared pack later automatically
            // restores its tile on the next Theme Picker visit.
            if (figure && !installed) return@forEach
            val implemented = installed && (
                spec.id == PlaybackThemeId.VISUALIZER ||
                    figure ||
                    isStandaloneHeroTheme(spec.id)
            )

            val selected =
                spec.id == selectedThemeId

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    if (figure) dp(9) else dp(16),
                    if (figure) dp(9) else dp(14),
                    if (figure) dp(9) else dp(16),
                    if (figure) dp(9) else dp(14),
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

                if (figure) {
                    val image = ImageView(this@MainActivity).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = false
                    }
                    addView(image, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(126),
                    ).apply { bottomMargin = dp(8) })
                    if (installed) thread(name = "faric-hero-tile") {
                        val preview = runCatching {
                            GraphicFigureCatalog.loadPreview(applicationContext, spec.id)
                        }.getOrNull()
                        runOnUiThread {
                            if (!isDestroyed && image.parent != null) {
                                image.setImageBitmap(preview)
                            } else {
                                preview?.recycle()
                            }
                        }
                    }
                }
                addView(
                    label(
                        buildString {
                            if (selected && !figure) append("✓  ")
                            append(spec.title)
                            if (!figure && !installed) append("  · ПАКЕТ")
                            else if (!implemented) append("  · СКОРО")
                        },
                        if (figure) 16f else 18f,
                        Color.WHITE,
                        true,
                    ).apply {
                        if (figure) {
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            minHeight = dp(40)
                        }
                    },
                )

                addView(
                    label(
                        spec.subtitle,
                        if (figure) 11f else 12f,
                        COLOR_MUTED,
                        false,
                    ).apply {
                        if (figure) {
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            minHeight = dp(35)
                        }
                    },
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
                                when {
                                    selected -> "✓ Обрано"
                                    !installed -> "Потрібен пакет"
                                    else -> "Обрати"
                                }

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
                    if (!installed) {
                        toast("Потрібен підготовлений пакет графіки героїв")
                        return@setOnClickListener
                    }
                    if (!implemented) {
                        toast("${spec.title}: ще будуємо")
                        return@setOnClickListener
                    }

                    // Canonical live switch: no full player rebuild for any
                    // implemented theme (Shark/Panther/Neon/Energy/Orbital/
                    // Star/Wave/Vinyl/Cassette/projectM).
                    if (retainedHost != null &&
                        themeMenuOverlay != null &&
                        latestSnapshot.trackName != null &&
                        (isLayeredBoardTheme(spec.id) ||
                            isStandaloneHeroTheme(spec.id) ||
                            spec.id == PlaybackThemeId.VISUALIZER)
                    ) {
                        val requestedTheme = spec.id
                        if (requestedTheme == selectedThemeId) {
                            showNowPlaying()
                        } else if (!isLayeredBoardTheme(requestedTheme)) {
                            // Canvas themes have no PNG decode; just change
                            // the theme on the existing HeroThemeView.
                            val startNs = System.nanoTime()
                            heroAssetLoadGeneration++
                            activateLivePlaybackTheme(requestedTheme)
                            themeMenuLiveHeroSwitch = true
                            showNowPlaying()
                            Log.i(
                                "FARIC-nav",
                                "Live theme: $requestedTheme; UIApplyAndReturn=" +
                                    ((System.nanoTime() - startNs) / 1_000_000L) +
                                    "ms; native projectM retained",
                            )
                        } else {
                            // Only bitmap-based GF themes need background PNG
                            // decode; apply preloaded images to existing view.
                            val generation = ++heroAssetLoadGeneration
                            val requestedOverlay = themeMenuOverlay
                            thread(name = "faric-hero-assets") {
                                val loadStartNs = System.nanoTime()
                                val assets = runCatching {
                                    GraphicFigureCatalog.loadAssets(
                                        applicationContext,
                                        requestedTheme,
                                    )
                                }.getOrNull()
                                val loadMs =
                                    (System.nanoTime() - loadStartNs) / 1_000_000L
                                runOnUiThread {
                                    if (isFinishing || isDestroyed ||
                                        heroAssetLoadGeneration != generation ||
                                        screen != Screen.THEME_PICKER ||
                                        themeMenuOverlay !== requestedOverlay
                                    ) return@runOnUiThread
                                    if (assets == null) {
                                        toast("Не вдалося завантажити тему")
                                        return@runOnUiThread
                                    }
                                    val applyStartNs = System.nanoTime()
                                    activateLivePlaybackTheme(
                                        requestedTheme,
                                        preloaded = assets,
                                    )
                                    themeMenuLiveHeroSwitch = true
                                    showNowPlaying()
                                    Log.i(
                                        "FARIC-nav",
                                        "Live GF: $requestedTheme; " +
                                            "assetDecode=${loadMs}ms; " +
                                            "UIApplyAndReturn=" +
                                            ((System.nanoTime() - applyStartNs) / 1_000_000L) +
                                            "ms; native projectM retained",
                                    )
                                }
                            }
                        }
                        return@setOnClickListener
                    }

                    selectedThemeId = spec.id
                    themeStore.selectedThemeId = spec.id
                    if (isLayeredBoardTheme(spec.id)) {
                        setGraphicFigureThemeId(spec.id)
                    }
                    if (latestSnapshot.trackName != null) {
                        showNowPlaying()
                    } else {
                        showThemePicker()
                    }
                }
            }

            if (figure) {
                if (heroCount % 2 == 0) {
                    heroRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }
                    heroGrid.addView(heroRow)
                }
                heroRow?.addView(
                    card,
                    LinearLayout.LayoutParams(
                        0, dp(248), 1f,
                    ).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) },
                )
                heroCount++
            } else {
                content.addView(
                    card,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = dp(10) },
                )
            }
        }

        scroll.addView(content)
        root.addView(scroll)

        if (retainedHost != null) {
            retainedHost.addView(
                root,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            themeMenuOverlay = root
            Log.i("FARIC-nav", "Theme menu opened; native GL retained")
        } else {
            setContentView(root)
        }
        restorePendingScrollPositions()
        enableImmersiveFullscreen()
    }

    private fun isLayeredBoardTheme(
        id: PlaybackThemeId,
    ): Boolean = id in GraphicFigureCatalog.ids

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
            navItem("⌕", "Пошук", false) { showAllTracks(focusSearch = true) },
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

    /** Export Lab button (V57): text wraps and the button grows, never clipped; ≥ 54 dp. */
    private fun exportPill(text: String, accent: Boolean, action: () -> Unit): TextView =
        actionPill(text, accent, action).apply { minimumHeight = dp(54) }

    private fun actionPill(text: String, accent: Boolean, action: () -> Unit): TextView =
        label(text, 16f, if (accent) Color.BLACK else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            // Compact selector chips can remain 42dp; primary actions use
            // WRAP_CONTENT plus their own >= 48dp minimum.
            minimumHeight = dp(42)
            breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
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

    private fun showSystemBarsSettingsDialog() {
        val current = PulseDeckSystemBars.alwaysVisible(this)
        PulseDeckDialogs.showSingleChoice(
            context = this,
            title = "Системні панелі Android",
            labels = listOf(
                "Завжди показувати верхню та нижню панелі",
                "На весь екран, як раніше",
            ),
            checkedIndex = if (current) 0 else 1,
            cancelLabel = "Скасувати",
        ) { which ->
            PulseDeckSystemBars.setAlwaysVisible(this, which == 0)
            // Do not rebuild projectM, player, HUD, or Board just to show
            // Android's native status and navigation bars.
            PulseDeckSystemBars.apply(this)
            window.decorView.requestApplyInsets()
            toast(if (which == 0) "Панелі Android завжди видимі"
                  else "Повернено повноекранний режим")
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

    /**
     * releaseProjectMBlocking() waited on GLThread on the Android UI thread
     * for up to 1.5 seconds on every Board ↔ Player rebuild. The native bridge
     * is a singleton, so a new view may only be installed AFTER old GL release.
     *
     * Queue the native release on the existing GL thread, keep the currently
     * visible screen interactive, then render the last requested destination.
     * Other screen types retain their prior handling until separately audited.
     */
    private fun deferMainProjectMRebuild(
        target: Screen,
    ): Boolean {
        if (projectMRebuildPending) {
            projectMRebuildTarget = target
            return true
        }

        val oldView = projectMMainView ?: return false
        if (projectMOpenPending) return true

        projectMRebuildPending = true
        projectMRebuildTarget = target
        val startNs = System.nanoTime()
        Log.i("FARIC-nav", "Board/player GL release queued → $target")

        oldView.releaseProjectMThen {
            if (projectMMainView === oldView) {
                projectMMainView = null
                if (projectMMainResumed) {
                    oldView.onPause()
                    projectMMainResumed = false
                }
            }

            projectMRebuildPending = false
            val destination = projectMRebuildTarget
            projectMRebuildTarget = null

            Log.i(
                "FARIC-nav",
                "Board/player GL released in " +
                    ((System.nanoTime() - startNs) / 1_000_000L) + " ms",
            )
            if (!isFinishing && !isDestroyed) {
                when (destination) {
                    Screen.NOW_PLAYING -> showNowPlaying()
                    Screen.BOARD_TRANSFORM -> showBoardTransform()
                    else -> Unit
                }
            }
        }
        return true
    }

    private fun clearScreenRefs() {
        projectMFocusHandler.removeCallbacks(projectMFocusAutoTick)
        projectMFocusCatalogGeneration++
        projectMFocusOverlay = null
        projectMFocusStatus = null
        projectMFocusAutoControl = null
        projectMFocusBackgroundControl = null
        projectMFocusCenterControl = null
        projectMFocusEdgeControl = null
        projectMFocusSavedVisibility = null
        projectMFocusPresets = emptyList()
        boardMenuOverlay = null
        themeMenuOverlay = null
        mediaMenuOverlay = null
        themeMenuOriginalTheme = null
        themeMenuLiveHeroSwitch = false
        heroAssetLoadGeneration++
        persistentSceneRoot = null
        exportLabOverlay = null
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

            Screen.TRACKS ->
                showAllTracks(category = musicBrowseCategory, selectedGroup = musicBrowseGroup, restoredQuery = musicBrowseQuery)

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

    private fun showProjectMSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).show(
            WindowInsetsCompat.Type.systemBars(),
        )
        projectMFocusOverlay?.let(ViewCompat::requestApplyInsets)
    }

    /** Home and media browser intentionally keep Android status + navigation bars visible. */
    private fun showMediaLibrarySystemBars() {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                show(WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            window.decorView.requestApplyInsets()
        }.onFailure { error ->
            Log.e("FARIC-library", "Could not show Android system bars", error)
        }
    }

    private fun enableImmersiveFullscreen() {
        // Legacy callers (Board, Theme, Export and live player) now respect
        // the shared default-visible preference instead of forcing immersive.
        PulseDeckSystemBars.apply(this)
    }

    private fun applySafeArea(root: View, includeSystemBars: Boolean = false) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            runCatching {
                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
                val bars = if (includeSystemBars || PulseDeckSystemBars.alwaysVisible(this)) {
                    insets.getInsets(WindowInsetsCompat.Type.systemBars())
                } else {
                    androidx.core.graphics.Insets.NONE
                }
                val keyboard = if (includeSystemBars || PulseDeckSystemBars.alwaysVisible(this)) {
                    insets.getInsets(WindowInsetsCompat.Type.ime())
                } else {
                    androidx.core.graphics.Insets.NONE
                }
                view.setPadding(
                    maxOf(cutout.left, bars.left),
                    maxOf(cutout.top, bars.top),
                    maxOf(cutout.right, bars.right),
                    maxOf(cutout.bottom, gestures.bottom, bars.bottom, keyboard.bottom),
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

        showCopyableReport("Експорт завершено", message)
    }

    /** Export report with «Копіювати текст»; it comes back after a rotation (V49). */
    private fun showCopyableReport(
        title: String,
        message: String,
    ) {
        val reportDialog =
            PulseDeckDialogs.showMessage(
                context = this,
                title = title,
                message = message,
                primaryLabel = "OK",
                secondaryLabel = "Копіювати текст",
                scrollableMessage = true,
                keepOpenOnSecondary = true,
            ) {
                val clipboard =
                    getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE,
                    ) as
                        android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData
                        .newPlainText(
                            "PulseDeck export timing",
                            message,
                        ),
                )
                toast(
                    "Текст скопійовано",
                )
            }
        rememberWindow(reportDialog, RestorableWindow(WINDOW_EXPORT_REPORT, title, message, ""))
    }

    // ---- V49: important windows come back after Android re-creates the screen ----------
    // (faric: «відкрите вікно закривається при повороті»). Saved in onSaveInstanceState,
    // re-opened in onCreate; a window the user closed is forgotten.
    private data class RestorableWindow(
        val kind: String,
        val title: String,
        val message: String,
        val extra: String,
    )

    private var restorableWindow: RestorableWindow? = null
    private var lastBatchTreeUri: Uri? = null

    private fun rememberWindow(
        dialog: android.app.Dialog,
        window: RestorableWindow,
    ) {
        restorableWindow = window
        dialog.setOnDismissListener {
            if (restorableWindow === window) restorableWindow = null
        }
    }

    private fun reopenWindow(window: RestorableWindow) {
        when (window.kind) {
            WINDOW_EXPORT_REPORT -> showCopyableReport(window.title, window.message)
            WINDOW_BATCH_SUMMARY -> showBatchSummaryWindow(window.title, window.message, window.extra)
            WINDOW_BATCH_CONFIRM -> {
                val uri = window.extra.substringBeforeLast('|')
                val testMode = window.extra.substringAfterLast('|') == "true"
                if (uri.isNotBlank()) onBatchFolderPicked(Uri.parse(uri), testMode)
            }
        }
    }

    private fun showBatchSummaryWindow(
        title: String,
        summary: String,
        relativePath: String,
    ) {
        // V63 (faric 19.10 23:14: «у папці жодного файла»): the run folder opens in one tap.
        val summaryDialog =
            PulseDeckDialogs.show(
                context = this,
                title = title,
                message = summary,
                actions =
                    listOf(
                        PulseDeckDialogs.Action(label = "Відкрити папку з відео", accent = true, dismissOnClick = false) {
                            openExportFolder(relativePath.trimEnd('/'))
                        },
                        PulseDeckDialogs.Action(label = "Копіювати", dismissOnClick = false) {
                            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PulseDeck пакет", summary))
                            toast("Текст скопійовано")
                        },
                        PulseDeckDialogs.Action(label = "OK") {},
                    ),
                scrollableMessage = true,
            )
        rememberWindow(summaryDialog, RestorableWindow(WINDOW_BATCH_SUMMARY, title, summary, relativePath))
    }
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** Hint that stays ~3.5 s (V59: «натиснув сіру кнопку — підказка на 3–4 с»). */
    private fun longToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_OPEN_SCREEN = "faric.open.screen"
        private const val KEY_EXPORT_HUD_IN_VIDEO = "hud_in_video"
        private const val KEY_RESTORE_WINDOW_KIND = "restore_window_kind"
        private const val KEY_RESTORE_WINDOW_TITLE = "restore_window_title"
        private const val KEY_RESTORE_WINDOW_MESSAGE = "restore_window_message"
        private const val KEY_RESTORE_WINDOW_EXTRA = "restore_window_extra"
        private const val WINDOW_EXPORT_REPORT = "export_report"
        private const val WINDOW_BATCH_SUMMARY = "batch_summary"
        private const val WINDOW_BATCH_CONFIRM = "batch_confirm"
        private const val EXPORT_PREVIEW_DIM_TAG = "export_projectm_dim"
        private const val BATCH_REQUEST_PREFS = "faric_batch_request_v1"
        private const val KEY_BATCH_TEST_MODE = "test_mode"
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
        private const val KEY_PROJECTM_COMPONENTS_MIGRATED =
            "faric.projectm_components_migrated_v3"
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
