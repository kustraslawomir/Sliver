package app.sliver

import androidx.compose.ui.graphics.Color
import app.sliver.core.theme.generated.Tokens
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TokensGeneratedTest {
    private val root: JsonObject = Json.parseToJsonElement(File("../design/tokens.json").readText()).jsonObject

    private fun hex(path: List<String>): Color {
        var o: JsonObject = root
        path.dropLast(1).forEach { o = o.getValue(it).jsonObject }
        val h = o.getValue(path.last()).jsonPrimitive.content.removePrefix("#")
        val v = h.toLong(16)
        return Color(if (h.length == 6) 0xFF000000 or v else ((v and 0xFF) shl 24) or (v shr 8))
    }

    @Test fun darkPalette() {
        assertEquals(hex(listOf("color", "bgBase")), Tokens.DarkColors.bgBase)
        assertEquals(hex(listOf("color", "textPrimary")), Tokens.DarkColors.textPrimary)
        assertEquals(hex(listOf("accent", "dark", "primaryFill")), Tokens.DarkColors.accent)
        assertEquals(hex(listOf("color", "scrim")), Tokens.Scrim)
    }

    @Test fun lightPalette() {
        assertEquals(hex(listOf("themeLight", "bg")), Tokens.LightColors.bgBase)
        assertEquals(hex(listOf("themeLight", "muted")), Tokens.LightColors.textTertiary)
        assertEquals(hex(listOf("accent", "light", "primaryFill")), Tokens.LightColors.accent)
        assertEquals(hex(listOf("accent", "record")), Tokens.RecordRed)
    }

    @Test fun scales() {
        assertEquals(root.getValue("type").jsonObject.getValue("displayXL").jsonObject.getValue("size").jsonPrimitive.int, Tokens.DisplayXL.size)
        assertEquals(root.getValue("radius").jsonObject.getValue("pill").jsonPrimitive.int, Tokens.RadiusPill)
    }
}
