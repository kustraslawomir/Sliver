package app.sliver

import app.sliver.core.time.GoldenState
import app.sliver.core.time.SUNSET_ELEVATION
import app.sliver.core.time.goldenState
import app.sliver.core.time.goldenWindows
import app.sliver.core.time.sunCrossing
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class SolarTest {
    private fun minutesBetween(a: Instant, b: Instant) = abs((a - b).inWholeSeconds) / 60.0

    @Test fun londonSunsetOnTheSolstice() {
        val sunset = sunCrossing(LocalDate(2026, 6, 21), 51.5074, -0.1278, SUNSET_ELEVATION, false)!!
        assertTrue(minutesBetween(sunset, Instant.parse("2026-06-21T20:21:00Z")) <= 3, sunset.toString())
    }

    @Test fun londonSunriseOnTheSolstice() {
        val sunrise = sunCrossing(LocalDate(2026, 6, 21), 51.5074, -0.1278, SUNSET_ELEVATION, true)!!
        assertTrue(minutesBetween(sunrise, Instant.parse("2026-06-21T03:43:00Z")) <= 3, sunrise.toString())
    }

    @Test fun equatorSunsetAtTheEquinoxIsAboutSixPmSolarTime() {
        val sunset = sunCrossing(LocalDate(2026, 3, 20), 0.0, 0.0, SUNSET_ELEVATION, false)!!
        assertTrue(minutesBetween(sunset, Instant.parse("2026-03-20T18:08:00Z")) <= 4, sunset.toString())
    }

    @Test fun sunsetAtTheOtherSideOfTheWorldIsOnTheSameLocalDay() {
        val sunset = sunCrossing(LocalDate(2026, 3, 20), -41.29, 174.78, SUNSET_ELEVATION, false)!! // Wellington
        // Equinox sunset is ~18:10 mean solar time at 174.78E (UTC+11:39), so ~06:31 UTC on the same local day.
        assertTrue(minutesBetween(sunset, Instant.parse("2026-03-20T06:31:00Z")) <= 8, sunset.toString())
    }

    @Test fun eveningGoldenHourEndsAtDuskSideAndStartsAboutAnHourBeforeSunset() {
        val (_, evening) = goldenWindows(LocalDate(2026, 6, 21), 51.5074, -0.1278)
        val sunset = Instant.parse("2026-06-21T20:21:00Z")
        val lead = (sunset - evening.start).inWholeMinutes
        assertTrue(lead in 40..90, "lead $lead")
        assertTrue(evening.end > sunset)
    }

    @Test fun polarNightHasNoCrossing() {
        assertNull(sunCrossing(LocalDate(2026, 12, 21), 80.0, 0.0, SUNSET_ELEVATION, false))
    }

    @Test fun stateIsSoonWithinThreeHoursNowInsideAndNullOtherwise() {
        val date = LocalDate(2026, 6, 21)
        val (_, evening) = goldenWindows(date, 51.5074, -0.1278)
        val soon = goldenState(evening.start - kotlin.time.Duration.parse("38m"), date, 51.5074, -0.1278)
        assertTrue(soon is GoldenState.Soon && soon.minutes in 37..38, soon.toString())
        val now = goldenState(evening.start + kotlin.time.Duration.parse("10m"), date, 51.5074, -0.1278)
        assertTrue(now is GoldenState.Now && now.minutesLeft > 10, now.toString())
        assertEquals(null, goldenState(Instant.parse("2026-06-21T11:00:00Z"), date, 51.5074, -0.1278))
    }
}
