package org.darenom.leadme.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.maps.model.LatLng
import org.darenom.leadme.AppExecutors
import org.darenom.leadme.BaseApp
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.db.AppDatabase
import org.darenom.leadme.db.DateConverter
import org.darenom.leadme.db.entities.TravelSetEntity
import org.darenom.leadme.db.entities.TravelStatEntity
import org.darenom.leadme.model.Travel
import org.darenom.leadme.service.TravelService.Companion.travel
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt


/**
 * Created by adm on 01/02/2018.
 */

class SharedViewModel(app: Application) : AndroidViewModel(app) {

    private val cTag = this.javaClass.simpleName

    private var settings: SharedPreferences? = null

    val travelSetList: MediatorLiveData<List<TravelSetEntity>> = MediatorLiveData()
    val travelSet: MediatorLiveData<TravelSetEntity> = MediatorLiveData()
    private var dbTravelSet: LiveData<TravelSetEntity>? = null

    private var currentMax: Int = 0
    var canCancel: Boolean = false
        get() = if (null != travelSet.value) {
            travelSet.value!!.originAddress.isNotEmpty() ||
                    travelSet.value!!.destinationAddress.isNotEmpty() ||
                    travelSet.value!!.waypointAddress.isNotEmpty()
        } else false

    // region SharedPrefs
    var name = object : MutableLiveData<String>() {
        override fun setValue(value: String) {
            super.setValue(value)
            val editor = settings!!.edit()
            editor.putString(OPTION_NAME_KEY, value)
            editor.apply()
        }
    }
    var optCompass = object : MutableLiveData<Boolean>() {
        override fun setValue(value: Boolean) {
            super.setValue(value)
            val editor = settings!!.edit()
            editor.putBoolean(OPTIONS_COMPASS_KEY, value)
            editor.apply()
        }
    }
    var tmode = object : MutableLiveData<Int>() {
        override fun setValue(value: Int) {
            super.setValue(value)
            val editor = settings!!.edit()
            editor.putInt(OPTION_MODE_KEY, value)
            editor.apply()
        }
    }
    // endregion



    init {


        settings = getApplication<BaseApp>()
                .getSharedPreferences(getApplication<BaseApp>().packageName, 0)

        optCompass.value = settings!!.getBoolean(OPTIONS_COMPASS_KEY, false)
        tmode.value = settings!!.getInt(OPTION_MODE_KEY, 0)
        name.value = settings!!.getString(OPTION_NAME_KEY, BuildConfig.TMP_NAME)

        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute({
            val dbTravelSetList = getApplication<BaseApp>().database!!.travelSetDao().getAll()
            getApplication<BaseApp>().mAppExecutors!!.mainThread().execute({
                travelSetList.addSource(dbTravelSetList, travelSetList::setValue)

            })
        })
    }

    // reset to empty
    fun clear() {

        currentMax = 0
        if (travelSet.value!!.name.contentEquals(BuildConfig.TMP_NAME)) {
            delete(BuildConfig.TMP_NAME)
            travel.value = null
            travelSet.value = TravelSetEntity()
            update(travelSet.value!!)
        } else
            name.value = BuildConfig.TMP_NAME

    }

    // travelSet cursor
    fun monitor(name: String) {
        if (travelSet.hasActiveObservers())
            if (null != dbTravelSet)
                travelSet.removeSource(dbTravelSet!!)

        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute({
            dbTravelSet = getApplication<BaseApp>().database!!.travelSetDao().getByName(name)

            getApplication<BaseApp>().mAppExecutors!!.mainThread().execute({
                try {
                    travelSet.addSource(dbTravelSet!!, travelSet::setValue)
                } catch (e: IllegalArgumentException) {
                    Log.e(cTag, "monitor: ${e.message}")
                }
            })
        })
    }

    // handling edit text inputs
    fun setPoint(a: PointType, s: String) {

        travelSet.value ?: return

        val hasChanged = when (a) {
            PointType.ORIGIN -> !s.contentEquals(travelSet.value!!.originAddress)
            PointType.DESTINATION -> !s.contentEquals(travelSet.value!!.destinationAddress)
            PointType.WAYPOINT -> !s.contentEquals(travelSet.value!!.waypointAddress)
        }

        if (hasChanged) {
            getApplication<BaseApp>().mAppExecutors!!.networkIO().execute({
                val t = isAddress(s)
                getApplication<BaseApp>().mAppExecutors!!.mainThread().execute({
                    when (t[1]) {
                        "" -> {
                            when (a) {
                                PointType.ORIGIN -> travelSet.value!!.originAddress = t[0]
                                PointType.DESTINATION -> travelSet.value!!.destinationAddress = t[0]
                                PointType.WAYPOINT -> travelSet.value!!.waypointAddress += "${t[0]}#"
                            }
                        }
                        else -> {
                            when (a) {
                                PointType.ORIGIN -> {
                                    travelSet.value!!.originAddress = t[0]
                                    travelSet.value!!.originPosition = t[1]
                                }
                                PointType.DESTINATION -> {
                                    travelSet.value!!.destinationAddress = t[0]
                                    travelSet.value!!.destinationPosition = t[1]
                                }
                                PointType.WAYPOINT -> {
                                    travelSet.value!!.waypointAddress += "${t[0]}#"
                                    travelSet.value!!.waypointPosition += "${t[1]}#"
                                }
                            }
                            update(travelSet.value!!)
                        }
                    }
                })
            })
        }
    }

    // handling map click inputs
    fun setLocationAs(a: PointType?, pos: LatLng): Boolean {

        val b: PointType? = a ?: when {
            travelSet.value!!.originAddress.isEmpty() -> PointType.ORIGIN
            travelSet.value!!.destinationAddress.isEmpty() -> PointType.DESTINATION
            travelSet.value!!.waypointAddress.isEmpty() -> PointType.WAYPOINT
            else -> PointType.WAYPOINT
        }

        when (b) {
            PointType.ORIGIN -> travelSet.value!!.originPosition = "${pos.lat}/${pos.lng}"
            PointType.DESTINATION -> travelSet.value!!.destinationPosition = "${pos.lat}/${pos.lng}"
            PointType.WAYPOINT -> {
                if (currentMax >= SharedViewModel.wayMax)
                    return false
                travelSet.value!!.waypointPosition += "${pos.lat}/${pos.lng}#"
                currentMax++
            }
        }

        getApplication<BaseApp>().mAppExecutors!!.networkIO().execute({
            val adr = getAddress(pos.lat, pos.lng)

            getApplication<BaseApp>().mAppExecutors!!.mainThread().execute({
                when (adr) {
                    "" -> {
                        when (b) {
                            PointType.ORIGIN -> travelSet.value!!.originAddress = travelSet.value!!.originPosition
                            PointType.DESTINATION -> travelSet.value!!.destinationAddress = travelSet.value!!.destinationPosition
                            PointType.WAYPOINT -> travelSet.value!!.waypointAddress += "${pos.lat}/${pos.lng}#"
                        }
                    }
                    else -> {
                        when (b) {
                            PointType.ORIGIN -> travelSet.value!!.originAddress = adr
                            PointType.DESTINATION -> travelSet.value!!.destinationAddress = adr
                            PointType.WAYPOINT -> travelSet.value!!.waypointAddress += "$adr#"
                        }
                    }
                }
                update(travelSet.value!!)

            })
        })
        return true
    }

    fun removePoint(address: String) {
        when (address) {
            travelSet.value!!.originAddress -> {
                travelSet.value!!.originAddress = ""
                travelSet.value!!.originPosition = ""
            }
            travelSet.value!!.destinationAddress -> {
                travelSet.value!!.destinationAddress = ""
                travelSet.value!!.destinationPosition = ""
            }
            else -> {
                val adr = travelSet.value!!.waypointAddress.split("#")
                val pos = travelSet.value!!.waypointPosition.split("#")
                var ad = ""
                var po = ""
                adr.forEach {
                    if (it.isNotEmpty()) {
                        if (!address.contentEquals(it)) {
                            ad += "$it#"
                            po += "${pos[adr.indexOf(it)]}#"
                        }
                    }
                }
                travelSet.value!!.waypointAddress = ad
                travelSet.value!!.waypointPosition = po
            }
        }
        update(travelSet.value!!)
    }

    // handling saving
    fun okSave(name: String) {
        travel.value!!.name = name
        write(travel.value!!)             // save named travel

        travelSet.value!!.name = name
        update(travelSet.value!!)   // save named travelset
        records(name)                     // move tmp to named

        delete(BuildConfig.TMP_NAME)      // delete tmp file

        travelSet.value = TravelSetEntity() // reset tmp
        update(travelSet.value!!)   // save named travelset

        this.name.value = name                 // triggers loading and populate
    }

    fun cancelSave() {
        wipe(BuildConfig.TMP_NAME)
        travelSet.value!!.max = 0
    }

    // region db
    internal fun update(travelSetEntity: TravelSetEntity) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            getApplication<BaseApp>().database!!.travelSetDao().insert(travelSetEntity)
        }

    }

    internal fun createStatRecord() {
        val t = TravelStatEntity()
        t.name = travelSet.value!!.name
        t.iter = travelSet.value!!.max
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            var d = 0.0
            val tmpLoc = Location("tmp")
            val u = getApplication<BaseApp>().database!!.travelStampDao().get(t.name, t.iter)
            t.timestart = DateConverter.toDate(u[0].time!!).toString().split("GMT")[0]
            t.timeend = DateConverter.toDate(u[u.lastIndex].time!!).toString().split("GMT")[0]
            t.timed = DateConverter.compoundDuration((u[u.lastIndex].time!! - u[0].time!!) / 1000L)
            u.forEachIndexed { index, it ->
                if (index > 0) {
                    val p = Location("po")
                    p.latitude = it.lat!!
                    p.longitude = it.lng!!
                    d += p.distanceTo(tmpLoc)
                }
                tmpLoc.latitude = it.lat!!
                tmpLoc.longitude = it.lng!!

            }
            t.distance = if (d < 1000) "${d.roundToInt()} m" else "${(d / 1000).toFloat()} km"
            t.avgSpeed = "${((d / 1000) / ((u[u.lastIndex].time!! - u[0].time!!) / 3600)).toFloat()} km/h"

            getApplication<BaseApp>().database!!.travelStatDao().insert(t)
        }
    }

    private fun wipe(name: String) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            getApplication<BaseApp>().database!!.travelStampDao().wipe(name)
        }
    }

    private fun records(name: String) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            getApplication<BaseApp>().database!!.travelStampDao().updateSet(name)
        }
    }

    val travelRun = MutableLiveData<List<com.google.android.gms.maps.model.LatLng>>()
    val travelAlt = MutableLiveData<HashMap<Long, Double>>()

    internal fun getStampRecords(name: String, iter: Int){
        val run = ArrayList<com.google.android.gms.maps.model.LatLng>()
        val alt = HashMap<Long, Double>()
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            val t= getApplication<BaseApp>().database!!.travelStampDao().get(name, iter)
                    t.forEach { it ->
                        alt[it.time!!] = it.altitude!!
                        run.add(com.google.android.gms.maps.model.LatLng(it.lat!!, it.lng!!)) }
            travelRun.postValue(run.toList())
            travelAlt.postValue(alt)
        }
    }

    // endregion

    // region Files
    fun delete(name: String) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute {
            val file1 = File("${getApplication<BaseApp>().filesDir.absolutePath}/$name${BuildConfig.RTE}")
            if (file1.exists())
                file1.delete()

            val file2 = File("${getApplication<BaseApp>().filesDir.absolutePath}/$name${BuildConfig.PNG}")
            if (file2.exists())
                file2.delete()
        }
    }

    fun write(bmp: Bitmap) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute({
            val file = File("${getApplication<BaseApp>().filesDir.absolutePath}/${travel.value!!.name}${BuildConfig.PNG}")
            // avoid unnecessary
            if (!file.exists()) {
                val out: FileOutputStream
                try {
                    out = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.close()
                } catch (e: Exception) {
                    Log.e(cTag, "write: ${e.message}")
                } catch (e: IOException) {
                    Log.e(cTag, "write: ${e.message}")
                }
            }
        })
    }

    fun write(travel: Travel) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute({
            val file = File("${getApplication<BaseApp>().filesDir.absolutePath}/${travel.name}${BuildConfig.RTE}")
            // avoid unnecessary
            if (!file.exists()) {
                val g = Gson()
                val fileStream: FileOutputStream
                val writer: OutputStreamWriter
                try {
                    val s = g.toJson(travel)
                    fileStream = FileOutputStream(file)
                    writer = OutputStreamWriter(fileStream, StandardCharsets.UTF_8)
                    writer.write(s)
                    writer.close()
                    fileStream.close()
                } catch (e: FileNotFoundException) {
                    Log.e(cTag, "write: ${e.message}")
                } catch (e: IOException) {
                    Log.e(cTag, "write: ${e.message}")
                }
            }
        })
    }

    fun read(name: String) {
        getApplication<BaseApp>().mAppExecutors!!.diskIO().execute({
            var tmpTravel: Travel? = null
            val inputStream: FileInputStream
            val inputStreamReader: InputStreamReader
            val bufferedReader: BufferedReader
            try {
                inputStream = getApplication<BaseApp>().openFileInput("$name${BuildConfig.RTE}")
                inputStreamReader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
                bufferedReader = BufferedReader(inputStreamReader)
                val sb = StringBuilder()
                var line = bufferedReader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = bufferedReader.readLine()
                }
                val g = Gson()
                tmpTravel = g.fromJson(sb.toString(), Travel::class.java)
                inputStreamReader.close()
                inputStream.close()
            } catch (e: FileNotFoundException) {
                if (!name.contentEquals(BuildConfig.TMP_NAME))
                    Log.e(cTag, "read: ${e.message}")
            } catch (e: IOException) {
                Log.e(cTag, "read: ${e.message}")
            }
            travel.postValue(tmpTravel)
        })
    }
    // endregion

    // region Geocoder
    /**
     *  retrieve address and location
     *
     * @param value - inputed string location
     * @return [string, string]
     * [0] - geocoder resolved address
     * [1] - latLng [0]
     *
     * both empty -> failed to resolve
     * [1] == emptyLatLng -> failed to connect
     */

    private fun isAddress(value: String): Array<String> {
        val out = arrayOf("", "")
        val addresses: List<Address>
        try {
            val geocoder = Geocoder(getApplication<BaseApp>().applicationContext, Locale.getDefault())
            addresses = geocoder.getFromLocationName(value, 3)
            if (addresses.isEmpty() || addresses[0].getAddressLine(0).isEmpty()) {
                return out
            }
            out[1] = "${addresses[0].latitude}/${addresses[0].longitude}"
            out[0] = addresses[0].getAddressLine(0).toString()
        } catch (e: IOException) {
            out[1] = emptyLatLng
            out[0] = value
        }
        return out
    }

    /**
     *  retrieve address from location
     *
     * @param lat - latitude
     * @param lng - longitude
     * @return geocoder resolved address
     */
    fun getAddress(lat: Double, lng: Double): String {
        var str = ""
        try {
            val geocoder = Geocoder(getApplication<BaseApp>().applicationContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses.isNotEmpty()) {
                if (addresses[0].maxAddressLineIndex >= 0) {
                    for (i in 0..addresses[0].maxAddressLineIndex)
                        str += addresses[0].getAddressLine(i)
                }
            }
            return str
        } catch (e: IOException) {
            Log.w("Geocoder", e.message)
        }
        return str
    }
    // endregion

    companion object {
        enum class PointType { ORIGIN, DESTINATION, WAYPOINT }

        const val emptyLatLng = "0.0/0.0"
        private const val wayMax = 17
        const val OPTIONS_COMPASS_KEY = "OptionCompassKey"
        const val OPTION_MODE_KEY = "OptionModeKey"
        const val OPTION_NAME_KEY = "OptionNameKey"
    }

}