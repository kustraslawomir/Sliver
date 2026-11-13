package app.sliver

import app.sliver.feature.autoimport.DraftCandidate
import app.sliver.feature.autoimport.dismissTokens
import app.sliver.feature.autoimport.documentName
import app.sliver.feature.autoimport.isDemoMedia
import app.sliver.feature.autoimport.isDismissed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DismissMatchTest {
    private val folderUri = "content://com.android.externalstorage.documents/tree/primary%3ALena/document/primary%3ALena%2F20260403_211310.jpg"
    private val galleryUri = "content://media/external/images/media/1000076759"

    @Test fun nameFromAFolderUri() {
        assertEquals("20260403_211310.jpg", documentName(folderUri))
        assertEquals(null, documentName(galleryUri))
    }

    @Test fun rejectedInAFolderIsRecognisedInTheGallery() {
        assertTrue(isDismissed(galleryUri, "20260403_211310.jpg", listOf(folderUri)))
        assertFalse(isDismissed(galleryUri, "20260403_999999.jpg", listOf(folderUri)))
        assertFalse(isDismissed(galleryUri, null, listOf(folderUri)))
    }

    @Test fun rejectedInTheGalleryIsRecognisedInAFolder() {
        val seen = dismissTokens(DraftCandidate(galleryUri, video = false, name = "20260403_211310.jpg"))
        assertTrue(isDismissed(folderUri, null, seen))
        assertTrue(isDismissed(galleryUri, null, seen))
    }

    @Test fun demoFoldersAreTestMedia() {
        assertTrue(isDemoMedia("Movies/SliverDemo/"))
        assertFalse(isDemoMedia("Lena iOS/"))
    }
}
