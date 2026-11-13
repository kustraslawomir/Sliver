package app.sliver.feature.autoimport

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.FILL_NOTIFICATION_ID
import app.sliver.feature.notifications.ProgressNotifier
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.sliver.data.media.MotionPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.mp.KoinPlatform

private const val WORK_NAME = "autoImport"

class AndroidAutoImportRunner(private val context: Context) : AutoImportRunner {
    override fun start() {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<AutoImportWorker>().build())
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/** Progress lives in [AutoImportStore], so an interrupted run picks up where it stopped. */
class AutoImportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Same id as ProgressCoordinator's "Importing drafts", so the progress updates land in this notification.
        val n = ProgressNotifier(applicationContext, FILL_NOTIFICATION_ID, DeepLinkTarget.FillThePast)
            .build("Importing drafts", "Looking through your gallery", 0, 0)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        return ForegroundInfo(FILL_NOTIFICATION_ID, n, type)
    }

    override suspend fun doWork(): Result {
        // Owner 13 Nov (Samsung): a background scan of thousands of gallery items got the app killed for "excessive
        // binder traffic during cached". As a foreground job it keeps running after leaving the screen.
        runCatching { setForeground(getForegroundInfo()) }
        KoinPlatform.getKoin().get<AutoImportExecutor>().run()
        return Result.success()
    }
}

/** MediaStore query by DATE_TAKEN; items that only have a modified date are ignored (messengers set that to the day received). */
class AndroidMediaScanner(private val context: Context) : MediaScanner {
    override suspend fun scan(from: LocalDate, to: LocalDate, zone: TimeZone, sniffDays: Set<LocalDate>): List<MediaEntry> =
        withContext(Dispatchers.IO) {
            val lo = from.atStartOfDayIn(zone).toEpochMilliseconds()
            val hi = LocalDate.fromEpochDays(to.toEpochDays() + 1).atStartOfDayIn(zone).toEpochMilliseconds()
            val out = mutableListOf<MediaEntry>()
            query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, lo, hi) { uri, taken, _, camera ->
                val day = app.sliver.feature.autoimport.localDateOf(taken, zone)
                val motion = day in sniffDays && isMotion(uri)
                out += MediaEntry(uri.toString(), video = false, takenAtMs = taken, durationMs = 0, motion = motion, camera = camera)
            }
            query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, lo, hi) { uri, taken, duration, camera ->
                out += MediaEntry(uri.toString(), video = true, takenAtMs = taken, durationMs = duration, camera = camera)
            }
            out
        }

    private fun query(base: Uri, video: Boolean, lo: Long, hi: Long, each: (Uri, Long, Long, Boolean) -> Unit) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_TAKEN,
            if (video) MediaStore.Video.Media.DURATION else MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME,
        )
        runCatching {
            context.contentResolver.query(
                base, projection, "${MediaStore.MediaColumns.DATE_TAKEN} >= ? AND ${MediaStore.MediaColumns.DATE_TAKEN} < ?",
                arrayOf(lo.toString(), hi.toString()), "${MediaStore.MediaColumns.DATE_TAKEN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(3)
                    if (app.sliver.feature.autoimport.isScreenCapture(path, c.getString(4))) continue
                    each(ContentUris.withAppendedId(base, c.getLong(0)), c.getLong(1), if (video) c.getLong(2) else 0L, app.sliver.feature.autoimport.isCameraPath(path))
                }
            }
        }
    }

    private fun isMotion(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(MotionPhoto.HEAD_BYTES)
            var n = 0
            while (n < buf.size) {
                val r = input.read(buf, n, buf.size - n)
                if (r < 0) break
                n += r
            }
            MotionPhoto.declaresMotion(buf.copyOf(n))
        }
    }.getOrNull() == true
}
