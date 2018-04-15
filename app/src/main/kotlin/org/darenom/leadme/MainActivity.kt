package org.darenom.leadme

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.ui.SaveTravelDialog
import org.darenom.leadme.ui.fragment.TravelFragment
import org.darenom.leadme.ui.fragment.TravelListFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SaveTravelDialog.SaveTravelDialogListener  {
    override fun onCancel() {
        travelFragment.onCancel()
    }

    override fun onKeyListener(name: String) {
        travelFragment.onKeyListener(name)
    }

    private val travelFragment = TravelFragment.getInstance()
    private val listFragment = TravelListFragment.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        supportFragmentManager.beginTransaction().add(R.id.main_content, travelFragment).commit()

        travel.observe(this, Observer { it ->
            if (null != it)
                if (it.name.contentEquals(BuildConfig.TMP_NAME))
                    supportActionBar?.setTitle(R.string.app_name)
                else
                    supportActionBar?.title = it.name
            else
                supportActionBar?.setTitle(R.string.app_name)
            //supportFragmentManager.beginTransaction().replace(R.id.main_content, travelFragment).commit()
        })

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_travel -> {
                supportFragmentManager.beginTransaction().replace(R.id.main_content, travelFragment).commit()
            }
            R.id.nav_list -> {
                supportFragmentManager.beginTransaction().replace(R.id.main_content, listFragment).commit()
            }

            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        nav_view.setCheckedItem(item.itemId)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    fun startStopTravel(m: MenuItem?) {
        travelFragment.startStopTravel()
    }

    fun swapFromTo(v: View) {
        travelFragment.swapFromTo()
    }
}
