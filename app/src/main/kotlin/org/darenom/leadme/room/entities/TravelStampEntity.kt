package org.darenom.leadme.room.entities

import android.arch.persistence.room.*

/**
 * Created by adm on 12/02/2018.
 */

@Entity(
        indices = [(Index(value = arrayOf("name", "iter")))],
        foreignKeys = [ForeignKey(entity = TravelSetEntity::class, parentColumns = ["name"], childColumns = ["name"])]
)
data class TravelStampEntity(

        @ColumnInfo
        var name: String? = null,
        @ColumnInfo
        var iter: Int? = null,
        @ColumnInfo
        var time: Long? = null,
        @ColumnInfo
        var lat: Double? = null,
        @ColumnInfo
        var lng: Double? = null,
        @ColumnInfo
        var accuracy: Float? = null,
        @ColumnInfo
        var bearing: Float? = null,
        @ColumnInfo
        var provider: String? = null,
        @ColumnInfo
        var altitude: Double? = null,
        @ColumnInfo
        var data: String? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var id: Int = 0

    @Ignore
    constructor() : this(null,null,null,null,null,null,null,null,null)
}