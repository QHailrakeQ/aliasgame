package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.AppStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val selectedLanguage: Flow<String>
    val roundTime: Flow<Long>
    val targetScore: Flow<Int>
    val teamNames: Flow<List<String>>
    val selectedPack: Flow<String>
    val isSoundEnabled: Flow<Boolean>
    val isVibrationEnabled: Flow<Boolean>
    val appStyle: Flow<AppStyle>


    suspend fun saveSoundEnabled(enabled: Boolean)
    suspend fun saveVibrationEnabled(enabled: Boolean)
    suspend fun saveAppStyle(style: AppStyle)
    suspend fun saveLanguage(language: String)
    suspend fun saveRoundTime(time: Long)
    suspend fun saveTargetScore(score: Int)
    suspend fun saveTeamNames(names: List<String>)
    suspend fun savePack(packId: String)
}