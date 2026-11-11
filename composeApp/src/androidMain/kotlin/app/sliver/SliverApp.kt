package app.sliver

import android.app.Application
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.files.FileStore
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
        val coordinator: ReminderCoordinator by inject()
        coordinator.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val backup: BackupCoordinator by inject()
        backup.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val days: DayRepository by inject()
        val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        widgetScope.launch {
            days.observeAll().map { list -> list.map { it.date to it.thumb } }.distinctUntilChanged()
                .collect { refreshTodayWidgets(this@SliverApp) }
        }
        Thread { store.cleanTmp(System.currentTimeMillis()) }.start()
    }
}
