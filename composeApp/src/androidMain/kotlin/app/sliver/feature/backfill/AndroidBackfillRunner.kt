package app.sliver.feature.backfill

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.mp.KoinPlatform

private const val WORK_NAME = "backfill"

class AndroidBackfillRunner(private val context: Context) : BackfillRunner {
    override fun start() {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<BackfillWorker>().build())
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/** Progress lives in [BackfillStore], so an interrupted run resumes where it stopped. */
class BackfillWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        KoinPlatform.getKoin().get<BackfillExecutor>().run()
        return Result.success()
    }
}
