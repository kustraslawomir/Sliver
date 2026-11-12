package app.sliver.shots

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.SliverColors
import app.sliver.core.ui.LocalWindowSize
import app.sliver.core.ui.WindowSize
import androidx.compose.ui.graphics.asImageBitmap
import app.sliver.feature.trim.ThumbCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.core.theme.SliverTheme
import app.sliver.core.time.AppClock
import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.FilmFormat
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Project
import app.sliver.data.db.SliverDatabase
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.hours

/** Same date as the design boards: Tue 10 Nov 2026. */
val ShotToday = LocalDate(2026, 11, 10)
const val ShotProject = "My 2026"
const val ShotClips = 129
const val ShotProjectId = "shot-main"
val ShotZone: TimeZone = TimeZone.UTC

/** Shared base for screen goldens: real Koin graph over an in-memory Room database filled with synthetic data. */
abstract class ShotTest {
    @get:Rule val rule = createComposeRule()
    protected lateinit var ctx: Context
    protected lateinit var db: SliverDatabase
    protected lateinit var settings: SettingsRepository
    private val thumbs = mutableListOf<String>()

    @Before
    fun startGraph() {
        AppClock.override = object : Clock {
            override fun now(): Instant = ShotToday.atStartOfDayIn(ShotZone) + 18.hours
        }
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        ctx = ApplicationProvider.getApplicationContext()
        org.robolectric.Shadows.shadowOf(ctx as Application).grantPermissions(
            android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS,
        )
        db = Room.inMemoryDatabaseBuilder<SliverDatabase>(ctx).allowMainThreadQueries().build()
        settings = DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
                File.createTempFile("prefs", ".preferences_pb").apply { delete() }
            },
        )
        stopKoin()
        startKoin {
            androidContext(ctx as Application)
            modules(
                commonModule, platformModule,
                module {
                    single { db }
                    single<app.sliver.feature.films.FilmRenderer> {
                        object : app.sliver.feature.films.FilmRenderer {
                            override fun start(filmId: String) {}
                            override fun cancel(filmId: String) {}
                            override fun state(filmId: String) = kotlinx.coroutines.flow.flowOf(app.sliver.feature.films.RenderState.Idle)
                        }
                    }
                    single { settings }
                    single<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>> {
                        PreferenceDataStoreFactory.create { File.createTempFile("prefs2", ".preferences_pb").apply { delete() } }
                    }
                },
            )
        }
        thumbs += (0 until Variants).map { writeThumb(it) }
        runBlocking { seed() }
    }

    @After
    fun stopGraph() {
        stopKoin()
        db.close()
        AppClock.override = null
        SliverColors.forceDark = false
    }

    protected open suspend fun seed() {
        seedMain()
        seedSecondProject()
        settings.update { it.copy(onboardingDone = true, activeProjectId = ShotProjectId) }
    }

    protected suspend fun seedMain() {
        db.projectDao().upsert(
            Project(ShotProjectId, ShotProject, FilmFormat.CINEMA, ShotToday.minus(DatePeriod(days = 142)), createdAt = at(ShotToday)),
        )
        val dates = (142 downTo 1).map { ShotToday.minus(DatePeriod(days = it)) }
            .filterIndexed { i, _ -> i % 11 != 2 }
        check(dates.size == ShotClips) { "seed has ${dates.size} clips" }
        dates.forEachIndexed { i, d -> db.dayDao().upsert(day(ShotProjectId, d, i, FilmFormat.CINEMA)) }
        db.filmDao().upsert(
            Film(
                id = "shot-film", name = "My 2026", format = FilmFormat.CINEMA,
                projectId = ShotProjectId,
            ),
        )
    }

    protected suspend fun seedSecondProject() {
        db.projectDao().upsert(
            Project("shot-second", "Lisbon trip", FilmFormat.STORY, ShotToday.minus(DatePeriod(days = 9)), createdAt = at(ShotToday)),
        )
        (9 downTo 1).forEachIndexed { i, back ->
            db.dayDao().upsert(day("shot-second", ShotToday.minus(DatePeriod(days = back)), i, FilmFormat.STORY))
        }
    }

    private fun at(d: LocalDate) = d.atStartOfDayIn(ShotZone) + 12.hours

    protected fun day(project: String, date: LocalDate, i: Int, format: FilmFormat): Day {
        val path = thumbs[(i * 5 + i / 7) % Variants]
        return Day(
            date = date, file = path, thumb = path, source = DaySource.CAMERA, takenAt = at(date), createdAt = at(date),
            backup = BackupState.DONE, projectId = project, luma = 0.3f + (i % 5) * 0.1f, tone = 0xFF000000.toInt() or (i * 40503),
        )
    }

    protected fun thumbPath(variant: Int) = thumbs[variant]

    private fun writeThumb(variant: Int): String {
        val hues = floatArrayOf(25f, 200f, 140f, 280f, 50f, 340f)
        val w = 90; val h = 160
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val top = Color.HSVToColor(floatArrayOf(hues[variant], 0.45f, 0.55f))
        val bottom = Color.HSVToColor(floatArrayOf(hues[variant], 0.6f, 0.2f))
        Canvas(bmp).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP) })
        val f = File(ctx.cacheDir, "shot-thumb-$variant-${System.nanoTime()}.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Robolectric's native ImageDecoder aborts on the IO thread, so the screens find the thumbnails already cached.
        ThumbCache.put(ThumbCache.keyFor(f.absolutePath), listOf(bmp.asImageBitmap()))
        return f.absolutePath
    }

    /** Renders [content] once in the dark theme and once in the light theme and records both goldens. */
    protected fun shoot(name: String, darkOnly: Boolean = false, inspection: Boolean = false, settled: String? = null, content: @Composable () -> Unit) {
        var light by mutableStateOf(false)
        SliverColors.forceDark = darkOnly
        rule.setContent {
            val nav = androidx.navigation.compose.rememberNavController()
            SliverTheme(light = light) {
                CompositionLocalProvider(
                    app.sliver.core.navigation.LocalNavigator provides
                        androidx.compose.runtime.remember { app.sliver.core.navigation.AppNavigator(nav) },
                    LocalWindowSize provides WindowSize(390.dp, 844.dp),
                    androidx.compose.ui.platform.LocalInspectionMode provides inspection,
                ) { content() }
            }
        }
        rule.waitForIdle()
        settled?.let { text -> rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
        capture(name, "dark")
        if (darkOnly) return
        light = true
        rule.waitForIdle()
        capture(name, "light")
    }

    private fun capture(name: String, theme: String) {
        // Room and DataStore emit on IO threads the compose clock cannot see: give them real time to land.
        repeat(4) {
            Thread.sleep(250)
            rule.mainClock.advanceTimeBy(250)
            rule.waitForIdle()
        }
        rule.onRoot().captureRoboImage("src/test/snapshots/$name-$theme.png", roborazziOptions = ShotOptions)
    }

    companion object {
        const val Variants = 6
        val ShotOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.001f))
    }
}
