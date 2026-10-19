package com.saney.musicvisualizer.theme

/**
 * Text on the vinyl label (V72, faric 19.10 23:29): the song name did not fit
 * («Ed Sheeran - Shivers (» cut off) and «FARIC VINYL» was printed under it.
 * Now: the name is cleaned of file-name junk, split into lines
 * («Ed Sheeran» / «- Shivers» / «(Radio Edit)») and under it — the artist or just «VINYL».
 * Pure Kotlin: the live screen and MP4 export use the same lines.
 */
object VinylLabel {
    const val MAX_CHARS_PER_LINE = 14
    const val MAX_LINES = 3
    const val FALLBACK_BOTTOM = "VINYL"

    private val ignore = setOf(RegexOption.IGNORE_CASE)
    private val audioExtension = Regex("\\.(mp3|flac|wav|m4a|aac|ogg|opus|wma|alac|aiff?)$", ignore)
    private const val DOMAIN = "(?:www\\.)?[A-Za-z0-9-]+\\.(?:net|com|ru|ua|pro|org|me|info|fm|cc|to|biz|club|top)"
    private val siteInBrackets = Regex("[(\\[]\\s*$DOMAIN\\s*[)\\]]", ignore)
    private val siteAtStart = Regex("^$DOMAIN(?=[\\s_-]|$)", ignore)
    private val bracketGroup = Regex("[(\\[][^)\\]]*[)\\]]")
    private val dashSeparator = Regex("\\s+[-–—]\\s+")

    /** «Ed_Sheeran_-_Shivers_(www.muzofan.net).mp3» → «Ed Sheeran - Shivers». */
    fun cleanTitle(raw: String): String {
        val original = raw.trim()
        var text = original.replace(audioExtension, "")
        text = text.replace(siteInBrackets, " ")
        text = text.replace('_', ' ')
        text = text.replace(siteAtStart, " ")
        text = text.replace(Regex("\\s+"), " ").trim().trim('-', ' ')
        return text.ifBlank { original.replace(audioExtension, "") }
    }

    /** Lines for the label: artist part, «- song» part, every «(…)» on its own line; wrapped and capped. */
    fun titleLines(
        rawTitle: String,
        maxChars: Int = MAX_CHARS_PER_LINE,
        maxLines: Int = MAX_LINES,
    ): List<String> {
        val clean = cleanTitle(rawTitle)
        if (clean.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        clean.split(dashSeparator).forEachIndexed { index, segment ->
            val main = segment.replace(bracketGroup, " ").replace(Regex("\\s+"), " ").trim()
            if (main.isNotEmpty()) parts += if (index > 0) "- $main" else main
            bracketGroup.findAll(segment).forEach { parts += it.value.trim() }
        }
        val wrapped = parts.flatMap { wrap(it, maxChars) }
        if (wrapped.size <= maxLines) return wrapped
        val kept = wrapped.take(maxLines).toMutableList()
        kept[kept.lastIndex] = ellipsize(kept.last(), maxChars)
        return kept
    }

    /** Line under the title: the artist when known, otherwise «VINYL» (no «FARIC»). */
    fun bottomLine(artist: String?): String =
        artist?.let(::cleanTitle)?.takeIf { it.isNotBlank() && !it.equals("Невідомий виконавець", true) }
            ?.let { if (it.length > 24) it.take(23).trimEnd() + "…" else it }
            ?: FALLBACK_BOTTOM

    private fun wrap(text: String, maxChars: Int): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            var piece = word
            while (piece.length > maxChars) {
                if (current.isNotEmpty()) {
                    lines += current
                    current = ""
                }
                lines += piece.take(maxChars)
                piece = piece.drop(maxChars)
            }
            current =
                when {
                    current.isEmpty() -> piece
                    current.length + 1 + piece.length <= maxChars -> "$current $piece"
                    else -> {
                        lines += current
                        piece
                    }
                }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    private fun ellipsize(line: String, maxChars: Int): String =
        (if (line.length >= maxChars) line.take(maxChars - 1).trimEnd() else line) + "…"
}
