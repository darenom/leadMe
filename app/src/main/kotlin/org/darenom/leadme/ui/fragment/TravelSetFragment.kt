package org.darenom.leadme.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.darenom.leadme.AppExecutors
import org.darenom.leadme.R
import org.darenom.leadme.databinding.FragmentTravelsetBinding
import org.darenom.leadme.room.AppDatabase
import org.darenom.leadme.room.entities.TravelSetEntity
import org.darenom.leadme.ui.viewmodel.TravelSetViewModel


/**
 * Created by adm on 16/02/2018.
 */

class TravelSetFragment : Fragment() {

    private var mBinding: FragmentTravelsetBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_travelset, container, false)
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val factory = TravelSetViewModel.Factory(
                activity!!.application,
                AppDatabase.getInstance(activity!!.applicationContext, AppExecutors.getInstance()),
                arguments!!.getString(KEY_TRAVELSET_NAME))
        val model = ViewModelProviders.of(this, factory).get(TravelSetViewModel::class.java)
        subscribeToModel(model)
    }

    private fun subscribeToModel(model: TravelSetViewModel) {

        model.observableTravelSet.observe(this, Observer<TravelSetEntity> { it ->
            if (null != it) {
                mBinding!!.travelSet = it
                mBinding!!.imgMode = ContextCompat.getDrawable(context!!,
                        context!!.resources.getIdentifier(
                                context!!.resources.getStringArray(R.array.travel_mode_drw)[it.mode],
                                "drawable",
                                context!!.packageName))
            }
        })

    }

    companion object {

        const val KEY_TRAVELSET_NAME = "travelset_name"

        /** Creates travelSet fragment for specific travelSet ID  */
        fun forTravelSet(name: String): TravelSetFragment {
            val fragment = TravelSetFragment()
            val args = Bundle()
            args.putString(KEY_TRAVELSET_NAME, name)
            fragment.arguments = args
            return fragment
        }
    }
}
