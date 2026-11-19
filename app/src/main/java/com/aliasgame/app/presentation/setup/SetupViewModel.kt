package com.aliasgame.app.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliasgame.app.domain.engine.GameEngine
import com.aliasgame.app.domain.model.GameSettings
import com.aliasgame.app.domain.model.TeamNames
import com.aliasgame.app.domain.repository.GameSessionRepository
import com.aliasgame.app.domain.repository.SettingsRepository
import com.aliasgame.app.domain.repository.WordsRepository
import com.aliasgame.app.domain.usecase.GetPacksUseCase
import com.aliasgame.app.domain.usecase.EmptyWordPackException
import com.aliasgame.app.domain.usecase.GetWordsUseCase
import com.aliasgame.app.domain.usecase.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * ViewModel for Game Setup.
 * Manages game rules using bounded integer increments/decrements.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: WordsRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: GameSessionRepository,
    private val getWordsUseCase: GetWordsUseCase,
    private val getPacksUseCase: GetPacksUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<SetupUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private var settingsJob: Job? = null
    private var packsJob: Job? = null

    private val namePool = MutableStateFlow<List<String>>(emptyList())
    /** The pool the current names were rolled from; `null` until names have been rolled. */
    private var appliedNamePool: List<String>? = null

    // Constants for game rules
    companion object {
        const val MIN_ROUND_TIME = 10L
        const val MAX_ROUND_TIME = 120L
        const val STEP_ROUND_TIME = 10L

        const val MIN_TARGET_SCORE = 10
        const val MAX_TARGET_SCORE = 100
        const val STEP_TARGET_SCORE = 10
    }

    init {
        viewModelScope.launch {
            // Synchronize with global language to load correct packs
            launch {
                settingsRepository.selectedLanguage.collect { lang ->
                    _uiState.update { it.copy(selectedLanguage = lang) }
                    loadPacks(lang)
                }
            }
            
            // Observe game-specific persistent settings with clamping
            launch {
                settingsRepository.roundTime.collect { time ->
                    _uiState.update { it.copy(roundTime = normalizeRoundTime(time)) }
                }
            }
            launch {
                settingsRepository.targetScore.collect { score ->
                    _uiState.update { it.copy(targetScore = normalizeTargetScore(score)) }
                }
            }
            launch {
                // Only the number of teams is kept between games; the names are rolled afresh
                // every time from the funny-name pool of the current language.
                val savedTeamCount = settingsRepository.teamNames.first().size
                    .coerceAtLeast(TeamNames.MIN_TEAMS)
                namePool.filter { it.isNotEmpty() }.collect { pool ->
                    applyNamePool(pool, savedTeamCount)
                }
            }
            launch {
                settingsRepository.selectedPack.collect { pack ->
                    _uiState.update { it.copy(selectedPack = pack) }
                }
            }
        }
    }

    fun onEntranceAnimationFinished() {
        _uiState.update { it.copy(isEntranceAnimationPlayed = true) }
    }

    fun onRoundTimeChanged(newTime: Long) {
        val normalizedTime = normalizeRoundTime(newTime)
        if (_uiState.value.roundTime == normalizedTime) return
        
        _uiState.update { it.copy(roundTime = normalizedTime) }
        debounceSettingsSave { saveSettingsUseCase.saveRoundTime(normalizedTime) }
    }

    fun onTargetScoreChanged(newScore: Int) {
        val normalizedScore = normalizeTargetScore(newScore)
        if (_uiState.value.targetScore == normalizedScore) return

        _uiState.update { it.copy(targetScore = normalizedScore) }
        debounceSettingsSave { saveSettingsUseCase.saveTargetScore(normalizedScore) }
    }

    private fun normalizeRoundTime(value: Long): Long =
        normalizeStepperValue(value, MIN_ROUND_TIME, MAX_ROUND_TIME, STEP_ROUND_TIME)

    private fun normalizeTargetScore(value: Int): Int =
        normalizeStepperValue(
            value.toLong(),
            MIN_TARGET_SCORE.toLong(),
            MAX_TARGET_SCORE.toLong(),
            STEP_TARGET_SCORE.toLong()
        ).toInt()

    private fun normalizeStepperValue(value: Long, min: Long, max: Long, step: Long): Long {
        val bounded = value.coerceIn(min, max)
        val steps = ((bounded - min).toDouble() / step).roundToLong()
        return (min + steps * step).coerceIn(min, max)
    }

    /**
     * The funny team names of the current app language, read from resources by the screen
     * (it has the right locale). Called again with another pool when the language changes.
     */
    fun onTeamNamePoolChanged(pool: List<String>) {
        namePool.value = pool
    }

    fun onTeamNameChanged(index: Int, name: String) =
        updateTeamNames { names -> names.apply { set(index, name.take(TeamNames.MAX_LENGTH)) } }

    fun onTeamNameCleared(index: Int) = updateTeamNames { names -> names.apply { set(index, "") } }

    /** Replaces one name with a random funny name that no other team uses. */
    fun onTeamNameRerolled(index: Int) {
        val pool = namePool.value.ifEmpty { return }
        updateTeamNames { names ->
            val others = names.filterIndexed { i, _ -> i != index }
            // The current name is excluded too, so a reroll always shows a different name.
            names.apply { set(index, TeamNames.randomUnused(pool, others + names[index])) }
        }
    }

    fun onTeamAdded() {
        val pool = namePool.value.ifEmpty { return }
        updateTeamNames { names -> names.apply { add(TeamNames.randomUnused(pool, names)) } }
    }

    fun onTeamRemoved(index: Int) {
        if (_uiState.value.teamNames.size <= TeamNames.MIN_TEAMS) return
        updateTeamNames { names -> names.apply { removeAt(index) } }
    }

    private inline fun updateTeamNames(change: (MutableList<String>) -> MutableList<String>) {
        val current = _uiState.value.teamNames
        val updated: List<String> = change(current.toMutableList())
        if (updated == current) return
        _uiState.update { it.copy(teamNames = updated) }
        debounceSettingsSave { saveSettingsUseCase.saveTeamNames(updated) }
    }

    /**
     * Rolls the names when the first pool arrives. A later pool means the language changed:
     * names that were rolled from the previous pool are rolled again in the new language,
     * names the players typed themselves are kept.
     */
    private fun applyNamePool(pool: List<String>, initialTeamCount: Int) {
        val previousPool = appliedNamePool
        appliedNamePool = pool
        val current = _uiState.value.teamNames
        val updated = if (previousPool == null) {
            TeamNames.randomDistinct(pool, initialTeamCount)
        } else {
            current.foldIndexed(mutableListOf<String>()) { index, result, name ->
                val keep = name.isNotBlank() && !TeamNames.isPoolName(name, previousPool)
                result += if (keep) name else TeamNames.randomUnused(pool, result + current.drop(index + 1))
                result
            }
        }
        _uiState.update { it.copy(teamNames = updated) }
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
        packsJob?.cancel()
        _uiState.update {
            it.copy(isLoadingPacks = true, hasPacksLoadError = false)
        }
        packsJob = viewModelScope.launch {
            getPacksUseCase(language)
                .onSuccess { availablePacks ->
                    _uiState.update { state ->
                        val nextPack = if (availablePacks.none { it.id == state.selectedPack }) {
                            availablePacks.firstOrNull()?.id ?: "Easy"
                        } else state.selectedPack
                        state.copy(
                            packs = availablePacks,
                            selectedPack = nextPack,
                            isLoadingPacks = false,
                            hasPacksLoadError = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingPacks = false, hasPacksLoadError = true)
                    }
                    _uiEffect.send(SetupUiEffect.ShowError(error.message ?: "Failed to load categories"))
                }
        }
    }

    fun retryLoadPacks() {
        loadPacks(_uiState.value.selectedLanguage)
    }

    fun startGame() {
        // The button is disabled in this case; the guard keeps the rule in one testable place.
        if (!_uiState.value.canStartGame) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getWordsUseCase(
                language = _uiState.value.selectedLanguage,
                packId = _uiState.value.selectedPack
            ).onSuccess { words ->
                if (words.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(SetupUiEffect.ShowNoWordsAvailable)
                    return@onSuccess
                }

                val state = _uiState.value
                // The game screen loads its engine from this session, exactly as it does after
                // process death, so starting and restoring a game share one code path.
                sessionRepository.saveSession(
                    GameEngine.newSession(
                        teamNames = state.teamNames.map { it.trim() },
                        settings = GameSettings(roundTime = state.roundTime, targetScore = state.targetScore),
                        language = state.selectedLanguage,
                        packId = state.selectedPack
                    )
                )
                _uiState.update { it.copy(isLoading = false) }
                _uiEffect.send(SetupUiEffect.NavigateToGame)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                if (error is EmptyWordPackException) {
                    _uiEffect.send(SetupUiEffect.ShowNoWordsAvailable)
                } else {
                    _uiEffect.send(SetupUiEffect.ShowError(error.message ?: "Failed to load words"))
                }
            }
        }
    }
}
