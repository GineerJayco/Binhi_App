package com.example.binhi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.CameraPositionState
import kotlin.math.cos
import kotlin.math.pow

@Composable
fun MapScaleBar(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState
) {
    val density = LocalDensity.current
    val targetLatitude = cameraPositionState.position.target.latitude
    val zoomLevel = cameraPositionState.position.zoom

    // Define a few nice scale distances in meters
    val scaleDistances = listOf(1, 2, 3, 5 , 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000)

    val maxScaleWidthDp = 100.dp
    val maxScaleWidthPx = with(density) { maxScaleWidthDp.toPx() }

    val metersPerPixel = remember(targetLatitude, zoomLevel) {
        (156543.03392 * cos(targetLatitude * Math.PI / 180) / 2.0.pow(zoomLevel.toDouble())) / 2
    }

    val maxDistanceInMeters = metersPerPixel * maxScaleWidthPx

    // Find the best scale distance that fits within maxScaleWidthDp
    val scaleDistanceInMeters = scaleDistances.lastOrNull { it < maxDistanceInMeters } ?: scaleDistances.first()

    val scaleWidthPx = scaleDistanceInMeters / metersPerPixel
    val scaleWidthDp = with(density) { scaleWidthPx.toFloat().toDp() }

    val (displayDistance, unit) = if (scaleDistanceInMeters < 1000) {
        scaleDistanceInMeters to "m"
    } else {
        scaleDistanceInMeters / 1000 to "km"
    }

    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$displayDistance $unit", color = Color.Black)
        Canvas(modifier = Modifier
            .width(scaleWidthDp)
            .padding(top = 2.dp)) {
            val strokeWidth = 2.dp.toPx()
            drawLine(
                color = Color.Black,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = Color.Black,
                start = Offset(0f, -4.dp.toPx()),
                end = Offset(0f, 4.dp.toPx()),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = Color.Black,
                start = Offset(size.width, -4.dp.toPx()),
                end = Offset(size.width, 4.dp.toPx()),
                strokeWidth = strokeWidth
            )
        }
    }
}