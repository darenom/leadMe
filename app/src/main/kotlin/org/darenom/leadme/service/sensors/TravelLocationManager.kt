package org.darenom.leadme.service.sensors


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

/**
 * Created by adm on 26/10/2017.
 *
 * get locations from providers
 * computes best
 * forwards to service
 *
 */

internal class TravelLocationManager(private val context: Context, private val callback: Callback)
    : LocationCallback() {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var mLocationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    internal fun canLocate(): Boolean =
            mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    internal fun mayLocate(): Boolean =
            ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    internal fun locate(enable: Boolean, mode: Boolean) {

        val locationRequest: LocationRequest?
        if (enable) {
            if (mode) {
                locationRequest = LocationRequest().apply {
                    interval = 36000
                    fastestInterval = 12000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
            } else {
                locationRequest = LocationRequest().apply {
                    interval = 60000
                    fastestInterval = 30000
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                }
            }
            startLocationUpdates(locationRequest, if (mode) 1 else 0)
        } else {
            stopLocationUpdates()
        }

    }

    private fun startLocationUpdates(locationRequest: LocationRequest, mode: Int) {
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { response ->

            if (response.locationSettingsStates.isLocationPresent) {

                if (response.locationSettingsStates.isLocationUsable) {

                    builder.addLocationRequest(locationRequest)
                    if (ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                this,
                                null /* Looper */)
                        callback.onStatusChanged(mode)         // ok

                    } else
                        callback.onStatusChanged(-5) // missing permission

                } else
                    callback.onStatusChanged(-4) // missing feature

            } else
                callback.onStatusChanged(-3) // missing hardware

        }

        task.addOnFailureListener { _ -> callback.onStatusChanged(-2) } // failed to add
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(this)
        callback.onStatusChanged(-1)                    // stopped
    }

    override fun onLocationResult(p0: LocationResult?) {
        super.onLocationResult(p0)
        Log.e(this.javaClass.simpleName, "onLocationResult")
        if (p0!!.locations[0].hasAltitude())
            if (p0.locations[0].altitude > 0)
                if (p0.locations[0].accuracy < 70)
                    callback.onLocationChanged(p0.locations[0])
    }


    interface Callback {
        fun onStatusChanged(status: Int)
        fun onLocationChanged(location: Location)
    }
}