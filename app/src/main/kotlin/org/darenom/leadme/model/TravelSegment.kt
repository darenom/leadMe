package org.darenom.leadme.model

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.darenom.leadme.BuildConfig

/**
 * Created by adm on 25/10/2017.
 * Infos about my location within the Travel.points
 */

class TravelSegment(private val travel: Travel) {

    // closest point in travel from my position
    var closest: Int = 0
    // segment length
    var length: Float = 0f

    // previous, next (indexes of travel points)
    var index: IntArray = IntArray(2)

    // previous, next, right, left (distances me to locations)
    private var distance: FloatArray = FloatArray(4)
    var latlngs: ArrayList<LatLng> = ArrayList(4)

    /**
     * Actually returned by computeSegment
     * retreive the user's status according to the provided location to any of :
     *      ARRIVED          within MAX_RADIUS of last point
     *      OUT_OF_BOUNDS    out of the way
     *      REACHED          is within MAX_RADIUS of way point
     *      ON_THE_WAY       on tracks
     *
     * being X, travelling from  A to B.
     * C and D are defined as summits of the equilateral triangle of AB base,
     * the way is defined as the superposed areas of circles:
     *      c(A) and c(B) of AB/2 radius
     *      c(C), c(D) of AB radius
     *
     * @return status
     */
    private var status: Int? = null
        get() {
            val distToRight: Float
            val distToLeft: Float
            val lenght: Float
            val distToPrevious = this.distance[0]
            val distToNext = this.distance[1]
            var status = OUT_OF_BOUNDS
            if (this.index[0] == -1) {
                //am before first
                if (distToNext < MAX_RADIUS) {
                    status = REACHED
                }
            } else if (this.index[1] == -1) {
                // am after last
                if (distToPrevious < MAX_RADIUS) {
                    status = REACHED
                }
            } else {
                // am within travel
                lenght = this.length
                distToRight = this.distance[2]
                distToLeft = this.distance[3]

                if (distToRight < lenght && distToLeft < lenght) {
                    // i am still within both sides circles, good positioning
                    status = if (distToNext < MAX_RADIUS) {
                        // i am about to reach next
                        if (this.index[1] == travel.points!!.size - 1) {
                            // is Last
                            ARRIVED
                        } else {
                            // is any
                            REACHED
                        }
                    } else {
                        ON_THE_WAY
                    }
                } else if (distToRight < lenght) {
                    // in right
                    if (distToNext < lenght / 2) {
                        // next is in reach
                        status = if (distToNext < MAX_RADIUS) {
                            // i am about to reach next
                            if (this.index[1] == travel.points!!.size - 1) {
                                // is Last
                                ARRIVED
                            } else {
                                // is any
                                REACHED
                            }
                        } else {
                            ON_THE_WAY
                        }
                    } else if (distToPrevious < lenght / 2) {
                        // previous is in reach
                        status = ON_THE_WAY
                    }

                } else if (distToLeft < lenght) {
                    // in left
                    if (distToNext < lenght / 2) {
                        // next is in reach
                        status = if (distToNext < MAX_RADIUS) {
                            // i am about to reach next
                            if (this.index[1] == travel.points!!.size - 1) {
                                // is Last
                                ARRIVED
                            } else {
                                // is any
                                REACHED
                            }
                        } else {
                            ON_THE_WAY
                        }
                    } else if (distToPrevious < lenght / 2) {
                        // previous is in reach
                        status = ON_THE_WAY
                    }
                } else {
                    // in none
                    if (distToNext < lenght / 2) {
                        // next is in reach
                        status = if (distToNext < MAX_RADIUS) {
                            // i am about to reach next
                            if (this.index[1] == travel.points!!.size - 1) {
                                // is Last
                                ARRIVED
                            } else {
                                // is any
                                REACHED
                            }
                        } else {
                            ON_THE_WAY
                        }
                    } else if (distToPrevious < lenght / 2) {
                        // previous is in reach
                        status = ON_THE_WAY
                    }
                }
            }
            return status
        }

    /**
     * Sets the segments according to location:
     *  get previous and next points,
     *  calculate sides points to make a square plan,
     *  get all distances from these points,
     *  computes status.
     *
     * @param here - current Location
     * @return status,  any of ARRIVED, OUT_OF_BOUNDS, ON_THE_WAY, REACHED.
     */
    fun computeSegment(here: Location): Int {

        val lastIndex: Int
        val medists: ArrayList<Float> = ArrayList() // distances from me
        for (point in travel.points!!) {
            val loc = Location("loc")
            loc.latitude = point.latitude
            loc.longitude = point.longitude
            medists += here.distanceTo(loc)
        }
        lastIndex = medists.size - 1

        // getClosest
        val itr = medists.listIterator()
        var min: Float? = itr.next() // first element as the current minimum
        var minIndex = itr.previousIndex()
        while (itr.hasNext()) {
            val curr: Float = itr.next()
            if (curr < min!!) {
                min = curr
                minIndex = itr.previousIndex()
            }
        }

        this.closest = minIndex

        if (minIndex in 1..(lastIndex - 1)) {
            if (travel.dists!![minIndex] - medists[minIndex] > 0 && travel.dists!![minIndex - 1] - medists[minIndex - 1] < 0) {
                this.index[0] = minIndex
                this.index[1] = minIndex + 1
                this.distance[0] = medists[minIndex]
                this.distance[1] = medists[minIndex + 1]
            } else if (travel.dists!![minIndex] - medists[minIndex] < 0 && travel.dists!![minIndex - 1] - medists[minIndex - 1] > 0) {
                this.index[0] = minIndex - 1
                this.index[1] = minIndex
                this.distance[0] = medists[minIndex - 1]
                this.distance[1] = medists[minIndex]
            } else {
                if (medists[minIndex - 1] < medists[minIndex + 1]) {
                    this.index[0] = minIndex - 1
                    this.index[1] = minIndex
                    this.distance[0] = medists[minIndex - 1]
                    this.distance[1] = medists[minIndex]
                } else {
                    this.index[0] = minIndex
                    this.index[1] = minIndex + 1
                    this.distance[0] = medists[minIndex]
                    this.distance[1] = medists[minIndex + 1]
                }
            }
            completeSegment(here)

        } else if (minIndex == 0) {
            if (medists[1] > travel.dists!![0]) {
                this.index[0] = -1
                this.index[1] = 0
                this.distance[0] = -1f
                this.distance[1] = medists[0]
            } else {
                this.index[0] = 0
                this.index[1] = 1
                this.distance[0] = medists[0]
                this.distance[1] = medists[1]
                completeSegment(here)
            }

        } else if (minIndex == lastIndex) {
            if (medists[lastIndex - 1] > travel.dists!![lastIndex - 1]) {
                this.index[0] = minIndex
                this.index[1] = -1
                this.distance[0] = medists[minIndex]
                this.distance[1] = -1f
            } else {
                this.index[0] = minIndex - 1
                this.index[1] = minIndex
                this.distance[0] = medists[minIndex - 1]
                this.distance[1] = medists[minIndex]
                completeSegment(here)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d("Travel______compute", "Closest is $minIndex, ${medists[minIndex]}" +
                    "Next is ${if (this.index[1] == -1) this.index[0] else this.index[1]}, " +
                    "${if (this.index[1] == -1) this.distance[0] else this.distance[1]}m")
        }

        return status!!
    }

    private fun completeSegment(here: Location) {
        val b = Location("next")
        b.latitude = travel.points!![this.index[1]].latitude
        b.longitude = travel.points!![this.index[1]].longitude
        b.bearing = 0f

        val a = Location("previous")
        a.latitude = travel.points!![this.index[0]].latitude
        a.longitude = travel.points!![this.index[0]].longitude
        a.bearing = 0f

        // restrictive on laced roads
        this.length = a.distanceTo(b)

        // abusive on straight lines
        //travelthis.length = travel.getDistances().get(this.routePoint)

        this.latlngs = getSidePoint(a, b, this.length)
        this.latlngs[2] = travel.points!![this.index[0]]
        this.latlngs[3] = travel.points!![this.index[1]]

        val c = Location("right")
        c.latitude = this.latlngs[0].latitude
        c.longitude = this.latlngs[0].longitude
        c.bearing = 0f
        this.distance[2] = here.distanceTo(c)

        val d = Location("left")
        d.latitude = this.latlngs[1].latitude
        d.longitude = this.latlngs[1].longitude
        d.bearing = 0f
        this.distance[3] = here.distanceTo(d)
    }

    /**
     * Get lat/long given current point, distance and bearing
     *
     * @param from
     * @param e    distance in kilometers
     * @return latlng
     */
    private fun getSidePoint(from: Location, to: Location, e: Float): ArrayList<LatLng> {

        val sides = ArrayList<LatLng>()
        val r = 6378.1 //Radius of the Earth
        val brnginit = 1.0472 //Bearing is 60 degrees converted to radians.
        val d = e / 1000

        val brng = Math.toRadians(from.bearingTo(to).toDouble())
        val brng0 = brng + brnginit
        val brng1 = brng - brnginit

        val lat1 = Math.toRadians(from.latitude) //Current lat point converted to radians
        val lon1 = Math.toRadians(from.longitude) //Current long point converted to radians

        var lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / r) + Math.cos(lat1) * Math.sin(d / r) * Math.cos(brng0))

        var lon2 = lon1 + Math.atan2(Math.sin(brng0) * Math.sin(d / r) * Math.cos(lat1),
                Math.cos(d / r) - Math.sin(lat1) * Math.sin(lat2))


        var lat3 = Math.asin(Math.sin(lat1) * Math.cos(d / r) + Math.cos(lat1) * Math.sin(d / r) * Math.cos(brng1))

        var lon3 = lon1 + Math.atan2(Math.sin(brng1) * Math.sin(d / r) * Math.cos(lat1),
                Math.cos(d / r) - Math.sin(lat1) * Math.sin(lat3))

        lat2 = Math.toDegrees(lat2)
        lon2 = Math.toDegrees(lon2)

        lat3 = Math.toDegrees(lat3)
        lon3 = Math.toDegrees(lon3)

        sides.add(LatLng(lat2, lon2))
        sides.add(LatLng(lat3, lon3))
        sides.add(LatLng(0.0, 0.0))
        sides.add(LatLng(0.0, 0.0))
        return sides

    }

    companion object {
        const val MAX_RADIUS = 40
        const val ARRIVED = 0
        const val OUT_OF_BOUNDS = 1
        const val ON_THE_WAY = 2
        const val REACHED = 3
    }
}