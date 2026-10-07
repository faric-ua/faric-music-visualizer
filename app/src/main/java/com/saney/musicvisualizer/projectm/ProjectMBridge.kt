package com.saney.musicvisualizer.projectm

import com.saney.musicvisualizer.analysis.SceneSignal

object ProjectMBridge {
    init {
        System.loadLibrary("faric_projectm")
    }

    @Volatile
    private var active = false

    @Volatile
    private var offlineExportMode = false

    fun create(
        width: Int,
        height: Int,
        presetPath: String,
        texturePath: String,
        profile: ProjectMPerformanceProfile,
    ) {
        nativeCreate(
            width,
            height,
            presetPath,
            texturePath,
            profile.meshX,
            profile.meshY,
            profile.targetFps,
            profile.softCutSeconds,
        )
        active = true
    }

    fun resize(width: Int, height: Int) {
        if (active) nativeResize(width, height)
    }

    fun render() {
        if (active) nativeRender()
    }

    fun renderToFramebuffer(
        framebuffer: Int,
    ) {
        if (active) {
            nativeRenderToFramebuffer(
                framebuffer,
            )
        }
    }

    fun loadPreset(
        path: String,
        smoothTransition: Boolean = true,
    ): Long {
        if (!active || path.isBlank()) return -1L
        return nativeLoadPreset(
            path,
            smoothTransition,
        )
    }

    fun nextPreset() {
        if (active) nativeNextPreset()
    }

    fun enableAutoPresetSwitching(enabled: Boolean) {
        if (active) nativeSetAutoPresetSwitching(enabled)
    }

    fun setForegroundSample(sample: FaricForegroundSample) {
        if (active) nativeSetForegroundSample(sample.nativeId)
    }

    fun setBackgroundVisible(
        visible: Boolean,
    ) {
        if (active) {
            nativeSetBackgroundVisible(
                visible,
            )
        }
    }

    fun setForegroundVisibility(
        centerVisible: Boolean,
        edgeFxVisible: Boolean,
    ) {
        if (active) {
            nativeSetForegroundVisibility(
                centerVisible,
                edgeFxVisible,
            )
        }
    }

    fun setForegroundTuning(
        tuning: ProjectMForegroundTuning,
    ) {
        if (!active) return

        val safe =
            tuning.sanitized()

        nativeSetForegroundTuning(
            safe.centerScale,
            safe.centerRotationDegrees,
            safe.centerOpacity,
            safe.centerBassGain,
            safe.centerMidGain,
            safe.centerHighGain,
            safe.centerBeatGain,
            safe.edgeOpacity,
            safe.edgeBassGain,
            safe.edgeHighGain,
            safe.edgeBeatGain,
        )
    }

    fun beginOfflineExport() {
        offlineExportMode = true
    }

    fun endOfflineExport() {
        offlineExportMode = false
        if (active) {
            nativeSetFrameTime(
                -1.0,
            )
        }
    }

    fun setFrameTime(
        seconds: Double,
    ) {
        if (active) {
            nativeSetFrameTime(
                seconds,
            )
        }
    }

    fun pushOfflinePcm(
        pcm: ShortArray,
    ) {
        if (
            !active ||
            pcm.isEmpty()
        ) {
            return
        }
        nativeAddPcm(
            pcm,
            pcm.size,
        )
    }

    fun pushOfflineSignal(
        signal: SceneSignal,
    ) {
        if (!active) return
        nativeSetSignal(
            signal.amplitude,
            signal.bass,
            signal.mid,
            signal.high,
            signal.beatStrength,
        )
    }

    fun destroy() {
        if (!active) return
        active = false
        nativeDestroy()
    }

    fun pushPcmIfActive(pcm: ShortArray) {
        if (
            !active ||
            offlineExportMode ||
            pcm.isEmpty()
        ) {
            return
        }
        nativeAddPcm(pcm, pcm.size)
    }

    fun pushSignalIfActive(signal: SceneSignal) {
        if (
            !active ||
            offlineExportMode
        ) {
            return
        }
        nativeSetSignal(
            signal.amplitude,
            signal.bass,
            signal.mid,
            signal.high,
            signal.beatStrength,
        )
    }

    private external fun nativeCreate(
        width: Int,
        height: Int,
        presetPath: String,
        texturePath: String,
        meshX: Int,
        meshY: Int,
        targetFps: Int,
        softCutSeconds: Double,
    )

    private external fun nativeResize(width: Int, height: Int)
    private external fun nativeRender()
    private external fun nativeRenderToFramebuffer(
        framebuffer: Int,
    )
    private external fun nativeSetFrameTime(
        seconds: Double,
    )
    private external fun nativeAddPcm(pcm: ShortArray, frameCount: Int)
    private external fun nativeSetSignal(
        amplitude: Float,
        bass: Float,
        mid: Float,
        high: Float,
        beat: Float,
    )
    private external fun nativeLoadPreset(
        path: String,
        smoothTransition: Boolean,
    ): Long
    private external fun nativeSetAutoPresetSwitching(enabled: Boolean)
    private external fun nativeSetBackgroundVisible(
        visible: Boolean,
    )
    private external fun nativeSetForegroundSample(sampleId: Int)
    private external fun nativeSetForegroundVisibility(
        centerVisible: Boolean,
        edgeFxVisible: Boolean,
    )
    private external fun nativeSetForegroundTuning(
        centerScale: Float,
        centerRotationDegrees: Float,
        centerOpacity: Float,
        centerBassGain: Float,
        centerMidGain: Float,
        centerHighGain: Float,
        centerBeatGain: Float,
        edgeOpacity: Float,
        edgeBassGain: Float,
        edgeHighGain: Float,
        edgeBeatGain: Float,
    )
    private external fun nativeNextPreset()
    private external fun nativeDestroy()
}
