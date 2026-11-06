package com.saney.musicvisualizer.export

/**
 * V240 (faric 06.11 05:36, третій пакет поспіль: одна пісня ×2,9, друга ×7,8 при тих самих налаштуваннях і 34 °C;
 * що робилось на телефоні — «не пам'ятаю»; 17:15: «додавай»).
 *
 * How long PulseDeck was on screen, hidden (another app / the lock screen) or with the screen off — so the batch
 * summary itself says why a song was slow. MainActivity tells it each change; every song takes the difference of
 * two [snapshot]s. Pure — tested on JVM (the time is passed in).
 */
class VisibilityClock(
    startState: State,
    startMs: Long,
) {
    enum class State { ON_SCREEN, HIDDEN, SCREEN_OFF }

    data class Share(
        val onScreenMs: Long = 0L,
        val hiddenMs: Long = 0L,
        val screenOffMs: Long = 0L,
    ) {
        val totalMs: Long get() = onScreenMs + hiddenMs + screenOffMs

        operator fun minus(o: Share) = Share(onScreenMs - o.onScreenMs, hiddenMs - o.hiddenMs, screenOffMs - o.screenOffMs)

        /** «на екрані 10 % · згорнуто 70 % · екран вимкнено 20 %» — only the parts that were there. */
        fun label(): String? {
            val total = totalMs
            if (total <= 0L) return null
            val parts =
                listOf(onScreenMs to "на екрані", hiddenMs to "згорнуто", screenOffMs to "екран вимкнено")
                    .filter { it.first > 0L }
            if (parts.size == 1) return parts[0].second + " весь час"
            // Percent rounding that adds up to 100.
            val raw = parts.map { it.first * 100.0 / total }
            val floors = raw.map { it.toInt() }.toMutableList()
            var left = 100 - floors.sum()
            raw.indices.sortedByDescending { raw[it] - floors[it] }.forEach { if (left > 0) { floors[it]++; left-- } }
            return parts.indices.filter { floors[it] > 0 }.joinToString(" · ") { parts[it].second + " " + floors[it] + " %" }
                .ifBlank { null }
        }
    }

    private var state = startState
    private var since = startMs
    private var done = Share()

    @Synchronized
    fun mark(
        newState: State,
        nowMs: Long,
    ) {
        if (newState == state) return
        done = add(done, state, (nowMs - since).coerceAtLeast(0L))
        state = newState
        since = nowMs
    }

    /** Everything so far, the running part included. */
    @Synchronized
    fun snapshot(nowMs: Long): Share = add(done, state, (nowMs - since).coerceAtLeast(0L))

    @get:Synchronized
    val current: State get() = state

    private fun add(
        s: Share,
        st: State,
        ms: Long,
    ): Share =
        when (st) {
            State.ON_SCREEN -> s.copy(onScreenMs = s.onScreenMs + ms)
            State.HIDDEN -> s.copy(hiddenMs = s.hiddenMs + ms)
            State.SCREEN_OFF -> s.copy(screenOffMs = s.screenOffMs + ms)
        }
}
