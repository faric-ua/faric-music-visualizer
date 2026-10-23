package com.saney.musicvisualizer.projectm

/**
 * V152 (faric 23.10 04:34: «щоб рандомно ще було»): visualizations in a random order — every one
 * once before any repeats (a shuffled «round»), the next round shuffled again. The MP4 gets the same
 * order as the screen. Pure Kotlin — PresetOrderTest.
 */
object PresetOrder {
    fun shuffled(
        pool: List<String>,
        seed: Long,
    ): List<String> {
        val out = pool.toMutableList()
        val random = java.util.Random(seed)
        for (i in out.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val t = out[i]
            out[i] = out[j]
            out[j] = t
        }
        return out
    }

    /**
     * The next one after [current] in [order]; at the end of a round [reshuffle] gives the next round
     * (it never starts with the one just shown).
     */
    fun next(
        order: List<String>,
        current: String?,
        reshuffle: () -> List<String>,
    ): Pair<String, List<String>> {
        if (order.isEmpty()) throw IllegalArgumentException("empty")
        val index = order.indexOf(current)
        if (index < 0) return order.first().let { first -> (if (first == current && order.size > 1) order[1] else first) to order }
        if (index + 1 < order.size) return order[index + 1] to order
        var round = reshuffle()
        if (round.size > 1 && round.first() == current) round = round.drop(1) + round.first()
        return round.first() to round
    }
}
