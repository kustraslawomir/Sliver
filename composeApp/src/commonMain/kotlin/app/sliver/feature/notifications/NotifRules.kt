package app.sliver.feature.notifications

import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.LastCallTime
import app.sliver.data.settings.ReminderSettings
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

enum class NotifKind { REMINDER, LAST_CALL, ON_THIS_DAY }

data class PlannedNotif(val kind: NotifKind, val at: LocalDateTime)

const val HORIZON_DAYS = 7
const val MAX_PER_DAY = 2
const val WINDOW_MS = 10 * 60_000L
const val GOLDEN_LEAD_MINUTES = 20
val OnThisDayTime = LocalTime(12, 0)

fun ReminderSettings.enabledOn(date: LocalDate) = days.getOrElse(date.dayOfWeek.isoDayNumber - 1) { false }

/** Fixed time, or [GOLDEN_LEAD_MINUTES] before the evening golden hour when that is known for [date]. */
fun ReminderSettings.timeOn(date: LocalDate, golden: (LocalDate) -> LocalTime?): LocalTime {
    val sun = (if (followGoldenHour) golden(date) else null) ?: return time
    val minutes = (sun.toSecondOfDay() / 60 - GOLDEN_LEAD_MINUTES).coerceAtLeast(0)
    return LocalTime(minutes / 60, minutes % 60)
}

/** First moment after [now] (searching a week ahead) at which [kind] is due, honouring the days mask. */
fun nextFire(
    r: ReminderSettings,
    kind: NotifKind,
    now: LocalDateTime,
    golden: (LocalDate) -> LocalTime? = { null },
    handledToday: Set<NotifKind> = emptySet(),
): LocalDateTime? {
    if (!r.enabled) return null
    for (i in 0..HORIZON_DAYS) {
        val date = now.date.plus(DatePeriod(days = i))
        if (!r.enabledOn(date)) continue
        val main = r.timeOn(date, golden)
        val time = when (kind) {
            NotifKind.REMINDER -> main
            NotifKind.LAST_CALL -> if (r.lastCall && main < LastCallTime) LastCallTime else continue
            NotifKind.ON_THIS_DAY -> return null
        }
        val at = LocalDateTime(date, time)
        if (at > now) return at
        // Inside its delivery window and not yet handled: keep it, so a re-plan never swallows a reminder that is merely late.
        if (i == 0 && kind !in handledToday && at.epochMillisIn(TimeZone.UTC) + WINDOW_MS > now.epochMillisIn(TimeZone.UTC)) return at
    }
    return null
}

/** One alarm for the next reminder, one for the next last call, plus any "on this day" memories within the week. */
fun planNotifications(
    s: AppSettings,
    now: LocalDateTime,
    withClip: Set<LocalDate>,
    golden: (LocalDate) -> LocalTime? = { null },
    handledToday: Set<NotifKind> = emptySet(),
): List<PlannedNotif> {
    val plan = listOf(NotifKind.REMINDER, NotifKind.LAST_CALL)
        .mapNotNull { k -> nextFire(s.reminder, k, now, golden, handledToday)?.let { PlannedNotif(k, it) } }.toMutableList()
    if (s.notif.onThisDay) {
        List(HORIZON_DAYS) { now.date.plus(DatePeriod(days = it)) }.forEach { date ->
            if (date.minus(DatePeriod(months = 1)) in withClip) plan += PlannedNotif(NotifKind.ON_THIS_DAY, LocalDateTime(date, OnThisDayTime))
        }
    }
    return plan.filter { it.kind != NotifKind.ON_THIS_DAY || it.at > now }.sortedBy { it.at }
}

/** What fired today: [handled] = every fire (posted or skipped), [posted] = notifications actually shown. */
interface ReminderLog {
    fun handled(today: LocalDate): Set<NotifKind>
    fun posted(today: LocalDate): Set<NotifKind>
    fun record(today: LocalDate, kind: NotifKind, posted: Boolean)
}

/** Fire-time decision: [posted] is what was already posted today (max [MAX_PER_DAY], each kind once). */
fun shouldPost(kind: NotifKind, r: ReminderSettings, todayHasClip: Boolean, posted: Set<NotifKind>): Boolean = when (kind) {
    NotifKind.ON_THIS_DAY -> true
    NotifKind.REMINDER -> r.enabled && kind !in posted && posted.size < MAX_PER_DAY && !(r.skipIfDone && todayHasClip)
    NotifKind.LAST_CALL -> r.enabled && r.lastCall && kind !in posted && posted.size < MAX_PER_DAY && !todayHasClip
}

data class NotifCopy(val title: String, val body: String)

fun weekdayName(date: LocalDate) = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

fun reminderCopy(kind: NotifKind, date: LocalDate): NotifCopy = when (kind) {
    NotifKind.REMINDER -> NotifCopy("Today's clip", "${weekdayName(date)} is still empty. A few seconds is enough.")
    NotifKind.LAST_CALL -> NotifCopy("Today's clip", "Still time for ${weekdayName(date)}. A few seconds is enough.")
    NotifKind.ON_THIS_DAY -> {
        val then = date.minus(DatePeriod(months = 1))
        NotifCopy(
            "One month ago today",
            "${then.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${then.day} ${then.month.name.lowercase().replaceFirstChar { it.uppercase() }}. Want to see it?"
        )
    }
}

fun filmReadyCopy(name: String, seconds: Int) = NotifCopy("“$name” is ready", "$seconds clips. Press play.")

val BackupPausedCopy = NotifCopy("Backup paused", "Your Drive is full. Your clips are safe on this phone.")
val BackupStuckCopy = NotifCopy("Backup paused", "We couldn't reach your Drive. Your clips are safe on this phone.")

fun backupProblemCopy(error: app.sliver.data.backup.BackupError) =
    if (error == app.sliver.data.backup.BackupError.DRIVE_FULL) BackupPausedCopy else BackupStuckCopy

sealed interface DeepLinkTarget {
    data object Camera : DeepLinkTarget
    data object Backup : DeepLinkTarget
    data object FillThePast : DeepLinkTarget
    data object Project : DeepLinkTarget
    data object Gallery : DeepLinkTarget
    data object MakeFilm : DeepLinkTarget
    data class Film(val id: String) : DeepLinkTarget
    data class Day(val date: LocalDate) : DeepLinkTarget
}

fun DeepLinkTarget.encode(): String = when (this) {
    DeepLinkTarget.Camera -> "camera"
    DeepLinkTarget.Backup -> "backup"
    DeepLinkTarget.FillThePast -> "fill"
    DeepLinkTarget.Project -> "project"
    DeepLinkTarget.Gallery -> "gallery"
    DeepLinkTarget.MakeFilm -> "film-new"
    is DeepLinkTarget.Film -> "film:$id"
    is DeepLinkTarget.Day -> "day:$date"
}

fun parseDeepLink(raw: String?): DeepLinkTarget? = when {
    raw == null -> null
    raw == "camera" -> DeepLinkTarget.Camera
    raw == "backup" -> DeepLinkTarget.Backup
    raw == "fill" -> DeepLinkTarget.FillThePast
    raw == "project" -> DeepLinkTarget.Project
    raw == "gallery" -> DeepLinkTarget.Gallery
    raw == "film-new" -> DeepLinkTarget.MakeFilm
    raw.startsWith("film:") -> DeepLinkTarget.Film(raw.removePrefix("film:"))
    raw.startsWith("day:") -> runCatching {
        LocalDate.parse(raw.removePrefix("day:"))
    }.getOrNull()?.let(DeepLinkTarget::Day)
    else -> null
}

fun LocalDateTime.epochMillisIn(zone: TimeZone): Long = toInstant(zone).toEpochMilliseconds()
