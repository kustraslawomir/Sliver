package app.sliver.feature.notifications

import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.ReminderSettings
import kotlin.random.Random
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class NotifKind { REMINDER, RANDOM, RUNNING_LATE, ON_THIS_DAY }

data class PlannedNotif(val kind: NotifKind, val at: LocalDateTime)

const val HORIZON_DAYS = 7
val RunningLateTime = LocalTime(21, 0)
val OnThisDayTime = LocalTime(12, 0)

/** Same day always yields the same minute, so rescheduling on every app open never moves it. */
fun randomTimeFor(date: LocalDate, start: LocalTime, end: LocalTime): LocalTime? {
    val from = start.toSecondOfDay() / 60
    val to = end.toSecondOfDay() / 60
    if (to <= from) return null
    val minute = from + Random(date.toEpochDays() * 1009L + from * 31 + to).nextInt(to - from + 1)
    return LocalTime(minute / 60, minute % 60)
}

private fun ReminderSettings.enabledOn(date: LocalDate) = days.getOrElse(date.dayOfWeek.isoDayNumber - 1) { false }

/** Reminder fire times for the next [HORIZON_DAYS] days, skipping days that already have a second. */
fun nextReminderTimes(r: ReminderSettings, now: LocalDateTime, withSecond: Set<LocalDate>): List<LocalDateTime> {
    if (r.mode == ReminderMode.OFF) return emptyList()
    return horizon(now).mapNotNull { date ->
        if (date in withSecond || !r.enabledOn(date)) return@mapNotNull null
        val time = if (r.mode == ReminderMode.FIXED) r.time else randomTimeFor(date, r.windowStart, r.windowEnd)
        time?.let { LocalDateTime(date, it) }?.takeIf { it > now }
    }
}

fun planNotifications(s: AppSettings, now: LocalDateTime, withSecond: Set<LocalDate>): List<PlannedNotif> {
    val kind = if (s.reminder.mode == ReminderMode.RANDOM) NotifKind.RANDOM else NotifKind.REMINDER
    val plan = nextReminderTimes(s.reminder, now, withSecond).map { PlannedNotif(kind, it) }.toMutableList()
    horizon(now).forEach { date ->
        if (s.notif.runningLate && date !in withSecond) {
            plan += PlannedNotif(NotifKind.RUNNING_LATE, LocalDateTime(date, RunningLateTime))
        }
        if (s.notif.onThisDay && date.minus(DatePeriod(months = 1)) in withSecond) {
            plan += PlannedNotif(NotifKind.ON_THIS_DAY, LocalDateTime(date, OnThisDayTime))
        }
    }
    return plan.filter { it.at > now }.sortedBy { it.at }
}

private fun horizon(now: LocalDateTime) = List(HORIZON_DAYS) { now.date.plus(DatePeriod(days = it)) }

data class NotifCopy(val title: String, val body: String)

fun reminderCopy(kind: NotifKind, date: LocalDate): NotifCopy = when (kind) {
    NotifKind.REMINDER -> NotifCopy("Your clip for today", "Whatever's in front of you counts.")
    NotifKind.RANDOM -> NotifCopy("Right now.", "Whatever you're looking at — that's today.")
    NotifKind.RUNNING_LATE -> NotifCopy("Still time for today", "Even the ceiling counts.")
    NotifKind.ON_THIS_DAY -> {
        val then = date.minus(DatePeriod(months = 1))
        NotifCopy("One month ago today", "${then.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${then.day} ${then.month.name.lowercase().replaceFirstChar { it.uppercase() }}. Want to see it?")
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
    data class Film(val id: String) : DeepLinkTarget
    data class Day(val date: LocalDate) : DeepLinkTarget
}

fun DeepLinkTarget.encode(): String = when (this) {
    DeepLinkTarget.Camera -> "camera"
    DeepLinkTarget.Backup -> "backup"
    is DeepLinkTarget.Film -> "film:$id"
    is DeepLinkTarget.Day -> "day:$date"
}

fun parseDeepLink(raw: String?): DeepLinkTarget? = when {
    raw == null -> null
    raw == "camera" -> DeepLinkTarget.Camera
    raw == "backup" -> DeepLinkTarget.Backup
    raw.startsWith("film:") -> DeepLinkTarget.Film(raw.removePrefix("film:"))
    raw.startsWith("day:") -> runCatching { LocalDate.parse(raw.removePrefix("day:")) }.getOrNull()?.let(DeepLinkTarget::Day)
    else -> null
}
