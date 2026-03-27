package com.example.binhi.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for SoilDataPoint entities
 * Provides database operations for saving and retrieving soil data points
 */
@Dao
interface SoilDataPointDao {
    /**
     * Insert a new soil data point into the database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilDataPoint(soilDataPoint: SoilDataPointEntity)

    /**
     * Insert multiple soil data points at once
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilDataPoints(soilDataPoints: List<SoilDataPointEntity>)

    /**
     * Get all soil data points for a specific session
     */
    @Query("SELECT * FROM soil_data_points WHERE sessionId = :sessionId")
    suspend fun getSoilDataPointsBySession(sessionId: String): List<SoilDataPointEntity>

    /**
     * Get a specific soil data point by session and location
     */
    @Query("SELECT * FROM soil_data_points WHERE sessionId = :sessionId AND latitude = :latitude AND longitude = :longitude")
    suspend fun getSoilDataPoint(sessionId: String, latitude: Double, longitude: Double): SoilDataPointEntity?

    /**
     * Update a soil data point
     */
    @Update
    suspend fun updateSoilDataPoint(soilDataPoint: SoilDataPointEntity)

    /**
     * Delete a soil data point
     */
    @Delete
    suspend fun deleteSoilDataPoint(soilDataPoint: SoilDataPointEntity)

    /**
     * Delete all soil data points for a specific session
     */
    @Query("DELETE FROM soil_data_points WHERE sessionId = :sessionId")
    suspend fun deleteSoilDataPointsBySession(sessionId: String)

    /**
     * Get the count of soil data points for a specific session
     */
    @Query("SELECT COUNT(*) FROM soil_data_points WHERE sessionId = :sessionId")
    suspend fun getCountBySession(sessionId: String): Int
}

