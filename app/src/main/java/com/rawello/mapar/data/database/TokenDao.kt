package com.rawello.mapar.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rawello.mapar.data.models.Token

@Dao
interface TokenDao {
    @Insert
    suspend fun insertToken(token: Token)

    @Update
    suspend fun updateToken(token: Token)

    @Delete
    suspend fun deleteToken(token: Token)

    @Query("SELECT * FROM tokens")
    suspend fun getAllTokens(): List<Token>

    @Query("SELECT COUNT(*) FROM tokens")
    suspend fun isDbEmpty(): Int

    @Query("DELETE FROM tokens WHERE name = :name")
    suspend fun deleteTokenByName(name: String)

    @Query("SELECT * FROM tokens WHERE name = :name")
    suspend fun getTokenByName(name: String): Token?
}