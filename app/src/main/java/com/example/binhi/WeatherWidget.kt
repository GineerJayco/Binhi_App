package com.example.binhi

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.binhi.data.WeatherRepository
import com.example.binhi.data.WeatherUIState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    var weatherState by remember { mutableStateOf<WeatherUIState>(WeatherUIState()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun fallbackWeather() {
        scope.launch(Dispatchers.IO) {
            try {
                // Fetch weather for default location (Philippines)
                val response = WeatherRepository.getWeatherByLocation("Manila, Philippines")
                weatherState = WeatherUIState(
                    temperature = response.current.tempC,
                    condition = response.current.condition.text,
                    location = "${response.location.name}, ${response.location.region}",
                    humidity = response.current.humidity,
                    windSpeed = response.current.wind_kph,
                    isDay = response.current.isDay == 1,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                weatherState = weatherState.copy(
                    isLoading = false,
                    error = "Unable to load weather"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                // Get user's current location using callback
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                // Fetch weather for user's current location
                                val response = WeatherRepository.getWeatherByCoordinates(
                                    location.latitude,
                                    location.longitude
                                )
                                weatherState = WeatherUIState(
                                    temperature = response.current.tempC,
                                    condition = response.current.condition.text,
                                    location = "${response.location.name}, ${response.location.country}",
                                    humidity = response.current.humidity,
                                    windSpeed = response.current.wind_kph,
                                    isDay = response.current.isDay == 1,
                                    isLoading = false,
                                    error = null
                                )
                            } catch (e: Exception) {
                                weatherState = weatherState.copy(
                                    isLoading = false,
                                    error = "Unable to load weather"
                                )
                            }
                        }
                    } else {
                        fallbackWeather()
                    }
                }.addOnFailureListener {
                    fallbackWeather()
                }
            } catch (e: Exception) {
                fallbackWeather()
            }
        } else {
            fallbackWeather()
        }
    }

    val roundedCornerShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (weatherState.isDay) {
                        listOf(
                            Color(0xFF87CEEB).copy(alpha = 0.9f),
                            Color(0xFFE0F6FF).copy(alpha = 0.85f)
                        )
                    } else {
                        listOf(
                            Color(0xFF1a1a2e).copy(alpha = 0.9f),
                            Color(0xFF16213e).copy(alpha = 0.85f)
                        )
                    }
                ),
                shape = roundedCornerShape
            )
            .border(
                width = 2.dp,
                color = if (weatherState.isDay) Color(0xFF64B5F6) else Color(0xFF0d47a1),
                shape = roundedCornerShape
            )
            .padding(24.dp)
    ) {
        AnimatedVisibility(
            visible = weatherState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = if (weatherState.isDay) Color(0xFF1976D2) else Color(0xFF64B5F6),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = !weatherState.isLoading && weatherState.error == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Location
                Text(
                    text = weatherState.location,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (weatherState.isDay) Color(0xFF01579b) else Color(0xFFB3E5FC),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Temperature Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (weatherState.isDay) Color.White.copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Weather Icon",
                            tint = if (weatherState.isDay) Color(0xFF1976D2) else Color(0xFF64B5F6),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = String.format("%.1f°C", weatherState.temperature),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weatherState.isDay) Color(0xFF01579b) else Color(0xFFE1F5FE)
                            )
                            Text(
                                text = weatherState.condition,
                                fontSize = 11.sp,
                                color = if (weatherState.isDay) Color(0xFF0277BD) else Color(0xFF81D4FA),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Additional Info Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (weatherState.isDay) Color.White.copy(alpha = 0.25f)
                            else Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Humidity
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (weatherState.isDay) Color(0xFFB3E5FC).copy(alpha = 0.4f)
                                else Color(0xFF01579b).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Humidity",
                            tint = if (weatherState.isDay) Color(0xFF0288D1) else Color(0xFF4FC3F7),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${weatherState.humidity}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weatherState.isDay) Color(0xFF01579b) else Color(0xFFE1F5FE)
                        )
                        Text(
                            text = "Humidity",
                            fontSize = 8.sp,
                            color = if (weatherState.isDay) Color(0xFF0277BD) else Color(0xFF81D4FA)
                        )
                    }

                    // Wind Speed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (weatherState.isDay) Color(0xFFB3E5FC).copy(alpha = 0.4f)
                                else Color(0xFF01579b).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Wind",
                            tint = if (weatherState.isDay) Color(0xFF0288D1) else Color(0xFF4FC3F7),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = String.format("%.1f", weatherState.windSpeed),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (weatherState.isDay) Color(0xFF01579b) else Color(0xFFE1F5FE)
                        )
                        Text(
                            text = "km/h",
                            fontSize = 8.sp,
                            color = if (weatherState.isDay) Color(0xFF0277BD) else Color(0xFF81D4FA)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !weatherState.isLoading && weatherState.error != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = weatherState.error ?: "Unknown error",
                fontSize = 11.sp,
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
