package app.sliver

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import app.sliver.feature.notifications.DeepLink
import app.sliver.feature.notifications.EXTRA_DEEPLINK
import app.sliver.feature.notifications.parseDeepLink
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import app.sliver.data.auth.AuthHost
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The Compose SplashOverlay draws the same disc in the same place and runs the rest of the sequence.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        val consent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { AuthHost.onResult(it.data) }
        AuthHost.launcher = { consent.launch(it) }
        setContent { App() }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean =
        app.sliver.feature.camera.VolumeKeys.dispatch(event) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean =
        app.sliver.feature.camera.VolumeKeys.dispatch(event) || super.onKeyUp(keyCode, event)

    override fun onResume() {
        super.onResume()
        AuthHost.activity = this
    }

    override fun onPause() {
        if (AuthHost.activity === this) AuthHost.activity = null
        super.onPause()
    }

    override fun onDestroy() {
        // STEP 5.5: the launcher lambda captures this activity; a singleton must not keep it after destroy.
        AuthHost.launcher = null
        if (AuthHost.activity === this) AuthHost.activity = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        parseDeepLink(intent?.getStringExtra(EXTRA_DEEPLINK))?.let { DeepLink.target.value = it }
    }
}
