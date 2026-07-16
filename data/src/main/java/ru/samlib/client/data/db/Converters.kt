package ru.samlib.client.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.samlib.client.domain.model.Genre
import java.math.BigDecimal
import java.util.Date

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun dateToLong(value: Date?): Long? = value?.time

    @TypeConverter
    fun longToDate(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun fromGenreList(value: List<Genre>): String = json.encodeToString(value)

    @TypeConverter
    fun toGenreList(value: String): List<Genre> = json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
