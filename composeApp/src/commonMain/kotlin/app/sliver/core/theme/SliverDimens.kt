package app.sliver.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object SliverSpace {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S5 = 20.dp
    val S6 = 24.dp
    val S7 = 32.dp
    val S8 = 40.dp
    val S9 = 56.dp
    val S10 = 72.dp
    val Gutter = 20.dp
}

object SliverRadius {
    val Xs = 4.dp
    val S = 10.dp
    val M = 16.dp
    val L = 24.dp
    val Xl = 32.dp
    val Pill = 999.dp
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
