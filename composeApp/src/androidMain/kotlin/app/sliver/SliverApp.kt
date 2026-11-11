package app.sliver

import android.app.Application
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import app.sliver.data.files.FileStore
import app.sliver.feature.notifications.ReminderCoordinator
import app.sliver.feature.notifications.ensureChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        Thread { store.cleanTmp(System.currentTimeMillis()) }.start()
    }
}
