package org.darenom.leadme.ui

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
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
import kotlinx.android.synthetic.main.activity_travel.*
import kotlinx.android.synthetic.main.layout_view_compass.*
import org.darenom.leadme.BaseApp
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.R
import org.darenom.leadme.db.DateConverter
import org.darenom.leadme.db.entities.TravelSetEntity
import org.darenom.leadme.model.Travel
import org.darenom.leadme.service.TravelService
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.ui.fragment.MakerFragment
import org.darenom.leadme.ui.fragment.TravelMapFragment
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*


class TravelActivity : AppCompatActivity(), PendingResult.Callback<DirectionsResult>, SaveTravelDialog.SaveTravelDialogListener {

    companion object {
        const val CHECK_TTS_ACCESS = 40
        const val CHECK_NET_ACCESS = 41
        const val PERM_SET_HERE = 101
        const val PERM_START_MOTION = 102
        const val LOCATION_START_MOTION = 103
        const val LOCATION_SET_HERE = 104
    }

    private var svm: SharedViewModel? = null

    private val mMapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
            // point to north
                TravelService.ORIENTATION_CHANGED -> {
                    (map as TravelMapFragment).layout_compass!!.onOrientationChanged(intent.getFloatArrayExtra(TravelService.ORIENTATION_CHANGED))
                }
            // point to closest
                TravelService.DIRECTION_CHANGED -> {
                    (map as TravelMapFragment).mBinding!!.showDirection = true
                    (map as TravelMapFragment).layout_compass!!.onDirectionChanged(intent.getFloatExtra(TravelService.DIRECTION_CHANGED, 0f))
                }
                TravelService.ARRIVED -> {
                    if (TravelService.travelling)
                        startStopTravel(null)
                }
                TravelService.MY_WAY_BACK -> {
                    (map as TravelMapFragment).mBinding!!.showDirection = false
                }
                TravelService.SEGMENT_CHANGED -> {
                    val lats = intent.getParcelableArrayListExtra<com.google.android.gms.maps.model.LatLng>(TravelService.SEGMENT_SIDES)
                    val length = intent.getFloatExtra(TravelService.SEGMENT_LENGTH, 0f)
                    (map as TravelMapFragment).redraw(lats, length)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        svm = ViewModelProviders.of(this).get(SharedViewModel::class.java)

        subscribeUI()
        ttsChecker()
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(this).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.ORIENTATION_CHANGED))
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.DIRECTION_CHANGED))
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.ARRIVED))
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.MY_WAY_BACK))
        LocalBroadcastManager.getInstance(this).registerReceiver(mMapReceiver,
                IntentFilter(TravelService.SEGMENT_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMapReceiver)
    }

    private fun subscribeUI() {
        svm!!.name.observe(this, Observer { it ->
            if (null != it) {

                if (it.contentEquals(BuildConfig.TMP_NAME))
                    supportActionBar?.setTitle(R.string.app_name)
                else {
                    supportActionBar?.title = it
                    if (null != (map as TravelMapFragment).mBinding)
                        (map as TravelMapFragment).mBinding!!.showProgress = true
                }

                // todo async task
                travel.value = null
                svm!!.read(it)
                svm!!.monitor(it)
            }
        })

        svm!!.optCompass.observe(this, Observer {
            if (null != it) {
                (map as TravelMapFragment).mBinding!!.showCompass = it
                (application as BaseApp).travelService!!.enableCompass(it, 0)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_appbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)

        // visibility

        val mnuCompass = menu?.findItem(R.id.opt_compass)
        val mnuClear = menu?.findItem(R.id.opt_clear)
        val mnuTravel = menu?.findItem(R.id.opt_play_stop)
        val mnuAskSave = menu?.findItem(R.id.opt_direction_save)

        mnuCompass?.isVisible = (application as BaseApp).travelService!!.hasCompass

        mnuTravel?.isVisible = travel.value != null
        if (TravelService.travelling) {
            mnuClear?.isVisible = false
            mnuTravel?.setIcon(R.drawable.ic_pause)
        } else {
            mnuClear?.isVisible = svm!!.canCancel
            mnuTravel?.setIcon(R.drawable.ic_play)
        }

        // values
        mnuCompass?.isChecked = svm!!.optCompass.value!!

        if (TravelService.travelling) mnuClear?.isVisible = false else mnuClear?.isVisible = svm!!.canCancel

        if (null != travel.value) {
            mnuAskSave?.setIcon(R.drawable.ic_save)
            if (travel.value!!.name.contentEquals(BuildConfig.TMP_NAME)) {
                mnuAskSave?.isVisible = true
                mnuAskSave?.isChecked = true
            } else {
                (maker as MakerFragment).enable(false)
                mnuAskSave?.isVisible = false
            }
        } else {
            mnuAskSave?.setIcon(R.drawable.ic_directions)
            if (null == svm!!.travelSet.value) {
                (maker as MakerFragment).enable(true)
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

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)

        when (item.itemId) {

            R.id.opt_clear -> {
                travel.value = null
                (map as TravelMapFragment).clear()
                svm!!.clear()
                (maker as MakerFragment).enable(true)
            }

            R.id.opt_compass -> {
                svm!!.optCompass.value = !svm!!.optCompass.value!!
            }

            R.id.opt_direction_save -> {
                if (!item.isChecked)
                    directions()
                else
                    SaveTravelDialog().show(supportFragmentManager, R.id.opt_direction_save.toString())
            }
        }
        return false
    }

    // start voice service on status completion or
    // redirect to download
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_TTS_ACCESS -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    (application as BaseApp).travelService!!.startTTS()

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
                    startStopTravel(null)
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
                    startStopTravel(null)
                }
            }
        }
    }

    override fun onCancel() {
        svm!!.wipe(BuildConfig.TMP_NAME)
        svm!!.travelSet.value!!.max = 0
    }

    // todo async task
    override fun onKeyListener(name: String) {
        // delete tmp
        svm!!.delete(BuildConfig.TMP_NAME)
        travel.value!!.name = name
        svm!!.travelSet.value!!.name = name
        svm!!.update(svm!!.travelSet.value!!)
        svm!!.records(name)
        svm!!.write(travel.value!!)

        svm!!.travelSet.value = TravelSetEntity()
        svm!!.update(svm!!.travelSet.value!!)

        // switch monitoring
        svm!!.monitor(name)

    }

    // check voice availability
    private fun ttsChecker() {
        val checkIntent = Intent()
        checkIntent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        startActivityForResult(checkIntent, CHECK_TTS_ACCESS)
    }

    // request directionsAPI
    private fun directions() {

        (map as TravelMapFragment).mBinding!!.showProgress = true

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
            startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), TravelActivity.CHECK_NET_ACCESS)
    }

    // DirectionsAPI result
    override fun onResult(result: DirectionsResult?) {
        if (result!!.routes.isNotEmpty()) {

            (application as BaseApp).mAppExecutors!!.diskIO().execute({
                val tmpTravel = Travel().transform(result.routes[0])
                var d = 0
                var t = 0
                result.routes[0].legs.forEach {
                    d += it.distance.inMeters.toInt()
                    t += it.duration.inSeconds.toInt()
                }

                (application as BaseApp).mAppExecutors!!.mainThread().execute({
                    travel.value = tmpTravel
                    svm!!.travelSet.value!!.distance = if (d > 999) "${d / 1000} km" else "$d m"
                    svm!!.travelSet.value!!.estimatedTime = DateConverter.compoundDuration(t)
                    svm!!.update(svm!!.travelSet.value!!)


                })
            })
        } else {
            (map as TravelMapFragment).mBinding!!.showProgress = false
        }

    }

    fun startStopTravel(m: MenuItem?) {
        (map as TravelMapFragment)?.mBinding!!.showDirection = false
        if (!TravelService.travelling) {
            // attempt start
            if (!(application as BaseApp).travelService!!.startMotion(svm!!.travelSet.value!!.max + 1))
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
        } else {
            // stop
            (application as BaseApp).travelService!!.stopMotion()
            svm!!.travelSet.value!!.max += 1

            if (svm!!.travelSet.value!!.name.contentEquals(BuildConfig.TMP_NAME))
                SaveTravelDialog().show(supportFragmentManager, R.string.app_name.toString())
            else
                svm!!.update(svm!!.travelSet.value!!)


        }
        invalidateOptionsMenu()
    }

    fun swapFromTo(v: View) {

        val tmpAd = svm!!.travelSet.value!!.originAddress
        val tmpPo = svm!!.travelSet.value!!.originPosition

        svm!!.travelSet.value!!.originAddress = svm!!.travelSet.value!!.destinationAddress
        svm!!.travelSet.value!!.originPosition = svm!!.travelSet.value!!.destinationPosition

        svm!!.travelSet.value!!.destinationAddress = tmpAd
        svm!!.travelSet.value!!.destinationPosition = tmpPo

        svm!!.update(svm!!.travelSet.value!!)

    }
}