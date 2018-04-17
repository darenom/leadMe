package org.darenom.leadme.service.sensors


import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v4.content.ContextCompat
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

class TravelLocationManager
internal constructor(private val context: Context, private val listener: LocationListener)
    : LocationCallback() {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var mLocationManager: LocationManager? = context.getSystemService(LOCATION_SERVICE) as LocationManager

    override fun onLocationResult(p0: LocationResult?) {
        super.onLocationResult(p0)
        listener.onLocationChanged(p0!!.locations[0])
    }

    // Features availability
    internal fun canLocate(): Boolean =
            mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    internal fun mayLocate(): Boolean =
            ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED


    internal fun locate(enable: Boolean, mode: Boolean): Boolean {
        return if (initLocation()) {
            val builder = LocationSettingsRequest.Builder()
            val client: SettingsClient = LocationServices.getSettingsClient(context)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
            val locationRequest: LocationRequest?
            if (enable) {
                if (mode) {
                    locationRequest = LocationRequest().apply {
                        interval = 10000
                        fastestInterval = 5000
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    }
                } else {
                    locationRequest = LocationRequest().apply {
                        interval = 60000
                        fastestInterval = 30000
                        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                }

                task.addOnSuccessListener { _ ->
                    builder.addLocationRequest(locationRequest)
                    startLocationUpdates(locationRequest)
                }

                task.addOnFailureListener { _ ->

                }
            } else {
                stopLocationUpdates()
            }
            true
        } else
            false
    }

    private fun initLocation(): Boolean {
        return if (canLocate()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            listener.onLocationChanged(location)
                        }
                true
            } else
                false
        } else
            false
    }

    private fun startLocationUpdates(locationRequest: LocationRequest) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    this,
                    null /* Looper */)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(this)
    }
}