package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardBitmapGeometryTest {
    @Test fun squareSourcesRemainUnchanged() {
        assertEquals(
            BoardBitmapGeometry.Extent(155f, 155f),
            BoardBitmapGeometry.fitCanvas(1024, 1024, 155f),
        )
    }

    @Test fun wideImagePreservesItsOriginalRatio() {
        val extent = BoardBitmapGeometry.fitCanvas(1200, 400, 155f)
        assertEquals(155f, extent.width, 0.001f)
        assertEquals(155f / 3f, extent.height, 0.001f)
    }

    @Test fun tallImagePreservesItsOriginalRatio() {
        val extent = BoardBitmapGeometry.fitCanvas(400, 1200, 155f)
        assertEquals(155f / 3f, extent.width, 0.001f)
        assertEquals(155f, extent.height, 0.001f)
    }

    @Test fun changingSizeIsUniformNotHorizontalOrVerticalStretch() {
        for (size in listOf(100f, 155f, 200f)) {
            val frame = BoardBitmapGeometry.fitCanvas(1536, 1024, size)
            assertEquals(1.5f, frame.width / frame.height, 0.00001f)
            assertEquals(size, frame.width, 0.001f)
        }
    }

    @Test fun originalTransparentCanvasIsNotTrimmedOrReframed() {
        // Full bitmap dimensions, NOT opaque alpha bounds, determine the
        // placement. A transparent 1280x720 canvas is still 16:9 even if
        // the artwork inside the canvas occupies a much smaller area.
        val fullCanvas = BoardBitmapGeometry.fitCanvas(1280, 720, 200f)
        assertEquals(200f, fullCanvas.width, 0.001f)
        assertEquals(112.5f, fullCanvas.height, 0.001f)
    }

    @Test fun invalidDimensionsOrExtentAreNotRendered() {
        assertEquals(
            BoardBitmapGeometry.Extent(0f, 0f),
            BoardBitmapGeometry.fitCanvas(0, 720, 200f),
        )
        assertEquals(
            BoardBitmapGeometry.Extent(0f, 0f),
            BoardBitmapGeometry.fitCanvas(300, 700, Float.NaN),
        )
        assertTrue(BoardBitmapGeometry.fitCanvas(400, 200, 1f).width > 0f)
    }
}
