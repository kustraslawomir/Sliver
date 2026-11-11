package app.sliver.feature.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import app.sliver.R

private const val CH_PROGRESS = "progress"
private const val EMBER = 0xFFFF8A3D.toInt()
private const val PENDING = 0xFF6B6B70.toInt()

/**
 * Determinate progress for long work. API 36+: Notification.ProgressStyle (one segment per unit, a point at the
 * current one, slit tracker icon, promoted-ongoing request). Below: a plain ongoing progress notification.
 */
class ProgressNotifier(
    private val context: Context,
    private val id: Int,
    private val target: DeepLinkTarget,
    private val channelId: String = CH_PROGRESS,
    private val channelName: String = "Progress",
) {
    private var lastPostMs: Long? = null

    fun build(title: String, text: String, done: Int, total: Int, segments: Int = 1): Notification {
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW))
        val b = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_sliver)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openTargetIntent(context, id, target))
        if (Build.VERSION.SDK_INT >= 36) {
            val n = segments.coerceAtLeast(1)
            val finished = segmentsDone(n, done, total)
            val style = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgressSegments(List(n) { Notification.ProgressStyle.Segment(100).setColor(if (it < finished) EMBER else PENDING) })
                .setProgress((if (total <= 0) 0 else done.coerceIn(0, total) * n * 100 / total))
                .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_stat_sliver))
            if (n > 1) style.setProgressPoints(listOf(Notification.ProgressStyle.Point((finished.coerceAtMost(n - 1) * 100 + 50)).setColor(EMBER)))
            if (total <= 0) style.setProgressIndeterminate(true)
            b.setStyle(style)
            if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                b.setRequestPromotedOngoing(true).setShortCriticalText(if (total > 0) "${done * 100 / total}%" else "")
            }
        } else {
            b.setProgress(total.coerceAtLeast(0), done.coerceIn(0, total.coerceAtLeast(0)), total <= 0)
        }
        return b.build()
    }

    /** Posts at most once per second; returns false when skipped. */
    fun post(title: String, text: String, done: Int, total: Int, segments: Int = 1, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!canNotify(context) || !progressDue(lastPostMs, nowMs)) return false
        lastPostMs = nowMs
        context.getSystemService(NotificationManager::class.java).notify(id, build(title, text, done, total, segments))
        return true
    }

    fun cancel() {
        lastPostMs = null
        context.getSystemService(NotificationManager::class.java).cancel(id)
    }
}
