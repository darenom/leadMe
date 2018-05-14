package org.darenom.leadme

import android.app.Application
import android.app.Service
import android.app.Service.START_FLAG_RETRY
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import org.darenom.leadme.db.AppDatabase
import org.darenom.leadme.service.TravelService
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*


/**
 * Created by adm on 06/02/2018.
 */

class BaseApp : Application() {

    var splash: Splash? = null
    var mAppExecutors: AppExecutors? = null
    val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    val database: AppDatabase
        get() = AppDatabase.getInstance(this, mAppExecutors!!)

    var travelService: TravelService? = null
    private var travelCnx: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.e("BaseApp", "Connected to travelService")
            val binder = service as TravelService.TravelServiceBinder
            travelService = binder.service
            travelService!!.onStartCommand(null, Service.START_FLAG_RETRY, 10)
            moveOn()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.e("BaseApp", "Disconnected from travelService")
            travelService = null
        }
    }

    fun moveOn() {
        splash?.loader?.progress = 30
        startActivity(Intent(applicationContext, TravelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onCreate() {
        super.onCreate()

        mAppExecutors = AppExecutors()

        splash?.loader?.progress = 20
        bindService(Intent(this, TravelService::class.java),
                travelCnx, Context.BIND_AUTO_CREATE)

    }

}