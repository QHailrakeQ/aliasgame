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

    /**
     * The language to use before the player has chosen one: the first of the device's
     * preferred languages that the app supports, or [DEFAULT]. Android resolves the interface
     * language from the same list, so words and team names match the interface.
     *
     * @param languageTags BCP 47 tags in order of preference, e.g. `["uk-UA", "en-US"]`.
     */
    fun fromDeviceLanguages(languageTags: List<String>): String =
        languageTags.firstNotNullOfOrNull { tag ->
            val language = tag.substringBefore('-').substringBefore('_').lowercase()
            supported.firstOrNull { toLanguageTag(it) == language }
        } ?: DEFAULT
}
