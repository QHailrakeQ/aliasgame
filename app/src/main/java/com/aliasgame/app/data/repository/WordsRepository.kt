package com.aliasgame.app.data.repository

import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.repository.WordsRepository

class WordsRepositoryImpl : WordsRepository {
    override suspend fun getWordsByPack(packId: String): List<Word> {
        return emptyList()
    }
}
