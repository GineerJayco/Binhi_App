package com.example.binhi

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Polyline
import kotlin.math.*

/**
 * Direction class handles navigation guidance from a starting point to a destination point
 * with visual feedback via polyline and distance/duration information
 */
data class DirectionRoute(
    val startPoint: LatLng,
    val endPoint: LatLng,
    val distance: Double = 0.0, // in meters
    val duration: Long = 0L, // in seconds
    val polylinePoints: List<LatLng> = emptyList()
)

/**
 * Composable for the Direction UI controls
 * Shows direction info and clear button when direction is active
 */
@Composable
fun DirectionControls(
    currentUserLocation: LatLng?,
    availableDots: List<LatLng>,
    onDirectionCleared: () -> Unit,
    isDirectionActive: Boolean,
    directionStart: LatLng?,
    directionEnd: LatLng?,
    modifier: Modifier = Modifier
) {
    if (isDirectionActive && directionStart != null && directionEnd != null) {
        val distance = calculateDistance(directionStart, directionEnd)

        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Distance Info
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Distance",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Distance: ${String.format("%.1f", distance)}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1565C0),
                    fontSize = 10.sp
                )
            }

            // Clear Button
            IconButton(
                onClick = { onDirectionCleared() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Direction",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF6F6F)
                )
            }
        }
    }
}

/**
 * Composable that renders the direction polyline on the map
 * Shows a blue line from start to end point
 */
@Composable
fun DirectionPolyline(
    startPoint: LatLng,
    endPoint: LatLng
) {
    // Create intermediate points for a smooth polyline
    val polylinePoints = generateIntermediatePoints(startPoint, endPoint, 10)

    Polyline(
        points = polylinePoints,
        color = Color(0xFF2196F3), // Blue color for direction
        width = 8f,
        geodesic = true,
        clickable = false
    )

    // Optional: Add markers at start and end
    // These can be added separately in the main Google Map composable if needed
}

/**
 * Utility function to calculate distance between two points using Haversine formula
 */
fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val earthRadiusKm = 6371.0
    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLon = Math.toRadians(point2.longitude - point1.longitude)

    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1Rad) * cos(lat2Rad) *
            sin(deltaLon / 2) * sin(deltaLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distanceKm = earthRadiusKm * c

    return distanceKm * 1000 // Convert to meters
}

/**
 * Utility function to calculate walking time in minutes
 * Average walking speed: 1.4 m/s
 */
fun calculateWalkingTime(distanceMeters: Double): Int {
    val walkingSpeedMsPerSecond = 1.4
    val timeSeconds = distanceMeters / walkingSpeedMsPerSecond
    return (timeSeconds / 60).toInt() // Convert to minutes
}

/**
 * Generate intermediate points between two locations for smooth polyline
 * Uses linear interpolation for simplicity
 */
fun generateIntermediatePoints(
    start: LatLng,
    end: LatLng,
    pointCount: Int = 10
): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(start)

    for (i in 1 until pointCount) {
        val fraction = i.toDouble() / pointCount
        val lat = start.latitude + (end.latitude - start.latitude) * fraction
        val lon = start.longitude + (end.longitude - start.longitude) * fraction
        points.add(LatLng(lat, lon))
    }

    points.add(end)
    return points
}





