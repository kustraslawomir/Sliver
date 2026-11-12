package app.sliver.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import app.sliver.core.AppScope
import app.sliver.feature.films.FilmRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.context.GlobalContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock

private const val TAG = "SliverSeed"
const val ACTION_SEED = "app.sliver.debug.SEED"

private val seedLock = Mutex()

private class PrefsSeedStore(context: Context) : SeedStore {
    private val prefs = context.getSharedPreferences("debug_seed", Context.MODE_PRIVATE)
    override fun load() = SeedState(
        prefs.getString("projects", "").orEmpty().split(',').filter { it.isNotEmpty() },
        prefs.getString("previous", null),
    )

    override fun save(state: SeedState) {
        prefs.edit().putString("projects", state.projectIds.joinToString(",")).putString("previous", state.previousActive).commit()
    }
}

/** Six 2 s gradient videos, each a different colour and brightness, made from generated bitmaps; cached in cacheDir/seed. */
@OptIn(UnstableApi::class)
private class GradientSources(private val context: Context) : SeedSources {
    private val dir = File(context.cacheDir, "seed").apply { mkdirs() }

    override suspend fun source(variant: Int): String {
        val video = File(dir, "src$variant.mp4")
        if (video.length() > 0) return video.path
        val image = File(dir, "src$variant.jpg")
        withContext(Dispatchers.IO) {
            val hue = variant * 60f + 10f
            val value = floatArrayOf(0.35f, 0.9f, 0.55f, 0.75f, 0.45f, 1f)[variant % SEED_VARIANTS]
            val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 1280f, 720f,
                    Color.HSVToColor(floatArrayOf(hue, 0.7f, value)), Color.HSVToColor(floatArrayOf((hue + 40f) % 360f, 0.5f, value * 0.45f)),
                    Shader.TileMode.CLAMP,
                )
            }
            Canvas(bmp).drawRect(0f, 0f, 1280f, 720f, paint)
            image.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        val item = EditedMediaItem.Builder(MediaItem.Builder().setUri(Uri.fromFile(image)).setImageDurationMs(2000).build())
            .setFrameRate(30).build()
        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence.Builder(listOf(item)).experimentalSetForceAudioTrack(true).build()),
        ).build()
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val transformer = Transformer.Builder(context).addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) = cont.resume(Unit)
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) =
                        cont.resumeWithException(exportException)
                }).build()
                cont.invokeOnCancellation { transformer.cancel() }
                transformer.start(composition, video.path)
            }
        }
        return video.path
    }
}

fun runSeed(context: Context, mode: String, days: Int, project: String, includeToday: Boolean) {
    AppScope.launch {
        if (!seedLock.tryLock()) {
            Log.w(TAG, "already running")
            return@launch
        }
        try {
            val koin = GlobalContext.get()
            val renderer = koin.get<FilmRenderer>()
            val seeder = Seeder(
                koin.get(), koin.get(), koin.get(), GradientSources(context), PrefsSeedStore(context),
                renderer::start, { Log.i(TAG, it) },
            )
            if (mode == "wipe") seeder.wipe()
            else seeder.seed(Clock.System.todayIn(TimeZone.currentSystemDefault()), TimeZone.currentSystemDefault(), days, project, includeToday)
            Log.i(TAG, "done $mode")
        } catch (t: Throwable) {
            Log.e(TAG, "failed $mode", t)
        } finally {
            seedLock.unlock()
        }
    }
}

/** `adb shell am broadcast -n skustra.fragments/app.sliver.debug.SeedReceiver -a app.sliver.debug.SEED --ei days 365 --es project "My 2026" [--es mode wipe] [--ez today true]` */
class SeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runSeed(
            context.applicationContext, intent.getStringExtra("mode") ?: "seed", intent.getIntExtra("days", 365),
            intent.getStringExtra("project") ?: "My 2026", intent.getBooleanExtra("today", false),
        )
    }
}
