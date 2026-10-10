package com.saney.musicvisualizer.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GfCompositionTest {
    @Test
    fun fourPartsCanComeFromFourDifferentSourcePacks() {
        val mixed = GfComposition(
            compositionId = "my-scene",
            title = "My Hero",
            instances = listOf(
                GfLayerInstance("frame", GfAssetRef("CYBER_SHARK", GfArtType.FRAME), zOrder = 1),
                GfLayerInstance("fx", GfAssetRef("HERO_INFERNO_PHOENIX", GfArtType.FX), zOrder = 0),
                GfLayerInstance("hero", GfAssetRef("HERO_THUNDER_WOLF", GfArtType.CREATURE), zOrder = 3),
                GfLayerInstance("logo", GfAssetRef("HERO_PLASMA_COBRA", GfArtType.WORDMARK), zOrder = 2),
            ),
        )
        assertEquals(
            listOf("fx", "frame", "logo", "hero"),
            mixed.orderedInstances().map { it.instanceId },
        )
        assertEquals("HERO_PLASMA_COBRA:wordmark", mixed.instances[3].asset.assetId)
        assertTrue(mixed.instances.map { it.asset.sourcePackId }.distinct().size == 4)
    }

    @Test
    fun oneAssetMayAppearTwiceWithoutMutatingItsSource() {
        val ref = GfAssetRef("CYBER_PANTHER", GfArtType.FX)
        val scene = GfComposition(
            "double",
            "Two sparks",
            listOf(
                GfLayerInstance("left", ref, x = -0.3f),
                GfLayerInstance("right", ref, x = 0.3f, opacity = 0.5f),
            ),
        )
        assertEquals(ref, scene.instances[0].asset)
        assertEquals(ref, scene.instances[1].asset)
        assertNotEquals(scene.instances[0].x, scene.instances[1].x)
    }

    @Test
    fun fifthCreatureHalfIsOptionalAndDoesNotReplaceFullCreature() {
        val standard = GfComposition.fromSourcePack("a", "Standard", "HERO_NEON_GRIFFIN")
        assertEquals(4, standard.instances.size)
        assertFalse(standard.instances.any { it.asset.type == GfArtType.CREATURE_HALF })
        val withOptional = standard.copy(
            instances = standard.instances + GfLayerInstance(
                "optional-half",
                GfAssetRef("HERO_NEON_GRIFFIN", GfArtType.CREATURE_HALF),
            ),
        )
        assertEquals(5, withOptional.instances.size)
        assertTrue(withOptional.instances.any { it.asset.type == GfArtType.CREATURE })
    }

    @Test
    fun transformsAreSafeAndSourceIdsAreNotRewritten() {
        val instance = GfLayerInstance(
            "hero",
            GfAssetRef("HERO_VOID_DRAGON", GfArtType.CREATURE),
            x = Float.NaN,
            y = Float.POSITIVE_INFINITY,
            scale = -7f,
            rotationDegrees = 9999f,
            opacity = 9f,
        ).sanitized()
        assertEquals(0f, instance.x, 0f)
        assertEquals(0f, instance.y, 0f)
        assertEquals(0.1f, instance.scale, 0f)
        assertEquals(360f, instance.rotationDegrees, 0f)
        assertEquals(1f, instance.opacity, 0f)
        assertEquals("HERO_VOID_DRAGON", instance.asset.sourcePackId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateInstanceIdsAreRejected() {
        val ref = GfAssetRef("CYBER_SHARK", GfArtType.FRAME)
        GfComposition(
            "bad", "Bad", listOf(
                GfLayerInstance("duplicated", ref),
                GfLayerInstance("duplicated", ref),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeAssetPackIdsAreRejected() {
        GfAssetRef("../../evil", GfArtType.CREATURE)
    }
}
