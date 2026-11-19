package com.aliasgame.app.presentation.setup

import com.aliasgame.app.domain.model.TeamNameError
import com.aliasgame.app.domain.model.TeamNames
import com.aliasgame.app.domain.model.WordPack

/**
 * Represent the UI state for the Game Setup screen.
 * Global settings (language, sound, vibration) are now managed in SettingsViewModel.
 * Values for time and score are now integers to match bounded stepper logic.
 */
data class SetupUiState(
    val selectedLanguage: String = "EN",
    val packs: List<WordPack> = emptyList(),
    val isLoadingPacks: Boolean = false,
    val hasPacksLoadError: Boolean = false,
    val selectedPack: String = "Easy",
    val roundTime: Long = 60,
    val targetScore: Int = 50,
    val teamNames: List<String> = listOf("", ""),
    val isLoading: Boolean = false,
    val isEntranceAnimationPlayed: Boolean = false,
    val errorMessage: String? = null
) {
    /** The problem with each team name, or `null` where it is fine. */
    val teamNameErrors: List<TeamNameError?> get() = TeamNames.errors(teamNames)

    /** Every team needs a name, and no two teams may share one. */
    val canStartGame: Boolean get() = !isLoading && teamNameErrors.all { it == null }
}

/**
 * One-time side effects for the Setup screen.
 */
sealed interface SetupUiEffect {
    data object NavigateToGame : SetupUiEffect
    data class ShowError(val message: String) : SetupUiEffect
    data object ShowNoWordsAvailable : SetupUiEffect
}
