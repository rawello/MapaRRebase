package com.rawello.mapar.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromFloatArray(floatArray: FloatArray?): String? {
        return floatArray?.joinToString(separator = ",")
    }

    @TypeConverter
    fun toFloatArray(string: String?): FloatArray? {
        return string?.split(",")?.map { it.toFloat() }?.toFloatArray()
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return list?.joinToString(separator = ",") { it.toString() }
    }

    @TypeConverter
    fun toIntList(string: String?): List<Int>? {
        return string?.split(",")?.map { it.toInt() }
    }

    @TypeConverter
    fun fromLongList(list: List<Long>?): String? {
        return list?.joinToString(separator = ",")
    }

    @TypeConverter
    fun toLongList(string: String?): List<Long>? {
        return string?.split(",")?.map { it.toLong() }
    }
}