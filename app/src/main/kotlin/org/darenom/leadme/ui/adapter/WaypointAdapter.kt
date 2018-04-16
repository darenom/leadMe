package org.darenom.leadme.ui.adapter

import android.databinding.DataBindingUtil
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.darenom.leadme.R
import org.darenom.leadme.databinding.ViewholderWaypointBinding
import org.darenom.leadme.ui.adapter.helper.ItemTouchHelperAdapter
import org.darenom.leadme.ui.adapter.helper.ItemTouchHelperViewHolder
import org.darenom.leadme.ui.callback.WaypointsChanged
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by admadmin on 05/03/2018.
 */

class WaypointAdapter(waypointsaddr: String, waypointsposs: String, val callback: WaypointsChanged) :
        RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder>(),
        ItemTouchHelperAdapter {

    private var listaddr: ArrayList<String> = ArrayList()
    private var listposs: ArrayList<String> = ArrayList()

    init {
        setList(waypointsaddr, waypointsposs)
    }

    fun setList(waypointsaddr: String, waypointsposs: String) {

        if (waypointsaddr.isNotEmpty()) {
            listaddr.clear()
            waypointsaddr.split("#")
                    .filter { it.isNotEmpty() }
                    .forEach { listaddr.add(it) }
            listposs.clear()
            waypointsposs.split("#")
                    .filter { it.isNotEmpty() }
                    .forEach { listposs.add(it) }
            notifyDataSetChanged()

        } else {
            listaddr.clear()
            listposs.clear()
            notifyDataSetChanged()
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val binding = DataBindingUtil.inflate<ViewholderWaypointBinding>(
                LayoutInflater.from(parent.context), R.layout.viewholder_waypoint, parent, false)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WaypointViewHolder(binding, this, parent.context.resources.getColor(R.color.colorPrimaryDark,null))
        } else {
            WaypointViewHolder(binding, this, parent.context.resources.getColor(R.color.colorPrimaryDark))
        }
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        holder.binding.text = listaddr[position]
    }

    override fun getItemCount(): Int {
        return listaddr.size
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(listaddr, fromPosition, toPosition)
        Collections.swap(listposs, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        callback.onListchanged(listaddr, listposs)
        return true
    }

    override fun onItemDismiss(position: Int) {
        listaddr.removeAt(position)
        listposs.removeAt(position)
        notifyItemRemoved(position)
        callback.onListchanged(listaddr, listposs)
    }

    override fun hasChanged() {
        callback.onListchanged(listaddr, listposs)
    }

    class WaypointViewHolder(val binding: ViewholderWaypointBinding, val listener: ItemTouchHelperAdapter, val color: Int) :
            RecyclerView.ViewHolder(binding.root),
            ItemTouchHelperViewHolder {

        override fun onItemSelected() {
            itemView.setBackgroundColor(color)
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
            listener.hasChanged()
        }
    }

}