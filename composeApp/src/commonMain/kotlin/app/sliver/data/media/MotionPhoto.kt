package app.sliver.data.media

/** The MP4 embedded in a Motion Photo: [offset] and [length] inside the file, plus the still's position in the video when declared. */
data class MotionVideo(val offset: Int, val length: Int, val presentationUs: Long?)

object MotionPhoto {
    private val itemTag = Regex("""<Container:Item\b[^>]*?/?>""", RegexOption.DOT_MATCHES_ALL)
    private val presentation = Regex("""MotionPhotoPresentationTimestampUs\s*=\s*"(-?\d+)"""")

    /** Cheap pre-check on the first [HEAD_BYTES] of a file: only files that declare a Motion Photo need to be read in full. */
    fun declaresMotion(head: ByteArray): Boolean =
        xmpOf(head)?.let { Regex("""MotionPhoto\s*=\s*"1"""").containsMatchIn(it) } == true

    const val HEAD_BYTES = 256 * 1024

    /** Finds the video of a Motion Photo 1.0 (JPEG: trailing bytes; HEIC/AVIF: `mpvd` box). Legacy MicroVideo is ignored. */
    fun find(bytes: ByteArray): MotionVideo? {
        val xmp = xmpOf(bytes) ?: return null
        if (!Regex("""MotionPhoto\s*=\s*"1"""").containsMatchIn(xmp)) return null
        val ts = presentation.find(xmp)?.groupValues?.get(1)?.toLongOrNull()?.takeIf { it >= 0 }
        val isoBox = isIsoFile(bytes)
        if (isoBox) return mpvd(bytes)?.let { MotionVideo(it.first, it.second, ts) }?.takeIf { looksLikeMp4(bytes, it.offset, it.length) }
        val length = itemTag.findAll(xmp)
            .map { it.value }
            .firstOrNull { Regex("""Semantic\s*=\s*"MotionPhoto"""").containsMatchIn(it) }
            ?.let { Regex("""Item:Length\s*=\s*"(\d+)"""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: return null
        if (length <= 8 || length >= bytes.size) return null
        val offset = bytes.size - length
        return MotionVideo(offset, length, ts).takeIf { looksLikeMp4(bytes, offset, length) }
    }

    private fun xmpOf(bytes: ByteArray): String? {
        val head = bytes.decodeToString(0, minOf(bytes.size, HEAD_BYTES), throwOnInvalidSequence = false)
        val start = head.indexOf("<x:xmpmeta")
        if (start < 0) return null
        val end = head.indexOf("</x:xmpmeta>", start)
        return if (end < 0) null else head.substring(start, end)
    }

    private fun isIsoFile(b: ByteArray) = b.size > 12 && b[4] == 'f'.code.toByte() && b[5] == 't'.code.toByte() && b[6] == 'y'.code.toByte() && b[7] == 'p'.code.toByte()

    private fun looksLikeMp4(b: ByteArray, offset: Int, length: Int): Boolean =
        offset >= 0 && length > 12 && offset + length <= b.size &&
            b[offset + 4] == 'f'.code.toByte() && b[offset + 5] == 't'.code.toByte() && b[offset + 6] == 'y'.code.toByte() && b[offset + 7] == 'p'.code.toByte()

    private fun mpvd(b: ByteArray): Pair<Int, Int>? {
        var pos = 0
        while (pos + 8 <= b.size) {
            val size32 = u32(b, pos)
            val header = if (size32 == 1L) 16 else 8
            val size = when (size32) {
                0L -> (b.size - pos).toLong()
                1L -> if (pos + 16 <= b.size) u32(b, pos + 8) * 0x1_0000_0000L + u32(b, pos + 12) else return null
                else -> size32
            }
            if (size < header || pos + size > b.size) return null
            if (b[pos + 4] == 'm'.code.toByte() && b[pos + 5] == 'p'.code.toByte() && b[pos + 6] == 'v'.code.toByte() && b[pos + 7] == 'd'.code.toByte()) {
                return (pos + header) to (size - header).toInt()
            }
            pos += size.toInt()
        }
        return null
    }

    private fun u32(b: ByteArray, p: Int): Long =
        ((b[p].toLong() and 0xFF) shl 24) or ((b[p + 1].toLong() and 0xFF) shl 16) or ((b[p + 2].toLong() and 0xFF) shl 8) or (b[p + 3].toLong() and 0xFF)

    /** Default clip start: the 1 s window centered on [presentationUs], else on the midpoint, kept inside the video. */
    fun windowStartMs(presentationUs: Long?, durationMs: Long): Long {
        val center = presentationUs?.div(1000)?.takeIf { it in 0..durationMs } ?: (durationMs / 2)
        return (center - 500).coerceIn(0L, maxOf(0L, durationMs - 1000))
    }
}
