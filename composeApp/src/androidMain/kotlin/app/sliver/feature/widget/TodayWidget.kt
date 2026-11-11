package app.sliver.feature.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import app.sliver.MainActivity
import app.sliver.R
import app.sliver.core.time.todayLocal
import app.sliver.data.repo.DayRepository
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.EXTRA_DEEPLINK
import app.sliver.feature.notifications.encode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.koin.mp.KoinPlatform

private const val ACTION_MIDNIGHT = "app.sliver.widget.MIDNIGHT"
private const val THUMB_MAX_PX = 400

fun cameraIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(EXTRA_DEEPLINK, DeepLinkTarget.Camera.encode())

private fun decodeThumb(path: String) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMB_MAX_PX) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

/** Rebuilds every placed widget from today's row and re-arms the midnight refresh. */
suspend fun refreshTodayWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidget::class.java))
    if (ids.isEmpty()) return
    val today = todayLocal()
    val day = KoinPlatform.getKoin().get<DayRepository>().getDay(today)
    val thumb = day?.thumb?.takeIf { File(it).exists() }?.let(::decodeThumb)
    val tap = PendingIntent.getActivity(
        context, 0, cameraIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
        val captured = day != null
        setViewVisibility(R.id.widget_waiting, if (captured) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.widget_captured, if (captured) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.widget_thumb, if (thumb != null) View.VISIBLE else View.GONE)
        if (thumb != null) setImageViewBitmap(R.id.widget_thumb, thumb)
        setOnClickPendingIntent(R.id.widget_root, tap)
    }
    manager.updateAppWidget(ids, views)
    scheduleMidnight(context, today.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
}

private fun scheduleMidnight(context: Context, atMillis: Long) {
    val pi = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, TodayWidget::class.java).setAction(ACTION_MIDNIGHT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, atMillis, pi)
}

class TodayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) = refreshAsync(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MIDNIGHT) refreshAsync(context)
    }

    private fun refreshAsync(context: Context) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                refreshTodayWidgets(context.applicationContext)
            } finally {
                result.finish()
            }
        }
    }
}
