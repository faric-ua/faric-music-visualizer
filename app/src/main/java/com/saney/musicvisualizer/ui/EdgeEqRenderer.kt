package com.saney.musicvisualizer.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient

/**
 * V141: draws [EdgeEqSettings] round a tile — bars (rhythm), dots (highs), wave (melody). Cheap enough
 * for every MP4 frame: one SweepGradient per layer (colours go round the tile and turn with
 * [EdgeEqSettings.scroll]), the bars in one drawLines, the dots in one drawPoints, the wave one Path;
 * the glow is the same shapes once more, wider and faint — no blur, no layers (lessons V127 / V132).
 * One renderer per view: it remembers the levels between frames (fast up, slower down).
 */
class EdgeEqRenderer {
    var settings: EdgeEqSettings = EdgeEqSettings.DEFAULT

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val wavePath = Path()
    private val shaderMatrix = android.graphics.Matrix()
    private val shaders = HashMap<String, SweepGradient>()
    private val rings = HashMap<String, FloatArray>()
    private var barLevels = FloatArray(0)
    private var dotLevels = FloatArray(0)
    private var waveLevels = FloatArray(0)
    private var lines = FloatArray(0)
    private var points = FloatArray(0)

    fun draw(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        spectrum: FloatArray,
        clockMs: Long,
        alpha: Int,
        unit: Float,
    ) {
        val s = settings
        if (!s.on || spectrum.isEmpty() || rect.width() <= 0f) return
        val turn = EdgeEqMath.turn(clockMs, s.scroll)
        val cx = rect.centerX()
        val cy = rect.centerY()
        shaderMatrix.setRotate(turn * 360f, cx, cy)
        val gap = unit * 0.004f
        var barTips: FloatArray? = null

        if (s.bars.enabled) {
            val n = EdgeEqMath.sampleCount(s.bars.density)
            val ring = ring(rect, radius, n)
            if (barLevels.size != n) barLevels = FloatArray(n)
            if (lines.size != n * 4) lines = FloatArray(n * 4)
            barTips = FloatArray(n)
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size)] * s.bars.reaction / 100f
                barLevels[i] = EdgeEqMath.smooth(barLevels[i], raw.coerceIn(0f, 1.4f))
                val len = unit * (0.006f + 0.05f * barLevels[i])
                barTips[i] = gap + len
                val x = ring[i * 4]
                val y = ring[i * 4 + 1]
                val nx = ring[i * 4 + 2]
                val ny = ring[i * 4 + 3]
                lines[i * 4] = x + nx * gap
                lines[i * 4 + 1] = y + ny * gap
                lines[i * 4 + 2] = x + nx * (gap + len)
                lines[i * 4 + 3] = y + ny * (gap + len)
            }
            val width = unit * (0.0012f + 0.006f * s.bars.thickness / 100f)
            paint(glowPaint, s.bars.palette, cx, cy, width * 3f, alpha * 0.32f)
            canvas.drawLines(lines, 0, n * 4, glowPaint)
            paint(linePaint, s.bars.palette, cx, cy, width, alpha.toFloat())
            canvas.drawLines(lines, 0, n * 4, linePaint)
        }

        if (s.dots.enabled) {
            val n = EdgeEqMath.sampleCount(s.dots.density) / 2
            val ring = ring(rect, radius, n)
            if (dotLevels.size != n) dotLevels = FloatArray(n)
            val maxDots = 6
            if (points.size != n * maxDots * 2) points = FloatArray(n * maxDots * 2)
            var count = 0
            val step = unit * 0.011f
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size, 0.55f, 1f)] * s.dots.reaction / 100f
                dotLevels[i] = EdgeEqMath.smooth(dotLevels[i], raw.coerceIn(0f, 1.4f), 0.22f)
                val k = (dotLevels[i] * maxDots).toInt().coerceIn(0, maxDots)
                val base =
                    barTips?.let { tips -> tips[(i.toLong() * tips.size / n).toInt().coerceIn(0, tips.lastIndex)] }
                        ?: gap
                val x = ring[i * 4]
                val y = ring[i * 4 + 1]
                val nx = ring[i * 4 + 2]
                val ny = ring[i * 4 + 3]
                for (j in 0 until k) {
                    val d = base + step * (j + 1)
                    points[count++] = x + nx * d
                    points[count++] = y + ny * d
                }
            }
            if (count > 0) {
                val size = unit * (0.0025f + 0.008f * s.dots.thickness / 100f)
                paint(glowPaint, s.dots.palette, cx, cy, size * 2.4f, alpha * 0.3f)
                canvas.drawPoints(points, 0, count, glowPaint)
                paint(linePaint, s.dots.palette, cx, cy, size, alpha.toFloat())
                canvas.drawPoints(points, 0, count, linePaint)
            }
        }

        if (s.wave.enabled) {
            val n = EdgeEqMath.sampleCount(s.wave.density)
            val ring = ring(rect, radius, n)
            if (waveLevels.size != n) waveLevels = FloatArray(n)
            wavePath.rewind()
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size, 0.15f, 0.6f)] * s.wave.reaction / 100f
                waveLevels[i] = EdgeEqMath.smooth(waveLevels[i], raw.coerceIn(0f, 1.4f), 0.08f)
                val d = gap + unit * 0.055f * waveLevels[i]
                val x = ring[i * 4] + ring[i * 4 + 2] * d
                val y = ring[i * 4 + 1] + ring[i * 4 + 3] * d
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            wavePath.close()
            val width = unit * (0.0015f + 0.006f * s.wave.thickness / 100f)
            paint(glowPaint, s.wave.palette, cx, cy, width * 4f, alpha * 0.35f)
            canvas.drawPath(wavePath, glowPaint)
            paint(linePaint, s.wave.palette, cx, cy, width, alpha.toFloat())
            canvas.drawPath(wavePath, linePaint)
        }
    }

    private fun paint(
        p: Paint,
        palette: Int,
        cx: Float,
        cy: Float,
        width: Float,
        alpha: Float,
    ) {
        val key = palette.toString() + "|" + cx.toInt() + "|" + cy.toInt()
        val shader =
            shaders.getOrPut(key) {
                if (shaders.size > 24) shaders.clear()
                SweepGradient(cx, cy, EdgePalettes.get(palette).colors, null)
            }
        shader.setLocalMatrix(shaderMatrix)
        p.shader = shader
        p.strokeWidth = width
        p.alpha = alpha.toInt().coerceIn(0, 255)
    }

    private fun ring(
        rect: RectF,
        radius: Float,
        n: Int,
    ): FloatArray {
        val key = "${rect.left.toInt()}|${rect.top.toInt()}|${rect.right.toInt()}|${rect.bottom.toInt()}|${radius.toInt()}|$n"
        return rings.getOrPut(key) {
            if (rings.size > 12) rings.clear()
            EdgeEqMath.ring(rect.left, rect.top, rect.right, rect.bottom, radius, n)
        }
    }
}
