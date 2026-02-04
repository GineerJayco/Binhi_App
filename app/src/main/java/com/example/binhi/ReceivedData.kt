package com.example.binhi

data class ReceivedData(
    val soilPh: Double,
    val soilMoisture: Double,
    val temperature: Double,
    val humidity: Double,
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int
)
