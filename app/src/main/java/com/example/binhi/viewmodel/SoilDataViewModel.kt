package com.example.binhi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.example.binhi.data.SoilData
import com.example.binhi.data.SavedSession
import com.example.binhi.data.database.SessionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel to manage soil data storage per map location with database persistence
 * Uses observable state to ensure Compose reacts to changes
 * Integrates with Room database for persistent storage
 */
class SoilDataViewModel(
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {

    /**
     * Map of LatLng coordinates to their stored SoilData
     * This is wrapped in mutableStateOf to notify Compose of changes
     */
    private var soilDataStorage by mutableStateOf(mutableMapOf<LatLng, SoilData>())

    /**
     * Track the total number of dots that need data
     * This is set by the UI layer when dots are generated
     */
    var totalDotsCount by mutableStateOf(0)

    /**
     * Store all saved sessions (loaded from database)
     */
    var savedSessions by mutableStateOf(listOf<SavedSession>())

    /**
     * Track if data is loading from database
     */
    var isLoadingFromDatabase by mutableStateOf(false)

    /**
     * Store temporary polygon state for navigation
     * Used to preserve polygon center and rotation when navigating to MappingInfo
     */
    var tempPolygonCenter by mutableStateOf<LatLng?>(null)
    var tempPolygonRotation by mutableStateOf(0f)

    /**
     * Store the currently loaded session
     * Used to retrieve landArea, length, width in downstream screens
     */
    var currentLoadedSession by mutableStateOf<SavedSession?>(null)

    /**
     * Derived state that checks if all dots have saved soil data
     * Returns true only when:
     * - totalDotsCount > 0
     * - Number of saved dots equals total dots count
     */
    val allDotsComplete by derivedStateOf {
        totalDotsCount > 0 && soilDataStorage.size == totalDotsCount
    }

    /**
     * Initialize ViewModel by loading sessions from database
     */
    init {
        loadAllSessionsFromDatabase()
    }

    /**
     * Load all sessions from the database
     */
    fun loadAllSessionsFromDatabase() {
        if (sessionRepository == null) {
            Log.w("SoilDataViewModel", "SessionRepository not initialized, using in-memory storage only")
            return
        }

        viewModelScope.launch {
            try {
                isLoadingFromDatabase = true
                val sessions = sessionRepository.getAllSessions()
                savedSessions = sessions
                Log.d("SoilDataViewModel", "✓ Loaded ${sessions.size} sessions from database")
                isLoadingFromDatabase = false
            } catch (e: Exception) {
                Log.e("SoilDataViewModel", "Error loading sessions from database: ${e.message}", e)
                isLoadingFromDatabase = false
            }
        }
    }

    /**
     * Store soil data for a specific location
     * @param location The LatLng coordinate where data was collected
     * @param data The SoilData to store
     * @return true if data was successfully stored, false if invalid
     */
    fun saveSoilData(location: LatLng, data: SoilData): Boolean {
        return if (data.isValid()) {
            // Create a new map to trigger recomposition
            soilDataStorage = soilDataStorage.toMutableMap().apply {
                this[location] = data
            }
            true
        } else {
            false
        }
    }

    /**
     * Retrieve soil data for a specific location
     * @param location The LatLng coordinate to retrieve data for
     * @return SoilData if found, null otherwise
     */
    fun getSoilData(location: LatLng): SoilData? {
        return soilDataStorage[location]
    }

    /**
     * Check if a location has stored soil data
     * @param location The LatLng coordinate to check
     * @return true if data exists for this location
     */
    fun hasSoilData(location: LatLng): Boolean {
        return soilDataStorage.containsKey(location)
    }

    /**
     * Get all stored soil data locations
     * @return Set of LatLng coordinates that have stored data
     */
    fun getAllStoredLocations(): Set<LatLng> {
        return soilDataStorage.keys.toSet()
    }

    /**
     * Delete soil data for a specific location
     * @param location The LatLng coordinate to delete data for
     */
    fun deleteSoilData(location: LatLng) {
        soilDataStorage = soilDataStorage.toMutableMap().apply {
            this.remove(location)
        }
    }

    /**
     * Clear all stored soil data
     */
    fun clearAllData() {
        soilDataStorage = mutableMapOf()
    }

    /**
     * Get count of locations with stored data
     */
    fun getStoredDataCount(): Int {
        return soilDataStorage.size
    }

    /**
     * Get progress percentage (0-100)
     * @return Percentage of dots that have data
     */
    fun getCompletionPercentage(): Int {
        return if (totalDotsCount > 0) {
            (soilDataStorage.size * 100) / totalDotsCount
        } else {
            0
        }
    }

    /**
     * Save current session with all map data and soil data
     * Also persists to database for permanent storage
     * @param sessionName Name for the saved session
     * @param landArea Land area in square meters
     * @param length Field length in meters
     * @param width Field width in meters
     * @param crop Crop name
     * @param polygonCenter Center of the polygon
     * @param rotation Current rotation of the polygon
     * @param mapType Current map type (SATELLITE or NORMAL)
     * @return The saved session
     */
    fun saveCurrentSession(
        sessionName: String,
        landArea: Double,
        length: Double,
        width: Double,
        crop: String,
        polygonCenter: LatLng,
        rotation: Float,
        mapType: String,
        cameraZoom: Float = 15f
    ): SavedSession {
        // Convert LatLng keys to Pair for serialization
        val soilDataMap = soilDataStorage.mapKeys { (latLng, _) ->
            SavedSession.latLngToPair(latLng)
        }

        val session = SavedSession(
            sessionName = sessionName,
            landArea = landArea,
            length = length,
            width = width,
            crop = crop,
            polygonCenter = Pair(polygonCenter.latitude, polygonCenter.longitude),
            rotation = rotation,
            mapType = mapType,
            cameraZoom = cameraZoom,
            totalDots = totalDotsCount,
            soilDataPoints = soilDataMap
        )

        // Add to saved sessions list in memory
        savedSessions = savedSessions + session

        // Persist to database
        if (sessionRepository != null) {
            viewModelScope.launch {
                try {
                    val success = sessionRepository.saveSession(session)
                    if (success) {
                        Log.d("SoilDataViewModel", "✓ Session persisted to database: $sessionName")
                    } else {
                        Log.e("SoilDataViewModel", "Failed to persist session to database")
                    }
                } catch (e: Exception) {
                    Log.e("SoilDataViewModel", "Error persisting session to database: ${e.message}", e)
                }
            }
        }

        return session
    }

    /**
     * Load a saved session and restore its data
     * @param session The session to load
     */
    fun loadSession(session: SavedSession) {
        // Convert Pair keys back to LatLng
        val restoredData = session.soilDataPoints.mapKeys { (pair, _) ->
            SavedSession.pairToLatLng(pair)
        }.toMutableMap()

        soilDataStorage = restoredData
        totalDotsCount = session.totalDots
        currentLoadedSession = session  // Store the loaded session for downstream use
        Log.d("SoilDataViewModel", "✓ Session loaded: ${session.sessionName}, landArea=${session.landArea}, length=${session.length}, width=${session.width}")
    }

    /**
     * Get all saved sessions
     */
    fun getAllSavedSessions(): List<SavedSession> {
        return savedSessions
    }

    /**
     * Delete a saved session from memory and database
     */
    fun deleteSavedSession(sessionId: String) {
        savedSessions = savedSessions.filter { it.id != sessionId }

        // Delete from database
        if (sessionRepository != null) {
            viewModelScope.launch {
                try {
                    val success = sessionRepository.deleteSession(sessionId)
                    if (success) {
                        Log.d("SoilDataViewModel", "✓ Session deleted from database: $sessionId")
                    } else {
                        Log.e("SoilDataViewModel", "Failed to delete session from database")
                    }
                } catch (e: Exception) {
                    Log.e("SoilDataViewModel", "Error deleting session from database: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Update the name of a saved session
     */
    fun updateSessionName(sessionId: String, newName: String) {
        savedSessions = savedSessions.map { session ->
            if (session.id == sessionId) {
                session.copy(sessionName = newName)
            } else {
                session
            }
        }

        // Update in database
        if (sessionRepository != null) {
            viewModelScope.launch {
                try {
                    val session = savedSessions.find { it.id == sessionId }
                    if (session != null) {
                        val success = sessionRepository.updateSession(session)
                        if (success) {
                            Log.d("SoilDataViewModel", "✓ Session name updated in database: $sessionId -> $newName")
                        } else {
                            Log.e("SoilDataViewModel", "Failed to update session name in database")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SoilDataViewModel", "Error updating session name: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Get a saved session by ID
     */
    fun getSavedSession(sessionId: String): SavedSession? {
        return savedSessions.find { it.id == sessionId }
    }

    /**
     * Clear current session state (for viewing/editing a saved session)
     */
    fun clearCurrentSession() {
        soilDataStorage = mutableMapOf()
        totalDotsCount = 0
    }

    /**
     * Save temporary polygon state when navigating away from GetSoilData
     * @param center The polygon center (LatLng)
     * @param rotation The polygon rotation in degrees
     */
    fun saveTempPolygonState(center: LatLng, rotation: Float) {
        tempPolygonCenter = center
        tempPolygonRotation = rotation
        Log.d("SoilDataViewModel", "✓ Polygon state saved: center=$center, rotation=$rotation")
    }

    /**
     * Restore temporary polygon state when returning to GetSoilData
     * @return Pair of LatLng center and Float rotation, or null if not saved
     */
    fun restoreTempPolygonState(): Pair<LatLng, Float>? {
        return if (tempPolygonCenter != null) {
            Log.d("SoilDataViewModel", "✓ Polygon state restored: center=$tempPolygonCenter, rotation=$tempPolygonRotation")
            Pair(tempPolygonCenter!!, tempPolygonRotation)
        } else {
            null
        }
    }

    /**
     * Clear temporary polygon state
     */
    fun clearTempPolygonState() {
        tempPolygonCenter = null
        tempPolygonRotation = 0f
    }
}

