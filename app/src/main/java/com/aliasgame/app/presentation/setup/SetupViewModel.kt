package com.aliasgame.app.presentation.setup

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing game setup state and persisting user preferences.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val engine: GameEngine,
    private val repository: WordsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages = _languages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("EN")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedPack = MutableStateFlow("Easy")
    val selectedPack = _selectedPack.asStateFlow()

    private val _packs = MutableStateFlow<List<String>>(emptyList())
    val packs = _packs.asStateFlow()

    // Observable UI state for game parameters
    private val _roundTime = MutableStateFlow(60f)
    val roundTime = _roundTime.asStateFlow()

    private val _targetScore = MutableStateFlow(50f)
    val targetScore = _targetScore.asStateFlow()

    private val _teamNames = MutableStateFlow<List<String>>(listOf("Team 1", "Team 2"))
    val teamNames = _teamNames.asStateFlow()

    init {
        viewModelScope.launch {
            _languages.value = repository.getLanguages()

            // Observe and synchronize persistent settings with UI state
            launch { settingsRepository.selectedLanguage.collect { _selectedLanguage.value = it } }
            launch { settingsRepository.roundTime.collect { _roundTime.value = it.toFloat() } }
            launch { settingsRepository.targetScore.collect { _targetScore.value = it.toFloat() } }
            launch { settingsRepository.teamNames.collect { _teamNames.value = it } }

            loadPacks(_selectedLanguage.value)
        }
    }

    /**
     * Updates the selected language, persists the change, and applies the system locale.
     */
    fun onLanguageSelected(language: String) {
        _selectedLanguage.value = language
        loadPacks(language)

        viewModelScope.launch { settingsRepository.saveLanguage(language) }

        val systemLangCode = if (language.uppercase() == "UA") "uk" else language.lowercase()
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(systemLangCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun onRoundTimeChanged(newTime: Float) {
        _roundTime.value = newTime
        viewModelScope.launch { settingsRepository.saveRoundTime(newTime.toLong()) }
    }

    fun onTargetScoreChanged(newScore: Float) {
        _targetScore.value = newScore
        viewModelScope.launch { settingsRepository.saveTargetScore(newScore.toInt()) }
    }

    fun onTeamNamesChanged(newNames: List<String>) {
        _teamNames.value = newNames
        viewModelScope.launch { settingsRepository.saveTeamNames(newNames) }
    }

    fun onPackSelected(packId: String) {
        _selectedPack.value = packId
    }

    private fun loadPacks(language: String) {
        viewModelScope.launch {
            val availablePacks = repository.getPacks(language)
            _packs.value = availablePacks
            if (_selectedPack.value !in availablePacks) {
                _selectedPack.value = availablePacks.firstOrNull() ?: "Easy"
            }
        }
    }

    /**
     * Configures the game engine and signals navigation to the match screen.
     */
    fun startGame(onNavigate: () -> Unit) {
        viewModelScope.launch {
            val words = repository.getWords(
                language = _selectedLanguage.value,
                packId = _selectedPack.value
            )

            engine.setupGame(
                _teamNames.value,
                engine.settings.copy(
                    roundTime = _roundTime.value.toLong(),
                    targetScore = _targetScore.value.toInt()
                ),
                words
            )
            onNavigate()
        }
    }
}
