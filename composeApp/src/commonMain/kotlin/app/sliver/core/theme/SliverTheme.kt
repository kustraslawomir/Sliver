package app.sliver.core.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import sliver.composeapp.generated.resources.Res
import sliver.composeapp.generated.resources.geist_medium
import sliver.composeapp.generated.resources.geist_mono_medium
import sliver.composeapp.generated.resources.geist_mono_regular
import sliver.composeapp.generated.resources.geist_regular
import sliver.composeapp.generated.resources.geist_semibold

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun SliverTheme(reducedMotion: Boolean = false, light: Boolean = false, content: @Composable () -> Unit) {
    SliverColors.userLight = light
    SystemBarsEffect(SliverColors.light)
    val sans = FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semibold, FontWeight.SemiBold),
    )
    val mono = FontFamily(
        Font(Res.font.geist_mono_regular, FontWeight.Normal),
        Font(Res.font.geist_mono_medium, FontWeight.Medium),
    )
    SliverFonts.Sans = sans
    SliverFonts.Mono = mono
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion, content = content)
}

@Composable
fun SliverText(
    text: String,
    style: TextStyle = SliverType.Body,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
) {
    BasicText(
        text,
        modifier,
        if (color == Color.Unspecified) style else style.copy(color = color),
        softWrap = softWrap,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun SliverText(
    text: androidx.compose.ui.text.AnnotatedString,
    style: TextStyle = SliverType.Body,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(text, modifier, if (color == Color.Unspecified) style else style.copy(color = color), maxLines = maxLines)
}
