package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.Word

interface WordsRepository {
    suspend fun getWordsByPack(language: String, packId: String): List<Word>
    suspend fun getPacks(language: String): List<String>
    suspend fun getLanguages(): List<String>
}


