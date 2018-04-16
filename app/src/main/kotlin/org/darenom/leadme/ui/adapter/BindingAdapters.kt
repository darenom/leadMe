package org.darenom.leadme.ui.adapter

import android.databinding.BindingAdapter
import android.view.View


/**
 * Created by adm on 12/02/2018.
 */

object BindingAdapters {
    @JvmStatic @BindingAdapter("visibleGone")
    fun showHide(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }
}