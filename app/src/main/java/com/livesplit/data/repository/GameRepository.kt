package com.livesplit.data.repository

import com.livesplit.data.db.GameDao
import com.livesplit.data.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao
) {
    fun getAllGames(): Flow<List<Game>> = gameDao.getAllGames()

    suspend fun getGameById(id: Long): Game? = gameDao.getGameById(id)

    fun searchGames(query: String): Flow<List<Game>> = gameDao.searchGames(query)

    suspend fun addGame(name: String): Long {
        val game = Game(name = name)
        return gameDao.insert(game)
    }

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun renameGame(id: Long, name: String) = gameDao.rename(id, name)

    suspend fun deleteGame(game: Game) = gameDao.delete(game)

    suspend fun deleteGameById(id: Long) = gameDao.deleteById(id)
}
