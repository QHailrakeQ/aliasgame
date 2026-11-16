package com.aliasgame.app.data.repository

import android.content.Context
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
private data class WordDto(
    val text: String,
    val packId: String
)

private fun WordDto.toDomain() = Word(
    text = text,
    packId = packId
)

class WordsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WordsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun getInternalLanguageCode(language: String): String {
        return if (language.uppercase() == "UA") "uk" else language.lowercase()
    }

    override suspend fun getWords(
        language: String,
        packId: String
    ): List<Word> {
        val langCode = getInternalLanguageCode(language)
        val fileName = "words_${langCode}.json"
        val jsonString = context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
        val allDtos: List<WordDto> = json.decodeFromString(jsonString)
        return allDtos.map { it.toDomain() }.filter { it.packId == packId }
    }

    override suspend fun getPacks(language: String): List<String> {
        val langCode = getInternalLanguageCode(language)
        val fileName = "words_${langCode}.json"
        val jsonString = context.assets.open(fileName)
            .bufferedReader()
            .use { it.readText() }
        val allDtos: List<WordDto> = json.decodeFromString(jsonString)
        return allDtos.map { it.packId }.distinct()
    }

    override suspend fun getLanguages(): List<String> {
        return listOf("EN", "UA", "RU", "DE")
    }
}
