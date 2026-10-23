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

    private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "oga", "opus", "wma", "aif", "aiff", "alac", "ape", "wv")

    /**
     * V151 (faric 23.10 03:57: «в кінці назви MP3 стоїть… давай не писати»): a file name without its
     * audio extension; other dots stay («VOL. 1 — …»).
     */
    fun withoutAudioExtension(name: String?): String {
        val n = name.orEmpty().trim()
        val dot = n.lastIndexOf('.')
        if (dot <= 0) return n
        return if (n.substring(dot + 1).lowercase() in AUDIO_EXTENSIONS) n.substring(0, dot).trim() else n
    }

    /**
     * V151: what the screen, the shade and the video call the song — the tag title; no tag (or a tag
     * that lost emoji, V97) → the file name without «.mp3».
     */
    fun display(
        tagTitle: String?,
        fileName: String?,
    ): String {
        val tag = tagTitle.orEmpty().trim()
        val file = withoutAudioExtension(fileName)
        if (tag.isBlank()) return file.ifBlank { "Без назви" }
        if (file.isNotBlank() && lostCharacters(tag) > lostCharacters(file)) return file
        return tag
    }

    /** «?» and «�» — characters a text lost on the way (an emoji written into an old tag). */
    private fun lostCharacters(text: String): Int = text.count { it == '?' || it == '�' }
}
