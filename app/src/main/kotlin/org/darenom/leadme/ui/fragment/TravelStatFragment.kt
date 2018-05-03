package org.darenom.leadme.ui.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.fragment_travelstat.*
import org.darenom.leadme.R
import org.darenom.leadme.TravelActivity
import org.darenom.leadme.databinding.FragmentTravelstatBinding
import org.darenom.leadme.db.entities.TravelStatEntity
import org.darenom.leadme.db.model.TravelStat
import org.darenom.leadme.ui.adapter.TravelStatAdapter
import org.darenom.leadme.ui.callback.TravelStatClickCallback
import org.darenom.leadme.ui.viewmodel.TravelStatViewModel
import java.util.*


/**
 * Created by admadmin on 18/03/2018.
 */

class TravelStatFragment : Fragment() {

    var mBinding: FragmentTravelstatBinding? = null

    private lateinit var mTravelStatAdapter: TravelStatAdapter

    private var ref: String = ""
    private val mTravelStatClickCallback = object : TravelStatClickCallback {
        override fun onClick(travelStat: TravelStat) {
            if (!String.format("%s%s", travelStat.name, travelStat.iter.toString()).contentEquals(ref)) {
                (activity!! as TravelActivity).setRun(travelStat.name, travelStat.iter)
                ref = String.format("%s%s", travelStat.name, travelStat.iter.toString())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_travelstat, container, false)
        mTravelStatAdapter = TravelStatAdapter(mTravelStatClickCallback)
        mBinding!!.travelstatlist.layoutManager = LinearLayoutManager(context)
        mBinding!!.travelstatlist.adapter = mTravelStatAdapter
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val factory = TravelStatViewModel.Factory(activity!!.application, arguments!!.getString(KEY_TRAVELSET_NAME))
        val model = ViewModelProviders.of(this, factory).get(TravelStatViewModel::class.java)
        subscribeToModel(model)
    }

    private fun subscribeToModel(model: TravelStatViewModel) {

        model.observableTravelStat.observe(this, Observer<List<TravelStatEntity>> { it ->
            if (null != it) {
                mTravelStatAdapter.setTravelStatList(it)
            }
            mBinding!!.executePendingBindings()
        })

    }


    fun showGraph(p: HashMap<Long, Double>?) {
        graph.removeAllSeries()
        val series = LineGraphSeries<DataPoint>()
        for (z in 0 until p!!.entries.size - 1) {
            series.appendData(
                    DataPoint(
                            z.toDouble(), //Date(p.keys.elementAt(z)),
                            p.values.elementAt(z)
                    ),
                    true,
                    p.entries.size)
        }
        // p!! .forEach { (t, u) -> series.appendData(DataPoint(Date(t), u), true, p.size) }
        graph.addSeries(series)
        mBinding!!.mSwitch = true
    }


    companion object {

        const val KEY_TRAVELSET_NAME = "travelset_name"

        /** Creates travelSet fragment for specific travelSet ID  */
        fun forTravelSet(name: String): TravelStatFragment {
            val fragment = TravelStatFragment()
            val args = Bundle()
            args.putString(KEY_TRAVELSET_NAME, name)
            fragment.arguments = args
            return fragment
        }
    }
}
