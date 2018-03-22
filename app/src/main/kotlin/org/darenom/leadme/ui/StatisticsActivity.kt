package org.darenom.leadme.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.darenom.leadme.R
import org.darenom.leadme.db.model.TravelSet
import org.darenom.leadme.ui.callback.TravelSetClickCallback
import org.darenom.leadme.ui.fragment.TravelSetFragment
import org.darenom.leadme.ui.fragment.TravelStatFragment

/**
 * Created by adm on 16/02/2018.
 */

class StatisticsActivity : AppCompatActivity() {

    private val mTravelSetClickCallback = object : TravelSetClickCallback {
        override fun loadInMap(travelSet: TravelSet) {
            // do nothing, is hidden
        }

        override fun onClick(travelSet: TravelSet) {
            // load on map
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        setSupportActionBar(null)

        if (intent.hasExtra(TravelSetFragment.KEY_TRAVELSET_NAME)) {
            val name = intent.getStringExtra(TravelSetFragment.KEY_TRAVELSET_NAME)

            val travelSetFragment = TravelSetFragment.forTravelSet(name)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_set,
                            travelSetFragment, name).commit()

            val travelStatFragment = TravelStatFragment.forTravelSet(name)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_stat,

                            travelStatFragment, name).commit()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }


}