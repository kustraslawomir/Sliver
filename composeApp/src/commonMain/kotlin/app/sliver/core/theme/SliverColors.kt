package app.sliver.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Two palettes from tokens.json (themeDarkMap / themeLight); every read follows the active one. */
object SliverColors {
    /** User choice. */
    var userLight by mutableStateOf(false)
    /** Camera, editing preview and player stay dark in both themes. */
    var forceDark by mutableStateOf(false)
    val light get() = userLight && !forceDark

    private fun pick(dark: Long, light: Long) = Color(if (this.light) light else dark)

    val BgVoid = Color(0xFF050505)
    val BgBase get() = pick(0xFF0A0A0B, 0xFFF6F4EF)
    val Surface1 get() = pick(0xFF111113, 0xFFFFFFFF)
    val Surface2 get() = pick(0xFF18181B, 0xFFECE8E0)
    val Surface3 get() = pick(0xFF202024, 0xFFE2DED6)
    val LineHairline get() = pick(0xFF26262A, 0xFFE2DED6)
    val LineStrong get() = pick(0xFF3A3A40, 0xFFCFCAC0)
    val TextPrimary get() = pick(0xFFEDEDE8, 0xFF141413)
    val TextSecondary get() = pick(0xFFB9B9B3, 0xFF4A4843)
    val TextTertiary get() = pick(0xFF8E8E88, 0xFF6E6B64)
    val TextDisabled get() = pick(0xFF55555A, 0xFFA8A49B)
    /** Ember fill (buttons, record dot, selected borders). */
    val Accent = Color(0xFFFF8A3D)
    /** Ember as text or icon on the page: emberOnLight in the light theme. */
    val AccentText get() = pick(0xFFFF8A3D, 0xFFC2540F)
    val AccentCore get() = pick(0xFFFFD2AE, 0xFFE8671C)
    val AccentLine get() = pick(0x55FF8A3D, 0x55E8671C)
    val AccentTint get() = pick(0x1FFF8A3D, 0x1FE8671C)
    val OnAccent = Color(0xFF0A0A0B)
    val Success get() = pick(0xFF8FC7A4, 0xFF2F7D55)
    val Warning get() = pick(0xFFE8C27A, 0xFF9A6A0B)
    val Error get() = pick(0xFFE5675F, 0xFFC0392B)
    val Media = Color.Black
    val OnMedia = Color.White
    val Scrim = Color(0xB3000000)
    val CellEmpty get() = pick(0xFF1C1C20, 0xFFECE8E0)
    val CellEmptyLine get() = pick(0xFF1F1F22, 0xFFE2DED6)
    val PosterTints = listOf(Color(0xFF2A2018), Color(0xFF34201A), Color(0xFF3A2E16))
}
