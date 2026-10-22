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

    @Test fun codecKeepsAlphaAndHiddenAndReadsOldFormat() {
        val map = mapOf("cover" to LayoutAdjust(scale = 4.3f, alpha = 0.4f), "back" to LayoutAdjust(hidden = true))
        val decoded = LayoutProfileCodec.decode(LayoutProfileCodec.encode(map))
        assertEquals(0.4f, decoded.getValue("cover").alpha, 1e-6f)
        assertEquals(4.3f, decoded.getValue("cover").scale, 1e-6f)
        assertTrue(decoded.getValue("back").hidden)
        assertEquals(1f, LayoutProfileCodec.decode("cover:0:0:1.2").getValue("cover").alpha, 1e-6f)
    }

    @Test fun edgeSnapSticksLeftEdgeToScreenEdge() {
        val base = LayoutBox(15f, 100f, 115f, 200f)
        val r = LayoutSnapEdges.snap(LayoutAdjust(), base, 1080f, 2340f, emptyList(), SnapSet(center = false).withAllEdges(true), 20f)
        assertEquals(0f, r.guideX)
        assertEquals(0f, base.adjusted(r.adjust, 1080f, 2340f).left, 1e-3f)
        val off = LayoutSnapEdges.snap(LayoutAdjust(), base, 1080f, 2340f, emptyList(), SnapSet(center = false), 20f)
        assertEquals(LayoutAdjust(), off.adjust)
    }

    @Test fun bottomSnapUsesOtherPartsBottom() {
        val base = LayoutBox(100f, 100f, 200f, 195f)
        val other = LayoutBox(400f, 150f, 500f, 200f)
        val r = LayoutSnapEdges.snap(LayoutAdjust(), base, 1080f, 2340f, listOf(other), SnapSet(center = false, bottom = true), 10f)
        assertEquals(200f, r.guideY)
        assertNull(r.guideX)
    }

    @Test fun snapSetLabelAndCodec() {
        val set = SnapSet(center = true).withAllEdges(true)
        assertEquals("центр + 4 краї", set.label)
        assertEquals(set, SnapSet.decode(set.encode()))
        assertEquals("центр + лівий", SnapSet(center = true, left = true).label)
        assertTrue(SnapSet(center = false).isOff)
        assertEquals(SnapSet(), SnapSet.decode("bad"))
    }

    @Test fun snapshotNameShortStaysWhole() {
        assertEquals("Велика обкладинка", SnapshotName.header("  Велика   обкладинка "))
        assertNull(SnapshotName.description("Велика обкладинка"))
        assertEquals("Без назви", SnapshotName.clean(" \n "))
    }

    @Test fun snapshotNameLongGetsHeaderAndDescription() {
        val name = "Обкладинка по центру + меню знизу 50 % + керування 50 %"
        assertEquals("Обкладинка по центру …", SnapshotName.header(name))
        assertEquals(name, SnapshotName.description(name))
        val plain = "Дуже довга назва без жодних роздільників тільки слова й слова"
        val header = SnapshotName.header(plain)
        assertTrue(header.length <= SnapshotName.HEADER_MAX)
        assertTrue(header.endsWith("…"))
        assertEquals(SnapshotName.NAME_MAX, SnapshotName.clean("x".repeat(500)).length)
    }
}
