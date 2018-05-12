package org.darenom.leadme

import android.os.Bundle
import android.os.Handler
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_splash.*

/**
 * Created by adm on 13/02/2018.
 * Gives time for the service to start and get a location
 */

class Splash : AppCompatActivity() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if (null == (application as BaseApp).splash)
            (application as BaseApp).splash = this
        else {
            (application as BaseApp).moveOn()
            finish()
        }

    }
}