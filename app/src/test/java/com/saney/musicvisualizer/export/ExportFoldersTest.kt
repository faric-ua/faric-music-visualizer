package com.saney.musicvisualizer.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportFoldersTest {
    @Test
    fun newFoldersArePulseDeck() {
        assertEquals("Movies/PulseDeck", ExportFolders.VIDEO)
        assertEquals("Pictures/PulseDeck", ExportFolders.PICTURES)
        assertEquals("Movies/PulseDeck/Album/", BatchPlan.relativePath("Album"))
    }

    @Test
    fun legacyPathMapsBatchFolderToOldFaricFolder() {
        assertEquals("Movies/FARIC/Album/", ExportFolders.legacyVideoPath("Movies/PulseDeck/Album/"))
        assertEquals("Movies/FARIC", ExportFolders.legacyVideoPath("Movies/PulseDeck"))
    }

    @Test
    fun legacyPathIgnoresOtherFolders() {
        assertNull(ExportFolders.legacyVideoPath("Movies/PulseDeckX/"))
        assertNull(ExportFolders.legacyVideoPath("Movies/FARIC/Album/"))
    }
}
