package com.example.binhi.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.binhi.data.SavedSession

/**
 * Database entity for storing SavedSession data
 * This entity stores the session metadata and is linked to SoilDataPointEntity entries
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val sessionName: String,
    val landArea: Double,
    val length: Double,
    val width: Double,
    val crop: String,
    val polygonCenterLatitude: Double,
    val polygonCenterLongitude: Double,
    val rotation: Float,
    val mapType: String,
    val cameraZoom: Float,
    val totalDots: Int,
    val timestamp: Long
) {
    /**
     * Convert database entity to domain object
     */
    fun toDomainWithData(soilDataPoints: Map<Pair<Double, Double>, com.example.binhi.data.SoilData>): SavedSession {
        return SavedSession(
            id = id,
            sessionName = sessionName,
            landArea = landArea,
            length = length,
            width = width,
            crop = crop,
            polygonCenter = Pair(polygonCenterLatitude, polygonCenterLongitude),
            rotation = rotation,
            mapType = mapType,
            cameraZoom = cameraZoom,
            totalDots = totalDots,
            soilDataPoints = soilDataPoints,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * Create database entity from domain object (without soil data)
         */
        fun fromDomain(session: SavedSession): SessionEntity {
            return SessionEntity(
                id = session.id,
                sessionName = session.sessionName,
                landArea = session.landArea,
                length = session.length,
                width = session.width,
                crop = session.crop,
                polygonCenterLatitude = session.polygonCenter.first,
                polygonCenterLongitude = session.polygonCenter.second,
                rotation = session.rotation,
                mapType = session.mapType,
                cameraZoom = session.cameraZoom,
                totalDots = session.totalDots,
                timestamp = session.timestamp
            )
        }
    }
}

