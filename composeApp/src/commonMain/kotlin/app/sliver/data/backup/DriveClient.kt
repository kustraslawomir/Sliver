package app.sliver.data.backup

enum class BackupError { DRIVE_FULL, REVOKED, OFFLINE, OTHER }

class DriveException(val error: BackupError, message: String? = null, cause: Throwable? = null) :
    Exception(message ?: error.name, cause)

data class DriveFile(val id: String, val name: String, val appProperties: Map<String, String> = emptyMap())

interface DriveClient {
    /** Returns [storedId] if that folder still exists, otherwise finds the app folder (also under an older app name) or creates it. */
    suspend fun ensureFolder(storedId: String? = null): String

    /** Finds or creates the folder [name] directly under [parentId]. */
    suspend fun ensureSubfolder(parentId: String, name: String): String

    suspend fun renameFolder(folderId: String, name: String)

    /** Direct subfolders of [parentId]. */
    suspend fun listFolders(parentId: String): List<DriveFile>

    /** Uploads [bytes] as [name] into [folderId]; replaces the content of [existingId] if given. */
    suspend fun upload(
        folderId: String,
        name: String,
        appProperties: Map<String, String>,
        bytes: ByteArray,
        existingId: String? = null,
    ): DriveFile

    /** Replaces the appProperties of [fileId] without touching its content. */
    suspend fun setProperties(fileId: String, appProperties: Map<String, String>)

    suspend fun list(folderId: String): List<DriveFile>
    suspend fun download(fileId: String): ByteArray
    suspend fun trash(fileId: String)
    suspend fun readManifest(folderId: String): String?
    suspend fun writeManifest(folderId: String, json: String)

    /** about.get storageQuota; [DriveQuota.limit] is null for unlimited plans. */
    suspend fun storageQuota(): DriveQuota
}

data class DriveQuota(val used: Long, val limit: Long?)

const val DRIVE_FOLDER_NAME = app.sliver.core.APP_NAME

/** Folder names used by earlier builds; an existing one is reused, never renamed. */
val LEGACY_DRIVE_FOLDER_NAMES = listOf("Fragments", "SLIVER")

private val ALL_FOLDER_NAMES = listOf(DRIVE_FOLDER_NAME) + LEGACY_DRIVE_FOLDER_NAMES

class InMemoryDriveClient : DriveClient {
    private class Entry(var file: DriveFile, val parent: String?, var bytes: ByteArray, var trashed: Boolean = false, val folder: Boolean = false)

    private val entries = LinkedHashMap<String, Entry>()
    private var next = 1

    /** Makes every upload fail with this error (tests, offline/full demos). */
    var failUploads: BackupError? = null

    /** Lets this many more uploads / downloads succeed, then fails every one (simulates a connection dropping mid-sync). */
    var uploadsBeforeFailure: Int? = null
    var downloadsBeforeFailure: Int? = null
    var limitBytes: Long? = null

    override suspend fun storageQuota() = DriveQuota(entries.values.filter { !it.trashed }.sumOf { it.bytes.size.toLong() }, limitBytes)

    override suspend fun ensureFolder(storedId: String?): String {
        entries[storedId]?.takeIf { !it.trashed }?.let { return it.file.id }
        ALL_FOLDER_NAMES.firstNotNullOfOrNull { n -> entries.values.firstOrNull { it.file.name == n && it.parent == null && !it.trashed } }
            ?.let { return it.file.id }
        return add(DRIVE_FOLDER_NAME, null, emptyMap(), ByteArray(0), folder = true).id
    }

    override suspend fun ensureSubfolder(parentId: String, name: String): String =
        entries.values.firstOrNull { it.folder && it.parent == parentId && it.file.name == name && !it.trashed }?.file?.id
            ?: add(name, parentId, emptyMap(), ByteArray(0), folder = true).id

    override suspend fun renameFolder(folderId: String, name: String) {
        entries[folderId]?.let { it.file = it.file.copy(name = name) }
    }

    override suspend fun listFolders(parentId: String) =
        entries.values.filter { it.folder && it.parent == parentId && !it.trashed }.map { it.file }

    override suspend fun upload(
        folderId: String,
        name: String,
        appProperties: Map<String, String>,
        bytes: ByteArray,
        existingId: String?,
    ): DriveFile {
        failUploads?.let { throw DriveException(it) }
        uploadsBeforeFailure?.let { left -> if (left <= 0) throw DriveException(BackupError.OFFLINE) else uploadsBeforeFailure = left - 1 }
        val e = existingId?.let { entries[it] }
        if (e == null) return add(name, folderId, appProperties, bytes)
        e.file = e.file.copy(name = name, appProperties = appProperties)
        e.bytes = bytes
        return e.file
    }

    override suspend fun setProperties(fileId: String, appProperties: Map<String, String>) {
        entries[fileId]?.let { it.file = it.file.copy(appProperties = appProperties) }
    }

    override suspend fun list(folderId: String) =
        if (entries[folderId]?.trashed == true) emptyList()
        else entries.values.filter { it.parent == folderId && !it.trashed && !it.folder }.map { it.file }

    override suspend fun download(fileId: String): ByteArray {
        downloadsBeforeFailure?.let { left -> if (left <= 0) throw DriveException(BackupError.OFFLINE) else downloadsBeforeFailure = left - 1 }
        return entries[fileId]?.bytes ?: throw DriveException(BackupError.OTHER, "not found")
    }

    override suspend fun trash(fileId: String) {
        entries[fileId]?.trashed = true
    }

    override suspend fun readManifest(folderId: String): String? =
        list(folderId).firstOrNull { it.name == MANIFEST_NAME }?.let { download(it.id).decodeToString() }

    override suspend fun writeManifest(folderId: String, json: String) {
        val existing = list(folderId).firstOrNull { it.name == MANIFEST_NAME }
        upload(folderId, MANIFEST_NAME, emptyMap(), json.encodeToByteArray(), existing?.id)
    }

    private fun add(name: String, parent: String?, props: Map<String, String>, bytes: ByteArray, folder: Boolean = false): DriveFile {
        val file = DriveFile("fake-${next++}", name, props)
        entries[file.id] = Entry(file, parent, bytes, folder = folder)
        return file
    }
}
