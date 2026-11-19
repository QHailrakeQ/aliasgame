package com.aliasgame.app.domain.model

import kotlin.random.Random

/** Why a team name cannot be used to start a game. */
enum class TeamNameError { BLANK, DUPLICATE }

/**
 * Framework-free rules for team names: random picks from a pool of funny names and validation.
 * Names are compared trimmed and case-insensitively, so "Owls" and " owls " count as the same.
 */
object TeamNames {

    const val MIN_TEAMS = 2
    const val MAX_LENGTH = 24

    fun normalize(name: String): String = name.trim().lowercase()

    /**
     * A random name from [pool] that none of the [taken] names already uses. When the whole pool
     * is in use, a pool name gets a number ("Wise Owls 2") so the result is still unique.
     */
    fun randomUnused(pool: List<String>, taken: Collection<String>, random: Random = Random.Default): String {
        require(pool.isNotEmpty()) { "The name pool is empty" }
        val takenKeys = taken.map(::normalize).toSet()
        pool.filter { normalize(it) !in takenKeys }.randomOrNull(random)?.let { return it }

        val base = pool.random(random)
        return generateSequence(2) { it + 1 }
            .map { "$base $it" }
            .first { normalize(it) !in takenKeys }
    }

    /** [count] different random names from [pool]. */
    fun randomDistinct(pool: List<String>, count: Int, random: Random = Random.Default): List<String> {
        val names = mutableListOf<String>()
        repeat(count) { names += randomUnused(pool, names, random) }
        return names
    }

    /** The problem with each name, or `null` where the name is fine. Every copy of a duplicate is flagged. */
    fun errors(names: List<String>): List<TeamNameError?> {
        val counts = names.filter { it.isNotBlank() }.groupingBy(::normalize).eachCount()
        return names.map { name ->
            when {
                name.isBlank() -> TeamNameError.BLANK
                (counts[normalize(name)] ?: 0) > 1 -> TeamNameError.DUPLICATE
                else -> null
            }
        }
    }

    fun isPoolName(name: String, pool: List<String>): Boolean =
        pool.any { normalize(it) == normalize(name) }
}
