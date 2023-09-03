package edu.vt.cs5254.dreamcatcher.database

import androidx.room.TypeConverter
import java.util.Date

class DreamTypeConverters {
    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long): Date {
        return Date(millisSinceEpoch)
    }
}