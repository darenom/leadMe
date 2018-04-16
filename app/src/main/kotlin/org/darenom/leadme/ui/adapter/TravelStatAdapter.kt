package org.darenom.leadme.ui.adapter

import android.databinding.DataBindingUtil
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.darenom.leadme.R
import org.darenom.leadme.databinding.ItemTravelstatBinding
import org.darenom.leadme.db.model.TravelStat
import org.darenom.leadme.ui.callback.TravelStatClickCallback


/**
 * Created by adm on 16/02/2018.
 */


class TravelStatAdapter(private val mTravelStatClickCallback: TravelStatClickCallback?)
    : RecyclerView.Adapter<TravelStatAdapter.CommentViewHolder>() {

    private var mTravelStatList: List<TravelStat>? = null

    fun setTravelStatList(travelStat: List<TravelStat>) {
        if (mTravelStatList == null) {
            mTravelStatList = travelStat
            notifyItemRangeInserted(0, travelStat.size)
        } else {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return mTravelStatList!!.size
                }

                override fun getNewListSize(): Int {
                    return travelStat.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val one = mTravelStatList!![oldItemPosition]
                    val two = travelStat[newItemPosition]
                    return one.name === two.name
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val one = mTravelStatList!![oldItemPosition]
                    val two = travelStat[newItemPosition]
                    return (one.name === two.name)
                }
            })
            mTravelStatList = travelStat
            diffResult.dispatchUpdatesTo(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = DataBindingUtil
                .inflate<ItemTravelstatBinding>(LayoutInflater.from(parent.context), R.layout.item_travelstat,
                        parent, false)
        binding.callback = mTravelStatClickCallback
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.binding.travelStat = mTravelStatList!![position]
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return if (mTravelStatList == null) 0 else mTravelStatList!!.size
    }

    class CommentViewHolder(val binding: ItemTravelstatBinding) : RecyclerView.ViewHolder(binding.root)
}
