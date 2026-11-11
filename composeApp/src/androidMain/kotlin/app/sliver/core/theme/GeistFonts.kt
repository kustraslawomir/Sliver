package app.sliver.core.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private fun Context.font(name: String, weight: FontWeight): Font? =
    resources.getIdentifier(name, "font", packageName).takeIf { it != 0 }?.let { Font(it, weight) }

/** Wires res/font/geist_*.ttf by name; families that have no files keep the system fallback. */
fun installGeistFonts(context: Context) {
    val sans = listOfNotNull(
        context.font("geist_regular", FontWeight.Normal),
        context.font("geist_medium", FontWeight.Medium),
        context.font("geist_semibold", FontWeight.SemiBold),
    )
    val mono = listOfNotNull(context.font("geist_mono_medium", FontWeight.Medium))
    SliverFonts.install(sans.takeIf { it.isNotEmpty() }?.let { FontFamily(it) }, mono.takeIf { it.isNotEmpty() }?.let { FontFamily(it) })
}
