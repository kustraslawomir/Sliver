package app.sliver

import androidx.compose.ui.window.ComposeUIViewController
import app.sliver.core.AppScope
import app.sliver.data.backup.BackupCoordinator
import app.sliver.data.files.FileStore
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import app.sliver.feature.notifications.ReminderCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

private val koinStarted by lazy {
    startKoin { modules(commonModule, platformModule) }
    val koin = KoinPlatform.getKoin()
    koin.get<ReminderCoordinator>().start(AppScope)
    koin.get<BackupCoordinator>().start(AppScope)
    AppScope.launch(Dispatchers.IO) { koin.get<FileStore>().cleanTmp(Clock.System.now().toEpochMilliseconds()) }
    true
}

fun MainViewController() = ComposeUIViewController {
    check(koinStarted)
    App()
}
