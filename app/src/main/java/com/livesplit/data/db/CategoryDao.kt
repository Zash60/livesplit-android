package com.livesplit.data.db

import androidx.room.*
import com.livesplit.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE gameId = :gameId ORDER BY name ASC")
    fun getCategoriesByGameId(gameId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE categories SET personalBestMs = :pbMs WHERE id = :id")
    suspend fun updatePersonalBest(id: Long, pbMs: Long)

    @Query("UPDATE categories SET runCount = runCount + 1 WHERE id = :id")
    suspend fun incrementRunCount(id: Long)

    @Query("SELECT * FROM categories WHERE personalBestMs > 0 ORDER BY personalBestMs ASC LIMIT :limit")
    fun getTopPersonalBests(limit: Int = 10): Flow<List<Category>>
}
