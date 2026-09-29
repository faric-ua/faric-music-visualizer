package com.saney.musicvisualizer.projectm

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import kotlin.concurrent.thread

class ProjectMActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var root: FrameLayout
    private lateinit var status: TextView
    private lateinit var stateStore: ProjectMStateStore
    private lateinit var ratingsStore: ProjectMPresetRatingsStore
    private lateinit var performanceStore: ProjectMPresetPerformanceStore

    private var projectMView: ProjectMView? = null
    private var downloadRunning = false

    private var currentForegroundSample = FaricForegroundSample.PULSE_RAYS
    private var currentBackgroundMode = ProjectMBackgroundMode.TOP
    private var autoEnabled = true

    private var displayedPreset: File? = null
    private var presetQueue: ProjectMPresetQueue? = null

    private val catalogCache =
        mutableMapOf<ProjectMBackgroundMode, List<File>>()

    private var catalogGeneration = 0

    private val autoRunnable = Runnable {
        if (autoEnabled) {
            advancePreset(manual = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateStore = ProjectMStateStore(this)
        ratingsStore = ProjectMPresetRatingsStore(this)
        performanceStore = ProjectMPresetPerformanceStore(this)
        currentForegroundSample = stateStore.foregroundSample
        currentBackgroundMode = stateStore.backgroundMode
        autoEnabled = stateStore.autoEnabled

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        status = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(145, 0, 0, 0))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(4))
            setBackgroundColor(Color.argb(155, 0, 0, 0))

            addView(control("ТОП") {
                activateMode(
                    mode = ProjectMBackgroundMode.TOP,
                    auto = true,
                )
            })

            addView(control("ВСІ") {
                activateMode(
                    mode = ProjectMBackgroundMode.ALL,
                    auto = true,
                )
            })

            addView(control("NEXT") {
                manualNext()
            })

            addView(control("FG") {
                cycleForeground()
            })
        }

        val ratingControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(2), dp(8), dp(10))
            setBackgroundColor(Color.argb(155, 0, 0, 0))

            addView(control("👍") {
                rateCurrent(ProjectMPresetRating.UP)
            })

            addView(control("👎") {
                rateCurrent(ProjectMPresetRating.DOWN)
            })

            addView(control("−") {
                rateCurrent(ProjectMPresetRating.HIDDEN)
            })
        }

        val bottomControls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                controls,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                ratingControls,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        root.addView(
            status,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP,
            ),
        )

        root.addView(
            bottomControls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        setContentView(root)
        enableFullscreen()

        val libraryState = ProjectMLibraryManager.state(this)

        if (libraryState.installed) {
            openFastFromRememberedState()
            prepareInstalledLibrary()
        } else {
            openFallbackAndInstall()
        }
    }

    override fun onResume() {
        super.onResume()
        projectMView?.onResume()
        enableFullscreen()

        if (autoEnabled && presetQueue != null) {
            scheduleAuto()
        }
    }

    override fun onPause() {
        cancelAuto()
        projectMView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        cancelAuto()
        projectMView?.releaseProjectM()
        projectMView = null
        super.onDestroy()
    }

    private fun openFastFromRememberedState() {
        val remembered =
            stateStore
                .lastPresetFileOrNull()
                ?.takeIf {
                    ratingsStore.ratingFor(it) !=
                        ProjectMPresetRating.HIDDEN
                }
        val quickFallback =
            ProjectMLibraryManager.firstIndexedPreset(this)
                ?: firstMilk(ProjectMLibraryManager.testPresetDir(this))

        val initial = remembered ?: quickFallback

        if (initial != null) {
            showProjectM(initial)
            status.text =
                "FAST START · ${currentBackgroundMode.name} · FG ${currentForegroundSample.label}"
        } else {
            status.text = "Готую projectM бібліотеку…"
        }
    }

    private fun prepareInstalledLibrary() {
        thread(name = "projectm-prepare-library") {
            try {
                ProjectMLibraryManager.ensureDerivedPacks(this)

                mainHandler.post {
                    if (isFinishing || isDestroyed) return@post

                    activateMode(
                        mode = currentBackgroundMode,
                        auto = autoEnabled,
                    )
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    if (!isFinishing && !isDestroyed) {
                        status.text =
                            "Помилка бібліотеки: ${error.message ?: error.javaClass.simpleName}"
                    }
                }
            }
        }
    }

    private fun openFallbackAndInstall() {
        val fallbackDir = ProjectMAssets.prepare(this)
        val fallback = firstMilk(fallbackDir)

        if (fallback != null) {
            showProjectM(fallback)
        }

        status.text =
            "projectM · fallback · завантажую повну бібліотеку…"

        installFullLibrary()
    }

    private fun installFullLibrary() {
        if (downloadRunning) return
        downloadRunning = true

        thread(name = "projectm-library-install") {
            try {
                val state =
                    ProjectMLibraryManager.installAll(this) { message ->
                        mainHandler.post {
                            if (!isFinishing && !isDestroyed) {
                                status.text = "projectM · $message"
                            }
                        }
                    }

                mainHandler.post {
                    downloadRunning = false
                    if (isFinishing || isDestroyed) return@post

                    status.text =
                        "Бібліотека ${state.presetCount} · ТОП ${state.topPresetCount}"

                    activateMode(
                        mode = ProjectMBackgroundMode.TOP,
                        auto = true,
                    )
                }
            } catch (error: Throwable) {
                mainHandler.post {
                    downloadRunning = false

                    if (!isFinishing && !isDestroyed) {
                        status.text =
                            "Помилка бібліотеки: ${error.message ?: error.javaClass.simpleName}"
                    }
                }
            }
        }
    }

    private fun activateMode(
        mode: ProjectMBackgroundMode,
        auto: Boolean,
    ) {
        currentBackgroundMode = mode
        autoEnabled = auto

        stateStore.backgroundMode = mode
        stateStore.autoEnabled = auto

        cancelAuto()

        val cached = catalogCache[mode]

        if (cached != null && cached.isNotEmpty()) {
            prepareQueueAsync(
                mode = mode,
                pool = cached,
                auto = auto,
            )
            return
        }

        val generation = ++catalogGeneration

        status.text =
            "INDEX · ${mode.name} · FG ${currentForegroundSample.label}"

        thread(name = "projectm-index-${mode.name.lowercase()}") {
            val sourcePool =
                when (mode) {
                    ProjectMBackgroundMode.TOP ->
                        ProjectMLibraryManager.fastPresetFiles(this)

                    ProjectMBackgroundMode.ALL ->
                        ProjectMLibraryManager.allPresetFiles(this)
                }

            val pool =
                if (mode == ProjectMBackgroundMode.TOP) {
                    sourcePool.filterNot {
                        performanceStore.isHeavy(it)
                    }
                } else {
                    sourcePool
                }

            mainHandler.post {
                if (
                    isFinishing ||
                    isDestroyed ||
                    generation != catalogGeneration
                ) {
                    return@post
                }

                catalogCache[mode] = pool

                prepareQueueAsync(
                    mode = mode,
                    pool = pool,
                    auto = auto,
                )
            }
        }
    }

    private fun prepareQueueAsync(
        mode: ProjectMBackgroundMode,
        pool: List<File>,
        auto: Boolean,
    ) {
        if (pool.isEmpty()) {
            status.text = "Немає preset-ів у ${mode.name}"
            return
        }

        val generation = ++catalogGeneration
        val previousCurrent = presetQueue?.current
        val remembered = stateStore.lastPresetFileOrNull()

        status.text =
            "QUEUE · ${mode.name} · FG ${currentForegroundSample.label}"

        thread(name = "projectm-queue-${mode.name.lowercase()}") {
            val poolPaths =
                pool
                    .asSequence()
                    .map { it.absolutePath }
                    .toHashSet()

            val current =
                previousCurrent
                    ?.takeIf {
                        it.absolutePath in poolPaths
                    }
                    ?: remembered
                        ?.takeIf {
                            it.absolutePath in poolPaths &&
                                ratingsStore.ratingFor(it) !=
                                    ProjectMPresetRating.HIDDEN
                        }

            val queue =
                ProjectMPresetQueue(
                    pool = pool,
                    current = current,
                    ratingOf = { preset ->
                        ratingsStore.ratingFor(preset)
                    },
                )

            mainHandler.post {
                if (
                    isFinishing ||
                    isDestroyed ||
                    generation != catalogGeneration
                ) {
                    return@post
                }

                applyPreparedQueue(
                    mode = mode,
                    queue = queue,
                    auto = auto,
                )
            }
        }
    }

    private fun applyPreparedQueue(
        mode: ProjectMBackgroundMode,
        queue: ProjectMPresetQueue,
        auto: Boolean,
    ) {
        presetQueue = queue

        val selected = queue.current

        if (selected != null) {
            switchDisplayedPreset(
                file = selected,
                smoothTransition = displayedPreset != null,
            )
        }

        warmAhead(queue)

        currentBackgroundMode = mode
        autoEnabled = auto

        stateStore.backgroundMode = mode
        stateStore.autoEnabled = auto

        if (auto) {
            scheduleAuto()
        } else {
            cancelAuto()
        }

        updateStatus()
    }

    private fun manualNext() {
        if (presetQueue == null) {
            activateMode(
                mode = currentBackgroundMode,
                auto = false,
            )
            return
        }

        autoEnabled = false
        stateStore.autoEnabled = false
        cancelAuto()

        advancePreset(manual = true)
    }

    private fun advancePreset(manual: Boolean) {
        val queue = presetQueue ?: return
        val next = queue.advance() ?: return

        switchDisplayedPreset(
            file = next,
            smoothTransition = !manual,
        )

        warmAhead(queue)

        if (!manual && autoEnabled) {
            scheduleAuto()
        }

        updateStatus()
    }

    private fun warmAhead(queue: ProjectMPresetQueue) {
        val files = queue.nextAhead()

        thread(name = "projectm-prefetch") {
            ProjectMPresetQueue.warmFiles(files)
        }
    }

    private fun switchDisplayedPreset(
        file: File,
        smoothTransition: Boolean,
    ) {
        if (!file.isFile) return

        val view = projectMView

        if (view == null) {
            showProjectM(file)
        } else if (
            displayedPreset?.absolutePath != file.absolutePath
        ) {
            view.loadPreset(
                file = file,
                smoothTransition = smoothTransition,
                onLoaded = { loadMs ->
                    performanceStore.recordLoad(
                        file = file,
                        loadMs = loadMs,
                    )

                    if (
                        loadMs >=
                        ProjectMPresetPerformanceStore
                            .HEAVY_PRESET_MS
                    ) {
                        status.text =
                            "HEAVY ${loadMs}ms · ${file.nameWithoutExtension.take(28)} · у FAST буде пропущено"
                    } else {
                        updateStatus(
                            lastLoadMs = loadMs,
                        )
                    }
                },
            )

            displayedPreset = file
        }

        stateStore.lastPresetPath = file.absolutePath
    }

    private fun rateCurrent(
        rating: ProjectMPresetRating,
    ) {
        val current =
            displayedPreset
                ?: return

        ratingsStore.setRating(
            file = current,
            rating = rating,
        )

        val queue =
            presetQueue
                ?: run {
                    updateStatus()
                    return
                }

        val hidCurrent =
            queue.updateRating(
                file = current,
                rating = rating,
            )

        if (hidCurrent) {
            val next = queue.advance()

            if (next != null) {
                switchDisplayedPreset(
                    file = next,
                    smoothTransition = false,
                )
            }

            warmAhead(queue)
        }

        updateStatus()
    }

    private fun cycleForeground() {
        currentForegroundSample =
            currentForegroundSample.next()

        stateStore.foregroundSample =
            currentForegroundSample

        projectMView?.setForegroundSample(
            currentForegroundSample,
        )

        updateStatus()
    }

    private fun showProjectM(initialPreset: File) {
        val old = projectMView

        if (old != null) {
            old.onPause()
            old.releaseProjectM()
            root.removeView(old)
        }

        val profile =
            ProjectMPerformanceProfile.BALANCED_BACKGROUND

        val view =
            ProjectMView(
                context = this,
                initialPreset = initialPreset,
                textureDirectory =
                    ProjectMLibraryManager.textureDir(this),
                profile = profile,
                foregroundSample = currentForegroundSample,
                onTapNext = {
                    manualNext()
                },
            )

        projectMView = view
        displayedPreset = initialPreset
        stateStore.lastPresetPath = initialPreset.absolutePath

        root.addView(
            view,
            0,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        view.onResume()
    }

    private fun scheduleAuto() {
        cancelAuto()

        if (
            autoEnabled &&
            presetQueue != null
        ) {
            mainHandler.postDelayed(
                autoRunnable,
                AUTO_SWITCH_MS,
            )
        }
    }

    private fun cancelAuto() {
        mainHandler.removeCallbacks(autoRunnable)
    }

    private fun updateStatus(
        lastLoadMs: Long? = null,
    ) {
        val mode =
            if (autoEnabled) "AUTO"
            else "MANUAL"

        val queueSize =
            presetQueue
                ?.nextAhead()
                ?.size
                ?: 0

        val visibleCount =
            presetQueue
                ?.availableCount
                ?: 0

        val current = displayedPreset
        val rating =
            current
                ?.let {
                    ratingsStore.ratingFor(it)
                }
                ?: ProjectMPresetRating.NONE

        val presetName =
            current
                ?.nameWithoutExtension
                ?.take(28)
                ?: "—"

        val loadPart =
            lastLoadMs
                ?.takeIf { it >= 0L }
                ?.let { " · LOAD ${it}ms" }
                .orEmpty()

        val heavyCount =
            performanceStore.heavyCount()

        status.text =
            "$mode · ${currentBackgroundMode.name} $visibleCount · PRELOAD $queueSize/3 · HEAVY $heavyCount · ${ratingSymbol(rating)} $presetName$loadPart · FG ${currentForegroundSample.label}"
    }

    private fun ratingSymbol(
        rating: ProjectMPresetRating,
    ): String =
        when (rating) {
            ProjectMPresetRating.NONE -> "○"
            ProjectMPresetRating.UP -> "👍"
            ProjectMPresetRating.DOWN -> "👎"
            ProjectMPresetRating.HIDDEN -> "−"
        }

    private fun firstMilk(root: File): File? {
        if (!root.isDirectory) return null

        return root
            .walkTopDown()
            .firstOrNull {
                it.isFile &&
                    it.extension.equals(
                        "milk",
                        ignoreCase = true,
                    )
            }
    }

    private fun control(
        label: String,
        action: () -> Unit,
    ): TextView =
        TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            setPadding(
                dp(10),
                dp(10),
                dp(10),
                dp(10),
            )
            setOnClickListener {
                action()
            }
            setBackgroundColor(
                Color.argb(
                    150,
                    15,
                    22,
                    31,
                ),
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f,
                ).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
        }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false,
        )

        WindowCompat
            .getInsetsController(
                window,
                window.decorView,
            )
            .apply {
                hide(
                    WindowInsetsCompat.Type.systemBars(),
                )

                systemBarsBehavior =
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
    }

    private fun dp(value: Int): Int =
        (
            value *
                resources.displayMetrics.density
            ).toInt()

    companion object {
        private const val AUTO_SWITCH_MS = 18_000L
    }
}
