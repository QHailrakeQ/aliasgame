package com.aliasgame.app.domain.usecase

import com.aliasgame.app.domain.repository.WordsRepository
import javax.inject.Inject

/**
 * Use case to retrieve the list of available word packs (categories) for a given language.
 */
class GetPacksUseCase @Inject constructor(
    private val repository: WordsRepository
) {
    suspend operator fun invoke(language: String): List<String> {
        return try {
            repository.getPacks(language)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
