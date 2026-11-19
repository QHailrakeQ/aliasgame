package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.WordPack

interface WordsRepository {
    /** Words of [packId]; [WordPack.MIXED_ID] returns every word of the language. */
    suspend fun getWords(language: String, packId: String): List<Word>
    suspend fun getLanguages(): List<String>
    /** The language's packs in file order, followed by the mixed pack when there are several. */
    suspend fun getPacks(language: String): List<WordPack>
}
