package com.aliasgame.app.domain.model

/**
 * The visual style of the app, chosen in the settings.
 *
 * [id] is what gets stored, so renaming an entry does not lose the player's choice.
 */
enum class AppStyle(val id: String) {
    /** The original flat purple look. */
    CLASSIC("classic"),

    /** Neon on a dark background: pink actions, cyan outlines. */
    NEON_PINK("neon_pink"),

    /** Neon on a dark background: violet actions, lime outlines. */
    NEON_VIOLET("neon_violet");

    companion object {
        val DEFAULT = NEON_PINK

        /** The style stored as [id], or [DEFAULT] for nothing stored or an unknown value. */
        fun fromId(id: String?): AppStyle = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
