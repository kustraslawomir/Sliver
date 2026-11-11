package app.sliver.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import app.sliver.BuildConfig
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.Presentation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import app.sliver.data.FilmFormat
import app.sliver.data.LookParams
import app.sliver.data.Motion
import app.sliver.data.look.LookEffects
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@UnstableApi
class AndroidClipExporter(private val context: Context) : ClipExporter {

    override suspend fun exportSecond(
        source: String,
        startMs: Long,
        speed: Float,
        cropY: Float?,
        format: FilmFormat,
        videoPath: String,
        thumbPath: String,
        shortMode: ShortMode,
        look: LookParams,
    ): ExportedClip {
        val uri = if (source.contains("://")) Uri.parse(source) else Uri.fromFile(File(source))
        val info = withContext(Dispatchers.IO) { probe(uri) }
        val plan = ClipPlanner.plan(info.durationMs, startMs, speed, shortMode)
        val (outW, outH) = if (format == FilmFormat.STORY) 1080 to 1920 else 1920 to 1080

        val crop = ClipPlanner.cropRect(info.width.toFloat() / info.height, outW.toFloat() / outH, cropY)
        if (BuildConfig.DEBUG) withContext(Dispatchers.IO) { warnIfVariableFrameRate(uri) }
        val videoEffects = buildList<Effect> {
            if (info.fps > TARGET_FPS + 1f) add(FrameDropEffect.createDefaultFrameDropEffect(TARGET_FPS))
            add(Crop(crop.left, crop.right, crop.bottom, crop.top))
            add(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT))
            addAll(LookEffects.build(context, look, forExport = true))
        }
        fun item(mediaItem: MediaItem, itemSpeed: Float) = EditedMediaItem.Builder(mediaItem)
            .setEffects(
                Effects(
                    if (itemSpeed == 1f) emptyList<AudioProcessor>()
                    else listOf(SonicAudioProcessor().apply { setSpeed(itemSpeed) }),
                    if (itemSpeed == 1f) videoEffects else listOf(SpeedChangeEffect(itemSpeed)) + videoEffects,
                ),
            )
            .build()

        fun clipItem(endMs: Long) = item(
            MediaItem.Builder().setUri(uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(plan.startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            ).build(),
            plan.speed,
        )

        val items = mutableListOf<EditedMediaItem>()
        val padMs = (EXTRA_MS * plan.speed).toLong()
        val passMs = ((plan.endMs - plan.startMs) / plan.speed).toLong()
        if (plan.loops > 1) {
            repeat(plan.loops - 1) { items += clipItem(plan.endMs) }
            val lastOutMs = SECOND_MS - passMs * (plan.loops - 1)
            items += clipItem(plan.startMs + (lastOutMs * plan.speed).toLong())
        } else {
            // Some encoders lose the last frames of a clipped export; ask for a little extra and cut to exactly 1 s afterwards.
            val room = plan.holdMs == 0L && info.durationMs >= plan.endMs + padMs
            items += clipItem(if (room) plan.endMs + padMs else plan.endMs)
        }

        var holdFrame: File? = null
        if (plan.holdMs > 0) {
            holdFrame = File("$videoPath.hold.jpg")
            withContext(Dispatchers.IO) { writeLastFrame(uri, info.durationMs, holdFrame) }
            items += EditedMediaItem.Builder(
                MediaItem.Builder().setUri(Uri.fromFile(holdFrame)).setImageDurationMs(plan.holdMs).build(),
            ).setFrameRate(30).setEffects(Effects(emptyList(), videoEffects)).build()
        }

        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence.Builder(items).experimentalSetForceAudioTrack(true).build()),
        ).build()

        try {
            runTransformer(composition, videoPath)
        } finally {
            holdFrame?.delete()
        }
        val (duration, color) = withContext(Dispatchers.IO) {
            trimAudioToSecond(videoPath)
            val color = writeThumbnail(videoPath, thumbPath)
            probe(Uri.fromFile(File(videoPath))).durationMs to color
        }
        return ExportedClip(videoPath, thumbPath, duration, color)
    }

    override suspend fun photoToSecond(
        source: String,
        motion: Motion,
        format: FilmFormat,
        videoPath: String,
        thumbPath: String,
        look: LookParams,
    ): ExportedClip {
        val uri = if (source.contains("://")) Uri.parse(source) else Uri.fromFile(File(source))
        val (outW, outH) = if (format == FilmFormat.STORY) 1080 to 1920 else 1920 to 1080
        val effects = buildList<Effect> {
            add(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
            if (motion == Motion.ZOOM) {
                add(
                    MatrixTransformation { presentationTimeUs ->
                        val s = PhotoMotion.zoomScale(presentationTimeUs / (SECOND_MS * 1000f))
                        Matrix().apply { setScale(s, s) }
                    },
                )
            }
            addAll(LookEffects.build(context, look, forExport = true))
        }
        val item = EditedMediaItem.Builder(MediaItem.Builder().setUri(uri).setImageDurationMs(SECOND_MS).build())
            .setFrameRate(PhotoMotion.FPS)
            .setEffects(Effects(emptyList(), effects))
            .build()
        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence.Builder(listOf(item)).experimentalSetForceAudioTrack(true).build()),
        ).build()
        runTransformer(composition, videoPath)
        val (duration, color) = withContext(Dispatchers.IO) {
            trimAudioToSecond(videoPath)
            val color = writeThumbnail(videoPath, thumbPath)
            probe(Uri.fromFile(File(videoPath))).durationMs to color
        }
        return ExportedClip(videoPath, thumbPath, duration, color)
    }

    /** Cuts the file to exactly 1.000 s: drops AAC frames and video frames that start past the second. */
    private fun trimAudioToSecond(path: String) {
        val tmp = File("$path.tmp")
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(tmp.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            extractor.setDataSource(path)
            val rotation = MediaMetadataRetriever().let { r ->
                try {
                    r.setDataSource(path)
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                } finally {
                    r.release()
                }
            }
            muxer.setOrientationHint(rotation)
            val sampleRates = HashMap<Int, Int>()
            val isVideo = HashSet<Int>()
            val outTracks = HashMap<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    sampleRates[i] = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) isVideo += i
                outTracks[i] = muxer.addTrack(format)
                extractor.selectTrack(i)
            }
            muxer.start()
            val buffer = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            while (true) {
                buffer.clear()
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val track = extractor.sampleTrackIndex
                val rate = sampleRates[track]
                val inside = if (track in isVideo) extractor.sampleTime < SECOND_MS * 1000 else rate == null || AudioFit.keep(extractor.sampleTime, rate)
                if (inside) {
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    info.offset = 0
                    muxer.writeSampleData(outTracks.getValue(track), buffer, info)
                }
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            tmp.renameTo(File(path))
        } catch (e: Exception) {
            runCatching { muxer.release() }
            tmp.delete()
        } finally {
            extractor.release()
        }
    }

    private suspend fun runTransformer(composition: Composition, outPath: String) = withContext(Dispatchers.Main) {
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

    private class Info(val durationMs: Long, val width: Int, val height: Int, val fps: Float = 0f)

    private fun probe(uri: Uri): Info {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            fun key(k: Int) = r.extractMetadata(k)?.toIntOrNull() ?: 0
            val w = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotated = key(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).let { it == 90 || it == 270 }
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frames = if (Build.VERSION.SDK_INT >= 28) key(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT) else 0
            val fps = if (frames > 1 && dur > 0) frames * 1000f / dur else 0f
            return if (rotated) Info(dur, h, w, fps) else Info(dur, w, h, fps)
        } finally {
            r.release()
        }
    }

    private fun writeLastFrame(uri: Uri, durationMs: Long, out: File) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            val frame = r.getFrameAtTime((durationMs - 1).coerceAtLeast(0) * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: r.getFrameAtTime(0) ?: error("No frame in source")
            FileOutputStream(out).use { frame.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        } finally {
            r.release()
        }
    }

    /** Writes the JPEG thumbnail and returns its average color as opaque ARGB. */
    private fun writeThumbnail(videoPath: String, thumbPath: String): Int {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(videoPath)
            val frame = r.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST) ?: error("No frame in export")
            val thumb = Bitmap.createScaledBitmap(frame, THUMB_WIDTH, (THUMB_WIDTH.toFloat() * frame.height / frame.width).toInt().coerceAtLeast(1), true)
            FileOutputStream(thumbPath).use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            return Bitmap.createScaledBitmap(thumb, 1, 1, true).getPixel(0, 0) or (0xFF shl 24)
        } finally {
            r.release()
        }
    }

    /** Debug only: logs a warning when the source frame spacing is uneven (variable frame rate). */
    private fun warnIfVariableFrameRate(uri: Uri) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            } ?: return
            extractor.selectTrack(track)
            val times = ArrayList<Long>()
            while (times.size < 120 && extractor.sampleTime >= 0) {
                times += extractor.sampleTime
                extractor.advance()
            }
            if (isVariableFrameRate(times)) Log.w(TAG, "Variable frame rate source; normalised to ${TARGET_FPS.toInt()} fps on export")
        } catch (e: Exception) {
            Log.d(TAG, "VFR check skipped: ${e.message}")
        } finally {
            extractor.release()
        }
    }

    /** Debug check: duration in ms of an exported file (expected 1000 +- one frame). */
    fun measureDurationMs(path: String): Long = probe(Uri.fromFile(File(path))).durationMs

    private companion object {
        const val THUMB_WIDTH = 320
        const val TARGET_FPS = 30f
        const val EXTRA_MS = 100L
        const val TAG = "ClipExport"
    }
}
