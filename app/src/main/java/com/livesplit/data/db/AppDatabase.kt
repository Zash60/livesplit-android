package com.livesplit.data.db

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
    }
}
