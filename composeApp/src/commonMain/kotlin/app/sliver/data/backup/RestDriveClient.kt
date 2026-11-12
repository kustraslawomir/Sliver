package app.sliver.data.backup

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Drive v3 client limited to `drive.file` behaviour. */
class RestDriveClient(
    engine: HttpClientEngine,
    private val token: suspend () -> String?,
    private val chunkSize: Int = 2 * 256 * 1024,
    private val maxRetries: Int = 3,
    private val baseUrl: String = "https://www.googleapis.com",
) : DriveClient {
    private val http = HttpClient(engine) { expectSuccess = false }

    override suspend fun ensureFolder(storedId: String?): String {
        if (storedId != null) {
            val r = call(HttpMethod.Get, "/drive/v3/files/$storedId") { parameter("fields", "id,trashed") }
            if (r.status == HttpStatusCode.NotFound) Unit
            else {
                val body = r.checked().json().jsonObject
                if (body["trashed"]?.jsonPrimitive?.booleanOrNull != true) return storedId
            }
        }
        val q = "name = '$DRIVE_FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false"
        val found = query(q).firstOrNull()
        if (found != null) return found.id
        val created = call(HttpMethod.Post, "/drive/v3/files") {
            jsonBody(buildJsonObject { put("name", DRIVE_FOLDER_NAME); put("mimeType", FOLDER_MIME) })
        }.checked().json().jsonObject
        return created.getValue("id").jsonPrimitive.content
    }

    override suspend fun ensureSubfolder(parentId: String, name: String): String {
        val q = "name = '${name.escapeQuery()}' and mimeType = '$FOLDER_MIME' and '$parentId' in parents and trashed = false"
        query(q).firstOrNull()?.let { return it.id }
        val created = call(HttpMethod.Post, "/drive/v3/files") {
            jsonBody(
                buildJsonObject {
                    put("name", name)
                    put("mimeType", FOLDER_MIME)
                    put("parents", JsonArray(listOf(JsonPrimitive(parentId))))
                },
            )
        }.checked().json().jsonObject
        return created.getValue("id").jsonPrimitive.content
    }

    override suspend fun renameFolder(folderId: String, name: String) {
        call(HttpMethod.Patch, "/drive/v3/files/$folderId") {
            jsonBody(buildJsonObject { put("name", name) })
        }.checked()
    }

    override suspend fun listFolders(parentId: String): List<DriveFile> =
        query("'$parentId' in parents and mimeType = '$FOLDER_MIME' and trashed = false")

    private fun String.escapeQuery() = replace("\\", "\\\\").replace("'", "\\'")

    override suspend fun upload(
        folderId: String,
        name: String,
        appProperties: Map<String, String>,
        bytes: ByteArray,
        existingId: String?,
    ): DriveFile {
        val meta = buildJsonObject {
            put("name", name)
            if (existingId == null) put("parents", JsonArray(listOf(JsonPrimitive(folderId))))
            put("appProperties", JsonObject(appProperties.mapValues { JsonPrimitive(it.value) }))
        }
        val start = call(
            if (existingId == null) HttpMethod.Post else HttpMethod.Patch,
            "/upload/drive/v3/files" + (existingId?.let { "/$it" } ?: ""),
        ) {
            parameter("uploadType", "resumable")
            parameter("fields", FILE_FIELDS)
            jsonBody(meta)
        }.checked()
        val session = start.headers[HttpHeaders.Location] ?: throw DriveException(BackupError.OTHER, "no session")

        val total = bytes.size
        var offset = 0
        var failures = 0
        var probe = false
        while (true) {
            val range = if (probe) "bytes */$total" else chunkRange(offset, total)
            val body = if (probe) ByteArray(0) else bytes.copyOfRange(offset, minOf(total, offset + chunkSize))
            val resp = try {
                send(HttpMethod.Put, session) {
                    header(HttpHeaders.ContentRange, range)
                    setBody(body)
                }
            } catch (e: DriveException) {
                if (e.error != BackupError.OFFLINE || ++failures > maxRetries) throw e
                probe = true
                continue
            }
            when {
                resp.status.value == 200 || resp.status.value == 201 -> return resp.json().toFile()
                resp.status.value == 308 -> {
                    offset = resp.headers[HttpHeaders.Range]?.substringAfter('-')?.toIntOrNull()?.plus(1) ?: 0
                    probe = false
                }
                resp.status.value >= 500 -> {
                    if (++failures > maxRetries) resp.checked()
                    probe = true
                }
                else -> resp.checked()
            }
        }
    }

    override suspend fun setProperties(fileId: String, appProperties: Map<String, String>) {
        call(HttpMethod.Patch, "/drive/v3/files/$fileId") {
            jsonBody(buildJsonObject { put("appProperties", JsonObject(appProperties.mapValues { JsonPrimitive(it.value) })) })
        }.checked()
    }

    override suspend fun list(folderId: String): List<DriveFile> =
        query("'$folderId' in parents and trashed = false")

    override suspend fun download(fileId: String): ByteArray =
        call(HttpMethod.Get, "/drive/v3/files/$fileId") { parameter("alt", "media") }.checked().bytes()

    override suspend fun trash(fileId: String) {
        call(HttpMethod.Patch, "/drive/v3/files/$fileId") {
            jsonBody(buildJsonObject { put("trashed", true) })
        }.checked()
    }

    override suspend fun storageQuota(): DriveQuota {
        val q = call(HttpMethod.Get, "/drive/v3/about") { parameter("fields", "storageQuota") }
            .checked().json().jsonObject["storageQuota"]?.jsonObject
        return DriveQuota(
            used = q?.get("usage")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            limit = q?.get("limit")?.jsonPrimitive?.contentOrNull?.toLongOrNull(),
        )
    }

    override suspend fun readManifest(folderId: String): String? =
        list(folderId).firstOrNull { it.name == MANIFEST_NAME }?.let { download(it.id).decodeToString() }

    override suspend fun writeManifest(folderId: String, json: String) {
        val existing = list(folderId).firstOrNull { it.name == MANIFEST_NAME }
        upload(folderId, MANIFEST_NAME, emptyMap(), json.encodeToByteArray(), existing?.id)
    }

    private suspend fun query(q: String): List<DriveFile> {
        val out = mutableListOf<DriveFile>()
        var page: String? = null
        do {
            val body = call(HttpMethod.Get, "/drive/v3/files") {
                parameter("q", q)
                parameter("spaces", "drive")
                parameter("pageSize", 1000)
                parameter("fields", "nextPageToken,files($FILE_FIELDS)")
                page?.let { parameter("pageToken", it) }
            }.checked().json().jsonObject
            body["files"]?.jsonArray?.forEach { out += it.toFile() }
            page = body["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (page != null)
        return out
    }

    private suspend fun call(method: HttpMethod, path: String, block: HttpRequestBuilder.() -> Unit = {}) =
        send(method, baseUrl + path, block)

    private suspend fun send(method: HttpMethod, url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val t = token() ?: throw DriveException(BackupError.REVOKED, "no token")
        return try {
            http.request {
                this.method = method
                url(url)
                header(HttpHeaders.Authorization, "Bearer $t")
                block()
            }
        } catch (e: IOException) {
            throw DriveException(BackupError.OFFLINE, cause = e)
        }
    }

    private fun HttpRequestBuilder.jsonBody(json: JsonElement) {
        contentType(ContentType.Application.Json)
        setBody(json.toString())
    }

    private fun chunkRange(offset: Int, total: Int): String =
        if (total == 0) "bytes */0" else "bytes $offset-${minOf(total, offset + chunkSize) - 1}/$total"

    private suspend fun HttpResponse.checked(): HttpResponse {
        if (status.value in 200..299) return this
        val text = runCatching { bodyAsText() }.getOrDefault("")
        throw DriveException(mapError(status.value, text), "HTTP ${status.value}")
    }

    private suspend fun HttpResponse.json(): JsonElement = Json.parseToJsonElement(bodyAsText())

    private suspend fun HttpResponse.bytes(): ByteArray = bodyAsBytes()

    private fun JsonElement.toFile(): DriveFile {
        val o = jsonObject
        return DriveFile(
            id = o.getValue("id").jsonPrimitive.content,
            name = o["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            appProperties = o["appProperties"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty(),
        )
    }

    companion object {
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val FILE_FIELDS = "id,name,appProperties"

        fun mapError(status: Int, body: String): BackupError = when {
            status == 401 || body.contains("invalid_grant") -> BackupError.REVOKED
            status == 403 && (body.contains("storageQuotaExceeded") || body.contains("quotaExceeded")) ->
                BackupError.DRIVE_FULL
            else -> BackupError.OTHER
        }
    }
}
