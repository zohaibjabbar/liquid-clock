package com.example.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.example.R

object SoundHapticHelper {

    private var preloadedPlayer3124: MediaPlayer? = null
    private var preloadedPlayer269: MediaPlayer? = null

    /**
     * Eagerly preloads the MediaPlayer instances for clicks and toggles to minimize latency.
     */
    fun preload(context: Context) {
        synchronized(this) {
            val appContext = context.applicationContext
            if (preloadedPlayer3124 == null) {
                preloadedPlayer3124 = createAndPreparePlayer(appContext, R.raw.sound_3124_preview)
            }
            if (preloadedPlayer269 == null) {
                preloadedPlayer269 = createAndPreparePlayer(appContext, R.raw.sound_269_preview)
            }
            Log.d("HAPTIC_TEST", "Sound preloading completed")
        }
    }

    private fun createAndPreparePlayer(context: Context, resId: Int): MediaPlayer? {
        return try {
            // MediaPlayer.create automatically prepares the player and loads the source
            MediaPlayer.create(context, resId)
        } catch (e: Exception) {
            Log.e("SoundHapticHelper", "Failed to preload resource $resId", e)
            null
        }
    }

    fun playSound3124(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            synchronized(this) {
                try {
                    val player = preloadedPlayer3124 ?: run {
                        val newPlayer = createAndPreparePlayer(context.applicationContext, R.raw.sound_3124_preview)
                        preloadedPlayer3124 = newPlayer
                        newPlayer
                    }

                    player?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                        mp.seekTo(0)
                        mp.start()
                        Log.d("HAPTIC_TEST", "sound_3124 preloaded instance played")
                    } ?: run {
                        // Dynamic fallback if preloading and instantiation failed completely
                        val mp = MediaPlayer.create(context.applicationContext, R.raw.sound_3124_preview)
                        mp?.setOnCompletionListener { it.release() }
                        mp?.start()
                        Log.d("HAPTIC_TEST", "sound_3124 fallback played")
                    }
                } catch (e: Exception) {
                    Log.e("SoundHapticHelper", "Error playing preloaded sound_3124, recreating", e)
                    try { preloadedPlayer3124?.release() } catch (ex: Exception) {}
                    preloadedPlayer3124 = null
                    // Quick dynamic playback fallback
                    try {
                        val mp = MediaPlayer.create(context.applicationContext, R.raw.sound_3124_preview)
                        mp?.setOnCompletionListener { it.release() }
                        mp?.start()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } else {
            Log.d("HAPTIC_TEST", "sound_3124 skipped due to silent mode")
        }
    }

    fun playSound269(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            synchronized(this) {
                try {
                    val player = preloadedPlayer269 ?: run {
                        val newPlayer = createAndPreparePlayer(context.applicationContext, R.raw.sound_269_preview)
                        preloadedPlayer269 = newPlayer
                        newPlayer
                    }

                    player?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                        mp.seekTo(0)
                        mp.start()
                        Log.d("HAPTIC_TEST", "sound_269 preloaded instance played")
                    } ?: run {
                        // Dynamic fallback if preloading and instantiation failed completely
                        val mp = MediaPlayer.create(context.applicationContext, R.raw.sound_269_preview)
                        mp?.setOnCompletionListener { it.release() }
                        mp?.start()
                        Log.d("HAPTIC_TEST", "sound_269 fallback played")
                    }
                } catch (e: Exception) {
                    Log.e("SoundHapticHelper", "Error playing preloaded sound_269, recreating", e)
                    try { preloadedPlayer269?.release() } catch (ex: Exception) {}
                    preloadedPlayer269 = null
                    // Quick dynamic playback fallback
                    try {
                        val mp = MediaPlayer.create(context.applicationContext, R.raw.sound_269_preview)
                        mp?.setOnCompletionListener { it.release() }
                        mp?.start()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } else {
            Log.d("HAPTIC_TEST", "sound_269 skipped due to silent mode")
        }
    }
}
