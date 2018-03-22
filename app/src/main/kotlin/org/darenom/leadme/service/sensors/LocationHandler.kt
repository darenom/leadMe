package org.darenom.leadme.service.sensors

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log


/**
 * Created by adm on 26/10/2017.
 */

internal class LocationHandler(
        context: Context,
        private val provider: String,
        private val interval: Long,
        private val f_interval: Long,
        private val listener: LocationListener
) : Handler(), Runnable, LocationListener {

    private val locationManager: LocationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

    private var list: ArrayList<Location> = ArrayList()
    internal var isUp: Boolean

    init {
        isUp = false
    }

    override fun onLocationChanged(l: Location) {
        // todo smooth/select
        list.add(l)
    }

    override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {listener.onStatusChanged(s, i, bundle)}

    override fun onProviderEnabled(s: String) {listener.onProviderEnabled(s)}

    override fun onProviderDisabled(s: String) {listener.onProviderDisabled(s)}

    override fun run() {

        if (list.isNotEmpty())
            listener.onLocationChanged(list[list.size-1])

        if (isUp) {
            locationManager.removeUpdates(this)
            list.clear()
        }

        startLocationRequest()

        postDelayed(this, interval)


    }

    private fun startLocationRequest() {
        isUp = try { locationManager.requestLocationUpdates(
                this.provider, this.f_interval, 0f, this)
            true }
        catch (e: IllegalArgumentException) { Log.e("LocationHandler", "IllegalArgumentException : ${e.message}"); false }
        catch (e: RuntimeException) { Log.e("LocationHandler", "RuntimeException : ${e.message}"); false }
        catch (e: SecurityException) { Log.e("LocationHandler", "SecurityException : ${e.message}"); false }


    }

    fun stop() {
        isUp = false
        list.clear()
        removeCallbacks(this)
        locationManager.removeUpdates(this)
    }

}