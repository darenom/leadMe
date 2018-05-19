package org.darenom.leadme

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * Created by adm on 06/02/2018.
 */

class AppExecutors private constructor(private val mDiskIO: Executor, private val mNetworkIO: Executor, private val mMainThread: Executor) {

    constructor() : this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(3),
            MainThreadExecutor())

    fun diskIO(): Executor {
        return mDiskIO
    }

    fun networkIO(): Executor {
        return mNetworkIO
    }

    fun mainThread(): Executor {
        return mMainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    companion object {
        private var sInstance: AppExecutors? = null
        fun getInstance(): AppExecutors {
            if (sInstance == null) {
                synchronized(AppExecutors::class.java) {
                    if (sInstance == null) {
                        sInstance = AppExecutors()
                    }
                }
            }
            return sInstance!!
        }
    }
}
