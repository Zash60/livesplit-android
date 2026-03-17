package com.livesplit.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Game search response
@JsonClass(generateAdapter = true)
data class GameSearchResponse(
    @Json(name = "data") val data: List<SpeedrunGame>
)

@JsonClass(generateAdapter = true)
data class SpeedrunGame(
    @Json(name = "id") val id: String,
    @Json(name = "names") val names: GameNames,
    @Json(name = "abbreviation") val abbreviation: String,
    @Json(name = "released") val released: Int?,
    @Json(name = "game_type") val gameType: String?
)

@JsonClass(generateAdapter = true)
data class GameNames(
    @Json(name = "international") val international: String,
    @Json(name = "japanese") val japanese: String?,
    @Json(name = "turkish") val turkish: String?,
    @Json(name = "romanized") val romanized: String?
)

// Categories response
@JsonClass(generateAdapter = true)
data class CategoriesResponse(
    @Json(name = "data") val data: List<SpeedrunCategory>
)

@JsonClass(generateAdapter = true)
data class SpeedrunCategory(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String, // "per-game", "full-game", "level"
    @Json(name = "rules") val rules: String?
)

// Leaderboard response
@JsonClass(generateAdapter = true)
data class LeaderboardResponse(
    @Json(name = "data") val data: SpeedrunLeaderboard
)

@JsonClass(generateAdapter = true)
data class SpeedrunLeaderboard(
    @Json(name = "game") val game: String,
    @Json(name = "category") val category: String,
    @Json(name = "platform") val platform: String?,
    @Json(name = "emulated") val emulated: Boolean,
    @Json(name = "video") val video: String?,
    @Json(name = "times") val times: LeaderboardTimes,
    @Json(name = "run") val run: LeaderboardRun,
    @Json(name = "players") val players: List<LeaderboardPlayer>
)

@JsonClass(generateAdapter = true)
data class LeaderboardTimes(
    @Json(name = "primary_t") val primaryT: Double,
    @Json(name = "primary") val primary: String,
    @Json(name = "realtime_t") val realtimeT: Double?,
    @Json(name = "realtime") val realtime: String?,
    @Json(name = "ingame_t") val ingameT: Double?,
    @Json(name = "ingame") val ingame: String?
)

@JsonClass(generateAdapter = true)
data class LeaderboardRun(
    @Json(name = "id") val id: String,
    @Json(name = "weblink") val weblink: String,
    @Json(name = "game") val game: String,
    @Json(name = "category") val category: String,
    @Json(name = "date") val date: String?,
    @Json(name = "times") val times: RunTimes,
    @Json(name = "system") val system: RunSystem?,
    @Json(name = "splits") val splits: List<RunSplit>?
)

@JsonClass(generateAdapter = true)
data class RunTimes(
    @Json(name = "primary_t") val primaryT: Double,
    @Json(name = "primary") val primary: String,
    @Json(name = "realtime_t") val realtimeT: Double?,
    @Json(name = "realtime") val realtime: String?,
    @Json(name = "ingame_t") val ingameT: Double?,
    @Json(name = "ingame") val ingame: String?
)

@JsonClass(generateAdapter = true)
data class RunSystem(
    @Json(name = "platform") val platform: String?,
    @Json(name = "emulated") val emulated: Boolean,
    @Json(name = "region") val region: String?
)

@JsonClass(generateAdapter = true)
data class RunSplit(
    @Json(name = "name") val name: String,
    @Json(name = "duration") val duration: Double,  // cumulative time in seconds
    @Json(name = "splitTime") val splitTime: String?
)

@JsonClass(generateAdapter = true)
data class LeaderboardPlayer(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "uri") val uri: String?
)

// Runs response for user's personal best
@JsonClass(generateAdapter = true)
data class RunsResponse(
    @Json(name = "data") val data: List<SpeedrunRun>
)

@JsonClass(generateAdapter = true)
data class SpeedrunRun(
    @Json(name = "id") val id: String,
    @Json(name = "game") val game: String,
    @Json(name = "category") val category: String,
    @Json(name = "level") val level: String?,
    @Json(name = "platform") val platform: String?,
    @Json(name = "date") val date: String?,
    @Json(name = "times") val runTimes: RunTimes,
    @Json(name = "comment") val comment: String?,
    @Json(name = "splits") val splits: List<RunSplit>?
)

