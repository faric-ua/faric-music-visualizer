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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

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
    val overlayNs: Long,
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
    private val positionHandle: Int
    private val texCoordHandle: Int
    private val samplerHandle: Int

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
                1,
            )
        GLES20.glGenTextures(
            1,
            textureIds,
            0,
        )
        textureId =
            textureIds[0]

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
    }

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
            overlayNs = overlayNs,
        )
    }

    fun drawProjectMComposite(
        projectMBitmap: Bitmap,
        overlayBitmap: Bitmap,
        glow: CyberSharkGpuGlow,
        presentationTimeNs: Long,
    ): EglCompositeDrawTiming {
        validateBitmap(
            projectMBitmap,
        )
        validateBitmap(
            overlayBitmap,
        )
        makeCurrent()
        beginFrame()

        val projectMStartedNs =
            System.nanoTime()

        drawBitmapLayer(
            bitmap = projectMBitmap,
            alphaBlend = false,
            rawGlOrientation = true,
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
            overlayNs = overlayNs,
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
    ) {
        vertexBuffer.position(
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
            vertexBuffer,
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
            GLES20.glDeleteTextures(
                1,
                intArrayOf(
                    textureId,
                ),
                0,
            )
            GLES20.glDeleteProgram(
                program,
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
