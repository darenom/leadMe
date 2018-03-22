package org.darenom.leadme.service.sensors

import android.content.Context
import android.os.Build
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
    private val ttsSaid = object : UtteranceProgressListener() {
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
        }

        override fun onDone(utteranceId: String) {
            Log.e(cTAG, "done")
        }
    }

    init{
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

    fun say(toSay: String, uId: String): Boolean {
        return when {
            null == mTts ->
                false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                mTts!!.speak(toSay, TextToSpeech.QUEUE_FLUSH, null, uId) == TextToSpeech.SUCCESS
            else ->
                mTts!!.speak(toSay, TextToSpeech.QUEUE_FLUSH, null) == TextToSpeech.SUCCESS
        }

    }


}