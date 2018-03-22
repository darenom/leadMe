package org.darenom.leadme.db.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import org.darenom.leadme.db.entities.TravelSetEntity

/**
 * Created by adm on 01/02/2018.
 */

@Dao
interface TravelSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg travelSetEntities: TravelSetEntity)

    @Update
    fun update(vararg travelSetEntities: TravelSetEntity)

    @Delete
    fun delete(vararg travelSetEntity: TravelSetEntity)

   @Query("SELECT * FROM TravelSetEntity WHERE name LIKE :name")
   fun getByName(name: String): LiveData<TravelSetEntity>

   @Query("SELECT * FROM TravelSetEntity WHERE name NOT LIKE 'tmp'")
   fun getAll(): LiveData<List<TravelSetEntity>>
}
