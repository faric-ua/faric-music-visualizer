package com.saney.musicvisualizer.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList

/**
 * V213: can this phone's H.264 encoder make a [width] × [height] video at [fps] (2K / 4K are not everywhere, 4K 60 is
 * rare)? Asked before the export, so the app says «телефон не вміє» instead of failing half way.
 */
object EncoderSupport {
    private const val MIME = "video/avc"

    private fun capabilities(): List<MediaCodecInfo.VideoCapabilities> =
        runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS)
                .codecInfos
                .filter { it.isEncoder && it.supportedTypes.any { type -> type.equals(MIME, ignoreCase = true) } }
                .mapNotNull { runCatching { it.getCapabilitiesForType(MIME).videoCapabilities }.getOrNull() }
        }.getOrDefault(emptyList())

    /** null — the phone did not answer (then the export just tries). */
    fun supports(
        width: Int,
        height: Int,
        fps: Int,
    ): Boolean? {
        val caps = capabilities()
        if (caps.isEmpty()) return null
        return caps.any { cap ->
            runCatching { cap.areSizeAndRateSupported(width, height, fps.toDouble()) }.getOrDefault(false) ||
                runCatching { cap.areSizeAndRateSupported(height, width, fps.toDouble()) }.getOrDefault(false)
        }
    }

    /** The highest bit rate an encoder that can make this size takes (null — unknown). */
    fun maxBitRate(
        width: Int,
        height: Int,
    ): Int? =
        capabilities()
            .filter { cap -> runCatching { cap.isSizeSupported(width, height) }.getOrDefault(false) }
            .maxOfOrNull { it.bitrateRange.upper }
}
