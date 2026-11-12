package app.sliver.feature.notifications

import app.sliver.R
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
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import android.graphics.BitmapFactory
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.mp.KoinPlatform

private const val CH_REMINDERS = "daily_reminder"
private const val CH_REMINDERS_OLD = "reminders"
private const val CH_FILMS = "films"
private const val CH_BACKUP = "backup"
private const val CH_MEMORIES = "memories"
private const val EXTRA_KIND = "kind"
private const val EXTRA_DATE = "date"
const val EXTRA_DEEPLINK = "deeplink"
const val EXTRA_NOTIF_ID = "notifId"
private const val WINDOW_MS = 10 * 60_000L

fun ensureChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)
    nm.deleteNotificationChannel(CH_REMINDERS_OLD)
    nm.createNotificationChannels(
        listOf(
            NotificationChannel(CH_REMINDERS, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_FILMS, "Films", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_BACKUP, "Backup", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CH_MEMORIES, "Memories", NotificationManager.IMPORTANCE_LOW),
        ),
    )
}

fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun openTargetIntent(context: Context, requestCode: Int, target: DeepLinkTarget): PendingIntent = PendingIntent.getActivity(
    context, requestCode,
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .putExtra(EXTRA_DEEPLINK, target.encode()),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun post(context: Context, id: Int, channel: String, copy: NotifCopy, target: DeepLinkTarget) {
    if (!canNotify(context)) return
    ensureChannels(context)
    val open = openTargetIntent(context, id, target)
    val n = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_sliver)
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

private fun memoryDeepLink(date: LocalDate) = DeepLinkTarget.Day(date.minus(DatePeriod(months = 1)))

private fun postReminder(context: Context, kind: NotifKind, date: LocalDate, thumb: String?) {
    if (!canNotify(context)) return
    ensureChannels(context)
    val id = 8000 + kind.ordinal
    val copy = reminderCopy(kind, date)
    val record = openTargetIntent(context, id, DeepLinkTarget.Camera)
    val tomorrow = PendingIntent.getBroadcast(
        context, id,
        Intent(context, ReminderDismissReceiver::class.java).putExtra(EXTRA_NOTIF_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val n = NotificationCompat.Builder(context, CH_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_sliver)
        .setContentTitle(copy.title)
        .setContentText(copy.body)
        .setAutoCancel(true)
        .setContentIntent(record)
        .addAction(0, "Record", record)
        .addAction(0, "Tomorrow", tomorrow)
        .setSilent(kind == NotifKind.LAST_CALL)
        .apply { thumb?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }?.let(::setLargeIcon) }
        .build()
    context.getSystemService(NotificationManager::class.java).notify(id, n)
}

private fun postMemory(context: Context, date: LocalDate) =
    post(context, 8000 + NotifKind.ON_THIS_DAY.ordinal, CH_MEMORIES, reminderCopy(NotifKind.ON_THIS_DAY, date), memoryDeepLink(date))

/** Per-day record of fired reminders, so a re-plan or settings change can neither repeat one nor exceed the daily limit. */
class AndroidReminderLog(private val context: Context) : ReminderLog {
    private val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)

    private fun load(key: String, today: LocalDate): Set<NotifKind> {
        val (day, kinds) = (prefs.getString(key, null) ?: return emptySet()).split("|").let { it[0] to it.getOrElse(1) { "" } }
        if (day != today.toString()) return emptySet()
        return kinds.split(",").mapNotNull { k -> NotifKind.entries.firstOrNull { it.name == k } }.toSet()
    }

    override fun handled(today: LocalDate) = load("handled", today)
    override fun posted(today: LocalDate) = load("posted", today)

    @Synchronized
    override fun record(today: LocalDate, kind: NotifKind, posted: Boolean) {
        val e = prefs.edit()
        e.putString("handled", "$today|${(handled(today) + kind).joinToString(",") { it.name }}")
        if (posted) e.putString("posted", "$today|${(posted(today) + kind).joinToString(",") { it.name }}")
        e.commit()
    }
}

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
            val pi = intent(p.kind, p.at.date, PendingIntent.FLAG_UPDATE_CURRENT)!!
            alarms.setWindow(AlarmManager.RTC_WAKEUP, p.at.epochMillisIn(zone), WINDOW_MS, pi)
        }
    }
}

/** Posts the nudge unless the day is done (or the daily limit is reached), then re-plans so the next one is set. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { NotifKind.valueOf(it) }.getOrNull() } ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val koin = KoinPlatform.getKoin()
                val coordinator = koin.get<ReminderCoordinator>()
                if (kind == NotifKind.ON_THIS_DAY) {
                    postMemory(context, date)
                } else {
                    val r = koin.get<SettingsRepository>().settings.first().reminder
                    val today = todayLocal()
                    val log = koin.get<ReminderLog>()
                    val show = shouldPost(kind, r, coordinator.todayHasClip(r, today), log.posted(today))
                    if (show) postReminder(context, kind, today, coordinator.yesterdayThumb(r, today))
                    log.record(today, kind, show)
                }
                coordinator.replanOnce()
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Tomorrow": just dismisses; nothing is counted. */
class ReminderDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra(EXTRA_NOTIF_ID, -1))
    }
}

/** Alarms are cleared by a reboot and shift with the clock or zone; re-plan from the settings. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
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
