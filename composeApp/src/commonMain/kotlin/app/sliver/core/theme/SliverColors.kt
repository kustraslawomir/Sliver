package app.sliver.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.sliver.core.theme.generated.Tokens

/** Two palettes generated from design/tokens.json (Tokens.DarkColors / LightColors); every read follows the active one. Hand-written extras below are not in tokens.json. */
object SliverColors {
    /** User choice. */
    var userLight by mutableStateOf(false)
    /** Camera, editing preview and player stay dark in both themes. */
    var forceDark by mutableStateOf(false)
    val light get() = userLight && !forceDark

    private val p get() = if (this.light) Tokens.LightColors else Tokens.DarkColors

    val BgVoid = Tokens.DarkColors.bgVoid
    val BgBase get() = p.bgBase
    val Surface1 get() = p.surface1
    val Surface2 get() = p.surface2
    val Surface3 get() = p.surface3
    val LineHairline get() = p.lineHairline
    val LineStrong get() = p.lineStrong
    val TextPrimary get() = p.textPrimary
    val TextSecondary get() = p.textSecondary
    val TextTertiary get() = p.textTertiary
    val TextDisabled get() = p.textDisabled
    /** v1.4 mono accent: primary fill, selected, switch-on. No hue. */
    val Accent get() = p.accent
    val OnAccent get() = p.onAccent
    val AccentText get() = Accent
    val AccentMark get() = Accent
    val AccentCore get() = Accent
    val AccentLine get() = p.accentLine
    val AccentTint get() = p.accentTint
    /** Red is only for recording: record button, "Record today", recording dot, timer after 1 s. */
    val Record = Tokens.RecordRed
    /** v1.7 record core gradient: light → base → deep. */
    val RecordLight = Color(0xFFFF6F63)
    val RecordDeep = Color(0xFFD9281F)
    /** Record button v2 core (design/motion/RecordButton.html). */
    val RecordCoreBase = Color(0xFFE3261C)
    val RecordLiquid = listOf(Color(0xFFFF2D55), Color(0xFFFF3B30), Color(0xFFFF6A3D), Color(0xFFE8261C), Color(0xFFFF4A6A), Color(0xFFFF2D55))
    val RecordSubsurface = Color(0xE6FF965F)
    val RecordEdge = Color(0x8C6E0000)
    val RecordRimWarm = Color(0x8CFF5A3C)
    val PhotoCoreLow = Color(0xFFCFCFC8)
    /** Photo mode disc of the record button: flat off-white. */
    val PhotoCore = Color(0xFFF2F2EE)
    /** Empty project thumbnail. */
    val ThumbEmpty = Color(0xFF1C1C1F)
    /** Settings group card (light: white). */
    val GroupCard get() = if (light) Color.White else Color(0xFF141416)
    /** v1.7 sheet scrim: nothing behind a sheet stays readable. */
    val SheetScrim get() = Color.Black.copy(alpha = if (light) 0.30f else 0.55f)
    /** Text on a TextPrimary-filled pill (selected segment, inverted button). */
    val OnInverse get() = p.onInverse
    val Success get() = p.success
    val Warning get() = p.warning
    // DECISION: light error stays #B5301F (AA contrast on surfaces); tokens.json themeLight.danger (#C0392B) is not used for text.
    val Error get() = if (this.light) Color(0xFFB5301F) else p.error
    val Media = Color.Black
    val OnMedia = Color.White
    val Scrim = Tokens.Scrim
    val CellEmpty get() = if (this.light) Color(0xFFECE8E0) else Color(0xFF1C1C20)
    val CellEmptyLine get() = if (this.light) Color(0xFFE2DED6) else Color(0xFF1F1F22)
    /** v1.7: no orange; "now" differs from idle by brightness and its label. */
    val GoldenNow = Color.White
    val GoldenIdle = Color(0xD9FFFFFF)
    /** Record button liquid glow (red while idle, white while recording). */
    val RecordGlowRed = listOf(Color(0xFFFF3B30), Color(0xFFFF7A45), Color(0xFFFF2D7A))
    val RecordGlowWhite = listOf(Color(0xFFFFFFFF), Color(0xFFBFD4FF), Color(0xFFFFE9D6))
    val PermBannerBg get() = Surface1
    val PermBannerBorder get() = LineStrong
    val PermBannerIcon get() = TextPrimary
    val PermBannerText get() = TextPrimary
    val NotifPreviewDark = Color(0xFF1C1C1F)
    val NotifThumbFrom = Color(0xFF8A6A48)
    val NotifThumbTo = Color(0xFF1A120B)
    val PosterTints = listOf(Color(0xFF2A2018), Color(0xFF34201A), Color(0xFF3A2E16))
}
