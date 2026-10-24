package com.saney.musicvisualizer.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * V141: draws [EdgeEqSettings] round a tile — bars (rhythm), dots (highs), wave (melody). Colours go
 * round the tile and turn with [EdgeEqSettings.scroll] (V174: 48 solid colour steps, not a per-pixel
 * sweep gradient); strokes of one step are drawn together;
 * the glow is the same shapes once more, wider and faint — no blur, no layers (lessons V127 / V132).
 * One renderer per view: it remembers the levels between frames (fast up, slower down).
 */
class EdgeEqRenderer {
    var settings: EdgeEqSettings = EdgeEqSettings.DEFAULT

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val rings = HashMap<String, FloatArray>()
    private val ringAngles = HashMap<String, FloatArray>()
    private val paletteSteps = HashMap<Int, IntArray>()
    private var barLevels = FloatArray(0)
    private var dotLevels = FloatArray(0)
    private var waveLevels = FloatArray(0)
    private var lines = FloatArray(0)
    private var lineAngles = FloatArray(0)
    private var points = FloatArray(0)
    private var pointAngles = FloatArray(0)
    private var waveXy = FloatArray(0)

    // V174 (замір 23.10 21:08: «рамка: окантовка-EQ» 15,9 мс на кадр відео): a SweepGradient is worked out
    // for every pixel of every stroke. Now each stroke gets one solid colour from 48 steps round the
    // tile, strokes of one step are drawn together — the same colours, a fraction of the work.
    private val bucketCount = IntArray(EdgeEqMath.COLOR_BUCKETS)
    private val bucketStart = IntArray(EdgeEqMath.COLOR_BUCKETS)
    private val bucketCursor = IntArray(EdgeEqMath.COLOR_BUCKETS)
    private var bucketOf = IntArray(0)
    private var sorted = FloatArray(0)

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
        // V174 крок 4 → відкат (замір 25.10 01:17: окантовка 1 614 → 1 639 мс — «сяйво без згладжування» не дало
        // нічого): the glow is smooth again everywhere.
        val turn = EdgeEqMath.turn(clockMs, s.scroll)
        val gap = unit * 0.004f
        // V154: everything stays within REACH of the tile (the frame from «Розкладка»).
        val reach = unit * EdgeEqMath.REACH
        var barTips: FloatArray? = null

        if (s.bars.enabled) {
            val n = EdgeEqMath.sampleCount(s.bars.density)
            val ring = ring(rect, radius, n)
            val angles = angles(rect, radius, n, ring)
            if (barLevels.size != n) barLevels = FloatArray(n)
            if (lines.size < n * 4) lines = FloatArray(n * 4)
            if (lineAngles.size < n) lineAngles = FloatArray(n)
            barTips = FloatArray(n)
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size)] * s.bars.reaction / 100f
                barLevels[i] = EdgeEqMath.smooth(barLevels[i], raw.coerceIn(0f, 1f), 0.25f)
                // V181: from a short stub to the frame's limit, with the bottom equalizer's curve.
                val minLen = unit * 0.004f
                val len = minLen + (reach - gap - minLen).coerceAtLeast(0f) * EdgeEqMath.shape(barLevels[i])
                barTips[i] = gap + len
                val x = ring[i * 4]
                val y = ring[i * 4 + 1]
                val nx = ring[i * 4 + 2]
                val ny = ring[i * 4 + 3]
                lines[i * 4] = x + nx * gap
                lines[i * 4 + 1] = y + ny * gap
                lines[i * 4 + 2] = x + nx * (gap + len)
                lines[i * 4 + 3] = y + ny * (gap + len)
                lineAngles[i] = angles[i]
            }
            val width = unit * (0.0012f + 0.006f * s.bars.thickness / 100f)
            drawSteps(canvas, lines, lineAngles, n, 4, turn, s.bars.palette, width * 3f, alpha * 0.32f, width, alpha.toFloat())
        }

        if (s.dots.enabled) {
            val n = EdgeEqMath.sampleCount(s.dots.density) / 2
            val ring = ring(rect, radius, n)
            val angles = angles(rect, radius, n, ring)
            if (dotLevels.size != n) dotLevels = FloatArray(n)
            val maxDots = 6
            if (points.size < n * maxDots * 2) points = FloatArray(n * maxDots * 2)
            if (pointAngles.size < n * maxDots) pointAngles = FloatArray(n * maxDots)
            var count = 0
            val step = unit * 0.008f
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size, 0.55f, 1f)] * s.dots.reaction / 100f
                dotLevels[i] = EdgeEqMath.smooth(dotLevels[i], raw.coerceIn(0f, 1f), 0.25f)
                val k = (EdgeEqMath.shape(dotLevels[i]) * maxDots + 0.5f).toInt().coerceIn(0, maxDots)
                val base =
                    barTips?.let { tips -> tips[(i.toLong() * tips.size / n).toInt().coerceIn(0, tips.lastIndex)] }
                        ?: gap
                val x = ring[i * 4]
                val y = ring[i * 4 + 1]
                val nx = ring[i * 4 + 2]
                val ny = ring[i * 4 + 3]
                for (j in 0 until k) {
                    val d = base + step * (j + 1)
                    if (d > reach) break
                    points[count * 2] = x + nx * d
                    points[count * 2 + 1] = y + ny * d
                    pointAngles[count] = angles[i]
                    count++
                }
            }
            if (count > 0) {
                val size = unit * (0.0025f + 0.008f * s.dots.thickness / 100f)
                drawSteps(canvas, points, pointAngles, count, 2, turn, s.dots.palette, size * 2.4f, alpha * 0.3f, size, alpha.toFloat())
            }
        }

        if (s.wave.enabled) {
            val n = EdgeEqMath.sampleCount(s.wave.density)
            val ring = ring(rect, radius, n)
            val angles = angles(rect, radius, n, ring)
            if (waveLevels.size != n) waveLevels = FloatArray(n)
            if (waveXy.size < n * 2) waveXy = FloatArray(n * 2)
            if (lines.size < n * 4) lines = FloatArray(n * 4)
            if (lineAngles.size < n) lineAngles = FloatArray(n)
            for (i in 0 until n) {
                val u = i.toFloat() / n + turn
                val raw = spectrum[EdgeEqMath.band(u, spectrum.size, 0.15f, 0.6f)] * s.wave.reaction / 100f
                waveLevels[i] = EdgeEqMath.smooth(waveLevels[i], raw.coerceIn(0f, 1f), 0.12f)
                val d = gap + (reach - gap).coerceAtLeast(0f) * EdgeEqMath.shape(waveLevels[i])
                waveXy[i * 2] = ring[i * 4] + ring[i * 4 + 2] * d
                waveXy[i * 2 + 1] = ring[i * 4 + 1] + ring[i * 4 + 3] * d
            }
            // The closed wave as short segments (round ends join them), each with its own colour step.
            for (i in 0 until n) {
                val j = if (i + 1 == n) 0 else i + 1
                lines[i * 4] = waveXy[i * 2]
                lines[i * 4 + 1] = waveXy[i * 2 + 1]
                lines[i * 4 + 2] = waveXy[j * 2]
                lines[i * 4 + 3] = waveXy[j * 2 + 1]
                lineAngles[i] = angles[i]
            }
            val width = unit * (0.0015f + 0.006f * s.wave.thickness / 100f)
            drawSteps(canvas, lines, lineAngles, n, 4, turn, s.wave.palette, width * 4f, alpha * 0.35f, width, alpha.toFloat())
        }
    }

    /** Lines (stride 4) or points (stride 2), grouped by colour step: the faint wide glow first, then the line. */
    private fun drawSteps(
        canvas: Canvas,
        data: FloatArray,
        angles: FloatArray,
        count: Int,
        stride: Int,
        turn: Float,
        palette: Int,
        glowWidth: Float,
        glowAlpha: Float,
        width: Float,
        lineAlpha: Float,
    ) {
        if (count <= 0) return
        val steps = steps(palette)
        if (bucketOf.size < count) bucketOf = IntArray(count)
        if (sorted.size < count * stride) sorted = FloatArray(count * stride)
        bucketCount.fill(0)
        for (i in 0 until count) {
            val b = EdgeEqMath.bucket(angles[i], turn)
            bucketOf[i] = b
            bucketCount[b]++
        }
        var offset = 0
        for (b in 0 until EdgeEqMath.COLOR_BUCKETS) {
            bucketStart[b] = offset
            bucketCursor[b] = offset
            offset += bucketCount[b]
        }
        for (i in 0 until count) {
            val dest = bucketCursor[bucketOf[i]]++ * stride
            System.arraycopy(data, i * stride, sorted, dest, stride)
        }
        for (pass in 0..1) {
            val p = if (pass == 0) glowPaint else linePaint
            p.shader = null
            p.strokeWidth = if (pass == 0) glowWidth else width
            val a = (if (pass == 0) glowAlpha else lineAlpha).toInt().coerceIn(0, 255)
            for (b in 0 until EdgeEqMath.COLOR_BUCKETS) {
                val c = bucketCount[b]
                if (c == 0) continue
                p.color = steps[b]
                p.alpha = a
                if (stride == 4) {
                    canvas.drawLines(sorted, bucketStart[b] * 4, c * 4, p)
                } else {
                    canvas.drawPoints(sorted, bucketStart[b] * 2, c * 2, p)
                }
            }
        }
    }

    private fun steps(palette: Int): IntArray =
        paletteSteps.getOrPut(palette) {
            val colors = EdgePalettes.get(palette).colors
            IntArray(EdgeEqMath.COLOR_BUCKETS) { b -> EdgeEqMath.colorAt(colors, (b + 0.5f) / EdgeEqMath.COLOR_BUCKETS) }
        }

    private fun angles(
        rect: RectF,
        radius: Float,
        n: Int,
        ring: FloatArray,
    ): FloatArray {
        val key = "${rect.left.toInt()}|${rect.top.toInt()}|${rect.right.toInt()}|${rect.bottom.toInt()}|${radius.toInt()}|$n"
        return ringAngles.getOrPut(key) {
            if (ringAngles.size > 12) ringAngles.clear()
            val cx = rect.centerX()
            val cy = rect.centerY()
            FloatArray(n) { i -> EdgeEqMath.angle01(ring[i * 4], ring[i * 4 + 1], cx, cy) }
        }
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
