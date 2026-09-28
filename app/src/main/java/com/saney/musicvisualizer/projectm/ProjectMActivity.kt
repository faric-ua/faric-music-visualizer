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
import kotlin.concurrent.thread

class ProjectMActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var root: FrameLayout
    private lateinit var status: TextView
    private var projectMView: ProjectMView? = null
    private var downloadRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            setPadding(dp(8), dp(8), dp(8), dp(12))
            setBackgroundColor(Color.argb(155, 0, 0, 0))

            addView(control("TEST 40") { launchTestPack() })
            addView(control("ВСІ") { launchAllPresets() })
            addView(control("NEXT") { projectMView?.queueEvent { ProjectMBridge.nextPreset() } })
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
            controls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        setContentView(root)
        enableFullscreen()

        val state = ProjectMLibraryManager.state(this)
        if (state.installed) {
            status.text = "projectM · бібліотека ${state.presetCount} · TEST ${state.testPresetCount}"
            launchTestPack()
        } else {
            val fallback = ProjectMAssets.prepare(this)
            showProjectM(
                presetDir = fallback,
                textureDir = ProjectMLibraryManager.textureDir(this),
            )
            status.text = "projectM · fallback preset-и · завантажую повну бібліотеку…"
            installFullLibrary()
        }
    }

    override fun onResume() {
        super.onResume()
        projectMView?.onResume()
        enableFullscreen()
    }

    override fun onPause() {
        projectMView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        projectMView?.releaseProjectM()
        projectMView = null
        super.onDestroy()
    }

    private fun installFullLibrary() {
        if (downloadRunning) return
        downloadRunning = true

        thread(name = "projectm-library-install") {
            try {
                val state = ProjectMLibraryManager.installAll(this) { message ->
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
                        "projectM · бібліотека ${state.presetCount} · TEST ${state.testPresetCount}"
                    launchTestPack()
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

    private fun launchTestPack() {
        val state = ProjectMLibraryManager.state(this)

        if (!state.installed) {
            status.text = "Спочатку встановлюю повну бібліотеку…"
            installFullLibrary()
            return
        }

        if (state.testPresetCount < 40) {
            ProjectMLibraryManager.buildTestPack(this)
        }

        showProjectM(
            presetDir = ProjectMLibraryManager.testPresetDir(this),
            textureDir = ProjectMLibraryManager.textureDir(this),
        )

        val updated = ProjectMLibraryManager.state(this)
        status.text =
            "BG projectM + FG FARIC · TEST ${updated.testPresetCount} · tap/NEXT"
    }

    private fun launchAllPresets() {
        val state = ProjectMLibraryManager.state(this)

        if (!state.installed) {
            status.text = "Завантажую повну бібліотеку…"
            installFullLibrary()
            return
        }

        showProjectM(
            presetDir = ProjectMLibraryManager.fullPresetDir(this),
            textureDir = ProjectMLibraryManager.textureDir(this),
        )

        status.text =
            "BG projectM + FG FARIC · ВСІ ${state.presetCount} · tap/NEXT"
    }

    private fun showProjectM(
        presetDir: java.io.File,
        textureDir: java.io.File,
    ) {
        val old = projectMView

        if (old != null) {
            old.onPause()
            old.releaseProjectM()
            root.removeView(old)
        }

        val profile = ProjectMPerformanceProfile.BALANCED_BACKGROUND
        val view = ProjectMView(
            context = this,
            presetDirectory = presetDir,
            textureDirectory = textureDir,
            profile = profile,
        )

        projectMView = view

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
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { action() }
            setBackgroundColor(Color.argb(150, 15, 22, 31))

            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(48),
                1f,
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
