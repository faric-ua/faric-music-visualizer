package com.saney.musicvisualizer.export

/**
 * Where exports land (V76, faric 20.10 16:44: «папку перейменувати можна»).
 * Since v0.19.87 — PulseDeck; files made before stay in the old FARIC
 * folders, nothing is moved or deleted. The batch "already exists" check
 * looks in both.
 */
object ExportFolders {
    const val NAME = "PulseDeck"
    const val VIDEO = "Movies/$NAME"
    const val PICTURES = "Pictures/$NAME"
    const val LEGACY_VIDEO = "Movies/FARIC"
    const val LEGACY_PICTURES = "Pictures/FARIC"

    /** Movies/PulseDeck/Album/ → Movies/FARIC/Album/ (folder before v0.19.87); null when not under [VIDEO]. */
    fun legacyVideoPath(relativePath: String): String? =
        if (relativePath == VIDEO || relativePath.startsWith("$VIDEO/")) {
            LEGACY_VIDEO + relativePath.removePrefix(VIDEO)
        } else {
            null
        }
}
