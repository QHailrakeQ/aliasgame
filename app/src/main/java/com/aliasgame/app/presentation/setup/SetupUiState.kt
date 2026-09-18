package com.aliasgame.app.presentation.setup

/**
 * Represent the complete UI state for the Setup screen.
 */
data class SetupUiState(
    val languages: List<String> = emptyList(),
    val selectedLanguage: String = "EN",
    val packs: List<String> = emptyList(),
    val selectedPack: String = "Easy",
    val roundTime: Float = 60f,
    val targetScore: Float = 50f,
    val teamNames: List<String> = listOf("", ""),
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * One-time side effects for the Setup screen.
 */
sealed interface SetupUiEffect {
    data object NavigateToGame : SetupUiEffect
    data class ShowError(val message: String) : SetupUiEffect
}
