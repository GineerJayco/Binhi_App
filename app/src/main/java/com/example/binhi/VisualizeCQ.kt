package com.example.binhi

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlin.math.*
import com.example.binhi.utils.convertToDMS

fun isPointInsidePolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if ((polygon[i].longitude > point.longitude) != (polygon[j].longitude > point.longitude) &&
            point.latitude < (polygon[j].latitude - polygon[i].latitude) *
            (point.longitude - polygon[i].longitude) /
            (polygon[j].longitude - polygon[i].longitude) + polygon[i].latitude
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * Get the scale factor for crop icons based on crop type.
 * Different crops can have different icon sizes.
 *
 * Group 1 (Small icons): corn, sweet potato, cassava - scale factor 0.3
 * Group 2 (Large icons): banan, coconut - scale factor 0.5
 */
private fun getIconScaleFactor(cropType: String): Float {
    return when (cropType.lowercase()) {
        "corn", "sweet potato", "cassava" -> 0.3f      // Smaller size
        "banana", "coconut" -> 0.6f                      // Larger size
        else -> 0.4f                                     // Default size for other crops
    }
}

/**
 * Create and cache crop icon descriptors to avoid repeated bitmap generation.
 * Uses a mutable map to store generated bitmaps keyed by crop type.
 */
private val iconCache = mutableMapOf<String, BitmapDescriptor>()

/**
 * Get or create a BitmapDescriptor for a crop type with proper scaling.
 * Results are cached to avoid repeated drawable scaling and bitmap creation.
 */
private fun getCropIcon(
    context: android.content.Context,
    cropType: String
): BitmapDescriptor {
    // Check cache first
    val cacheKey = cropType.lowercase()
    if (iconCache.containsKey(cacheKey)) {
        return iconCache[cacheKey]!!
    }

    // Create new icon if not cached
    val icon = CropData.crops[cropType]?.let { cropData ->
        val drawable = ContextCompat.getDrawable(context, cropData.iconResource)!!
        val scaleFactor = getIconScaleFactor(cropType)
        val scaledWidth = (drawable.intrinsicWidth * scaleFactor).toInt()
        val scaledHeight = (drawable.intrinsicHeight * scaleFactor).toInt()
        drawable.setBounds(0, 0, scaledWidth, scaledHeight)
        val bitmap = createBitmap(
            width = scaledWidth,
            height = scaledHeight,
            config = Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

    // Store in cache
    iconCache[cacheKey] = icon
    return icon
}

private fun calculateCropPositions(
    polygonPoints: List<LatLng>,
    cropType: String?,
    estimatedQuantity: Int
): List<LatLng> {
    if (polygonPoints.isEmpty() || cropType == null || estimatedQuantity <= 0) return emptyList()

    // Calculate the dimensions of the plot
    val minLat = polygonPoints.minOf { it.latitude }
    val maxLat = polygonPoints.maxOf { it.latitude }
    val minLng = polygonPoints.minOf { it.longitude }
    val maxLng = polygonPoints.maxOf { it.longitude }

    val positions = mutableListOf<LatLng>()

    // Create a grid based on the requested quantity
    // Calculate how many crops we can fit per row and how many rows we need
    val cropsPerRow = max(1, sqrt(estimatedQuantity.toDouble()).toInt())
    val numRows = (estimatedQuantity + cropsPerRow - 1) / cropsPerRow  // Ceiling division

    // Calculate the spacing needed to fit all crops within the polygon
    val actualWidth = maxLng - minLng
    val actualHeight = maxLat - minLat

    // Adjust spacing to fit the requested quantity within available space
    val effectiveColSpacing = if (cropsPerRow > 1) actualWidth / cropsPerRow else actualWidth
    val effectiveRowSpacing = if (numRows > 1) actualHeight / numRows else actualHeight

    // Place crops at the center of each grid cell
    var cropsPlaced = 0
    for (row in 0 until numRows) {
        for (col in 0 until cropsPerRow) {
            if (cropsPlaced >= estimatedQuantity) break

            // Calculate center position of this grid cell
            val cellCenterLat = minLat + (row + 0.5) * effectiveRowSpacing
            val cellCenterLng = minLng + (col + 0.5) * effectiveColSpacing

            val position = LatLng(cellCenterLat, cellCenterLng)

            // Only add position if it's inside the polygon
            if (isPointInsidePolygon(position, polygonPoints)) {
                positions.add(position)
                cropsPlaced++
            }
        }
    }

    return positions
}

private fun transformCropPositions(
    baselineCropPositions: List<LatLng>,
    oldCenter: LatLng,
    oldRotation: Float,
    newCenter: LatLng,
    newRotation: Float
): List<LatLng> {
    if (baselineCropPositions.isEmpty()) return emptyList()

    val rotationDelta = newRotation - oldRotation
    val angleRad = Math.toRadians(rotationDelta.toDouble())

    return baselineCropPositions.map { position ->
        // Convert lat/lng to meters relative to old center
        val oldCenterLatRad = Math.toRadians(oldCenter.latitude)

        // Calculate relative position in meters
        val deltaLat = (position.latitude - oldCenter.latitude) * 111132.0
        val deltaLng = (position.longitude - oldCenter.longitude) * 111320.0 * cos(oldCenterLatRad)

        // Apply rotation transformation
        val rotatedDeltaX = deltaLng * cos(angleRad) - deltaLat * sin(angleRad)
        val rotatedDeltaY = deltaLng * sin(angleRad) + deltaLat * cos(angleRad)

        // Convert back to lat/lng relative to new center
        val newCenterLatRad = Math.toRadians(newCenter.latitude)
        val newLat = newCenter.latitude + rotatedDeltaY / 111132.0
        val newLng = newCenter.longitude + rotatedDeltaX / (111320.0 * cos(newCenterLatRad))

        LatLng(newLat, newLng)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizeCQ(
    navController: NavController,
    crop: String?,
    cropQuantity: String?,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    var showDetails by remember { mutableStateOf(false) }
    val dumaguete = LatLng(9.3093, 123.308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dumaguete, 15f)
    }
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var showMyLocation by remember { mutableStateOf(true) }
    var polygonCenter by remember { mutableStateOf(dumaguete) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Auto-fetch location on screen load
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            try {
                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(ContextCompat.getMainExecutor(context)) { task ->
                    if (task.isSuccessful && task.result != null) {
                        val result = LatLng(task.result.latitude, task.result.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(result, 15f)
                        polygonCenter = result
                        Log.d("VisualizeCQ", "✓ Location auto-fetched: $result")
                    } else {
                        Log.w("VisualizeCQ", "Failed to auto-fetch location, using default")
                    }
                }
            } catch (e: SecurityException) {
                Log.e("VisualizeCQ", "SecurityException during auto-fetch: ${e.message}")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            hasLocationPermission = isGranted
            if (isGranted) {
                showMyLocation = true
                coroutineScope.launch {
                    try {
                        val locationResult = fusedLocationClient.lastLocation
                        locationResult.addOnCompleteListener(ContextCompat.getMainExecutor(context)) {
                            if(it.isSuccessful && it.result != null) {
                                val result = LatLng(it.result.latitude, it.result.longitude)
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(result, 15f)
                                polygonCenter = result
                            } else {
                                Log.e("VisualizeCQ", "Failed to get location")
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("VisualizeCQ", "SecurityException: ${e.message}")
                    }
                }
            } else {
                showMyLocation = false
                // Handle permission denial
            }
        }
    )

    var isPolygonDragging by remember { mutableStateOf(false) }
    var selectedMarkerPosition by remember { mutableStateOf<LatLng?>(null) }
    var showMarkerDialog by remember { mutableStateOf(false) }
    var showCropListDialog by remember { mutableStateOf(false) }
    var cropLocationsList by remember { mutableStateOf<List<Pair<Int, LatLng>>>(emptyList()) }
    var baselineCropPositions by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var lastPolygonCenter by remember { mutableStateOf(dumaguete) }
    var lastRotation by remember { mutableFloatStateOf(0f) }
    var radarScale by remember { mutableFloatStateOf(1f) }
    var radarAlpha by remember { mutableFloatStateOf(1f) }
    var showCropNavigator by remember { mutableStateOf(false) }
    var currentCropIndex by remember { mutableStateOf(0) }

    // Pagination state for rendering markers efficiently
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 50  // Number of markers to display per page

    // Use Animatable for radar effect animation
    val radarScaleAnimatable = remember { Animatable(1f) }
    val radarAlphaAnimatable = remember { Animatable(1f) }

    LaunchedEffect(selectedMarkerPosition) {
        if (selectedMarkerPosition != null) {
            // Start animation when marker is selected
            try {
                radarScaleAnimatable.animateTo(
                    targetValue = 3f,
                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                )
            } catch (e: Exception) {
                Log.e("VisualizeCQ", "Animation error: ${e.message}")
            }

            try {
                radarAlphaAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                )
            } catch (e: Exception) {
                Log.e("VisualizeCQ", "Animation error: ${e.message}")
            }
        } else {
            // Reset animation when marker is deselected
            radarScaleAnimatable.snapTo(1f)
            radarAlphaAnimatable.snapTo(1f)
        }
    }

    radarScale = radarScaleAnimatable.value
    radarAlpha = radarAlphaAnimatable.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        isPolygonDragging = true
                    },
                    onDragEnd = {
                        isPolygonDragging = false
                    },
                    onDragCancel = {
                        isPolygonDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isPolygonDragging) {

                            val sensitivity = 0.25f
                            val centerLatRad = Math.toRadians(polygonCenter.latitude)
                            val latOffset = -dragAmount.y * sensitivity / 111132.0
                            val lonOffset = dragAmount.x * sensitivity / (111320.0 * cos(centerLatRad))

                            polygonCenter = LatLng(
                                polygonCenter.latitude + latOffset,
                                polygonCenter.longitude + lonOffset
                            )
                        }
                    }
                )
            }
    ) {
        val areaPerPlant = CropData.crops[crop]?.areaPerPlant ?: 0.0

        val quantity = cropQuantity?.toDoubleOrNull() ?: 0.0
        val estimatedLandArea = quantity * areaPerPlant
        val lengthInMeters = sqrt(estimatedLandArea)
        val widthInMeters = sqrt(estimatedLandArea)
        val estimatedQuantity = cropQuantity?.toDoubleOrNull()?.toInt() ?: 0

        val polygonPoints = remember(polygonCenter, rotation, lengthInMeters, widthInMeters) {
            if (lengthInMeters > 0 && widthInMeters > 0) {
                val halfLength = lengthInMeters / 2
                val halfWidth = widthInMeters / 2

                val corners = listOf(
                    Pair(-halfWidth, halfLength),
                    Pair(halfWidth, halfLength),
                    Pair(halfWidth, -halfLength),
                    Pair(-halfWidth, -halfLength)
                )

                val angleRad = Math.toRadians(rotation.toDouble())

                val rotatedCorners = corners.map { (x, y) ->
                    val rotatedX = x * cos(angleRad) - y * sin(angleRad)
                    val rotatedY = x * sin(angleRad) + y * cos(angleRad)
                    Pair(rotatedX, rotatedY)
                }

                val centerLatRad = Math.toRadians(polygonCenter.latitude)

                rotatedCorners.map { (x, y) ->
                    val latOffset = y / 111132.0
                    val lonOffset = x / (111320.0 * cos(centerLatRad))
                    LatLng(polygonCenter.latitude + latOffset, polygonCenter.longitude + lonOffset)
                }
            } else {
                emptyList()
            }
        }

        // Calculate baseline crop positions when polygon points change significantly (not just rotation)
        LaunchedEffect(lengthInMeters, widthInMeters, crop, estimatedQuantity) {
            if (polygonPoints.isNotEmpty()) {
                baselineCropPositions = calculateCropPositions(polygonPoints, crop, estimatedQuantity)
                lastPolygonCenter = polygonCenter
                lastRotation = rotation
            }
        }

        // Transform crop positions based on rotation and center movement
        val transformedCropPositions = remember(baselineCropPositions, polygonCenter, rotation, lastPolygonCenter, lastRotation) {
            if (baselineCropPositions.isNotEmpty()) {
                transformCropPositions(
                    baselineCropPositions,
                    lastPolygonCenter,
                    lastRotation,
                    polygonCenter,
                    rotation
                )
            } else {
                emptyList()
            }
        }

        // Calculate pagination values
        val totalPages = if (transformedCropPositions.isNotEmpty()) {
            (transformedCropPositions.size + pageSize - 1) / pageSize
        } else {
            1
        }

        // Ensure currentPage doesn't exceed available pages
        LaunchedEffect(totalPages) {
            if (currentPage >= totalPages) {
                currentPage = 0
            }
        }

        // Get paginated crop positions
        val startIdx = currentPage * pageSize
        val endIdx = minOf(startIdx + pageSize, transformedCropPositions.size)
        val paginatedCropPositions = if (transformedCropPositions.isNotEmpty()) {
            transformedCropPositions.subList(startIdx, endIdx)
        } else {
            emptyList()
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission && showMyLocation
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = !isPolygonDragging,
                zoomGesturesEnabled = !isPolygonDragging
            )
        ) {
            if (polygonPoints.isNotEmpty()) {
                key(polygonPoints) {  // Add key to force recomposition when polygon moves
                    Polygon(
                        points = polygonPoints,
                        fillColor = Color.Red.copy(alpha = 0.5f),
                        strokeColor = Color.Red,
                        strokeWidth = 5f
                    )

                    // Update crop locations list for the crop list dialog (showing all positions for reference)
                    LaunchedEffect(transformedCropPositions) {
                        cropLocationsList = transformedCropPositions.mapIndexed { index, position ->
                            Pair(index + 1, position)
                        }
                    }

                    // Cache the crop icon to avoid recreating it for each marker
                    val cachedIcon = crop?.let { getCropIcon(context, it) }
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

                    // Render only paginated crop markers (performance optimization)
                    paginatedCropPositions.forEachIndexed { paginatedIndex, position ->
                        val actualIndex = startIdx + paginatedIndex
                        key(actualIndex, position) {  // Use key for each marker to prevent unnecessary recompositions
                            val markerState = rememberMarkerState(position = position)

                            Marker(
                                state = markerState,
                                title = "${crop ?: "Crop"} ${actualIndex + 1}",
                                icon = cachedIcon,  // Use cached icon instead of recreating
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),  // Center the marker at the exact position
                                rotation = -cameraPositionState.position.bearing,
                                onClick = {
                                    selectedMarkerPosition = position
                                    showMarkerDialog = true
                                    true
                                }
                            )
                        }
                    }
                }
            }

            // Draw small animated radar circles for selected crop
            if (selectedMarkerPosition != null) {
                // Small outer pulsing circle
                Circle(
                    center = selectedMarkerPosition!!,
                    radius = 0.5 * radarScale,
                    fillColor = Color.Blue.copy(alpha = radarAlpha * 0.2f),
                    strokeColor = Color.Blue.copy(alpha = radarAlpha * 0.5f),
                    strokeWidth = 1f
                )

                // Small inner circle - only render if radarAlpha is high enough to be visible
                Circle(
                        center = selectedMarkerPosition!!,
                        radius = 0.3,
                        fillColor = Color.Blue,
                        strokeColor = Color.White,
                        strokeWidth = 0.5f
                )
            }
        }


        // Back button with immersive fullscreen support
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        FloatingActionButton(
            onClick = {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    hasLocationPermission = true
                    showMyLocation = true
                    coroutineScope.launch {
                        try {
                            val locationResult = fusedLocationClient.lastLocation
                            locationResult.addOnCompleteListener(ContextCompat.getMainExecutor(context)) {
                                if(it.isSuccessful && it.result != null) {
                                    val result = LatLng(it.result.latitude, it.result.longitude)
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(result, 15f)
                                    polygonCenter = result
                                } else {
                                    Log.e("VisualizeCQ", "Failed to get location")
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("VisualizeCQ", "SecurityException: ${e.message}")
                        }
                    }
                }
            },
            modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
        }

        FloatingActionButton(
            onClick = { showCropListDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .padding(top = 72.dp),
            containerColor = Color.White
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Crop Locations")
        }

        // Pagination Controls
        if (transformedCropPositions.isNotEmpty() && totalPages > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 128.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Page",
                        tint = if (currentPage > 0) Color.White else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${currentPage + 1}/${totalPages}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Page",
                        tint = if (currentPage < totalPages - 1) Color.White else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            IconButton(onClick = { rotation += 5f }) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right", tint = Color.White)
            }
            IconButton(onClick = { rotation -= 5f }) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left", tint = Color.White)
            }

            val moveDistance = 1.0 // meters
            IconButton(onClick = {
                val latOffset = moveDistance / 111132.0
                polygonCenter = LatLng(polygonCenter.latitude + latOffset, polygonCenter.longitude)
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
            }
            IconButton(onClick = {
                val latOffset = moveDistance / 111132.0
                polygonCenter = LatLng(polygonCenter.latitude - latOffset, polygonCenter.longitude)
            }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
            }
            IconButton(onClick = {
                val centerLatRad = Math.toRadians(polygonCenter.latitude)
                val lonOffset = moveDistance / (111320.0 * cos(centerLatRad))
                polygonCenter = LatLng(polygonCenter.latitude, polygonCenter.longitude - lonOffset)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.White)
            }
            IconButton(onClick = {
                val centerLatRad = Math.toRadians(polygonCenter.latitude)
                val lonOffset = moveDistance / (111320.0 * cos(centerLatRad))
                polygonCenter = LatLng(polygonCenter.latitude, polygonCenter.longitude + lonOffset)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.White)
            }
            IconButton(onClick = { showMyLocation = !showMyLocation }) {
                Icon(if (showMyLocation) Icons.Default.MyLocation else Icons.Default.LocationOff, contentDescription = "Toggle My Location Layer", tint = Color.White)
            }
            Text("Satellite", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = mapType == MapType.SATELLITE,
                onCheckedChange = { checked ->
                    mapType = if (checked) MapType.SATELLITE else MapType.NORMAL
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp, top = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            MapScaleBar(
                modifier = Modifier.padding(0.dp),
                cameraPositionState = cameraPositionState
            )
        }

        Button(
            onClick = { showDetails = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text("Visualization Details", fontSize = 12.sp)
        }

        if (showDetails) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.White)
            ) {
                VisualizationDetails2(
                    crop = crop,
                    cropQuantity = cropQuantity,
                    onClose = { showDetails = false }
                )
            }
        }

        if (showMarkerDialog && selectedMarkerPosition != null) {
            AlertDialog(
                onDismissRequest = { showMarkerDialog = false },
                title = { Text("Crop Location") },
                text = {
                    Column {
                        Text("Coordinates:")
                        Text(convertToDMS(selectedMarkerPosition!!.latitude, true))
                        Text(convertToDMS(selectedMarkerPosition!!.longitude, false))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMarkerDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showCropListDialog && cropLocationsList.isNotEmpty()) {
            // Crop Navigator Panel at the top
            if (showCropNavigator) {
                val (currentCropNumber, currentLocation) = cropLocationsList[currentCropIndex]
                val isDarkMode = isDarkModeState.value
                val backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
                val textColor = if (isDarkMode) Color.White else Color.Black
                val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.90f)
                        .padding(top = 16.dp)
                        .background(
                            color = backgroundColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with crop name and count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$crop ${currentCropNumber}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = if (isDarkMode) Color(0xFF404040) else Color(0xFFE8F5E9),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "${currentCropIndex + 1}/${cropLocationsList.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Divider
                        HorizontalDivider(
                            color = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Coordinates Card
                        Surface(
                            color = surfaceColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Latitude",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Latitude",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryTextColor,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = convertToDMS(currentLocation.latitude, true),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Longitude",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Longitude",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = secondaryTextColor,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = convertToDMS(currentLocation.longitude, false),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Navigation buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentCropIndex > 0) {
                                        currentCropIndex--
                                        val (_, location) = cropLocationsList[currentCropIndex]
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 50f)
                                        selectedMarkerPosition = location
                                    }
                                },
                                enabled = currentCropIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2),
                                    disabledContainerColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0)
                                ),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Prev", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            }

                            Button(
                                onClick = {
                                    if (currentCropIndex < cropLocationsList.size - 1) {
                                        currentCropIndex++
                                        val (_, location) = cropLocationsList[currentCropIndex]
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 50f)
                                        selectedMarkerPosition = location
                                    }
                                },
                                enabled = currentCropIndex < cropLocationsList.size - 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF388E3C),
                                    disabledContainerColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0)
                                ),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Text("Next", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Close button
                        Button(
                            onClick = { showCropNavigator = false },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(38.dp)
                                .fillMaxWidth(0.6f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0)
                            ),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to List", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = textColor)
                        }
                    }
                }
            } else {
                // Enhanced Crop List Dialog with dark mode support
                val isDarkMode = isDarkModeState.value
                val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White
                val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
                val textColor = if (isDarkMode) Color.White else Color.Black
                val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)

                AlertDialog(
                    onDismissRequest = { showCropListDialog = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f),
                    containerColor = backgroundColor,
                    titleContentColor = textColor,
                    textContentColor = textColor,
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Agriculture,
                                    contentDescription = "Crops",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Crop Locations",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = textColor
                                )
                            }
                            Surface(
                                color = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFE8F5E9),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${cropLocationsList.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cropLocationsList.size) { index ->
                                val (cropNumber, location) = cropLocationsList[index]
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentCropIndex = index
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 50f)
                                            selectedMarkerPosition = location
                                            showCropNavigator = true
                                        },
                                    color = surfaceColor,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Index badge
                                        Surface(
                                            color = Color(0xFF4CAF50),
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = cropNumber.toString(),
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 13.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Content
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$crop #$cropNumber",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = "Location",
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = convertToDMS(location.latitude, true),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = secondaryTextColor,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = "Coordinates",
                                                    tint = Color(0xFFD32F2F),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = convertToDMS(location.longitude, false),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = secondaryTextColor,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Arrow indicator
                                        Icon(
                                            Icons.AutoMirrored.Filled.NavigateNext,
                                            contentDescription = "View",
                                            tint = if (isDarkMode) Color(0xFF666666) else Color(0xFFBDBDBD),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showCropListDialog = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text("Close", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                    }
                )
            }
        }
    }
}