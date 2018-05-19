package org.darenom.leadme.room.model

import android.arch.persistence.room.ColumnInfo

/**
 * Created by adm on 15/02/2018.
 */

class TravelStamp {
    @ColumnInfo
    var name: String? = null

    @ColumnInfo
    var iter: Int? = null
}