package app.sliver.data.backup

import app.sliver.data.BackupState
import app.sliver.data.db.Day
import app.sliver.data.db.Film
import app.sliver.data.db.Tombstone
import app.sliver.data.db.TombstoneDao

object TombstoneKind {
    const val DAY = "DAY"
    const val FILM = "FILM"
    const val PROJECT = "PROJECT"
}

/** Remembers local deletes until the sync has written them to Drive. */
interface TombstoneStore {
    suspend fun record(kind: String, projectId: String, key: String, at: Long)
    suspend fun all(): List<Tombstone>
    suspend fun clear(kind: String, projectId: String, key: String)
}

object NoTombstones : TombstoneStore {
    override suspend fun record(kind: String, projectId: String, key: String, at: Long) = Unit
    override suspend fun all(): List<Tombstone> = emptyList()
    override suspend fun clear(kind: String, projectId: String, key: String) = Unit
}

class DaoTombstoneStore(private val dao: TombstoneDao) : TombstoneStore {
    override suspend fun record(kind: String, projectId: String, key: String, at: Long) = dao.upsert(Tombstone(kind, projectId, key, at))
    override suspend fun all() = dao.all()
    override suspend fun clear(kind: String, projectId: String, key: String) = dao.delete(kind, projectId, key)
}

/** Everything the user can change about a clip; bookkeeping (backup state, Drive id, light stats, thumbnail colour) is not content. */
private fun Day.content() = copy(
    backup = BackupState.PENDING, driveFileId = null, luma = 0f, tone = 0, color = 0, place = null, thumb = "",
    updatedAt = 0, updatedBy = "",
)

private fun Film.definition() = copy(renderPath = null, renderedAt = null, updatedAt = 0, updatedBy = "")

/** Same film settings; the local render and the stamp don't count. */
fun Film.sameDefinition(o: Film) = definition() == o.definition()

/** A save that changes the content is a new edit by this device; a pure bookkeeping save keeps the old stamp. */
fun stampDay(existing: Day?, incoming: Day, now: Long): Day =
    if (existing != null && existing.content() == incoming.content()) {
        incoming.copy(updatedAt = existing.updatedAt, updatedBy = existing.updatedBy)
    } else {
        incoming.copy(updatedAt = now, updatedBy = "", backup = if (incoming.backup == BackupState.DONE) BackupState.PENDING else incoming.backup)
    }

fun stampFilm(existing: Film?, incoming: Film, now: Long): Film =
    if (existing != null && existing.definition() == incoming.definition()) {
        incoming.copy(updatedAt = existing.updatedAt, updatedBy = existing.updatedBy)
    } else {
        incoming.copy(updatedAt = now, updatedBy = "")
    }

/** Last writer wins by time, ties broken by device id so both phones pick the same winner. */
fun compareVersions(aAt: Long, aBy: String, bAt: Long, bBy: String): Int =
    if (aAt != bAt) aAt.compareTo(bAt) else aBy.compareTo(bBy)

/** 64-bit FNV-1a, enough to tell whether two copies of a clip are the same bytes. */
fun contentHash(bytes: ByteArray): String {
    var h = -3750763034362895579L
    for (b in bytes) h = (h xor (b.toLong() and 0xff)) * 1099511628211L
    return h.toULong().toString(16) + "-" + bytes.size
}
