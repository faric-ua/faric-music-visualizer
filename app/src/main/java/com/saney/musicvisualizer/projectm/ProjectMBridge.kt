package com.saney.musicvisualizer.projectm

object ProjectMBridge {
    init {
        System.loadLibrary("faric_projectm")
    }

    @Volatile
    private var active = false

    fun create(width: Int, height: Int, presetPath: String) {
        nativeCreate(width, height, presetPath)
        active = true
    }

    fun resize(width: Int, height: Int) {
        if (active) nativeResize(width, height)
    }

    fun render() {
        if (active) nativeRender()
    }

    fun nextPreset() {
        if (active) nativeNextPreset()
    }

    fun destroy() {
        if (!active) return
        active = false
        nativeDestroy()
    }

    fun pushPcmIfActive(pcm: ShortArray) {
        if (!active || pcm.isEmpty()) return
        nativeAddPcm(pcm, pcm.size)
    }

    private external fun nativeCreate(width: Int, height: Int, presetPath: String)
    private external fun nativeResize(width: Int, height: Int)
    private external fun nativeRender()
    private external fun nativeAddPcm(pcm: ShortArray, frameCount: Int)
    private external fun nativeNextPreset()
    private external fun nativeDestroy()
}
