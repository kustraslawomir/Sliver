package app.sliver.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Wraps all existing days and films into one "My <year>" project and makes day keys per project. */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `project` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `format` TEXT NOT NULL, " +
                "`startDate` TEXT NOT NULL, `endDate` TEXT, `createdAt` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                "`colorSeed` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "INSERT INTO project (id, name, format, startDate, endDate, createdAt, archived, colorSeed) VALUES (" +
                "lower(hex(randomblob(16))), " +
                "'My ' || COALESCE(strftime('%Y', (SELECT MIN(date) FROM day)), strftime('%Y', 'now')), " +
                "COALESCE((SELECT format FROM film ORDER BY name LIMIT 1), 'CINEMA'), " +
                "COALESCE((SELECT MIN(date) FROM day), date('now')), NULL, CAST(strftime('%s', 'now') AS INTEGER) * 1000, 0, 0)",
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
