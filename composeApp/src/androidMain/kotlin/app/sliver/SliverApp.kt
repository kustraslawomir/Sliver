package app.sliver

import android.app.Application
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import app.sliver.data.files.FileStore
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
        Thread { store.cleanTmp(System.currentTimeMillis()) }.start()
    }
}
