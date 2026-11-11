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
