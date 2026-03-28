package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.Word

interface WordsRepository {
    suspend fun getWordsByPack(packId: String) : List<Word>

}