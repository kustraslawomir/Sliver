package app.sliver.di

import app.sliver.data.auth.AccountDeleter
import app.sliver.data.auth.AccountManager
import kotlinx.coroutines.flow.first
import app.sliver.data.auth.GoogleAuth
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.backup.BackupEngine
import app.sliver.data.backup.DriveClient
import app.sliver.data.backup.RestoreEngine
import app.sliver.data.backup.DriveTrashQueue
import app.sliver.data.backup.SettingsTrashQueue
import app.sliver.data.backup.InMemoryDriveClient
import app.sliver.data.backup.RestDriveClient
import app.sliver.data.db.SliverDatabase
import io.ktor.client.engine.HttpClientEngine
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.DayRepositoryImpl
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.FilmRepositoryImpl
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.repo.ProjectRepositoryImpl
import app.sliver.data.settings.DataStoreSettingsRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.backfill.BackfillExecutor
import app.sliver.feature.backfill.BackfillStore
import app.sliver.feature.notifications.ReminderCoordinator
import app.sliver.feature.trim.SaveUndo
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { get<SliverDatabase>().projectDao() }
    single { get<SliverDatabase>().dayDao() }
    single { get<SliverDatabase>().filmDao() }
    single<DriveTrashQueue> { SettingsTrashQueue(get()) }
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get()) }
    single<DayRepository> { DayRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single { BackupEngine(get(), get(), get(), get(), get()) }
    single { BackupCoordinator(get(), get(), get()) }
    single { RestoreEngine(get(), get(), get(), get(), get(), get(), get()) }
    single { SaveUndo(get(), get(), get()) }
    single { BackfillStore(get()) }
    single { BackfillExecutor(get(), get(), get(), get(), get(), get()) }
    single<FilmRepository> { FilmRepositoryImpl(get(), get()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
    single { AccountManager(get(), get()) }
    single {
        val days = get<DayRepository>()
        AccountDeleter(get(), get(), get()) { days.observeAll().first().forEach { days.delete(it.date) } }
    }
    single<DriveClient> {
        val auth = get<GoogleAuth>()
        if (auth.isFake) InMemoryDriveClient() else RestDriveClient(get<HttpClientEngine>(), auth::accessToken)
    }
    single { ReminderCoordinator(get(), get(), get()) }
}

expect val platformModule: Module
