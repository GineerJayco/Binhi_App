package com.example.binhi.bluetooth

import com.example.binhi.data.SoilData

/**
 * Data class representing Bluetooth sensor data received from ESP32
 * ESP32 sends data in format: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
 */
data class SoilSensorData(
    val nitrogen: Int = 0,
    val phosphorus: Int = 0,
    val potassium: Int = 0,
    val phLevel: Float = 0f,
    val temperature: Float = 0f,
    val moisture: Int = 0,
    val rawData: String = "",
    val isError: Boolean = false,
    val errorMessage: String = ""
) {
    companion object {
        /**
         * Parse raw response from ESP32 into SoilSensorData
         * Expected format: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
         * Returns error data if parsing fails
         */
        fun fromResponse(response: String): SoilSensorData {
            return try {
                // Check if response contains error indicator
                if (response.startsWith("Error:", ignoreCase = true)) {
                    return SoilSensorData(
                        rawData = response,
                        isError = true,
                        errorMessage = response
                    )
                }

                // Parse complete format: "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
                val npkRegex = """NPK\s*=\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)""".toRegex()
                val phRegex = """PH\s*=\s*([\d.]+)""".toRegex()
                val tempRegex = """TEMP\s*=\s*([\d.]+)""".toRegex()
                val moistRegex = """MOIST\s*=\s*(\d+)""".toRegex()

                val npkMatch = npkRegex.find(response)
                val phMatch = phRegex.find(response)
                val tempMatch = tempRegex.find(response)
                val moistMatch = moistRegex.find(response)

                // If we have at least NPK data, parse it
                if (npkMatch != null) {
                    val (n, p, k) = npkMatch.destructured
                    val ph = phMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                    val temp = tempMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                    val moist = moistMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                    SoilSensorData(
                        nitrogen = n.toInt(),
                        phosphorus = p.toInt(),
                        potassium = k.toInt(),
                        phLevel = ph,
                        temperature = temp,
                        moisture = moist,
                        rawData = response,
                        isError = false
                    )
                } else {
                    // Response doesn't match expected format
                    SoilSensorData(
                        rawData = response,
                        isError = true,
                        errorMessage = "Invalid format: $response"
                    )
                }
            } catch (e: Exception) {
                SoilSensorData(
                    rawData = response,
                    isError = true,
                    errorMessage = "Parse error: ${e.message}"
                )
            }
        }
    }

    /**
     * Convert SoilSensorData to SoilData for permanent storage
     */
    fun toSoilData(): SoilData? {
        return if (isError) {
            null
        } else {
            SoilData(
                nitrogen = nitrogen,
                phosphorus = phosphorus,
                potassium = potassium,
                phLevel = phLevel,
                temperature = temperature,
                moisture = moisture
            )
        }
    }

    override fun toString(): String {
        return if (isError) {
            errorMessage
        } else {
            "N: $nitrogen, P: $phosphorus, K: $potassium, pH: $phLevel, Temp: ${temperature}°C, Moisture: $moisture%"
        }
    }
}

