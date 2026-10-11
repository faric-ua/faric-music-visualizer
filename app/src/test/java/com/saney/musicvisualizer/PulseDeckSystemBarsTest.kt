package com.saney.musicvisualizer

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PulseDeckSystemBarsTest {
    private val context = mock(Context::class.java)
    private val preferences = mock(SharedPreferences::class.java)

    @Test
    fun defaultsToPersistentVisibleBarsForExistingInstallations() {
        `when`(
            context.getSharedPreferences(
                PulseDeckSystemBars.PREFS_NAME,
                Context.MODE_PRIVATE,
            ),
        ).thenReturn(preferences)
        `when`(
            preferences.getBoolean(PulseDeckSystemBars.KEY_ALWAYS_VISIBLE, true),
        ).thenReturn(true)

        assertTrue(PulseDeckSystemBars.alwaysVisible(context))
        verify(preferences).getBoolean(PulseDeckSystemBars.KEY_ALWAYS_VISIBLE, true)
    }

    @Test
    fun respectsUserImmersivePreference() {
        `when`(
            context.getSharedPreferences(
                PulseDeckSystemBars.PREFS_NAME,
                Context.MODE_PRIVATE,
            ),
        ).thenReturn(preferences)
        `when`(
            preferences.getBoolean(PulseDeckSystemBars.KEY_ALWAYS_VISIBLE, true),
        ).thenReturn(false)

        assertFalse(PulseDeckSystemBars.alwaysVisible(context))
    }

    @Test
    fun savesToggleWithoutChangingAnyOtherPreferences() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(
            context.getSharedPreferences(
                PulseDeckSystemBars.PREFS_NAME,
                Context.MODE_PRIVATE,
            ),
        ).thenReturn(preferences)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(PulseDeckSystemBars.KEY_ALWAYS_VISIBLE, true))
            .thenReturn(editor)

        PulseDeckSystemBars.setAlwaysVisible(context, true)

        verify(editor).putBoolean(PulseDeckSystemBars.KEY_ALWAYS_VISIBLE, true)
        verify(editor).apply()
    }
}
