package com.saney.musicvisualizer.ui

/**
 * Presentation only. External hero source titles/IDs and their order in SAF
 * storage remain untouched; the tile has its own stable display index.
 */
object ExternalHeroTileLabel {
    private val leadingOrdinal = Regex("""^\\s*\\d{1,3}(?:[\\s._-]+)""")

    fun number(index: Int): String {
        require(index >= 0) { "Hero tile index cannot be negative" }
        return (index + 1).toString().padStart(2, '0')
    }

    fun caption(sourceTitle: String): String =
        sourceTitle.replace(leadingOrdinal, "").trim()
            .ifBlank { "PNG-герой" }
}
