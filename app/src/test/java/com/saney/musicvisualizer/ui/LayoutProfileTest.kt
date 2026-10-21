package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutProfileTest {
    @Test fun codecRoundTripAndSkipsIdentity() {
        val map =
            mapOf(
                "cover" to LayoutAdjust(0.01f, -0.02f, 1.15f),
                "back" to LayoutAdjust(),
            )
        val decoded = LayoutProfileCodec.decode(LayoutProfileCodec.encode(map))
        assertEquals(setOf("cover"), decoded.keys)
        assertEquals(1.15f, decoded.getValue("cover").scale, 1e-6f)
        assertEquals(-0.02f, decoded.getValue("cover").dy, 1e-6f)
    }

    @Test fun decodeIgnoresBrokenPartsAndClamps() {
        val decoded = LayoutProfileCodec.decode("cover:0.1:0.2:9;bad;x:NaN:0:1;:1:1:1")
        assertEquals(setOf("cover"), decoded.keys)
        assertEquals(LayoutAdjust.MAX_SCALE, decoded.getValue("cover").scale, 1e-6f)
    }

    @Test fun resizeInWholePercentsWithinLimits() {
        var a = LayoutAdjust()
        a = a.resized(5).resized(1)
        assertEquals(1.06f, a.scale, 1e-6f)
        repeat(200) { a = a.resized(-5) }
        assertEquals(LayoutAdjust.MIN_SCALE, a.scale, 1e-6f)
    }

    @Test fun groupsCoverEveryPanelPart() {
        assertEquals("transport", LayoutGroups.groupOf("play_pause"))
        assertEquals("quick", LayoutGroups.groupOf("export"))
        assertEquals("cover", LayoutGroups.groupOf("cover_box"))
        assertNull(LayoutGroups.groupOf("hero_core"))
        val ids = LayoutGroups.ALL.flatMap { it.members }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun choiceByOrientationAndVideoFallsBackToPortrait() {
        val portrait = mapOf("cover" to LayoutAdjust(scale = 1.2f))
        val saved = mapOf(LayoutProfileId.PORTRAIT to portrait)
        assertEquals(portrait, LayoutProfileChoice.forView(1080, 2340, null, saved))
        assertTrue(LayoutProfileChoice.forView(2340, 1080, null, saved).isEmpty())
        assertEquals(portrait, LayoutProfileChoice.forView(1080, 2340, LayoutProfileId.VIDEO_9X16, saved))
        val video = mapOf("back" to LayoutAdjust(dy = 0.05f))
        assertEquals(
            video,
            LayoutProfileChoice.forView(1080, 2340, LayoutProfileId.VIDEO_9X16, saved + (LayoutProfileId.VIDEO_9X16 to video)),
        )
    }

    @Test fun adjustedBoxGrowsFromCentreThenMoves() {
        val base = LayoutBox(100f, 100f, 200f, 200f)
        val box = base.adjusted(LayoutAdjust(dx = 0.1f, dy = 0f, scale = 2f), 1000f, 2000f)
        assertEquals(LayoutBox(150f, 50f, 350f, 250f), box)
    }

    @Test fun snapsCentreToNearestGuideWithinThreshold() {
        val base = LayoutBox(480f, 100f, 580f, 200f) // centre 530
        val near = LayoutSnap.snap(LayoutAdjust(), base, 1080f, 2340f, listOf(540f), listOf(1170f), 12f)
        assertEquals(540f, near.guideX)
        assertNull(near.guideY)
        assertEquals(540f, base.adjusted(near.adjust, 1080f, 2340f).centerX, 1e-3f)
        val far = LayoutSnap.snap(LayoutAdjust(), base, 1080f, 2340f, listOf(560f), emptyList(), 12f)
        assertNull(far.guideX)
        assertEquals(LayoutAdjust(), far.adjust)
    }
}
