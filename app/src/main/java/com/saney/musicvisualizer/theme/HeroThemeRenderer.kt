package com.saney.musicvisualizer.theme

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

object HeroThemeRenderer {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    fun render(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        themeId: PlaybackThemeId,
        input: ThemeInput,
    ) {
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val minSide = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.39f
        val t = timeMs / 1000f

        drawBackground(canvas, w, h, cx, cy, t, input, themeId)
        drawParticles(canvas, w, h, cx, cy, t, input)

        when (themeId) {
            PlaybackThemeId.NEON_EMBLEM ->
                drawNeonEmblem(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.ENERGY_CORE ->
                drawEnergyCore(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.ORBITAL_CROWN ->
                drawOrbitalCrown(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.STAR_SEED ->
                drawStarSeed(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.WAVE_IDOL ->
                drawWaveIdol(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.VINYL ->
                drawVinyl(canvas, cx, cy, minSide, t, input)
            PlaybackThemeId.CASSETTE ->
                drawCassette(canvas, cx, cy, minSide, t, input)
            else ->
                drawNeonEmblem(canvas, cx, cy, minSide, t, input)
        }

        drawMetadata(canvas, w, h, input)
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        t: Float,
        input: ThemeInput,
        themeId: PlaybackThemeId,
    ) {
        val accentA = when (themeId) {
            PlaybackThemeId.ENERGY_CORE -> Color.rgb(255, 118, 35)
            PlaybackThemeId.ORBITAL_CROWN -> Color.rgb(92, 225, 255)
            PlaybackThemeId.STAR_SEED -> Color.rgb(152, 106, 255)
            PlaybackThemeId.WAVE_IDOL -> Color.rgb(46, 255, 185)
            else -> Color.rgb(36, 255, 182)
        }
        val accentB = when (themeId) {
            PlaybackThemeId.ENERGY_CORE -> Color.rgb(255, 210, 86)
            PlaybackThemeId.ORBITAL_CROWN -> Color.rgb(155, 82, 255)
            PlaybackThemeId.STAR_SEED -> Color.rgb(32, 210, 255)
            PlaybackThemeId.WAVE_IDOL -> Color.rgb(255, 68, 198)
            else -> Color.rgb(20, 118, 255)
        }

        fill.shader = LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(
                Color.rgb(1, 5, 9),
                darken(accentB, 0.12f),
                Color.rgb(2, 3, 8),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null

        val pulse =
            0.10f +
                input.bass * 0.11f +
                input.beat * 0.13f

        fill.shader = RadialGradient(
            cx,
            cy,
            min(w, h) * (0.46f + pulse),
            intArrayOf(
                withAlpha(accentA, 0.16f + input.beat * 0.10f),
                withAlpha(accentB, 0.06f + input.high * 0.06f),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, min(w, h) * 0.64f, fill)
        fill.shader = null

        val sweep = h * (0.15f + 0.04f * sinf(t * 0.28f))
        fill.shader = LinearGradient(
            0f,
            sweep,
            w,
            sweep + h * 0.32f,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(accentA, 0.045f),
                Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
    }

    private fun drawParticles(
        canvas: Canvas,
        w: Float,
        h: Float,
        cx: Float,
        cy: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val minSide = min(w, h)

        repeat(58) { i ->
            val angle =
                hash01(i * 31 + 7) *
                    PI.toFloat() *
                    2f

            val speed =
                0.06f +
                    hash01(i * 17 + 3) *
                    0.18f

            val radius =
                minSide *
                    (
                        0.18f +
                            0.52f *
                            fract(
                                hash01(i * 61 + 11) +
                                    t * speed * 0.08f,
                            )
                        )

            val flutter =
                sinf(
                    t *
                        (
                            0.4f +
                                hash01(i * 19) *
                                1.4f
                            ) +
                        i,
                ) *
                    minSide *
                    0.014f

            val x =
                cx +
                    cosf(angle) *
                    radius +
                    flutter

            val y =
                cy +
                    sinf(angle) *
                    radius *
                    1.18f

            val energy =
                (
                    0.25f +
                        input.high * 0.58f +
                        input.beat * 0.30f
                    )
                    .coerceIn(0f, 1f)

            val dot =
                0.7f +
                    hash01(i * 13 + 9) *
                    2.3f +
                    input.beat *
                    1.3f

            fill.color =
                if (i % 4 == 0) {
                    withAlpha(
                        Color.rgb(255, 163, 66),
                        energy * 0.62f,
                    )
                } else {
                    withAlpha(
                        Color.rgb(85, 224, 255),
                        energy * 0.54f,
                    )
                }

            canvas.drawCircle(x, y, dot, fill)
        }
    }

    private fun drawNeonEmblem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val baseRadius =
            minSide *
                (
                    0.15f +
                        input.bass * 0.085f +
                        input.beat * 0.055f
                    )

        repeat(3) { layer ->
            path.reset()
            val points = 180

            for (i in 0..points) {
                val a =
                    i.toFloat() /
                        points *
                        PI.toFloat() *
                        2f

                val local =
                    1f +
                        0.11f *
                        sinf(
                            a * 5f +
                                t *
                                (
                                    1.15f +
                                        input.mid * 2.5f
                                    ),
                        ) +
                        0.055f *
                        sinf(
                            a * 11f -
                                t *
                                (
                                    1.8f +
                                        input.high * 3f
                                    ),
                        ) +
                        input.beat *
                        0.08f *
                        sinf(a * 3f)

                val r =
                    baseRadius *
                        local *
                        (
                            1f +
                                layer * 0.035f
                            )

                val x = cx + cosf(a) * r
                val y = cy + sinf(a) * r

                if (i == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }

            stroke.strokeWidth =
                1.6f +
                    layer * 2f +
                    input.beat * 2.8f
            stroke.color =
                if (layer == 1) {
                    Color.rgb(255, 167, 61)
                } else {
                    Color.rgb(47, 236, 255)
                }
            stroke.alpha =
                when (layer) {
                    0 -> 245
                    1 -> 128
                    else -> 70
                }

            canvas.drawPath(path, stroke)
        }

        drawCenterBadge(
            canvas,
            cx,
            cy,
            baseRadius * 0.70f,
            "F",
            input,
            Color.rgb(40, 232, 255),
        )

        drawShockwave(
            canvas,
            cx,
            cy,
            minSide,
            input,
            Color.rgb(255, 155, 46),
        )
    }

    private fun drawEnergyCore(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val coreRadius =
            minSide *
                (
                    0.085f +
                        input.bass * 0.045f +
                        input.beat * 0.035f
                    )

        repeat(5) { i ->
            val phase =
                t *
                    (
                        0.8f +
                            i * 0.13f
                        )

            val orbit =
                coreRadius *
                    (
                        1.55f +
                            i * 0.22f
                        )

            val x =
                cx +
                    cosf(
                        phase +
                            i * 1.4f,
                    ) *
                    orbit *
                    0.42f

            val y =
                cy +
                    sinf(
                        phase * 1.15f +
                            i,
                    ) *
                    orbit *
                    0.28f

            fill.shader = RadialGradient(
                x,
                y,
                coreRadius * (1.3f + input.beat * 0.8f),
                intArrayOf(
                    withAlpha(
                        Color.rgb(255, 196, 78),
                        0.34f,
                    ),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )

            canvas.drawCircle(
                x,
                y,
                coreRadius * 1.8f,
                fill,
            )
            fill.shader = null
        }

        fill.shader = RadialGradient(
            cx,
            cy,
            coreRadius * 2.1f,
            intArrayOf(
                Color.WHITE,
                Color.rgb(255, 163, 43),
                Color.rgb(132, 26, 4),
                Color.TRANSPARENT,
            ),
            floatArrayOf(
                0f,
                0.22f,
                0.55f,
                1f,
            ),
            Shader.TileMode.CLAMP,
        )

        canvas.drawCircle(
            cx,
            cy,
            coreRadius * 2.1f,
            fill,
        )
        fill.shader = null

        drawShockwave(
            canvas,
            cx,
            cy,
            minSide,
            input,
            Color.rgb(255, 143, 31),
        )
    }

    private fun drawOrbitalCrown(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val base =
            minSide *
                (
                    0.115f +
                        input.bass * 0.035f +
                        input.beat * 0.022f
                    )

        repeat(4) { ring ->
            val radius =
                base *
                    (
                        1f +
                            ring * 0.34f
                        )

            val sweep =
                70f +
                    input.mid * 95f +
                    ring * 12f

            val direction =
                if (ring % 2 == 0) 1f
                else -1f

            val rotation =
                t *
                    (
                        22f +
                            ring * 9f +
                            input.high * 38f
                        ) *
                    direction

            stroke.strokeWidth =
                2.2f +
                    ring * 0.7f +
                    input.beat * 2f

            stroke.color =
                if (ring % 2 == 0) {
                    Color.rgb(51, 226, 255)
                } else {
                    Color.rgb(167, 87, 255)
                }

            stroke.alpha =
                210 - ring * 26

            repeat(3) { segment ->
                canvas.drawArc(
                    cx - radius,
                    cy - radius,
                    cx + radius,
                    cy + radius,
                    rotation +
                        segment * 120f,
                    sweep,
                    false,
                    stroke,
                )
            }
        }

        drawCenterBadge(
            canvas,
            cx,
            cy,
            base * 0.64f,
            "O",
            input,
            Color.rgb(126, 91, 255),
        )
    }

    private fun drawStarSeed(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val inner =
            minSide *
                (
                    0.070f +
                        input.bass * 0.028f
                    )

        val outer =
            minSide *
                (
                    0.17f +
                        input.bass * 0.085f +
                        input.beat * 0.105f
                    )

        path.reset()

        repeat(12) { i ->
            val a =
                -PI.toFloat() / 2f +
                    i *
                    PI.toFloat() /
                    6f +
                    t * 0.11f

            val r =
                if (i % 2 == 0) outer
                else inner

            val x = cx + cosf(a) * r
            val y = cy + sinf(a) * r

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        path.close()

        fill.shader = RadialGradient(
            cx,
            cy,
            outer,
            intArrayOf(
                Color.WHITE,
                Color.rgb(94, 217, 255),
                withAlpha(
                    Color.rgb(152, 80, 255),
                    0.16f,
                ),
            ),
            null,
            Shader.TileMode.CLAMP,
        )

        fill.alpha =
            (
                185 +
                    input.beat * 70f
                )
                .toInt()
                .coerceIn(0, 255)

        canvas.drawPath(path, fill)
        fill.shader = null
        fill.alpha = 255

        repeat(6) { i ->
            val a =
                i *
                    PI.toFloat() /
                    3f +
                    t * 0.15f

            val ray =
                outer *
                    (
                        1.15f +
                            input.beat * 1.8f +
                            input.high * 0.3f
                        )

            stroke.color =
                Color.rgb(88, 225, 255)
            stroke.alpha =
                (
                    70 +
                        input.beat * 150f
                    )
                    .toInt()
                    .coerceIn(0, 255)
            stroke.strokeWidth =
                1.2f +
                    input.beat * 2.8f

            canvas.drawLine(
                cx + cosf(a) * inner,
                cy + sinf(a) * inner,
                cx + cosf(a) * ray,
                cy + sinf(a) * ray,
                stroke,
            )
        }
    }

    private fun drawWaveIdol(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        path.reset()

        val height =
            minSide *
                (
                    0.34f +
                        input.beat * 0.08f
                    )

        val steps = 96

        fun widthAt(
            normalizedY: Float,
        ): Float {
            val body =
                0.042f +
                    0.095f *
                    (
                        1f -
                            abs(normalizedY)
                                .pow(1.7f)
                        )

            val wave =
                0.020f *
                    sinf(
                        normalizedY *
                            13f -
                            t *
                            (
                                2.4f +
                                    input.mid * 3f
                                ),
                    )

            return minSide *
                (
                    body +
                        wave +
                        input.bass * 0.030f
                    )
        }

        for (i in 0..steps) {
            val p = i.toFloat() / steps
            val ny = -1f + p * 2f
            val x = cx + widthAt(ny)
            val y = cy + ny * height * 0.5f

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        for (i in steps downTo 0) {
            val p = i.toFloat() / steps
            val ny = -1f + p * 2f
            val x = cx - widthAt(ny)
            val y = cy + ny * height * 0.5f
            path.lineTo(x, y)
        }

        path.close()

        fill.shader = LinearGradient(
            cx - minSide * 0.16f,
            cy,
            cx + minSide * 0.16f,
            cy,
            intArrayOf(
                withAlpha(
                    Color.rgb(32, 255, 183),
                    0.25f,
                ),
                withAlpha(
                    Color.rgb(255, 68, 204),
                    0.78f,
                ),
                withAlpha(
                    Color.rgb(42, 229, 255),
                    0.25f,
                ),
            ),
            null,
            Shader.TileMode.CLAMP,
        )

        canvas.drawPath(path, fill)
        fill.shader = null

        stroke.color =
            Color.rgb(115, 255, 219)
        stroke.strokeWidth =
            1.8f +
                input.beat * 2.8f
        stroke.alpha = 220
        canvas.drawPath(path, stroke)

        drawShockwave(
            canvas,
            cx,
            cy,
            minSide,
            input,
            Color.rgb(255, 62, 188),
        )
    }

    private fun drawVinyl(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val rotation =
            t * (
                34f +
                    input.mid * 18f
                )

        val radius =
            minSide *
                (
                    0.245f +
                        input.bass * 0.016f +
                        input.beat * 0.012f
                    )

        fill.color = Color.rgb(6, 7, 10)
        fill.alpha = 248
        canvas.drawCircle(cx, cy, radius, fill)

        repeat(11) { index ->
            val groove =
                radius *
                    (
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
                110 +
                    (input.high * 55f)
                        .toInt()
            stroke.strokeWidth =
                minSide * 0.0014f
            canvas.drawCircle(
                cx,
                cy,
                groove,
                stroke,
            )
        }

        val labelRadius =
            radius *
                (
                    0.31f +
                        input.beat * 0.02f
                    )

        fill.shader = RadialGradient(
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

        canvas.save()
        canvas.rotate(
            rotation,
            cx,
            cy,
        )

        fill.textAlign = Paint.Align.CENTER
        fill.isFakeBoldText = true
        fill.color = Color.WHITE
        fill.textSize =
            labelRadius * 0.22f
        canvas.drawText(
            input.title
                .substringBeforeLast(
                    '.',
                    input.title,
                )
                .take(22),
            cx,
            cy - labelRadius * 0.14f,
            fill,
        )

        fill.isFakeBoldText = false
        fill.textSize =
            labelRadius * 0.14f
        fill.color =
            Color.rgb(255, 225, 185)
        canvas.drawText(
            input.artist
                .ifBlank { "FARIC VINYL" }
                .take(24),
            cx,
            cy + labelRadius * 0.22f,
            fill,
        )

        canvas.restore()

        fill.color =
            Color.rgb(235, 216, 174)
        canvas.drawCircle(
            cx,
            cy,
            minSide * 0.010f,
            fill,
        )
    }

    private fun drawCassette(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        t: Float,
        input: ThemeInput,
    ) {
        val bodyWidth =
            minSide *
                (
                    0.62f +
                        input.beat * 0.012f
                    )
        val bodyHeight =
            minSide *
                (
                    0.34f +
                        input.bass * 0.012f
                    )

        val left =
            cx - bodyWidth / 2f
        val top =
            cy - bodyHeight / 2f
        val right =
            cx + bodyWidth / 2f
        val bottom =
            cy + bodyHeight / 2f
        val corner =
            minSide * 0.035f

        fill.color =
            Color.rgb(219, 210, 185)
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

        stroke.color =
            Color.rgb(84, 69, 54)
        stroke.alpha = 220
        stroke.strokeWidth =
            minSide * 0.004f
        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            corner,
            corner,
            stroke,
        )

        val labelInset =
            minSide * 0.045f
        val labelTop =
            top + minSide * 0.035f
        val labelBottom =
            cy + minSide * 0.035f

        fill.color =
            Color.rgb(245, 137, 45)
        canvas.drawRoundRect(
            left + labelInset,
            labelTop,
            right - labelInset,
            labelBottom,
            minSide * 0.018f,
            minSide * 0.018f,
            fill,
        )

        val reelY =
            cy + bodyHeight * 0.10f
        val reelOffset =
            bodyWidth * 0.205f
        val reelRadius =
            minSide *
                (
                    0.070f +
                        input.bass * 0.008f
                    )

        drawCassetteReel(
            canvas,
            cx - reelOffset,
            reelY,
            reelRadius,
            t * 120f,
            minSide,
        )
        drawCassetteReel(
            canvas,
            cx + reelOffset,
            reelY,
            reelRadius,
            -t * 128f,
            minSide,
        )

        fill.color =
            Color.rgb(35, 33, 29)
        canvas.drawRoundRect(
            cx - bodyWidth * 0.13f,
            reelY - reelRadius * 0.62f,
            cx + bodyWidth * 0.13f,
            reelY + reelRadius * 0.62f,
            minSide * 0.012f,
            minSide * 0.012f,
            fill,
        )

        fill.textAlign =
            Paint.Align.CENTER
        fill.isFakeBoldText = true
        fill.color =
            Color.rgb(44, 28, 17)
        fill.textSize =
            minSide * 0.030f

        canvas.drawText(
            input.title
                .substringBeforeLast(
                    '.',
                    input.title,
                )
                .take(28),
            cx,
            labelTop + minSide * 0.045f,
            fill,
        )

        fill.isFakeBoldText = false
        fill.textSize =
            minSide * 0.020f
        canvas.drawText(
            input.artist
                .ifBlank { "FARIC MIX" }
                .take(30),
            cx,
            labelTop + minSide * 0.075f,
            fill,
        )
    }

    private fun drawCassetteReel(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        degrees: Float,
        minSide: Float,
    ) {
        fill.color =
            Color.rgb(40, 37, 32)
        canvas.drawCircle(
            x,
            y,
            radius,
            fill,
        )

        fill.color =
            Color.rgb(233, 224, 196)
        canvas.drawCircle(
            x,
            y,
            radius * 0.56f,
            fill,
        )

        val base =
            degrees /
                180f *
                PI.toFloat()

        repeat(6) { index ->
            val angle =
                base +
                    index *
                    PI.toFloat() /
                    3f

            stroke.color =
                Color.rgb(92, 76, 58)
            stroke.alpha = 220
            stroke.strokeWidth =
                minSide * 0.005f

            canvas.drawLine(
                x +
                    cosf(angle) *
                    radius * 0.18f,
                y +
                    sinf(angle) *
                    radius * 0.18f,
                x +
                    cosf(angle) *
                    radius * 0.52f,
                y +
                    sinf(angle) *
                    radius * 0.52f,
                stroke,
            )
        }
    }

    private fun drawCenterBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        label: String,
        input: ThemeInput,
        accent: Int,
    ) {
        fill.shader = RadialGradient(
            cx,
            cy,
            radius * 1.6f,
            intArrayOf(
                Color.rgb(18, 25, 32),
                Color.rgb(3, 8, 13),
                Color.TRANSPARENT,
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(
            cx,
            cy,
            radius * 1.6f,
            fill,
        )
        fill.shader = null

        stroke.color = accent
        stroke.alpha = 225
        stroke.strokeWidth =
            2.2f +
                input.beat * 2.4f
        canvas.drawCircle(cx, cy, radius, stroke)

        fill.color = Color.WHITE
        fill.textAlign = Paint.Align.CENTER
        fill.textSize = radius * 0.88f
        fill.isFakeBoldText = true
        canvas.drawText(
            label,
            cx,
            cy + fill.textSize * 0.33f,
            fill,
        )
        fill.isFakeBoldText = false
    }

    private fun drawShockwave(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        minSide: Float,
        input: ThemeInput,
        color: Int,
    ) {
        val beat =
            input.beat.coerceIn(0f, 1f)
        if (beat <= 0.02f) return

        val radius =
            minSide *
                (
                    0.19f +
                        (1f - beat) *
                        0.18f
                    )

        stroke.color = color
        stroke.alpha =
            (
                beat * 190f
                )
                .toInt()
                .coerceIn(0, 255)
        stroke.strokeWidth =
            1.2f +
                beat * 4.2f

        canvas.drawCircle(
            cx,
            cy,
            radius,
            stroke,
        )
    }

    private fun drawMetadata(
        canvas: Canvas,
        w: Float,
        h: Float,
        input: ThemeInput,
    ) {
        val title =
            input.title
                .substringBeforeLast(
                    '.',
                    input.title,
                )
                .take(34)

        if (title.isBlank()) return

        val minSide = min(w, h)

        fill.textAlign = Paint.Align.CENTER
        fill.color = Color.WHITE
        fill.isFakeBoldText = true
        fill.textSize = minSide * 0.045f
        canvas.drawText(
            title,
            w * 0.5f,
            h * 0.64f,
            fill,
        )

        fill.isFakeBoldText = false
        fill.textSize = minSide * 0.026f
        fill.color = Color.rgb(171, 189, 202)
        canvas.drawText(
            input.artist.ifBlank { "FARIC" },
            w * 0.5f,
            h * 0.675f,
            fill,
        )

        if (input.durationMs > 0L) {
            val p =
                (
                    input.positionMs.toDouble() /
                        input.durationMs.toDouble()
                    )
                    .coerceIn(0.0, 1.0)
                    .toFloat()

            val left = w * 0.22f
            val right = w * 0.78f
            val y = h * 0.705f

            stroke.strokeWidth = 3f
            stroke.alpha = 90
            stroke.color = Color.WHITE
            canvas.drawLine(left, y, right, y, stroke)

            stroke.alpha = 230
            stroke.color = Color.rgb(54, 222, 255)
            canvas.drawLine(
                left,
                y,
                left + (right - left) * p,
                y,
                stroke,
            )
        }
    }

    private fun withAlpha(
        color: Int,
        alpha: Float,
    ): Int =
        Color.argb(
            (
                alpha
                    .coerceIn(0f, 1f) *
                    255f
                )
                .toInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun darken(
        color: Int,
        amount: Float,
    ): Int =
        Color.rgb(
            (
                Color.red(color) *
                    amount
                )
                .toInt()
                .coerceIn(0, 255),
            (
                Color.green(color) *
                    amount
                )
                .toInt()
                .coerceIn(0, 255),
            (
                Color.blue(color) *
                    amount
                )
                .toInt()
                .coerceIn(0, 255),
        )

    private fun hash01(
        seed: Int,
    ): Float {
        val x =
            sinf(seed * 12.9898f) *
                43758.5453f

        return abs(
            x -
                floor(x.toDouble())
                    .toFloat(),
        )
    }

    private fun fract(
        value: Float,
    ): Float =
        value -
            floor(value.toDouble())
                .toFloat()

    private fun sinf(
        value: Float,
    ): Float =
        kotlin.math.sin(
            value.toDouble(),
        ).toFloat()

    private fun cosf(
        value: Float,
    ): Float =
        kotlin.math.cos(
            value.toDouble(),
        ).toFloat()
}
