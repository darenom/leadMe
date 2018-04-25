package org.darenom.leadme.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.R
import org.darenom.leadme.db.model.TravelSet
import org.darenom.leadme.ui.callback.TravelSetClickCallback
import org.darenom.leadme.ui.fragment.TravelSetFragment
import org.darenom.leadme.ui.fragment.TravelStatFragment

/**
 * Created by adm on 16/02/2018.
 */

class StatisticsActivity : Fragment() {

    internal var name = BuildConfig.TMP_NAME

    private val mTravelSetClickCallback = object : TravelSetClickCallback {
        override fun loadInMap(travelSet: TravelSet) {
            // do nothing, is hidden
        }

        override fun onClick(travelSet: TravelSet) {
            // load on map
        }
    }

    private lateinit var travelSetFragment: TravelSetFragment

    private lateinit var travelStatFragment: TravelStatFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        retainInstance = true

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val v = inflater.inflate(R.layout.activity_statistics, container, false)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        travelSetFragment = TravelSetFragment.forTravelSet(name)
        childFragmentManager
                .beginTransaction()
                .add(R.id.fragment_set,
                        travelSetFragment, name).commit()

        travelStatFragment = TravelStatFragment.forTravelSet(name)
        childFragmentManager
                .beginTransaction()
                .add(R.id.fragment_stat,
                        travelStatFragment, name).commit()
    }

    private var setOk: Boolean = false
    private var statOk: Boolean = false

    override fun onAttachFragment(childFragment: Fragment?) {
        super.onAttachFragment(childFragment)
        when (childFragment!!.id){
            R.id.fragment_set -> setOk = true
            R.id.fragment_stat -> statOk = true
        }
    }

    override fun onDetach() {
        super.onDetach()
        setOk = false
        statOk = false
    }

    fun swapTo(name: String){
        this.name = name
        if (setOk) {
            travelSetFragment = TravelSetFragment.forTravelSet(name)
            childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_set,
                            travelSetFragment, name).commit()
        }
        if (statOk){
        travelStatFragment = TravelStatFragment.forTravelSet(name)
            childFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_stat,
                            travelStatFragment, name).commit()
        }

    }

    companion object {
        private var fragment: StatisticsActivity? = null
        fun getInstance(): StatisticsActivity {
            if (null == fragment) {
                fragment = StatisticsActivity()
            }
            return fragment!!
        }
    }
}