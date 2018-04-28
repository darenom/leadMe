package org.darenom.leadme.db.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import org.darenom.leadme.db.entities.TravelStatEntity

/**
 * Created by adm on 12/02/2018.
 */

@Dao
interface TravelStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg travelSetEntities: TravelStatEntity)

    @Update
    fun update(vararg travelSetEntities: TravelStatEntity)

    @Delete
    fun delete(vararg travelSetEntity: TravelStatEntity)

    @Query("SELECT * FROM TravelStatEntity WHERE name LIKE :name")
    fun getByName(name: String): LiveData<List<TravelStatEntity>>

    @Query("SELECT * FROM TravelStatEntity")
    fun getAll(): LiveData<List<TravelStatEntity>>
}
