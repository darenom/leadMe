package org.darenom.leadme.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.darenom.leadme.BaseApp
import org.darenom.leadme.db.AppDatabase
import org.darenom.leadme.db.entities.TravelSetEntity


/**
 * Created by adm on 12/02/2018.
 */


class TravelSetViewModel(application: Application, database: AppDatabase, name: String)
    : AndroidViewModel(application) {

    var observableTravelSet: MediatorLiveData<TravelSetEntity> = MediatorLiveData()

    init {
        observableTravelSet.addSource<TravelSetEntity>(database.travelSetDao().getByName(name), { observableTravelSet.setValue(it) })
    }

    class Factory(private val mApplication: Application, private val name: String) : ViewModelProvider.NewInstanceFactory() {

        private val mRepository: AppDatabase = (mApplication as BaseApp).database
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TravelSetViewModel(mApplication, mRepository, name) as T
        }
    }
}
