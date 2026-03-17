package com.livesplit.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpeedrunApi {

    @GET("games")
    suspend fun searchGames(
        @Query("name") name: String,
        @Query("max") max: Int = 20
    ): Response<GameSearchResponse>

    @GET("games/{id}")
    suspend fun getGame(
        @Path("id") gameId: String
    ): Response<SpeedrunGame>

    @GET("games/{id}/categories")
    suspend fun getCategories(
        @Path("id") gameId: String
    ): Response<CategoriesResponse>

    @GET("leaderboards/{gameId}/category/{categoryId}")
    suspend fun getLeaderboard(
        @Path("gameId") gameId: String,
        @Path("categoryId") categoryId: String,
        @Query("top") top: Int = 1,
        @Query("embedGame") embedGame: Boolean = false
    ): Response<LeaderboardResponse>

    @GET("users/{userId}/personal-bests")
    suspend fun getUserPersonalBests(
        @Path("userId") userId: String,
        @Query("game") gameId: String? = null
    ): Response<RunsResponse>
}
