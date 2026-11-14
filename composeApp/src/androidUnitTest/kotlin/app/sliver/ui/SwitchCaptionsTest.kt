package app.sliver.ui

import app.sliver.feature.settings.SettingsCaptions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Audit v1.8 §1: a switch subtitle describes what the switch does, so it never names one state ("On:"/"Off:"). */
class SwitchCaptionsTest {
    private val stateWords = Regex("""^(On|Off)\b""")

    @Test
    fun settingsCaptionsNeverNameOneState() {
        SettingsCaptions.all.forEach { assertTrue(!stateWords.containsMatchIn(it), it) }
    }

    @Test
    fun noSwitchCaptionInTheAppStartsWithOnOrOff() {
        val bad = File("src/commonMain").walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            Regex("""caption\s*=\s*"((?:On|Off):[^"]*)"""").findAll(f.readText()).map { "${f.name}: ${it.groupValues[1]}" }
        }.toList()
        assertTrue(bad.isEmpty(), bad.joinToString("\n"))
    }
}
