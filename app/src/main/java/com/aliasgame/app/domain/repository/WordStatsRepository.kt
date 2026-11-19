package com.aliasgame.app.domain.repository

import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.WordStat
import kotlinx.coroutines.flow.Flow

/** Persistent per-word statistics collected across games. */
interface WordStatsRepository {
    val stats: Flow<List<WordStat>>

    /** Adds the reviewed results of one finished turn played in [language]. */
    suspend fun recordTurn(language: String, results: List<RoundResult>)

    suspend fun clear()
}
