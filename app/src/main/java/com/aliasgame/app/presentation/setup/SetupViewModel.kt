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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing game setup state and persisting user preferences.
 * Implements debouncing for I/O and activity-recreating operations for smooth UX.
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

    private var languageJob: Job? = null
    private var settingsJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(languages = repository.getLanguages()) }

            // Observe and synchronize persistent storage
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

    /**
     * Signals that the initial entrance animation has been played to prevent replaying it.
     */
    fun onEntranceAnimationFinished() {
        _uiState.update { it.copy(isEntranceAnimationPlayed = true) }
    }

    /**
     * Updates language with optimistic UI update and debounced persistence.
     */
    fun onLanguageSelected(language: String) {
        if (_uiState.value.selectedLanguage == language) return
        
        // Optimistic update for immediate visual feedback
        _uiState.update { it.copy(selectedLanguage = language) }
        loadPacks(language)
        
        languageJob?.cancel()
        languageJob = viewModelScope.launch {
            delay(500) // Slightly longer delay to let user finish selecting
            saveSettingsUseCase.saveLanguage(language)
            val systemLangCode = if (language.uppercase() == "UA") "uk" else language.lowercase()
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(systemLangCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun onSoundToggled(enabled: Boolean) {
        _uiState.update { it.copy(isSoundEnabled = enabled) }
        viewModelScope.launch { saveSettingsUseCase.saveSoundEnabled(enabled) }
    }

    fun onVibrationToggled(enabled: Boolean) {
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
        viewModelScope.launch { saveSettingsUseCase.saveVibrationEnabled(enabled) }
    }

    fun onRoundTimeChanged(newTime: Float) {
        _uiState.update { it.copy(roundTime = newTime) }
        debounceSettingsSave { saveSettingsUseCase.saveRoundTime(newTime.toLong()) }
    }

    fun onTargetScoreChanged(newScore: Float) {
        _uiState.update { it.copy(targetScore = newScore) }
        debounceSettingsSave { saveSettingsUseCase.saveTargetScore(newScore.toInt()) }
    }

    fun onTeamNamesChanged(newNames: List<String>) {
        _uiState.update { it.copy(teamNames = newNames) }
        debounceSettingsSave { saveSettingsUseCase.saveTeamNames(newNames) }
    }

    fun onPackSelected(packId: String) {
        _uiState.update { it.copy(selectedPack = packId) }
        viewModelScope.launch { saveSettingsUseCase.savePack(packId) }
    }

    private fun debounceSettingsSave(action: suspend () -> Unit) {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            delay(300)
            action()
        }
    }

    private fun loadPacks(language: String) {
        viewModelScope.launch {
            getPacksUseCase(language)
                .onSuccess { availablePacks ->
                    _uiState.update { state ->
                        val nextPack = if (state.selectedPack !in availablePacks) {
                            availablePacks.firstOrNull() ?: "Easy"
                        } else state.selectedPack
                        state.copy(packs = availablePacks, selectedPack = nextPack)
                    }
                }
                .onFailure { error ->
                    _uiEffect.send(SetupUiEffect.ShowError(error.message ?: "Failed to load categories"))
                }
        }
    }

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
