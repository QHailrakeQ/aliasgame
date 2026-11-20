package com.aliasgame.app.domain.model

/**
 * A word category that can be chosen for a game, with the number of words it contains.
 * [isTheme] marks topic packs (cinema, coffee shop, ...) as opposed to difficulty levels.
 */
data class WordPack(
    val id: String,
    val wordCount: Int,
    val isTheme: Boolean = false
) {
    companion object {
        /**
         * The virtual pack that combines every word of a language. It has no file of its own;
         * the words repository builds it from the regular packs.
         */
        const val MIXED_ID = "Mixed"
    }
}
