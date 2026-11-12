package app.sliver.feature.notifications

import app.sliver.data.db.Day
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.ReminderSettings
import app.sliver.data.settings.SettingsRepository
import app.sliver.core.time.goldenWindows
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Replaces every pending local notification with [plan]. */
interface ReminderScheduler {
    fun schedule(plan: List<PlannedNotif>)
}

/** Evening golden hour start for [date] at the last known coarse location; null when unknown. */
interface GoldenHourProvider {
    /** Last known coarse (latitude, longitude); null when never granted, revoked or never fixed. */
    fun location(): Pair<Double, Double>?

    /** Re-reads the system's last known location into the cache; no-op without permission. */
    fun refresh() {}

    fun eveningGoldenHour(date: LocalDate): LocalTime? {
        val (lat, lon) = location() ?: return null
        val start = goldenWindows(date, lat, lon).getOrNull(1)?.start ?: return null
        return start.toLocalDateTime(TimeZone.currentSystemDefault()).time
    }
}

object NoGoldenHour : GoldenHourProvider {
    override fun location(): Pair<Double, Double>? = null
}

/** Tap target set by a notification, consumed by the nav host. */
object DeepLink {
    val target = MutableStateFlow<DeepLinkTarget?>(null)
}

/** Re-plans on app open, boot, time-zone change, settings change and whenever the set of captured days changes. */
class ReminderCoordinator(
    private val settings: SettingsRepository,
    private val days: DayRepository,
    private val projects: ProjectRepository,
    private val scheduler: ReminderScheduler,
    private val log: ReminderLog,
    private val golden: GoldenHourProvider = NoGoldenHour,
) {
    suspend fun replanOnce() {
        plan(settings.settings.first(), days.observeAllProjects().first())
    }

    private fun plan(s: AppSettings, all: List<Day>) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        scheduler.schedule(planNotifications(s, now, all.map { it.date }.toSet(), golden::eveningGoldenHour, log.handled(now.date)))
    }

    /** Whether today has a clip in the projects the reminder watches (every open project when none are picked). */
    suspend fun todayHasClip(r: ReminderSettings, today: LocalDate): Boolean {
        val open = projects.projects.first().filter { !it.archived }.map { it.id }
        val watched = if (r.projects.isEmpty()) open.toSet() else r.projects.intersect(open.toSet())
        return days.observeAllProjects().first().any { it.date == today && it.projectId in watched }
    }

    /** Yesterday's clip in a watched project, for the notification's large icon. */
    suspend fun yesterdayThumb(r: ReminderSettings, today: LocalDate): String? {
        val open = projects.projects.first().filter { !it.archived }.map { it.id }.toSet()
        val watched = if (r.projects.isEmpty()) open else r.projects.intersect(open)
        val yesterday = today.minus(DatePeriod(days = 1))
        return days.observeAllProjects().first().firstOrNull { it.date == yesterday && it.projectId in watched }?.thumb
    }

    fun start(scope: CoroutineScope) {
        val captured = days.observeAllProjects().map { list -> list.map { it.date }.toSet() }.distinctUntilChanged()
        val relevant = settings.settings.distinctUntilChangedBy { Triple(it.reminder, it.notif.onThisDay, it.showGoldenHour) }
        scope.launch {
            combine(relevant, captured) { s, _ -> s }.collect { s -> plan(s, days.observeAllProjects().first()) }
        }
    }
}
