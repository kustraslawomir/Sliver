package app.sliver.feature.camera

import app.sliver.data.LookPreset
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val HOLD_THRESHOLD_MS = 300L

/** "Night Neon · 09:05" for the today-captured card; HH:mm in [zone]. */
fun todayCardDetail(look: LookPreset, takenAt: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val t = takenAt.toLocalDateTime(zone)
    val name = look.name.lowercase().split('_').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    return "$name · ${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

val SelfTimerOptions = listOf(0, 3, 10)

fun nextSelfTimer(current: Int): Int =
    SelfTimerOptions[(SelfTimerOptions.indexOf(current) + 1) % SelfTimerOptions.size]

/** mm:ss, e.g. 00:07. */
fun formatTimer(elapsedMs: Long): String {
    val total = (elapsedMs / 1000).coerceAtLeast(0)
    return "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
}
