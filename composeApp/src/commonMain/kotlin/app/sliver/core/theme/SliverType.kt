package app.sliver.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Swap these for bundled Instrument Serif / Geist / Geist Mono resources (see HANDOFF.md). */
object SliverFonts {
    val Serif: FontFamily = FontFamily.Serif
    val Sans: FontFamily = FontFamily.SansSerif
    val Mono: FontFamily = FontFamily.Monospace
}

object SliverType {
    val DisplayXL = TextStyle(SliverColors.TextPrimary, 64.sp, FontWeight.Normal, fontFamily = SliverFonts.Serif, lineHeight = 64.sp)
    val DisplayL = TextStyle(SliverColors.TextPrimary, 44.sp, FontWeight.Normal, fontFamily = SliverFonts.Serif, lineHeight = 48.sp)
    val DisplayM = TextStyle(SliverColors.TextPrimary, 30.sp, FontWeight.Normal, fontFamily = SliverFonts.Serif, lineHeight = 34.sp)
    val Title = TextStyle(SliverColors.TextPrimary, 22.sp, FontWeight.SemiBold, fontFamily = SliverFonts.Sans, lineHeight = 28.sp)
    val Headline = TextStyle(SliverColors.TextPrimary, 17.sp, FontWeight.Medium, fontFamily = SliverFonts.Sans, lineHeight = 24.sp)
    val Body = TextStyle(SliverColors.TextPrimary, 17.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 26.sp)
    val BodySmall = TextStyle(SliverColors.TextSecondary, 15.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 22.sp)
    val Caption = TextStyle(SliverColors.TextTertiary, 13.sp, FontWeight.Normal, fontFamily = SliverFonts.Sans, lineHeight = 18.sp)
    val MonoL = TextStyle(SliverColors.TextPrimary, 15.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 20.sp, letterSpacing = 0.9.sp)
    val MonoS = TextStyle(SliverColors.TextSecondary, 12.sp, FontWeight.Medium, fontFamily = SliverFonts.Mono, lineHeight = 16.sp, letterSpacing = 1.44.sp)
}
