package com.aliasgame.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.model.WordStat
import com.aliasgame.app.domain.model.WordStatistics
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    /** `false` until the statistics have been read for the first time. */
    val isLoaded: Boolean = false,
    /** Languages that have skipped words; the picker is shown only for more than one. */
    val languages: List<String> = emptyList(),
    val selectedLanguage: String? = null,
    val hardestWords: List<WordStat> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val wordStatsRepository: WordStatsRepository
) : ViewModel() {

    /** The language picked on this screen; `null` follows the game language from the settings. */
    private val pickedLanguage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        wordStatsRepository.stats,
        settingsRepository.selectedLanguage,
        pickedLanguage
    ) { stats, gameLanguage, picked ->
        val languages = WordStatistics.languagesWithSkips(stats)
        val selected = picked?.takeIf { it in languages }
            ?: gameLanguage.takeIf { it in languages }
            ?: languages.firstOrNull()
        StatisticsUiState(
            isLoaded = true,
            languages = languages,
            selectedLanguage = selected,
            hardestWords = selected?.let { WordStatistics.hardest(stats, it) }.orEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun onLanguageSelected(language: String) {
        pickedLanguage.value = language
    }

    fun onResetConfirmed() {
        viewModelScope.launch { wordStatsRepository.clear() }
    }
}
