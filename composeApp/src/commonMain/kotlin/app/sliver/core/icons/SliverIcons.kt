package app.sliver.core.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun lucide(name: String, vararg paths: String): ImageVector = designIcon(name, 1.75f, false, *paths)

/** Icon from the design's own SVG path data (24 viewBox); [stroke] is the SVG stroke-width, [filled] fills instead. */
private fun designIcon(name: String, stroke: Float, filled: Boolean, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                PathParser().parsePathString(d).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Project screen icons, copied from design/src/ProjectHub*.dc.html. */
object HubIcons {
    val Back = designIcon("hub-back", 1.6f, false, "M6 9l6 6 6-6")
    val Switcher = designIcon("hub-switcher", 2f, false, "M6 9l6 6 6-6")
    val Prev = designIcon("hub-prev", 1.8f, false, "M15 18l-6-6 6-6")
    val Next = designIcon("hub-next", 1.8f, false, "M9 18l6-6-6-6")
    val User = designIcon("hub-user", 1.6f, false, "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z")
    val Gear = designIcon(
        "hub-gear", 1.6f, false,
        "M12.2 2h-.4a2 2 0 0 0-2 2v.2a2 2 0 0 1-1 1.7l-.4.3a2 2 0 0 1-2 0l-.2-.1a2 2 0 0 0-2.7.7l-.2.4a2 2 0 0 0 .7 2.7l.2.1a2 2 0 0 1 1 1.7v.5a2 2 0 0 1-1 1.7l-.2.1a2 2 0 0 0-.7 2.7l.2.4a2 2 0 0 0 2.7.7l.2-.1a2 2 0 0 1 2 0l.4.3a2 2 0 0 1 1 1.7v.2a2 2 0 0 0 2 2h.4a2 2 0 0 0 2-2v-.2a2 2 0 0 1 1-1.7l.4-.3a2 2 0 0 1 2 0l.2.1a2 2 0 0 0 2.7-.7l.2-.4a2 2 0 0 0-.7-2.7l-.2-.1a2 2 0 0 1-1-1.7v-.5a2 2 0 0 1 1-1.7l.2-.1a2 2 0 0 0 .7-2.7l-.2-.4a2 2 0 0 0-2.7-.7l-.2.1a2 2 0 0 1-2 0l-.4-.3a2 2 0 0 1-1-1.7V4a2 2 0 0 0-2-2z",
        "M15 12a3 3 0 1 0-6 0 3 3 0 0 0 6 0z",
    )
    val More = designIcon(
        "hub-more", 0f, true,
        "M6.6 12a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0z", "M13.6 12a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0z", "M20.6 12a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0z",
    )
    val Bell = designIcon("hub-bell", 1.6f, false, "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.9 1.9 0 0 0 3.4 0")
    val Play = designIcon("hub-play", 0f, true, "M8 5v14l11-7z")
    val Plus = designIcon("hub-plus", 1.8f, false, "M12 5v14M5 12h14")
    val Rename = designIcon("hub-rename", 1.6f, false, "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val Shape = designIcon("hub-shape", 1.6f, false, "M3 7h18v10H3z")
    val FillDays = designIcon("hub-fill", 1.6f, false, "M3 5h18v16H3z M3 10h18 M8 3v4 M16 3v4 M12 13v5 M9.5 15.5h5")
    val Archive = designIcon("hub-archive", 1.6f, false, "M3 4h18v4H3z M5 8v12h14V8 M10 12h4")
    val Trash = designIcon("hub-trash", 1.6f, false, "M3 6h18 M8 6V4h8v2 M6 6l1 14h10l1-14")
    val Photos = designIcon(
        "hub-photos", 1.8f, false,
        "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M11 9a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M21 15l-3.1-3.1a2 2 0 0 0-2.8 0L6 21",
    )
}

object SliverIcons {
    val X = lucide("x", "M18 6 6 18", "m6 6 12 12")
    val Hash = lucide("hash", "M4 9h16", "M4 15h16", "M10 3 8 21", "M16 3l-2 18")
    val Ban = lucide("ban", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "m4.9 4.9 14.2 14.2")
    val ChevronDown = lucide("chevron-down", "m6 9 6 6 6-6")
    val ChevronLeft = lucide("chevron-left", "m15 18-6-6 6-6")
    val ChevronRight = lucide("chevron-right", "m9 18 6-6-6-6")
    val Volume2 = lucide("volume-2", "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "M16 9a5 5 0 0 1 0 6", "M19.364 18.364a9 9 0 0 0 0-12.728")
    val VolumeX = lucide("volume-x", "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "m22 9-6 6", "m16 9 6 6")
    val Grid3x3 = lucide("grid-3x3", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M3 9h18", "M3 15h18", "M9 3v18", "M15 3v18")
    val Layers = lucide("layers", "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z", "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12", "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17")
    val Scissors = lucide("scissors", "M6 6m-3 0a3 3 0 1 0 6 0 3 3 0 1 0-6 0", "M8.12 8.12 12 12", "M20 4 8.12 15.88", "M6 18m-3 0a3 3 0 1 0 6 0 3 3 0 1 0-6 0", "M14.8 14.8 20 20")
    val Timer = lucide("timer", "M10 2h4", "M12 14l3-3", "M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16z")
    val Zap = lucide("zap", "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z")
    val ZapOff = lucide("zap-off", "M10.513 4.856 13.12 2.17a.5.5 0 0 1 .86.46l-1.377 4.317", "M15.656 10H20a1 1 0 0 1 .78 1.63l-1.72 1.773", "M16.273 16.273 10.88 21.83a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14H4a1 1 0 0 1-.78-1.63l4.507-4.643", "m2 2 20 20")
    val AlertTriangle = lucide("triangle-alert", "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01")
    val SwitchCamera = lucide("refresh-ccw", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5")
    val SlidersHorizontal = lucide("sliders-horizontal", "M10 5H3", "M12 19H3", "M14 3v4", "M16 17v4", "M21 12h-9", "M21 19h-5", "M21 5h-7", "M8 10v4", "M8 12H3")
    val ImageIcon = lucide("image", "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M9 9m-2 0a2 2 0 1 0 4 0 2 2 0 1 0-4 0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21")
    val CircleDot = lucide("circle-dot", "M12 12m-10 0a10 10 0 1 0 20 0 10 10 0 1 0-20 0", "M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0")
    val VideoCam = lucide("video", "m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5", "M4 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z")
    val ImagePlus = lucide("image-plus", "M16 5h6", "M19 2v6", "M21 11.5V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7.5", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21", "M9 9m-2 0a2 2 0 1 0 4 0 2 2 0 1 0-4 0")
    val Calendar = lucide("calendar", "M8 2v4", "M16 2v4", "M3 6h18a0 0 0 0 1 0 0v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "M3 10h18")
    val Film = lucide("film", "M3 3h18v18H3z", "M7 3v18", "M3 7.5h4", "M3 12h18", "M3 16.5h4", "M17 3v18", "M17 7.5h4", "M17 16.5h4")
    val Settings = lucide("settings", "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915", "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z")
    val More = lucide("ellipsis", "M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M19 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M5 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0")
    val Sun = lucide("sun", "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41")
    val Pencil = lucide("pencil", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4")
    val MoreHorizontal = lucide("ellipsis", "M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M19 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M5 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0")
    val MoreVertical = lucide("ellipsis-vertical", "M12 12m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M12 5m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0", "M12 19m-1 0a1 1 0 1 0 2 0 1 1 0 1 0-2 0")
    val Maximize = lucide("maximize", "M4 9V4h5", "M20 9V4h-5", "M4 15v5h5", "M20 15v5h-5")
    val Check = lucide("check", "M20 6 9 17l-5-5")
    val Plus = lucide("plus", "M5 12h14", "M12 5v14")
    val Play = lucide("play", "M6 3 20 12 6 21z")
    val Pause = lucide("pause", "M14 4h4v16h-4z", "M6 4h4v16H6z")
    val Share = lucide("share", "M12 2v13", "m16 6-4-4-4 4", "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8")
    val Download = lucide("download", "M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5")
    val Trash2 = lucide("trash-2", "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
    val RotateCcw = lucide("rotate-ccw", "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5")
    val Smartphone = lucide("smartphone", "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M12 18h.01")
    val Folder = lucide("folder", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2z")
    val CloudUpload = lucide("cloud-upload", "M12 13v8", "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242", "m8 17 4-4 4 4")
    val Bell = lucide("bell", "M10.268 21a2 2 0 0 0 3.464 0", "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326")
}

@Composable
fun SliverIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Image(rememberVectorPainter(icon), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))
}
