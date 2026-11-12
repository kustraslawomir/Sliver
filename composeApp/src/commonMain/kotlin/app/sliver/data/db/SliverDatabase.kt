package app.sliver.data.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Project::class, Day::class, Film::class], version = 4, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)])
@TypeConverters(Converters::class)
@ConstructedBy(SliverDatabaseConstructor::class)
abstract class SliverDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun dayDao(): DayDao
    abstract fun filmDao(): FilmDao
}

@Suppress("KotlinNoActualForExpect")
expect object SliverDatabaseConstructor : RoomDatabaseConstructor<SliverDatabase> {
    override fun initialize(): SliverDatabase
}
