package org.darenom.leadme.ui.fragment

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.fragment_maker.*
import kotlinx.android.synthetic.main.layout_search.*
import org.darenom.leadme.BaseApp
import org.darenom.leadme.R
import org.darenom.leadme.databinding.FragmentMakerBinding
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.ui.TravelActivity
import org.darenom.leadme.ui.adapter.WaypointAdapter
import org.darenom.leadme.ui.adapter.helper.ItemTouchHelperAdapter
import org.darenom.leadme.ui.adapter.helper.SimpleItemTouchHelperCallback
import org.darenom.leadme.ui.callback.WaypointsChanged
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import android.view.View.OnTouchListener
import com.google.maps.model.LatLng
import org.darenom.leadme.service.TravelService


/**
 * Created by adm on 02/02/2018.
 *
 * written travel maker:
 *
 * input start, end are mandatory to request DirectionAPI.
 * waypoints may be added
 *
 * Travel can be namely saved upon results or if played at least once,
 * otherwise it'll be saved as temporary for maintained state purposes.
 * If not saved after being played, records will be wiped from the database
 * (still, infos about the travel remains as temporary)
 */

class MakerFragment : Fragment(), WaypointsChanged {

    private var mBinding: FragmentMakerBinding? = null

    private var svm: SharedViewModel? = null

    private var mItemTouchHelper: ItemTouchHelper? = null

    private var focus = View.OnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            if ((v as EditText).text.toString().isNotEmpty())
                setPoint(v.id, v.text.toString())
            (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private var editor = TextView.OnEditorActionListener { v, actionId, event ->

        if (((event != null &&
                        (event.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_NEXT) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)))

            when (v.id) {
                R.id.edtFrom -> if (svm!!.travelSet.value!!.destinationAddress.isEmpty()) edtTo.requestFocus() else edtWaypoint.requestFocus()
                R.id.edtTo -> if (svm!!.travelSet.value!!.originAddress.isEmpty()) edtFrom.requestFocus() else edtWaypoint.requestFocus()
                R.id.edtWaypoint -> {
                    when {
                        svm!!.travelSet.value!!.originAddress.isEmpty() -> edtFrom.requestFocus()
                        svm!!.travelSet.value!!.destinationAddress.isEmpty() -> edtTo.requestFocus()
                        (v as EditText).text.toString().isNotEmpty() -> {
                            (activity!!.application as BaseApp).mAppExecutors!!.mainThread().execute({ edtWaypoint.requestFocus() })
                        }
                    }
                }
            }
        false
    }

    private var touch = OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= v.getRight() - (v as EditText).getCompoundDrawables()[2].getBounds().width()) {
                if ((activity!!.application as BaseApp).travelService!!.hasPos) {
                    if (null != TravelService.here) {
                        when (v.id){
                            edtFrom.id -> svm!!.setLocationAs(SharedViewModel.Companion.PointType.ORIGIN,
                                    LatLng(TravelService.here!!.latitude, TravelService.here!!.longitude))
                            edtTo.id -> svm!!.setLocationAs(SharedViewModel.Companion.PointType.DESTINATION,
                                    LatLng(TravelService.here!!.latitude, TravelService.here!!.longitude))
                            edtWaypoint.id-> svm!!.setLocationAs(SharedViewModel.Companion.PointType.WAYPOINT,
                                    LatLng(TravelService.here!!.latitude, TravelService.here!!.longitude))
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.searching_loc), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(context!!,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    //  missing feature
                        startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), TravelActivity.LOCATION_SET_HERE)
                    else
                    // missing perm
                        ActivityCompat.requestPermissions(activity!!,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                TravelActivity.PERM_SET_HERE)
                }

                return@OnTouchListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        retainInstance = true   // onConfigChange retain
        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_maker, container, false)

        mBinding!!.rvWaypoints.adapter = WaypointAdapter("", "", this)
        mBinding!!.rvWaypoints.layoutManager = LinearLayoutManager(context)

        mItemTouchHelper = ItemTouchHelper(
                SimpleItemTouchHelperCallback(mBinding!!.rvWaypoints.adapter as ItemTouchHelperAdapter))
        mItemTouchHelper!!.attachToRecyclerView(mBinding!!.rvWaypoints)

        mBinding!!.search!!.edtFrom.setOnEditorActionListener(editor)
        mBinding!!.search!!.edtFrom.onFocusChangeListener = focus
        mBinding!!.search!!.edtFrom.setOnTouchListener(touch)

        mBinding!!.search!!.edtTo.setOnEditorActionListener(editor)
        mBinding!!.search!!.edtTo.onFocusChangeListener = focus
        mBinding!!.search!!.edtTo.setOnTouchListener(touch)

        mBinding!!.edtWaypoint.setOnEditorActionListener(editor)
        mBinding!!.edtWaypoint.onFocusChangeListener = focus
        mBinding!!.edtWaypoint.setOnTouchListener(touch)

        mBinding!!.spinnerMode.adapter = TravelModeAdapter(context!!, R.layout.item_image)
        mBinding!!.spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (null != svm!!.travelSet.value) {
                    svm!!.travelSet.value!!.mode = pos
                    svm!!.update(svm!!.travelSet.value!!)
                    svm!!.tmode.value = pos
                }
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {}
        }

        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeUI()
    }

    override fun onListchanged(addr: ArrayList<String>, poss: ArrayList<String>) {
        travel.value = null // reset travel as input has changed
        var straddr = ""
        var strposs = ""
        for (s in addr) {
            straddr += "$s#"
            strposs += "${poss[addr.indexOf(s)]}#"
        }
        svm!!.travelSet.value!!.waypointAddress = straddr
        svm!!.travelSet.value!!.waypointPosition = strposs
        svm!!.update(svm!!.travelSet.value!!)
    }

    private fun subscribeUI() {

        svm!!.travelSet.observe(this, Observer { it ->
            if (null != it) {
                mBinding!!.travelSet = it
                (mBinding!!.rvWaypoints.adapter as WaypointAdapter).setList(it.waypointAddress, it.waypointPosition)
                edtWaypoint.setText("")
                activity!!.invalidateOptionsMenu()
            }
        })

        travel.observe(this, Observer { _ ->
            activity!!.invalidateOptionsMenu()
        })
    }

    // UI locker
    internal fun enable(b: Boolean) {
        edtFrom.isEnabled = b
        edtTo.isEnabled = b
        edtWaypoint.isEnabled = b
        rvWaypoints.isEnabled = b
        search_swap.isEnabled = b
        spinnerMode.isEnabled = b
        edtWaypoint.visibility = if (!b) View.GONE else View.VISIBLE
    }

    private fun setPoint(i: Int, s: String) {
        if ((activity!!.application as BaseApp).isNetworkAvailable){

            val a: SharedViewModel.Companion.PointType? = when (i) {
                R.id.edtFrom -> SharedViewModel.Companion.PointType.ORIGIN
                R.id.edtTo -> SharedViewModel.Companion.PointType.DESTINATION
                R.id.edtWaypoint -> SharedViewModel.Companion.PointType.WAYPOINT
                else -> null }

            if (null != a)
                svm!!.setPoint(a, s)

        } else
            activity!!.startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), TravelActivity.CHECK_NET_ACCESS)

    }

    companion object {
        private var fragment: MakerFragment? = null
        fun getInstance(): MakerFragment {
            if (null == fragment) {
                fragment = MakerFragment()
            }
            return fragment!!
        }
    }

    inner class TravelModeAdapter(context: Context, resource: Int) : ArrayAdapter<String>(context, resource) {

        inner class ViewHolder constructor(context: Context) {
            var img: ImageView = ImageView(context)
        }

        private var mViewHolder: ViewHolder? = null
        private var mInflater: LayoutInflater? = null
        var list: Array<String?> = arrayOfNulls(4)

        init {
            mInflater = LayoutInflater.from(context)
            list = context.resources.getStringArray(R.array.travel_mode_drw)
            mViewHolder = ViewHolder(context)
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
                mViewHolder!!.img = view.findViewById(R.id.img)
                view.tag = mViewHolder
            } else {
                mViewHolder = view.tag as ViewHolder
            }
            mViewHolder!!.img.background = context.resources.getDrawable(
                    context.resources.getIdentifier(this.list[position],
                            "drawable", context.packageName))
            return view!!
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return getView(position, convertView, parent)
        }
    }

}

