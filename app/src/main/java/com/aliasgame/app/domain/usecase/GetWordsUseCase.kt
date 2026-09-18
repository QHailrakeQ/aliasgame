package com.aliasgame.app.domain.usecase

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.WordsRepository
import javax.inject.Inject

/**
 * Use case to fetch words for a specific language and pack.
 * Includes validation to ensure the game doesn't start with an empty dictionary.
 */
class GetWordsUseCase @Inject constructor(
    private val repository: WordsRepository
) {
    suspend operator fun invoke(language: String, packId: String): Result<List<Word>> {
        return try {
            val words = repository.getWords(language, packId)
            if (words.isEmpty()) {
                Result.failure(Exception("No words found for this category"))
            } else {
                Result.success(words)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
