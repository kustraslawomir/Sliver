package app.sliver.di

import app.sliver.data.auth.AccountDeleter
import app.sliver.data.auth.AccountManager
import kotlinx.coroutines.flow.first
import app.sliver.data.auth.GoogleAuth
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.backup.DaoTombstoneStore
import app.sliver.data.backup.SyncEngine
import app.sliver.data.backup.TombstoneStore
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
import app.sliver.feature.autoimport.AutoImportExecutor
import app.sliver.feature.autoimport.AutoImportStore
import app.sliver.feature.autoimport.DraftActions
import app.sliver.feature.notifications.ReminderCoordinator
import app.sliver.feature.trim.SaveUndo
import app.sliver.feature.films.FilmDeleter
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { get<SliverDatabase>().projectDao() }
    single { get<SliverDatabase>().dayDao() }
    single { get<SliverDatabase>().filmDao() }
    single { get<SliverDatabase>().tombstoneDao() }
    single<TombstoneStore> { DaoTombstoneStore(get()) }
    single<DriveTrashQueue> { SettingsTrashQueue(get()) }
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<DayRepository> { DayRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single { SyncEngine(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { BackupCoordinator(get(), get(), get()) }
    single { RestoreEngine(get(), get(), get()) }
    single { SaveUndo(get(), get(), get()) }
    single { app.sliver.feature.trim.OriginalRescue(get(), get()) }
    single { FilmDeleter(get(), get(), get()) }
    single { AutoImportStore(get()) }
    single { app.sliver.feature.autoimport.FolderSourceStore(get()) }
    single { DraftActions(get(), get(), get(), get(), get()) }
    single { AutoImportExecutor(get(), get(), get(), get(), get(), get(), get(), folderScanner = get()) }
    single<FilmRepository> { FilmRepositoryImpl(get(), get(), get()) }
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
    single { ReminderCoordinator(get(), get(), get(), get(), get(), get()) }
}

expect val platformModule: Module
