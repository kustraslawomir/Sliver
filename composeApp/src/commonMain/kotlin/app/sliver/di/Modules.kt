package app.sliver.di

import app.sliver.data.db.SliverDatabase
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.backfill.BackfillExecutor
import app.sliver.feature.backfill.BackfillStore
import app.sliver.feature.trim.SaveUndo
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { get<SliverDatabase>().dayDao() }
    single { get<SliverDatabase>().filmDao() }
    single<DayRepository> { DayRepositoryImpl(get(), get(), get(), get()) }
    single { SaveUndo(get(), get()) }
    single { BackfillStore(get()) }
    single { BackfillExecutor(get(), get(), get(), get(), get(), get()) }
    single<FilmRepository> { FilmRepositoryImpl(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}

expect val platformModule: Module
