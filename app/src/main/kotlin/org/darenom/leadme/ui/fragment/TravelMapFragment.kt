package org.darenom.leadme.ui.fragment

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.*
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import kotlinx.android.synthetic.main.layout_view_compass.*
import org.darenom.leadme.BaseApp
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.MainActivity
import org.darenom.leadme.R
import org.darenom.leadme.databinding.FragmentMapBinding
import org.darenom.leadme.db.entities.TravelSetEntity
import org.darenom.leadme.model.Travel
import org.darenom.leadme.service.TravelService
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.service.TravelService.Companion.travelling
import org.darenom.leadme.ui.TravelFragment.Companion.CHECK_NET_ACCESS
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*

/**
 * Created by admadmin on 24/01/2018.
 */

class TravelMapFragment : Fragment(), OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnInfoWindowClickListener {

    internal var mBinding: FragmentMapBinding? = null

    private var svm: SharedViewModel? = null

    private var gm: GoogleMap? = null

    private var subscribed: Boolean = false

    private var infoWindowId: String = ""

    private val mMapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
            // point to north
                TravelService.ORIENTATION_CHANGED -> {
                    layout_compass?.onOrientationChanged(intent.getFloatArrayExtra(TravelService.ORIENTATION_CHANGED))
                }
            // point to closest
                TravelService.DIRECTION_CHANGED -> {
                    mBinding?.showDirection = true
                    layout_compass?.onDirectionChanged(intent.getFloatExtra(TravelService.DIRECTION_CHANGED, 0f))
                }
                TravelService.ARRIVED -> {
                    if (TravelService.travelling)
                        (activity!! as MainActivity).startStopTravel(null)
                }
                TravelService.MY_WAY_BACK -> {
                    mBinding?.showDirection = false
                }
                TravelService.SEGMENT_CHANGED -> {
                    val lats = intent.getParcelableArrayListExtra<com.google.android.gms.maps.model.LatLng>(TravelService.SEGMENT_SIDES)
                    val length = intent.getFloatExtra(TravelService.SEGMENT_LENGTH, 0f)
                    redraw(lats, length)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        retainInstance = true
        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeUI()
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.ORIENTATION_CHANGED))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.DIRECTION_CHANGED))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.ARRIVED))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.MY_WAY_BACK))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.SEGMENT_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mMapReceiver)
    }

    // region Listeners
    override fun onMapReady(p0: GoogleMap?) {

        gm = p0

        gm!!.setOnMapClickListener(this)
        gm!!.setOnMarkerClickListener(this)
        gm!!.setOnMarkerDragListener(this)
        gm!!.setOnInfoWindowClickListener(this)

        gm!!.uiSettings.isMapToolbarEnabled = false
        gm!!.uiSettings.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            gm!!.isMyLocationEnabled = true

        // observer may occur before map is ready so...
        if (null != travel.value) {
            drawTravel(travel.value!!)
        } else if (null != svm!!.travelSet.value) {
            drawSet(svm!!.travelSet.value!!)
        }

    }

    override fun onMapClick(it: LatLng) {
        // prevent click if travel is loaded
        if (null == travel.value) {
            // check needed data access
            if (!(activity!!.application as BaseApp).isNetworkAvailable) {
                activity!!.startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), CHECK_NET_ACCESS)
            } else {
                if (infoWindowId.contentEquals("")) {
                    // process
                    if (!svm!!.setLocationAs(null, com.google.maps.model.LatLng(it.latitude, it.longitude))) {
                        Toast.makeText(activity!!, getString(R.string.max_waypoints), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // off info window
                    infoWindowId = ""
                }
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (infoWindowId.contentEquals(marker.id)) {
            if (null == travel.value) {
                svm!!.removePoint(marker.title)
                infoWindowId = ""
            }
        } else {
            infoWindowId = marker.id
            marker.showInfoWindow()
        }
        return true
    }

    override fun onMarkerDragEnd(marker: Marker) {

        if (null == travel.value) {
            val s = svm!!.getAddress(marker.position.latitude, marker.position.longitude)
            when (marker.title) {
                svm!!.travelSet.value!!.originAddress -> {
                    svm!!.travelSet.value!!.originAddress = s
                    svm!!.travelSet.value!!.originPosition = "${marker.position.latitude}/${marker.position.longitude}"
                }
                svm!!.travelSet.value!!.destinationAddress -> {
                    svm!!.travelSet.value!!.destinationAddress = s
                    svm!!.travelSet.value!!.destinationPosition = "${marker.position.latitude}/${marker.position.longitude}"
                }
                else -> {
                    val adr = svm!!.travelSet.value!!.waypointAddress.split("#")
                    val pos = svm!!.travelSet.value!!.waypointPosition.split("#")
                    var ad = ""
                    var po = ""
                    adr.forEach {
                        if (it.isNotEmpty()) {
                            if (!marker.title!!.contentEquals(it)) {
                                ad += "$it#"
                                po += "${pos[adr.indexOf(it)]}#"
                            } else {
                                ad += "$s#"
                                po += "${marker.position.latitude}/${marker.position.longitude}#"
                            }
                        }
                    }
                    svm!!.travelSet.value!!.waypointAddress = ad
                    svm!!.travelSet.value!!.waypointPosition = po
                }
            }
            svm!!.update(svm!!.travelSet.value!!)
        }
    }

    override fun onMarkerDragStart(p0: Marker?) {
        // do nothing
    }

    override fun onMarkerDrag(p0: Marker?) {
        // do nothing
    }

    override fun onInfoWindowClick(p0: Marker?) {
        infoWindowId = ""
        p0!!.hideInfoWindow()

    }
    // endregion

    private fun subscribeUI() {
        subscribed = true

        svm!!.optCompass.observe(this, Observer { it ->
            if (null != it) {
                mBinding!!.showCompass = it
                (activity!!.application as BaseApp).travelService!!.enableCompass(it, 0)
            }
        })

        svm!!.travelSet.observe(this, Observer { it ->
            if (null != it) {
                if (null == travel.value) {
                    drawSet(it)     // otherwise draw from input
                }
                activity!!.invalidateOptionsMenu()
            }
        })

        travel.observe(this, Observer { it ->
            if (null != it) {
                if (!processing) { // long time running ops, travel change may occur multiple at a time
                    mBinding!!.showProgress = true
                    processing = true
                    drawTravel(it)
                    // save tmp file only
                    if (it.name.contentEquals(BuildConfig.TMP_NAME))
                        svm!!.write(travel.value!!)
                }
            } else {
                gm?.clear()
            }
            activity!!.invalidateOptionsMenu()
        })
    }

    private var processing = false

    fun clear() {
        infoWindowId = ""
        if (null != gm) {
            gm!!.clear()
        }
    }

    // region DrawOnMap
    private fun drawSet(it: TravelSetEntity) {
        val ways = ArrayList<MarkerOptions>()
        if (it.waypointPosition.isNotEmpty()) {
            val h = it.waypointAddress.split("#")
            val i = it.waypointPosition.split("#")
            for (l in i) {
                if (l.isNotEmpty()) {
                    val j = l.split("/")
                    ways.add(MarkerOptions()
                            .draggable(true)
                            .title(h[i.indexOf(l)])
                            .position(com.google.android.gms.maps.model.LatLng(j[0].toDouble(), j[1].toDouble()))
                    )
                }
            }
        }
        drawMarkers(ways, null)
    }

    // redraw travel infos while travelling (travel + support)
    internal fun redraw(lats: java.util.ArrayList<LatLng>?, length: Float) {
        val mMarkers: ArrayList<MarkerOptions> = ArrayList()
        val mCircles: ArrayList<CircleOptions> = ArrayList()
        for (i in 0..3) {
            val c: CircleOptions = CircleOptions()
                    .center(lats!![i])
                    .radius((length / 2).toDouble())
                    .strokeColor(Color.RED)
                    .strokeWidth(2f)

            if (i < 2) {
                c.radius(length.toDouble())
                val m: MarkerOptions = MarkerOptions()
                        .title(" _ ")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                        .position(lats[i])
                mMarkers.add(m)
            }
            mCircles.add(c)
        }
        drawTravel(travel.value!!)
        drawMarkers(mMarkers, mCircles)
    }

    private fun drawMarkers(markers: ArrayList<MarkerOptions>, circles: ArrayList<CircleOptions>?) {
        if (null != gm) {
            // support
            if (null != circles) {
                if (markers.isNotEmpty())
                    for (marker: MarkerOptions in markers)
                        gm!!.addMarker(marker)

                if (circles.isNotEmpty())
                    for (c: CircleOptions in circles)
                        gm!!.addCircle(c)
            } else {
                // normal
                gm!!.clear()

                // add origin
                if (svm!!.travelSet.value!!.originPosition.isNotEmpty()) {
                    val o = svm!!.travelSet.value!!.originPosition.split("/")
                    markers.add(MarkerOptions()
                            .draggable(true)
                            .title(svm!!.travelSet.value!!.originAddress)
                            .position(LatLng(o[0].toDouble(), o[1].toDouble()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                }

                // add destination
                if (svm!!.travelSet.value!!.destinationPosition.isNotEmpty()) {
                    val d = svm!!.travelSet.value!!.destinationPosition.split("/")
                    markers.add(MarkerOptions()
                            .draggable(true)
                            .title(svm!!.travelSet.value!!.destinationAddress)
                            .position(LatLng(d[0].toDouble(), d[1].toDouble()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                }

                // waypoints
                markers.forEach { gm!!.addMarker(it) }
            }
        }

    }

    private fun drawTravel(travel: Travel) {
        if (null != gm) {
            gm!!.clear()
            (activity!!.application as BaseApp).mAppExecutors!!.diskIO().execute {
                if (travel.markerList.isEmpty())
                    travel.computeDists()
                (activity!!.application as BaseApp).mAppExecutors!!.mainThread().execute {
                    travel.markerList.forEachIndexed { index, it ->
                        gm!!.addMarker(it)
                        gm!!.addPolyline(PolylineOptions().width(3f).color(R.color.colorPrimary)
                                .addAll(PolyUtil.decode(travel.lines!![index])))
                    }
                    if (!travelling)
                        zoomOnAll(travel.markerList)
                }
            }
        }
    }

    private fun zoomOnAll(list: ArrayList<MarkerOptions>) {
        if (null != gm) {
            if (list.isNotEmpty()) {
                // define bounds of markers
                val builder = LatLngBounds.Builder()
                list.forEach { builder.include(it.position) }
                val bounds = builder.build()
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, 50) // animate
                gm!!.animateCamera(cu, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {   // on finish save snapshot
                        gm!!.snapshot { svm!!.write(it) }
                        mBinding!!.showProgress = false
                        processing = false
                    }
                    override fun onCancel() {
                        mBinding!!.showProgress = false
                        processing = false
                    }
                })
            } else {
                mBinding!!.showProgress = false
                processing = false
            }
        } else {
            mBinding!!.showProgress = false
            processing = false
        }
    }
    // endregion

    companion object {

        private var fragment: TravelMapFragment? = null
        fun getInstance(): TravelMapFragment {
            if (null == fragment) {
                fragment = TravelMapFragment()
            }
            return fragment!!
        }
    }

}