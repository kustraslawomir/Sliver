package app.sliver.data.repo

import android.content.Context
import android.location.Geocoder
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlaceResolver(private val context: Context) : PlaceResolver {
    override suspend fun resolve(source: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val (lat, lon) = location(source) ?: return@runCatching null
            if (!Geocoder.isPresent()) return@runCatching null
            @Suppress("DEPRECATION")
            Geocoder(context).getFromLocation(lat, lon, 1)?.firstOrNull()
                ?.let { it.locality ?: it.subAdminArea ?: it.adminArea ?: it.countryName }
        }.getOrNull()
    }

    private fun location(source: String): Pair<Double, Double>? {
        val uri = if (source.contains("://")) Uri.parse(source) else Uri.fromFile(File(source))
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)?.let { iso ->
                Regex("([+-]\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)").find(iso)?.let {
                    return it.groupValues[1].toDouble() to it.groupValues[2].toDouble()
                }
            }
        } catch (_: Exception) {
        } finally {
            r.release()
        }
        return context.contentResolver.openInputStream(uri)?.use { s ->
            val out = FloatArray(2)
            if (ExifInterface(s).getLatLong(out)) out[0].toDouble() to out[1].toDouble() else null
        }
    }
}
