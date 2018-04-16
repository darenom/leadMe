package org.darenom.leadme.db.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.db.model.TravelSet

/**
 * Created by adm on 01/02/2018.
 */

@Entity
data class TravelSetEntity(

        @PrimaryKey
        @ColumnInfo
        override var name: String = BuildConfig.TMP_NAME,
        @ColumnInfo
        override var originAddress: String = "",
        @ColumnInfo
        var originPosition: String = "",
        @ColumnInfo
        override var destinationAddress: String = "",
        @ColumnInfo
        var destinationPosition: String = "",
        @ColumnInfo
        var waypointAddress: String = "",
        @ColumnInfo
        var waypointPosition: String = "",
        @ColumnInfo
        override var max: Int = 0,
        @ColumnInfo
        override var mode: Int = 0,
        @ColumnInfo
        override var distance: String = "",
        @ColumnInfo
        override var estimatedTime: String = ""

) : TravelSet {

    @Ignore
    constructor() : this(
            BuildConfig.TMP_NAME,
            "",
            "",
            "",
            "",
            "",
            "",
            0,
            0,
            "",
            ""
    )
}