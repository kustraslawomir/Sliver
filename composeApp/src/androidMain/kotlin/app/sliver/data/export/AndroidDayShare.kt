package app.sliver.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@UnstableApi
class AndroidDayShare(private val context: Context) : DayShare {

    override suspend fun share(source: String, date: LocalDate, options: ShareOptions) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val out = File(dir, "sliver-$date.mp4")
        val (w, h) = withContext(Dispatchers.IO) { dimensions(source) }

        val effects = if (options.dateStamp || options.mark) {
            listOf(
                OverlayEffect(
                    ImmutableList.of<TextureOverlay>(BitmapOverlay.createStaticBitmapOverlay(overlayBitmap(w, h, date, options))),
                ),
            )
        } else {
            emptyList()
        }
        val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(File(source))))
            .setRemoveAudio(!options.sound)
            .setEffects(Effects(emptyList(), effects))
            .build()
        val composition = Composition.Builder(listOf(EditedMediaItemSequence.Builder(listOf(item)).build())).build()
        run(composition, out.absolutePath)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun overlayBitmap(w: Int, h: Int, date: LocalDate, options: ShareOptions): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val unit = minOf(w, h) / 1080f
        val margin = 48f * unit
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 36f * unit
        }
        if (options.dateStamp) {
            paint.color = 0xFFFFD2AE.toInt()
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(stampText(date), w - margin, margin + paint.textSize, paint)
        }
        if (options.mark) {
            paint.color = 0xB3EDEDE8.toInt()
            paint.textAlign = Paint.Align.LEFT
            paint.letterSpacing = 0.2f
            canvas.drawText("SLIVER", margin, h - margin, paint)
        }
        return bmp
    }

    private fun dimensions(path: String): Pair<Int, Int> {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(path)
            fun key(k: Int) = r.extractMetadata(k)?.toIntOrNull() ?: 0
            val w = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotated = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).let { it == 90 || it == 270 }
            return if (rotated) h to w else w to h
        } finally {
            r.release()
        }
    }

    private suspend fun run(composition: Composition, outPath: String) = withContext(Dispatchers.Main) {
        File(outPath).delete()
        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) = cont.resume(Unit)
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) =
                        cont.resumeWithException(exportException)
                })
                .build()
            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(composition, outPath)
        }
    }
}
