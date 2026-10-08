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

data class ProjectMOfflineTiming(
    val queueWaitMs: Long,
    val nativeRenderMs: Long,
    val readPixelsMs: Long,
    val bitmapCopyMs: Long,
)

class ProjectMOfflineFrameRequest internal constructor(
    internal val slotIndex: Int,
) {
    internal val latch =
        CountDownLatch(
            1,
        )

    @Volatile
    internal var result: Bitmap? =
        null
}

class ProjectMView(
    context: Context,
    private val initialPreset: File,
    private val textureDirectory: File,
    private val profile: ProjectMPerformanceProfile = ProjectMPerformanceProfile.BALANCED_BACKGROUND,
    private var foregroundSample: FaricForegroundSample = FaricForegroundSample.PULSE_RAYS,
    private var backgroundVisible: Boolean = true,
    private var foregroundCenterVisible: Boolean = true,
    private var foregroundEdgeFxVisible: Boolean = true,
    private var foregroundTuning: ProjectMForegroundTuning =
        ProjectMForegroundTuning.default(),
    private val onTapNext: () -> Unit = {},
    private val manualFrameMode: Boolean = false,
    private val manualRenderWidth: Int? = null,
    private val manualRenderHeight: Int? = null,
) : GLSurfaceView(context) {

    @Volatile
    private var glWidth = 0

    @Volatile
    private var glHeight = 0

    // Old SurfaceView GL threads must never touch the shared native bridge after release.
    @Volatile
    private var releaseRequested = false

    // Offline export reuses two large readback slots. While Canvas composes
    // frame N, the GL thread may render/read frame N+1 into the other slot.
    // This preserves exact pixels while overlapping GPU->CPU readback with CPU
    // composition instead of serializing both stages for every frame.
    private val offlineReadbackBuffers =
        arrayOfNulls<ByteBuffer>(
            OFFLINE_READBACK_SLOT_COUNT,
        )
    private val offlineReadbackBitmaps =
        arrayOfNulls<Bitmap>(
            OFFLINE_READBACK_SLOT_COUNT,
        )
    private var offlineReadbackWidth =
        0
    private var offlineReadbackHeight =
        0

    @Volatile
    private var offlineReadbackChannelsCorrect =
        false

    private var offlineBgraReadbackSupported:
        Boolean? =
        null

    private var offlineQueueWaitNs =
        0L
    private var offlineNativeRenderNs =
        0L
    private var offlineReadPixelsNs =
        0L
    private var offlineBitmapCopyNs =
        0L

    fun offlineReadbackChannelsAreCorrect():
        Boolean =
        offlineReadbackChannelsCorrect

    fun offlineTimingSnapshot():
        ProjectMOfflineTiming =
        ProjectMOfflineTiming(
            queueWaitMs =
                offlineQueueWaitNs /
                    1_000_000L,
            nativeRenderMs =
                offlineNativeRenderNs /
                    1_000_000L,
            readPixelsMs =
                offlineReadPixelsNs /
                    1_000_000L,
            bitmapCopyMs =
                offlineBitmapCopyNs /
                    1_000_000L,
        )

    init {
        setEGLContextClientVersion(2)
        setRenderer(
            Renderer(
                presetPath = initialPreset.absolutePath,
                texturePath = textureDirectory.absolutePath,
                profile = profile,
                foregroundSample = foregroundSample,
                backgroundVisible =
                    backgroundVisible,
                foregroundCenterVisible =
                    foregroundCenterVisible,
                foregroundEdgeFxVisible =
                    foregroundEdgeFxVisible,
                foregroundTuning =
                    foregroundTuning,
                manualFrameMode = manualFrameMode,
                shouldSkipNativeRendering = { releaseRequested },
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

        val baseWidth =
            manualRenderWidth
                ?.takeIf {
                    it > 0
                }
                ?: w
        val baseHeight =
            manualRenderHeight
                ?.takeIf {
                    it > 0
                }
                ?: h

        val scaledWidth =
            (
                baseWidth *
                    profile.renderScale
                )
                .roundToInt()
                .coerceAtLeast(
                    1,
                )
        val scaledHeight =
            (
                baseHeight *
                    profile.renderScale
                )
                .roundToInt()
                .coerceAtLeast(
                    1,
                )

        post {
            holder.setFixedSize(
                scaledWidth,
                scaledHeight,
            )
            Log.i(
                TAG,
                "surface ${scaledWidth}x${scaledHeight} from " +
                    "${baseWidth}x${baseHeight} · ${profile.name}" +
                    if (
                        manualRenderWidth != null &&
                        manualRenderHeight != null
                    ) {
                        " · manual export geometry"
                    } else {
                        ""
                    },
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

    fun setBackgroundVisible(
        visible: Boolean,
    ) {
        backgroundVisible =
            visible
        ProjectMBridge.setBackgroundVisible(
            visible,
        )
    }

    fun setForegroundVisibility(
        centerVisible: Boolean,
        edgeFxVisible: Boolean,
    ) {
        foregroundCenterVisible =
            centerVisible
        foregroundEdgeFxVisible =
            edgeFxVisible

        ProjectMBridge.setForegroundVisibility(
            centerVisible =
                centerVisible,
            edgeFxVisible =
                edgeFxVisible,
        )
    }

    fun setForegroundTuning(
        tuning: ProjectMForegroundTuning,
    ) {
        foregroundTuning =
            tuning.sanitized()
        ProjectMBridge.setForegroundTuning(
            foregroundTuning,
        )
    }

    fun releaseProjectM() {
        releaseRequested = true
        queueEvent {
            ProjectMBridge.destroy()
            clearOfflineReadbackCache()
        }
    }

    fun releaseProjectMThen(
        onReleased: () -> Unit,
    ) {
        releaseRequested = true
        val queuedNs = System.nanoTime()
        queueEvent {
            val startNs = System.nanoTime()
            ProjectMBridge.destroy()
            clearOfflineReadbackCache()
            Log.i(
                TAG,
                "native release: queued=" +
                    ((startNs - queuedNs) / 1_000_000L) + "ms, destroy=" +
                    ((System.nanoTime() - startNs) / 1_000_000L) + "ms",
            )

            post {
                onReleased()
            }
        }
    }

    fun releaseProjectMBlocking(
        timeoutMs: Long = 1_500L,
    ): Boolean {
        releaseRequested = true
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
            offlineQueueWaitNs =
                0L
            offlineNativeRenderNs =
                0L
            offlineReadPixelsNs =
                0L
            offlineBitmapCopyNs =
                0L

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
                .setBackgroundVisible(
                    backgroundVisible,
                )
            ProjectMBridge
                .setForegroundVisibility(
                    centerVisible =
                        foregroundCenterVisible,
                    edgeFxVisible =
                        foregroundEdgeFxVisible,
                )
            ProjectMBridge
                .setForegroundTuning(
                    foregroundTuning,
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

        val requestStartedNs =
            System.nanoTime()

        val latch =
            CountDownLatch(
                1,
            )

        var result: Bitmap? =
            null

        queueEvent {
            offlineQueueWaitNs +=
                System.nanoTime() -
                    requestStartedNs

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

    fun queueOfflineFrame(
        frameIndex: Int,
        frameTimeSeconds: Double,
        pcm: ShortArray,
        signal: com.saney.musicvisualizer.analysis.SceneSignal,
    ): ProjectMOfflineFrameRequest? {
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

        val requestStartedNs =
            System.nanoTime()

        val request =
            ProjectMOfflineFrameRequest(
                slotIndex =
                    frameIndex %
                        OFFLINE_READBACK_SLOT_COUNT,
            )

        queueEvent {
            try {
                offlineQueueWaitNs +=
                    System.nanoTime() -
                        requestStartedNs

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

                request.result =
                    readFramebuffer(
                        width = width,
                        height = height,
                        renderFirst = true,
                        reuseOfflineBuffers = true,
                        offlineSlot =
                            request.slotIndex,
                    )
            } finally {
                request.latch.countDown()
            }
        }

        return request
    }

    fun awaitOfflineFrame(
        request: ProjectMOfflineFrameRequest,
        timeoutMs: Long = 4_000L,
    ): Bitmap? {
        val completed =
            runCatching {
                request.latch.await(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )
            }.getOrDefault(
                false,
            )

        return if (completed) {
            request.result
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
        offlineSlot: Int = 0,
    ): Bitmap? =
        runCatching {
            if (renderFirst) {
                val renderStartedNs =
                    System.nanoTime()

                ProjectMBridge.render()

                if (reuseOfflineBuffers) {
                    offlineNativeRenderNs +=
                        System.nanoTime() -
                            renderStartedNs
                }
            }

            val pixelCount =
                width *
                    height

            val safeOfflineSlot =
                offlineSlot.coerceIn(
                    0,
                    OFFLINE_READBACK_SLOT_COUNT -
                        1,
                )

            val buffer =
                if (reuseOfflineBuffers) {
                    ensureOfflineReadbackCache(
                        width = width,
                        height = height,
                    )
                    offlineReadbackBuffers[
                        safeOfflineSlot
                    ]
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
            val readPixelsStartedNs =
                if (reuseOfflineBuffers) {
                    System.nanoTime()
                } else {
                    0L
                }

            if (
                reuseOfflineBuffers &&
                supportsBgraReadback()
            ) {
                clearGlErrors()

                GLES20.glReadPixels(
                    0,
                    0,
                    width,
                    height,
                    GL_BGRA_EXT,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )

                if (
                    GLES20.glGetError() ==
                    GLES20.GL_NO_ERROR
                ) {
                    offlineReadbackChannelsCorrect =
                        true
                } else {
                    // Extension reporting can still be unreliable on some
                    // drivers. Fall back to the established RGBA path.
                    buffer.position(
                        0,
                    )
                    clearGlErrors()
                    GLES20.glReadPixels(
                        0,
                        0,
                        width,
                        height,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        buffer,
                    )
                    offlineReadbackChannelsCorrect =
                        false
                }
            } else {
                GLES20.glReadPixels(
                    0,
                    0,
                    width,
                    height,
                    GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )
                offlineReadbackChannelsCorrect =
                    false
            }

            if (reuseOfflineBuffers) {
                offlineReadPixelsNs +=
                    System.nanoTime() -
                        readPixelsStartedNs
            }

            if (reuseOfflineBuffers) {
                val bitmap =
                    offlineReadbackBitmaps[
                        safeOfflineSlot
                    ]
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

                val copyStartedNs =
                    System.nanoTime()

                bitmap.copyPixelsFromBuffer(
                    buffer,
                )

                offlineBitmapCopyNs +=
                    System.nanoTime() -
                        copyStartedNs

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

    private fun supportsBgraReadback():
        Boolean {
        offlineBgraReadbackSupported
            ?.let {
                return it
            }

        val extensions =
            GLES20.glGetString(
                GLES20.GL_EXTENSIONS,
            ).orEmpty()

        val supported =
            extensions
                .split(
                    ' ',
                )
                .any {
                    it ==
                        "GL_EXT_read_format_bgra"
                }

        offlineBgraReadbackSupported =
            supported

        return supported
    }

    private fun clearGlErrors() {
        repeat(
            8,
        ) {
            if (
                GLES20.glGetError() ==
                GLES20.GL_NO_ERROR
            ) {
                return
            }
        }
    }

    private fun ensureOfflineReadbackCache(
        width: Int,
        height: Int,
    ) {
        if (
            offlineReadbackWidth ==
                width &&
            offlineReadbackHeight ==
                height &&
            offlineReadbackBuffers
                .all {
                    it != null
                } &&
            offlineReadbackBitmaps
                .all {
                    it?.isRecycled ==
                        false
                }
        ) {
            return
        }

        clearOfflineReadbackCache()

        repeat(
            OFFLINE_READBACK_SLOT_COUNT,
        ) { index ->
            offlineReadbackBuffers[index] =
                ByteBuffer
                    .allocateDirect(
                        width *
                            height *
                            4,
                    )
                    .order(
                        ByteOrder.nativeOrder(),
                    )
            offlineReadbackBitmaps[index] =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )
        }

        offlineReadbackWidth =
            width
        offlineReadbackHeight =
            height
    }

    private fun clearOfflineReadbackCache() {
        offlineReadbackBitmaps
            .forEach {
                    bitmap ->
                bitmap
                    ?.takeIf {
                        !it.isRecycled
                    }
                    ?.recycle()
            }

        offlineReadbackBitmaps
            .indices
            .forEach {
                    index ->
                offlineReadbackBitmaps[index] =
                    null
                offlineReadbackBuffers[index] =
                    null
            }

        offlineReadbackWidth =
            0
        offlineReadbackHeight =
            0
        offlineReadbackChannelsCorrect =
            false
    }

    private class Renderer(
        private val presetPath: String,
        private val texturePath: String,
        private val profile: ProjectMPerformanceProfile,
        private val foregroundSample: FaricForegroundSample,
        private val backgroundVisible: Boolean,
        private val foregroundCenterVisible: Boolean,
        private val foregroundEdgeFxVisible: Boolean,
        private val foregroundTuning: ProjectMForegroundTuning,
        private val manualFrameMode: Boolean,
        private val shouldSkipNativeRendering: () -> Boolean,
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
            if (shouldSkipNativeRendering()) return
            onSurfaceSize(
                width,
                height,
            )

            if (!created) {
                val createStartedNs = System.nanoTime()
                ProjectMBridge.create(
                    width = width,
                    height = height,
                    presetPath = presetPath,
                    texturePath = texturePath,
                    profile = profile,
                )
                Log.i(
                    TAG,
                    "native create: " +
                        ((System.nanoTime() - createStartedNs) / 1_000_000L) +
                        " ms · profile=" + profile.name,
                )
                // FARIC owns AUTO/MANUAL timing. projectM internal switching stays locked.
                ProjectMBridge.enableAutoPresetSwitching(false)
                ProjectMBridge.setForegroundSample(foregroundSample)
                ProjectMBridge.setBackgroundVisible(
                    backgroundVisible,
                )
                ProjectMBridge.setForegroundVisibility(
                    centerVisible =
                        foregroundCenterVisible,
                    edgeFxVisible =
                        foregroundEdgeFxVisible,
                )
                ProjectMBridge.setForegroundTuning(
                    foregroundTuning,
                )
                created = true
            } else {
                ProjectMBridge.resize(width, height)
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            if (manualFrameMode || shouldSkipNativeRendering()) {
                return
            }

            ProjectMBridge.render()
            if (frameCount == 0) {
                Log.i(TAG, "first GL render frame after surface setup")
            }

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
        private const val GL_BGRA_EXT =
            0x80E1
        private const val OFFLINE_READBACK_SLOT_COUNT =
            2
    }
}
