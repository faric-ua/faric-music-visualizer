package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeEqMeshTest {
    @Test fun oneStrokeIsCoreStepAndFadingGlow() {
        val m = EdgeEqMesh()
        m.begin()
        m.addStroke(10f, 10f, 10f, 30f, coreHalf = 1f, glowHalf = 3f, feather = 0.5f, rgb = 0x12AB34, lineAlpha = 255, glowAlpha = 80)
        assertEquals(12, m.vertexCount)
        assertEquals(54, m.indexCount)
        // core opaque, step at the glow's strength, outer edge fully transparent
        for (i in 0 until 4) assertEquals(0xFF12AB34.toInt(), m.colors[i])
        for (i in 4 until 8) assertEquals(80, m.colors[i] ushr 24)
        for (i in 8 until 12) assertEquals(0, m.colors[i] ushr 24)
        // the outer rectangle is the glow's width (half 3 → x from 7 to 13) and reaches past the ends
        val xs = (8 until 12).map { m.verts[it * 2] }
        val ys = (8 until 12).map { m.verts[it * 2 + 1] }
        assertEquals(7f, xs.minOrNull()!!, 1e-4f)
        assertEquals(13f, xs.maxOrNull()!!, 1e-4f)
        assertEquals(7f, ys.minOrNull()!!, 1e-4f)
        assertEquals(33f, ys.maxOrNull()!!, 1e-4f)
        // every index points at a real vertex
        for (i in 0 until m.indexCount) assertTrue(m.indices[i] in 0 until 12)
    }

    @Test fun aDotAndManyStrokes() {
        val m = EdgeEqMesh()
        m.begin()
        m.addStroke(5f, 5f, 5f, 5f, 1f, 2.4f, 0.5f, 0xFFFFFF, 200, 60)
        assertEquals(12, m.vertexCount)
        m.begin()
        repeat(1300) { m.addStroke(0f, 0f, it.toFloat(), 1f, 1f, 3f, 0.5f, 0x00FF00, 255, 80) }
        assertEquals(1300 * 12, m.vertexCount)
        assertEquals(1300 * 54, m.indexCount)
        // 16-bit indices: the last vertex number survives as an unsigned short
        assertEquals(1300 * 12 - 1, m.indices.copyOf(m.indexCount).maxOf { it.toInt() and 0xFFFF })
    }

    @Test fun glowNeverInsideTheCore() {
        val m = EdgeEqMesh()
        m.begin()
        // a glow narrower than core + step is widened, so the fade never folds back over the line
        m.addStroke(0f, 0f, 10f, 0f, coreHalf = 2f, glowHalf = 1f, feather = 0.5f, rgb = 0, lineAlpha = 255, glowAlpha = 50)
        val ys = (8 until 12).map { m.verts[it * 2 + 1] }
        assertEquals(3f, ys.maxOrNull()!!, 1e-4f)
    }
}
