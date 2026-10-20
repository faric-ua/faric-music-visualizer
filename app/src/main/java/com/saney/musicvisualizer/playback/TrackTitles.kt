package com.saney.musicvisualizer.playback

/**
 * V97 (faric 20.10 23:47–23:55, M30s: «???? TRACK 06 ??? SKYBR» — «файли підписані значками,
 * замість них знак питання»). The song's tag title often cannot hold emoji (old ID3 text), so the
 * tag says «????» while the FILE NAME keeps 🔥 ⚡ …. When the tag lost characters and the file name
 * did not, show the file name. Pure Kotlin — tested on JVM.
 */
object TrackTitles {
    fun pick(tagTitle: String?, fileName: String?): String {
        val tag = tagTitle.orEmpty().trim()
        val file = fileName.orEmpty().substringBeforeLast('.', fileName.orEmpty()).trim()
        if (tag.isBlank()) return file.ifBlank { "Без назви" }
        if (file.isNotBlank() && lostCharacters(tag) > lostCharacters(file)) return file
        return tag
    }

    /** «?» and «�» — characters a text lost on the way (an emoji written into an old tag). */
    private fun lostCharacters(text: String): Int = text.count { it == '?' || it == '�' }
}
