package com.novexel_studio.newapp


import android.media.SoundPool
import android.media.AudioAttributes
import android.util.Log


object SoundPlayer {


    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var isLoaded: Boolean = false

    fun init() {

        if (soundPool == null) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build()



            soundPool!!.setOnLoadCompleteListener { _, _, status ->
                if (status == 0) {
                    isLoaded = true
                } else {
                    Log.e("SoundPlayer", "Failed to load sound, status: $status")
                }
            }
        }
    }

    fun playVib(view: android.view.View) {
        if (isLoaded && soundPool != null) {
            soundPool!!.play(soundId, 1f, 1f, 0, 0, 1f)
        }


        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

}