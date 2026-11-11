package app.sliver.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.sliver.data.db.Migration2To3
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

    private fun createV2(path: String) {
        val schema = v2Schema()
        val sql = SQLiteDatabase.openOrCreateDatabase(path, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            sql.execSQL(entities.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entities.getJSONObject(i).getString("tableName")))
        }
        sql.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '${schema.getString("identityHash")}')")
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

        val db = Room.databaseBuilder<SliverDatabase>(ctx, path).addMigrations(Migration2To3).allowMainThreadQueries().build()
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
}
