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
    override suspend fun getWordsByPack(packId: String): List<Word> {
        return try {
            val jsonString = context.assets.open("words_uk.json")
                .bufferedReader()
                .use { it.readText() }
            val allWords: List<Word> = Json.decodeFromString(jsonString)
            allWords.filter { it.packId == packId }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
