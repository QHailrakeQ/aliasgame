package com.aliasgame.app.domain.model

/**
 * How often a word has been played in a language: shown to a team and skipped by it.
 * A word counts as skipped when its final result after the round review was "not guessed".
 */
data class WordStat(
    val language: String,
    val text: String,
    val shown: Int,
    val skipped: Int
)

/** Framework-free rules for collecting and ranking [WordStat]s. */
object WordStatistics {

    const val DEFAULT_TOP_SIZE = 20

    /**
     * Adds one finished turn to [stats]: every result counts as one showing of its word, and
     * the ones that were not guessed count as skips. Words are matched by text within [language].
     */
    fun record(stats: List<WordStat>, language: String, results: List<RoundResult>): List<WordStat> {
        if (results.isEmpty()) return stats
        val byKey = stats.associateByTo(LinkedHashMap()) { it.language to it.text }
        for (result in results) {
            val key = language to result.word.text
            val current = byKey[key] ?: WordStat(language, result.word.text, shown = 0, skipped = 0)
            byKey[key] = current.copy(
                shown = current.shown + 1,
                skipped = current.skipped + if (result.isCorrect) 0 else 1
            )
        }
        return byKey.values.toList()
    }

    /**
     * The words of [language] that were skipped most often. Among words skipped equally often,
     * the one shown fewer times ranks higher (it was skipped on a larger share of its showings);
     * remaining ties are ordered alphabetically so the list is stable. Never-skipped words are left out.
     */
    fun hardest(stats: List<WordStat>, language: String, limit: Int = DEFAULT_TOP_SIZE): List<WordStat> =
        stats.asSequence()
            .filter { it.language == language && it.skipped > 0 }
            .sortedWith(
                compareByDescending<WordStat> { it.skipped }
                    .thenBy { it.shown }
                    .thenBy { it.text }
            )
            .take(limit)
            .toList()

    /** Languages that have at least one skipped word, in the app's language order. */
    fun languagesWithSkips(stats: List<WordStat>): List<String> {
        val languages = stats.filter { it.skipped > 0 }.map { it.language }.toSet()
        return AppLanguages.supported.filter { it in languages } +
            languages.filterNot { it in AppLanguages.supported }.sorted()
    }
}
