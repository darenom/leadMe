package org.darenom.leadme.db

import android.arch.persistence.room.TypeConverter
import java.util.*


/**
 * Created by adm on 06/02/2018.
 */

object DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp == null) null else Date(timestamp)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (if (date == null) null else date.time)!!.toLong()
    }

    fun compoundDuration(n: Int): String {
        if (n < 0) return "" // task doesn't ask for negative integers to be converted
        if (n == 0) return "0 sec"
        val weeks  : Int
        val days   : Int
        val hours  : Int
        val minutes: Int
        val seconds: Int
        var divisor: Int = 7 * 24 * 60 * 60
        var rem    : Int
        var result = ""

        weeks = n / divisor
        rem   = n % divisor
        divisor /= 7
        days  = rem / divisor
        rem  %= divisor
        divisor /= 24
        hours = rem / divisor
        rem  %= divisor
        divisor /= 60
        minutes = rem / divisor
        seconds = rem % divisor

        if (weeks > 0)   result += "$weeks w, "
        if (days > 0)    result += "$days d, "
        if (hours > 0)   result += "$hours h, "
        if (minutes > 0) result += "$minutes mn, "
        if (seconds > 0)
            result += "$seconds s"
        else
            result = result.substring(0, result.length - 2)
        return result
    }
}
