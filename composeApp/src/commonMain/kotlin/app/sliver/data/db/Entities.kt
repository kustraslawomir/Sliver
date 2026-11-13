package app.sliver.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.sliver.data.BackupState
import app.sliver.data.DaySource
import app.sliver.data.FilmAudio
import app.sliver.data.FilmCut
import app.sliver.data.FilmFormat
import app.sliver.data.FilmRangeType
import app.sliver.data.LookParams
import app.sliver.data.LookPreset
import app.sliver.data.Mismatch
import app.sliver.data.Motion
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "project")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val format: FilmFormat = FilmFormat.CINEMA,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val createdAt: Instant,
    val archived: Boolean = false,
    val colorSeed: Int = 0,
    /** Millis of the last edit; the sync rule is last writer wins per record. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    /** Device that made the last edit; empty = this device. */
    @ColumnInfo(defaultValue = "''") val updatedBy: String = "",
)

// DECISION: projectId "" means "not assigned yet"; repositories stamp the active project on save.
@Entity(tableName = "day", primaryKeys = ["projectId", "date"])
data class Day(
    val date: LocalDate,
    val file: String,
    val thumb: String,
    val source: DaySource,
    val look: LookParams = LookParams(),
    val motion: Motion = Motion.STILL,
    val keepAudio: Boolean = true,
    val speed: Float = 1.0f,
    val color: Int = 0,
    val cropY: Float? = null,
    val cropX: Float? = null,
    val takenAt: Instant,
    val createdAt: Instant,
    val driveFileId: String? = null,
    val backup: BackupState = BackupState.PENDING,
    val place: String? = null,
    val projectId: String = "",
    /** Average brightness 0..1, or -1 until computed. */
    @ColumnInfo(defaultValue = "-1") val luma: Float = -1f,
    /** Dominant colour (opaque ARGB), or 0 until computed. */
    @ColumnInfo(defaultValue = "0") val tone: Int = 0,
    /** Auto-imported and not yet confirmed: shown in the calendar only, never counted anywhere else. */
    @ColumnInfo(defaultValue = "0") val draft: Boolean = false,
    /** JSON list of the day's gallery items (DraftCandidate), default pick first; null for normal days. */
    val draftCandidates: String? = null,
    val importRunId: String? = null,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    @ColumnInfo(defaultValue = "''") val updatedBy: String = "",
    /** Full in-app camera recording this clip was cut from (files/originals), kept for Change moment. Local only, never synced. */
    val original: String? = null,
    /** Where the 1 s starts inside [original]. */
    val originalStartMs: Long? = null,
)

@Entity(tableName = "film")
data class Film(
    @PrimaryKey val id: String,
    val name: String,
    val format: FilmFormat = FilmFormat.CINEMA,
    /** Legacy: only read from old rows and backups; new films are always the whole project. */
    val rangeType: FilmRangeType = FilmRangeType.YEAR,
    val rangeFrom: LocalDate? = null,
    val rangeTo: LocalDate? = null,
    val live: Boolean = false,
    val audio: FilmAudio = FilmAudio.ORIGINAL,
    val track: String? = null,
    val cut: FilmCut = FilmCut.CUT,
    val dateOn: Boolean = true,
    val look: LookPreset? = null,
    @ColumnInfo(defaultValue = "100") val lookIntensity: Int = 100,
    /** Absolute grain 0..100; null keeps the filter's own grain. */
    val lookGrain: Int? = null,
    val mismatch: Mismatch = Mismatch.CENTER,
    /** Owner-chosen date style; null follows the filter (Kiosk → LED, Tape → camcorder, else plain). */
    val stampStyle: app.sliver.data.StampStyle? = null,
    /** Background music volume 0..100; null = the mode's default (Music 100, Mix 30). */
    val musicVolume: Int? = null,
    val renderPath: String? = null,
    val renderedAt: Instant? = null,
    val projectId: String = "",
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    @ColumnInfo(defaultValue = "''") val updatedBy: String = "",
)

/** A local delete waiting to be written to Drive so other phones delete too. */
@Entity(tableName = "tombstone", primaryKeys = ["kind", "projectId", "key"])
data class Tombstone(val kind: String, val projectId: String, val key: String, val deletedAt: Long)
