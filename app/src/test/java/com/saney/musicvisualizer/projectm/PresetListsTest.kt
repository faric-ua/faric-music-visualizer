package com.saney.musicvisualizer.projectm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetListsTest {
    @Test fun listsSurviveSavingWithAnyName() {
        val lists = listOf(PresetLists.Entry("l1", "Для альбому White Side"), PresetLists.Entry("l2", "Тест: 4K · 60"))
        assertEquals(lists, PresetLists.decode(PresetLists.encode(lists)))
        assertEquals(emptyList<PresetLists.Entry>(), PresetLists.decode(null))
    }

    @Test fun namesAreOneTidyLine() {
        assertEquals("Нічні два рядки", PresetLists.cleanName("  Нічні\n два   рядки "))
        assertEquals("Мій список", PresetLists.cleanName("   "))
        assertEquals(PresetLists.MAX_NAME, PresetLists.cleanName("x".repeat(100)).length)
    }

    @Test fun newIdNeverRepeats() {
        val first = PresetLists.newId(emptyList(), 1000L)
        val second = PresetLists.newId(listOf(PresetLists.Entry(first, "a")), 1000L)
        assertNotEquals(first, second)
    }

    @Test fun membershipKeysBelongToTheirList() {
        val key = PresetLists.memberKey("l1", "martin/X Portal.milk")
        assertTrue(PresetLists.isMemberKeyOf(key, "l1"))
        assertFalse(PresetLists.isMemberKeyOf(key, "l12"))
        assertFalse(PresetLists.isMemberKeyOf("fav:martin/X Portal.milk", "l1"))
    }
}
