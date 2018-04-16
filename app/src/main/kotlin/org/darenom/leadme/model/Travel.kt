package org.darenom.leadme.model

import android.location.Location
import android.os.Build
import android.text.Html
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.model.DirectionsRoute
import org.darenom.leadme.BuildConfig

/**
 * Created by adm on 20/10/2017.
 * simple Travel model
 *      list of LatLng defining the route
 *      list of directions to be said
 *      list of road distances to follow
 *      list of straight distances to lead (see TravelSegment)
 *      list of ready to draw markers
 *      polyline to draw road on map
 *
 */

class Travel {

    var name = BuildConfig.TMP_NAME
    // lat/lng list of points
    var points: ArrayList<LatLng>? = null
    // Said infos
    var infos: ArrayList<String>? = null
    // direction results distances between points (roads length)
    var distances: ArrayList<Long>? = null
    // duration in seconds
    var durations: ArrayList<Long>? = null
    // straight distance between points
    var dists: ArrayList<Float>? = null
    // polylines
    var lines: ArrayList<String>? = null

    // exclude from Gson
    @Transient
    var markerList = ArrayList<MarkerOptions>()

    init {
        this.points = ArrayList()
        this.infos = ArrayList()
        this.distances = ArrayList()
        this.durations = ArrayList()
        this.lines = ArrayList()
    }

    @Suppress("DEPRECATION")
    fun transform(route: DirectionsRoute): Travel {
        route.legs.forEach {
            it.steps.forEachIndexed { index, s ->
                when (index) {
                    it.steps.size - 1 -> {
                        points?.add(LatLng(s.startLocation.lat, s.startLocation.lng))
                        distances?.add(s.distance.inMeters)
                        durations?.add(s.duration.inSeconds)
                        lines?.add(s.polyline.encodedPath)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            infos?.add(Html.fromHtml(s.htmlInstructions, Html.FROM_HTML_MODE_COMPACT).toString())
                        } else {
                            infos?.add(Html.fromHtml(s.htmlInstructions).toString())
                        }
                    }
                    else -> {
                        points?.add(LatLng(s.startLocation.lat, s.startLocation.lng))
                        distances?.add(s.distance.inMeters)
                        durations?.add(s.duration.inSeconds)
                        lines?.add(s.polyline.encodedPath)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            infos?.add(Html.fromHtml(s.htmlInstructions, Html.FROM_HTML_MODE_COMPACT).toString())
                        } else {
                            infos?.add(Html.fromHtml(s.htmlInstructions).toString())
                        }
                    }
                }
            }
        }

        distances?.add(0)
        durations?.add(0)
        infos?.add(" arrivee ")
        lines?.add("")
        points?.add(LatLng(
                route.legs[route.legs.size - 1]
                        .steps[route.legs[route.legs.size - 1].steps.size - 1]
                        .endLocation.lat,
                route.legs[route.legs.size - 1]
                        .steps[route.legs[route.legs.size - 1].steps.size - 1]
                        .endLocation.lng))

        computeDists()
        return this
    }

    /**
     * Get straight distance between points
     */
    fun computeDists() {
        this.dists = ArrayList()
        for (i in 0 until this.points!!.size - 1) {
            val a = Location("a")
            a.latitude = this.points!![i].latitude
            a.longitude = this.points!![i].longitude

            val b = Location("b")
            b.latitude = this.points!![i + 1].latitude
            b.longitude = this.points!![i + 1].longitude

            this.dists?.add(a.distanceTo(b))
            this.markerList.add(toMarkerOption(i))
        }
        this.markerList.add(toMarkerOption(this.points!!.size - 1))
    }

    private fun toMarkerOption(index: Int): MarkerOptions {
        val m = MarkerOptions()
        when (index) {
            0 -> m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            points!!.size - 1 -> m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            else -> m.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        }
        m.draggable(false)
        m.title(this.distances!![index].toString() + "m - " + this.infos!![index])
        m.snippet(this.points!![index].toString())
        m.position(this.points!![index])
        return m
    }
}