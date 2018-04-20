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
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_maker.*
import org.darenom.leadme.BaseApp
import org.darenom.leadme.R
import org.darenom.leadme.TravelActivity
import org.darenom.leadme.databinding.FragmentMakerBinding
import org.darenom.leadme.service.TravelService
import org.darenom.leadme.service.TravelService.Companion.travel
import org.darenom.leadme.ui.adapter.WaypointAdapter
import org.darenom.leadme.ui.adapter.helper.ItemTouchHelperAdapter
import org.darenom.leadme.ui.adapter.helper.SimpleItemTouchHelperCallback
import org.darenom.leadme.ui.callback.WaypointsChanged
import org.darenom.leadme.ui.viewmodel.SharedViewModel
import java.util.*


class TravelMakerFragment : Fragment(), WaypointsChanged {

    private var mBinding: FragmentMakerBinding? = null

    private var svm: SharedViewModel? = null

    private var mItemTouchHelper: ItemTouchHelper? = null

    private var focus = View.OnFocusChangeListener { v, hasFocus ->
        if (!hasFocus)
            if ((v as EditText).text.toString().isNotEmpty())
                setPoint(v.id, v.text.toString())
    }

    private var editor = TextView.OnEditorActionListener { v, actionId, event ->
        if (((event != null &&
                        (event.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_NEXT) ||
                        (actionId == EditorInfo.IME_ACTION_DONE))) {
            if ((v as EditText).text.toString().isNotEmpty()) {
                when (v.id) {

                    R.id.edtFrom ->
                        if (svm!!.travelSet.value!!.destinationAddress.isEmpty())
                            edtTo.requestFocus()
                        else edtWaypoint.requestFocus()

                    R.id.edtTo ->
                        if (svm!!.travelSet.value!!.originAddress.isEmpty())
                            edtFrom.requestFocus()
                        else edtWaypoint.requestFocus()

                    R.id.edtWaypoint ->
                        when {
                            svm!!.travelSet.value!!.originAddress.isEmpty() ->
                                edtFrom.requestFocus()
                            svm!!.travelSet.value!!.destinationAddress.isEmpty() ->
                                edtTo.requestFocus()
                            v.text.toString().isNotEmpty() ->
                                (activity!!.application as BaseApp).mAppExecutors!!.mainThread()
                                        .execute({ edtWaypoint.requestFocus() })
                        }

                }
            }
        }
        false
    }

    // editText drawables click handling
    private var touch = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX <= ((v as EditText).compoundDrawables[0].bounds.width()) * 2.5) {
                if (v.text.isEmpty())
                    setLocationText(v.id)
                else
                    clearText(v.id)
            }
            return@OnTouchListener true
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        svm = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)

        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_maker, container, false)

        mBinding!!.edtFrom.setOnEditorActionListener(editor)
        mBinding!!.edtFrom.onFocusChangeListener = focus
        (mBinding!!.edtFrom as View).setOnTouchListener(touch)

        mBinding!!.edtTo.setOnEditorActionListener(editor)
        mBinding!!.edtTo.onFocusChangeListener = focus
        (mBinding!!.edtTo as View).setOnTouchListener(touch)

        mBinding!!.edtWaypoint.setOnEditorActionListener(editor)
        mBinding!!.edtWaypoint.onFocusChangeListener = focus
        (mBinding!!.edtWaypoint as View).setOnTouchListener(touch)

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

        mBinding!!.rvWaypoints.adapter = WaypointAdapter("", "", this)
        mBinding!!.rvWaypoints.layoutManager = LinearLayoutManager(context)

        mItemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(
                mBinding!!.rvWaypoints.adapter as ItemTouchHelperAdapter))
        mItemTouchHelper!!.attachToRecyclerView(mBinding!!.rvWaypoints)

        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeUI()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when (item.itemId) {
            R.id.opt_clear -> {
                mBinding!!.edtFrom.requestFocus()
            }
            R.id.opt_play_stop -> {
                if (TravelService.travelling)
                    enable(false)
                else
                    enable(true)
            }
        }
        return false
    }

    override fun onListchanged(listaddr: ArrayList<String>, listposs: ArrayList<String>) {
        travel.value = null // reset travel as input has changed
        var straddr = ""
        var strposs = ""
        for (s in listaddr) {
            straddr += "$s#"
            strposs += "${listposs[listaddr.indexOf(s)]}#"
        }
        svm!!.travelSet.value!!.waypointAddress = straddr
        svm!!.travelSet.value!!.waypointPosition = strposs
        svm!!.update(svm!!.travelSet.value!!)
    }

    private fun subscribeUI() {

        svm!!.travelSet.observe(this, Observer { it ->
            if (null != it) {
                mBinding!!.travelSet = it
                if (it.originPosition.isEmpty())
                    mBinding!!.edtFrom.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_opt_pos), null, null, null)
                else
                    mBinding!!.edtFrom.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_clear_cancel), null, null, null)
                if (it.destinationPosition.isEmpty())
                    mBinding!!.edtTo.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_opt_pos), null, null, null)
                else
                    mBinding!!.edtTo.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_clear_cancel), null, null, null)
                (mBinding!!.rvWaypoints.adapter as WaypointAdapter).setList(it.waypointAddress, it.waypointPosition)
                mBinding!!.edtWaypoint.setText("")
            }
        })

        travel.observe(this, Observer { it ->
            if (null == it)
                enable(true)
            else
                enable(false)

        })

        mBinding!!.edtFrom.requestFocus()
    }

    // UI locker
    private fun enable(b: Boolean) {


        if (!b) {
            mBinding!!.edtFrom.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            mBinding!!.edtTo.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        } else {
            if (svm!!.travelSet.value!!.originPosition.isEmpty())
                mBinding!!.edtFrom.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_opt_pos), null, null, null)
            else
                mBinding!!.edtFrom.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_clear_cancel), null, null, null)
            if (svm!!.travelSet.value!!.destinationPosition.isEmpty())
                mBinding!!.edtTo.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_opt_pos), null, null, null)
            else
                mBinding!!.edtTo.setCompoundDrawablesWithIntrinsicBounds(context!!.getDrawable(R.drawable.ic_clear_cancel), null, null, null)

        }


        if (b) {
            mItemTouchHelper!!.attachToRecyclerView(mBinding!!.rvWaypoints)
        } else {
            mItemTouchHelper!!.attachToRecyclerView(null)
        }

        mBinding!!.enabled = b
    }

    private fun setPoint(i: Int, s: String) {
        if ((activity!!.application as BaseApp).isNetworkAvailable) {

            val a: SharedViewModel.Companion.PointType? = when (i) {
                R.id.edtFrom -> SharedViewModel.Companion.PointType.ORIGIN
                R.id.edtTo -> SharedViewModel.Companion.PointType.DESTINATION
                R.id.edtWaypoint -> SharedViewModel.Companion.PointType.WAYPOINT
                else -> null
            }

            if (null != a)
                svm!!.setPoint(a, s)

        } else
            activity!!.startActivityForResult(Intent(Settings.ACTION_WIFI_SETTINGS), TravelActivity.CHECK_NET_ACCESS)

    }


    private fun setLocationText(id: Int) {

        if ((activity!!.application as BaseApp).travelService!!.hasPos) {
            if (null != TravelService.here) {
                when (id) {
                    R.id.edtFrom -> svm!!.setLocationAs(SharedViewModel.Companion.PointType.ORIGIN,
                            LatLng(TravelService.here!!.latitude, TravelService.here!!.longitude))
                    R.id.edtTo -> svm!!.setLocationAs(SharedViewModel.Companion.PointType.DESTINATION,
                            LatLng(TravelService.here!!.latitude, TravelService.here!!.longitude))
                    R.id.edtWaypoint -> svm!!.setLocationAs(SharedViewModel.Companion.PointType.WAYPOINT,
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
    }

    private fun clearText(id: Int) {
        when (id) {
            R.id.edtFrom -> {
                svm!!.travelSet.value!!.originPosition = ""
                svm!!.travelSet.value!!.originAddress = ""
                svm!!.update(svm!!.travelSet.value!!)
            }
            R.id.edtTo -> {
                svm!!.travelSet.value!!.destinationPosition = ""
                svm!!.travelSet.value!!.destinationAddress = ""
                svm!!.update(svm!!.travelSet.value!!)
            }
            R.id.edtWaypoint -> edtWaypoint.setText("")
        }
    }

    companion object {
        private var fragment: TravelMakerFragment? = null
        fun getInstance(): TravelMakerFragment {
            if (null == fragment) {
                fragment = TravelMakerFragment()
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
            mViewHolder!!.img.background = ContextCompat.getDrawable(context,
                    context.resources.getIdentifier(
                            this.list[position],
                            "drawable",
                            context.packageName))
            return view!!
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return getView(position, convertView, parent)
        }
    }

}

