package com.aliasgame.app.domain.model

/**
 * Languages supported by the app.
 *
 * Settings store the short code shown to the user ("UA" for Ukrainian), while Android
 * locales and word-pack files use ISO 639-1 language tags ("uk"). This is the single
 * place where the two are mapped.
 */
object AppLanguages {
    const val DEFAULT = "EN"

    val supported: List<String> = listOf("EN", "UA", "RU", "DE")

    /** ISO 639-1 language tag for a stored language code, e.g. "UA" -> "uk", "DE" -> "de". */
    fun toLanguageTag(code: String): String =
        if (code.equals("UA", ignoreCase = true)) "uk" else code.lowercase()
}
