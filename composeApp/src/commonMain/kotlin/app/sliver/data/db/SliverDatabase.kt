package app.sliver.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Day::class, Film::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SliverDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun filmDao(): FilmDao
}
