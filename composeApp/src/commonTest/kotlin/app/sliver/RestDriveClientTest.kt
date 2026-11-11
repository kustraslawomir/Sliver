package app.sliver

import app.sliver.data.backup.BackupError
import app.sliver.data.backup.DriveException
import app.sliver.data.backup.InMemoryDriveClient
import app.sliver.data.backup.RestDriveClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestDriveClientTest {
    private val log = mutableListOf<HttpRequestData>()

    private fun client(
        token: String? = "tok",
        chunk: Int = 4,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = RestDriveClient(MockEngine { log += it; handler(it) }, { token }, chunkSize = chunk, baseUrl = "https://x")

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun ensureFolderFindsExisting() = runTest {
        val c = client { respond("""{"files":[{"id":"F1","name":"SLIVER"}]}""", headers = json) }
        assertEquals("F1", c.ensureFolder())
        assertEquals("Bearer tok", log.single().headers[HttpHeaders.Authorization])
        assertTrue(log.single().url.parameters["q"]!!.contains("name = 'SLIVER'"))
    }

    @Test
    fun ensureFolderCreatesWhenMissing() = runTest {
        val c = client {
            if (it.method == HttpMethod.Get) respond("""{"files":[]}""", headers = json)
            else respond("""{"id":"NEW"}""", headers = json)
        }
        assertEquals("NEW", c.ensureFolder())
        assertEquals(HttpMethod.Post, log.last().method)
    }

    @Test
    fun ensureFolderKeepsValidStoredId() = runTest {
        val c = client { respond("""{"id":"S","trashed":false}""", headers = json) }
        assertEquals("S", c.ensureFolder("S"))
        assertEquals(1, log.size)
    }

    @Test
    fun ensureFolderRecreatesWhenStoredIdTrashed() = runTest {
        val c = client {
            when {
                it.url.encodedPath.endsWith("/files/S") -> respond("""{"id":"S","trashed":true}""", headers = json)
                it.method == HttpMethod.Get -> respond("""{"files":[{"id":"F2","name":"SLIVER"}]}""", headers = json)
                else -> error("unexpected")
            }
        }
        assertEquals("F2", c.ensureFolder("S"))
    }

    @Test
    fun resumableUploadSendsMetadataAndChunks() = runTest {
        val c = client(chunk = 4) {
            when {
                it.method == HttpMethod.Post -> respond(
                    "", HttpStatusCode.OK, headersOf(HttpHeaders.Location, "https://x/session"),
                )
                it.headers[HttpHeaders.ContentRange] == "bytes 0-3/6" -> respond(
                    "", HttpStatusCode(308, "Resume Incomplete"), headersOf(HttpHeaders.Range, "bytes=0-3"),
                )
                it.headers[HttpHeaders.ContentRange] == "bytes 4-5/6" ->
                    respond("""{"id":"D1","name":"2026-01-01.mp4","appProperties":{"sliverDate":"2026-01-01"}}""", headers = json)
                else -> error("unexpected ${it.headers[HttpHeaders.ContentRange]}")
            }
        }
        val f = c.upload("F", "2026-01-01.mp4", mapOf("sliverDate" to "2026-01-01"), byteArrayOf(1, 2, 3, 4, 5, 6))
        assertEquals("D1", f.id)
        assertEquals("2026-01-01", f.appProperties["sliverDate"])
        val meta = (log[0].body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()
        assertTrue(meta.contains("\"parents\":[\"F\"]") && meta.contains("\"sliverDate\":\"2026-01-01\""))
        assertEquals(3, log.size)
    }

    @Test
    fun uploadResumesFromServerOffsetAfterFailure() = runTest {
        var failed = false
        val c = client(chunk = 4) {
            val range = it.headers[HttpHeaders.ContentRange]
            when {
                it.method == HttpMethod.Post -> respond("", headers = headersOf(HttpHeaders.Location, "https://x/s"))
                range == "bytes 0-3/6" && !failed -> { failed = true; throw IOException("drop") }
                range == "bytes */6" -> respond(
                    "", HttpStatusCode(308, "Resume Incomplete"), headersOf(HttpHeaders.Range, "bytes=0-3"),
                )
                range == "bytes 4-5/6" -> respond("""{"id":"D2","name":"a"}""", headers = json)
                else -> error("unexpected $range")
            }
        }
        assertEquals("D2", c.upload("F", "a", emptyMap(), ByteArray(6)).id)
    }

    @Test
    fun uploadGivesUpAfterRepeatedNetworkFailures() = runTest {
        val c = client {
            if (it.method == HttpMethod.Post) respond("", headers = headersOf(HttpHeaders.Location, "https://x/s"))
            else throw IOException("down")
        }
        val e = assertFailsWith<DriveException> { c.upload("F", "a", emptyMap(), ByteArray(6)) }
        assertEquals(BackupError.OFFLINE, e.error)
    }

    @Test
    fun updateUsesPatchOnExistingId() = runTest {
        val c = client(chunk = 100) {
            if (it.method == HttpMethod.Patch) respond("", headers = headersOf(HttpHeaders.Location, "https://x/s"))
            else respond("""{"id":"E","name":"a"}""", headers = json)
        }
        c.upload("F", "a", emptyMap(), ByteArray(2), existingId = "E")
        assertTrue(log[0].url.encodedPath.endsWith("/upload/drive/v3/files/E"))
    }

    @Test
    fun listParsesAppPropertiesAndPages() = runTest {
        val c = client {
            if (it.url.parameters["pageToken"] == null)
                respond("""{"nextPageToken":"n","files":[{"id":"1","name":"a","appProperties":{"look":"{}"}}]}""", headers = json)
            else respond("""{"files":[{"id":"2","name":"b"}]}""", headers = json)
        }
        val files = c.list("F")
        assertEquals(listOf("1", "2"), files.map { it.id })
        assertEquals("{}", files[0].appProperties["look"])
        assertTrue(log[0].url.parameters["q"]!!.contains("'F' in parents"))
    }

    @Test
    fun downloadReturnsBytes() = runTest {
        val c = client { respond(byteArrayOf(9, 8, 7)) }
        assertEquals(listOf<Byte>(9, 8, 7), c.download("X").toList())
        assertEquals("media", log.single().url.parameters["alt"])
    }

    @Test
    fun trashPatchesTrashedTrue() = runTest {
        val c = client { respond("{}", headers = json) }
        c.trash("X")
        assertEquals(HttpMethod.Patch, log.single().method)
        assertEquals("""{"trashed":true}""", log.single().body.toByteArray().decodeToString())
    }

    @Test
    fun manifestRoundTrip() = runTest {
        val c = client(chunk = 1000) {
            when {
                it.method == HttpMethod.Get && it.url.parameters["alt"] == "media" -> respond("""{"v":1}""")
                it.method == HttpMethod.Get -> respond("""{"files":[{"id":"M","name":"manifest.json"}]}""", headers = json)
                it.method == HttpMethod.Patch -> respond("", headers = headersOf(HttpHeaders.Location, "https://x/s"))
                else -> respond("""{"id":"M","name":"manifest.json"}""", headers = json)
            }
        }
        assertEquals("""{"v":1}""", c.readManifest("F"))
        c.writeManifest("F", """{"v":2}""")
        assertEquals("""{"v":2}""", log.last().body.toByteArray().decodeToString())
    }

    @Test
    fun readManifestNullWhenAbsent() = runTest {
        assertNull(client { respond("""{"files":[]}""", headers = json) }.readManifest("F"))
    }

    @Test
    fun errorMapping() = runTest {
        suspend fun err(status: HttpStatusCode, body: String) = assertFailsWith<DriveException> {
            client { respond(body, status) }.list("F")
        }.error
        assertEquals(BackupError.DRIVE_FULL, err(HttpStatusCode.Forbidden, """{"error":{"errors":[{"reason":"storageQuotaExceeded"}]}}"""))
        assertEquals(BackupError.REVOKED, err(HttpStatusCode.Unauthorized, "{}"))
        assertEquals(BackupError.REVOKED, err(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}"""))
        assertEquals(BackupError.OTHER, err(HttpStatusCode.InternalServerError, "{}"))
        val offline = assertFailsWith<DriveException> { client { throw IOException("no net") }.list("F") }
        assertEquals(BackupError.OFFLINE, offline.error)
        val noToken = assertFailsWith<DriveException> { client(token = null) { respond("{}") }.list("F") }
        assertEquals(BackupError.REVOKED, noToken.error)
    }

    @Test
    fun inMemoryFakeRoundTrip() = runTest {
        val d = InMemoryDriveClient()
        val f = d.ensureFolder()
        assertEquals(f, d.ensureFolder(f))
        val file = d.upload(f, "a.mp4", mapOf("k" to "v"), byteArrayOf(1))
        assertEquals(listOf(file), d.list(f))
        d.writeManifest(f, "m1"); d.writeManifest(f, "m2")
        assertEquals("m2", d.readManifest(f))
        d.trash(file.id)
        assertEquals(listOf("manifest.json"), d.list(f).map { it.name })
    }
}
