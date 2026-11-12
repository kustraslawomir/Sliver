package app.sliver.data.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.repo.ProjectRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.feature.notifications.notifyBackupPaused
import java.io.BufferedOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform

private const val WORK_NAME = "backup"
private const val RESTORE_WORK_NAME = "restore"
private const val RESTORE_THUMB_WIDTH = 320

class AndroidRestoredDayWriter : RestoredDayWriter {
    override suspend fun write(bytes: ByteArray, videoPath: String, thumbPath: String): Int = withContext(Dispatchers.IO) {
        File(videoPath).writeBytes(bytes)
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(videoPath)
            val frame = r.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@withContext 0
            val height = (RESTORE_THUMB_WIDTH.toFloat() * frame.height / frame.width).toInt().coerceAtLeast(1)
            val thumb = Bitmap.createScaledBitmap(frame, RESTORE_THUMB_WIDTH, height, true)
            File(thumbPath).outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            Bitmap.createScaledBitmap(thumb, 1, 1, true).getPixel(0, 0) or (0xFF shl 24)
        } catch (e: RuntimeException) {
            0
        } finally {
            r.release()
        }
    }
}

class AndroidRestoreScheduler(private val context: Context) : RestoreScheduler {
    override fun start() {
        val request = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(RESTORE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}

class RestoreWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (KoinPlatform.getKoin().get<RestoreEngine>().run()) {
        null -> Result.success()
        BackupError.REVOKED -> Result.failure()
        else -> Result.retry()
    }
}

val AndroidDayFileReader = DayFileReader { path -> File(path).takeIf { it.exists() }?.readBytes() }

class AndroidBackupScheduler(private val context: Context) : BackupScheduler {
    override fun schedule(wifiOnly: Boolean, anyNetwork: Boolean) {
        val network = if (wifiOnly && !anyNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val koin = KoinPlatform.getKoin()
        val settings = koin.get<SettingsRepository>()
        if (!settings.settings.first().account.signedIn) return Result.success()
        val error = koin.get<SyncEngine>().run()
        val now = settings.settings.first()
        val account = now.account
        if (shouldNotifyBackupProblem(account, now.notif.backupProblems, System.currentTimeMillis())) {
            notifyBackupPaused(applicationContext, account.backupError!!)
            settings.update { s -> s.copy(account = s.account.copy(problemNotified = true)) }
        }
        return when (error) {
            null -> Result.success()
            BackupError.REVOKED -> Result.failure()
            else -> Result.retry()
        }
    }
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    private fun current(): NetworkKind {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return when {
            caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkKind.NONE
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkKind.UNMETERED
            else -> NetworkKind.METERED
        }
    }

    override val network: Flow<NetworkKind> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
            override fun onLost(network: Network) { trySend(NetworkKind.NONE) }
        }
        cm.registerDefaultNetworkCallback(callback)
        trySend(current())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

class AndroidArchiveExporter(
    private val context: Context,
    private val days: DayRepository,
    private val films: FilmRepository,
    private val settings: SettingsRepository,
    private val projects: ProjectRepository,
) : ArchiveExporter {
    override suspend fun exportAll(destination: String, onProgress: (done: Int, total: Int) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val list = days.observeAll().first()
                val out = context.contentResolver.openOutputStream(Uri.parse(destination), "w")
                    ?: error("Cannot open destination")
                ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    list.forEachIndexed { i, day ->
                        val file = File(day.file)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("days/${day.date}.mp4"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                        onProgress(i + 1, list.size)
                    }
                    zip.putNextEntry(ZipEntry(MANIFEST_NAME))
                    zip.write(
                        buildManifest(settings.settings.first(), films.observeAll().first(), System.currentTimeMillis(), projects.active.first())
                            .encodeToByteArray(),
                    )
                    zip.closeEntry()
                }
            }
        }
}
