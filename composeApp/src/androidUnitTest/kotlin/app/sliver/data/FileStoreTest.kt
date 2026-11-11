package app.sliver.data

import app.sliver.data.files.AndroidFileStore
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class FileStoreTest {
    private val base = kotlin.io.path.createTempDirectory("fs").toFile()
    private val store = AndroidFileStore(File(base, "files"), File(base, "cache"))

    @Test fun paths() {
        assertTrue(store.dayPath("p1", LocalDate(2026, 11, 10)).endsWith("days/p1-2026-11-10.mp4"))
        assertTrue(store.thumbPath("p1", LocalDate(2026, 11, 10)).endsWith("thumbs/p1-2026-11-10.jpg"))
        assertTrue(store.filmPath("abc").endsWith("films/abc.mp4"))
    }

    @Test fun cleanTmpRemovesOnlyOld() {
        val now = 10L * 24 * 3600 * 1000
        val old = File(store.newTmpPath(1)).apply { writeText("x"); setLastModified(now - 25L * 3600 * 1000) }
        val fresh = File(store.newTmpPath(2)).apply { writeText("x"); setLastModified(now - 1L * 3600 * 1000) }
        assertEquals(1, store.cleanTmp(now))
        assertFalse(old.exists())
        assertTrue(fresh.exists())
        assertTrue(store.delete(fresh.path))
    }
}
