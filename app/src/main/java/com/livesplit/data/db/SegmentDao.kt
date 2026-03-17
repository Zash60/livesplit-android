package com.livesplit.data.db

import androidx.room.*
import com.livesplit.data.model.Segment
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {
    @Query("SELECT * FROM segments WHERE categoryId = :categoryId ORDER BY position ASC")
    fun getSegmentsByCategoryId(categoryId: Long): Flow<List<Segment>>

    @Query("SELECT * FROM segments WHERE categoryId = :categoryId ORDER BY position ASC")
    suspend fun getSegmentsByCategoryIdSync(categoryId: Long): List<Segment>

    @Query("SELECT * FROM segments WHERE id = :id")
    suspend fun getSegmentById(id: Long): Segment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: Segment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<Segment>)

    @Update
    suspend fun update(segment: Segment)

    @Delete
    suspend fun delete(segment: Segment)

    @Query("DELETE FROM segments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM segments WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("UPDATE segments SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("UPDATE segments SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE segments SET pbTimeMs = :pbMs WHERE id = :id")
    suspend fun updatePbTime(id: Long, pbMs: Long)

    @Query("UPDATE segments SET bestTimeMs = :bestMs WHERE id = :id")
    suspend fun updateBestTime(id: Long, bestMs: Long)

    @Query("SELECT COALESCE(SUM(bestTimeMs), 0) FROM segments WHERE categoryId = :categoryId")
    suspend fun getSumOfBests(categoryId: Long): Long

    @Query("SELECT COUNT(*) FROM segments WHERE categoryId = :categoryId")
    suspend fun getSegmentCount(categoryId: Long): Int
}
