package com.example.binhi.data.database

import android.util.Log
import com.example.binhi.data.SoilData
import com.example.binhi.data.SavedSession

/**
 * Repository for managing all database operations related to soil data sessions
 * This follows the repository pattern to abstract database operations from the ViewModel
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val soilDataPointDao: SoilDataPointDao
) {
    /**
     * Save a complete session with all its soil data points
     */
    suspend fun saveSession(session: SavedSession): Boolean {
        return try {
            // Save the session entity
            val sessionEntity = SessionEntity.fromDomain(session)
            sessionDao.insertSession(sessionEntity)
            Log.d("SessionRepository", "✓ Saved session: ${session.sessionName}")

            // Save all soil data points
            val dataPoints = session.soilDataPoints.map { (location, data) ->
                SoilDataPointEntity.fromDomain(session.id, location, data)
            }
            if (dataPoints.isNotEmpty()) {
                soilDataPointDao.insertSoilDataPoints(dataPoints)
                Log.d("SessionRepository", "✓ Saved ${dataPoints.size} soil data points")
            }

            true
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error saving session: ${e.message}", e)
            false
        }
    }

    /**
     * Load a complete session from the database with all its soil data points
     */
    suspend fun loadSession(sessionId: String): SavedSession? {
        return try {
            val sessionEntity = sessionDao.getSessionById(sessionId)
            if (sessionEntity != null) {
                // Load all soil data points for this session
                val dataPoints = soilDataPointDao.getSoilDataPointsBySession(sessionId)

                // Convert to domain objects
                val soilDataMap = dataPoints.associate { point ->
                    Pair(point.latitude, point.longitude) to point.toDomain()
                }

                val session = sessionEntity.toDomainWithData(soilDataMap)
                Log.d("SessionRepository", "✓ Loaded session: ${session.sessionName} with ${soilDataMap.size} data points")
                session
            } else {
                Log.w("SessionRepository", "Session not found: $sessionId")
                null
            }
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error loading session: ${e.message}", e)
            null
        }
    }

    /**
     * Get all saved sessions from the database
     */
    suspend fun getAllSessions(): List<SavedSession> {
        return try {
            val sessionEntities = sessionDao.getAllSessions()
            val sessions = mutableListOf<SavedSession>()

            for (sessionEntity in sessionEntities) {
                // Load soil data points for each session
                val dataPoints = soilDataPointDao.getSoilDataPointsBySession(sessionEntity.id)
                val soilDataMap = dataPoints.associate { point ->
                    Pair(point.latitude, point.longitude) to point.toDomain()
                }

                sessions.add(sessionEntity.toDomainWithData(soilDataMap))
            }

            Log.d("SessionRepository", "✓ Loaded ${sessions.size} sessions from database")
            sessions
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting all sessions: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Delete a session and all its associated soil data points
     */
    suspend fun deleteSession(sessionId: String): Boolean {
        return try {
            sessionDao.deleteSessionById(sessionId)
            Log.d("SessionRepository", "✓ Deleted session: $sessionId")
            true
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting session: ${e.message}", e)
            false
        }
    }

    /**
     * Update an existing session
     */
    suspend fun updateSession(session: SavedSession): Boolean {
        return try {
            val sessionEntity = SessionEntity.fromDomain(session)
            sessionDao.updateSession(sessionEntity)

            // Update soil data points
            soilDataPointDao.deleteSoilDataPointsBySession(session.id)
            val dataPoints = session.soilDataPoints.map { (location, data) ->
                SoilDataPointEntity.fromDomain(session.id, location, data)
            }
            if (dataPoints.isNotEmpty()) {
                soilDataPointDao.insertSoilDataPoints(dataPoints)
            }

            Log.d("SessionRepository", "✓ Updated session: ${session.sessionName}")
            true
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error updating session: ${e.message}", e)
            false
        }
    }

    /**
     * Get the count of saved sessions
     */
    suspend fun getSessionCount(): Int {
        return try {
            sessionDao.getSessionCount()
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error getting session count: ${e.message}", e)
            0
        }
    }

    /**
     * Check if a session exists in the database
     */
    suspend fun sessionExists(sessionId: String): Boolean {
        return try {
            sessionDao.getSessionById(sessionId) != null
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error checking if session exists: ${e.message}", e)
            false
        }
    }

    /**
     * Delete all sessions from the database
     */
    suspend fun deleteAllSessions(): Boolean {
        return try {
            val sessions = sessionDao.getAllSessions()
            sessions.forEach { session ->
                sessionDao.deleteSession(session)
            }
            Log.d("SessionRepository", "✓ Deleted all sessions")
            true
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error deleting all sessions: ${e.message}", e)
            false
        }
    }
}

