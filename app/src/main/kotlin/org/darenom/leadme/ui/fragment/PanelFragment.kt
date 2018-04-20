package org.darenom.leadme.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_panel.*
import org.darenom.leadme.R
import org.darenom.leadme.ui.StatisticsActivity
import org.darenom.leadme.ui.viewmodel.SharedViewModel


class PanelFragment : Fragment() {

    private lateinit var svm: SharedViewModel
    var makerFragment: TravelMakerFragment? = null
    var listFragment: TravelListFragment? = null
    var statFragment: StatisticsActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = true

        statFragment = StatisticsActivity.getInstance()
        makerFragment = TravelMakerFragment.getInstance()
        listFragment = TravelListFragment.getInstance()


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_panel, container, false)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
        subscribeUi(svm)

        childFragmentManager.beginTransaction().add(R.id.fragment_panel, makerFragment, "maker").commit()

    }

    private fun subscribeUi(vm: SharedViewModel) {
        vm.name.observe(activity!!, Observer { it ->
            if (null != it)
                statFragment!!.swapTo(it)
        })
    }

    internal fun setPanel(i : Int){
        val f = when (i){
            1 -> {statFragment}
            2 -> {listFragment}
            else -> {makerFragment}

        }
        childFragmentManager.beginTransaction().replace(R.id.fragment_panel, f, "maker").commit()
    }


}