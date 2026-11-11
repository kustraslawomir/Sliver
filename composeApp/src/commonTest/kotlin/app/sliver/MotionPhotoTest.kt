package app.sliver

import app.sliver.data.media.MotionPhoto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MotionPhotoTest {
    private fun mp4(n: Int) = ByteArray(n) { 7 }.also {
        it[0] = 0; it[1] = 0; it[2] = 0; it[3] = 24
        "ftypmp42".encodeToByteArray().copyInto(it, 4)
    }

    private fun xmp(videoLen: Int, ts: String = "", flag: String = "1", semantic: String = "MotionPhoto") =
        """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:Description Camera:MotionPhoto="$flag" $ts>
<Container:Directory><rdf:Seq>
<rdf:li><Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0"/></rdf:li>
<rdf:li><Container:Item Item:Mime="video/mp4" Item:Semantic="$semantic" Item:Length="$videoLen"/></rdf:li>
</rdf:Seq></Container:Directory></rdf:Description></x:xmpmeta>""".encodeToByteArray()

    private fun jpeg(xmp: ByteArray, video: ByteArray) =
        byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + xmp + ByteArray(100) { 1 } + video

    @Test fun jpegVideoIsTrailingBytes() {
        val v = mp4(500)
        val found = assertNotNull(MotionPhoto.find(jpeg(xmp(500, """Camera:MotionPhotoPresentationTimestampUs="1500000""""), v)))
        assertEquals(500, found.length)
        assertEquals(1_500_000L, found.presentationUs)
    }

    @Test fun missingFlagOrSemanticOrBytesIsNotMotion() {
        assertNull(MotionPhoto.find(jpeg(xmp(500, flag = "0"), mp4(500))))
        assertNull(MotionPhoto.find(jpeg(xmp(500, semantic = "GainMap"), mp4(500))))
        assertNull(MotionPhoto.find(jpeg(xmp(500), ByteArray(500))))
        assertNull(MotionPhoto.find(jpeg(xmp(90_000), mp4(500))))
        assertNull(MotionPhoto.find(byteArrayOf(1, 2, 3)))
    }

    @Test fun legacyMicroVideoIgnored() {
        val legacy = """<x:xmpmeta><rdf:Description GCamera:MicroVideo="1" GCamera:MicroVideoOffset="500"/></x:xmpmeta>""".encodeToByteArray()
        assertNull(MotionPhoto.find(jpeg(legacy, mp4(500))))
    }

    private fun box(type: String, payload: ByteArray): ByteArray {
        val size = payload.size + 8
        return byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()) + type.encodeToByteArray() + payload
    }

    @Test fun heicVideoIsMpvdPayload() {
        val v = mp4(300)
        val file = box("ftyp", "heic".encodeToByteArray() + ByteArray(8)) + box("meta", xmp(300)) + box("mdat", ByteArray(50)) + box("mpvd", v)
        val found = assertNotNull(MotionPhoto.find(file))
        assertEquals(300, found.length)
        assertEquals(file.size - 300, found.offset)
        assertNull(found.presentationUs)
    }

    @Test fun windowCenteredOnTimestampOrMidpoint() {
        assertEquals(1000L, MotionPhoto.windowStartMs(1_500_000, 3000))
        assertEquals(0L, MotionPhoto.windowStartMs(100_000, 3000))
        assertEquals(2000L, MotionPhoto.windowStartMs(2_900_000, 3000))
        assertEquals(1000L, MotionPhoto.windowStartMs(null, 3000))
        assertEquals(0L, MotionPhoto.windowStartMs(null, 800))
    }
}
