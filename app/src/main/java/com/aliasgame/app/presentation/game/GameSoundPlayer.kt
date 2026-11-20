package com.aliasgame.app.presentation.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aliasgame.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

/** Every sound effect of the game screen. */
enum class GameSound {
    /** A word was guessed. */
    CORRECT,

    /** A word was skipped. */
    SKIP,

    /** The countdown of a turn started. */
    TURN_START,

    /** One of the last seconds of a turn ticked away. */
    COUNTDOWN_TICK,

    /** One of the very last seconds ticked away: the same tick, played higher. */
    COUNTDOWN_TICK_FINAL,

    /** The time of a turn ran out. */
    TIMER_END,

    /** A team won the game. */
    VICTORY
}

/**
 * Plays the game's sound effects and owns the underlying audio resources.
 *
 * All sounds are short (the longest, [GameSound.VICTORY], lasts about two seconds), so they
 * all use [SoundPool]: each one is decoded once and starts instantly, even when the same sound
 * is requested again before it finished, as with quick answers or countdown ticks.
 *
 * Call [release] when the player is no longer needed; see [rememberGameSoundPlayer].
 */
class GameSoundPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_SIMULTANEOUS_SOUNDS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // SoundPool decodes asynchronously; play() before loading finishes is silently skipped.
    // load() returns 0 when the file cannot be opened; such a sound is skipped as well.
    private val soundIds: Map<GameSound, Int> = run {
        val correct = soundPool.load(context, R.raw.correct_sound, 1)
        val skip = soundPool.load(context, R.raw.skip_sound, 1)
        val turnStart = soundPool.load(context, R.raw.turn_start_sound, 1)
        val tick = soundPool.load(context, R.raw.countdown_tick_sound, 1)
        val timeUp = soundPool.load(context, R.raw.time_up_sound, 1)
        val victory = soundPool.load(context, R.raw.victory_sound, 1)
        mapOf(
            GameSound.CORRECT to correct,
            GameSound.SKIP to skip,
            GameSound.TURN_START to turnStart,
            GameSound.COUNTDOWN_TICK to tick,
            GameSound.COUNTDOWN_TICK_FINAL to tick,
            GameSound.TIMER_END to timeUp,
            GameSound.VICTORY to victory
        )
    }

    fun play(sound: GameSound) {
        val soundId = soundIds[sound] ?: return
        if (soundId == 0) return
        // A playback rate above 1 raises the pitch, so one tick file serves both tick sounds.
        val rate = if (sound == GameSound.COUNTDOWN_TICK_FINAL) FINAL_TICK_RATE else NORMAL_RATE
        soundPool.play(soundId, 1f, 1f, 1, 0, rate)
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        // An answer, a countdown tick and the end of the turn can overlap, plus one spare.
        const val MAX_SIMULTANEOUS_SOUNDS = 4
        const val NORMAL_RATE = 1f
        const val FINAL_TICK_RATE = 1.5f
    }
}

/**
 * A [GameSoundPlayer] tied to the composition, or `null` while it is still being created.
 *
 * Creating the player opens and registers the audio files synchronously, which takes from
 * hundreds of milliseconds to seconds on slower devices. Doing that on the
 * main thread froze the game screen right after it opened (buttons ignored every touch), so
 * the player is created on a background thread; sounds requested before it is ready are skipped.
 * It is released when it leaves the composition.
 */
@Composable
fun rememberGameSoundPlayer(): GameSoundPlayer? {
    val context = LocalContext.current.applicationContext
    var player by remember(context) { mutableStateOf<GameSoundPlayer?>(null) }
    LaunchedEffect(context) {
        // NonCancellable: if the screen leaves while the player is being created, the finished
        // player is still returned here and released below instead of leaking its resources.
        val created = withContext(Dispatchers.IO + NonCancellable) { GameSoundPlayer(context) }
        try {
            player = created
            awaitCancellation()
        } finally {
            player = null
            created.release()
        }
    }
    return player
}
