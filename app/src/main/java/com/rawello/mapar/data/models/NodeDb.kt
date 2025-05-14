package com.rawello.mapar.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Nodes")
data class NodeDb(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val buildingId: Long,
    val nameNode: String,
    val translation: FloatArray,
    val quaternion: FloatArray,
    val type: String, //3 types, 1)entry with ViewNode, 2)path ModelNode as node between entries 3)link ModelNode cylinder as strait line between paths
    val title: String, //scanned number\string
    var neighbors: String //vaR bcoz we can append or delete links(third type)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeDb

        if (id != other.id) return false
        if (!translation.contentEquals(other.translation)) return false
        if (!quaternion.contentEquals(other.quaternion)) return false
        if (type != other.type) return false
        if (title != other.title) return false
        if (neighbors != other.neighbors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + translation.contentHashCode()
        result = 31 * result + quaternion.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + neighbors.hashCode()
        return result
    }
}