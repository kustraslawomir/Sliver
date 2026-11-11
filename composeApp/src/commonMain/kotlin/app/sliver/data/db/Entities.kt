package app.sliver.data.db

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
    val takenAt: Instant,
    val createdAt: Instant,
    val driveFileId: String? = null,
    val backup: BackupState = BackupState.PENDING,
    val place: String? = null,
    val projectId: String = "",
)

@Entity(tableName = "film")
data class Film(
    @PrimaryKey val id: String,
    val name: String,
    val format: FilmFormat = FilmFormat.CINEMA,
    val rangeType: FilmRangeType,
    val rangeFrom: LocalDate? = null,
    val rangeTo: LocalDate? = null,
    val live: Boolean = false,
    val audio: FilmAudio = FilmAudio.ORIGINAL,
    val track: String? = null,
    val cut: FilmCut = FilmCut.CUT,
    val dateOn: Boolean = true,
    val look: LookPreset? = null,
    val mismatch: Mismatch = Mismatch.CENTER,
    val renderPath: String? = null,
    val renderedAt: Instant? = null,
    val projectId: String = "",
)
