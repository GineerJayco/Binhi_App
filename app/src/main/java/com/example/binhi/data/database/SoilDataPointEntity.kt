package com.example.binhi.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.binhi.data.SoilData

/**
 * Database entity for storing soil data points associated with a session
 * This entity is linked to a session via sessionId foreign key
 */
@Entity(
    tableName = "soil_data_points",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SoilDataPointEntity(
    @PrimaryKey(autoGenerate = true)
    val pointId: Int = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int,
    val phLevel: Float,
    val temperature: Float,
    val moisture: Int,
    val timestamp: Long
) {
    /**
     * Convert database entity to domain object
     */
    fun toDomain(): SoilData {
        return SoilData(
            nitrogen = nitrogen,
            phosphorus = phosphorus,
            potassium = potassium,
            phLevel = phLevel,
            temperature = temperature,
            moisture = moisture,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * Create database entity from domain object
         */
        fun fromDomain(sessionId: String, location: Pair<Double, Double>, data: SoilData): SoilDataPointEntity {
            return SoilDataPointEntity(
                sessionId = sessionId,
                latitude = location.first,
                longitude = location.second,
                nitrogen = data.nitrogen,
                phosphorus = data.phosphorus,
                potassium = data.potassium,
                phLevel = data.phLevel,
                temperature = data.temperature,
                moisture = data.moisture,
                timestamp = data.timestamp
            )
        }
    }
}

