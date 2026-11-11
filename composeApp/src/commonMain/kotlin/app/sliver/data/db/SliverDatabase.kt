package app.sliver.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Day::class, Film::class], version = 2, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)])
@TypeConverters(Converters::class)
abstract class SliverDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun filmDao(): FilmDao
}
