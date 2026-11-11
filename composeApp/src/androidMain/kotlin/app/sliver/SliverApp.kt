package app.sliver

import android.app.Application
import app.sliver.di.commonModule
import app.sliver.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SliverApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SliverApp)
            modules(commonModule, platformModule)
        }
    }
}
