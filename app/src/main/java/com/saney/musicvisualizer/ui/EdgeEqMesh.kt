package com.saney.musicvisualizer.ui

import kotlin.math.sqrt

/**
 * V200 (faric 29.10 17:28 / 19:30: «пришвидшимо окантовку — той самий вигляд, тест»). In the video the
 * edge equalizer is ~600 strokes drawn twice (glow + line); a software canvas pays ~4 µs per stroke
 * whatever its size (measured 29.10 in Chromium's Skia: 5,6 ms per frame). Here every stroke becomes a
 * few triangles of ONE mesh that is drawn with a single `Canvas.drawVertices` per layer: a solid core,
 * a thin step to the glow and the glow fading to nothing at its edge (the fade gives smooth edges without
 * anti-aliasing). Pure — tested on JVM.
 */
class EdgeEqMesh {
    var verts = FloatArray(0)
        private set
    var colors = IntArray(0)
        private set
    var indices = ShortArray(0)
        private set
    var vertexCount = 0
        private set
    var indexCount = 0
        private set

    fun begin() {
        vertexCount = 0
        indexCount = 0
    }

    /**
     * One stroke from (x0, y0) to (x1, y1) — a dot when both ends meet. [coreHalf] / [glowHalf] are half
     * the line / glow widths, [feather] the soft step (canvas units); [rgb] the colour, alphas 0–255.
     */
    fun addStroke(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        coreHalf: Float,
        glowHalf: Float,
        feather: Float,
        rgb: Int,
        lineAlpha: Int,
        glowAlpha: Int,
    ) {
        if (vertexCount + VERTS_PER_STROKE > MAX_VERTS) return
        var ux = x1 - x0
        var uy = y1 - y0
        val len = sqrt(ux * ux + uy * uy)
        if (len < 1e-4f) {
            ux = 1f
            uy = 0f
        } else {
            ux /= len
            uy /= len
        }
        val nx = -uy
        val ny = ux
        val base = vertexCount
        val glowOuter = maxOf(glowHalf, coreHalf + feather * 2f)
        // Three nested rectangles round the stroke; round caps reach out as far as the stroke is wide.
        level(x0, y0, x1, y1, ux, uy, nx, ny, coreHalf, coreHalf, colour(rgb, lineAlpha))
        level(x0, y0, x1, y1, ux, uy, nx, ny, coreHalf + feather, coreHalf + feather, colour(rgb, glowAlpha))
        level(x0, y0, x1, y1, ux, uy, nx, ny, glowOuter, glowOuter, colour(rgb, 0))
        ensureIndices(indexCount + INDICES_PER_STROKE)
        // core
        tri(base, base + 1, base + 2)
        tri(base, base + 2, base + 3)
        ring(base, base + 4)
        ring(base + 4, base + 8)
    }

    private fun level(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        ux: Float,
        uy: Float,
        nx: Float,
        ny: Float,
        half: Float,
        reach: Float,
        color: Int,
    ) {
        val ax = x0 - ux * reach
        val ay = y0 - uy * reach
        val bx = x1 + ux * reach
        val by = y1 + uy * reach
        vertex(ax - nx * half, ay - ny * half, color)
        vertex(ax + nx * half, ay + ny * half, color)
        vertex(bx + nx * half, by + ny * half, color)
        vertex(bx - nx * half, by - ny * half, color)
    }

    /** The band between two nested rectangles [inner] and [outer] (4 corners each). */
    private fun ring(
        inner: Int,
        outer: Int,
    ) {
        for (k in 0 until 4) {
            val k1 = (k + 1) % 4
            tri(inner + k, outer + k, outer + k1)
            tri(inner + k, outer + k1, inner + k1)
        }
    }

    private fun vertex(
        x: Float,
        y: Float,
        color: Int,
    ) {
        if (verts.size < (vertexCount + 1) * 2) verts = verts.copyOf(maxOf(64, verts.size * 2))
        if (colors.size < vertexCount + 1) colors = colors.copyOf(maxOf(32, colors.size * 2))
        verts[vertexCount * 2] = x
        verts[vertexCount * 2 + 1] = y
        colors[vertexCount] = color
        vertexCount++
    }

    private fun ensureIndices(n: Int) {
        if (indices.size < n) indices = indices.copyOf(maxOf(n, indices.size * 2))
    }

    private fun tri(
        a: Int,
        b: Int,
        c: Int,
    ) {
        indices[indexCount++] = a.toShort()
        indices[indexCount++] = b.toShort()
        indices[indexCount++] = c.toShort()
    }

    companion object {
        const val VERTS_PER_STROKE = 12
        const val INDICES_PER_STROKE = 18 * 3

        /** Indices are 16-bit: at most 65 535 vertices in one mesh. */
        const val MAX_VERTS = 65_535

        fun colour(
            rgb: Int,
            alpha: Int,
        ): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)
    }
}
