package app.sliver.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.sliver.BuildConfig
import app.sliver.data.auth.AndroidGoogleAuth
import app.sliver.data.auth.FakeGoogleAuth
import app.sliver.data.auth.GoogleAuth
import app.sliver.data.auth.googleConfigured
import app.sliver.data.backup.AndroidArchiveExporter
import app.sliver.data.backup.AndroidBackupScheduler
import app.sliver.data.backup.AndroidDayFileReader
import app.sliver.data.backup.AndroidNetworkMonitor
import app.sliver.data.backup.ArchiveExporter
import app.sliver.data.backup.BackupScheduler
import app.sliver.data.backup.DayFileReader
import app.sliver.data.backup.NetworkMonitor
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.AndroidClipExporter
import app.sliver.data.export.AndroidDayShare
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.DayShare
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.repo.AndroidPlaceResolver
import app.sliver.data.repo.PlaceResolver
import app.sliver.data.files.FileStore
import app.sliver.feature.backfill.AndroidBackfillRunner
import app.sliver.feature.backfill.BackfillRunner
import app.sliver.feature.films.AndroidFilmRenderer
import app.sliver.feature.films.AndroidFilmShare
import app.sliver.feature.films.FilmRenderer
import app.sliver.feature.films.FilmShare
import app.sliver.feature.gallery.AndroidGalleryProvider
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.notifications.AndroidReminderScheduler
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

@androidx.annotation.OptIn(UnstableApi::class)
private fun dayShare(ctx: Context): DayShare = AndroidDayShare(ctx)

actual val platformModule: Module = module {
    single<PlaceResolver> { AndroidPlaceResolver(get<Context>()) }
    single<DayShare> { dayShare(get<Context>()) }
    single {
        val ctx = get<Context>()
        Room.databaseBuilder<SliverDatabase>(ctx, ctx.getDatabasePath("sliver.db").absolutePath).build()
    }
    single<FileStore> { get<Context>().let { AndroidFileStore(it.filesDir, it.cacheDir) } }
    single<ClipExporter> { clipExporter(get<Context>()) }
    single<TrimMedia> { AndroidTrimMedia() }
    single<BackfillRunner> { AndroidBackfillRunner(get<Context>()) }
    single<FilmRenderer> { AndroidFilmRenderer(get<Context>()) }
    single<ReminderScheduler> { AndroidReminderScheduler(get<Context>()) }
    single<GoogleAuth> {
        if (googleConfigured) AndroidGoogleAuth(get<Context>(), BuildConfig.GOOGLE_WEB_CLIENT_ID) else FakeGoogleAuth()
    }
    single<DayFileReader> { AndroidDayFileReader }
    single<BackupScheduler> { AndroidBackupScheduler(get<Context>()) }
    single<NetworkMonitor> { AndroidNetworkMonitor(get<Context>()) }
    single<ArchiveExporter> { AndroidArchiveExporter(get<Context>(), get(), get(), get()) }
    single<FilmShare> { AndroidFilmShare(get<Context>()) }
    single<HttpClientEngine> { OkHttp.create() }
    single<GalleryProvider> { AndroidGalleryProvider(get<Context>(), get()) }
    single {
        PreferenceDataStoreFactory.create { get<Context>().filesDir.resolve("settings.preferences_pb") }
    }
}
