package org.darenom.leadme.service.sensors

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Created by adm on 26/10/2017.
 */

internal class TravelCompassManager(context: Context) : SensorEventListener {

    private val mSensorManager: SensorManager?
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    // return computed data
    var myOrientation: FloatArray? = null
        get() {
            val orientation = FloatArray(3)
            val rotationMatrixI = FloatArray(9)
            val rotationMatrixR = FloatArray(9)

            if (null != mSensorManager && SensorManager.getRotationMatrix(
                            rotationMatrixR, rotationMatrixI, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrixR, orientation)
            }
            return orientation
        }

    init {
        this.mSensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    }

    // register/unregister to sensors
    fun compute(compute: Boolean) {
        if (compute) {
            mSensorManager!!.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME)
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_GAME)
        } else {
            mSensorManager!!.unregisterListener(this)
        }
    }

    // update raw data
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.e("TravelCompassManager", "onAccuracyChanged => " +
                sensor.name + " : " + accuracy.toString())
    }

}
