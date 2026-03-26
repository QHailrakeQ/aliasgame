package com.aliasgame.app.data.repository

class WordsRepositoryImpl : WordsRepository {
    override suspend fun getWordsByPack(packId: String): List<Word> {
        return emptyList()
    }
}