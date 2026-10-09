package com.saney.musicvisualizer.export

import java.util.concurrent.atomic.AtomicBoolean

/**
 * One export run per Activity instance. Thread-safe against repeated user taps.
 * Does NOT survive process death and is not a background export coordinator.
 */
class ExportRunGate {
    private val running = AtomicBoolean(false)

    fun tryStart(): Boolean = running.compareAndSet(false, true)

    fun finish() {
        running.set(false)
    }

    fun isActive(): Boolean = running.get()
}
