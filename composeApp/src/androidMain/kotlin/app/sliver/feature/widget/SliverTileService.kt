package app.sliver.feature.widget

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.TileService

class SliverTileService : TileService() {
    override fun onClick() {
        val intent = cameraIntent(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
