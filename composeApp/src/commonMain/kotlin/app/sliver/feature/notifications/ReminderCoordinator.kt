package app.sliver.feature.notifications

import app.sliver.data.repo.DayRepository
import app.sliver.data.settings.SettingsRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import app.sliver.data.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Replaces every pending local notification with [plan]. */
interface ReminderScheduler {
    fun schedule(plan: List<PlannedNotif>)
}

/** Tap target set by a notification, consumed by the nav host. */
object DeepLink {
    val target = MutableStateFlow<DeepLinkTarget?>(null)
}

/** Re-plans on app open, settings change and whenever the set of captured days changes (saving today cancels today's nudges). */
class ReminderCoordinator(
    private val settings: SettingsRepository,
    private val days: DayRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend fun replanOnce() {
        val withSecond = days.observeAll().first().map { it.date }.toSet()
        plan(settings.settings.first(), withSecond)
    }

    private fun plan(s: AppSettings, withSecond: Set<LocalDate>) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        scheduler.schedule(planNotifications(s, now, withSecond))
    }

    fun start(scope: CoroutineScope) {
        val captured = days.observeAll().map { list -> list.map { it.date }.toSet() }.distinctUntilChanged()
        val relevant = settings.settings.distinctUntilChangedBy { it.reminder to it.notif }
        scope.launch {
            combine(relevant, captured) { s, withSecond -> s to withSecond }.collect { (s, withSecond) -> plan(s, withSecond) }
        }
    }
}
