package app.sliver.data.backup

enum class BackupError { DRIVE_FULL, REVOKED, OFFLINE, OTHER }

class DriveException(val error: BackupError, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: error.name, cause)

data class DriveFile(val id: String, val name: String, val appProperties: Map<String, String> = emptyMap())

interface DriveClient {
    /** Returns [storedId] if that folder still exists, otherwise finds or creates the 'SLIVER' folder. */
    suspend fun ensureFolder(storedId: String? = null): String

    /** Uploads [bytes] as [name] into [folderId]; replaces the content of [existingId] if given. */
    suspend fun upload(
        folderId: String,
        name: String,
        appProperties: Map<String, String>,
        bytes: ByteArray,
        existingId: String? = null,
    ): DriveFile

    suspend fun list(folderId: String): List<DriveFile>
    suspend fun download(fileId: String): ByteArray
    suspend fun trash(fileId: String)
    suspend fun readManifest(folderId: String): String?
    suspend fun writeManifest(folderId: String, json: String)

    /** about.get storageQuota; [DriveQuota.limit] is null for unlimited plans. */
    suspend fun storageQuota(): DriveQuota
}

data class DriveQuota(val used: Long, val limit: Long?)

const val DRIVE_FOLDER_NAME = "SLIVER"
const val MANIFEST_NAME = "manifest.json"

class InMemoryDriveClient : DriveClient {
    private class Entry(var file: DriveFile, val parent: String?, var bytes: ByteArray, var trashed: Boolean = false)

    private val entries = LinkedHashMap<String, Entry>()
    private var next = 1

    /** Makes every upload fail with this error (tests, offline/full demos). */
    var failUploads: BackupError? = null
    var limitBytes: Long? = null

    override suspend fun storageQuota() = DriveQuota(entries.values.filter { !it.trashed }.sumOf { it.bytes.size.toLong() }, limitBytes)

    override suspend fun ensureFolder(storedId: String?): String {
        entries[storedId]?.takeIf { !it.trashed }?.let { return it.file.id }
        entries.values.firstOrNull { it.file.name == DRIVE_FOLDER_NAME && it.parent == null && !it.trashed }
            ?.let { return it.file.id }
        return add(DRIVE_FOLDER_NAME, null, emptyMap(), ByteArray(0)).id
    }

    override suspend fun upload(
        folderId: String,
        name: String,
        appProperties: Map<String, String>,
        bytes: ByteArray,
        existingId: String?,
    ): DriveFile {
        failUploads?.let { throw DriveException(it) }
        val e = existingId?.let { entries[it] }
        if (e == null) return add(name, folderId, appProperties, bytes)
        e.file = e.file.copy(name = name, appProperties = appProperties)
        e.bytes = bytes
        return e.file
    }

    override suspend fun list(folderId: String) =
        entries.values.filter { it.parent == folderId && !it.trashed }.map { it.file }

    override suspend fun download(fileId: String): ByteArray =
        entries[fileId]?.bytes ?: throw DriveException(BackupError.OTHER, "not found")

    override suspend fun trash(fileId: String) {
        entries[fileId]?.trashed = true
    }

    override suspend fun readManifest(folderId: String): String? =
        list(folderId).firstOrNull { it.name == MANIFEST_NAME }?.let { download(it.id).decodeToString() }

    override suspend fun writeManifest(folderId: String, json: String) {
        val existing = list(folderId).firstOrNull { it.name == MANIFEST_NAME }
        upload(folderId, MANIFEST_NAME, emptyMap(), json.encodeToByteArray(), existing?.id)
    }

    private fun add(name: String, parent: String?, props: Map<String, String>, bytes: ByteArray): DriveFile {
        val file = DriveFile("fake-${next++}", name, props)
        entries[file.id] = Entry(file, parent, bytes)
        return file
    }
}
