package com.aliasgame.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.model.AppLanguages
import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val selectedLanguage: String = AppLanguages.DEFAULT,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val appStyle: AppStyle = AppStyle.DEFAULT,
    val availableLanguages: List<String> = AppLanguages.supported
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                settingsRepository.selectedLanguage.collect { lang ->
                    _uiState.update { it.copy(selectedLanguage = lang) }
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
            launch {
                settingsRepository.appStyle.collect { style ->
                    _uiState.update { it.copy(appStyle = style) }
                }
            }
        }
    }

    /**
     * Applies [language] to the interface immediately and stores it for the word packs.
     * @return `true` if the language changed.
     */
    fun onLanguageSelected(language: String): Boolean {
        if (_uiState.value.selectedLanguage == language) return false
        _uiState.update { it.copy(selectedLanguage = language) }

        // Applied synchronously, so a screen recreated right after this call already uses it.
        // Persisted by AppCompat itself: by the system on API 33+, and on older versions via
        // AppLocalesMetadataHolderService (autoStoreLocales) declared in the manifest.
        val appLocale = LocaleListCompat.forLanguageTags(AppLanguages.toLanguageTag(language))
        AppCompatDelegate.setApplicationLocales(appLocale)
        viewModelScope.launch { saveSettingsUseCase.saveLanguage(language) }
        return true
    }

    fun onSoundToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveSoundEnabled(enabled) }
    }

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveVibrationEnabled(enabled) }
    }

    /** Stores [style]; the whole app follows it as soon as it is saved. */
    fun onStyleSelected(style: AppStyle) {
        if (_uiState.value.appStyle == style) return
        _uiState.update { it.copy(appStyle = style) }
        viewModelScope.launch { saveSettingsUseCase.saveAppStyle(style) }
    }
}
