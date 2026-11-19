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

    /**
     * Parses a theme file: a plain JSON list of words, e.g. `["Espresso", "Latte"]`. Every word
     * gets [themeId] (the file name without `.json`) as its pack.
     */
    fun parseTheme(document: String, themeId: String): List<Word> =
        json.decodeFromString<List<String>>(document).map { Word(text = it, packId = themeId) }

    /** Asset folder with the theme files of a language, e.g. `themes/uk`. */
    fun themeDirFor(language: String): String = "themes/${AppLanguages.toLanguageTag(language)}"

    /** `cinema.json` -> `cinema`; `null` for files that are not theme files. */
    fun themeIdOf(fileName: String): String? = fileName.removeSuffix(".json").takeIf { fileName.endsWith(".json") }
}

/**
 * Builds the playable packs of a language from its difficulty words and its theme words.
 * Free of Android dependencies so it can be unit-tested on the JVM.
 */
internal object WordPackCatalog {

    /**
     * Difficulty packs in the order they first appear, then the mixed pack if there are several,
     * then the themes in the order given. The mixed pack combines the difficulty packs only:
     * themes are niche topics that would feel out of place in a general game.
     */
    fun packsOf(words: List<Word>, themes: Map<String, List<Word>> = emptyMap()): List<WordPack> {
        val regular = words.groupingBy { it.packId }.eachCount()
            .map { (id, count) -> WordPack(id, count) }
        val mixed = if (regular.size > 1) listOf(WordPack(WordPack.MIXED_ID, words.size)) else emptyList()
        val themePacks = themes.map { (id, themeWords) -> WordPack(id, themeWords.size, isTheme = true) }
        return regular + mixed + themePacks
    }

    fun wordsOf(words: List<Word>, packId: String, themes: Map<String, List<Word>> = emptyMap()): List<Word> =
        when (packId) {
            WordPack.MIXED_ID -> words
            in themes -> themes.getValue(packId)
            else -> words.filter { it.packId == packId }
        }
}

/** All words of one language: the difficulty packs plus one list per theme. */
private class LanguageWords(val words: List<Word>, val themes: Map<String, List<Word>>)

class WordsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WordsRepository {

    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<String, LanguageWords>()

    /**
     * Reads and parses the language file and its theme files once, off the main thread, then
     * serves them from memory. Themes are discovered from the language's theme folder, so a new
     * theme only needs a new file there (and, optionally, a localized name in the resources).
     */
    private suspend fun loadAll(language: String): LanguageWords {
        val fileName = WordPackParser.fileNameFor(language)
        return cacheMutex.withLock {
            cache[fileName] ?: withContext(ioDispatcher) {
                val words = WordPackParser.parse(readAsset(fileName))
                val themeDir = WordPackParser.themeDirFor(language)
                val themes = context.assets.list(themeDir).orEmpty().sorted()
                    .mapNotNull { file ->
                        WordPackParser.themeIdOf(file)?.let { id ->
                            id to WordPackParser.parseTheme(readAsset("$themeDir/$file"), id)
                        }
                    }
                    .toMap(LinkedHashMap())
                LanguageWords(words, themes)
            }.also { cache[fileName] = it }
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    override suspend fun getWords(language: String, packId: String): List<Word> =
        loadAll(language).let { WordPackCatalog.wordsOf(it.words, packId, it.themes) }

    override suspend fun getPacks(language: String): List<WordPack> =
        loadAll(language).let { WordPackCatalog.packsOf(it.words, it.themes) }

    override suspend fun getLanguages(): List<String> = AppLanguages.supported
}
