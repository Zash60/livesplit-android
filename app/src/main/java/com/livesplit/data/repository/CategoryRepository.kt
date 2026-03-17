package com.livesplit.data.repository

import com.livesplit.data.db.CategoryDao
import com.livesplit.data.db.SegmentDao
import com.livesplit.data.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val segmentDao: SegmentDao
) {
    fun getCategoriesByGameId(gameId: Long): Flow<List<Category>> =
        categoryDao.getCategoriesByGameId(gameId)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun addCategory(gameId: Long, name: String): Long {
        val category = Category(gameId = gameId, name = name)
        return categoryDao.insert(category)
    }

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun renameCategory(id: Long, name: String) = categoryDao.rename(id, name)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteById(id)
    }

    suspend fun updatePersonalBest(id: Long, pbMs: Long) =
        categoryDao.updatePersonalBest(id, pbMs)

    suspend fun incrementRunCount(id: Long) = categoryDao.incrementRunCount(id)

    suspend fun hasSegments(categoryId: Long): Boolean {
        return segmentDao.getSegmentCount(categoryId) > 0
    }

    fun getTopPersonalBests(limit: Int = 10): Flow<List<Category>> =
        categoryDao.getTopPersonalBests(limit)
}
