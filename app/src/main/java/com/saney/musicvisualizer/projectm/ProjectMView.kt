package com.saney.musicvisualizer.projectm

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import java.io.File
import kotlin.math.roundToInt
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ProjectMView(
    context: Context,
    private val initialPreset: File,
    private val textureDirectory: File,
    private val profile: ProjectMPerformanceProfile = ProjectMPerformanceProfile.BALANCED_BACKGROUND,
    private var foregroundSample: FaricForegroundSample = FaricForegroundSample.PULSE_RAYS,
    private val onTapNext: () -> Unit = {},
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(
            Renderer(
                presetPath = initialPreset.absolutePath,
                texturePath = textureDirectory.absolutePath,
                profile = profile,
                foregroundSample = foregroundSample,
            ),
        )
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        val scaledWidth = (w * profile.renderScale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (h * profile.renderScale).roundToInt().coerceAtLeast(1)

        post {
            holder.setFixedSize(scaledWidth, scaledHeight)
            Log.i(
                TAG,
                "surface ${scaledWidth}x${scaledHeight} from ${w}x${h} · ${profile.name}",
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            onTapNext()
            return true
        }
        return true
    }

    fun loadPreset(file: File, smoothTransition: Boolean = true) {
        if (!file.isFile) return
        queueEvent {
            ProjectMBridge.loadPreset(
                path = file.absolutePath,
                smoothTransition = smoothTransition,
            )
        }
    }

    fun setForegroundSample(sample: FaricForegroundSample) {
        foregroundSample = sample
        ProjectMBridge.setForegroundSample(sample)
    }

    fun releaseProjectM() {
        queueEvent { ProjectMBridge.destroy() }
    }

    private class Renderer(
        private val presetPath: String,
        private val texturePath: String,
        private val profile: ProjectMPerformanceProfile,
        private val foregroundSample: FaricForegroundSample,
    ) : GLSurfaceView.Renderer {
        private var created = false
        private var frameCount = 0
        private var sampleStartedNs = 0L

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            created = false
            frameCount = 0
            sampleStartedNs = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            if (!created) {
                ProjectMBridge.create(
                    width = width,
                    height = height,
                    presetPath = presetPath,
                    texturePath = texturePath,
                    profile = profile,
                )
                // FARIC owns AUTO/MANUAL timing. projectM internal switching stays locked.
                ProjectMBridge.enableAutoPresetSwitching(false)
                ProjectMBridge.setForegroundSample(foregroundSample)
                created = true
            } else {
                ProjectMBridge.resize(width, height)
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            ProjectMBridge.render()

            frameCount++
            if (frameCount >= 120) {
                val now = System.nanoTime()
                val elapsedSeconds = (now - sampleStartedNs) / 1_000_000_000.0
                if (elapsedSeconds > 0.0) {
                    val fps = frameCount / elapsedSeconds
                    Log.i(TAG, "render fps=%.1f · %s".format(fps, profile.name))
                }
                frameCount = 0
                sampleStartedNs = now
            }
        }
    }

    companion object {
        private const val TAG = "FARIC-projectM"
    }
}
