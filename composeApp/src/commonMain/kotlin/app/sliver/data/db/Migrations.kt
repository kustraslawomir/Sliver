package app.sliver.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Wraps all existing days and films into one "My <year>" project (unless a project row already exists) and makes day keys per project. */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `project` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `format` TEXT NOT NULL, " +
                "`startDate` TEXT NOT NULL, `endDate` TEXT, `createdAt` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                "`colorSeed` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "INSERT INTO project (id, name, format, startDate, endDate, createdAt, archived, colorSeed) SELECT " +
                "lower(hex(randomblob(16))), " +
                "'My ' || COALESCE(strftime('%Y', (SELECT MIN(date) FROM day)), strftime('%Y', 'now')), " +
                "COALESCE((SELECT format FROM film ORDER BY name LIMIT 1), 'CINEMA'), " +
                "COALESCE((SELECT MIN(date) FROM day), date('now')), NULL, CAST(strftime('%s', 'now') AS INTEGER) * 1000, 0, 0 " +
                "WHERE NOT EXISTS (SELECT 1 FROM project)",
        )
        connection.execSQL("ALTER TABLE day RENAME TO day_old")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `day` (`projectId` TEXT NOT NULL, `date` TEXT NOT NULL, `file` TEXT NOT NULL, " +
                "`thumb` TEXT NOT NULL, `source` TEXT NOT NULL, `look` TEXT NOT NULL, `motion` TEXT NOT NULL, " +
                "`keepAudio` INTEGER NOT NULL, `speed` REAL NOT NULL, `color` INTEGER NOT NULL, `cropY` REAL, " +
                "`takenAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `driveFileId` TEXT, `backup` TEXT NOT NULL, " +
                "`place` TEXT, PRIMARY KEY(`projectId`, `date`))",
        )
        connection.execSQL(
            "INSERT INTO day (projectId, date, file, thumb, source, look, motion, keepAudio, speed, color, cropY, " +
                "takenAt, createdAt, driveFileId, backup, place) " +
                "SELECT (SELECT id FROM project LIMIT 1), date, file, thumb, source, look, motion, keepAudio, speed, color, " +
                "cropY, takenAt, createdAt, driveFileId, backup, place FROM day_old",
        )
        connection.execSQL("DROP TABLE day_old")
        connection.execSQL("ALTER TABLE film ADD COLUMN projectId TEXT NOT NULL DEFAULT ''")
        connection.execSQL("UPDATE film SET projectId = (SELECT id FROM project LIMIT 1)")
    }
}

/** Adds the per-clip light stats; existing rows are filled lazily. */
val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE day ADD COLUMN luma REAL NOT NULL DEFAULT -1")
        connection.execSQL("ALTER TABLE day ADD COLUMN tone INTEGER NOT NULL DEFAULT 0")
    }
}

/** Film-level filters: intensity and grain live on the film; old presets that no longer exist are cleared. */
val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE film ADD COLUMN lookIntensity INTEGER NOT NULL DEFAULT 100")
        connection.execSQL("ALTER TABLE film ADD COLUMN lookGrain INTEGER")
        connection.execSQL("UPDATE film SET look = NULL WHERE look NOT IN ('KIOSK','AFTERHOURS','SILVER','SUNDAY','TAPE','HALO')")
    }
}

/** Auto-import drafts: confirmed days keep draft = 0. */
val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE day ADD COLUMN draft INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE day ADD COLUMN draftCandidates TEXT")
        connection.execSQL("ALTER TABLE day ADD COLUMN importRunId TEXT")
    }
}

/** Sync: a last-edit stamp on every record (existing rows start at their creation time) and the delete queue. */
val Migration6To7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        for (table in listOf("project", "day", "film")) {
            connection.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE $table ADD COLUMN updatedBy TEXT NOT NULL DEFAULT ''")
        }
        connection.execSQL("UPDATE project SET updatedAt = createdAt")
        connection.execSQL("UPDATE day SET updatedAt = createdAt")
        connection.execSQL("UPDATE film SET updatedAt = COALESCE((SELECT createdAt FROM project WHERE project.id = film.projectId), 0)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `tombstone` (`kind` TEXT NOT NULL, `projectId` TEXT NOT NULL, `key` TEXT NOT NULL, " +
                "`deletedAt` INTEGER NOT NULL, PRIMARY KEY(`kind`, `projectId`, `key`))",
        )
    }
}

/** Landscape source in a vertical project: horizontal crop position; null = centered. */
val Migration7To8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE day ADD COLUMN cropX REAL")
    }
}

/** Film date style chosen by the user (null = follow the filter). */
val Migration8To9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE film ADD COLUMN stampStyle TEXT")
    }
}

/** Background music volume per film (null = mode default). */
val Migration9To10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE film ADD COLUMN musicVolume INTEGER")
    }
}

/** Change moment: the full camera recording behind a clip and where its 1 s starts. */
val Migration10To11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE day ADD COLUMN original TEXT")
        connection.execSQL("ALTER TABLE day ADD COLUMN originalStartMs INTEGER")
    }
}

/** Owner 13 Nov: a project starts at its earliest clip (older builds kept the creation day even after earlier days were filled). */
val Migration11To12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "UPDATE project SET startDate = (SELECT MIN(date) FROM day WHERE day.projectId = project.id AND day.draft = 0) " +
                "WHERE (SELECT MIN(date) FROM day WHERE day.projectId = project.id AND day.draft = 0) < startDate",
        )
    }
}
