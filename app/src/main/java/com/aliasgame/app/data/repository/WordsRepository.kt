package com.aliasgame.app.data.repository

import android.content.Context
import com.aliasgame.app.di.IoDispatcher
import com.aliasgame.app.domain.model.AppLanguages
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.domain.model.WordPack
import com.aliasgame.app.domain.repository.WordsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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

/**
 * Parses a word-pack JSON document (a list of `{text, packId}` objects) into domain words.
 * Kept free of Android dependencies so it can be unit-tested on the JVM.
 *
 * @throws kotlinx.serialization.SerializationException if the document is malformed.
 */
internal object WordPackParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(document: String): List<Word> =
        json.decodeFromString<List<WordDto>>(document).map { it.toDomain() }

    fun fileNameFor(language: String): String =
        "words_${AppLanguages.toLanguageTag(language)}.json"
}

/**
 * Builds the playable packs from all words of a language, including the virtual
 * [WordPack.MIXED_ID] pack. Free of Android dependencies so it can be unit-tested on the JVM.
 */
internal object WordPackCatalog {

    /** Regular packs in the order they first appear, then the mixed pack if there are several. */
    fun packsOf(words: List<Word>): List<WordPack> {
        val regular = words.groupingBy { it.packId }.eachCount()
            .map { (id, count) -> WordPack(id, count) }
        return if (regular.size > 1) regular + WordPack(WordPack.MIXED_ID, words.size) else regular
    }

    fun wordsOf(words: List<Word>, packId: String): List<Word> =
        if (packId == WordPack.MIXED_ID) words else words.filter { it.packId == packId }
}

class WordsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WordsRepository {

    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<String, List<Word>>()

    /** Reads and parses the language file once, off the main thread, then serves it from memory. */
    private suspend fun loadAll(language: String): List<Word> {
        val fileName = WordPackParser.fileNameFor(language)
        return cacheMutex.withLock {
            cache[fileName] ?: withContext(ioDispatcher) {
                val document = context.assets.open(fileName).bufferedReader().use { it.readText() }
                WordPackParser.parse(document)
            }.also { cache[fileName] = it }
        }
    }

    override suspend fun getWords(language: String, packId: String): List<Word> =
        WordPackCatalog.wordsOf(loadAll(language), packId)

    override suspend fun getPacks(language: String): List<WordPack> =
        WordPackCatalog.packsOf(loadAll(language))

    override suspend fun getLanguages(): List<String> = AppLanguages.supported
}
