package com.example.binhi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.example.binhi.data.SoilData

/**
 * ViewModel to manage soil data storage per map location
 * Uses observable state to ensure Compose reacts to changes
 */
class SoilDataViewModel : ViewModel() {

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
     * Derived state that checks if all dots have saved soil data
     * Returns true only when:
     * - totalDotsCount > 0
     * - Number of saved dots equals total dots count
     */
    val allDotsComplete by derivedStateOf {
        totalDotsCount > 0 && soilDataStorage.size == totalDotsCount
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
}

