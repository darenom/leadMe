package org.darenom.leadme.service

import android.app.Service
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationProvider
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import org.darenom.leadme.BaseApp
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.R
import org.darenom.leadme.db.entities.TravelStampEntity
import org.darenom.leadme.model.Travel
import org.darenom.leadme.model.TravelSegment
import org.darenom.leadme.service.sensors.TTS
import org.darenom.leadme.service.sensors.TravelCompassManager
import org.darenom.leadme.service.sensors.TravelLocationManager

/**
 * Created by adm on 25/10/2017.
 */

class TravelService : Service(),
        LocationListener {

    inner class TravelServiceBinder : Binder() {
        val service: TravelService
            get() = this@TravelService
    }

    private val binder = TravelServiceBinder()

    var hasCompass: Boolean = false
    var hasPos: Boolean = false
        get() = travelLocationManager?.canLocate()!! && travelLocationManager?.mayLocate()!!

    private val cTAG = "TravelService"
    private var travelLocationManager: TravelLocationManager? = null
    private var tts: TTS? = null



    // region Service lifecycle
    override fun onCreate() {
        super.onCreate()
        // check sensors availability
        var acc = false
        var mag = false
        val deviceSensors = (getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                .getSensorList(Sensor.TYPE_ALL)
        for (sensor in deviceSensors) {
            if (sensor.type == Sensor.TYPE_ACCELEROMETER)
                acc = true
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
                mag = true
        }
        if (acc && mag) {
            hasCompass = true
            travelCompass = TravelCompassManager(this)
        }
        travelLocationManager = TravelLocationManager(this, this)
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (BuildConfig.DEBUG)
            Log.d(cTAG, "TravelService Started : $startId")

        locate(true)

        return START_STICKY
    }

    override fun onDestroy() {

        tts?.shutdown()

        if (hasCompass) {
            if (oCompass)
                travelCompass?.compute(false)
            if (dCompass)
                travelCompass?.compute(false)
        }

        locate(false)

        if (BuildConfig.DEBUG)
            Log.d(cTAG, "TravelService Destroyed")
        super.onDestroy()
    }
    //endregion

    // region Location
    private fun locate(b: Boolean) {
        if (b)
            travelLocationManager?.locate(true, false)
        else
            travelLocationManager?.locate(false, false)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
        Log.e(cTAG, "onStatusChanged : $s, ${when (i) {
            LocationProvider.OUT_OF_SERVICE -> "out of service"
            LocationProvider.AVAILABLE -> "service available"
            LocationProvider.TEMPORARILY_UNAVAILABLE -> "temporarily unavailable"
            else -> i.toString()
        }}")
    }

    override fun onProviderEnabled(s: String) {
        Log.e(cTAG, "onProviderEnabled : $s")

    }

    override fun onProviderDisabled(s: String) {
        Log.e(cTAG, "onProviderDisabled : $s")

    }

    override fun onLocationChanged(location: Location?) {
        if (null != location) {
            // update position
            here = location
            // if travelling
            if (travelling && travel.value!!.points!!.isNotEmpty()) {
                // stamp bdd
                (application as BaseApp).mAppExecutors!!.diskIO().execute {
                    (application as BaseApp).database.travelStampDao().insert(TravelStampEntity(
                            travel.value!!.name, iter, location.time, location.latitude, location.longitude,
                            location.accuracy, location.bearing, location.provider, location.altitude,
                            location.toString()))
                }
                // provide info
                computeDirections(location)
            }
        }
    }
    // endregion

    // region Compass

    private var oCompass: Boolean = false // orientation request status
    private var dCompass: Boolean = false // direction request status
    private var travelCompass: TravelCompassManager? = null
    private val orientationHandler = Handler()
    private val computeOrientation = object : Runnable {
        override fun run() {
            val intent = Intent(ORIENTATION_CHANGED)
            intent.putExtra(ORIENTATION_CHANGED, travelCompass?.myOrientation)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            orientationHandler.postDelayed(this, COMPASS_UPDATE_INTERVAL)
        }
    }

    // handles compass sensors requests from direction and orientation
    // orientation is timed provided
    // direction updates onLocationChange
    fun enableCompass(enable: Boolean, feature: Int) {
        if (hasCompass) {
            when (feature) {
            // orientation
                0 -> {
                    if (enable) {
                        if (!oCompass && !dCompass)
                            travelCompass?.compute(true)

                        oCompass = true
                        computeOrientation.run()

                    } else {
                        orientationHandler.removeCallbacks(computeOrientation)
                        oCompass = false

                        if (!dCompass)
                            travelCompass?.compute(false)
                    }
                }
            // direction
                1 -> {
                    if (enable) {
                        if (!oCompass && !dCompass)
                            travelCompass?.compute(true)

                        dCompass = true
                    } else {
                        dCompass = false

                        if (!oCompass)
                            travelCompass?.compute(false)
                    }
                }
            }
        }
    }

    // get lead to a point (°NE)
    private fun computeDirection(location: Location, index: Int): Float {
        val dest = Location("dest")
        dest.latitude = travel.value!!.points!![index].latitude
        dest.longitude = travel.value!!.points!![index].longitude
        dest.bearing = 0f
        val bearing = Math.round(location.bearingTo(dest)).toFloat()    // -180 to 180
        val heading = Math.round(Math.toDegrees(travelCompass?.myOrientation!![0].toDouble())).toFloat() // get north
        return heading - bearing
    }

    // endregion

    // region Travel

    private var iter: Int? = null
    private var isFirstRound: Boolean = false
    private var oldStatus: Int = -1
    private var tmpTs: TravelSegment? = null

    fun startMotion(iter: Int): Boolean {
        this.iter = iter
        if (travelLocationManager!!.locate(true, true)) {
            isFirstRound = true
            travelling = true
            oldStatus = -1
            tts?.on()
            return true
        }
        return false
    }

    fun stopMotion() {
        travelling = false
        isFirstRound = false
        oldStatus = -1
        tmpTs = null
        travelLocationManager!!.locate(true, false)
        tts?.off()
        enableCompass(false, 1)
    }

    private fun computeDirections(location: Location) {
        var txt = ""
        var uId = ""
        var hasToSay = false

        val status = ts!!.computeSegment(location)

        // draw support on map if hasChanged
        if (ts!!.closest > 0 && ts!!.closest < travel.value!!.points!!.size - 1) {
            if (null == tmpTs) {
                // first
                val intent = Intent(SEGMENT_CHANGED)
                intent.putExtra(SEGMENT_LENGTH, ts!!.length)
                intent.putParcelableArrayListExtra(SEGMENT_SIDES, ts!!.latlngs)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            } else {
                // has changed
                if (!tmpTs!!.index.contentEquals(ts!!.index)) {
                    val intent = Intent(SEGMENT_CHANGED)
                    intent.putExtra(SEGMENT_LENGTH, ts!!.length)
                    intent.putParcelableArrayListExtra(SEGMENT_SIDES, ts!!.latlngs)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            }
        }

        when (status) {
            TravelSegment.ON_THE_WAY -> {

                txt = travel.value!!.infos!![ts!!.index[0]]
                uId = ts!!.index[0].toString()

                hasToSay = if (isFirstRound || oldStatus == TravelSegment.OUT_OF_BOUNDS) {
                    messageWayBack()
                    true
                } else {
                    false
                }

            }
            TravelSegment.ARRIVED -> {

                if (oldStatus == TravelSegment.OUT_OF_BOUNDS) {
                    messageWayBack()
                }
                txt = getString(R.string.destination)
                uId = (-2).toString()
                hasToSay = true
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(ARRIVED))
            }
            TravelSegment.REACHED -> {
                if (oldStatus == TravelSegment.OUT_OF_BOUNDS) {
                    messageWayBack()
                }
                txt = travel.value!!.infos!![ts!!.closest]
                uId = ts!!.closest.toString()
                hasToSay = isFirstRound || tmpTs!!.closest != ts!!.closest

            }
            TravelSegment.OUT_OF_BOUNDS -> {
                txt = getString(R.string.out_of_bounds)
                uId = (-1).toString()
                hasToSay = isFirstRound || oldStatus != TravelSegment.OUT_OF_BOUNDS

                if (hasCompass) {
                    enableCompass(true, 1)
                    val intent = Intent(DIRECTION_CHANGED)
                    intent.putExtra(DIRECTION_CHANGED, computeDirection(location,
                            if (ts!!.index[1] == -1)
                                ts!!.index[0]
                            else
                                ts!!.index[1])
                    )
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            }
        }

        oldStatus = status
        tmpTs = ts

        if (BuildConfig.DEBUG) {
            Log.d(cTAG, String.format("Status is %d, was: %d --- say: %s, first loop: %s",
                    status, oldStatus, hasToSay.toString(), isFirstRound.toString()))
        }

        // Say something if needed
        if (null != tts) {
            if ((hasToSay || isFirstRound)) {
                if (isFirstRound)
                    isFirstRound = false

                if (!tts!!.say(txt, uId)) {
                    tts!!.on()
                    tts!!.say(txt, uId)
                }
            }
        }

    }

    private fun messageWayBack() {
        enableCompass(false, 1)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(MY_WAY_BACK))
    }

    // endregion

    fun startTTS() {
        tts = TTS(this)
    }

    companion object {

        var here: Location? = null
        var travelling: Boolean = false
        val travel = object : MutableLiveData<Travel>() {}
        var ts: TravelSegment? = null

        const val COMPASS_UPDATE_INTERVAL: Long = 3000 // ms
        const val ORIENTATION_CHANGED = "OrientationChanged"
        const val DIRECTION_CHANGED = "DirectionChanged"
        const val SEGMENT_CHANGED = "SegmentChanged"
        const val SEGMENT_SIDES = "SegmentSides"
        const val SEGMENT_LENGTH = "SegmentLength"
        const val ARRIVED = "Arrived"
        const val MY_WAY_BACK = "WayBack"
    }
}