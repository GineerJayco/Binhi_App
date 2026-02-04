package com.example.binhi.data

/**
 * Data class representing soil sensor data for a specific map location
 * Contains all environmental measurements from the soil sensor
 */
data class SoilData(
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int,
    val phLevel: Float,
    val temperature: Float,
    val moisture: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this soil data is valid (all values within reasonable ranges)
     */
    fun isValid(): Boolean {
        return nitrogen >= 0 && phosphorus >= 0 && potassium >= 0 &&
                phLevel > 0f && phLevel <= 14f &&
                temperature >= -40f && temperature <= 80f &&
                moisture >= 0 && moisture <= 100
    }

    override fun toString(): String {
        return "N: $nitrogen, P: $phosphorus, K: $potassium, pH: $phLevel, Temp: ${temperature}°C, Moisture: $moisture%"
    }
}

