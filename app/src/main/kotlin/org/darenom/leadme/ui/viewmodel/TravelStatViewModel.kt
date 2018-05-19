package org.darenom.leadme.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.darenom.leadme.room.AppDatabase
import org.darenom.leadme.room.entities.TravelStatEntity

/**
 * Created by admadmin on 18/03/2018.
 */

class TravelStatViewModel(application: Application, db: AppDatabase, name: String)
    : AndroidViewModel(application) {

    var observableTravelStat: MediatorLiveData<List<TravelStatEntity>> = MediatorLiveData()

    init {

        observableTravelStat.addSource<List<TravelStatEntity>>(
                db.travelStatDao()
                        .getByName(name), observableTravelStat::setValue)
    }


    class Factory(
            private val mApplication: Application,
            private val db: AppDatabase,
            private val name: String
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TravelStatViewModel(mApplication, db, name) as T
        }
    }
}
