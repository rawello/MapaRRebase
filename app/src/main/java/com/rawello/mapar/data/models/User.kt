package com.rawello.mapar.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rawello.mapar.data.database.Converters

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int,
    val username: String,
    val password: String,
    @TypeConverters(Converters::class)
    val favoriteBuildings: List<Long>
)