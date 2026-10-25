package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class NeonHeroTest {
    @Test fun settingsSurviveSavingAndBadInput() {
        val s = NeonHeroSettings(shape = 1, rubber = 30, rainOn = false, photoUri = "content://x/a;b", rippleSize = 90, fxGlitch = false, heroOn = false, heroAlpha = 40, lrOn = false)
        assertEquals(s, NeonHeroSettings.decode(s.encode()))
        assertEquals(NeonHeroSettings(), NeonHeroSettings.decode(null))
        assertEquals(NeonHeroSettings(), NeonHeroSettings.decode("garbage"))
        // out of range → clamped; unknown keys ignored
        val d = NeonHeroSettings.decode("shape=9;rubber=500;dent=-3;newThing=1;rainOn=false")
        assertEquals(2, d.shape)
        assertEquals(100, d.rubber)
        assertEquals(0, d.dent)
        assertFalse(d.rainOn)
    }

    @Test fun shapes() {
        for (i in 0 until 36) assertEquals(1f, NeonHeroMath.baseRadius(0, (i * PI / 18).toFloat()), 1e-5f)
        val top = (-PI / 2).toFloat()
        val ear = top + 0.62f
        assertTrue("ears stick out", NeonHeroMath.baseRadius(1, ear) > 1.25f)
        assertTrue("chin narrower", NeonHeroMath.baseRadius(1, (PI / 2).toFloat()) < 0.9f)
        assertEquals("hexagon corner", 1f, NeonHeroMath.baseRadius(2, 0f), 1e-4f)
        assertEquals("hexagon side middle", 0.866f, NeonHeroMath.baseRadius(2, (PI / 6).toFloat()), 1e-3f)
        assertTrue(NeonHeroMath.baseRadius(2, (PI / 6).toFloat()) < NeonHeroMath.baseRadius(2, 0f))
    }

    @Test fun beatComesFromTheBassSide() {
        assertEquals((PI / 2).toFloat(), NeonHeroMath.hitAngle(0f), 1e-5f)
        assertTrue(NeonHeroMath.hitAngle(-0.6f) > (PI / 2).toFloat())
        assertTrue(NeonHeroMath.hitAngle(0.6f) < (PI / 2).toFloat())
        assertEquals(-1, NeonHeroMath.pushSide(-0.5f))
        assertEquals(1, NeonHeroMath.pushSide(0.5f))
        assertEquals(0, NeonHeroMath.pushSide(0.05f))
        assertTrue(NeonHeroMath.isBeat(0.1f, 0.6f, 500))
        assertFalse("a loud part is not a new beat every frame", NeonHeroMath.isBeat(0.55f, 0.6f, 500))
        assertFalse(NeonHeroMath.isBeat(0.1f, 0.6f, 60))
    }

    @Test fun rubberDentsWhereHitThenSettles() {
        val ring = RubberRing(96)
        val down = (PI / 2).toFloat()
        ring.hit(down, 0.2f)
        repeat(3) { ring.step(0.55f) }
        assertTrue("dented at the hit", ring.at(down) < -0.01f)
        assertTrue("the far side bulges", ring.at((-PI / 2).toFloat()) > 0f)
        var overshoot = false
        repeat(80) {
            ring.step(0.55f)
            if (ring.at(down) > 0.002f) overshoot = true
        }
        assertTrue("rubber: springs past its place", overshoot)
        repeat(600) { ring.step(0.55f) }
        assertTrue("and settles", ring.energy() < 0.01f)
        for (i in 0 until ring.n) assertTrue(abs(ring.offset[i]) <= 0.45f)
    }

    @Test fun titleOneWordPerLineOrBelow() {
        assertEquals(listOf("MIDNIGHT", "PULSE"), NeonHeroMath.titleLines("MIDNIGHT PULSE"))
        assertEquals(listOf("TWO", "VOICES,", "ONE", "ROAD."), NeonHeroMath.titleLines("TWO VOICES, ONE ROAD."))
        assertNull("too many words — below the hero", NeonHeroMath.titleLines("PREVIEW — MIDNIGHT PULSE — White Side"))
        assertNull("a too long word — below", NeonHeroMath.titleLines("SUPERCALIFRAGILISTIC"))
        assertNull(NeonHeroMath.titleLines("   "))
    }

    @Test fun bandsAndMeters() {
        val sp = FloatArray(32) { if (it < 4) 1f else 0.1f }
        val b = NeonHeroMath.bands(sp, 13, 0f, 0f, 0f)
        assertEquals(13, b.size)
        assertEquals(1f, b[0], 1e-5f)
        assertEquals(0.1f, b[12], 1e-5f)
        val f = NeonHeroMath.bands(FloatArray(0), 13, 1f, 0f, 0f)
        assertTrue(f[0] > f[12])
        assertEquals(13, NeonHeroMath.BAND_ORDER.toSet().size)
        val (l, r) = NeonHeroMath.meters(0.8f, -0.5f)
        assertTrue("bass on the left — L higher", l > r)
        val (l2, r2) = NeonHeroMath.meters(0.8f, 0f)
        assertEquals(l2, r2, 1e-6f)
    }

    @Test fun photoJerksWithTheBeat() {
        val (dx, dy) = NeonHeroMath.photoKick(1f, -0.6f, 0.9f, 0.2f, 1f)
        assertTrue("bass on the left — jerk left", dx < 0f)
        assertTrue("bass — down", dy > 0f)
        val (_, upY) = NeonHeroMath.photoKick(1f, 0f, 0.1f, 0.8f, 1f)
        assertTrue("highs — up", upY < 0f)
        val (sx, sy) = NeonHeroMath.photoKick(1f, 0.6f, 0.9f, 0.2f, 0f)
        assertEquals("move 0 — still", 0f, sx + sy, 1e-6f)
        val d = NeonHeroSettings.decode(NeonHeroSettings(photoMove = 70, shakePower = 130).encode())
        assertEquals(70, d.photoMove)
        assertEquals(130, d.shakePower)
    }

    @Test fun smallSettingsGiveRareDropsAndZeroIsOff() {
        assertEquals(0, NeonHeroMath.countFor(0f, 0f))
        assertEquals(0, NeonHeroMath.countFor(0.2f, 0.5f)) // 20 % chance — this time not
        assertEquals(1, NeonHeroMath.countFor(0.2f, 0.1f))
        assertEquals(2, NeonHeroMath.countFor(2.3f, 0.9f))
        assertEquals(3, NeonHeroMath.countFor(2.3f, 0.2f))
        val z = NeonHeroSettings.decode("rippleCount=0;rainCount=0;blobCount=0;aura=0;pendulum=0;heroAlpha=0;jumpPower=0")
        assertEquals(0, z.rippleCount)
        assertEquals(0, z.rainCount)
        assertEquals(0, z.blobCount)
        assertEquals(0, z.aura)
        assertEquals(0, z.pendulum)
        assertEquals(0, z.heroAlpha)
        assertEquals(0, z.jumpPower)
    }
}
