package com.saney.musicvisualizer

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.saney.musicvisualizer.ui.PulseDeckCenterCalibrationOverlayView
import com.saney.musicvisualizer.ui.PulseDeckMainSkinView

/**
 * Temporary engineering page used to calibrate the exact centers of the
 * permanent PulseDeck main-page objects on the real phone viewport.
 */
class PulseDeckCenterCalibrationActivity :
    ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(
            savedInstanceState,
        )

        enableImmersiveFullscreen()

        val root =
            FrameLayout(this)

        val skin =
            PulseDeckMainSkinView(this).apply {
                setPlaying(true)
                setPlaybackContent(
                    title =
                        "Neon Heartbeat (3).mp3",
                    artist =
                        "Невідомий виконавець",
                    status =
                        "Відтворення · reactive ON · PulseDeck HUD",
                    elapsed =
                        "0:39",
                    total =
                        "3:39",
                    progressFraction =
                        0.18f,
                )
                setControlsVisible(
                    visible = true,
                    animate = false,
                )
            }

        root.addView(
            skin,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        root.addView(
            PulseDeckCenterCalibrationOverlayView(
                context = this,
                sourceView = skin,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        setContentView(root)
    }

    private fun enableImmersiveFullscreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
}
