package org.darenom.leadme.service.sensors

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.ContextCompat

/**
 * Created by adm on 26/10/2017.
 *
 * get locations from providers
 * computes best
 * forwards to service
 *
 */

class TravelLocationManager
internal constructor(private val context: Context, private val listener: LocationListener) : LocationListener {

    private var mLocationManager: LocationManager? = null
    private var gpsLocation: Location? = null
    private var netLocation: Location? = null

    init {
        mLocationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    // Features availability
    internal fun canLocate(): Boolean =
            mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    internal fun mayLocate(): Boolean {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // fastest way to provide LastKnownLocation
    private fun initLocation(): Boolean {
        return if (canLocate()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gpsLocation = mLocationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                netLocation = mLocationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                computeLocation(null)
                true
            } else
                false
        } else
            false
    }
    // endregion

    // region fine location system
    internal var gps: LocationHandler? = null
    internal var net: LocationHandler? = null

    /**
     * returns true on expected behaviour
     *          false on pb
     */
    internal fun locate(enable: Boolean, mode: Boolean): Boolean {

        if (enable) {

            if (null != gps)
                gps!!.stop()

            if (null != net)
                net!!.stop()

            if (initLocation()) {
                when (mode) {
                    true -> {
                        gps = LocationHandler(context, LocationManager.GPS_PROVIDER,
                                GPS_UPDATE_INTERVAL, GPS_FASTEST_UPDATE_INTERVAL, this)
                        net = LocationHandler(context, LocationManager.NETWORK_PROVIDER,
                                NET_UPDATE_INTERVAL, NET_FASTEST_UPDATE_INTERVAL, this)
                    }
                    false -> {
                        gps = LocationHandler(context, LocationManager.GPS_PROVIDER,
                                NOT_TRAVELLING_UPDATE_INTERVAL, NOT_TRAVELLING_FASTEST_UPDATE_INTERVAL, this)
                        net = LocationHandler(context, LocationManager.NETWORK_PROVIDER,
                                NOT_TRAVELLING_UPDATE_INTERVAL, NOT_TRAVELLING_FASTEST_UPDATE_INTERVAL, this)
                    }
                }

                gps?.run()
                net?.run()

                return true

            }

            return false

        } else {

            if (null != gps)
                gps!!.stop()

            if (null != net)
                net!!.stop()

            gps = null
            net = null

            return true

        }
    }
    // endregion

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {
        listener.onStatusChanged(s, i, bundle)
    }

    override fun onProviderEnabled(s: String) {
        listener.onProviderEnabled(s)
    }

    override fun onProviderDisabled(s: String) {
        when (s) {
            LocationManager.GPS_PROVIDER -> gps?.stop()
            LocationManager.NETWORK_PROVIDER -> net?.stop()
        }
        listener.onProviderDisabled(s)
    }

    override fun onLocationChanged(location: Location) {
        computeLocation(location)
    }

    private fun computeLocation(loc: Location?) {
        var mLastKnownLocation: Location? = null
        when (loc) {
            null -> {
                mLastKnownLocation = if (null != gpsLocation) {
                    if (null != netLocation) {
                        if (netLocation!!.time > gpsLocation!!.time) {
                            netLocation
                        } else {
                            gpsLocation
                        }
                    } else {
                        gpsLocation
                    }
                } else {
                    null
                }
            }
            else -> {
                when (loc.provider!!) {
                    LocationManager.GPS_PROVIDER -> {
                        gpsLocation = loc
                        mLastKnownLocation = loc
                    }
                    LocationManager.NETWORK_PROVIDER -> {
                        netLocation = loc
                        mLastKnownLocation = when {
                            null == gpsLocation -> loc
                            loc.time - gpsLocation!!.time > OBSOLETE_POS -> loc
                            else -> gpsLocation!!
                        }
                    }
                }
            }
        }
        listener.onLocationChanged(mLastKnownLocation)
    }

    companion object {

        private const val NOT_TRAVELLING_UPDATE_INTERVAL: Long = 60000 //battery saver rate
        private const val NOT_TRAVELLING_FASTEST_UPDATE_INTERVAL: Long = 60000 //battery saver rate

        private const val GPS_UPDATE_INTERVAL: Long = 15000 // provide each
        private const val GPS_FASTEST_UPDATE_INTERVAL: Long = 5000 // not less than

        private const val NET_UPDATE_INTERVAL: Long = 15000
        private const val NET_FASTEST_UPDATE_INTERVAL: Long = 5000

        private const val OBSOLETE_POS = 60000 // 1mn
    }


}