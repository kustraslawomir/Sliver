package app.sliver.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.SliverDatabase
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.data.settings.ReminderMode
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class DataLayerTest {
    private lateinit var db: SliverDatabase
    private lateinit var days: DayRepositoryImpl
    private lateinit var films: FilmRepositoryImpl

    @BeforeTest
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        days = DayRepositoryImpl(db.dayDao())
        films = FilmRepositoryImpl(db.filmDao())
    }

    @AfterTest
    fun tearDown() = db.close()

    private fun day(date: String, file: String = "days/$date.mp4", look: LookParams = LookParams()) = Day(
        date = LocalDate.parse(date), file = file, thumb = "thumbs/$date.jpg", source = DaySource.CAMERA,
        look = look, takenAt = Instant.fromEpochMilliseconds(1000), createdAt = Instant.fromEpochMilliseconds(2000),
    )

    @Test
    fun insertReplaceDelete() = runTest {
        val d = day("2026-11-10", look = LookParams(LookPreset.NOIR, grain = 40))
        days.save(d)
        assertEquals(d, days.getDay(d.date))
        assertEquals(1, days.observeCount().first())

        days.save(d.copy(file = "days/new.mp4"))
        assertEquals("days/new.mp4", days.observeDay(d.date).first()?.file)
        assertEquals(1, days.observeCount().first())

        days.delete(d.date)
        assertNull(days.getDay(d.date))
        assertEquals(0, days.observeCount().first())
    }

    @Test
    fun rangeQueryInclusiveAndOrdered() = runTest {
        listOf("2026-11-12", "2026-11-10", "2026-11-11", "2026-11-20").forEach { days.save(day(it)) }
        val r = days.observeRange(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-12")).first()
        assertEquals(listOf("2026-11-10", "2026-11-11", "2026-11-12"), r.map { it.date.toString() })
    }

    @Test
    fun filmCrud() = runTest {
        val f = Film(
            id = "a", name = "Week", rangeType = FilmRangeType.CUSTOM,
            rangeFrom = LocalDate.parse("2026-11-01"), rangeTo = LocalDate.parse("2026-11-07"), look = LookPreset.EMBER,
        )
        films.save(f)
        assertEquals(f, films.getFilm("a"))
        films.save(f.copy(name = "Renamed"))
        assertEquals(listOf("Renamed"), films.observeAll().first().map { it.name })
        films.delete("a")
        assertEquals(0, films.observeCount().first())
    }

    @Test
    fun settingsDefaultsAndRoundTrip() = runTest {
        val file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        val repo = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        val s = repo.settings.first()
        assertEquals(LookPreset.NATURAL, s.defaultLook)
        assertEquals(FilmFormat.CINEMA, s.newFilmFormat)
        assertEquals(true, s.dateStamp)
        assertEquals(false, s.saveFullRecordings)
        assertEquals(LocalTime(19, 30), s.reminder.time)
        assertEquals(List(7) { true }, s.reminder.days)
        assertEquals(true, s.notif.filmReady)
        assertEquals(false, s.notif.runningLate)
        assertEquals(true, s.account.wifiOnly)
        assertNull(s.firstDay)

        repo.update {
            it.copy(
                firstDay = LocalDate.parse("2026-01-01"),
                reminder = it.reminder.copy(mode = ReminderMode.RANDOM, days = List(7) { i -> i < 5 }),
            )
        }
        val u = repo.settings.first()
        assertEquals(ReminderMode.RANDOM, u.reminder.mode)
        assertEquals(List(7) { i -> i < 5 }, u.reminder.days)
        assertEquals(LocalDate.parse("2026-01-01"), u.firstDay)
    }
}
