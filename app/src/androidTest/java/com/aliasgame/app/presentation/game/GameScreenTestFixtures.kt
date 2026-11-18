package com.aliasgame.app.presentation.game

import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.GameSessionSnapshot
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * A GameViewModel with in-memory repositories and a freshly started two-team game,
 * so GameScreen can be tested without Hilt, DataStore or assets.
 */
fun newGameViewModelForTest(): GameViewModel {
    val sessionRepository = InMemoryGameSessionRepository().apply {
        sessionSnapshot.value = GameEngine.newSession(
            teamNames = listOf("Team A", "Team B"),
            settings = GameSettings(roundTime = 60, targetScore = 50),
            language = "EN",
            packId = "Easy"
        )
    }
    return GameViewModel(SilentSettingsRepository(), TenWordsRepository(), sessionRepository)
}

private class SilentSettingsRepository : SettingsRepository {
    override val selectedLanguage = flowOf("EN")
    override val roundTime = flowOf(60L)
    override val targetScore = flowOf(50)
    override val teamNames = flowOf(listOf("Team A", "Team B"))
    override val selectedPack = flowOf("Easy")
    override val isSoundEnabled = flowOf(false)
    override val isVibrationEnabled = flowOf(false)
    override suspend fun saveLanguage(language: String) {}
    override suspend fun saveRoundTime(time: Long) {}
    override suspend fun saveTargetScore(score: Int) {}
    override suspend fun saveTeamNames(names: List<String>) {}
    override suspend fun savePack(packId: String) {}
    override suspend fun saveSoundEnabled(enabled: Boolean) {}
    override suspend fun saveVibrationEnabled(enabled: Boolean) {}
}

private class TenWordsRepository : WordsRepository {
    override suspend fun getWords(language: String, packId: String) = List(10) { Word("Word $it", "Easy") }
    override suspend fun getLanguages() = listOf("EN")
    override suspend fun getPacks(language: String) = listOf("Easy")
}

private class InMemoryGameSessionRepository : GameSessionRepository {
    override val sessionSnapshot = MutableStateFlow<GameSessionSnapshot?>(null)
    override suspend fun saveSession(snapshot: GameSessionSnapshot) {
        sessionSnapshot.value = snapshot
    }
    override suspend fun clearSession() {
        sessionSnapshot.value = null
    }
}
