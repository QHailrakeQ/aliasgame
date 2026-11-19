package com.aliasgame.app.domain.model

/**
 * The phase of a game session. Exactly one phase is active at a time, which makes
 * contradictory states (e.g. "paused on the ready screen") unrepresentable.
 *
 * Transitions (owned by [com.aliasgame.app.domain.engine.GameEngine]):
 * ```
 * READY_TO_START --startTurn--> RUNNING --pause--> PAUSED --resume--> RUNNING
 * RUNNING --time runs out--> LAST_WORD --decideLastWord--> ROUND_REVIEW | FINISHED
 * ROUND_REVIEW --nextRound--> READY_TO_START
 * ```
 */
enum class GamePhase {
    /** The current team is about to start its turn; the countdown has not started. */
    READY_TO_START,

    /** The countdown is running and words can be answered. */
    RUNNING,

    /** The countdown was interrupted and can be resumed. */
    PAUSED,

    /** Time is up; the teams decide who earned the last word. */
    LAST_WORD,

    /** The turn is over; its word results can be reviewed and corrected. */
    ROUND_REVIEW,

    /** A full round cycle ended with at least one team at the target score. */
    FINISHED
}
