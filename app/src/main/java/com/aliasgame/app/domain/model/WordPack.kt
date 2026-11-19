package com.aliasgame.app.domain.model

/**
 * A word category that can be chosen for a game, with the number of words it contains.
 */
data class WordPack(
    val id: String,
    val wordCount: Int
) {
    companion object {
        /**
         * The virtual pack that combines every word of a language. It has no file of its own;
         * the words repository builds it from the regular packs.
         */
        const val MIXED_ID = "Mixed"
    }
}
