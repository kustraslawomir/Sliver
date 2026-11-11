package app.sliver.di

import app.sliver.data.db.SliverDatabase
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.data.settings.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { get<SliverDatabase>().dayDao() }
    single { get<SliverDatabase>().filmDao() }
    single<DayRepository> { DayRepositoryImpl(get(), get(), get()) }
    single<FilmRepository> { FilmRepositoryImpl(get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}

expect val platformModule: Module
