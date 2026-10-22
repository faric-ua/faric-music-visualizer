package com.saney.musicvisualizer.ui

import java.util.Locale

/**
 * V138 (faric 22.10 22:50, скрін Poweramp «48 КГЦ 320 КБ/С MP3»): two small lines on the main screen
 * and in the video — the song file and the MP4 that PulseDeck makes from it. Pure, tested.
 */
object FileInfoText {
    fun formatOf(
        mime: String?,
        fileName: String?,
    ): String? {
        val m = mime?.lowercase(Locale.ROOT).orEmpty()
        val ext = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        return when {
            m == "audio/mpeg" || ext == "mp3" -> "MP3"
            m == "audio/flac" || m == "audio/x-flac" || ext == "flac" -> "FLAC"
            m.contains("opus") || ext == "opus" -> "OPUS"
            m.contains("vorbis") || ext == "ogg" -> "OGG"
            m.contains("wav") || m == "audio/raw" || ext == "wav" -> "WAV"
            m.contains("mp4a") || m == "audio/mp4" || ext == "m4a" || ext == "aac" -> if (ext == "aac") "AAC" else "M4A"
            ext.isNotEmpty() -> ext.uppercase(Locale.ROOT)
            else -> null
        }
    }

    private fun kHz(sampleRate: Int): String {
        val k = sampleRate / 1000.0
        return if (sampleRate % 1000 == 0) "${sampleRate / 1000} кГц" else String.format(Locale.ROOT, "%.1f кГц", k).replace('.', ',')
    }

    private fun kbps(bitRate: Long): String = "${(bitRate + 500) / 1000} кбіт/с"

    private fun mbps(bitRate: Long): String = String.format(Locale.ROOT, "%.1f Мбіт/с", bitRate / 1_000_000.0).replace('.', ',')

    /** «Пісня · MP3 · 48 кГц · 320 кбіт/с»; nothing known → null. */
    fun source(
        format: String?,
        sampleRateHz: Int?,
        bitRate: Long?,
    ): String? {
        val parts =
            listOfNotNull(
                format,
                sampleRateHz?.takeIf { it > 0 }?.let(::kHz),
                bitRate?.takeIf { it > 0 }?.let(::kbps),
            )
        return if (parts.isEmpty()) null else "Пісня · " + parts.joinToString(" · ")
    }

    /** «Відео · 1080×1920 · 30 к/с · 18,7 Мбіт/с · AAC 160 кбіт/с». */
    fun video(
        width: Int,
        height: Int,
        fps: Int,
        videoBitRate: Long,
        audioBitRate: Long,
    ): String = "Відео · ${width}×$height · $fps к/с · ${mbps(videoBitRate)} · AAC ${kbps(audioBitRate)}"
}
