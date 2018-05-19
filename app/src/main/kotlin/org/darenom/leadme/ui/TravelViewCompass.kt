package org.darenom.leadme.ui

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.layout_view_compass.view.*
import org.darenom.leadme.service.TravelService.Companion.COMPASS_UPDATE_INTERVAL

/**
 * Created by adm on 13/02/2018.
 * View that holds a compass pointing to north
 * and an arrow pointing to a specific location
 */

class TravelViewCompass @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var currentCompassDegree = 0f
    private var currentDirectionDegree = 0f

    // set compass angle and levels
    private fun setCompassOrientation(vararg orientation: Float) {
        // plan level
        val h = Math.round(Math.toDegrees(orientation[2].toDouble())).toFloat()
        if (h < 90 && h > -90) {
            hori.progress = Math.round(h + 50)
        }
        // plan level
        val v = Math.round(Math.toDegrees(orientation[1].toDouble())).toFloat()
        if (v < 90 && v > -90) {
            vert.progress = Math.round(v + 50)
        }

        // image angle to rotate to
        val degree = Math.round(Math.toDegrees(orientation[0].toDouble())).toFloat()
        val ra = RotateAnimation(
                currentCompassDegree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        ra.duration = COMPASS_UPDATE_INTERVAL / 2
        ra.fillAfter = true
        imageViewCompass.startAnimation(ra)
        currentCompassDegree = -degree
    }

    // set direction angle computed by service (arrow)
    private fun setDirectionOrientation(degree: Float) {
        val ra = RotateAnimation(
                currentDirectionDegree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
        ra.duration = 210
        ra.fillAfter = true
        imageViewDirection.startAnimation(ra)
        currentDirectionDegree = -degree
    }

    fun onOrientationChanged(orientation: FloatArray) {
        setCompassOrientation(*orientation)
    }

    fun onDirectionChanged(degree: Float) {
        setDirectionOrientation(degree)
    }
}