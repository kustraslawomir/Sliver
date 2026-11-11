package app.sliver.feature.trim

import app.sliver.data.export.SECOND_MS
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

const val MINIMAP_THRESHOLD_MS = 60_000L
const val TICK_MS = 250L
const val MAX_THUMBS = 120

fun maxStartMs(durationMs: Long, speed: Float): Long =
    (durationMs - (SECOND_MS * speed).toLong()).coerceAtLeast(0)

fun clampStart(startMs: Long, durationMs: Long, speed: Float): Long =
    startMs.coerceIn(0, maxStartMs(durationMs, speed))

/** Only our own recordings are ever deleted; gallery sources must be left alone. */
fun isTmp(path: String) = path.substringAfterLast('/').startsWith("rec_")

fun isShortClip(durationMs: Long) = durationMs < SECOND_MS

fun showMinimap(durationMs: Long) = durationMs > MINIMAP_THRESHOLD_MS

/** Index of the 0.25 s grid cell the window start sits in; a change means a haptic tick. */
fun tickIndex(startMs: Long) = startMs / TICK_MS

/** Start that centers the 1 s window on [fraction] (0..1) of the clip. */
fun minimapJump(fraction: Float, durationMs: Long, speed: Float): Long =
    clampStart((fraction.coerceIn(0f, 1f) * durationMs - SECOND_MS * speed / 2).toLong(), durationMs, speed)

/** Today and the six days before it, newest first. */
fun lastSevenDays(today: LocalDate): List<LocalDate> = (0..6).map { today.minus(DatePeriod(days = it)) }

fun thumbCount(durationMs: Long) = ((durationMs + SECOND_MS - 1) / SECOND_MS).toInt().coerceIn(1, MAX_THUMBS)

/** '4:32 · 812 MB' for the loading state; size is omitted when unknown (0). */
fun clipInfo(durationMs: Long, bytes: Long): String {
    val total = durationMs / 1000
    val time = "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    val size = when {
        bytes <= 0 -> return time
        bytes >= 1_000_000_000 -> "${bytes / 100_000_000 / 10}.${bytes / 100_000_000 % 10} GB"
        else -> "${(bytes / 1_000_000).coerceAtLeast(1)} MB"
    }
    return "$time · $size"
}

/** Overall preparing progress 0..1: open 10%, auto-pick 30%, thumbnails 60%. */
fun prepareProgress(opened: Boolean, picked: Boolean, thumbsDone: Int, thumbsTotal: Int): Float {
    if (!opened) return 0f
    if (!picked) return 0.1f
    return 0.4f + 0.6f * (thumbsDone.toFloat() / thumbsTotal.coerceAtLeast(1)).coerceIn(0f, 1f)
}
