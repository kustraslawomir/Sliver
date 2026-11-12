package app.sliver.data

import app.sliver.data.look.LutGenerator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LutFilesTest {
    private val dir = File("src/androidMain/assets/luts")

    private val presets = FilterPresets.map { it.info.lutFile!!.substringAfterLast('/').removeSuffix(".cube") to it.info.model }

    // Regenerate: ./gradlew :composeApp:testDebugUnitTest --tests '*LutFilesTest*' -PgenLuts=1
    @Test
    fun generateAndValidate() {
        if (System.getProperty("genLuts") != null) {
            dir.mkdirs()
            presets.forEach { (n, m) -> File(dir, "$n.cube").writeText(LutGenerator.cube(n, m)) }
        }
        assertEquals(6, presets.size)
        for ((name, model) in presets) {
            assertTrue(model.hasLut, "$name has no colour part")
            val file = File(dir, "$name.cube")
            assertTrue(file.exists(), "missing $name.cube")
            val lines = file.readLines().filter { it.isNotBlank() }
            assertEquals("TITLE \"$name\"", lines[0])
            assertEquals("LUT_3D_SIZE 33", lines[1])
            assertEquals("DOMAIN_MIN 0 0 0", lines[2])
            assertEquals("DOMAIN_MAX 1 1 1", lines[3])
            val data = lines.drop(4)
            assertEquals(33 * 33 * 33, data.size)
            data.forEach { l ->
                val v = l.split(' ').map { it.toDouble() }
                assertEquals(3, v.size)
                assertTrue(v.all { it in 0.0..1.0 })
            }
            assertEquals(LutGenerator.cube(name, model), file.readText())
        }
    }

    @Test
    fun everyShippedLutIsReferencedByAPreset() {
        val referenced = FilterPresets.mapNotNull { it.info.lutFile }.map { it.substringAfterLast('/') }.toSet()
        val onDisk = assertNotNull(dir.list()).toSet()
        assertEquals(referenced, onDisk)
    }
}
