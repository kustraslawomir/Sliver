package app.sliver

import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.NotifSettings
import app.sliver.data.settings.ReminderMode
import app.sliver.data.settings.ReminderSettings
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.NotifKind
import app.sliver.feature.notifications.encode
import app.sliver.feature.notifications.nextReminderTimes
import app.sliver.feature.notifications.parseDeepLink
import app.sliver.feature.notifications.planNotifications
import app.sliver.feature.notifications.randomTimeFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class NotifRulesTest {
    // 2026-03-02 is a Monday
    private val monday = LocalDate(2026, 3, 2)
    private val morning = LocalDateTime(monday, LocalTime(8, 0))

    @Test fun fixedFiresEveryEnabledDayAtTime() {
        val r = ReminderSettings(ReminderMode.FIXED, time = LocalTime(19, 30), days = listOf(true, false, true, false, false, false, false))
        val times = nextReminderTimes(r, morning, emptySet())
        assertEquals(listOf(2, 4), times.map { it.date.day })
        assertTrue(times.all { it.time == LocalTime(19, 30) })
    }

    @Test fun fixedSkipsPastTodayAndDaysWithASecond() {
        val r = ReminderSettings(ReminderMode.FIXED, time = LocalTime(9, 0))
        val after = LocalDateTime(monday, LocalTime(9, 30))
        val times = nextReminderTimes(r, after, setOf(LocalDate(2026, 3, 3)))
        assertEquals(listOf(4, 5, 6, 7, 8), times.map { it.date.day })
    }

    @Test fun randomIsOnePerDayInsideWindowAndStable() {
        val r = ReminderSettings(ReminderMode.RANDOM, windowStart = LocalTime(10, 0), windowEnd = LocalTime(20, 0))
        val a = nextReminderTimes(r, morning, emptySet())
        assertEquals(7, a.size)
        assertEquals(7, a.map { it.date }.toSet().size)
        assertTrue(a.all { it.time >= LocalTime(10, 0) && it.time <= LocalTime(20, 0) })
        assertEquals(a, nextReminderTimes(r, morning, emptySet()))
    }

    @Test fun randomWithEmptyWindowYieldsNothing() {
        assertNull(randomTimeFor(monday, LocalTime(20, 0), LocalTime(10, 0)))
    }

    @Test fun offYieldsNothing() {
        assertTrue(nextReminderTimes(ReminderSettings(ReminderMode.OFF), morning, emptySet()).isEmpty())
    }

    @Test fun savingTodayCancelsReminderAndRunningLateForToday() {
        val s = AppSettings(notif = NotifSettings(runningLate = true, onThisDay = false))
        val before = planNotifications(s, morning, emptySet()).filter { it.at.date == monday }
        assertEquals(setOf(NotifKind.REMINDER, NotifKind.RUNNING_LATE), before.map { it.kind }.toSet())
        val after = planNotifications(s, morning, setOf(monday)).filter { it.at.date == monday }
        assertTrue(after.isEmpty())
    }

    @Test fun runningLateOffByDefault() {
        val plan = planNotifications(AppSettings(), morning, emptySet())
        assertTrue(plan.none { it.kind == NotifKind.RUNNING_LATE })
    }

    @Test fun onThisDayFiresWhenSecondExistedAMonthAgo() {
        val plan = planNotifications(AppSettings(reminder = ReminderSettings(ReminderMode.OFF)), morning, setOf(LocalDate(2026, 2, 4)))
        assertEquals(listOf(LocalDate(2026, 3, 4)), plan.filter { it.kind == NotifKind.ON_THIS_DAY }.map { it.at.date })
    }

    @Test fun deepLinksRoundTrip() {
        listOf(DeepLinkTarget.Camera, DeepLinkTarget.Backup, DeepLinkTarget.Film("f1"), DeepLinkTarget.Day(monday)).forEach {
            assertEquals(it, parseDeepLink(it.encode()))
        }
        assertNull(parseDeepLink("day:nope"))
    }
}
