package org.darenom.leadme.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.*
import org.darenom.leadme.BaseApp
import org.darenom.leadme.db.AppDatabase
import org.darenom.leadme.db.entities.TravelStampEntity
import org.darenom.leadme.db.entities.TravelStatEntity

/**
 * Created by admadmin on 18/03/2018.
 */

class TravelStatViewModel(application: Application, name: String)
    : AndroidViewModel(application) {

    var observableTravelStat: MediatorLiveData<List<TravelStatEntity>> = MediatorLiveData()

    init {

        observableTravelStat.addSource<List<TravelStatEntity>> (
                (application as BaseApp).database!!.travelStatDao()
                        .getByName(name), observableTravelStat::setValue)
    }


    class Factory(private val mApplication: Application, private val name: String)
        : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TravelStatViewModel(mApplication, name) as T
        }
    }
}
