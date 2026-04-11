package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.Word

interface WordsRepository {
    suspend fun getWords(language: String, packId: String): List<Word>
    suspend fun getLanguages(): List<String>
    suspend fun getPacks(language: String): List<String>
}
