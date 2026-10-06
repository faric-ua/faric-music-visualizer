package com.saney.musicvisualizer.export

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.projectm.FaricForegroundSample
import com.saney.musicvisualizer.projectm.ProjectMBridge
import com.saney.musicvisualizer.projectm.ProjectMPerformanceProfile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Uploads already-composited ARGB frames directly to a MediaCodec input Surface.
 *
 * This removes the old CPU Bitmap -> IntArray -> Kotlin ARGB/YUV420 conversion
 * from the full-song export hot path. The encoder receives RGBA through EGL and
 * performs its own hardware-native color conversion.
 */
data class EglCompositeDrawTiming(
    val projectMNs: Long,
    val glowNs: Long,
    val frameNs: Long,
    val overlayNs: Long,
    val creatureNs: Long = 0L,
    val wordmarkNs: Long = 0L,
)

class EglBitmapEncoderSurface(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
) : AutoCloseable {
    private val display: EGLDisplay
    private val context: EGLContext
    private val eglSurface: EGLSurface

    private val program: Int
    private val textureId: Int
    private val projectMTextureId: Int
    private val frameTextureId: Int
    private val creatureTextureId: Int
    private val wordmarkTextureId: Int
    private var frameTextureLoaded =
        false
    private var creatureTextureLoaded =
        false
    private var wordmarkTextureLoaded =
        false
    private var projectMTextureWidth =
        0
    private var projectMTextureHeight =
        0
    private var projectMFramebufferId =
        0
    private var directProjectMActive =
        false
    private val positionHandle: Int
    private val texCoordHandle: Int
    private val samplerHandle: Int

    private val frameProgram: Int
    private val framePositionHandle: Int
    private val frameTexCoordHandle: Int
    private val frameSamplerHandle: Int
    private val frameAlphaHandle: Int

    private val glowProgram: Int
    private val glowPositionHandle: Int
    private val glowTexCoordHandle: Int
    private val glowCenterHandle: Int
    private val glowViewportHandle: Int
    private val glowCircleRadiusHandle: Int
    private val glowGradientRadiusHandle: Int
    private val glowCenterAlphaHandle: Int
    private val glowMidAlphaHandle: Int

    private val vertexBuffer: FloatBuffer =
        floatBufferOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )

    // Android Bitmap row 0 is the top row. OpenGL texture coordinates use the
    // lower-left origin, so V is flipped here once instead of flipping pixels.
    private val texCoordBuffer: FloatBuffer =
        floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
        )

    // Offline projectM readback is already GL-oriented (bottom-up), so direct
    // GPU composition must not apply the Android-Bitmap V flip a second time.
    private val rawGlTexCoordBuffer: FloatBuffer =
        floatBufferOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        )

    private val projectMTexCoordBuffer: FloatBuffer =
        floatBufferOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        )

    private val frameVertexBuffer: FloatBuffer =
        floatBufferOf(
            0f, 0f,
            0f, 0f,
            0f, 0f,
            0f, 0f,
        )

    init {
        display =
            EGL14.eglGetDisplay(
                EGL14.EGL_DEFAULT_DISPLAY,
            )
        check(
            display != EGL14.EGL_NO_DISPLAY,
        ) {
            "EGL display unavailable"
        }

        val versions =
            IntArray(
                2,
            )
        check(
            EGL14.eglInitialize(
                display,
                versions,
                0,
                versions,
                1,
            ),
        ) {
            "EGL initialization failed"
        }

        val config =
            chooseConfig(
                display,
            )

        val contextAttributes =
            intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION,
                2,
                EGL14.EGL_NONE,
            )

        context =
            EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                contextAttributes,
                0,
            )
        check(
            context != EGL14.EGL_NO_CONTEXT,
        ) {
            "EGL context creation failed"
        }

        eglSurface =
            EGL14.eglCreateWindowSurface(
                display,
                config,
                surface,
                intArrayOf(
                    EGL14.EGL_NONE,
                ),
                0,
            )
        check(
            eglSurface != EGL14.EGL_NO_SURFACE,
        ) {
            "EGL encoder surface creation failed"
        }

        makeCurrent()

        program =
            createProgram(
                VERTEX_SHADER,
                FRAGMENT_SHADER,
            )
        positionHandle =
            GLES20.glGetAttribLocation(
                program,
                "aPosition",
            )
        texCoordHandle =
            GLES20.glGetAttribLocation(
                program,
                "aTexCoord",
            )
        samplerHandle =
            GLES20.glGetUniformLocation(
                program,
                "uTexture",
            )

        frameProgram =
            createProgram(
                VERTEX_SHADER,
                ALPHA_FRAGMENT_SHADER,
            )
        framePositionHandle =
            GLES20.glGetAttribLocation(
                frameProgram,
                "aPosition",
            )
        frameTexCoordHandle =
            GLES20.glGetAttribLocation(
                frameProgram,
                "aTexCoord",
            )
        frameSamplerHandle =
            GLES20.glGetUniformLocation(
                frameProgram,
                "uTexture",
            )
        frameAlphaHandle =
            GLES20.glGetUniformLocation(
                frameProgram,
                "uAlpha",
            )

        glowProgram =
            createProgram(
                VERTEX_SHADER,
                GLOW_FRAGMENT_SHADER,
            )
        glowPositionHandle =
            GLES20.glGetAttribLocation(
                glowProgram,
                "aPosition",
            )
        glowTexCoordHandle =
            GLES20.glGetAttribLocation(
                glowProgram,
                "aTexCoord",
            )
        glowCenterHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uCenterPx",
            )
        glowViewportHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uViewportPx",
            )
        glowCircleRadiusHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uCircleRadiusPx",
            )
        glowGradientRadiusHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uGradientRadiusPx",
            )
        glowCenterAlphaHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uCenterAlpha",
            )
        glowMidAlphaHandle =
            GLES20.glGetUniformLocation(
                glowProgram,
                "uMidAlpha",
            )

        val textureIds =
            IntArray(
                5,
            )
        GLES20.glGenTextures(
            5,
            textureIds,
            0,
        )
        textureId =
            textureIds[0]
        projectMTextureId =
            textureIds[1]
        frameTextureId =
            textureIds[2]
        creatureTextureId =
            textureIds[3]
        wordmarkTextureId =
            textureIds[4]

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            textureId,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null,
        )
        checkGl(
            "texture allocation",
        )

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            projectMTextureId,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        checkGl(
            "projectM texture setup",
        )

        configureLayerTexture(
            frameTextureId,
            "frame",
        )
        configureLayerTexture(
            creatureTextureId,
            "creature",
        )
        configureLayerTexture(
            wordmarkTextureId,
            "wordmark",
        )
    }

    private fun configureLayerTexture(
        texture: Int,
        label: String,
    ) {
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            texture,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        checkGl(
            "$label texture setup",
        )
    }

    fun initializeOfflineProjectM(
        renderWidth: Int,
        renderHeight: Int,
        presetPath: String,
        texturePath: String,
        profile:
            ProjectMPerformanceProfile,
        foregroundSample:
            FaricForegroundSample,
    ) {
        check(
            renderWidth > 0 &&
                renderHeight > 0,
        ) {
            "Invalid projectM GPU render size"
        }

        makeCurrent()

        ensureProjectMTextureStorage(
            renderWidth,
            renderHeight,
        )

        if (
            projectMFramebufferId ==
                0
        ) {
            val ids =
                IntArray(
                    1,
                )
            GLES20.glGenFramebuffers(
                1,
                ids,
                0,
            )
            projectMFramebufferId =
                ids[0]
        }

        GLES20.glBindFramebuffer(
            GLES20.GL_FRAMEBUFFER,
            projectMFramebufferId,
        )
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            projectMTextureId,
            0,
        )
        check(
            GLES20.glCheckFramebufferStatus(
                GLES20.GL_FRAMEBUFFER,
            ) ==
                GLES20.GL_FRAMEBUFFER_COMPLETE,
        ) {
            "projectM GPU framebuffer incomplete"
        }
        GLES20.glBindFramebuffer(
            GLES20.GL_FRAMEBUFFER,
            0,
        )

        ProjectMBridge.destroy()
        ProjectMBridge.beginOfflineExport()
        ProjectMBridge.create(
            width = renderWidth,
            height = renderHeight,
            presetPath = presetPath,
            texturePath = texturePath,
            profile = profile,
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

        directProjectMActive =
            true
    }

    fun renderOfflineProjectM(
        frameTimeSeconds: Double,
        pcm: ShortArray,
        signal: SceneSignal,
    ): Long {
        check(
            directProjectMActive &&
                projectMFramebufferId !=
                0,
        ) {
            "Direct GPU projectM is not initialized"
        }

        makeCurrent()

        ProjectMBridge
            .beginOfflineExport()
        ProjectMBridge
            .setFrameTime(
                frameTimeSeconds,
            )
        ProjectMBridge
            .pushOfflinePcm(
                pcm,
            )
        ProjectMBridge
            .pushOfflineSignal(
                signal,
            )

        GLES20.glBindFramebuffer(
            GLES20.GL_FRAMEBUFFER,
            projectMFramebufferId,
        )
        GLES20.glViewport(
            0,
            0,
            projectMTextureWidth,
            projectMTextureHeight,
        )

        val startedNs =
            System.nanoTime()

        ProjectMBridge
            .renderToFramebuffer(
                projectMFramebufferId,
            )

        val elapsedNs =
            System.nanoTime() -
                startedNs

        GLES20.glBindFramebuffer(
            GLES20.GL_FRAMEBUFFER,
            0,
        )
        GLES20.glViewport(
            0,
            0,
            width,
            height,
        )

        return elapsedNs
    }

    fun hasDirectProjectM():
        Boolean =
        directProjectMActive

    fun draw(
        bitmap: Bitmap,
        presentationTimeNs: Long,
    ) {
        validateBitmap(
            bitmap,
        )
        makeCurrent()
        beginFrame()
        drawBitmapLayer(
            bitmap = bitmap,
            alphaBlend = false,
        )
        finishFrame(
            presentationTimeNs,
        )
    }

    fun drawComposite(
        baseBitmap: Bitmap,
        overlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        validateBitmap(
            baseBitmap,
        )
        validateBitmap(
            overlayBitmap,
        )
        makeCurrent()
        beginFrame()

        drawBitmapLayer(
            bitmap = baseBitmap,
            alphaBlend = false,
        )

        val glowStartedNs =
            System.nanoTime()

        drawGlow(
            glow,
        )

        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val overlayStartedNs =
            System.nanoTime()

        drawBitmapLayer(
            bitmap = overlayBitmap,
            alphaBlend = true,
        )

        val overlayNs =
            System.nanoTime() -
                overlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = 0L,
            glowNs = glowNs,
            frameNs = 0L,
            overlayNs = overlayNs,
        )
    }

    fun drawProjectMComposite(
        projectMBitmap: Bitmap,
        overlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        check(
            projectMBitmap.width > 0 &&
                projectMBitmap.height > 0,
        ) {
            "ProjectM frame is empty"
        }
        validateBitmap(
            overlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()

        drawProjectMRawLayer(
            projectMBitmap,
        )

        val projectMNs =
            System.nanoTime() -
                projectMStartedNs

        val glowStartedNs =
            System.nanoTime()

        drawGlow(
            glow,
        )

        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val overlayStartedNs =
            System.nanoTime()

        drawBitmapLayer(
            bitmap = overlayBitmap,
            alphaBlend = true,
        )

        val overlayNs =
            System.nanoTime() -
                overlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = projectMNs,
            glowNs = glowNs,
            frameNs = 0L,
            overlayNs = overlayNs,
        )
    }

    fun drawProjectMFrameComposite(
        projectMBitmap: Bitmap,
        lowerOverlayBitmap: Bitmap,
        frame: CyberSharkGpuFrame,
        upperOverlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        check(
            projectMBitmap.width > 0 &&
                projectMBitmap.height > 0,
        ) {
            "ProjectM frame is empty"
        }
        validateBitmap(
            lowerOverlayBitmap,
        )
        validateBitmap(
            upperOverlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()
        drawProjectMRawLayer(
            projectMBitmap,
        )
        val projectMNs =
            System.nanoTime() -
                projectMStartedNs

        val glowStartedNs =
            System.nanoTime()
        drawGlow(
            glow,
        )
        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val lowerOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                lowerOverlayBitmap,
            alphaBlend = true,
        )
        var overlayNs =
            System.nanoTime() -
                lowerOverlayStartedNs

        val frameStartedNs =
            System.nanoTime()
        drawFrame(
            frame,
        )
        val frameNs =
            System.nanoTime() -
                frameStartedNs

        val upperOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                upperOverlayBitmap,
            alphaBlend = true,
        )
        overlayNs +=
            System.nanoTime() -
                upperOverlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = projectMNs,
            glowNs = glowNs,
            frameNs = frameNs,
            overlayNs = overlayNs,
        )
    }

    fun drawGpuProjectMComposite(
        overlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        check(
            directProjectMActive,
        ) {
            "Direct GPU projectM is not initialized"
        }
        validateBitmap(
            overlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()
        drawProjectMTextureLayer()
        val projectMNs =
            System.nanoTime() -
                projectMStartedNs

        val glowStartedNs =
            System.nanoTime()
        drawGlow(
            glow,
        )
        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val overlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap = overlayBitmap,
            alphaBlend = true,
        )
        val overlayNs =
            System.nanoTime() -
                overlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = projectMNs,
            glowNs = glowNs,
            frameNs = 0L,
            overlayNs = overlayNs,
        )
    }

    fun drawGpuProjectMFrameComposite(
        lowerOverlayBitmap: Bitmap,
        frame: CyberSharkGpuFrame,
        upperOverlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        check(
            directProjectMActive,
        ) {
            "Direct GPU projectM is not initialized"
        }
        validateBitmap(
            lowerOverlayBitmap,
        )
        validateBitmap(
            upperOverlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()
        drawProjectMTextureLayer()
        val projectMNs =
            System.nanoTime() -
                projectMStartedNs

        val glowStartedNs =
            System.nanoTime()
        drawGlow(
            glow,
        )
        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val lowerOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                lowerOverlayBitmap,
            alphaBlend = true,
        )
        var overlayNs =
            System.nanoTime() -
                lowerOverlayStartedNs

        val frameStartedNs =
            System.nanoTime()
        drawFrame(
            frame,
        )
        val frameNs =
            System.nanoTime() -
                frameStartedNs

        val upperOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                upperOverlayBitmap,
            alphaBlend = true,
        )
        overlayNs +=
            System.nanoTime() -
                upperOverlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = projectMNs,
            glowNs = glowNs,
            frameNs = frameNs,
            overlayNs = overlayNs,
        )
    }

    fun drawGpuProjectMCyberSharkComposite(
        lowerOverlayBitmap: Bitmap,
        frame: CyberSharkGpuFrame,
        fxOverlayBitmap: Bitmap,
        creature: CyberSharkGpuFrame,
        wordmark: CyberSharkGpuFrame,
        topOverlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        check(
            directProjectMActive,
        ) {
            "Direct GPU projectM is not initialized"
        }
        validateBitmap(
            lowerOverlayBitmap,
        )
        validateBitmap(
            fxOverlayBitmap,
        )
        validateBitmap(
            topOverlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()
        drawProjectMTextureLayer()
        val projectMNs =
            System.nanoTime() -
                projectMStartedNs

        val glowStartedNs =
            System.nanoTime()
        drawGlow(
            glow,
        )
        val glowNs =
            System.nanoTime() -
                glowStartedNs

        val lowerOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                lowerOverlayBitmap,
            alphaBlend = true,
        )
        var overlayNs =
            System.nanoTime() -
                lowerOverlayStartedNs

        val frameStartedNs =
            System.nanoTime()
        drawFrame(
            frame,
        )
        val frameNs =
            System.nanoTime() -
                frameStartedNs

        val fxOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                fxOverlayBitmap,
            alphaBlend = true,
        )
        overlayNs +=
            System.nanoTime() -
                fxOverlayStartedNs

        val creatureStartedNs =
            System.nanoTime()
        drawCreature(
            creature,
        )
        val creatureNs =
            System.nanoTime() -
                creatureStartedNs

        val wordmarkStartedNs =
            System.nanoTime()
        drawWordmark(
            wordmark,
        )
        val wordmarkNs =
            System.nanoTime() -
                wordmarkStartedNs

        val topOverlayStartedNs =
            System.nanoTime()
        drawBitmapLayer(
            bitmap =
                topOverlayBitmap,
            alphaBlend = true,
        )
        overlayNs +=
            System.nanoTime() -
                topOverlayStartedNs

        finishFrame(
            presentationTimeNs,
        )

        return EglCompositeDrawTiming(
            projectMNs = projectMNs,
            glowNs = glowNs,
            frameNs = frameNs,
            overlayNs = overlayNs,
            creatureNs =
                creatureNs,
            wordmarkNs =
                wordmarkNs,
        )
    }

    private fun validateBitmap(
        bitmap: Bitmap,
    ) {
        check(
            bitmap.width == width &&
                bitmap.height == height,
        ) {
            "Encoder frame size mismatch: " +
                "${bitmap.width}x${bitmap.height} != " +
                "${width}x${height}"
        }
    }

    private fun beginFrame() {
        GLES20.glViewport(
            0,
            0,
            width,
            height,
        )
        GLES20.glDisable(
            GLES20.GL_BLEND,
        )
        GLES20.glDisable(
            GLES20.GL_SCISSOR_TEST,
        )
        GLES20.glClearColor(
            0f,
            0f,
            0f,
            1f,
        )
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT,
        )
    }

    private fun drawBitmapLayer(
        bitmap: Bitmap,
        alphaBlend: Boolean,
        rawGlOrientation:
            Boolean = false,
    ) {
        if (alphaBlend) {
            GLES20.glEnable(
                GLES20.GL_BLEND,
            )
            GLES20.glBlendFunc(
                GLES20.GL_ONE,
                GLES20.GL_ONE_MINUS_SRC_ALPHA,
            )
        } else {
            GLES20.glDisable(
                GLES20.GL_BLEND,
            )
        }

        GLES20.glUseProgram(
            program,
        )
        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0,
        )
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            textureId,
        )
        GLUtils.texSubImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            0,
            0,
            bitmap,
        )

        bindQuad(
            positionHandle,
            texCoordHandle,
            if (rawGlOrientation) {
                rawGlTexCoordBuffer
            } else {
                texCoordBuffer
            },
        )

        GLES20.glUniform1i(
            samplerHandle,
            0,
        )
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )
        checkGl(
            if (alphaBlend) {
                "overlay draw"
            } else {
                "frame draw"
            },
        )
    }

    private fun ensureProjectMTextureStorage(
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        if (
            projectMTextureWidth ==
                sourceWidth &&
            projectMTextureHeight ==
                sourceHeight
        ) {
            return
        }

        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            projectMTextureId,
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            sourceWidth,
            sourceHeight,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null,
        )
        projectMTextureWidth =
            sourceWidth
        projectMTextureHeight =
            sourceHeight
        checkGl(
            "projectM texture storage",
        )
    }

    private fun updateProjectMTexCoords(
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        val scale =
            max(
                width.toFloat() /
                    sourceWidth,
                height.toFloat() /
                    sourceHeight,
            )
        val renderedWidth =
            sourceWidth *
                scale
        val renderedHeight =
            sourceHeight *
                scale
        val cropX =
            (
                (
                    renderedWidth -
                        width
                    ) /
                    (
                        2f *
                            renderedWidth
                        )
                )
                .coerceAtLeast(
                    0f,
                )
        val cropY =
            (
                (
                    renderedHeight -
                        height
                    ) /
                    (
                        2f *
                            renderedHeight
                        )
                )
                .coerceAtLeast(
                    0f,
                )

        val u0 =
            cropX
        val u1 =
            1f -
                cropX
        val v0 =
            cropY
        val v1 =
            1f -
                cropY

        projectMTexCoordBuffer
            .position(
                0,
            )
        projectMTexCoordBuffer
            .put(
                floatArrayOf(
                    u0, v0,
                    u1, v0,
                    u0, v1,
                    u1, v1,
                ),
            )
        projectMTexCoordBuffer
            .position(
                0,
            )
    }

    private fun drawProjectMTextureLayer() {
        GLES20.glDisable(
            GLES20.GL_BLEND,
        )
        GLES20.glUseProgram(
            program,
        )
        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0,
        )
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            projectMTextureId,
        )
        updateProjectMTexCoords(
            projectMTextureWidth,
            projectMTextureHeight,
        )
        bindQuad(
            positionHandle,
            texCoordHandle,
            projectMTexCoordBuffer,
        )
        GLES20.glUniform1i(
            samplerHandle,
            0,
        )
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )
        checkGl(
            "projectM direct GPU draw",
        )
    }

    private fun drawProjectMRawLayer(
        bitmap: Bitmap,
    ) {
        GLES20.glDisable(
            GLES20.GL_BLEND,
        )
        GLES20.glUseProgram(
            program,
        )
        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0,
        )
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            projectMTextureId,
        )

        val sizeChanged =
            projectMTextureWidth !=
                bitmap.width ||
                projectMTextureHeight !=
                bitmap.height

        ensureProjectMTextureStorage(
            bitmap.width,
            bitmap.height,
        )

        if (sizeChanged) {
            GLUtils.texSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                bitmap,
            )
        } else {
            GLUtils.texSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                bitmap,
            )
        }

        // Match CompositionExportRenderer.drawProjectMFrame(): scale-to-fill
        // then center-crop. The source is still GL-oriented, so V remains
        // unflipped while the cropped texture coordinates are applied.
        updateProjectMTexCoords(
            bitmap.width,
            bitmap.height,
        )

        bindQuad(
            positionHandle,
            texCoordHandle,
            projectMTexCoordBuffer,
        )
        GLES20.glUniform1i(
            samplerHandle,
            0,
        )
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )
        checkGl(
            "projectM GPU base draw",
        )
    }

    private fun drawFrame(
        frame: CyberSharkGpuFrame,
    ) {
        frameTextureLoaded =
            drawStaticBitmapLayer(
                layer = frame,
                texture = frameTextureId,
                textureLoaded =
                    frameTextureLoaded,
                label = "frame",
            )
    }

    private fun drawCreature(
        creature: CyberSharkGpuFrame,
    ) {
        creatureTextureLoaded =
            drawStaticBitmapLayer(
                layer = creature,
                texture =
                    creatureTextureId,
                textureLoaded =
                    creatureTextureLoaded,
                label = "creature",
            )
    }

    private fun drawWordmark(
        wordmark: CyberSharkGpuFrame,
    ) {
        wordmarkTextureLoaded =
            drawStaticBitmapLayer(
                layer = wordmark,
                texture =
                    wordmarkTextureId,
                textureLoaded =
                    wordmarkTextureLoaded,
                label = "wordmark",
            )
    }

    private fun drawStaticBitmapLayer(
        layer: CyberSharkGpuFrame,
        texture: Int,
        textureLoaded: Boolean,
        label: String,
    ): Boolean {
        if (
            layer.size <= 0f ||
            layer.alpha <= 0f
        ) {
            return textureLoaded
        }

        GLES20.glEnable(
            GLES20.GL_BLEND,
        )
        GLES20.glBlendFunc(
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
        )

        GLES20.glUseProgram(
            frameProgram,
        )
        GLES20.glActiveTexture(
            GLES20.GL_TEXTURE0,
        )
        GLES20.glBindTexture(
            GLES20.GL_TEXTURE_2D,
            texture,
        )

        var loaded =
            textureLoaded
        if (!loaded) {
            GLUtils.texImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                layer.bitmap,
                0,
            )
            loaded =
                true
        }

        val half =
            layer.size *
                0.5f
        val radians =
            Math.toRadians(
                layer
                    .rotationDegrees
                    .toDouble(),
            )
        val c =
            cos(
                radians,
            )
                .toFloat()
        val sn =
            sin(
                radians,
            )
                .toFloat()

        fun point(
            dx: Float,
            dy: Float,
        ): Pair<Float, Float> {
            val screenX =
                layer.centerX +
                    dx * c -
                    dy * sn
            val screenY =
                layer.centerY +
                    dx * sn +
                    dy * c
            val ndcX =
                screenX /
                    width *
                    2f -
                    1f
            val ndcY =
                1f -
                    screenY /
                        height *
                        2f
            return Pair(
                ndcX,
                ndcY,
            )
        }

        val bottomLeft =
            point(
                -half,
                half,
            )
        val bottomRight =
            point(
                half,
                half,
            )
        val topLeft =
            point(
                -half,
                -half,
            )
        val topRight =
            point(
                half,
                -half,
            )

        frameVertexBuffer
            .position(
                0,
            )
        frameVertexBuffer
            .put(
                floatArrayOf(
                    bottomLeft.first,
                    bottomLeft.second,
                    bottomRight.first,
                    bottomRight.second,
                    topLeft.first,
                    topLeft.second,
                    topRight.first,
                    topRight.second,
                ),
            )
        frameVertexBuffer
            .position(
                0,
            )

        bindQuad(
            framePositionHandle,
            frameTexCoordHandle,
            texCoordBuffer,
            frameVertexBuffer,
        )
        GLES20.glUniform1i(
            frameSamplerHandle,
            0,
        )
        GLES20.glUniform1f(
            frameAlphaHandle,
            layer.alpha,
        )
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )
        checkGl(
            "GPU $label draw",
        )

        return loaded
    }

    private fun drawGlow(
        glow: CyberSharkGpuGlow,
    ) {
        if (
            glow.circleRadius <= 0f ||
            glow.gradientRadius <= 0f ||
            (
                glow.centerAlpha <= 0f &&
                    glow.midAlpha <= 0f
                )
        ) {
            return
        }

        GLES20.glEnable(
            GLES20.GL_BLEND,
        )
        GLES20.glBlendFunc(
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
        )

        val left =
            (
                glow.centerX -
                    glow.circleRadius
                )
                .toInt()
                .coerceIn(
                    0,
                    width,
                )
        val right =
            (
                glow.centerX +
                    glow.circleRadius
                )
                .toInt()
                .coerceIn(
                    0,
                    width,
                )
        val top =
            (
                glow.centerY -
                    glow.circleRadius
                )
                .toInt()
                .coerceIn(
                    0,
                    height,
                )
        val bottom =
            (
                glow.centerY +
                    glow.circleRadius
                )
                .toInt()
                .coerceIn(
                    0,
                    height,
                )

        if (
            right <= left ||
            bottom <= top
        ) {
            return
        }

        GLES20.glEnable(
            GLES20.GL_SCISSOR_TEST,
        )
        GLES20.glScissor(
            left,
            height - bottom,
            right - left,
            bottom - top,
        )

        GLES20.glUseProgram(
            glowProgram,
        )
        bindQuad(
            glowPositionHandle,
            glowTexCoordHandle,
            texCoordBuffer,
        )
        GLES20.glUniform2f(
            glowCenterHandle,
            glow.centerX,
            glow.centerY,
        )
        GLES20.glUniform2f(
            glowViewportHandle,
            width.toFloat(),
            height.toFloat(),
        )
        GLES20.glUniform1f(
            glowCircleRadiusHandle,
            glow.circleRadius,
        )
        GLES20.glUniform1f(
            glowGradientRadiusHandle,
            glow.gradientRadius,
        )
        GLES20.glUniform1f(
            glowCenterAlphaHandle,
            glow.centerAlpha,
        )
        GLES20.glUniform1f(
            glowMidAlphaHandle,
            glow.midAlpha,
        )

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLE_STRIP,
            0,
            4,
        )
        GLES20.glDisable(
            GLES20.GL_SCISSOR_TEST,
        )
        checkGl(
            "GPU glow draw",
        )
    }

    private fun bindQuad(
        position: Int,
        texCoord: Int,
        coordinates: FloatBuffer,
        positions:
            FloatBuffer =
            vertexBuffer,
    ) {
        positions.position(
            0,
        )
        GLES20.glEnableVertexAttribArray(
            position,
        )
        GLES20.glVertexAttribPointer(
            position,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            positions,
        )

        coordinates.position(
            0,
        )
        GLES20.glEnableVertexAttribArray(
            texCoord,
        )
        GLES20.glVertexAttribPointer(
            texCoord,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            coordinates,
        )
    }

    private fun finishFrame(
        presentationTimeNs: Long,
    ) {
        GLES20.glDisable(
            GLES20.GL_BLEND,
        )
        GLES20.glDisable(
            GLES20.GL_SCISSOR_TEST,
        )

        check(
            EGLExt.eglPresentationTimeANDROID(
                display,
                eglSurface,
                presentationTimeNs,
            ),
        ) {
            "Failed to set encoder presentation time"
        }

        check(
            EGL14.eglSwapBuffers(
                display,
                eglSurface,
            ),
        ) {
            "Failed to submit encoder frame"
        }
    }

    private fun makeCurrent() {
        check(
            EGL14.eglMakeCurrent(
                display,
                eglSurface,
                eglSurface,
                context,
            ),
        ) {
            "Failed to make encoder EGL context current"
        }
    }

    override fun close() {
        runCatching {
            makeCurrent()

            if (directProjectMActive) {
                ProjectMBridge
                    .endOfflineExport()
                ProjectMBridge
                    .destroy()
                directProjectMActive =
                    false
            }

            if (
                projectMFramebufferId !=
                    0
            ) {
                GLES20.glDeleteFramebuffers(
                    1,
                    intArrayOf(
                        projectMFramebufferId,
                    ),
                    0,
                )
                projectMFramebufferId =
                    0
            }

            GLES20.glDeleteTextures(
                5,
                intArrayOf(
                    textureId,
                    projectMTextureId,
                    frameTextureId,
                    creatureTextureId,
                    wordmarkTextureId,
                ),
                0,
            )
            GLES20.glDeleteProgram(
                program,
            )
            GLES20.glDeleteProgram(
                frameProgram,
            )
            GLES20.glDeleteProgram(
                glowProgram,
            )
        }

        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT,
        )
        EGL14.eglDestroySurface(
            display,
            eglSurface,
        )
        EGL14.eglDestroyContext(
            display,
            context,
        )
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(
            display,
        )

        surface.release()
    }

    private fun chooseConfig(
        display: EGLDisplay,
    ): EGLConfig {
        val attributes =
            intArrayOf(
                EGL14.EGL_RED_SIZE,
                8,
                EGL14.EGL_GREEN_SIZE,
                8,
                EGL14.EGL_BLUE_SIZE,
                8,
                EGL14.EGL_ALPHA_SIZE,
                8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL_RECORDABLE_ANDROID,
                1,
                EGL14.EGL_NONE,
            )

        val configs =
            arrayOfNulls<EGLConfig>(
                1,
            )
        val count =
            IntArray(
                1,
            )

        check(
            EGL14.eglChooseConfig(
                display,
                attributes,
                0,
                configs,
                0,
                configs.size,
                count,
                0,
            ) &&
                count[0] > 0 &&
                configs[0] != null,
        ) {
            "No recordable EGL config"
        }

        return configs[0]!!
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertex =
            compileShader(
                GLES20.GL_VERTEX_SHADER,
                vertexSource,
            )
        val fragment =
            compileShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentSource,
            )

        val result =
            GLES20.glCreateProgram()
        check(
            result != 0,
        ) {
            "Could not create GL program"
        }

        GLES20.glAttachShader(
            result,
            vertex,
        )
        GLES20.glAttachShader(
            result,
            fragment,
        )
        GLES20.glLinkProgram(
            result,
        )

        val status =
            IntArray(
                1,
            )
        GLES20.glGetProgramiv(
            result,
            GLES20.GL_LINK_STATUS,
            status,
            0,
        )

        GLES20.glDeleteShader(
            vertex,
        )
        GLES20.glDeleteShader(
            fragment,
        )

        check(
            status[0] == GLES20.GL_TRUE,
        ) {
            "GL program link failed: " +
                GLES20.glGetProgramInfoLog(
                    result,
                )
        }

        return result
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int {
        val shader =
            GLES20.glCreateShader(
                type,
            )
        check(
            shader != 0,
        ) {
            "Could not create GL shader"
        }

        GLES20.glShaderSource(
            shader,
            source,
        )
        GLES20.glCompileShader(
            shader,
        )

        val status =
            IntArray(
                1,
            )
        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            status,
            0,
        )

        check(
            status[0] == GLES20.GL_TRUE,
        ) {
            val log =
                GLES20.glGetShaderInfoLog(
                    shader,
                )
            GLES20.glDeleteShader(
                shader,
            )
            "GL shader compile failed: $log"
        }

        return shader
    }

    private fun checkGl(
        operation: String,
    ) {
        val error =
            GLES20.glGetError()
        check(
            error == GLES20.GL_NO_ERROR,
        ) {
            "GL error after $operation: 0x" +
                Integer.toHexString(
                    error,
                )
        }
    }

    companion object {
        private const val EGL_RECORDABLE_ANDROID =
            0x3142

        private const val VERTEX_SHADER =
            """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
            """

        private const val FRAGMENT_SHADER =
            """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;

            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
            """

        private const val ALPHA_FRAGMENT_SHADER =
            """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uAlpha;
            varying vec2 vTexCoord;

            void main() {
                gl_FragColor =
                    texture2D(
                        uTexture,
                        vTexCoord
                    ) *
                    uAlpha;
            }
            """

        private const val GLOW_FRAGMENT_SHADER =
            """
            precision mediump float;

            uniform vec2 uCenterPx;
            uniform vec2 uViewportPx;
            uniform float uCircleRadiusPx;
            uniform float uGradientRadiusPx;
            uniform float uCenterAlpha;
            uniform float uMidAlpha;

            varying vec2 vTexCoord;

            void main() {
                vec2 pixel =
                    vTexCoord *
                    uViewportPx;
                float distancePx =
                    distance(
                        pixel,
                        uCenterPx
                    );

                if (
                    distancePx >
                        uCircleRadiusPx ||
                    distancePx >=
                        uGradientRadiusPx
                ) {
                    gl_FragColor =
                        vec4(0.0);
                    return;
                }

                float t =
                    clamp(
                        distancePx /
                            uGradientRadiusPx,
                        0.0,
                        1.0
                    );

                vec4 centerColor =
                    vec4(
                        0.0,
                        136.0 / 255.0,
                        1.0,
                        uCenterAlpha
                    );
                vec4 midColor =
                    vec4(
                        0.0,
                        229.0 / 255.0,
                        1.0,
                        uMidAlpha
                    );

                vec4 color =
                    t <= 0.46
                        ? mix(
                            centerColor,
                            midColor,
                            t / 0.46
                        )
                        : mix(
                            midColor,
                            vec4(0.0),
                            (t - 0.46) /
                                0.54
                        );

                gl_FragColor =
                    vec4(
                        color.rgb *
                            color.a,
                        color.a
                    );
            }
            """

        private fun floatBufferOf(
            vararg values: Float,
        ): FloatBuffer =
            ByteBuffer
                .allocateDirect(
                    values.size *
                        Float.SIZE_BYTES,
                )
                .order(
                    ByteOrder.nativeOrder(),
                )
                .asFloatBuffer()
                .apply {
                    put(
                        values,
                    )
                    position(
                        0,
                    )
                }
    }
}
