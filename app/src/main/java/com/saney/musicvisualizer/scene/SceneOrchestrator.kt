package com.saney.musicvisualizer.scene

import android.os.Handler
import android.os.Looper

class SceneOrchestrator(
    private val engine: RandomSceneEngine = RandomSceneEngine(),
    private val onSceneChanged: (SceneSpec) -> Unit,
) : AutoCloseable {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var current: SceneSpec = engine.next()

    private val scheduledSwitch = object : Runnable {
        override fun run() {
            if (!running) return
            current = engine.next()
            onSceneChanged(current)
            scheduleNext()
        }
    }

    fun currentScene(): SceneSpec = current

    fun start() {
        if (running) return
        running = true
        onSceneChanged(current)
        scheduleNext()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(scheduledSwitch)
    }

    fun shuffleNow(): SceneSpec {
        current = engine.next()
        onSceneChanged(current)
        if (running) {
            scheduleNext()
        }
        return current
    }

    private fun scheduleNext() {
        handler.removeCallbacks(scheduledSwitch)
        if (running) {
            handler.postDelayed(scheduledSwitch, current.changeAfterMs)
        }
    }

    override fun close() {
        stop()
    }
}
