package com.livesplit.data.repository

import com.livesplit.data.db.SegmentDao
import com.livesplit.data.model.Segment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SegmentRepository @Inject constructor(
    private val segmentDao: SegmentDao
) {
    fun getSegmentsByCategoryId(categoryId: Long): Flow<List<Segment>> =
        segmentDao.getSegmentsByCategoryId(categoryId)

    suspend fun getSegmentsByCategoryIdSync(categoryId: Long): List<Segment> =
        segmentDao.getSegmentsByCategoryIdSync(categoryId)

    suspend fun getSegmentById(id: Long): Segment? = segmentDao.getSegmentById(id)

    suspend fun addSegment(categoryId: Long, name: String, position: Int? = null): Long {
        val actualPosition = position ?: segmentDao.getSegmentCount(categoryId)
        val segment = Segment(
            categoryId = categoryId,
            name = name,
            position = actualPosition
        )
        return segmentDao.insert(segment)
    }

    suspend fun updateSegment(segment: Segment) = segmentDao.update(segment)

    suspend fun renameSegment(id: Long, name: String) = segmentDao.rename(id, name)

    suspend fun deleteSegment(segment: Segment) = segmentDao.delete(segment)

    suspend fun deleteSegmentById(id: Long) = segmentDao.deleteById(id)

    suspend fun deleteByCategoryId(categoryId: Long) =
        segmentDao.deleteByCategoryId(categoryId)

    suspend fun updatePosition(id: Long, position: Int) =
        segmentDao.updatePosition(id, position)

    suspend fun reorderSegments(categoryId: Long, orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            segmentDao.updatePosition(id, index)
        }
    }

    suspend fun updatePbTime(id: Long, pbMs: Long) = segmentDao.updatePbTime(id, pbMs)

    suspend fun updateBestTime(id: Long, bestMs: Long) = segmentDao.updateBestTime(id, bestMs)

    suspend fun getSumOfBests(categoryId: Long): Long =
        segmentDao.getSumOfBests(categoryId)

    /**
     * Recalculate cumulative PB times for all segments in a category.
     * Each segment's PB time becomes the cumulative sum up to that point.
     */
    suspend fun recalculatePbTimes(categoryId: Long) {
        val segments = getSegmentsByCategoryIdSync(categoryId)
        var cumulative = 0L
        segments.forEach { segment ->
            cumulative += segment.bestTimeMs
            segmentDao.updatePbTime(segment.id, cumulative)
        }
    }
}
