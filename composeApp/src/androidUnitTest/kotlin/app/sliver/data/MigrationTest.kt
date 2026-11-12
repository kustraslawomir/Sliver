package app.sliver.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.data.db.Migration2To3
import app.sliver.data.db.Migration3To4
import app.sliver.data.db.SliverDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class MigrationTest {
    private fun v2Schema(): JSONObject {
        val json = File("schemas/app.sliver.data.db.SliverDatabase/2.json").readText()
        return JSONObject(json).getJSONObject("database")
    }

    private fun createV2(path: String, withEmptyProjectTable: Boolean = false) {
        val schema = v2Schema()
        val sql = SQLiteDatabase.openOrCreateDatabase(path, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            sql.execSQL(entities.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entities.getJSONObject(i).getString("tableName")))
        }
        sql.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '${schema.getString("identityHash")}')")
        if (withEmptyProjectTable) {
            sql.execSQL(
                "CREATE TABLE IF NOT EXISTS `project` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `format` TEXT NOT NULL, " +
                    "`startDate` TEXT NOT NULL, `endDate` TEXT, `createdAt` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                    "`colorSeed` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
        for ((date, file) in listOf("2025-03-02" to "a", "2025-03-03" to "b")) {
            sql.execSQL(
                "INSERT INTO day VALUES ('$date', '$file.mp4', '$file.jpg', 'CAMERA', '{}', 'STILL', 1, 1.0, 0, NULL, 1000, 2000, NULL, 'PENDING', NULL)",
            )
        }
        sql.execSQL(
            "INSERT INTO film (id, name, format, rangeType, live, audio, cut, dateOn, mismatch) " +
                "VALUES ('f1', 'March', 'STORY', 'MONTH', 0, 'ORIGINAL', 'CUT', 1, 'CENTER')",
        )
        sql.version = 2
        sql.close()
    }

    @Test
    fun v2DataMovesIntoMyYearProject() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val path = ctx.getDatabasePath("migration.db").apply { parentFile?.mkdirs(); delete() }.absolutePath
        createV2(path)

        val db = Room.databaseBuilder<SliverDatabase>(ctx, path).addMigrations(Migration2To3, Migration3To4).allowMainThreadQueries().build()
        val projects = db.projectDao().observeAll().first()
        assertEquals(1, projects.size)
        val project = projects.single()
        assertEquals("My 2025", project.name)
        assertEquals(FilmFormat.STORY, project.format)
        assertEquals(LocalDate(2025, 3, 2), project.startDate)

        val days = db.dayDao().observeAll(project.id).first()
        assertEquals(listOf(LocalDate(2025, 3, 2), LocalDate(2025, 3, 3)), days.map { it.date })
        assertTrue(days.all { it.projectId == project.id })
        assertEquals("a.mp4", days.first().file)
        assertEquals(project.id, db.filmDao().observeAll(project.id).first().single().projectId)
        db.close()
    }

    @Test
    fun daysWithEmptyProjectTableGetADefaultProject() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val path = ctx.getDatabasePath("migration-empty.db").apply { parentFile?.mkdirs(); delete() }.absolutePath
        createV2(path, withEmptyProjectTable = true)

        val db = Room.databaseBuilder<SliverDatabase>(ctx, path).addMigrations(Migration2To3, Migration3To4).allowMainThreadQueries().build()
        val project = db.projectDao().observeAll().first().single()
        assertEquals(2, db.dayDao().observeAll(project.id).first().size)
        db.close()
    }

    @Test
    fun existingProjectIsReusedNotDuplicated() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val path = ctx.getDatabasePath("migration-existing.db").apply { parentFile?.mkdirs(); delete() }.absolutePath
        createV2(path, withEmptyProjectTable = true)
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("INSERT INTO project VALUES ('p1', 'Mine', 'CINEMA', '2025-01-01', NULL, 1, 0, 0)")
        }

        val db = Room.databaseBuilder<SliverDatabase>(ctx, path).addMigrations(Migration2To3, Migration3To4).allowMainThreadQueries().build()
        assertEquals(listOf("p1"), db.projectDao().observeAll().first().map { it.id })
        assertEquals(2, db.dayDao().observeAll("p1").first().size)
        db.close()
    }

    @Test
    fun v3ToV4AddsLightColumnsWithDefaults() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val path = ctx.getDatabasePath("migration-v3.db").apply { parentFile?.mkdirs(); delete() }.absolutePath
        val schema = JSONObject(File("schemas/app.sliver.data.db.SliverDatabase/3.json").readText()).getJSONObject("database")
        val sql = SQLiteDatabase.openOrCreateDatabase(path, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            sql.execSQL(entities.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entities.getJSONObject(i).getString("tableName")))
        }
        sql.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '${schema.getString("identityHash")}')")
        sql.execSQL("INSERT INTO project VALUES ('p1', 'Mine', 'CINEMA', '2025-01-01', NULL, 1, 0, 0)")
        sql.execSQL("INSERT INTO day VALUES ('p1', '2025-03-02', 'a.mp4', 'a.jpg', 'CAMERA', '{}', 'STILL', 1, 1.0, 0, NULL, 1000, 2000, NULL, 'PENDING', NULL)")
        sql.version = 3
        sql.close()

        val db = Room.databaseBuilder<SliverDatabase>(ctx, path).addMigrations(Migration2To3, Migration3To4).allowMainThreadQueries().build()
        val day = db.dayDao().observeAll("p1").first().single()
        assertEquals(-1f, day.luma)
        assertEquals(0, day.tone)
        assertEquals(1, db.dayDao().withoutLight().size)
        db.dayDao().setLight("p1", day.date, 0.5f, 0xFF112233.toInt())
        assertEquals(0, db.dayDao().withoutLight().size)
        assertEquals(0.5f, db.dayDao().observeAll("p1").first().single().luma)
        db.close()
    }
}
