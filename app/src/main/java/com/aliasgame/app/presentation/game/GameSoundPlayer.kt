package com.aliasgame.app.presentation.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aliasgame.app.R

enum class GameSound { CORRECT, SKIP, TIMER_END }

/**
 * Plays the game's sound effects and owns the underlying audio resources.
 *
 * The short answer sounds use [SoundPool]: they are decoded once and can restart instantly,
 * so rapid answers each get their sound (a [MediaPlayer] ignores `start()` while still
 * playing). The longer timer-end sound plays once per turn and stays on [MediaPlayer],
 * which streams it instead of decoding it into memory.
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
    private val correctSoundId = soundPool.load(context, R.raw.correct_sound, 1)
    private val skipSoundId = soundPool.load(context, R.raw.skip_sound, 1)

    // create() returns null when the resource cannot be prepared; the sound is then skipped.
    private val timerEndPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.last_word_sound)

    fun play(sound: GameSound) {
        when (sound) {
            GameSound.CORRECT -> playFromPool(correctSoundId)
            GameSound.SKIP -> playFromPool(skipSoundId)
            GameSound.TIMER_END -> timerEndPlayer?.run {
                seekTo(0)
                start()
            }
        }
    }

    fun release() {
        soundPool.release()
        timerEndPlayer?.release()
    }

    private fun playFromPool(soundId: Int) {
        if (soundId != 0) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private companion object {
        const val MAX_SIMULTANEOUS_SOUNDS = 3
    }
}

/** A [GameSoundPlayer] tied to the composition: created once and released when it leaves. */
@Composable
fun rememberGameSoundPlayer(): GameSoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { GameSoundPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
