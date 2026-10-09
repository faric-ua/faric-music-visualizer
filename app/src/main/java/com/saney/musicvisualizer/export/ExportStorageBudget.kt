package com.saney.musicvisualizer.export

/**
 * Conservative *advisory* budget before a long export. Includes the encoded
 * temporary video AND the final MediaStore MP4 being written simultaneously.
 * Prevents running the whole song only to run out of disk at publish time.
 * Assumes typical mono 48kHz 16-bit projectM PCM cache when enabled.
 */
object ExportStorageBudget {
    private const val MIB = 1024L * 1024L
    private const val AAC_BIT_RATE = 160_000L
    private const val PCM_MONO_BYTES_PER_SECOND = 96_000L
    private const val MIN_VIDEO_BIT_RATE = 8_000_000L
    const val MIN_FREE_RESERVE_BYTES = 128L * MIB

    fun estimateRequiredBytes(
        durationMs: Long,
        width: Int,
        height: Int,
        needsPcm: Boolean,
    ): Long {
        require(durationMs > 0L && width > 0 && height > 0)
        val seconds = durationMs.toDouble() / 1000.0
        val videoBitRate = maxOf(MIN_VIDEO_BIT_RATE, width.toLong() * height * 6L)
        val encodedVideoBytes = videoBitRate.toDouble() / 8.0 * seconds
        val audioBytes = AAC_BIT_RATE.toDouble() / 8.0 * seconds
        val pcmBytes = if (needsPcm) PCM_MONO_BYTES_PER_SECOND * seconds else 0.0
        // Video temp + final MP4, AAC audio temp, mux overhead, 25% safety margin.
        val bytes = encodedVideoBytes * 2.5 + audioBytes * 2.0 + pcmBytes +
            MIN_FREE_RESERVE_BYTES
        return bytes.coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()
    }

    fun hasCapacity(availableBytes: Long, requiredBytes: Long): Boolean =
        availableBytes >= requiredBytes && requiredBytes > 0L

    fun formatMiB(bytes: Long): Long =
        ((bytes.coerceAtLeast(0L) + MIB - 1L) / MIB)
}
