package org.darenom.leadme.room.model

/**
 * Created by adm on 12/02/2018.
 */

interface TravelSet {
    val name: String
    val originAddress: String
    val destinationAddress: String
    val max: Int
    val mode: Int
    val distance: String
    val estimatedTime: String

}