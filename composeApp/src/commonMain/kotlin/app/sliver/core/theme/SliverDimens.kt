package app.sliver.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import app.sliver.core.theme.generated.Tokens

object SliverSpace {
    val S1 = Tokens.Space1.dp
    val S2 = Tokens.Space2.dp
    val S3 = Tokens.Space3.dp
    val S4 = Tokens.Space4.dp
    val S5 = Tokens.Space5.dp
    val S6 = Tokens.Space6.dp
    val S7 = Tokens.Space7.dp
    val S8 = Tokens.Space8.dp
    val S9 = Tokens.Space9.dp
    val S10 = Tokens.Space10.dp
    val Gutter = 20.dp
}

object SliverRadius {
    val Xs = Tokens.RadiusXs.dp
    val S = Tokens.RadiusS.dp
    val M = Tokens.RadiusM.dp
    val L = Tokens.RadiusL.dp
    val Xl = Tokens.RadiusXl.dp
    val Pill = Tokens.RadiusPill.dp
    val R12 = 12.dp
    val R14 = 14.dp
    val R18 = 18.dp
    val R20 = 20.dp
}

object SliverShapes {
    val Xs = RoundedCornerShape(SliverRadius.Xs)
    val S = RoundedCornerShape(SliverRadius.S)
    val M = RoundedCornerShape(SliverRadius.M)
    val L = RoundedCornerShape(SliverRadius.L)
    val Pill = RoundedCornerShape(SliverRadius.Pill)
    val R12 = RoundedCornerShape(SliverRadius.R12)
    val R14 = RoundedCornerShape(SliverRadius.R14)
    val R18 = RoundedCornerShape(SliverRadius.R18)
    val R20 = RoundedCornerShape(SliverRadius.R20)
    val SheetTop = RoundedCornerShape(topStart = SliverRadius.Xl, topEnd = SliverRadius.Xl)
}
