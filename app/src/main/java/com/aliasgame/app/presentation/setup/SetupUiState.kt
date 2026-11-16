package com.aliasgame.app.presentation.setup

/**
 * Represent the UI state for the Game Setup screen.
 * Global settings (language, sound, vibration) are now managed in SettingsViewModel.
 * Values for time and score are now integers to match bounded stepper logic.
 */
data class SetupUiState(
    val selectedLanguage: String = "EN",
    val packs: List<String> = emptyList(),
    val isLoadingPacks: Boolean = false,
    val hasPacksLoadError: Boolean = false,
    val selectedPack: String = "Easy",
    val roundTime: Long = 60,
    val targetScore: Int = 50,
    val teamNames: List<String> = listOf("", ""),
    val isLoading: Boolean = false,
    val isEntranceAnimationPlayed: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One-time side effects for the Setup screen.
 */
sealed interface SetupUiEffect {
    data object NavigateToGame : SetupUiEffect
    data class ShowError(val message: String) : SetupUiEffect
    data object ShowNoWordsAvailable : SetupUiEffect
}
