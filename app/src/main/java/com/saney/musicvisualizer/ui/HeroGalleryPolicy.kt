package com.saney.musicvisualizer.ui

import java.util.Locale

/**
 * Display policy only. No SAF source, persisted PNG, or GF composition is removed.
 */
object HeroGalleryPolicy {
    private val prefix = Regex("""^\s*\d{1,3}(?:[\s._-]+|$)""")
    private val scorpionB = Regex("""^cyber\s+scorpion\s+b$""", RegexOption.IGNORE_CASE)

    fun visibleExternalHero(title: String): Boolean {
        val readable = title.replace(prefix, "").replace('_', ' ').trim()
            .lowercase(Locale.ROOT)
        return !scorpionB.matches(readable)
    }
}
