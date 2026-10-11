package com.saney.musicvisualizer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroGalleryPolicyTest {
    @Test fun hideOnlyScorpionBDisplayEntry() {
        assertTrue(HeroGalleryPolicy.visibleExternalHero("09 cyber scorpion A"))
        assertFalse(HeroGalleryPolicy.visibleExternalHero("10 cyber scorpion B"))
        assertFalse(HeroGalleryPolicy.visibleExternalHero("CYBER_SCORPION_B"))
        assertTrue(HeroGalleryPolicy.visibleExternalHero("Cyber Panther"))
        assertTrue(HeroGalleryPolicy.visibleExternalHero("neon griffin"))
        assertTrue(HeroGalleryPolicy.visibleExternalHero("cyber scorpion battle"))
    }
}
