package com.aliasgame.app.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.WordPack

/** Localized name of a word pack; unknown packs keep their id. */
@Composable
internal fun packDisplayName(packId: String): String = when (packId.lowercase()) {
    "easy" -> stringResource(R.string.difficulty_easy)
    "medium" -> stringResource(R.string.difficulty_medium)
    "hard" -> stringResource(R.string.difficulty_hard)
    "insane" -> stringResource(R.string.difficulty_insane)
    WordPack.MIXED_ID.lowercase() -> stringResource(R.string.difficulty_mixed)
    "cinema" -> stringResource(R.string.theme_cinema)
    "coffee_shop" -> stringResource(R.string.theme_coffee_shop)
    "welding_ndt" -> stringResource(R.string.theme_welding_ndt)
    // A theme file added without a localized name: "board_games" -> "Board games".
    else -> packId.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

/** Localized one-line description of a word pack. */
@Composable
internal fun packDescription(packId: String): String = when (packId.lowercase()) {
    "easy" -> stringResource(R.string.desc_easy)
    "medium" -> stringResource(R.string.desc_medium)
    "hard" -> stringResource(R.string.desc_hard)
    "insane" -> stringResource(R.string.desc_insane)
    WordPack.MIXED_ID.lowercase() -> stringResource(R.string.desc_mixed)
    "cinema" -> stringResource(R.string.desc_theme_cinema)
    "coffee_shop" -> stringResource(R.string.desc_theme_coffee_shop)
    "welding_ndt" -> stringResource(R.string.desc_theme_welding_ndt)
    else -> stringResource(R.string.pack_description_fallback, packDisplayName(packId))
}

/** "413 words" with the correct plural form of the current language. */
@Composable
internal fun packWordCount(wordCount: Int): String =
    pluralStringResource(R.plurals.pack_word_count, wordCount, wordCount)
