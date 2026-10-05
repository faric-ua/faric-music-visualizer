package com.saney.musicvisualizer.projectm

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    private val manualFrameMode: Boolean = false,
) : GLSurfaceView(context) {

    @Volatile
    private var glWidth = 0

    @Volatile
    private var glHeight = 0

    // Offline export reuses these large buffers across frames. At 1080p-class
    // projectM sizes, allocating them for every frame creates severe GC pressure.
    private var offlineReadbackBuffer: ByteBuffer? =
        null
    private var offlineReadbackBitmap: Bitmap? =
        null
    private var offlineReadbackWidth =
        0
    private var offlineReadbackHeight =
        0

    init {
        setEGLContextClientVersion(2)
        setRenderer(
            Renderer(
                presetPath = initialPreset.absolutePath,
                texturePath = textureDirectory.absolutePath,
                profile = profile,
                foregroundSample = foregroundSample,
                manualFrameMode = manualFrameMode,
                onSurfaceSize = { width, height ->
                    glWidth = width
                    glHeight = height
                },
            ),
        )
        renderMode =
            if (manualFrameMode) {
                RENDERMODE_WHEN_DIRTY
            } else {
                RENDERMODE_CONTINUOUSLY
            }
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

    fun loadPreset(
        file: File,
        smoothTransition: Boolean = true,
        onLoaded: ((Long) -> Unit)? = null,
    ) {
        if (!file.isFile) return

        queueEvent {
            val loadMs =
                ProjectMBridge.loadPreset(
                    path = file.absolutePath,
                    smoothTransition = smoothTransition,
                )

            if (onLoaded != null) {
                post {
                    onLoaded(loadMs)
                }
            }
        }
    }

    fun setForegroundSample(sample: FaricForegroundSample) {
        foregroundSample = sample
        ProjectMBridge.setForegroundSample(sample)
    }

    fun releaseProjectM() {
        queueEvent {
            ProjectMBridge.destroy()
            clearOfflineReadbackCache()
        }
    }

    fun releaseProjectMBlocking(
        timeoutMs: Long = 1_500L,
    ): Boolean {
        val latch =
            CountDownLatch(
                1,
            )

        queueEvent {
            ProjectMBridge.destroy()
            clearOfflineReadbackCache()
            latch.countDown()
        }

        return runCatching {
            latch.await(
                timeoutMs,
                TimeUnit.MILLISECONDS,
            )
        }.getOrDefault(
            false,
        )
    }

    fun captureFrame(
        onCaptured: (Bitmap?) -> Unit,
    ) {
        val width =
            glWidth
        val height =
            glHeight

        if (
            width <= 0 ||
            height <= 0
        ) {
            onCaptured(
                null,
            )
            return
        }

        queueEvent {
            val result =
                readFramebuffer(
                    width = width,
                    height = height,
                    renderFirst = true,
                )

            post {
                onCaptured(
                    result,
                )
            }
        }
    }

    /**
     * Captures the currently rendered projectM framebuffer from a background
     * export thread. The GLSurfaceView keeps rendering continuously; this call
     * only samples the GL framebuffer and waits for that sample to complete.
     */
    fun captureFrameBlocking(
        timeoutMs: Long = 1_500L,
    ): Bitmap? {
        val width =
            glWidth
        val height =
            glHeight

        if (
            width <= 0 ||
            height <= 0
        ) {
            return null
        }

        val latch =
            CountDownLatch(
                1,
            )

        var result: Bitmap? =
            null

        queueEvent {
            result =
                readFramebuffer(
                    width = width,
                    height = height,
                    renderFirst = false,
                )
            latch.countDown()
        }

        val completed =
            runCatching {
                latch.await(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )
            }.getOrDefault(
                false,
            )

        return if (completed) {
            result
        } else {
            null
        }
    }

    fun awaitReadyBlocking(
        timeoutMs: Long = 4_000L,
    ): Boolean {
        val started =
            System.nanoTime()

        while (
            glWidth <= 0 ||
            glHeight <= 0
        ) {
            if (
                (
                    System.nanoTime() -
                        started
                    ) /
                    1_000_000L >=
                timeoutMs
            ) {
                return false
            }

            Thread.sleep(
                10L,
            )
        }

        return true
    }

    fun resetOfflineRendererBlocking(
        timeoutMs: Long = 4_000L,
    ): Boolean {
        val width =
            glWidth
        val height =
            glHeight

        if (
            width <= 0 ||
            height <= 0
        ) {
            return false
        }

        val latch =
            CountDownLatch(
                1,
            )

        queueEvent {
            ProjectMBridge.destroy()
            ProjectMBridge.beginOfflineExport()
            ProjectMBridge.create(
                width = width,
                height = height,
                presetPath =
                    initialPreset.absolutePath,
                texturePath =
                    textureDirectory.absolutePath,
                profile =
                    profile,
            )
            ProjectMBridge
                .enableAutoPresetSwitching(
                    false,
                )
            ProjectMBridge
                .setForegroundSample(
                    foregroundSample,
                )
            ProjectMBridge
                .setFrameTime(
                    0.0,
                )
            latch.countDown()
        }

        return runCatching {
            latch.await(
                timeoutMs,
                TimeUnit.MILLISECONDS,
            )
        }.getOrDefault(
            false,
        )
    }

    fun renderOfflineFrameBlocking(
        frameTimeSeconds: Double,
        pcm: ShortArray,
        signal: com.saney.musicvisualizer.analysis.SceneSignal,
        timeoutMs: Long = 4_000L,
    ): Bitmap? {
        val width =
            glWidth
        val height =
            glHeight

        if (
            width <= 0 ||
            height <= 0
        ) {
            return null
        }

        val latch =
            CountDownLatch(
                1,
            )

        var result: Bitmap? =
            null

        queueEvent {
            ProjectMBridge.beginOfflineExport()
            ProjectMBridge.setFrameTime(
                frameTimeSeconds,
            )
            ProjectMBridge.pushOfflinePcm(
                pcm,
            )
            ProjectMBridge.pushOfflineSignal(
                signal,
            )

            result =
                readFramebuffer(
                    width = width,
                    height = height,
                    renderFirst = true,
                    reuseOfflineBuffers = true,
                )

            latch.countDown()
        }

        val completed =
            runCatching {
                latch.await(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )
            }.getOrDefault(
                false,
            )

        return if (completed) {
            result
        } else {
            null
        }
    }

    fun finishOfflineExport() {
        queueEvent {
            ProjectMBridge.endOfflineExport()
        }
    }

    private fun readFramebuffer(
        width: Int,
        height: Int,
        renderFirst: Boolean,
        reuseOfflineBuffers: Boolean = false,
    ): Bitmap? =
        runCatching {
            if (renderFirst) {
                ProjectMBridge.render()
            }

            val pixelCount =
                width *
                    height

            val buffer =
                if (reuseOfflineBuffers) {
                    ensureOfflineReadbackCache(
                        width = width,
                        height = height,
                    )
                    offlineReadbackBuffer
                        ?: error(
                            "Offline readback buffer unavailable",
                        )
                } else {
                    ByteBuffer
                        .allocateDirect(
                            pixelCount *
                                4,
                        )
                        .order(
                            ByteOrder.nativeOrder(),
                        )
                }

            buffer.position(
                0,
            )

            if (!reuseOfflineBuffers) {
                // Legacy snapshot path keeps the explicit finish + CPU channel
                // conversion so PNG/static captures preserve their established
                // orientation and color contract.
                GLES20.glFinish()
            }

            // glReadPixels is synchronous for the requested framebuffer data,
            // so the offline hot path does not need a separate glFinish().
            GLES20.glReadPixels(
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buffer,
            )

            if (reuseOfflineBuffers) {
                val bitmap =
                    offlineReadbackBitmap
                        ?: error(
                            "Offline readback bitmap unavailable",
                        )

                // arm64 Android stores ARGB_8888 as native little-endian bytes.
                // A direct RGBA copy therefore avoids the previous Kotlin
                // per-pixel loop. The resulting offline frame is GL-oriented
                // (bottom-up) with R/B swapped; CompositionExportRenderer fixes
                // those two presentation details while drawing the frame.
                buffer.position(
                    0,
                )
                bitmap.copyPixelsFromBuffer(
                    buffer,
                )
                bitmap
            } else {
                val pixels =
                    IntArray(
                        pixelCount,
                    )

                for (
                    y in
                    0 until height
                ) {
                    val sourceY =
                        height -
                            1 -
                            y

                    for (
                        x in
                        0 until width
                    ) {
                        val sourceIndex =
                            (
                                sourceY *
                                    width +
                                    x
                                ) *
                                4

                        val r =
                            buffer
                                .get(
                                    sourceIndex,
                                )
                                .toInt() and
                                0xff
                        val g =
                            buffer
                                .get(
                                    sourceIndex +
                                        1,
                                )
                                .toInt() and
                                0xff
                        val b =
                            buffer
                                .get(
                                    sourceIndex +
                                        2,
                                )
                                .toInt() and
                                0xff
                        val a =
                            buffer
                                .get(
                                    sourceIndex +
                                        3,
                                )
                                .toInt() and
                                0xff

                        pixels[
                            y *
                                width +
                                x
                        ] =
                            (
                                a shl 24
                                ) or
                                (
                                    r shl 16
                                    ) or
                                (
                                    g shl 8
                                    ) or
                                b
                    }
                }

                Bitmap.createBitmap(
                    pixels,
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
            }
        }
            .getOrNull()

    private fun ensureOfflineReadbackCache(
        width: Int,
        height: Int,
    ) {
        if (
            offlineReadbackWidth ==
                width &&
            offlineReadbackHeight ==
                height &&
            offlineReadbackBuffer !=
                null &&
            offlineReadbackBitmap
                ?.isRecycled ==
                false
        ) {
            return
        }

        clearOfflineReadbackCache()

        offlineReadbackBuffer =
            ByteBuffer
                .allocateDirect(
                    width *
                        height *
                        4,
                )
                .order(
                    ByteOrder.nativeOrder(),
                )
        offlineReadbackBitmap =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888,
            )
        offlineReadbackWidth =
            width
        offlineReadbackHeight =
            height
    }

    private fun clearOfflineReadbackCache() {
        offlineReadbackBitmap
            ?.takeIf {
                !it.isRecycled
            }
            ?.recycle()
        offlineReadbackBitmap =
            null
        offlineReadbackBuffer =
            null
        offlineReadbackWidth =
            0
        offlineReadbackHeight =
            0
    }

    private class Renderer(
        private val presetPath: String,
        private val texturePath: String,
        private val profile: ProjectMPerformanceProfile,
        private val foregroundSample: FaricForegroundSample,
        private val manualFrameMode: Boolean,
        private val onSurfaceSize: (Int, Int) -> Unit,
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
            onSurfaceSize(
                width,
                height,
            )

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
            if (manualFrameMode) {
                return
            }

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
