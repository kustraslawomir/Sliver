package app.sliver.feature.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.sliver.MainActivity
import app.sliver.core.time.todayLocal
import app.sliver.data.repo.DayRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.mp.KoinPlatform

private const val CH_REMINDERS = "reminders"
private const val CH_FILMS = "films"
private const val CH_BACKUP = "backup"
private const val CH_MEMORIES = "memories"
private const val EXTRA_KIND = "kind"
private const val EXTRA_DATE = "date"
const val EXTRA_DEEPLINK = "deeplink"
private const val WINDOW_MS = 20 * 60_000L

private fun NotifKind.channel() = when (this) {
    NotifKind.ON_THIS_DAY -> CH_MEMORIES
    else -> CH_REMINDERS
}

private fun NotifKind.deepLink(date: LocalDate) =
    if (this == NotifKind.ON_THIS_DAY) DeepLinkTarget.Day(date.minus(DatePeriod(months = 1))) else DeepLinkTarget.Camera

fun ensureChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannels(
        listOf(
            NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_FILMS, "Films", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_BACKUP, "Backup", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_MEMORIES, "Memories", NotificationManager.IMPORTANCE_LOW),
        ),
    )
}

private fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun post(context: Context, id: Int, channel: String, copy: NotifCopy, target: DeepLinkTarget) {
    if (!canNotify(context)) return
    ensureChannels(context)
    val open = PendingIntent.getActivity(
        context, id,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_DEEPLINK, target.encode()),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val n = NotificationCompat.Builder(context, channel)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle(copy.title)
        .setContentText(copy.body)
        .setAutoCancel(true)
        .setContentIntent(open)
        .build()
    context.getSystemService(NotificationManager::class.java).notify(id, n)
}

fun notifyFilmReady(context: Context, filmId: String, name: String, seconds: Int) =
    post(context, 9000 + (filmId.hashCode() and 0xFFF), CH_FILMS, filmReadyCopy(name, seconds), DeepLinkTarget.Film(filmId))

fun notifyBackupPaused(context: Context, error: app.sliver.data.backup.BackupError) =
    post(context, 9100, CH_BACKUP, backupProblemCopy(error), DeepLinkTarget.Backup)

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun code(kind: NotifKind, date: LocalDate) = kind.ordinal * HORIZON_DAYS + (date.toEpochDays() % HORIZON_DAYS).toInt()

    private fun intent(kind: NotifKind, date: LocalDate, flags: Int) = PendingIntent.getBroadcast(
        context, code(kind, date),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind.name).putExtra(EXTRA_DATE, date.toString()),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun schedule(plan: List<PlannedNotif>) {
        val today = todayLocal()
        for (kind in NotifKind.entries) for (i in 0 until HORIZON_DAYS) {
            val date = today.plus(DatePeriod(days = i))
            intent(kind, date, PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
        }
        val zone = TimeZone.currentSystemDefault()
        plan.forEach { p ->
            val at = p.at.toInstant(zone).toEpochMilliseconds()
            val pi = intent(p.kind, p.at.date, PendingIntent.FLAG_UPDATE_CURRENT)!!
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                alarms.setWindow(AlarmManager.RTC_WAKEUP, at - WINDOW_MS / 2, WINDOW_MS, pi)
            }
        }
    }
}

/** Posts the nudge unless the day already has a second, then re-plans so the horizon keeps rolling. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { NotifKind.valueOf(it) }.getOrNull() } ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val captured = kind != NotifKind.ON_THIS_DAY &&
                    KoinPlatform.getKoin().get<DayRepository>().getDay(date) != null
                if (!captured) post(context, 8000 + kind.ordinal, kind.channel(), reminderCopy(kind, date), kind.deepLink(date))
                KoinPlatform.getKoin().get<ReminderCoordinator>().replanOnce()
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared by a reboot; app start re-plans from the coordinator. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                KoinPlatform.getKoin().get<ReminderCoordinator>().replanOnce()
            } finally {
                pending.finish()
            }
        }
    }
}
