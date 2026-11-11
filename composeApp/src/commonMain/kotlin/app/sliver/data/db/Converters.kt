package app.sliver.data.db

import androidx.room.TypeConverter
import app.sliver.data.LookParams
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter fun dateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilliseconds()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::fromEpochMilliseconds)

    @TypeConverter fun lookToJson(v: LookParams): String = json.encodeToString(LookParams.serializer(), v)
    @TypeConverter fun jsonToLook(v: String): LookParams = json.decodeFromString(LookParams.serializer(), v)

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
