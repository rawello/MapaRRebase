package com.rawello.mapar.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rawello.mapar.data.models.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun getUser(username: String, password: String): User?
}