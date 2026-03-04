package com.example.binhi.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val location: Location,
    val current: CurrentWeather
)

data class Location(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

data class CurrentWeather(
    @SerializedName("temp_c")
    val tempC: Double,
    @SerializedName("temp_f")
    val tempF: Double,
    @SerializedName("is_day")
    val isDay: Int,
    val condition: WeatherCondition,
    val wind_kph: Double,
    val wind_mph: Double,
    val humidity: Int,
    val feelslike_c: Double,
    val feelslike_f: Double,
    @SerializedName("uv_index")
    val uvIndex: Double,
    val pressure_mb: Double
)

data class WeatherCondition(
    val text: String,
    val icon: String,
    val code: Int
)

data class WeatherUIState(
    val temperature: Double = 0.0,
    val condition: String = "Loading...",
    val location: String = "",
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val isDay: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null
)
