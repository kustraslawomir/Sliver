package app.sliver

import app.sliver.core.AppScope
import android.app.Application
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.files.FileStore
import app.sliver.feature.notifications.ProgressCoordinator
import app.sliver.feature.notifications.ReminderCoordinator
import app.sliver.feature.notifications.ensureChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import app.sliver.data.repo.DayRepository
import app.sliver.feature.widget.refreshTodayWidgets
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class SliverApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SliverApp)
            modules(commonModule, platformModule)
        }
        val store: FileStore by inject()
        ensureChannels(this)
        val golden: app.sliver.feature.notifications.GoldenHourProvider by inject()
        golden.refresh()
        val coordinator: ReminderCoordinator by inject()
        coordinator.start(AppScope)
        val backup: BackupCoordinator by inject()
        backup.start(AppScope)
        val progress: ProgressCoordinator by inject()
        progress.start(AppScope)
        val days: DayRepository by inject()
        val widgetScope = AppScope
        widgetScope.launch {
            days.observeAll().map { list -> list.map { it.date to it.thumb } }.distinctUntilChanged()
                .collect { refreshTodayWidgets(this@SliverApp) }
        }
        Thread { store.cleanTmp(System.currentTimeMillis()) }.start()
    }
}
