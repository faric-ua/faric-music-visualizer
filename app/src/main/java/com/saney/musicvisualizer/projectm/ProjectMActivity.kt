package com.saney.musicvisualizer.projectm

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class ProjectMActivity : ComponentActivity() {
    private lateinit var projectMView: ProjectMView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val presets = ProjectMAssets.prepare(this)
        projectMView = ProjectMView(this, presets)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                projectMView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )

            addView(
                TextView(this@ProjectMActivity).apply {
                    text = "projectM LAB  •  торкнись екрана = наступний preset"
                    textSize = 12f
                    setTextColor(Color.argb(190, 255, 255, 255))
                    setPadding(24, 16, 24, 16)
                    setBackgroundColor(Color.argb(95, 0, 0, 0))
                    gravity = Gravity.CENTER
                },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP,
                ),
            )
        }

        setContentView(root)
        enableFullscreen()
    }

    override fun onResume() {
        super.onResume()
        projectMView.onResume()
        enableFullscreen()
    }

    override fun onPause() {
        projectMView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        projectMView.releaseProjectM()
        super.onDestroy()
    }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
