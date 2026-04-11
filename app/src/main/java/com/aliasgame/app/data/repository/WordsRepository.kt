package com.aliasgame.app.data.repository

import android.content.Context
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

class WordsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WordsRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getWordsByPack(
        language: String,
        packId: String
    ): List<Word> {
        val fileName = "words_${language.lowercase()}.json"
        return try {
            val jsonString = context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }
            val allWords: List<Word> = json.decodeFromString<List<Word>>(jsonString)
            allWords.filter { it.packId == packId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPacks(language: String): List<String> {
        val fileName = "words_${language.lowercase()}.json"
        return try {
            val jsonString = context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }
            val allWords: List<Word> = json.decodeFromString<List<Word>>(jsonString)
            allWords.map { it.packId }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getLanguages(): List<String> {
        return listOf("EN",  "UK" , "RU", "DE")
    }
}
