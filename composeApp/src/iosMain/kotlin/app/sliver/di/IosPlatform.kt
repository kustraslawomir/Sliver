package app.sliver.di

import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.export.ShortMode
import app.sliver.data.backup.ArchiveExporter
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.NetworkKind
import app.sliver.data.backup.NetworkMonitor
import app.sliver.data.backup.RestoreScheduler
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.ExportedClip
import app.sliver.feature.backfill.BackfillRunner
import app.sliver.feature.films.FilmRenderer
import app.sliver.feature.films.FilmShare
import app.sliver.feature.films.RenderState
import app.sliver.feature.gallery.GalleryItem
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.gallery.MotionClip
import app.sliver.feature.notifications.PlannedNotif
import app.sliver.feature.notifications.ReminderScheduler
import app.sliver.feature.trim.TrimMedia
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// TODO iOS later: every object here is a placeholder until the matching AVFoundation / BGTask / UNNotification work lands.
private fun notYet(what: String): Nothing = throw UnsupportedOperationException("$what not on iOS yet")

internal object IosClipExporter : ClipExporter {
    override suspend fun exportClip(
        source: String, startMs: Long, speed: Float, cropY: Float?, format: FilmFormat,
        videoPath: String, thumbPath: String, shortMode: ShortMode, look: LookParams,
    ): ExportedClip = notYet("Clip export")

    override suspend fun photoToClip(
        source: String, motion: Motion, format: FilmFormat, videoPath: String, thumbPath: String, look: LookParams,
    ): ExportedClip = notYet("Photo clip")
}

internal object IosTrimMedia : TrimMedia {
    override suspend fun durationMs(path: String) = 0L
    override suspend fun aspect(path: String) = 1f
    override suspend fun autoPickMs(path: String, durationMs: Long) = 0L
    override suspend fun fileSizeBytes(path: String) = 0L
    override suspend fun thumbnails(path: String, durationMs: Long, count: Int, onEach: (ImageBitmap) -> Unit) =
        emptyList<ImageBitmap>()
}

internal object IosBackfillRunner : BackfillRunner {
    override fun start() = Unit
    override fun cancel() = Unit
}

internal object IosFilmRenderer : FilmRenderer {
    override fun start(filmId: String) = Unit
    override fun cancel(filmId: String) = Unit
    override fun state(filmId: String): Flow<RenderState> = flowOf(RenderState.Idle)
}

internal object IosReminderScheduler : ReminderScheduler {
    override fun schedule(plan: List<PlannedNotif>) = Unit
}

internal object IosJobs : BackupScheduler, RestoreScheduler {
    override fun schedule(wifiOnly: Boolean, anyNetwork: Boolean) = Unit
    override fun cancel() = Unit
    override fun start() = Unit
}

// DECISION: report Wi-Fi until NWPathMonitor is wired; backup runs against the in-memory Drive client anyway.
internal object IosNetworkMonitor : NetworkMonitor {
    override val network: Flow<NetworkKind> = flowOf(NetworkKind.UNMETERED)
}

internal object IosArchiveExporter : ArchiveExporter {
    override suspend fun exportAll(destination: String, onProgress: (done: Int, total: Int) -> Unit) =
        Result.failure<Unit>(UnsupportedOperationException("Zip export not on iOS yet"))
}

internal object IosFilmShare : FilmShare {
    override fun share(path: String, name: String) = Unit
    override suspend fun saveToPhotos(path: String, name: String) = false
}

internal object IosGalleryProvider : GalleryProvider {
    override suspend fun isVideo(uri: String) = false
    override suspend fun describe(uris: List<String>) = emptyList<GalleryItem>()
    override suspend fun copyToTmp(uri: String): String = notYet("Gallery import")
    override suspend fun keep(uri: String): String = notYet("Gallery import")
    override suspend fun extractMotionVideo(uri: String): MotionClip? = null
}
