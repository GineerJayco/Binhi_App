package com.example.binhi

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        val posLatRad = Math.toRadians(position.latitude)
        val posLngRad = Math.toRadians(position.longitude)
        val oldCenterLngRad = Math.toRadians(oldCenter.longitude)

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

    // Animate radar when crop is selected
    LaunchedEffect(selectedMarkerPosition) {
        while (selectedMarkerPosition != null) {
            for (i in 0..100) {
                val progress = i / 100f
                radarScale = 1f + (progress * 2f)
                radarAlpha = 1f - progress
                kotlinx.coroutines.delay(20)
            }
        }
    }

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

                    // Update crop locations list for the crop list dialog
                    LaunchedEffect(transformedCropPositions) {
                        cropLocationsList = transformedCropPositions.mapIndexed { index, position ->
                            Pair(index + 1, position)
                        }
                    }

                    transformedCropPositions.forEachIndexed { index, position ->
                        val markerState = rememberMarkerState(position = position)
                        val icon = crop?.let { cropType ->
                            CropData.crops[cropType]?.let { cropData ->
                                val drawable = ContextCompat.getDrawable(context, cropData.iconResource)!!
                                // Scale down icons to 60% of original size
                                val scaleFactor = 0.4
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
                            }
                        } ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

                        Marker(
                            state = markerState,
                            title = "${crop ?: "Crop"} ${index + 1}",
                            icon = icon,
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

                // Small inner circle
                Circle(
                    center = selectedMarkerPosition!!,
                    radius = 0.2,
                    fillColor = Color.Blue,
                    strokeColor = Color.White,
                    strokeWidth = 0.5f
                )
            }
        }

        MapScaleBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            cameraPositionState = cameraPositionState
        )

        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {}
        )

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
            Icon(Icons.Default.List, contentDescription = "View Crop Locations")
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

        if (showCropListDialog) {
            AlertDialog(
                onDismissRequest = { showCropListDialog = false },
                title = { Text("Crop Locations") },
                text = {
                    LazyColumn {
                        items(cropLocationsList.size) { index ->
                            val (cropNumber, location) = cropLocationsList[index]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(Color.LightGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                                    .clickable {
                                        // Navigate to the crop location with super zoom
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 50f)
                                        selectedMarkerPosition = location
                                        showCropListDialog = false
                                    }
                            ) {
                                Text(
                                    text = "$crop $cropNumber",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = convertToDMS(location.latitude, true),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = convertToDMS(location.longitude, false),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCropListDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
