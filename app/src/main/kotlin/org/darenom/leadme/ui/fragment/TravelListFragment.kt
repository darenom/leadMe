package org.darenom.leadme.ui.fragment

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v4.app.Fragment
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import org.darenom.leadme.R
import org.darenom.leadme.databinding.FragmentTravelsetListBinding
import org.darenom.leadme.db.entities.TravelSetEntity
import org.darenom.leadme.db.model.TravelSet
import org.darenom.leadme.ui.StatisticsActivity
import org.darenom.leadme.ui.adapter.TravelSetAdapter
import org.darenom.leadme.ui.callback.TravelSetClickCallback
import org.darenom.leadme.ui.fragment.TravelSetFragment.Companion.KEY_TRAVELSET_NAME
import org.darenom.leadme.ui.viewmodel.SharedViewModel

/**
 * Created by adm on 01/02/2018.
 *
 * List of saved travels with basic info and a shortcut to load in map and maker fragments
 * clicking an item start the resume activity with detailed runs
 */

class TravelListFragment : Fragment() {

    private var svm: SharedViewModel? = null
    private var mTravelSetAdapter: TravelSetAdapter? = null
    private var mBinding: FragmentTravelsetListBinding? = null
    private val mTravelSetClickCallback = object : TravelSetClickCallback {
        override fun loadInMap(travelSet: TravelSet) {
            svm!!.name.value = travelSet.name
           // activity!!.bottombar.selectedItemId = R.id.action_map
        }

        override fun onClick(travelSet: TravelSet) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                val intent = Intent(activity, StatisticsActivity::class.java)
                        .putExtra(KEY_TRAVELSET_NAME, travelSet.name)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true   // onConfigChange retain
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.opt_compass)?.isVisible = false
        menu?.findItem(R.id.opt_play_stop)?.isVisible = false
        menu?.findItem(R.id.opt_direction_save)?.isVisible = false
        menu?.findItem(R.id.opt_clear)?.isVisible = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_travelset_list, container, false)
        mTravelSetAdapter = TravelSetAdapter(mTravelSetClickCallback)
        mBinding!!.travelsetList.adapter = mTravelSetAdapter
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
        subscribeUi(svm!!)
    }

    private fun subscribeUi(viewModel: SharedViewModel) {
        viewModel.travelSetList.observe(this, Observer<List<TravelSetEntity>> { travelSetList ->
            if (travelSetList != null) {
                mBinding!!.isLoading = false
                mTravelSetAdapter!!.setTravelSetList(travelSetList)
            } else { mBinding!!.isLoading = true }
            mBinding!!.executePendingBindings()
        })
    }

    companion object {
        private var fragment: TravelListFragment? = null
        fun getInstance(): TravelListFragment {
            if (null == fragment) {
                fragment = TravelListFragment()
            }
            return fragment!!
        }
    }
}
