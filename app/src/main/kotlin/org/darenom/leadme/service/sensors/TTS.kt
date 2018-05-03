package org.darenom.leadme.service.sensors

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import org.darenom.leadme.BuildConfig
import java.util.*





/**
 * Created by admadmin on 06/03/2018.
 */

class TTS(var context: Context) :
        TextToSpeech.OnInitListener {

    private val cTAG = "TTS"
    private var mTts: TextToSpeech? = null
    private var v: Int = 0
    private val ttsSaid = object : UtteranceProgressListener() {
        @Suppress("OverridingDeprecatedMember")
        override fun onError(utteranceId: String?) {
            Log.e(cTAG, "error")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            super.onError(utteranceId, errorCode)
            Log.e(cTAG, "error : $errorCode")
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            super.onStop(utteranceId, interrupted)
            Log.e(cTAG, "stop : $interrupted")
        }

        override fun onStart(utteranceId: String) {
            Log.e(cTAG, "start")
            // reduce
            v = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            mAudioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND)
        }

        override fun onDone(utteranceId: String) {
            Log.e(cTAG, "done")
            // reset
            mAudioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_PLAY_SOUND)
        }
    }

    private var mAudioManager: AudioManager? = null

    init {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        on()
    }

    fun on() {
        if (null == mTts)
            mTts = TextToSpeech(context, this)
    }

    fun off() {
        if (mTts != null) {
            mTts!!.stop()
        }
    }

    fun shutdown() {
        if (mTts != null) {
            mTts!!.shutdown()
        }
    }

    override fun onInit(status: Int) {
        mTts!!.language = Locale.getDefault()
        mTts!!.setOnUtteranceProgressListener(ttsSaid)
        if (BuildConfig.DEBUG)
            Log.e(cTAG, "TTS ready")

    }

    @Suppress("DEPRECATION")
    fun say(toSay: String, uId: String): Boolean {

        return when {
            null == mTts ->
                false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                val param = Bundle()
                param.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
                mTts!!.speak(toSay, TextToSpeech.QUEUE_FLUSH, param, uId) == TextToSpeech.SUCCESS
            }

            else -> {
                val param = HashMap<String, String>()
                param.put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION.toString())
                mTts!!.speak(toSay, TextToSpeech.QUEUE_FLUSH, param) == TextToSpeech.SUCCESS
            }

        }

    }


}