package com.livesplit.di

import com.livesplit.data.db.CategoryDao
import com.livesplit.data.db.GameDao
import com.livesplit.data.db.SegmentDao
import com.livesplit.data.repository.CategoryRepository
import com.livesplit.data.repository.GameRepository
import com.livesplit.data.repository.SegmentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGameRepository(gameDao: GameDao): GameRepository {
        return GameRepository(gameDao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao,
        segmentDao: SegmentDao
    ): CategoryRepository {
        return CategoryRepository(categoryDao, segmentDao)
    }

    @Provides
    @Singleton
    fun provideSegmentRepository(segmentDao: SegmentDao): SegmentRepository {
        return SegmentRepository(segmentDao)
    }
}
