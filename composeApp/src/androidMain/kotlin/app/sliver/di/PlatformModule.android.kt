package app.sliver.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.AndroidClipExporter
import app.sliver.data.export.AndroidDayShare
import app.sliver.data.export.ClipExporter
import app.sliver.data.export.DayShare
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.files.FileStore
import app.sliver.feature.backfill.AndroidBackfillRunner
import app.sliver.feature.backfill.BackfillRunner
import app.sliver.feature.gallery.AndroidGalleryProvider
import app.sliver.feature.gallery.GalleryProvider
import app.sliver.feature.trim.AndroidTrimMedia
import app.sliver.feature.trim.TrimMedia
import androidx.media3.common.util.UnstableApi
import org.koin.core.module.Module
import org.koin.dsl.module

@androidx.annotation.OptIn(UnstableApi::class)
private fun clipExporter(ctx: Context): ClipExporter = AndroidClipExporter(ctx)

@androidx.annotation.OptIn(UnstableApi::class)
private fun dayShare(ctx: Context): DayShare = AndroidDayShare(ctx)

actual val platformModule: Module = module {
    single<DayShare> { dayShare(get<Context>()) }
    single {
        val ctx = get<Context>()
        Room.databaseBuilder<SliverDatabase>(ctx, ctx.getDatabasePath("sliver.db").absolutePath).build()
    }
    single<FileStore> { get<Context>().let { AndroidFileStore(it.filesDir, it.cacheDir) } }
    single<ClipExporter> { clipExporter(get<Context>()) }
    single<TrimMedia> { AndroidTrimMedia() }
    single<BackfillRunner> { AndroidBackfillRunner(get<Context>()) }
    single<GalleryProvider> { AndroidGalleryProvider(get<Context>(), get()) }
    single {
        PreferenceDataStoreFactory.create { get<Context>().filesDir.resolve("settings.preferences_pb") }
    }
}
