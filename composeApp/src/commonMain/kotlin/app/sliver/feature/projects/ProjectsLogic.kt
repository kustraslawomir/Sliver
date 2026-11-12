package app.sliver.feature.projects

import app.sliver.data.FilmFormat
import app.sliver.data.db.Project
import app.sliver.data.db.ProjectStats
import app.sliver.feature.archive.MonthNames
import app.sliver.feature.camera.label

const val MAX_PROJECT_NAME = 40

/** "Project n" with the first n above the count that is not already taken. */
fun nextProjectName(existing: List<String>): String {
    var n = existing.size + 1
    while ("Project $n" in existing) n++
    return "Project $n"
}

fun cleanProjectName(raw: String): String? = raw.trim().take(MAX_PROJECT_NAME).ifBlank { null }

fun projectMeta(project: Project, stats: ProjectStats?): String {
    val clips = stats?.clips ?: 0
    val since = stats?.since ?: project.startDate
    return "${project.format.label()} · $clips ${if (clips == 1) "clip" else "clips"} · " +
        "since ${since.day} ${MonthNames[since.month.ordinal]}"
}

/** Archiving or deleting the last open project would leave the camera with nowhere to save. */
fun canLeaveActive(project: Project, all: List<Project>): Boolean =
    project.archived || all.any { !it.archived && it.id != project.id }

fun FilmFormat.useLine(): String = if (this == FilmFormat.CINEMA) "for TV & YouTube" else "for Instagram & TikTok"
