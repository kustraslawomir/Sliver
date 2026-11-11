package app.sliver.feature.backfill

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.sliver.core.log.logDebug
import app.sliver.data.DaySource
import app.sliver.data.toParams
import app.sliver.data.files.FileStore
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.SaveSecondRequest
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.trim.TrimMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json

/** Runs [BackfillExecutor.run] in the background, surviving app close. */
interface BackfillRunner {
    fun start()
    fun cancel()
}

/** The single backfill batch, persisted so the work resumes and Undo all can find it later. */
class BackfillStore(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey("backfillBatch")

    val batch: Flow<BackfillBatch?> = store.data.map { it[key]?.let(::decode) }

    suspend fun current(): BackfillBatch? = batch.first()

    suspend fun start(items: List<BackfillItem>) {
        store.edit { it[key] = Json.encodeToString(BackfillBatch.serializer(), BackfillBatch(items)) }
    }

    /** False when the batch was undone or cleared meanwhile. */
    suspend fun markProcessed(date: String): Boolean {
        var alive = false
        store.edit { prefs ->
            val batch = prefs[key]?.let(::decode) ?: return@edit
            if (batch.items.none { it.date == date }) return@edit
            alive = true
            prefs[key] = Json.encodeToString(BackfillBatch.serializer(), batch.copy(processed = batch.processed + date))
        }
        return alive
    }

    suspend fun clear() {
        store.edit { it.remove(key) }
    }

    private fun decode(raw: String) = runCatching { Json.decodeFromString(BackfillBatch.serializer(), raw) }.getOrNull()
}

class BackfillExecutor(
    private val store: BackfillStore,
    private val days: DayRepository,
    private val gallery: GalleryProvider,
    private val media: TrimMedia,
    private val settings: SettingsRepository,
    private val files: FileStore,
) {
    /** Fills every unprocessed item; resumable and never touches a day that already has a second. */
    suspend fun run() {
        val appSettings = settings.settings.first()
        val items = store.current()?.items ?: return
        for (item in items) {
            val batch = store.current() ?: return
            if (item.date in batch.processed) continue
            val date = LocalDate.parse(item.date)
            val saved = days.getDay(date) == null && fill(item, date, appSettings)
            if (!store.markProcessed(item.date) && saved) removeDay(date)
        }
    }

    /** Deletes exactly the days this batch created (source BACKFILL on a batch date) and forgets the batch. */
    suspend fun undoAll() {
        val batch = store.current() ?: return
        store.clear()
        batch.items.forEach { item ->
            val date = LocalDate.parse(item.date)
            if (days.getDay(date)?.source == DaySource.BACKFILL) removeDay(date)
        }
    }

    private suspend fun removeDay(date: LocalDate) {
        days.getDay(date)?.let {
            files.delete(it.file)
            files.delete(it.thumb)
        }
        days.delete(date)
    }

    private suspend fun fill(item: BackfillItem, date: LocalDate, appSettings: app.sliver.data.settings.AppSettings): Boolean {
        var tmp: String? = null
        return try {
            val path = if ("://" in item.uri) gallery.copyToTmp(item.uri) else item.uri
            tmp = path
            val start = if (item.video) media.autoPickMs(path, media.durationMs(path)) else 0L
            days.saveSecond(
                SaveSecondRequest(
                    date = date, source = path, startMs = start, speed = 1f, cropY = null,
                    format = appSettings.newFilmFormat, daySource = DaySource.BACKFILL,
                    takenAt = LocalDateTime(date, LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault()),
                    look = appSettings.defaultLook.toParams(), isPhoto = !item.video,
                ),
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logDebug("Backfill", "fill ${item.date} failed: $e")
            false
        } finally {
            tmp?.let(files::delete)
        }
    }
}
