package com.aliasgame.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.model.AppLanguages
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
        }
    }

    fun onLanguageSelected(language: String) {
        if (_uiState.value.selectedLanguage == language) return
        
        viewModelScope.launch {
            saveSettingsUseCase.saveLanguage(language)
            val appLocale = LocaleListCompat.forLanguageTags(AppLanguages.toLanguageTag(language))
            // Persisted by AppCompat itself: by the system on API 33+, and on older versions via
            // AppLocalesMetadataHolderService (autoStoreLocales) declared in the manifest.
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun onSoundToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveSoundEnabled(enabled) }
    }

    fun onVibrationToggled(enabled: Boolean) {
        viewModelScope.launch { saveSettingsUseCase.saveVibrationEnabled(enabled) }
    }
}
