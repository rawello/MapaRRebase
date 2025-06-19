package com.rawello.mapar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rawello.mapar.data.models.Building
import com.rawello.mapar.data.models.NodeDb
import com.rawello.mapar.data.models.Token
import com.rawello.mapar.data.models.User
import com.rawello.mapar.data.database.FavoriteEntity

@Database(
    entities = [
        NodeDb::class,
        Token::class,
        Building::class,
        User::class,
        FavoriteEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
    abstract fun tokenDao(): TokenDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun buildingDao(): BuildingDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "mapar_db_final")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}