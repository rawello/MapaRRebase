package com.rawello.mapar.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings")
data class Building(
    @PrimaryKey val id: Int,
    val name: String,
    val address: String
){
    constructor(name: String, address: String) : this(0, name, address)
}