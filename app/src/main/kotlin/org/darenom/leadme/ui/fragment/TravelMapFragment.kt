package org.darenom.leadme.ui.fragment

import android.Manifest
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
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
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import kotlinx.android.synthetic.main.layout_view_compass.*
import org.darenom.leadme.AppExecutors
import org.darenom.leadme.R
import org.darenom.leadme.TravelActivity
import org.darenom.leadme.TravelActivity.Companion.CHECK_MAP
import org.darenom.leadme.TravelActivity.Companion.CHECK_NET_ACCESS
import org.darenom.leadme.TravelActivity.Companion.CHECK_START_MOTION
import org.darenom.leadme.TravelActivity.Companion.NOTIF_ID
import org.darenom.leadme.TravelActivity.Companion.PERM_MAP
import org.darenom.leadme.TravelActivity.Companion.PERM_START_MOTION
import org.darenom.leadme.databinding.FragmentMapBinding
import org.darenom.leadme.model.Travel
import org.darenom.leadme.room.entities.TravelSetEntity
import org.darenom.leadme.service.TravelService
import org.darenom.leadme.service.TravelService.Companion.ARRIVED
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.service.TravelService.Companion.travelling
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*

/**
 * Created by admadmin on 24/01/2018.
 */

class TravelMapFragment : Fragment(), OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnInfoWindowClickListener {

    internal lateinit var mBinding: FragmentMapBinding

    private var svm: SharedViewModel? = null

    private var gm: GoogleMap? = null

    private var processing: Boolean = false

    private var infoWindowId: String = ""

    private val mMapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TravelService.LOCATION_UP -> {
                    activity!!.invalidateOptionsMenu()
                    locationButtonState()
                }
                TravelService.LOCATION_DOWN -> {
                    activity!!.invalidateOptionsMenu()
                    locationButtonState()
                }
                TravelService.NO_MOTION -> {
                    if (!(activity!! as TravelActivity).travelService!!.hasPos)
                        if (!(activity!! as TravelActivity).locCheck) {
                            (activity!! as TravelActivity).locCheck = true
                            activity!!.startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), CHECK_START_MOTION)
                        } else
                            if (ContextCompat.checkSelfPermission(context,
                                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                                if (!(activity!! as TravelActivity).locPerm) {
                                    (activity!! as TravelActivity).locPerm = true
                                    ActivityCompat.requestPermissions(activity!!,
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                            PERM_START_MOTION)
                                }
                }

                TravelService.REFRESH_UI -> {
                    activity!!.invalidateOptionsMenu()
                    // notify user, travel has started or stopped
                    val pendingIntent = PendingIntent.getActivity(
                            activity!!.applicationContext,
                            0,
                            Intent(activity!!.applicationContext, TravelActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            0
                    )

                    val notif =  NotificationCompat.Builder(activity!!.applicationContext, getString(R.string.app_name))
                            .setSmallIcon(R.drawable.ic_notif)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.notif_main_msg))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)

                    if (travelling){

                        val pendingActionIntent = PendingIntent.getActivity(activity!!.applicationContext, 1,
                                Intent(activity!!.applicationContext, TravelActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        .setAction(ARRIVED),
                                0
                        )

                        val action = NotificationCompat.Action.Builder(
                                R.drawable.ic_pause,
                                getString(R.string.stop),
                                pendingActionIntent
                        ).build()

                        notif.setContentText(getString(R.string.travelling))
                        notif.addAction(action)


                    }

                    NotificationManagerCompat.from(activity!!.applicationContext).notify(
                            NOTIF_ID,
                            notif.build()
                    )
                }
            // point to north
                TravelService.ORIENTATION_CHANGED -> {
                    layout_compass?.onOrientationChanged(intent.getFloatArrayExtra(TravelService.ORIENTATION_CHANGED))
                }
            // point to closest
                TravelService.DIRECTION_CHANGED -> {
                    mBinding.showDirection = true
                    layout_compass?.onDirectionChanged(intent.getFloatExtra(TravelService.DIRECTION_CHANGED, 0f))
                }
                TravelService.ARRIVED -> {
                    mBinding.showDirection = false
                    (activity!! as TravelActivity).startStopTravel()
                }
                TravelService.MY_WAY_BACK -> {
                    mBinding.showDirection = false
                }
                TravelService.SEGMENT_CHANGED -> {
                    val lats = intent.getParcelableArrayListExtra<com.google.android.gms.maps.model.LatLng>(TravelService.SEGMENT_SIDES)
                    val length = intent.getFloatExtra(TravelService.SEGMENT_LENGTH, 0f)
                    redraw(lats, length)
                }
            }
        }
    }

    // region LifeCycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mBinding.hasCompass = (activity!! as TravelActivity).travelService?.hasCompass ?: false
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

        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.NO_MOTION))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.LOCATION_DOWN))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.LOCATION_UP))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.REFRESH_UI))
    }

    override fun onResume() {
        super.onResume()
        if (null != gm) {
            activity!!.invalidateOptionsMenu()
            if ((activity!! as TravelActivity).travelService!!.hasPos) {
                locationButtonState()
            } else {
                gm!!.uiSettings.isMyLocationButtonEnabled = false
                mBinding.showDirection = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.opt_clear -> infoWindowId = ""
            R.id.opt_play_stop -> mBinding.showDirection = false
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mMapReceiver)
    }

    private fun subscribeUI() {
        svm!!.optCompass.observe(this, Observer { it ->
            if (null != it) {
                mBinding.showCompass = it
                (activity!! as TravelActivity).travelService?.enableCompass(it, 0)
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

        svm!!.travelRun.observe(this, Observer { it ->
            if (null != it) {
                if (!processing) { // long run op, fool
                    mBinding.showProgress = true
                    processing = true
                    drawTravel(travel.value!!)
                    drawRun(it)
                }
            }
        })

        travel.observe(this, Observer { it ->
            if (null != it) {
                if (!processing) { // long run op, fool
                    mBinding.showProgress = true
                    processing = true
                    drawTravel(it)
                }
            } else {
                gm?.clear()
            }
            activity!!.invalidateOptionsMenu()
        })

    }
    // endregion

    // region Listeners
    override fun onMyLocationButtonClick(): Boolean {
        if ((activity!! as TravelActivity).travelService!!.hasPos) {
            if (ContextCompat.checkSelfPermission(context!!,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                if (!(activity!! as TravelActivity).locPerm) {
                    (activity!! as TravelActivity).locPerm = true
                    ActivityCompat.requestPermissions(activity!!,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            PERM_MAP)
                }
        } else
            if (!(activity!! as TravelActivity).locCheck) {
                (activity!! as TravelActivity).locCheck = true
                activity!!.startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), CHECK_MAP)
            }
        return false
    }

    override fun onMapReady(p0: GoogleMap?) {
        if (null != p0) {
            gm = p0

            gm!!.setOnMapClickListener(this)
            gm!!.setOnMarkerClickListener(this)
            gm!!.setOnMarkerDragListener(this)
            gm!!.setOnInfoWindowClickListener(this)
            gm!!.setOnMyLocationButtonClickListener(this)

            gm!!.uiSettings.isMapToolbarEnabled = false

            if (!travelling) {
                // observer may occur before map is ready so...
                if (null != travel.value) {
                    drawTravel(travel.value!!)
                } else if (null != svm!!.travelSet.value) {
                    drawSet(svm!!.travelSet.value!!)
                }
            }
        }
    }

    private fun locationButtonState() {
        if (null != gm) {

            gm!!.isMyLocationEnabled = false
            gm!!.uiSettings.isMyLocationButtonEnabled = false
            mBinding.showDirection = false

            if ((activity!! as TravelActivity).travelService!!.hasPos) {
                gm!!.isMyLocationEnabled = true
                gm!!.uiSettings.isMyLocationButtonEnabled = true
            } else
                if (ContextCompat.checkSelfPermission(context!!,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (!(activity!! as TravelActivity).locPerm) {
                        (activity!! as TravelActivity).locPerm = true
                        ActivityCompat.requestPermissions(activity!!,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                PERM_MAP)
                    }
                } else {
                    if (!(activity!! as TravelActivity).locCheck) {
                        (activity!! as TravelActivity).locCheck = true
                        activity!!.startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), CHECK_MAP)
                    }
                }
        }
    }


    override fun onMapClick(it: LatLng) {
        // prevent click if travel is loaded
        if (null == travel.value) {
            // check needed data access
            if (!(activity!! as TravelActivity).isNetworkAvailable) {
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
            AppExecutors.getInstance().diskIO().execute {
                if (travel.markerList.isEmpty())
                    travel.computeDists()
                AppExecutors.getInstance().mainThread().execute {
                    travel.markerList.forEachIndexed { index, it ->
                        gm!!.addMarker(it)
                        gm!!.addPolyline(PolylineOptions().width(3f).color(R.color.colorPrimary)
                                .addAll(PolyUtil.decode(travel.lines!![index])))
                    }
                    zoomOnAll(travel.markerList)
                }
            }
        }
    }

    private fun drawRun(it: List<LatLng>) {
        gm!!.addPolyline(PolylineOptions().width(3f).color(R.color.colorAccent).addAll(it))
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
                        //gm!!.snapshot { svm!!.write(it) }
                        mBinding.showProgress = false
                        processing = false
                    }

                    override fun onCancel() {
                        mBinding.showProgress = false
                        processing = false
                    }
                })
            } else {
                mBinding.showProgress = false
                processing = false
            }
        } else {
            mBinding.showProgress = false
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