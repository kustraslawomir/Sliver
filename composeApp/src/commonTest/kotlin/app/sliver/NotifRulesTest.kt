package app.sliver

import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.NotifSettings
import app.sliver.data.settings.ReminderSettings
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.NotifKind
import app.sliver.feature.notifications.encode
import app.sliver.feature.notifications.epochMillisIn
import app.sliver.feature.notifications.nextFire
import app.sliver.feature.notifications.parseDeepLink
import app.sliver.feature.notifications.planNotifications
import app.sliver.feature.notifications.reminderCopy
import app.sliver.feature.notifications.shouldPost
import app.sliver.feature.notifications.timeOn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

class NotifRulesTest {
    // 2026-03-02 is a Monday
    private val monday = LocalDate(2026, 3, 2)
    private val morning = LocalDateTime(monday, LocalTime(8, 0))
    private val on = ReminderSettings(enabled = true)

    @Test fun nextFireIsTodayWhenTimeIsAhead() {
        assertEquals(LocalDateTime(monday, LocalTime(20, 0)), nextFire(on, NotifKind.REMINDER, morning))
    }

    @Test fun nextFireRollsToTomorrowAfterTheTime() {
        val late = LocalDateTime(monday, LocalTime(20, 11))
        assertEquals(LocalDateTime(LocalDate(2026, 3, 3), LocalTime(20, 0)), nextFire(on, NotifKind.REMINDER, late))
    }

    @Test fun daysMaskSkipsDisabledDays() {
        val r = on.copy(days = listOf(false, false, true, false, false, false, false))
        assertEquals(LocalDate(2026, 3, 4), nextFire(r, NotifKind.REMINDER, morning)?.date)
        val after = LocalDateTime(LocalDate(2026, 3, 4), LocalTime(21, 0))
        assertEquals(LocalDate(2026, 3, 11), nextFire(r, NotifKind.REMINDER, after)?.date)
    }

    @Test fun aReminderThatIsMerelyLateSurvivesAReplanInsideItsWindow() {
        val inWindow = LocalDateTime(monday, LocalTime(20, 5))
        assertEquals(LocalDateTime(monday, LocalTime(20, 0)), nextFire(on, NotifKind.REMINDER, inWindow))
        assertEquals(LocalDate(2026, 3, 3), nextFire(on, NotifKind.REMINDER, inWindow, handledToday = setOf(NotifKind.REMINDER))?.date)
        assertEquals(LocalDate(2026, 3, 3), nextFire(on, NotifKind.REMINDER, LocalDateTime(monday, LocalTime(20, 11)))?.date)
    }

    @Test fun disabledOrNoDaysYieldsNothing() {
        assertNull(nextFire(on.copy(enabled = false), NotifKind.REMINDER, morning))
        assertNull(nextFire(on.copy(days = List(7) { false }), NotifKind.REMINDER, morning))
    }

    @Test fun sameLocalTimeIsDifferentInstantInAnotherZone() {
        val at = LocalDateTime(monday, LocalTime(20, 0))
        val berlin = at.epochMillisIn(TimeZone.of("Europe/Berlin"))
        val tokyo = at.epochMillisIn(TimeZone.of("Asia/Tokyo"))
        assertEquals(8 * 3_600_000L, berlin - tokyo)
    }

    @Test fun lastCallOnlyWhenEnabledAndBeforeItsTime() {
        assertNull(nextFire(on, NotifKind.LAST_CALL, morning))
        val r = on.copy(lastCall = true)
        assertEquals(LocalDateTime(monday, LocalTime(22, 30)), nextFire(r, NotifKind.LAST_CALL, morning))
        val lateMain = r.copy(time = LocalTime(23, 0))
        assertNull(nextFire(lateMain, NotifKind.LAST_CALL, morning))
    }

    @Test fun planHasOneReminderAndOneLastCall() {
        val s = AppSettings(reminder = on.copy(lastCall = true), notif = NotifSettings(onThisDay = false))
        assertEquals(listOf(NotifKind.REMINDER, NotifKind.LAST_CALL), planNotifications(s, morning, emptySet()).map { it.kind })
    }

    @Test fun goldenHourMovesTheTimeTwentyMinutesBefore() {
        val r = on.copy(followGoldenHour = true)
        assertEquals(LocalTime(15, 4), r.timeOn(monday) { LocalTime(15, 24) })
        assertEquals(LocalTime(20, 0), r.timeOn(monday) { null })
        assertEquals(LocalTime(20, 0), on.timeOn(monday) { LocalTime(15, 24) })
    }

    @Test fun skipIfDoneSuppressesTheReminderOnly() {
        assertFalse(shouldPost(NotifKind.REMINDER, on, todayHasClip = true, posted = emptySet()))
        assertTrue(shouldPost(NotifKind.REMINDER, on.copy(skipIfDone = false), todayHasClip = true, posted = emptySet()))
        assertTrue(shouldPost(NotifKind.REMINDER, on, todayHasClip = false, posted = emptySet()))
    }

    @Test fun lastCallNeverPostsWhenTheDayHasAClip() {
        val r = on.copy(lastCall = true, skipIfDone = false)
        assertFalse(shouldPost(NotifKind.LAST_CALL, r, todayHasClip = true, posted = emptySet()))
        assertTrue(shouldPost(NotifKind.LAST_CALL, r, todayHasClip = false, posted = setOf(NotifKind.REMINDER)))
        assertFalse(shouldPost(NotifKind.LAST_CALL, on, todayHasClip = false, posted = emptySet()))
    }

    @Test fun neverMoreThanTwoAndEachKindOnce() {
        val r = on.copy(lastCall = true)
        assertFalse(shouldPost(NotifKind.REMINDER, r, false, setOf(NotifKind.REMINDER)))
        assertFalse(shouldPost(NotifKind.LAST_CALL, r, false, setOf(NotifKind.REMINDER, NotifKind.LAST_CALL)))
    }

    @Test fun disabledPostsNothing() {
        assertFalse(shouldPost(NotifKind.REMINDER, on.copy(enabled = false), false, emptySet()))
    }

    @Test fun onThisDayFiresWhenAClipExistedAMonthAgo() {
        val plan = planNotifications(AppSettings(), morning, setOf(LocalDate(2026, 2, 4)))
        assertEquals(listOf(LocalDate(2026, 3, 4)), plan.filter { it.kind == NotifKind.ON_THIS_DAY }.map { it.at.date })
    }

    @Test fun reminderCopyNamesTheWeekday() {
        assertEquals("Today's clip", reminderCopy(NotifKind.REMINDER, monday).title)
        assertEquals("Monday is still empty. A few seconds is enough.", reminderCopy(NotifKind.REMINDER, monday).body)
    }

    @Test fun deepLinksRoundTrip() {
        listOf(DeepLinkTarget.Camera, DeepLinkTarget.Backup, DeepLinkTarget.FillThePast, DeepLinkTarget.Project, DeepLinkTarget.Gallery, DeepLinkTarget.MakeFilm, DeepLinkTarget.Film("f1"), DeepLinkTarget.Day(monday)).forEach {
            assertEquals(it, parseDeepLink(it.encode()))
        }
        assertNull(parseDeepLink("day:nope"))
    }
}
