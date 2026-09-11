package com.aliasgame.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val selectedLanguage: Flow<String>
    val roundTime: Flow<Long>
    val targetScore: Flow<Int>
    val teamNames: Flow<List<String>>
    val selectedPack: Flow<String>

    suspend fun saveLanguage(language: String)
    suspend fun saveRoundTime(time: Long)
    suspend fun saveTargetScore(score: Int)
    suspend fun saveTeamNames(names: List<String>)
    suspend fun savePack(packId: String)
}