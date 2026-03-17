package com.livesplit.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import com.livesplit.data.model.Category
import com.livesplit.data.model.Game
import com.livesplit.data.model.Segment

@Database(
    entities = [Game::class, Category::class, Segment::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun categoryDao(): CategoryDao
    abstract fun segmentDao(): SegmentDao

    companion object {
        const val DATABASE_NAME = "livesplit_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
