package com.aliasgame.app.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val engine: GameEngine,
    private val repository: WordsRepository
) : ViewModel() {
    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages = _languages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("EN")
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedPack = MutableStateFlow<String>("Easy")
    val selectedPack = _selectedPack.asStateFlow()

    private val _packs = MutableStateFlow<List<String>>(emptyList())
    val packs = _packs.asStateFlow()

    init {
        viewModelScope.launch {
            _languages.value = repository.getLanguages()
            loadPacks(_selectedLanguage.value)
        }
    }

    fun onLanguageSelected(language: String) {
        _selectedLanguage.value = language
        loadPacks(language)


        val systemLangCode = if (language.uppercase() == "UA") "uk" else language.lowercase()
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(systemLangCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun onPackSelected(packId: String) {
        _selectedPack.value = packId
    }

    private fun loadPacks(language: String) {
        viewModelScope.launch {
            val availablePacks = repository.getPacks(language)
            _packs.value = availablePacks
            _selectedPack.value = availablePacks.firstOrNull() ?: "Easy"
        }
    }


    fun startGame(
        teamNames: List<String>,
        roundTime: Long,
        targetScore: Int
    ) {
        viewModelScope.launch {
            val words = repository.getWords(
                language = _selectedLanguage.value,
                packId = _selectedPack.value
            )

            engine.setupGame(
                teamNames,
                engine.settings.copy(
                    roundTime = roundTime,
                    targetScore = targetScore
                ),
                words
            )
        }

    }
}
