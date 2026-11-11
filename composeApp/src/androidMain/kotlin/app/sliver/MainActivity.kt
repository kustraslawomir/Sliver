package app.sliver

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import app.sliver.feature.notifications.DeepLink
import app.sliver.feature.notifications.EXTRA_DEEPLINK
import app.sliver.feature.notifications.parseDeepLink
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        parseDeepLink(intent?.getStringExtra(EXTRA_DEEPLINK))?.let { DeepLink.target.value = it }
    }
}
