package com.aliasgame.app.presentation.setup

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import com.aliasgame.app.domain.usecase.GetPacksUseCase
import com.aliasgame.app.domain.usecase.GetWordsUseCase
import com.aliasgame.app.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing game setup state and persisting user preferences.
 * Uses a unified SetupUiState and encapsulates logic in Use Cases.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val engine: GameEngine,
    private val repository: WordsRepository,
    private val settingsRepository: SettingsRepository,
    private val getWordsUseCase: GetWordsUseCase,
    private val getPacksUseCase: GetPacksUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<SetupUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(languages = repository.getLanguages()) }

            // Initial synchronization with persistent storage
            launch {
                settingsRepository.selectedLanguage.collect { lang ->
                    _uiState.update { it.copy(selectedLanguage = lang) }
                    loadPacks(lang)
                }
            }
            launch {
                settingsRepository.roundTime.collect { time ->
                    _uiState.update { it.copy(roundTime = time.toFloat()) }
                }
            }
            launch {
                settingsRepository.targetScore.collect { score ->
                    _uiState.update { it.copy(targetScore = score.toFloat()) }
                }
            }
            launch {
                settingsRepository.teamNames.collect { names ->
                    _uiState.update { it.copy(teamNames = names) }
                }
            }
            launch {
                settingsRepository.selectedPack.collect { pack ->
                    _uiState.update { it.copy(selectedPack = pack) }
                }
            }
            launch {
                settingsRepository.isSoundEnabled.collect { enabled ->
                    _uiState.update { it.copy(isSoundEnabled = enabled) }
                }
            }
            launch {
                settingsRepository.isVibrationEnabled.collect { enabled ->
                    _uiState.update { it.copy(isVibrationEnabled = enabled) }
                }
            }
        }
    }

    fun onLanguageSelected(language: String) {
        viewModelScope.launch {
            saveSettingsUseCase.saveLanguage(language)
            val systemLangCode = if (language.uppercase() == "UA") "uk" else language.lowercase()
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(systemLangCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun onSoundToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveSoundEnabled(enabled) }
    }

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveVibrationEnabled(enabled) }
    }

    fun onRoundTimeChanged(newTime: Float) {
        viewModelScope.launch { saveSettingsUseCase.saveRoundTime(newTime.toLong()) }
    }

    fun onTargetScoreChanged(newScore: Float) {
        viewModelScope.launch { saveSettingsUseCase.saveTargetScore(newScore.toInt()) }
    }

    fun onTeamNamesChanged(newNames: List<String>) {
        viewModelScope.launch { saveSettingsUseCase.saveTeamNames(newNames) }
    }

    fun onPackSelected(packId: String) {
        viewModelScope.launch { saveSettingsUseCase.savePack(packId) }
    }

    private fun loadPacks(language: String) {
        viewModelScope.launch {
            val availablePacks = getPacksUseCase(language)
            _uiState.update { state ->
                val nextPack = if (state.selectedPack !in availablePacks) {
                    availablePacks.firstOrNull() ?: "Easy"
                } else state.selectedPack
                state.copy(packs = availablePacks, selectedPack = nextPack)
            }
        }
    }

    /**
     * Configures the game engine using GetWordsUseCase.
     * Implements error handling for missing assets or empty packs.
     */
    fun startGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getWordsUseCase(
                language = _uiState.value.selectedLanguage,
                packId = _uiState.value.selectedPack
            ).onSuccess { words ->
                engine.setupGame(
                    _uiState.value.teamNames,
                    engine.settings.copy(
                        roundTime = _uiState.value.roundTime.toLong(),
                        targetScore = _uiState.value.targetScore.toInt()
                    ),
                    words
                )
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.send(SetupUiEffect.NavigateToGame)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.send(SetupUiEffect.ShowError(error.message ?: "Failed to load words"))
            }
        }
    }
}
