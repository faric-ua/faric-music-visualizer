package com.saney.musicvisualizer.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class LayersHelpTest {
    @Test fun everyHeaderButtonIsExplained() {
        listOf("⇩", "◎", "?", "✕").forEach { button ->
            assertTrue(button, LayersHelp.TEXT.lines().any { it.trimStart().startsWith(button) })
        }
    }

    @Test fun everyLayerIsExplained() {
        listOf("L0", "L2", "L3", "L4", "L5", "L6", "L7", "L8", "▸", "⚙", "40 %").forEach { word ->
            assertTrue(word, LayersHelp.TEXT.contains(word))
        }
    }
}
