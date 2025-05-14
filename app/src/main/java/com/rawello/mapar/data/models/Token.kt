package com.rawello.mapar.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class Token(
    @PrimaryKey val name: String,
    val token: String
)