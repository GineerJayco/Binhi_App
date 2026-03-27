package com.example.binhi.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for storing soil data sessions and points
 * This database handles all persistent storage for the application
 */
@Database(
    entities = [SessionEntity::class, SoilDataPointEntity::class],
    version = 1
)
abstract class SoilDataDatabase : RoomDatabase() {
    /**
     * Get the SessionDao for database operations on sessions
     */
    abstract fun sessionDao(): SessionDao

    /**
     * Get the SoilDataPointDao for database operations on soil data points
     */
    abstract fun soilDataPointDao(): SoilDataPointDao

    companion object {
        @Volatile
        private var instance: SoilDataDatabase? = null

        /**
         * Get or create a database instance (singleton pattern)
         */
        fun getInstance(context: Context): SoilDataDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoilDataDatabase::class.java,
                    "soil_data_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}


