package com.aliasgame.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state of [com.aliasgame.app.MainActivity]: the visual style every screen uses. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    /** The style chosen in the settings, or `null` until it has been read from storage. */
    val appStyle: StateFlow<AppStyle?> = settingsRepository.appStyle
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
