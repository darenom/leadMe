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

        viewpager.adapter = SectionsPagerAdapter(activity!!.supportFragmentManager)

        bottombar.setOnNavigationItemSelectedListener({ item ->
            item.isChecked = true
            when (item.itemId) {
                R.id.panel_maker -> viewpager.currentItem = 0
                R.id.panel_stat -> viewpager.currentItem = 1
                R.id.panel_list -> viewpager.currentItem = 2
            }
            false
        })
    }

    private fun subscribeUi(vm: SharedViewModel) {
        vm.name.observe(activity!!, Observer { it ->
            if (null != it)
                statFragment!!.swapTo(it)
        })
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            var f: Fragment? = null
            when (position) {
                0 -> f = makerFragment
                1 -> f = statFragment
                2 -> f = listFragment

            }
            return f!!
        }

        override fun getCount(): Int {
            return 3
        }
    }

}