package org.darenom.leadme.db.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import org.darenom.leadme.db.entities.TravelStampEntity
import org.darenom.leadme.db.model.TravelStamp


/**
 * Created by adm on 14/02/2018.
 */

@Dao
interface TravelStampDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg travelStampEntity: TravelStampEntity)

    @Delete
    fun delete(vararg travelStampEntity: TravelStampEntity)

    @Query("SELECT * FROM TravelStampEntity WHERE name LIKE :name")
    fun getByName(name: String): LiveData<List<TravelStampEntity>>

    @Query("SELECT DISTINCT name, iter FROM TravelStampEntity GROUP BY name, iter")
    fun getNames(): List<TravelStamp>

    @Query("SELECT * FROM TravelStampEntity WHERE name LIKE :name AND  iter = :iter")
    fun getByIter(name: String, iter: Int): List<TravelStampEntity>

    @Query("DELETE FROM TravelStampEntity WHERE name LIKE :name")
    fun wipe(name: String)

    @Query("UPDATE TravelStampEntity SET name = :name WHERE name LIKE 'tmp'")
    fun updateSet(name: String)



}
