package org.darenom.leadme

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity

/**
 * Created by adm on 13/02/2018.
 * Gives time for the service to start and get a location
 */

class Splash : AppCompatActivity() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (null == (application as BaseApp).splash) {
            (application as BaseApp).splash = this
            val checkIntent = Intent()
            checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            startActivityForResult(checkIntent, TravelActivity.CHECK_TTS_ACCESS)
        } else {
            (application as BaseApp).moveOn()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            TravelActivity.CHECK_TTS_ACCESS -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    (application as BaseApp).travelService!!.startTTS()
                } else {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                }
            }
        }
    }
}