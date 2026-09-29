package com.saney.musicvisualizer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import com.saney.musicvisualizer.analysis.SceneSignal
import com.saney.musicvisualizer.theme.PlaybackThemeId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class HeroThemeView(context: Context) : View(context) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val path = Path()

    private var themeId = PlaybackThemeId.NEON_EMBLEM

    private var targetAmplitude = 0f
    private var targetBass = 0f
    private var targetMid = 0f
    private var targetHigh = 0f

    private var amplitude = 0f
    private var bass = 0f
    private var mid = 0f
    private var high = 0f
    private var beat = 0f

    private var playing = false
    private var lastFrameMs = SystemClock.elapsedRealtime()
    private var vinylRotation = 0f
    private var trackTitle = "FARIC"
    private var artistName = ""

    fun setTheme(theme: PlaybackThemeId) {
        themeId = theme
        postInvalidateOnAnimation()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        postInvalidateOnAnimation()
    }

    fun setMetadata(
        title: String?,
        artist: String?,
    ) {
        trackTitle =
            title
                ?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
                ?: "FARIC"

        artistName =
            artist
                ?.takeIf { it.isNotBlank() }
                ?: ""

        postInvalidateOnAnimation()
    }

    fun updateSignal(signal: SceneSignal) {
        targetAmplitude = signal.amplitude
        targetBass = signal.bass
        targetMid = signal.mid
        targetHigh = signal.high

        amplitude = max(amplitude, signal.amplitude * 0.96f)
        bass = max(bass, signal.bass * 0.98f)
        mid = max(mid, signal.mid * 0.96f)
        high = max(high, signal.high * 0.96f)
        beat = max(beat, signal.beatStrength)

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = SystemClock.elapsedRealtime()
        val dt =
            ((now - lastFrameMs).coerceIn(1L, 50L) / 1000f)

        lastFrameMs = now

        amplitude = follow(amplitude, targetAmplitude, dt, 58f, 12f)
        bass = follow(bass, targetBass, dt, 74f, 15f)
        mid = follow(mid, targetMid, dt, 54f, 13f)
        high = follow(high, targetHigh, dt, 62f, 14f)
        beat = (beat - dt * 3.4f).coerceAtLeast(0f)

        if (!playing) {
            targetAmplitude *= 0.90f
            targetBass *= 0.90f
            targetMid *= 0.90f
            targetHigh *= 0.90f
        } else {
            vinylRotation =
                (vinylRotation + dt * 34f) % 360f
        }

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w * 0.5f
        val cy = h * 0.40f
        val minSide = min(w, h)
        val time = now / 1000f

        drawBackground(canvas, w, h, cx, cy, minSide, time)
        drawParticles(canvas, w, h, cx, cy, minSide, time)

        when (themeId) {
            PlaybackThemeId.NEON_EMBLEM ->
                drawNeonEmblem(canvas, cx, cy, minSide, time)

            PlaybackThemeId.ENERGY_CORE ->
                drawEnergyCore(canvas, cx, cy, minSide, time)

            PlaybackThemeId.ORBITAL_CROWN ->
                drawOrbitalCrown(canvas, cx, cy, minSide, time)

            PlaybackThemeId.STAR_SEED ->
                drawStarSeed(canvas, cx, cy, minSide, time)

            PlaybackThemeId.WAVE_IDOL ->
                drawWaveIdol(canvas, cx, cy, minSide, time)

            PlaybackThemeId.VINYL ->
                drawVinyl(canvas, cx, cy, minSide)

            PlaybackThemeId.CASSETTE ->
                drawCassette(canvas, cx, cy, minSide)

            else ->
                drawNeonEmblem(canvas, cx, cy, minSide, time)
        }

        drawBeatShockwave(canvas, cx, cy, minSide)
        postInvalidateOnAnimation()
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        fill.shader =
            RadialGradient(
                cx,
                cy,
                minSide * (0.72f + bass * 0.08f),
                intArrayOf(
                    Color.rgb(8, 23, 28),
                    Color.rgb(3, 8, 14),
                    Color.BLACK,
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null

        val glowRadius =
            minSide * (
                0.23f +
                    amplitude * 0.10f +
                    bass * 0.12f +
                    beat * 0.10f
                )

        drawGlow(
            canvas,
            cx,
            cy,
            glowRadius,
            Color.rgb(0, 235, 213),
            0.12f + bass * 0.13f + beat * 0.18f,
        )

        val sidePulse =
            0.5f + 0.5f * sinF(time * 0.55f)

        drawGlow(
            canvas,
            w * (0.14f + sidePulse * 0.04f),
            h * 0.33f,
            minSide * 0.34f,
            Color.rgb(41, 84, 255),
            0.07f + high * 0.05f,
        )

        drawGlow(
            canvas,
            w * (0.86f - sidePulse * 0.04f),
            h * 0.51f,
            minSide * 0.31f,
            Color.rgb(255, 71, 184),
            0.05f + mid * 0.05f,
        )
    }

    private fun drawParticles(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val count = 54

        repeat(count) { index ->
            val seedA = hash01(index * 17 + 3)
            val seedB = hash01(index * 41 + 9)
            val seedC = hash01(index * 73 + 13)

            val drift =
                time * (
                    0.025f +
                        seedC * 0.035f +
                        high * 0.025f
                    )

            var y =
                (seedB + drift) % 1f

            if (y < 0f) y += 1f

            val x =
                seedA * w +
                    sinF(
                        time * (0.28f + seedC * 0.45f) +
                            index,
                    ) * minSide * 0.025f

            val py = h - y * h
            val dx = x - cx
            val dy = py - cy
            val dist =
                kotlin.math.sqrt(
                    dx * dx + dy * dy,
                )

            val centerBurst =
                (
                    1f -
                        (
                            dist /
                                (minSide * 0.55f)
                            ).coerceIn(0f, 1f)
                    )

            val radius =
                minSide * (
                    0.0015f +
                        seedC * 0.0025f +
                        beat * centerBurst * 0.003f
                    )

            fill.color =
                if (index % 3 == 0) {
                    Color.rgb(0, 232, 214)
                } else {
                    Color.rgb(153, 189, 255)
                }

            fill.alpha =
                (
                    45 +
                        high * 100f +
                        beat * centerBurst * 90f
                    ).toInt().coerceIn(0, 220)

            canvas.drawCircle(
                x,
                py,
                radius,
                fill,
            )
        }

        fill.alpha = 255
    }

    private fun drawNeonEmblem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val baseRadius =
            minSide * (
                0.135f +
                    bass * 0.055f +
                    beat * 0.040f
                )

        val points = 132
        path.reset()

        repeat(points + 1) { i ->
            val angle =
                i / points.toFloat() *
                    (2f * PI.toFloat())

            val local =
                sinF(angle * 5f + time * (1.4f + mid * 2.2f)) *
                    minSide * 0.010f +
                    sinF(angle * 11f - time * (2.0f + high * 3.0f)) *
                    minSide * 0.006f

            val spike =
                if (beat > 0.04f) {
                    sinF(angle * 17f + time * 4f) *
                        minSide * 0.010f *
                        beat
                } else {
                    0f
                }

            val radius = baseRadius + local + spike
            val x = cx + cosF(angle) * radius
            val y = cy + sinF(angle) * radius

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        stroke.strokeWidth =
            minSide * (0.006f + high * 0.004f)
        stroke.color = Color.rgb(33, 255, 219)
        stroke.alpha = 215
        canvas.drawPath(path, stroke)

        stroke.strokeWidth =
            minSide * (0.018f + beat * 0.010f)
        stroke.alpha = 38
        canvas.drawPath(path, stroke)

        drawCenterEmblem(
            canvas,
            cx,
            cy,
            minSide,
            "F",
            Color.rgb(33, 255, 219),
        )
    }

    private fun drawEnergyCore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val pulse =
            minSide * (
                0.075f +
                    bass * 0.060f +
                    beat * 0.055f
                )

        drawGlow(
            canvas,
            cx,
            cy,
            pulse * 3.4f,
            Color.rgb(255, 119, 31),
            0.18f + bass * 0.24f + beat * 0.30f,
        )

        fill.shader =
            RadialGradient(
                cx,
                cy,
                pulse,
                intArrayOf(
                    Color.WHITE,
                    Color.rgb(255, 159, 53),
                    Color.rgb(91, 12, 5),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            pulse,
            fill,
        )
        fill.shader = null

        val rays = 24
        repeat(rays) { i ->
            val angle =
                i / rays.toFloat() *
                    (2f * PI.toFloat()) +
                    time * 0.08f

            val noise =
                0.55f +
                    0.45f *
                    sinF(
                        time * (2.1f + high * 2.4f) +
                            i * 1.73f,
                    )

            val start = pulse * 0.85f
            val length =
                minSide * (
                    0.025f +
                        bass * 0.065f +
                        beat * 0.10f * noise
                    )

            stroke.color =
                if (i % 2 == 0) {
                    Color.rgb(255, 191, 72)
                } else {
                    Color.rgb(255, 84, 28)
                }
            stroke.alpha =
                (90 + beat * 135f).toInt().coerceIn(0, 230)
            stroke.strokeWidth =
                minSide * (0.0025f + high * 0.002f)

            canvas.drawLine(
                cx + cosF(angle) * start,
                cy + sinF(angle) * start,
                cx + cosF(angle) * (start + length),
                cy + sinF(angle) * (start + length),
                stroke,
            )
        }
    }

    private fun drawOrbitalCrown(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val radii =
            floatArrayOf(
                0.105f,
                0.155f,
                0.215f,
            )

        radii.forEachIndexed { index, factor ->
            val radius =
                minSide * (
                    factor +
                        bass * (0.020f + index * 0.007f) +
                        beat * (0.012f + index * 0.010f)
                    )

            stroke.color =
                when (index) {
                    0 -> Color.rgb(0, 245, 214)
                    1 -> Color.rgb(92, 159, 255)
                    else -> Color.rgb(218, 78, 255)
                }

            stroke.alpha =
                (140 + beat * 90f).toInt().coerceIn(0, 235)

            stroke.strokeWidth =
                minSide * (
                    0.004f +
                        high * 0.003f
                    )

            val sweep =
                160f + mid * 100f

            val start =
                (
                    time *
                        (24f + index * 13f) *
                        if (index % 2 == 0) 1f else -1f
                    ) % 360f

            canvas.drawArc(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius,
                start,
                sweep,
                false,
                stroke,
            )

            canvas.drawArc(
                cx - radius,
                cy - radius,
                cx + radius,
                cy + radius,
                start + 190f,
                90f + beat * 80f,
                false,
                stroke,
            )
        }

        drawCenterEmblem(
            canvas,
            cx,
            cy,
            minSide,
            "◈",
            Color.rgb(119, 230, 255),
        )
    }

    private fun drawStarSeed(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val points = 12
        path.reset()

        repeat(points + 1) { i ->
            val angle =
                i / points.toFloat() *
                    (2f * PI.toFloat()) -
                    PI.toFloat() / 2f

            val outer = i % 2 == 0

            val radius =
                minSide * (
                    if (outer) {
                        0.17f + bass * 0.055f + beat * 0.060f
                    } else {
                        0.075f + mid * 0.020f
                    }
                    )

            val wobble =
                minSide *
                    0.008f *
                    sinF(time * 1.8f + i * 1.4f)

            val x =
                cx +
                    cosF(angle + time * 0.05f) *
                    (radius + wobble)

            val y =
                cy +
                    sinF(angle + time * 0.05f) *
                    (radius + wobble)

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        path.close()

        fill.color = Color.rgb(11, 26, 46)
        fill.alpha = 190
        canvas.drawPath(path, fill)

        stroke.color = Color.rgb(87, 211, 255)
        stroke.strokeWidth =
            minSide * (0.005f + high * 0.003f)
        stroke.alpha = 220
        canvas.drawPath(path, stroke)

        val rays = 6
        repeat(rays) { i ->
            val angle =
                i / rays.toFloat() *
                    (2f * PI.toFloat()) +
                    time * 0.08f

            val start = minSide * 0.13f
            val end =
                minSide * (
                    0.25f +
                        bass * 0.10f +
                        beat * 0.18f
                    )

            stroke.color = Color.rgb(87, 211, 255)
            stroke.alpha =
                (70 + beat * 150f).toInt().coerceIn(0, 230)
            stroke.strokeWidth =
                minSide * 0.003f

            canvas.drawLine(
                cx + cosF(angle) * start,
                cy + sinF(angle) * start,
                cx + cosF(angle) * end,
                cy + sinF(angle) * end,
                stroke,
            )
        }
    }

    private fun drawCassette(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
    ) {
        val bodyWidth =
            minSide * (
                0.62f +
                    beat * 0.012f
                )

        val bodyHeight =
            minSide * (
                0.34f +
                    bass * 0.012f
                )

        val left = cx - bodyWidth / 2f
        val top = cy - bodyHeight / 2f
        val right = cx + bodyWidth / 2f
        val bottom = cy + bodyHeight / 2f
        val corner = minSide * 0.035f

        drawGlow(
            canvas,
            cx,
            cy,
            bodyWidth * 0.70f,
            Color.rgb(255, 128, 36),
            0.05f + bass * 0.08f + beat * 0.10f,
        )

        fill.color = Color.rgb(219, 210, 185)
        fill.alpha = 250
        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            corner,
            corner,
            fill,
        )

        stroke.color = Color.rgb(84, 69, 54)
        stroke.alpha = 220
        stroke.strokeWidth = minSide * 0.004f
        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            corner,
            corner,
            stroke,
        )

        val labelInset = minSide * 0.045f
        val labelTop = top + minSide * 0.035f
        val labelBottom = cy + minSide * 0.035f

        fill.color = Color.rgb(245, 137, 45)
        fill.alpha = 245
        canvas.drawRoundRect(
            left + labelInset,
            labelTop,
            right - labelInset,
            labelBottom,
            minSide * 0.018f,
            minSide * 0.018f,
            fill,
        )

        val reelY = cy + bodyHeight * 0.10f
        val reelOffset = bodyWidth * 0.205f
        val reelRadius =
            minSide * (
                0.070f +
                    bass * 0.008f
                )

        drawCassetteReel(
            canvas,
            cx - reelOffset,
            reelY,
            reelRadius,
            vinylRotation,
            minSide,
        )
        drawCassetteReel(
            canvas,
            cx + reelOffset,
            reelY,
            reelRadius,
            -vinylRotation * 1.07f,
            minSide,
        )

        val windowLeft = cx - bodyWidth * 0.13f
        val windowRight = cx + bodyWidth * 0.13f
        val windowTop = reelY - reelRadius * 0.62f
        val windowBottom = reelY + reelRadius * 0.62f

        fill.color = Color.rgb(35, 33, 29)
        fill.alpha = 240
        canvas.drawRoundRect(
            windowLeft,
            windowTop,
            windowRight,
            windowBottom,
            minSide * 0.012f,
            minSide * 0.012f,
            fill,
        )

        stroke.color = Color.rgb(105, 75, 45)
        stroke.alpha = 180
        stroke.strokeWidth = minSide * 0.003f
        canvas.drawLine(
            cx - reelOffset + reelRadius * 0.72f,
            reelY,
            windowLeft,
            reelY,
            stroke,
        )
        canvas.drawLine(
            windowRight,
            reelY,
            cx + reelOffset - reelRadius * 0.72f,
            reelY,
            stroke,
        )

        val lowerPlateTop =
            cy + bodyHeight * 0.27f

        path.reset()
        path.moveTo(
            cx - bodyWidth * 0.25f,
            lowerPlateTop,
        )
        path.lineTo(
            cx + bodyWidth * 0.25f,
            lowerPlateTop,
        )
        path.lineTo(
            cx + bodyWidth * 0.18f,
            bottom - minSide * 0.035f,
        )
        path.lineTo(
            cx - bodyWidth * 0.18f,
            bottom - minSide * 0.035f,
        )
        path.close()

        fill.color = Color.rgb(84, 77, 66)
        fill.alpha = 220
        canvas.drawPath(path, fill)

        val title =
            trackTitle
                .replace('_', ' ')
                .take(28)

        textPaint.color = Color.rgb(44, 28, 17)
        textPaint.alpha = 245
        textPaint.textSize = minSide * 0.030f

        canvas.drawText(
            title,
            cx,
            labelTop + minSide * 0.045f,
            textPaint,
        )

        textPaint.textSize = minSide * 0.020f
        textPaint.alpha = 205

        canvas.drawText(
            if (artistName.isBlank()) {
                "FARIC MIX"
            } else {
                artistName.take(30)
            },
            cx,
            labelTop + minSide * 0.080f,
            textPaint,
        )

        val beatMarker =
            minSide * (
                0.010f +
                    beat * 0.020f
                )

        fill.color = Color.rgb(255, 62, 37)
        fill.alpha =
            (120 + beat * 120f).toInt().coerceIn(0, 255)

        canvas.drawCircle(
            right - labelInset * 1.45f,
            labelTop + labelInset * 0.55f,
            beatMarker,
            fill,
        )
    }

    private fun drawCassetteReel(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        rotationDegrees: Float,
        minSide: Float,
    ) {
        fill.color = Color.rgb(48, 44, 39)
        fill.alpha = 255
        canvas.drawCircle(
            x,
            y,
            radius,
            fill,
        )

        stroke.color = Color.rgb(230, 220, 194)
        stroke.alpha = 230
        stroke.strokeWidth = minSide * 0.003f
        canvas.drawCircle(
            x,
            y,
            radius * 0.68f,
            stroke,
        )

        val rotation =
            rotationDegrees /
                180f *
                PI.toFloat()

        repeat(6) { i ->
            val angle =
                rotation +
                    i / 6f *
                    (2f * PI.toFloat())

            val inner = radius * 0.20f
            val outer = radius * 0.58f

            stroke.color = Color.rgb(230, 220, 194)
            stroke.alpha = 215
            stroke.strokeWidth = minSide * 0.005f

            canvas.drawLine(
                x + cosF(angle) * inner,
                y + sinF(angle) * inner,
                x + cosF(angle) * outer,
                y + sinF(angle) * outer,
                stroke,
            )
        }

        fill.color = Color.rgb(219, 210, 185)
        fill.alpha = 255
        canvas.drawCircle(
            x,
            y,
            radius * 0.18f,
            fill,
        )
    }

    private fun drawVinyl(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
    ) {
        val recordRadius =
            minSide * (
                0.245f +
                    bass * 0.016f +
                    beat * 0.012f
                )

        drawGlow(
            canvas,
            cx,
            cy,
            recordRadius * 1.35f,
            Color.rgb(255, 112, 38),
            0.08f + bass * 0.13f + beat * 0.11f,
        )

        fill.color = Color.rgb(6, 7, 10)
        fill.alpha = 248
        canvas.drawCircle(
            cx,
            cy,
            recordRadius,
            fill,
        )

        repeat(11) { index ->
            val grooveRadius =
                recordRadius * (
                    0.32f +
                        index * 0.058f
                    )

            stroke.color =
                if (index % 2 == 0) {
                    Color.rgb(44, 49, 55)
                } else {
                    Color.rgb(23, 27, 32)
                }

            stroke.alpha =
                120 + (high * 45f).toInt()
            stroke.strokeWidth =
                minSide * 0.0014f

            canvas.drawCircle(
                cx,
                cy,
                grooveRadius,
                stroke,
            )
        }

        val markerRadius = recordRadius * 0.80f
        val markerAngle =
            vinylRotation /
                180f *
                PI.toFloat()

        stroke.color = Color.rgb(255, 255, 255)
        stroke.alpha = 70
        stroke.strokeWidth = minSide * 0.0025f

        canvas.drawCircle(
            cx + cosF(markerAngle) * markerRadius,
            cy + sinF(markerAngle) * markerRadius,
            minSide * 0.007f,
            stroke,
        )

        val labelRadius =
            recordRadius * (
                0.31f +
                    beat * 0.02f
                )

        fill.shader =
            RadialGradient(
                cx,
                cy,
                labelRadius,
                intArrayOf(
                    Color.rgb(255, 176, 51),
                    Color.rgb(202, 75, 28),
                    Color.rgb(83, 18, 15),
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        canvas.drawCircle(
            cx,
            cy,
            labelRadius,
            fill,
        )
        fill.shader = null

        stroke.color = Color.rgb(255, 216, 125)
        stroke.alpha = 200
        stroke.strokeWidth = minSide * 0.0025f
        canvas.drawCircle(
            cx,
            cy,
            labelRadius * 0.94f,
            stroke,
        )

        fill.color = Color.rgb(235, 216, 174)
        fill.alpha = 255
        canvas.drawCircle(
            cx,
            cy,
            minSide * 0.010f,
            fill,
        )

        val title =
            trackTitle
                .replace('_', ' ')
                .take(22)

        textPaint.color = Color.WHITE
        textPaint.alpha = 235
        textPaint.textSize =
            labelRadius * 0.22f

        canvas.save()
        canvas.rotate(
            vinylRotation,
            cx,
            cy,
        )

        canvas.drawText(
            title,
            cx,
            cy - labelRadius * 0.15f,
            textPaint,
        )

        textPaint.textSize =
            labelRadius * 0.14f
        textPaint.alpha = 190

        canvas.drawText(
            if (artistName.isBlank()) {
                "FARIC VINYL"
            } else {
                artistName.take(24)
            },
            cx,
            cy + labelRadius * 0.22f,
            textPaint,
        )

        canvas.restore()
    }

    private fun drawWaveIdol(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        time: Float,
    ) {
        val height =
            minSide * (
                0.22f +
                    beat * 0.055f
                )

        val segments = 56

        path.reset()

        repeat(segments + 1) { i ->
            val p =
                i / segments.toFloat()

            val y =
                cy -
                    height +
                    p * height * 2f

            val normalized =
                abs(p * 2f - 1f)

            val width =
                minSide * (
                    0.055f +
                        (1f - normalized) * 0.065f +
                        bass * 0.040f +
                        sinF(
                            p * 18f -
                                time * (2.0f + mid * 2.5f),
                        ) * 0.014f
                    )

            val x = cx + width

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        repeat(segments + 1) { index ->
            val i = segments - index
            val p =
                i / segments.toFloat()

            val y =
                cy -
                    height +
                    p * height * 2f

            val normalized =
                abs(p * 2f - 1f)

            val width =
                minSide * (
                    0.055f +
                        (1f - normalized) * 0.065f +
                        bass * 0.040f +
                        sinF(
                            p * 18f -
                                time * (2.0f + mid * 2.5f),
                        ) * 0.014f
                    )

            path.lineTo(cx - width, y)
        }

        path.close()

        fill.shader =
            RadialGradient(
                cx,
                cy,
                minSide * 0.22f,
                intArrayOf(
                    Color.rgb(7, 44, 47),
                    Color.rgb(8, 17, 27),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        fill.alpha = 225
        canvas.drawPath(path, fill)
        fill.shader = null

        stroke.color = Color.rgb(0, 245, 214)
        stroke.alpha = 220
        stroke.strokeWidth =
            minSide * (0.004f + high * 0.003f)
        canvas.drawPath(path, stroke)

        drawCenterEmblem(
            canvas,
            cx,
            cy,
            minSide,
            "∿",
            Color.rgb(0, 245, 214),
        )
    }

    private fun drawCenterEmblem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        symbol: String,
        color: Int,
    ) {
        val radius =
            minSide * (
                0.068f +
                    bass * 0.015f +
                    beat * 0.015f
                )

        fill.color = Color.rgb(4, 9, 14)
        fill.alpha = 225
        canvas.drawCircle(
            cx,
            cy,
            radius,
            fill,
        )

        stroke.color = color
        stroke.alpha = 220
        stroke.strokeWidth =
            minSide * (0.004f + beat * 0.002f)

        canvas.drawCircle(
            cx,
            cy,
            radius,
            stroke,
        )

        textPaint.color = Color.WHITE
        textPaint.alpha = 235
        textPaint.textSize = radius * 0.78f

        canvas.drawText(
            symbol,
            cx,
            cy - (textPaint.ascent() + textPaint.descent()) / 2f,
            textPaint,
        )
    }

    private fun drawBeatShockwave(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
    ) {
        if (beat <= 0.02f) return

        val radius =
            minSide * (
                0.19f +
                    (1f - beat) * 0.28f +
                    bass * 0.05f
                )

        stroke.color = Color.rgb(104, 239, 255)
        stroke.alpha =
            (beat * 130f).toInt().coerceIn(0, 145)
        stroke.strokeWidth =
            minSide * (0.002f + beat * 0.005f)

        canvas.drawCircle(
            cx,
            cy,
            radius,
            stroke,
        )
    }

    private fun drawGlow(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Float,
    ) {
        if (radius <= 0f || alpha <= 0f) return

        fill.shader =
            RadialGradient(
                x,
                y,
                radius,
                intArrayOf(
                    withAlpha(
                        color,
                        alpha.coerceIn(0f, 1f),
                    ),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )

        fill.alpha = 255
        canvas.drawCircle(
            x,
            y,
            radius,
            fill,
        )
        fill.shader = null
    }

    private fun follow(
        current: Float,
        target: Float,
        dt: Float,
        attackHz: Float,
        releaseHz: Float,
    ): Float {
        val rate =
            if (target > current) attackHz
            else releaseHz

        val factor =
            (dt * rate).coerceIn(0f, 1f)

        return current +
            (target - current) * factor
    }

    private fun withAlpha(
        color: Int,
        alpha: Float,
    ): Int =
        Color.argb(
            (255 * alpha).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun hash01(seed: Int): Float {
        val value =
            sinF(seed * 12.9898f) *
                43758.5453f

        return abs(
            value -
                kotlin.math.floor(
                    value.toDouble(),
                ).toFloat(),
        )
    }

    private fun sinF(value: Float): Float =
        kotlin.math.sin(
            value.toDouble(),
        ).toFloat()

    private fun cosF(value: Float): Float =
        kotlin.math.cos(
            value.toDouble(),
        ).toFloat()
}
