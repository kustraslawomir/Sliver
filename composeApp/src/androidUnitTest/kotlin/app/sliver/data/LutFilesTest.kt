package app.sliver.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LutFilesTest {
    private val dir = File("src/androidMain/assets/luts")

    // Regenerate: ./gradlew :composeApp:testDebugUnitTest --tests '*LutFilesTest*' -PgenLuts=1
    @Test
    fun generateAndValidate() {
        if (System.getProperty("genLuts") != null) {
            dir.mkdirs()
            LutGenerator.all().forEach { (n, text) -> File(dir, "$n.cube").writeText(text) }
        }
        assertEquals(8, LutGenerator.recipes.size)
        for ((name, expected) in LutGenerator.all()) {
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
            assertEquals(expected, file.readText())
        }
    }
}
