package app.sliver.feature.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlin.math.round

/** Coarse last-known location only (no GPS fix), cached rounded to 0.1 degree so nothing precise is stored. */
class AndroidGoldenHour(private val context: Context) : GoldenHourProvider {
    private val prefs = context.getSharedPreferences("golden_hour", Context.MODE_PRIVATE)

    private fun allowed() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun location(): Pair<Double, Double>? {
        if (!allowed() || !prefs.contains("lat")) return null
        return prefs.getFloat("lat", 0f).toDouble() to prefs.getFloat("lon", 0f).toDouble()
    }

    @Suppress("MissingPermission")
    override fun refresh() {
        if (!allowed()) return
        val lm = context.getSystemService(LocationManager::class.java)
        val last = runCatching {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull() ?: return
        prefs.edit().putFloat("lat", (round(last.latitude * 10) / 10).toFloat()).putFloat("lon", (round(last.longitude * 10) / 10).toFloat()).apply()
    }
}
