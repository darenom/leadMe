package org.darenom.leadme.ui.callback

import org.darenom.leadme.db.model.TravelSet

/**
 * Created by adm on 16/02/2018.
 */

interface TravelSetClickCallback {
    fun onClick(travelSet: TravelSet)
    fun loadInMap(travelSet: TravelSet)
}