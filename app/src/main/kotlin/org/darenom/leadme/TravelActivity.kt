package org.darenom.leadme

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.android.synthetic.main.activity_travel.*
import org.darenom.leadme.db.DateConverter
import org.darenom.leadme.model.Travel
import org.darenom.leadme.service.TravelService
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.service.TravelService.Companion.travelling
import org.darenom.leadme.ui.SaveTravelDialog
import org.darenom.leadme.ui.fragment.PanelFragment
import org.darenom.leadme.ui.fragment.TravelMapFragment
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*

/**
 * Created by adm on 02/02/2018.
 *
 * input start, end are mandatory to request DirectionAPI.
 * waypoints may be added
 *
 * Travel can be namely saved upon results or if played at least once,
 * otherwise it'll be saved as temporary for maintained state purposes.
 * If not saved after being played, records will be wiped from the database
 * (still, infos about the travel remains as temporary)
 */

class TravelActivity : AppCompatActivity(),
        PendingResult.Callback<DirectionsResult>,
        SaveTravelDialog.SaveTravelDialogListener,
        SlidingUpPanelLayout.PanelSlideListener {


    companion object {
        const val CHECK_TTS_ACCESS = 40
        const val CHECK_NET_ACCESS = 41
        const val PERM_SET_HERE = 101
        const val PERM_START_MOTION = 102
        const val LOCATION_START_MOTION = 103
        const val LOCATION_SET_HERE = 104

    }

    private var svm: SharedViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as BaseApp).mActivity?.loader?.progress = 40
        setContentView(R.layout.activity_travel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        sliding_panel.addPanelSlideListener(this)

        svm = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        subscribeUI()

        ttsChecker()

    }

    private fun subscribeUI() {

        travel.observe(this, Observer { it ->
            if (null != it) {
                if (it.name.contentEquals(BuildConfig.TMP_NAME)) {
                    svm!!.write(it)     // save tmp file only
                    supportActionBar?.setTitle(R.string.app_name)
                } else
                    supportActionBar?.title = it.name
                (panel as PanelFragment).setPanel(1)
                fabState(0)
            } else {
                supportActionBar?.setTitle(R.string.app_name)
                (panel as PanelFragment).setPanel(0)
                fabState(2)
            }
            setFabVisiblity()
            sliding_panel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        })

        svm!!.name.observe(this, Observer { it ->
            if (null != it) {
                // todo async task load data
                svm!!.read(it) // set Travel
                svm!!.monitor(it) // set TravelSet

            }
        })

        svm!!.travelRun.observe(this, Observer {
            if (null != it)
                sliding_panel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        })

        svm!!.travelAlt.observe(this, Observer {
            if (null != it)
                (panel as PanelFragment).statFragment!!.showGraph(it)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_appbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)

        val mnuCompass = menu?.findItem(R.id.opt_compass)
        val mnuClear = menu?.findItem(R.id.opt_clear)
        val mnuTravel = menu?.findItem(R.id.opt_play_stop)
        val mnuAskSave = menu?.findItem(R.id.opt_direction_save)

        mnuClear?.isVisible = when (TravelService.travelling) {
            true ->  false
            else -> svm!!.canCancel
        }


        mnuCompass?.isVisible = (application as BaseApp).travelService!!.hasCompass
        mnuCompass?.isChecked = svm!!.optCompass.value!!

        mnuTravel?.isVisible = travel.value != null
        mnuTravel?.setIcon(
                when (TravelService.travelling){
                        true -> R.drawable.ic_pause
                        false ->R.drawable.ic_play
                    }
            )

        if (TravelService.travelling)
            mnuAskSave?.isVisible = false
        else
            if (null != travel.value) {
                mnuAskSave?.setIcon(R.drawable.ic_save)
                if (travel.value!!.name.contentEquals(BuildConfig.TMP_NAME)) {
                    mnuAskSave?.isVisible = true
                    mnuAskSave?.isChecked = true
                } else {
                    mnuAskSave?.isVisible = false
                }
            } else {
                mnuAskSave?.setIcon(R.drawable.ic_directions)
                if (null == svm!!.travelSet.value) {
                    mnuAskSave?.isVisible = false
                } else {
                    if (svm!!.travelSet.value!!.originAddress.isNotEmpty() && svm!!.travelSet.value!!.destinationAddress.isNotEmpty()) {
                        mnuAskSave?.isVisible = true
                        mnuAskSave?.isChecked = false
                    } else {
                        mnuAskSave?.isVisible = false
                    }
                }
            }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)

        when (item.itemId) {

            R.id.opt_clear -> {
                svm!!.clear()
                if ((panel as PanelFragment).current != 0) (panel as PanelFragment).setPanel(0)
                fabState(2)
                setFabVisiblity()
            }

            R.id.opt_compass -> svm!!.optCompass.value = !svm!!.optCompass.value!!

            R.id.opt_play_stop -> startStopTravel()

            R.id.opt_direction_save -> {
                if (!item.isChecked)
                    directions()
                else
                    SaveTravelDialog().show(this.supportFragmentManager, R.id.opt_direction_save.toString())
            }
        }
        return false
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        // do nothing
    }

    override fun onPanelStateChanged(panelView: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
        if (!travelling)
            if (previousState == SlidingUpPanelLayout.PanelState.DRAGGING &&
                    newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {

                if (fab.tag == "2") (panel as PanelFragment).setPanel(0)
                setFabVisiblity()

            } else if (previousState == SlidingUpPanelLayout.PanelState.DRAGGING &&
                    newState == SlidingUpPanelLayout.PanelState.EXPANDED)
                setFabVisiblity()
    }

    private fun fabState(i: Int) {
        when (i) {
            0 -> {
                fab.setImageDrawable(getDrawable(R.drawable.ic_maker))
                fab.tag = "0"
            }
            1 -> {
                fab.setImageDrawable(getDrawable(R.drawable.ic_stat))
                fab.tag = "1"
            }
            2 -> {
                fab.setImageDrawable(getDrawable(R.drawable.ic_open))
                fab.tag = "2"
            }
        }
    }

    private fun setFabVisiblity(){
        if (TravelService.travelling) {
            fab.visibility = View.GONE
            fab.isEnabled = false
        } else {
            fab.isEnabled = true
            if (sliding_panel.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED)
                if (svm!!.name.value!!.contentEquals(BuildConfig.TMP_NAME) && null == travel.value)
                    fab.visibility = View.VISIBLE
                else
                    fab.visibility = View.GONE
            else  if (sliding_panel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED)
                if (svm!!.name.value!!.contentEquals(BuildConfig.TMP_NAME) && null == travel.value)
                    fab.visibility = View.GONE
                else
                    fab.visibility = View.VISIBLE

        }
    }

    fun fabClick(v: View) {
        if (v.id == R.id.fab) {
            fab.visibility = View.GONE
            when (v.tag) {
                "0" -> {
                    (panel as PanelFragment).setPanel(0)
                    if (svm!!.name.value!!.contentEquals(BuildConfig.TMP_NAME) && null == travel.value)
                        fabState(2)
                    else
                        fabState(1)
                }
                "1" -> {
                    (panel as PanelFragment).setPanel(1)
                    if (svm!!.name.value!!.contentEquals(BuildConfig.TMP_NAME) && null == travel.value)
                        fabState(2)
                    else
                        fabState(0)

                }
                "2" -> {
                    if ((panel as PanelFragment).current != 2)
                        (panel as PanelFragment).setPanel(2)
                }
            }
            sliding_panel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
            setFabVisiblity()
        }
    }

    // start voice service on status completion or
    // redirect to download
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_TTS_ACCESS -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    (this.application as BaseApp).travelService!!.startTTS()

                } else {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                }
            }

            CHECK_NET_ACCESS -> {
                if (!(application as BaseApp).isNetworkAvailable) {
                    Toast.makeText(this, getString(R.string.cant_net), Toast.LENGTH_SHORT).show()
                }
            }

            LOCATION_START_MOTION -> {
                if ((application as BaseApp).travelService!!.hasPos) {
                    startStopTravel()
                } else {
                    Toast.makeText(this, getString(R.string.cant_travel), Toast.LENGTH_SHORT).show()
                }
            }

            LOCATION_SET_HERE -> {
                if (!(application as BaseApp).travelService!!.hasPos) {
                    Toast.makeText(this, getString(R.string.cant_here), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_SET_HERE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.wont_here), Toast.LENGTH_SHORT).show()
                }
            }
            PERM_START_MOTION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.wont_travel), Toast.LENGTH_SHORT).show()
                } else {
                    startStopTravel()
                }
            }
        }
    }

    override fun onCancel() {
        svm!!.cancelSave()
    }

    // todo async task
    override fun onKeyListener(name: String) {
        svm!!.okSave(name)
    }

    // check voice availability
    private fun ttsChecker() {
        val checkIntent = Intent()
        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(checkIntent, CHECK_TTS_ACCESS)
    }

    // request directionsAPI
    private fun directions() {
        val mode = TravelMode.values()[svm!!.travelSet.value!!.mode]

        val op = svm!!.travelSet.value!!.originPosition.split("/")
        val origin = LatLng(op[0].toDouble(), op[1].toDouble())

        val dp = svm!!.travelSet.value!!.destinationPosition.split("/")
        val destination = LatLng(dp[0].toDouble(), dp[1].toDouble())

        val list = ArrayList<LatLng>()
        svm!!.travelSet.value!!.waypointPosition.split("#")
                .filter { it.isNotEmpty() }
                .map { it.split("/") }
                .mapTo(list) { LatLng(it[0].toDouble(), it[1].toDouble()) }

        DirectionsApi.newRequest(
                GeoApiContext().setApiKey(getString(R.string.google_maps_key))
        )
                .language(Locale.getDefault().language)
                .region(Locale.getDefault().language)
                .mode(mode)
                .origin(origin)
                .destination(destination)
                .waypoints(* Array(list.size, { i -> list[i] }))
                .setCallback(this)
    }

    // DirectionsAPI failure
    override fun onFailure(e: Throwable?) {
        Log.w(TravelMapFragment::class.java.simpleName, e?.message)
        if (!(application as BaseApp).isNetworkAvailable)
            startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), CHECK_NET_ACCESS)
    }

    // DirectionsAPI result
    override fun onResult(result: DirectionsResult?) {
        if (result!!.routes.isNotEmpty()) {

            (application as BaseApp).mAppExecutors!!.diskIO().execute({
                val tmpTravel = Travel().transform(result.routes[0])
                var d = 0L
                var t = 0L
                result.routes[0].legs.forEach {
                    d += it.distance.inMeters
                    t += it.duration.inSeconds
                }

                (application as BaseApp).mAppExecutors!!.mainThread().execute({
                    travel.value = tmpTravel
                    svm!!.travelSet.value!!.distance = if (d > 999) "${d / 1000} km" else "$d m"
                    svm!!.travelSet.value!!.estimatedTime = DateConverter.compoundDuration(t)
                    svm!!.update(svm!!.travelSet.value!!)
                })
            })
        }

    }

    // return true if start travelling, false otherwise
    internal fun startStopTravel() {
        if (TravelService.travelling) { // stop
            (application as BaseApp).travelService!!.stopMotion()
            svm!!.travelSet.value!!.max += 1

            if (svm!!.travelSet.value!!.name.contentEquals(BuildConfig.TMP_NAME))
                SaveTravelDialog().show(supportFragmentManager, R.string.app_name.toString()) // new travel
            else {
                svm!!.update(svm!!.travelSet.value!!) // existing one
                svm!!.createStatRecord()
            }

            invalidateOptionsMenu()
        } else
            if ((application as BaseApp).travelService!!.startMotion(svm!!.travelSet.value!!.max + 1)) {
                invalidateOptionsMenu()
                return
            } else {
                // check perm
                if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                //  missing feature
                    startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_START_MOTION)
                else
                // missing perm
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            PERM_START_MOTION)
            }

    }

    fun swapFromTo(v: View) {
        if (v.id == R.id.search_swap) {
            val tmpAd = svm!!.travelSet.value!!.originAddress
            val tmpPo = svm!!.travelSet.value!!.originPosition

            svm!!.travelSet.value!!.originAddress = svm!!.travelSet.value!!.destinationAddress
            svm!!.travelSet.value!!.originPosition = svm!!.travelSet.value!!.destinationPosition

            svm!!.travelSet.value!!.destinationAddress = tmpAd
            svm!!.travelSet.value!!.destinationPosition = tmpPo

            svm!!.update(svm!!.travelSet.value!!)
        }
    }

    fun setRun(name: String, iter: Int) {
        svm!!.getStampRecords(name, iter)
    }
}