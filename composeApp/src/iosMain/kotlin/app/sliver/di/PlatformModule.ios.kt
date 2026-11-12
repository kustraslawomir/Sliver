package app.sliver.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.sliver.data.auth.FakeGoogleAuth
import app.sliver.data.auth.GoogleAuth
import app.sliver.data.backup.ArchiveExporter
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.NetworkMonitor
import app.sliver.data.backup.RestoreScheduler
import app.sliver.data.backup.RestoredDayWriter
import app.sliver.data.db.Migration2To3
import app.sliver.data.db.Migration3To4
import app.sliver.data.db.Migration4To5
import app.sliver.data.light.ClipLightReader
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.ClipExporter
import app.sliver.data.files.FileStore
import app.sliver.data.files.IosFileStore
import app.sliver.data.files.documentsDir
import app.sliver.data.files.readFileBytes
import app.sliver.data.files.writeFileBytes
import app.sliver.data.repo.PlaceResolver
import app.sliver.feature.backfill.BackfillRunner
import app.sliver.feature.films.FilmRenderer
import app.sliver.feature.films.FilmShare
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.notifications.GoldenHourProvider
import app.sliver.feature.notifications.NoGoldenHour
import app.sliver.feature.notifications.ReminderScheduler
import app.sliver.feature.trim.TrimMedia
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlaceResolver> { PlaceResolver { null } }
    single {
        Room.databaseBuilder<SliverDatabase>(name = "${documentsDir()}/sliver.db")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(Migration2To3, Migration3To4, Migration4To5)
            .build()
    }
    single<FileStore> { IosFileStore() }
    single<ClipExporter> { IosClipExporter }
    single<TrimMedia> { IosTrimMedia }
    single<BackfillRunner> { IosBackfillRunner }
    single<FilmRenderer> { IosFilmRenderer }
    single<app.sliver.feature.notifications.ReminderLog> { IosReminderLog }
    single<ClipLightReader> { ClipLightReader { null } }
    single<GoldenHourProvider> { NoGoldenHour }
    single<ReminderScheduler> { IosReminderScheduler }
    // DECISION: Google sign-in on iOS needs its own client id; fake auth + in-memory Drive until then.
    single<GoogleAuth> { FakeGoogleAuth() }
    single<DayFileReader> { DayFileReader(::readFileBytes) }
    // DECISION: no thumbnail extraction yet; returning 0 means "no colour", like the Android failure path.
    single<RestoredDayWriter> { RestoredDayWriter { bytes, videoPath, _ -> writeFileBytes(videoPath, bytes); 0 } }
    single<RestoreScheduler> { IosJobs }
    single<BackupScheduler> { IosJobs }
    single<NetworkMonitor> { IosNetworkMonitor }
    single<ArchiveExporter> { IosArchiveExporter }
    single<FilmShare> { IosFilmShare }
    single<HttpClientEngine> { Darwin.create() }
    single<GalleryProvider> { IosGalleryProvider }
    single { PreferenceDataStoreFactory.createWithPath { "${documentsDir()}/settings.preferences_pb".toPath() } }
}
