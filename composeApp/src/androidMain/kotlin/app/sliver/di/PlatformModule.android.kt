package app.sliver.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.sliver.data.db.SliverDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        val ctx = get<Context>()
        Room.databaseBuilder<SliverDatabase>(ctx, ctx.getDatabasePath("sliver.db").absolutePath).build()
    }
    single {
        PreferenceDataStoreFactory.create { get<Context>().filesDir.resolve("settings.preferences_pb") }
    }
}
