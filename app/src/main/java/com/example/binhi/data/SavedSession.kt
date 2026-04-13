package com.example.binhi.data

import com.google.android.gms.maps.model.LatLng

/**
 * Data class representing a complete saved session including map state and all soil data
 * This captures the entire UI state for restoration
 */
data class SavedSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionName: String,
    val landArea: Double,
    val length: Double,
    val width: Double,
    val crop: String,
    val polygonCenter: Pair<Double, Double>, // latitude, longitude
    val rotation: Float,
    val mapType: String, // SATELLITE or NORMAL
    val cameraZoom: Float = 15f, // Camera zoom level
    val totalDots: Int,
    val soilDataPoints: Map<Pair<Double, Double>, SoilData>, // LatLng as pair to pair (only dots with data)
    val allDotLocations: List<Pair<Double, Double>> = emptyList(), // All dots including blue points (no data)
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convert LatLng to Pair for storage
     */
    companion object {
        fun latLngToPair(latLng: LatLng): Pair<Double, Double> {
            return Pair(latLng.latitude, latLng.longitude)
        }

        fun pairToLatLng(pair: Pair<Double, Double>): LatLng {
            return LatLng(pair.first, pair.second)
        }
    }

    /**
     * Get formatted timestamp for display
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Get completion info
     */
    fun getCompletionInfo(): String {
        return "Crop: $crop | Area: $landArea m² | Dots: $totalDots | Data Points: ${soilDataPoints.size}"
    }
}

