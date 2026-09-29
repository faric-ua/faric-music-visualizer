package com.saney.musicvisualizer.theme

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import kotlin.math.max

class HeroThemeView(context: Context) : View(context) {
    private var themeId =
        PlaybackThemeId.NEON_EMBLEM

    private var trackTitle = ""
    private var artist = ""
    private var durationMs = 0L
    private var positionMs = 0L
    private var playing = false

    private var targetAmplitude = 0f
    private var targetBass = 0f
    private var targetMid = 0f
    private var targetHigh = 0f

    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f

    private var lastFrameMs =
        SystemClock.elapsedRealtime()

    private val startedMs =
        SystemClock.elapsedRealtime()

    fun setTheme(
        themeId: PlaybackThemeId,
    ) {
        this.themeId = themeId
        postInvalidateOnAnimation()
    }

    fun setTrack(
        title: String?,
        artist: String,
        positionMs: Long,
        durationMs: Long,
    ) {
        trackTitle = title.orEmpty()
        this.artist = artist
        this.positionMs = positionMs
        this.durationMs = durationMs
        postInvalidateOnAnimation()
    }

    fun setPlaying(
        value: Boolean,
    ) {
        playing = value
        postInvalidateOnAnimation()
    }

    fun updateSignal(
        signal: SceneSignal,
    ) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high

        amplitude =
            max(
                amplitude,
                signal.amplitude * 0.95f,
            )
        bass =
            max(
                bass,
                signal.bass * 0.98f,
            )
        mid =
            max(
                mid,
                signal.mid * 0.95f,
            )
        high =
            max(
                high,
                signal.high * 0.95f,
            )
        beat =
            max(
                beat,
                signal.beatStrength,
            )

        postInvalidateOnAnimation()
    }

    override fun onDraw(
        canvas: Canvas,
    ) {
        super.onDraw(canvas)

        val now =
            SystemClock.elapsedRealtime()

        val dt =
            (
                (
                    now -
                        lastFrameMs
                    )
                    .coerceIn(1L, 50L)
                    .toFloat() /
                    1000f
                )

        lastFrameMs = now

        amplitude =
            follow(
                amplitude,
                targetAmplitude,
                dt,
                52f,
                12f,
            )
        bass =
            follow(
                bass,
                targetBass,
                dt,
                72f,
                15f,
            )
        mid =
            follow(
                mid,
                targetMid,
                dt,
                50f,
                12f,
            )
        high =
            follow(
                high,
                targetHigh,
                dt,
                58f,
                14f,
            )

        beat =
            (
                beat -
                    dt * 3f
                )
                .coerceAtLeast(0f)

        if (!playing) {
            targetAmplitude *= 0.90f
            targetBass *= 0.90f
            targetMid *= 0.90f
            targetHigh *= 0.90f
        }

        HeroThemeRenderer.render(
            canvas = canvas,
            width = width,
            height = height,
            timeMs = now - startedMs,
            themeId = themeId,
            input = ThemeInput(
                title = trackTitle,
                artist = artist,
                durationMs = durationMs,
                positionMs = positionMs,
                amplitude = amplitude,
                bass = bass,
                mid = mid,
                high = high,
                beat = beat,
            ),
        )

        postInvalidateOnAnimation()
    }

    private fun follow(
        current: Float,
        target: Float,
        dt: Float,
        attackHz: Float,
        releaseHz: Float,
    ): Float {
        val rate =
            if (target > current) {
                attackHz
            } else {
                releaseHz
            }

        return current +
            (target - current) *
            (dt * rate)
                .coerceIn(0f, 1f)
    }
}
