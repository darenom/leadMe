package org.darenom.leadme

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_splash.*
import org.darenom.leadme.db.AppDatabase
import org.darenom.leadme.service.TravelService


/**
 * Created by adm on 06/02/2018.
 */

class BaseApp : Application() {

    var mActivity: Activity? = null
    var mAppExecutors: AppExecutors? = null
    var database: AppDatabase? = null
    var travelService: TravelService? = null
    val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

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

    internal fun moveOn() {
        startActivity(Intent(applicationContext, TravelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onCreate() {
        super.onCreate()

        mAppExecutors = AppExecutors()
        database = AppDatabase.getInstance(this, mAppExecutors!!)

        bindService(Intent(this, TravelService::class.java),
                travelCnx, Context.BIND_AUTO_CREATE)

    }
}