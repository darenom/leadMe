package org.darenom.leadme

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.os.Handler
import android.support.annotation.Nullable
import org.darenom.leadme.ui.MainActivity


/**
 * Created by adm on 13/02/2018.
 * Gives time for the service to start and get a location
 */

class Splash : Activity() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler().postDelayed(Runnable {
            startActivity(Intent(this@Splash, MainActivity::class.java))
            finish()
        }, 3000)
    }
}