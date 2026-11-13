package app.sliver

import app.sliver.feature.autoimport.DateOrigin
import app.sliver.feature.autoimport.FolderLister
import app.sliver.feature.autoimport.FolderNode
import app.sliver.feature.autoimport.MediaEntry
import app.sliver.feature.autoimport.cleanFolderEntries
import app.sliver.feature.autoimport.collectFiles
import app.sliver.feature.autoimport.exifMillisWithOffset
import app.sliver.feature.autoimport.folderSummary
import app.sliver.feature.autoimport.groupByDate
import app.sliver.feature.autoimport.isMediaName
import app.sliver.feature.autoimport.isVideoName
import app.sliver.feature.autoimport.localDateOf
import app.sliver.feature.autoimport.parseExifOffsetMs
import app.sliver.feature.autoimport.resolveItemDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class FolderSourceTest {
    private val utc = TimeZone.UTC
    private val copyTime = 1_800_000_000_000L // 2027, the day the files were copied

    private fun day(ms: Long, zone: TimeZone = utc) = localDateOf(ms, zone)

    @Test fun exifWinsOverEverything() {
        val r = resolveItemDate("2024:01:31 08:15:00", null, null, "20240102T000000.000Z", "IMG_20240105_1.jpg", copyTime, utc)!!
        assertEquals(DateOrigin.EXIF, r.origin)
        assertEquals(LocalDate(2024, 1, 31), day(r.ms))
    }

    @Test fun digitizedUsedWhenOriginalMissing() {
        val r = resolveItemDate(null, "2023:07:04 10:00:00", null, null, "x.jpg", copyTime, utc)!!
        assertEquals(DateOrigin.EXIF, r.origin)
        assertEquals(LocalDate(2023, 7, 4), day(r.ms))
    }

    @Test fun exifOffsetMovesTheInstantToTheCamerasZone() {
        val withOffset = exifMillisWithOffset("2024:01:31 23:30:00", "+02:00", utc)!!
        assertEquals(LocalDate(2024, 1, 31), day(withOffset)) // 21:30 UTC
        val west = exifMillisWithOffset("2024:01:31 23:30:00", "-05:00", utc)!!
        assertEquals(LocalDate(2024, 2, 1), day(west)) // 04:30 UTC next day
        assertEquals(LocalDate(2024, 2, 1), day(west, TimeZone.of("Europe/Warsaw")))
    }

    @Test fun offsetParsing() {
        assertEquals(7_200_000L, parseExifOffsetMs("+02:00"))
        assertEquals(-19_800_000L, parseExifOffsetMs("-0530"))
        assertNull(parseExifOffsetMs("Z"))
        assertNull(parseExifOffsetMs(null))
    }

    @Test fun videoCreationTimeIsUsedWhenNoExif() {
        val r = resolveItemDate(null, null, null, "20240131T081500.000Z", "IMG_1234.MOV", copyTime, utc)!!
        assertEquals(DateOrigin.VIDEO, r.origin)
        assertEquals(LocalDate(2024, 1, 31), day(r.ms))
    }

    @Test fun filenamePatterns() {
        for (name in listOf("IMG_20240131_101500.jpg", "2024-01-31 10.15.00.mp4", "PXL_20240131_101500123.jpg", "VID_20240131_101500.mp4")) {
            val r = resolveItemDate(null, null, null, null, name, copyTime, utc)!!
            assertEquals(DateOrigin.FILENAME, r.origin, name)
            assertEquals(LocalDate(2024, 1, 31), day(r.ms), name)
        }
    }

    @Test fun fileTimeIsLastResort() {
        val r = resolveItemDate(null, null, null, null, "holiday.mp4", copyTime, utc)!!
        assertEquals(DateOrigin.FILE_TIME, r.origin)
        assertEquals(copyTime, r.ms)
    }

    @Test fun badInputGivesNull() {
        assertNull(resolveItemDate("garbage", "0000:00:00 00:00:00", "x", "nope", null, 0L, utc))
        assertNull(resolveItemDate(null, null, null, null, "a.jpg", 5L, utc)) // pre-2000 file time
    }

    @Test fun implausibleExifFallsThrough() {
        val r = resolveItemDate("1970:01:01 00:00:00", null, null, null, "IMG_20240131_1.jpg", copyTime, utc)!!
        assertEquals(DateOrigin.FILENAME, r.origin)
    }

    private fun entry(name: String, size: Long, ms: Long, video: Boolean = false, dur: Long = 0, fileTime: Boolean = false) =
        MediaEntry("content://$name/$size", video, ms, dur, name = name, sizeBytes = size, fileTime = fileTime)

    @Test fun strictDayRuleUsesCaptureDateNotCopyDate() {
        val a = entry("a.jpg", 10, resolveItemDate("2024:01:31 08:00:00", null, null, null, "a.jpg", copyTime, utc)!!.ms)
        val b = entry("b.jpg", 11, resolveItemDate("2024:02:01 08:00:00", null, null, null, "b.jpg", copyTime, utc)!!.ms)
        val grouped = groupByDate(listOf(a, b), utc, LocalDate(2024, 1, 31), LocalDate(2024, 1, 31))
        assertEquals(setOf(LocalDate(2024, 1, 31)), grouped.keys)
        assertEquals(listOf(a), grouped.getValue(LocalDate(2024, 1, 31)))
    }

    @Test fun duplicatesByNameAndSizeCountOnce() {
        val ms = 1_706_700_000_000L
        val list = listOf(entry("IMG_1.jpg", 100, ms), entry("img_1.JPG", 100, ms), entry("IMG_1.jpg", 101, ms))
        assertEquals(2, cleanFolderEntries(list).size)
    }

    @Test fun livePhotoKeepsItsMovingPartAndLongVideosStay() {
        val ms = 1_706_700_000_000L
        val still = entry("IMG_0001.HEIC", 5_000, ms)
        val liveMov = entry("IMG_0001.MOV", 2_000, ms, video = true, dur = 2_500)
        val longMov = entry("IMG_0002.MOV", 90_000, ms, video = true, dur = 30_000)
        val sameBaseButLong = entry("IMG_0003.MOV", 90_000, ms, video = true, dur = 30_000)
        val still3 = entry("IMG_0003.HEIC", 4_000, ms)
        val out = cleanFolderEntries(listOf(still, liveMov, longMov, sameBaseButLong, still3))
        assertEquals(listOf("IMG_0001.MOV", "IMG_0002.MOV", "IMG_0003.MOV", "IMG_0003.HEIC").toSet(), out.map { it.name }.toSet())
        assertEquals(listOf("IMG_0001.MOV"), out.filter { it.live }.map { it.name })
    }

    private fun tree(vararg dirs: Pair<String, List<FolderNode>>): FolderLister {
        val map = dirs.toMap()
        return FolderLister { map[it] ?: error("unreadable") }
    }

    private fun f(n: String) = FolderNode(n, n, false)
    private fun d(n: String) = FolderNode(n, n, true)

    @Test fun walkIsRecursiveAndSkipsUnreadableDirs() = runTest {
        val lister = tree("root" to listOf(f("a"), d("sub"), d("broken")), "sub" to listOf(f("b"), d("deep")), "deep" to listOf(f("c")))
        val got = collectFiles("root", lister)
        assertEquals(setOf("a", "b", "c"), got.files.map { it.id }.toSet())
        assertTrue(!got.truncated)
    }

    @Test fun fileCountIsCapped() = runTest {
        val lister = tree("root" to (1..50).map { f("f$it") })
        val got = collectFiles("root", lister, maxFiles = 10)
        assertEquals(10, got.files.size)
        assertTrue(got.truncated)
    }

    @Test fun depthIsCapped() = runTest {
        val lister = tree("root" to listOf(d("l1")), "l1" to listOf(f("x"), d("l2")), "l2" to listOf(f("y")))
        val got = collectFiles("root", lister, maxDepth = 1)
        assertEquals(listOf("x"), got.files.map { it.id })
        assertTrue(got.truncated)
    }

    @Test fun mediaNames() {
        assertTrue(isMediaName("a.HEIC", null))
        assertTrue(isMediaName("a.bin", "video/quicktime"))
        assertTrue(!isMediaName("notes.txt", "text/plain"))
        assertTrue(isVideoName("a.MOV", null))
        assertTrue(!isVideoName("a.heic", "image/heic"))
    }

    @Test fun summaryLines() {
        assertTrue(folderSummary(0, 0, false).isEmpty())
        val lines = folderSummary(3, 1, true)
        assertEquals(3, lines.size)
        assertEquals("Could not read 3 items", lines[0])
    }
}
