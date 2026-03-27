package com.example.binhi.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object for Session entities
 * Provides database operations for saving and retrieving sessions
 */
@Dao
interface SessionDao {
    /**
     * Insert a new session into the database
     * If a session with the same ID exists, it will be replaced
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    /**
     * Get all sessions from the database, ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    /**
     * Get a specific session by ID
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    /**
     * Update an existing session
     */
    @Update
    suspend fun updateSession(session: SessionEntity)

    /**
     * Delete a session by ID
     * This will also delete all associated soil data points due to CASCADE
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    /**
     * Delete a session entity
     * This will also delete all associated soil data points due to CASCADE
     */
    @Delete
    suspend fun deleteSession(session: SessionEntity)

    /**
     * Get the count of all sessions
     */
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
}

