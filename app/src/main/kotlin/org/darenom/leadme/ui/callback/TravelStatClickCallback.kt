package org.darenom.leadme.ui.callback

import org.darenom.leadme.db.model.TravelStat

/**
 * Created by adm on 16/02/2018.
 */

interface TravelStatClickCallback {
    fun onClick(stat: TravelStat)
}
