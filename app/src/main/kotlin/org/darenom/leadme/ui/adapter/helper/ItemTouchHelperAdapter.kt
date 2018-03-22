package org.darenom.leadme.ui.adapter.helper

/**
 * Created by admadmin on 15/03/2018.
 */

interface ItemTouchHelperAdapter {

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean

    fun onItemDismiss(position: Int)

    fun hasChanged()
}

