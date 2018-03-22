package org.darenom.leadme.db

/**
 * Created by adm on 01/02/2018.
 */


import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import android.support.annotation.VisibleForTesting
import org.darenom.leadme.AppExecutors
import org.darenom.leadme.BuildConfig
import org.darenom.leadme.db.dao.TravelSetDao
import org.darenom.leadme.db.dao.TravelStampDao
import org.darenom.leadme.db.dao.TravelStatDao
import org.darenom.leadme.db.entities.TravelSetEntity
import org.darenom.leadme.db.entities.TravelStampEntity
import org.darenom.leadme.db.entities.TravelStatEntity
import android.arch.persistence.room.migration.Migration







@Database(entities = arrayOf(TravelSetEntity::class, TravelStatEntity::class, TravelStampEntity::class), version = 2)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    private val mIsDatabaseCreated = MutableLiveData<Boolean>()

    val databaseCreated: LiveData<Boolean>
        get() = mIsDatabaseCreated

    abstract fun travelSetDao(): TravelSetDao
    abstract fun travelStatDao(): TravelStatDao
    abstract fun travelStampDao(): TravelStampDao


    /**
     * Check whether the database already exists and expose it via [.getDatabaseCreated]
     */
    private fun updateDatabaseCreated(context: Context) {
        if (context.getDatabasePath(DATABASE_NAME).exists()) {
            setDatabaseCreated()
        }
    }

    private fun setDatabaseCreated() {
        mIsDatabaseCreated.postValue(true)
    }



    companion object {

        private var sInstance: AppDatabase? = null

        @VisibleForTesting
        val DATABASE_NAME = "leadme.db"

        fun getInstance(context: Context, executors: AppExecutors): AppDatabase {
            if (sInstance == null) {
                synchronized(AppDatabase::class.java) {
                    if (sInstance == null) {
                        sInstance = buildDatabase(context.applicationContext, executors)
                        sInstance!!.updateDatabaseCreated(context.applicationContext)
                    }
                }
            }
            return sInstance!!
        }

        /**
         * Build the database. [Builder.build] only sets up the database configuration and
         * creates a new instance of the database.
         * The SQLite database is only created when it's accessed for the first time.
         */
        private fun buildDatabase(appContext: Context,
                                  executors: AppExecutors): AppDatabase {
            return Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)

                    .addMigrations(MIGRATION_1_2)

                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            executors.diskIO().execute({
                                val database = getInstance(appContext, executors)
                                val t = database.travelSetDao().getByName(BuildConfig.TMP_NAME)
                                if (null == t.value)
                                    database.travelSetDao().insert(TravelSetEntity())
                                database.setDatabaseCreated()
                            })
                        }
                    }).build()
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TravelSetEntity ADD COLUMN distance TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE TravelSetEntity ADD COLUMN estimatedTime TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
