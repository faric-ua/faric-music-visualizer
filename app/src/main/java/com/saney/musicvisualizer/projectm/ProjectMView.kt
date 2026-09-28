package com.saney.musicvisualizer.projectm

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ProjectMView(
    context: Context,
    private val presetDirectory: File,
    private val textureDirectory: File,
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(
            Renderer(
                presetPath = presetDirectory.absolutePath,
                texturePath = textureDirectory.absolutePath,
            ),
        )
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            queueEvent { ProjectMBridge.nextPreset() }
            return true
        }
        return true
    }

    fun releaseProjectM() {
        queueEvent { ProjectMBridge.destroy() }
    }

    private class Renderer(
        private val presetPath: String,
        private val texturePath: String,
    ) : GLSurfaceView.Renderer {
        private var created = false

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            created = false
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            if (!created) {
                ProjectMBridge.create(
                    width = width,
                    height = height,
                    presetPath = presetPath,
                    texturePath = texturePath,
                )
                created = true
            } else {
                ProjectMBridge.resize(width, height)
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            ProjectMBridge.render()
        }
    }
}
