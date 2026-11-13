package app.sliver.di

import app.sliver.feature.notifications.ProgressCoordinator
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.sliver.data.db.Migration2To3
import app.sliver.data.db.Migration3To4
import app.sliver.data.db.Migration4To5
import app.sliver.data.db.Migration5To6
import app.sliver.data.db.Migration6To7
import app.sliver.data.db.Migration7To8
import app.sliver.data.db.Migration8To9
import app.sliver.BuildConfig
import app.sliver.data.auth.AndroidGoogleAuth
import app.sliver.data.auth.FakeGoogleAuth
import app.sliver.data.auth.GoogleAuth
import app.sliver.data.auth.googleConfigured
import app.sliver.data.backup.AndroidArchiveExporter
import app.sliver.data.backup.AndroidBackupScheduler
import app.sliver.data.backup.AndroidDayFileReader
import app.sliver.data.backup.AndroidNetworkMonitor
import app.sliver.data.backup.AndroidRestoreScheduler
import app.sliver.data.backup.AndroidRestoredDayWriter
import app.sliver.data.backup.RestoreScheduler
import app.sliver.data.backup.RestoredDayWriter
import app.sliver.data.backup.ArchiveExporter
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.NetworkMonitor
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.AndroidClipExporter
import app.sliver.data.export.ClipExporter
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.repo.AndroidPlaceResolver
import app.sliver.data.repo.PlaceResolver
import app.sliver.data.files.FileStore
import app.sliver.feature.autoimport.AndroidAutoImportRunner
import app.sliver.feature.autoimport.AndroidMediaScanner
import app.sliver.feature.autoimport.AutoImportRunner
import app.sliver.feature.autoimport.MediaScanner
import app.sliver.feature.films.AndroidFilmRenderer
import app.sliver.feature.films.AndroidFilmShare
import app.sliver.feature.films.FilmRenderer
import app.sliver.feature.films.FilmShare
import app.sliver.feature.gallery.AndroidGalleryProvider
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.notifications.AndroidGoldenHour
import app.sliver.feature.notifications.AndroidReminderScheduler
import app.sliver.feature.notifications.GoldenHourProvider
import app.sliver.feature.notifications.AndroidReminderLog
import app.sliver.feature.notifications.ReminderLog
import app.sliver.feature.notifications.ReminderScheduler
import app.sliver.feature.trim.AndroidTrimMedia
import app.sliver.feature.trim.TrimMedia
import androidx.media3.common.util.UnstableApi
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

@androidx.annotation.OptIn(UnstableApi::class)
private fun clipExporter(ctx: Context): ClipExporter = AndroidClipExporter(ctx)

actual val platformModule: Module = module {
    single<PlaceResolver> { AndroidPlaceResolver(get<Context>()) }
    single {
        val ctx = get<Context>()
        Room.databaseBuilder<SliverDatabase>(ctx, ctx.getDatabasePath("sliver.db").absolutePath)
            .addMigrations(Migration2To3, Migration3To4, Migration4To5, Migration5To6, Migration6To7, Migration7To8, Migration8To9).build()
    }
    single<FileStore> { get<Context>().let { AndroidFileStore(it.filesDir, it.cacheDir) } }
    single<ClipExporter> { clipExporter(get<Context>()) }
    single<TrimMedia> { AndroidTrimMedia() }
    single { ProgressCoordinator(get<Context>(), get(), get()) }
    single<AutoImportRunner> { AndroidAutoImportRunner(get<Context>()) }
    single<MediaScanner> { AndroidMediaScanner(get<Context>()) }
    single<app.sliver.feature.autoimport.FolderScanner> { app.sliver.feature.autoimport.AndroidFolderScanner(get<Context>()) }
    single<app.sliver.feature.autoimport.FolderAccess> { app.sliver.feature.autoimport.AndroidFolderAccess(get<Context>()) }
    single<FilmRenderer> { AndroidFilmRenderer(get<Context>()) }
    single<ReminderLog> { AndroidReminderLog(get<Context>()) }
    single<GoldenHourProvider> { AndroidGoldenHour(get<Context>()) }
    single<ReminderScheduler> { AndroidReminderScheduler(get<Context>()) }
    single<GoogleAuth> {
        if (googleConfigured) AndroidGoogleAuth(get<Context>(), BuildConfig.GOOGLE_WEB_CLIENT_ID) else FakeGoogleAuth()
    }
    single<DayFileReader> { AndroidDayFileReader }
    single<RestoredDayWriter> { AndroidRestoredDayWriter() }
    single<RestoreScheduler> { AndroidRestoreScheduler(get<Context>()) }
    single<BackupScheduler> { AndroidBackupScheduler(get<Context>()) }
    single<NetworkMonitor> { AndroidNetworkMonitor(get<Context>()) }
    single<ArchiveExporter> { AndroidArchiveExporter(get<Context>(), get(), get(), get(), get()) }
    single<FilmShare> { AndroidFilmShare(get<Context>()) }
    single<HttpClientEngine> { OkHttp.create() }
    single<GalleryProvider> { AndroidGalleryProvider(get<Context>(), get()) }
    single {
        PreferenceDataStoreFactory.create { get<Context>().filesDir.resolve("settings.preferences_pb") }
    }
}
