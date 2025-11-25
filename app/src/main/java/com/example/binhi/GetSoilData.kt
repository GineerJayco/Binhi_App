package com.example.binhi

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.*
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch

// Utility function to convert decimal degrees to DMS format
private fun decimalToDMS(decimal: Double, isLatitude: Boolean): String {
    val direction = when {
        isLatitude && decimal >= 0 -> "N"
        isLatitude && decimal < 0 -> "S"
        !isLatitude && decimal >= 0 -> "E"
        else -> "W"
    }

    val absDecimal = abs(decimal)
    val degrees = absDecimal.toInt()
    val minutesDecimal = (absDecimal - degrees) * 60
    val minutes = minutesDecimal.toInt()
    val seconds = ((minutesDecimal - minutes) * 60)

    return String.format("%d° %d' %.4f\" %s", degrees, minutes, seconds, direction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetSoilData(
    navController: NavController,
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?,
) {
    val dumaguete = LatLng(9.3093, 123.308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dumaguete, 15f)
    }
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var showMyLocation by remember { mutableStateOf(true) }
    var polygonCenter by remember { mutableStateOf(dumaguete) }
    var selectedDot by remember { mutableStateOf<LatLng?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Calculate dots based on area with fixed minimum spacing
    val area = landArea?.toDoubleOrNull() ?: 0.0
    val minimumSpacing = 5.0 // minimum 5 meters between dots
    val dotSpacing = max(minimumSpacing, sqrt(area / 100.0)) // Ensure minimum spacing and more dots

    // State for dots with improved grid calculation
    val dots = remember(polygonCenter, rotation, length, width, dotSpacing) {
        val lengthInMeters = length?.toDoubleOrNull() ?: 0.0
        val widthInMeters = width?.toDoubleOrNull() ?: 0.0
        if (lengthInMeters <= 0 || widthInMeters <= 0) return@remember emptyList()

        // Calculate number of dots that can fit in each dimension
        val numDotsLength = max((lengthInMeters / dotSpacing).toInt(), 2)
        val numDotsWidth = max((widthInMeters / dotSpacing).toInt(), 2)

        val dots = mutableListOf<LatLng>()

        // Create a grid of dots with proper spacing
        for (i in 0 until numDotsLength) {
            for (j in 0 until numDotsWidth) {
                // Calculate the position of each dot within the rectangle
                val x = if (numDotsWidth > 1) {
                    (j.toDouble() / (numDotsWidth - 1)) * widthInMeters - (widthInMeters / 2)
                } else {
                    0.0
                }

                val y = if (numDotsLength > 1) {
                    (i.toDouble() / (numDotsLength - 1)) * lengthInMeters - (lengthInMeters / 2)
                } else {
                    0.0
                }

                // Rotate the point
                val angleRad = Math.toRadians(rotation.toDouble())
                val rotatedX = x * cos(angleRad) - y * sin(angleRad)
                val rotatedY = x * sin(angleRad) + y * cos(angleRad)

                // Convert to LatLng
                val centerLatRad = Math.toRadians(polygonCenter.latitude)
                val latOffset = rotatedY / 111132.0 // Convert meters to degrees of latitude
                val lonOffset = rotatedX / (111320.0 * cos(centerLatRad)) // Convert meters to degrees of longitude

                dots.add(LatLng(
                    polygonCenter.latitude + latOffset,
                    polygonCenter.longitude + lonOffset
                ))
            }
        }
        dots
    }

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
                                Log.e("VisualizeLA", "Failed to get location")
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("VisualizeLA", "SecurityException: ${e.message}")
                    }
                }
            } else {
                showMyLocation = false
                // Handle permission denial
            }
        }
    )

    var touchPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var isDragging by remember { mutableStateOf(false) }
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }
    var isPolygonDragging by remember { mutableStateOf(false) }

    // Show dialog when a dot is selected
    if (showDialog && selectedDot != null) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                selectedDot = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Coordinates",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latitude: ${decimalToDMS(selectedDot!!.latitude, true)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Longitude: ${decimalToDMS(selectedDot!!.longitude, false)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Handle receiving data
                            // You can add your data receiving logic here
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Receive Data")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            showDialog = false
                            selectedDot = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        lastDragPosition = offset
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
                            val currentPosition = lastDragPosition + Offset(dragAmount.x, dragAmount.y)
                            lastDragPosition = currentPosition

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
        val lengthInMeters = length?.toDoubleOrNull() ?: 0.0
        val widthInMeters = width?.toDoubleOrNull() ?: 0.0

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
            ),
            onMapClick = { // Clear selection when clicking on the map
                selectedDot = null
                showDialog = false
            }
        ) {
            if (polygonPoints.isNotEmpty()) {
                Polygon(
                    points = polygonPoints,
                    fillColor = Color.Red.copy(alpha = 0.5f),
                    strokeColor = Color.Red,
                    strokeWidth = 5f
                )
            }

            // Draw all dots with click handling
            dots.forEach { dot ->
                Marker(
                    state = MarkerState(position = dot),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    onClick = {
                        selectedDot = dot
                        showDialog = true
                        true
                    }
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
                                    Log.e("VisualizeLA", "Failed to get location")
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("VisualizeLA", "SecurityException: ${e.message}")
                        }
                    }
                }
            },
            modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
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
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.White)
            }
            IconButton(onClick = {
                val centerLatRad = Math.toRadians(polygonCenter.latitude)
                val lonOffset = moveDistance / (111320.0 * cos(centerLatRad))
                polygonCenter = LatLng(polygonCenter.latitude, polygonCenter.longitude + lonOffset)
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.White)
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
    }
}
