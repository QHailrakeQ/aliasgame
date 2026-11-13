package com.aliasgame.app.domain.usecase

import com.aliasgame.app.domain.repository.WordsRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Use case to retrieve the list of available word packs (categories) for a given language.
 */
class GetPacksUseCase @Inject constructor(
    private val repository: WordsRepository
) {
    suspend operator fun invoke(language: String): Result<List<String>> {
        return try {
            Result.success(repository.getPacks(language))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
