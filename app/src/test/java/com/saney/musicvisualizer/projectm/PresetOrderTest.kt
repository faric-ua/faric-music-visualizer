package com.saney.musicvisualizer.projectm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PresetOrderTest {
    private val pool = (1..20).map { "p$it" }

    @Test fun shuffledKeepsEveryOneOnceAndIsRepeatable() {
        val a = PresetOrder.shuffled(pool, 42L)
        assertEquals(pool.toSet(), a.toSet())
        assertEquals(pool.size, a.size)
        assertEquals(a, PresetOrder.shuffled(pool, 42L))
        assertNotEquals(pool, a)
    }

    @Test fun nextWalksTheRoundThenReshufflesWithoutRepeatingTheLast() {
        val order = PresetOrder.shuffled(pool, 1L)
        var current: String? = order.first()
        var round = order
        val seen = mutableListOf(current!!)
        repeat(pool.size - 1) {
            val (n, r) = PresetOrder.next(round, current) { PresetOrder.shuffled(pool, 2L) }
            seen += n
            current = n
            round = r
        }
        assertEquals(pool.toSet(), seen.toSet())
        // the round ends: the next round never starts with the one just shown
        val last = current!!
        val (n, r) = PresetOrder.next(round, last) { listOf(last) + pool.filter { it != last } }
        assertNotEquals(last, n)
        assertEquals(pool.size, r.size)
    }
}
