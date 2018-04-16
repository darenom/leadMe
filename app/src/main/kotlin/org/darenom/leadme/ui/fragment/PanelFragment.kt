package org.darenom.leadme.ui.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_panel.*
import org.darenom.leadme.R


class PanelFragment : Fragment() {

    var makerFragment: TravelMakerFragment? = null
    var listFragment: TravelListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        retainInstance = true

        makerFragment = TravelMakerFragment.getInstance()
        listFragment = TravelListFragment.getInstance()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_panel, container, false)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewpager.adapter = SectionsPagerAdapter(activity!!.supportFragmentManager)

        bottombar.setOnNavigationItemSelectedListener({ item ->
            item.isChecked = true
            when (item.itemId) {
                R.id.panel_maker -> viewpager.currentItem = 0
                R.id.panel_list -> viewpager.currentItem = 1
            }
            false
        })
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            var f: Fragment? = null
            when (position) {

                0 -> f = makerFragment
                1 -> f = listFragment
            }
            return f!!
        }

        override fun getCount(): Int {
            return 2
        }
    }

}