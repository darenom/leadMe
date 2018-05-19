package org.darenom.leadme.room.entities

import android.arch.persistence.room.*
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.room.model.TravelStat


/**
 * Created by adm on 12/02/2018.
 */

@Entity(
        indices = [(Index(value = arrayOf("name", "iter")))],
        foreignKeys = [(ForeignKey(entity = TravelSetEntity::class, parentColumns = ["name"], childColumns = ["name"]))]
)
data class TravelStatEntity(

        @ColumnInfo
        override var name: String = BuildConfig.TMP_NAME,
        @ColumnInfo
        override var iter: Int = 1,
        @ColumnInfo
        override var timestart: String = "",
        @ColumnInfo
        override var timeend: String = "",
        @ColumnInfo
        override var timed: String = "",
        @ColumnInfo
        override var distance: String = "",
        @ColumnInfo
        override var avgSpeed: String = ""

) : TravelStat {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var id: Int = 0

    @Ignore
    constructor() : this(BuildConfig.TMP_NAME, 1, "","","","","")
}