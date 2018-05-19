package org.darenom.leadme.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.darenom.leadme.room.AppDatabase
import org.darenom.leadme.room.entities.TravelSetEntity


/**
 * Created by adm on 12/02/2018.
 */


class TravelSetViewModel(application: Application, db: AppDatabase, name: String)
    : AndroidViewModel(application) {

    var observableTravelSet: MediatorLiveData<TravelSetEntity> = MediatorLiveData()

    init {
        observableTravelSet.addSource<TravelSetEntity>(
                db.travelSetDao()
                        .getByName(name), { observableTravelSet.setValue(it) })
    }

    class Factory(
            private val mApplication: Application,
            private val db: AppDatabase,
            private val name: String
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TravelSetViewModel(mApplication, db, name) as T
        }
    }
}
