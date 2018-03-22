package org.darenom.leadme.ui.adapter

import android.databinding.DataBindingUtil
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.darenom.leadme.R
import org.darenom.leadme.databinding.FragmentTravelsetBinding
import org.darenom.leadme.db.model.TravelSet
import org.darenom.leadme.ui.callback.TravelSetClickCallback


/**
 * Created by adm on 16/02/2018.
 */

class TravelSetAdapter(private val mTravelSetClickCallback: TravelSetClickCallback?)
    : RecyclerView.Adapter<TravelSetAdapter.TravelSetViewHolder>() {

    internal var mTravelSetList: List<TravelSet>? = null

    fun setTravelSetList(travelSetList: List<TravelSet>) {
        if (mTravelSetList == null) {
            mTravelSetList = travelSetList
            notifyItemRangeInserted(0, travelSetList.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return mTravelSetList!!.size
                }

                override fun getNewListSize(): Int {
                    return travelSetList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mTravelSetList!![oldItemPosition].name === travelSetList[newItemPosition].name
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val newProduct = travelSetList[newItemPosition]
                    val oldProduct = mTravelSetList!![oldItemPosition]
                    return (newProduct.name === oldProduct.name)
                }
            })
            mTravelSetList = travelSetList
            result.dispatchUpdatesTo(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelSetViewHolder {
        val binding = DataBindingUtil.inflate<FragmentTravelsetBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_travelset,
                parent,
                false
        )
        binding.callback = mTravelSetClickCallback
        return TravelSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TravelSetViewHolder, position: Int) {
        holder.binding.travelSet = mTravelSetList!![position]
        holder.binding.mSwitch = true
    }

    override fun getItemCount(): Int {
        return if (mTravelSetList == null) 0 else mTravelSetList!!.size
    }

    class TravelSetViewHolder(val binding: FragmentTravelsetBinding) : RecyclerView.ViewHolder(binding.root)
}
