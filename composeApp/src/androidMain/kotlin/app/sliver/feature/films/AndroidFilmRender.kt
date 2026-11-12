package app.sliver.feature.films

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import app.sliver.data.export.stampAnchor
import app.sliver.data.export.stampTextPx
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.OverlaySettings
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.GaussianBlur
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.sliver.data.FilmAudio
import app.sliver.data.FilmFormat
import app.sliver.data.Mismatch
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.export.ClipPlanner
import app.sliver.data.files.FileStore
import app.sliver.data.look.LookEffects
import app.sliver.data.repo.DayRepository
import app.sliver.data.repo.FilmRepository
import app.sliver.data.settings.SettingsRepository
import app.sliver.data.toParams
import app.sliver.feature.notifications.DeepLinkTarget
import app.sliver.feature.notifications.ProgressNotifier
import app.sliver.feature.notifications.notifyFilmReady
import app.sliver.feature.notifications.progressDue
import com.google.common.collect.ImmutableList
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

private const val KEY_FILM = "film"
private const val KEY_PERCENT = "percent"
private const val CHANNEL = "film_render"
private const val NOTIFICATION_ID = 4204

private fun workName(filmId: String) = "film-render-$filmId"

class AndroidFilmRenderer(private val context: Context) : FilmRenderer {
    override fun start(filmId: String) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(filmId), ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<FilmRenderWorker>().setInputData(workDataOf(KEY_FILM to filmId)).build(),
        )
    }

    override fun cancel(filmId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(filmId))
    }

    override fun state(filmId: String): Flow<RenderState> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(workName(filmId)).map { infos ->
            val info = infos.lastOrNull()
            when (info?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> RenderState.Running(0)
                WorkInfo.State.RUNNING -> RenderState.Running(info.progress.getInt(KEY_PERCENT, 0))
                WorkInfo.State.FAILED -> RenderState.Failed
                else -> RenderState.Idle
            }
        }
}

/** Foreground job: survives leaving the app. A failed render leaves the film untouched; Try again restarts it. */
@androidx.annotation.OptIn(UnstableApi::class)
class FilmRenderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private var notifier: ProgressNotifier? = null
    private var months = 1

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground(0)

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_FILM) ?: return Result.failure()
        notifier = ProgressNotifier(applicationContext, NOTIFICATION_ID, DeepLinkTarget.Film(id), CHANNEL, "Film rendering")
        setForeground(foreground(0))
        val koin = KoinPlatform.getKoin()
        months = runCatching {
            val film = koin.get<FilmRepository>().getFilm(id)
            film?.plan(koin.get<DayRepository>().observeAll().first())?.days?.map { it.date.year * 12 + it.date.monthNumber }?.distinct()?.size
        }.getOrNull()?.coerceIn(1, 36) ?: 1
        val composer = FilmComposer(applicationContext, koin.get(), koin.get(), koin.get())
        return try {
            val ok = composer.render(id) { percent ->
                setProgress(Data.Builder().putInt(KEY_PERCENT, percent).build())
                if (progressDue(lastForeground, System.currentTimeMillis())) setForeground(foreground(percent))
            }
            if (ok) {
                announceReady(id)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun announceReady(id: String) {
        val koin = KoinPlatform.getKoin()
        if (!koin.get<SettingsRepository>().settings.first().notif.filmReady) return
        val film = koin.get<FilmRepository>().getFilm(id) ?: return
        val seconds = film.plan(koin.get<DayRepository>().observeAll().first()).days.size
        notifyFilmReady(applicationContext, id, film.name, seconds)
    }

    private var lastForeground: Long? = null

    private fun foreground(percent: Int): ForegroundInfo {
        lastForeground = System.currentTimeMillis()
        val n = notifier ?: ProgressNotifier(applicationContext, NOTIFICATION_ID, DeepLinkTarget.Camera, CHANNEL, "Film rendering")
        val notification = n.build("Rendering your film", "$percent%", percent, 100, months)
        val type = if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        return ForegroundInfo(NOTIFICATION_ID, notification, type)
    }
}

@UnstableApi
class FilmComposer(
    private val context: Context,
    private val films: FilmRepository,
    private val days: DayRepository,
    private val files: FileStore,
) {
    private class Clip(val day: Day, val width: Int, val height: Int, val durationMs: Long) {
        val aspect get() = width.toFloat() / height
    }

    /** Renders to `films/<id>.mp4` through a tmp file so a failed run never leaves a broken film behind. */
    suspend fun render(filmId: String, onProgress: suspend (Int) -> Unit): Boolean {
        val film = films.getFilm(filmId) ?: return false
        val plan = film.plan(days.observeAll().first())
        val clips = withContext(Dispatchers.IO) {
            plan.days.filter { files.exists(it.file) }.mapNotNull { probe(it) }
        }
        if (clips.isEmpty()) return false
        val (outW, outH) = if (film.format == FilmFormat.STORY) 1080 to 1920 else 1920 to 1080

        val card = File(context.cacheDir, "film-card-$filmId.png")
        val tmp = File(context.cacheDir, "film-$filmId.mp4")
        try {
            withContext(Dispatchers.IO) { writeTitleCard(card, film, plan, outW, outH) }
            val composition = compose(film, clips, card, outW, outH)
            runTransformer(composition, tmp.path, onProgress)
            val out = files.filmPath(filmId)
            files.delete(out)
            check(files.move(tmp.path, out)) { "Could not move film into place" }
            films.save(film.copy(renderPath = out, renderedAt = Clock.System.now()))
            return true
        } finally {
            card.delete()
            tmp.delete()
        }
    }

    private fun compose(film: Film, clips: List<Clip>, card: File, outW: Int, outH: Int): Composition {
        val outAspect = outW.toFloat() / outH
        val audio = audioPlan(film.audio, film.track?.takeIf { trackAvailable(it) })
        val blur = film.mismatch == Mismatch.BLUR && clips.any { !aspectMatches(it.aspect, outAspect) }
        val filmLook = film.look?.let { LookEffects.build(context, it.toParams(), forExport = true) }.orEmpty()

        fun dip(): Effect = RgbMatrix { presentationTimeUs, _ ->
            val f = dipFactor(film.cut, presentationTimeUs / 1000)
            floatArrayOf(f, 0f, 0f, 0f, 0f, f, 0f, 0f, 0f, 0f, f, 0f, 0f, 0f, 0f, 1f)
        }

        fun stamp(date: kotlinx.datetime.LocalDate): List<Effect> {
            if (!film.dateOn) return emptyList()
            val size = stampTextPx(outW, outH)
            val text = SpannableString(dateStamp(date)).apply {
                setSpan(ForegroundColorSpan(Color.WHITE), 0, length, 0)
                setSpan(AbsoluteSizeSpan(size, false), 0, length, 0)
                stampTypeface?.let { setSpan(TypefaceSpan(it), 0, length, 0) }
                setSpan(StampShadow(size / 3f), 0, length, 0)
            }
            val (ax, ay) = stampAnchor(outW, outH)
            val settings = StaticOverlaySettings.Builder()
                .setBackgroundFrameAnchor(ax, ay)
                .setOverlayFrameAnchor(1f, -1f)
                .build()
            return listOf(OverlayEffect(ImmutableList.of<TextureOverlay>(TextOverlay.createStaticTextOverlay(text, settings))))
        }

        fun clipItem(clip: Clip, effects: List<Effect>, withAudio: Boolean): EditedMediaItem =
            EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(File(clip.day.file))))
                .setRemoveAudio(!withAudio || !audio.keepClipAudio || !clip.day.keepAudio)
                .setEffects(Effects(emptyList<AudioProcessor>(), effects))
                .build()

        val cardItem = EditedMediaItem.Builder(
            MediaItem.Builder().setUri(Uri.fromFile(card)).setImageDurationMs(FILM_TITLE_MS).build(),
        ).setFrameRate(30).setEffects(
            Effects(emptyList(), listOf(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT))),
        ).build()

        val sequences = mutableListOf<EditedMediaItemSequence>()
        if (blur) {
            val background = clips.map { clip ->
                clipItem(
                    clip,
                    listOf(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP), GaussianBlur(24f), dip()),
                    withAudio = false,
                )
            } + cardItem
            sequences += EditedMediaItemSequence.Builder(background).build()
        }
        val main = clips.map { clip ->
            val effects = buildList<Effect> {
                if (!blur) {
                    val crop = ClipPlanner.cropRect(clip.aspect, outAspect, clip.day.cropY)
                    add(Crop(crop.left, crop.right, crop.bottom, crop.top))
                    add(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT))
                }
                addAll(filmLook)
                addAll(stamp(clip.day.date))
                add(dip())
            }
            clipItem(clip, effects, withAudio = true)
        } + cardItem
        sequences += EditedMediaItemSequence.Builder(main)
            .experimentalSetForceAudioTrack(audio.keepClipAudio || !audio.music).build()

        if (audio.music) {
            val ref = checkNotNull(film.track)
            val uri = if (ref.startsWith(ASSET_PREFIX)) Uri.parse("asset:///" + ref.removePrefix(ASSET_PREFIX)) else Uri.parse(ref)
            val volume = ChannelMixingAudioProcessor().apply {
                if (audio.musicVolume != 1f) {
                    putChannelMixingMatrix(ChannelMixingMatrix.create(1, 1).scaleBy(audio.musicVolume))
                    putChannelMixingMatrix(ChannelMixingMatrix.create(2, 2).scaleBy(audio.musicVolume))
                }
            }
            val music = EditedMediaItem.Builder(MediaItem.fromUri(uri))
                .setRemoveVideo(true)
                .setEffects(Effects(if (audio.musicVolume != 1f) listOf<AudioProcessor>(volume) else emptyList(), emptyList()))
                .build()
            sequences += EditedMediaItemSequence.Builder(music).setIsLooping(true).build()
        }

        val builder = Composition.Builder(sequences)
        if (blur) builder.setVideoCompositorSettings(fitCompositor(clips, outW, outH))
        return builder.build()
    }

    private val stampTypeface: Typeface? by lazy {
        runCatching { Typeface.createFromAsset(context.assets, "composeResources/sliver.composeapp.generated.resources/font/geist_mono_medium.ttf") }.getOrNull()
    }

    private class StampShadow(private val radius: Float) : CharacterStyle() {
        override fun updateDrawState(tp: TextPaint) = tp.setShadowLayer(radius, 0f, 0f, 0x99000000.toInt())
    }

    /** A built-in track whose asset is missing is skipped (the film keeps its original sound) instead of failing the render. */
    private fun trackAvailable(ref: String): Boolean {
        if (!ref.startsWith(ASSET_PREFIX)) return true
        val path = ref.removePrefix(ASSET_PREFIX)
        val present = runCatching { context.assets.open(path).close() }.isSuccess
        if (!present) Log.w("FilmComposer", "Music asset $path missing, rendering without music")
        return present
    }

    /** Keeps each day's video letterboxed over the blurred background; the day under [presentationTimeUs] sets the scale. */
    private fun fitCompositor(clips: List<Clip>, outW: Int, outH: Int): VideoCompositorSettings {
        val outAspect = outW.toFloat() / outH
        val ends = clips.runningFold(0L) { acc, c -> acc + c.durationMs }.drop(1)
        val scales = clips.map { fitScale(it.aspect, outAspect) }
        return object : VideoCompositorSettings {
            override fun getOutputSize(inputSizes: List<Size>) = Size(outW, outH)

            override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
                if (inputId == 0) return StaticOverlaySettings.Builder().build()
                val i = ends.indexOfFirst { presentationTimeUs / 1000 < it }
                val (sx, sy) = if (i < 0) 1f to 1f else scales[i]
                return StaticOverlaySettings.Builder().setScale(sx, sy).build()
            }
        }
    }

    private suspend fun runTransformer(composition: Composition, outPath: String, onProgress: suspend (Int) -> Unit) =
        withContext(Dispatchers.Main) {
            File(outPath).delete()
            coroutineScope {
                var transformer: Transformer? = null
                val poller = launch {
                    val holder = ProgressHolder()
                    while (isActive) {
                        delay(1000)
                        val t = transformer ?: continue
                        if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                    }
                }
                try {
                    suspendCancellableCoroutine { cont ->
                        val t = Transformer.Builder(context)
                            .setVideoMimeType(MimeTypes.VIDEO_H264)
                            .setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, exportResult: ExportResult) = cont.resume(Unit)
                                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) =
                                    cont.resumeWithException(exportException)
                            })
                            .build()
                        transformer = t
                        cont.invokeOnCancellation { t.cancel() }
                        t.start(composition, outPath)
                    }
                } finally {
                    poller.cancel()
                }
            }
        }

    private fun probe(day: Day): Clip? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(day.file)
            fun key(k: Int) = r.extractMetadata(k)?.toIntOrNull() ?: 0
            val w = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotated = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).let { it == 90 || it == 270 }
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: FILM_FRAME_MS
            if (w == 0 || h == 0) null else if (rotated) Clip(day, h, w, dur) else Clip(day, w, h, dur)
        } catch (e: RuntimeException) {
            null
        } finally {
            r.release()
        }
    }

    private fun writeTitleCard(file: File, film: Film, plan: FilmPlan, w: Int, h: Int) {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val unit = minOf(w, h) / 1080f
        fun line(text: String, size: Float, y: Float, color: Int, bold: Boolean = false) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size * unit
                textAlign = Paint.Align.CENTER
                typeface = if (bold) Typeface.create(Typeface.SERIF, Typeface.BOLD) else Typeface.MONOSPACE
                letterSpacing = if (bold) 0f else 0.15f
            }
            canvas.drawText(text, w / 2f, h * y, paint)
        }
        line("A FILM BY YOU", 28f, 0.42f, 0xFFA8A29E.toInt())
        line(film.name, 96f, 0.52f, Color.WHITE, bold = true)
        line("${film.rangeText()} · ${plan.clips} CLIPS · ${formatRuntime(plan.clips)}", 28f, 0.60f, 0xFFA8A29E.toInt())
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
