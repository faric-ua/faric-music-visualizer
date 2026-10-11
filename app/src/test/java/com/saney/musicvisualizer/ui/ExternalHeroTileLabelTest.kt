package com.saney.musicvisualizer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalHeroTileLabelTest {
    @Test fun sequentialNumberAlwaysStartsAtOne() {
        assertEquals("01", ExternalHeroTileLabel.number(0))
        assertEquals("02", ExternalHeroTileLabel.number(1))
        assertEquals("10", ExternalHeroTileLabel.number(9))
        assertEquals("100", ExternalHeroTileLabel.number(99))
    }

    @Test fun sourceNumberIsNotDuplicatedInCaption() {
        assertEquals("galaxy dragon", ExternalHeroTileLabel.caption("01 galaxy dragon"))
        assertEquals("neon griffin", ExternalHeroTileLabel.caption("02_neon griffin"))
        assertEquals("plasma cobra", ExternalHeroTileLabel.caption(" 04 - plasma cobra "))
        assertEquals("Cyber Panther", ExternalHeroTileLabel.caption("Cyber Panther"))
        assertEquals("PNG-герой", ExternalHeroTileLabel.caption("01"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeDisplayIndexIsNotAllowed() {
        ExternalHeroTileLabel.number(-1)
    }
}
