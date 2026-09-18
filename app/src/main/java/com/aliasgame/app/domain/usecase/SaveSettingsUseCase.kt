package com.aliasgame.app.domain.usecase

import com.aliasgame.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for persisting various game settings.
 * Encapsulates the logic for updating round duration, target scores, team names, and local preferences.
 */
class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun saveRoundTime(time: Long) = settingsRepository.saveRoundTime(time)
    suspend fun saveTargetScore(score: Int) = settingsRepository.saveTargetScore(score)
    suspend fun saveTeamNames(names: List<String>) = settingsRepository.saveTeamNames(names)
    suspend fun saveLanguage(language: String) = settingsRepository.saveLanguage(language)
    suspend fun savePack(packId: String) = settingsRepository.savePack(packId)
    suspend fun saveSoundEnabled(enabled: Boolean) = settingsRepository.saveSoundEnabled(enabled)
    suspend fun saveVibrationEnabled(enabled: Boolean) = settingsRepository.saveVibrationEnabled(enabled)
}
