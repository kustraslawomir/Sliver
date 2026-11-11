package app.sliver.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.sliver.data.db.SliverDatabase
import app.sliver.data.export.AndroidClipExporter
import app.sliver.data.export.ClipExporter
import app.sliver.data.files.AndroidFileStore
import app.sliver.data.files.FileStore
import app.sliver.feature.trim.AndroidTrimMedia
import app.sliver.feature.trim.TrimMedia
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        val ctx = get<Context>()
        Room.databaseBuilder<SliverDatabase>(ctx, ctx.getDatabasePath("sliver.db").absolutePath).build()
    }
    single<FileStore> { get<Context>().let { AndroidFileStore(it.filesDir, it.cacheDir) } }
    single<ClipExporter> { AndroidClipExporter(get<Context>()) }
    single<TrimMedia> { AndroidTrimMedia() }
    single {
        PreferenceDataStoreFactory.create { get<Context>().filesDir.resolve("settings.preferences_pb") }
    }
}
