package app.sliver.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** System fallbacks until the platform installs Geist / Geist Mono (see HANDOFF.md). Install before first use of [SliverType]. */
object SliverFonts {
    var Sans: FontFamily = FontFamily.SansSerif
        private set
    var Mono: FontFamily = FontFamily.Monospace
        private set

    fun install(sans: FontFamily?, mono: FontFamily?) {
        sans?.let { Sans = it }
        mono?.let { Mono = it }
    }
}

object SliverType {
    // Weight 600 = Geist SemiBold; tracking per tokens.json.
    val DisplayXL = TextStyle(SliverColors.TextPrimary, 52.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 56.sp, letterSpacing = (-0.03).em)
    val DisplayL = TextStyle(SliverColors.TextPrimary, 36.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 40.sp, letterSpacing = (-0.025).em)
    val DisplayM = TextStyle(SliverColors.TextPrimary, 24.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 30.sp, letterSpacing = (-0.02).em)
    val Title = TextStyle(SliverColors.TextPrimary, 22.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 28.sp)
    val DisplayS = Title
    val Headline = TextStyle(SliverColors.TextPrimary, 17.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 24.sp)
    val Body = TextStyle(SliverColors.TextPrimary, 17.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 26.sp)
    val BodySmall = TextStyle(SliverColors.TextSecondary, 15.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 22.sp)
    val Caption = TextStyle(SliverColors.TextTertiary, 13.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 18.sp)
    val MonoL = TextStyle(SliverColors.TextPrimary, 15.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 20.sp, letterSpacing = 0.9.sp)
    val MonoS = TextStyle(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 16.sp, letterSpacing = 1.44.sp)
}
