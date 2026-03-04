package com.example.binhi.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("aqi") aqi: String = "no"
    ): WeatherResponse

    companion object {
        private const val BASE_URL = "https://api.weatherapi.com/v1/"

        fun create(): WeatherApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}

object WeatherRepository {
    private val apiService = WeatherApiService.create()
    // TODO: Replace with valid WeatherAPI.com API key from https://www.weatherapi.com/
    // Sign up for free and get your API key
    private val API_KEY = "56f25a17d2ce42e8a2314653260602"

    suspend fun getWeatherByCoordinates(latitude: Double, longitude: Double): WeatherResponse {
        return try {
            apiService.getCurrentWeather(
                apiKey = API_KEY,
                location = "$latitude,$longitude"
            )
        } catch (e: Exception) {
            // Fallback to mock data if API call fails
            getMockWeatherResponse()
        }
    }

    suspend fun getWeatherByLocation(locationName: String): WeatherResponse {
        return try {
            apiService.getCurrentWeather(
                apiKey = API_KEY,
                location = locationName
            )
        } catch (e: Exception) {
            // Fallback to mock data if API call fails
            getMockWeatherResponse()
        }
    }

    /**
     * Mock implementation for testing without valid API key
     * Returns sample weather data for Manila, Philippines
     * Remove this after setting up proper API key
     */
    private fun getMockWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            location = Location(
                name = "Manila",
                region = "National Capital Region",
                country = "Philippines",
                lat = 14.5995,
                lon = 120.9842
            ),
            current = CurrentWeather(
                tempC = 28.5,
                tempF = 83.3,
                isDay = 1,
                condition = WeatherCondition(
                    text = "Partly cloudy",
                    icon = "//cdn.weatherapi.com/weather/128x128/day/176.png",
                    code = 176
                ),
                wind_kph = 15.0,
                wind_mph = 9.3,
                humidity = 72,
                feelslike_c = 30.2,
                feelslike_f = 86.4,
                uvIndex = 6.0,
                pressure_mb = 1013.25
            )
        )
    }
}
