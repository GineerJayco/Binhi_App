package com.example.binhi

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.*
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import com.example.binhi.bluetooth.BluetoothClassicManager
import com.example.binhi.bluetooth.BluetoothPermissionHelper
import com.example.binhi.bluetooth.SoilSensorData
import com.example.binhi.data.SoilData
import com.example.binhi.viewmodel.SoilDataViewModel

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
    soilDataViewModel: SoilDataViewModel,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
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

    // Bluetooth state
    val bluetoothManager = remember { BluetoothClassicManager(context) }
    var bluetoothResponse by remember { mutableStateOf<SoilSensorData?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var isBluetoothLoading by remember { mutableStateOf(false) }
    var hasBluetoothPermission by remember {
        mutableStateOf(bluetoothManager.hasBluetoothPermissions())
    }

    // Soil data state for current dialog
    var currentSoilData by remember { mutableStateOf<SoilData?>(null) }
    var storedLocations by remember { mutableStateOf(setOf<LatLng>()) }
    var showSaveSuccessMessage by remember { mutableStateOf(false) }

    // Save session state
    var showSaveSessionDialog by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }
    var isSavingSession by remember { mutableStateOf(false) }
    var sessionSaveSuccess by remember { mutableStateOf(false) }

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

    // Update total dots count in ViewModel whenever dots change
    LaunchedEffect(dots.size) {
        soilDataViewModel.totalDotsCount = dots.size
        Log.d("SoilData", "Dots grid created: ${dots.size} dots")
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

    // Bluetooth permission launcher for multiple permissions
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            hasBluetoothPermission = allGranted
            if (!allGranted) {
                bluetoothResponse = SoilSensorData(
                    isError = true,
                    errorMessage = "Bluetooth permissions were denied"
                )
                showBluetoothDialog = true
            }
        }
    )

    var touchPosition by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var isDragging by remember { mutableStateOf(false) }
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }
    var isPolygonDragging by remember { mutableStateOf(false) }

    // Update stored locations whenever selectedDot changes or data is saved
    LaunchedEffect(selectedDot) {
        if (selectedDot != null && soilDataViewModel.hasSoilData(selectedDot!!) && currentSoilData == null) {
            // Load existing soil data when dot is selected
            currentSoilData = soilDataViewModel.getSoilData(selectedDot!!)
        }
    }

    // Show dialog when a dot is selected
    if (showDialog && selectedDot != null) {
        Dialog(
            onDismissRequest = {
                showDialog = false
                selectedDot = null
                currentSoilData = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sample Location",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latitude: ${decimalToDMS(selectedDot!!.latitude, true)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Longitude: ${decimalToDMS(selectedDot!!.longitude, false)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Show existing data if available
                    if (currentSoilData != null) {
                        Text(
                            text = "Stored Soil Data",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Green
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Display all soil data fields in a grid
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataRow("Nitrogen", "${currentSoilData!!.nitrogen}")
                            DataRow("Phosphorus", "${currentSoilData!!.phosphorus}")
                            DataRow("Potassium", "${currentSoilData!!.potassium}")
                            DataRow("pH Level", String.format("%.2f", currentSoilData!!.phLevel))
                            DataRow("Temperature", String.format("%.1f", currentSoilData!!.temperature) + "°C")
                            DataRow("Moisture", "${currentSoilData!!.moisture}%")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Show receive data button when no data exists
                        Text(
                            text = "No Data Stored",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            // Check permissions first
                            if (!hasBluetoothPermission) {
                                bluetoothPermissionLauncher.launch(
                                    BluetoothPermissionHelper.getRequiredPermissions()
                                )
                            } else {
                                // Permissions granted, proceed with Bluetooth communication
                                isBluetoothLoading = true
                                coroutineScope.launch {
                                    try {
                                        // Send READ command and receive data
                                        val response = bluetoothManager.sendCommandAndReceive("READ\n")
                                        // Parse the response
                                        bluetoothResponse = SoilSensorData.fromResponse(response)
                                    } catch (e: Exception) {
                                        bluetoothResponse = SoilSensorData(
                                            rawData = e.message ?: "Unknown error",
                                            isError = true,
                                            errorMessage = "Failed to receive data: ${e.message}"
                                        )
                                    } finally {
                                        isBluetoothLoading = false
                                        showBluetoothDialog = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBluetoothLoading
                    ) {
                        if (isBluetoothLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text("Receiving...")
                        } else {
                            Text("Receive Data")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            showDialog = false
                            selectedDot = null
                            currentSoilData = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Bluetooth Response Dialog
    if (showBluetoothDialog && bluetoothResponse != null) {
        Dialog(
            onDismissRequest = {
                showBluetoothDialog = false
                bluetoothResponse = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (bluetoothResponse!!.isError) "Error" else "Received Soil Data",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (bluetoothResponse!!.isError) Color.Red else Color.Green
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (bluetoothResponse!!.isError) {
                        Text(
                            text = bluetoothResponse!!.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Red
                        )
                    } else {
                        // Display all soil data fields
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataRow("Nitrogen", "${bluetoothResponse!!.nitrogen}")
                            DataRow("Phosphorus", "${bluetoothResponse!!.phosphorus}")
                            DataRow("Potassium", "${bluetoothResponse!!.potassium}")
                            DataRow("pH Level", String.format("%.2f", bluetoothResponse!!.phLevel))
                            DataRow("Temperature", String.format("%.1f", bluetoothResponse!!.temperature) + "°C")
                            DataRow("Moisture", "${bluetoothResponse!!.moisture}%")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Raw: ${bluetoothResponse!!.rawData}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!bluetoothResponse!!.isError && selectedDot != null) {
                        Button(
                            onClick = {
                                // Convert to SoilData and save
                                val soilData = bluetoothResponse!!.toSoilData()
                                if (soilData != null) {
                                    if (soilDataViewModel.saveSoilData(selectedDot!!, soilData)) {
                                        // Debug logging
                                        Log.d("SoilData", "✓ Data saved for dot: $selectedDot")
                                        Log.d("SoilData", "Total dots: ${soilDataViewModel.totalDotsCount}")
                                        Log.d("SoilData", "Saved dots: ${soilDataViewModel.getStoredDataCount()}")
                                        Log.d("SoilData", "All complete? ${soilDataViewModel.allDotsComplete}")

                                        currentSoilData = soilData
                                        showSaveSuccessMessage = true
                                        // Auto-close success message after 2 seconds
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            showSaveSuccessMessage = false
                                            showBluetoothDialog = false
                                            bluetoothResponse = null
                                            showDialog = false
                                            selectedDot = null
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Data")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            showBluetoothDialog = false
                            bluetoothResponse = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Success message
    if (showSaveSuccessMessage) {
        Dialog(
            onDismissRequest = { showSaveSuccessMessage = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Data Saved Successfully!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Marker color changed to green",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Save Session Dialog
    if (showSaveSessionDialog) {
        Dialog(
            onDismissRequest = {
                if (!isSavingSession) {
                    showSaveSessionDialog = false
                    sessionName = ""
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Save Session",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Session info display
                    Text(
                        text = "Session Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataRow("Crop", crop ?: "N/A")
                        DataRow("Land Area", "$landArea m²")
                        DataRow("Field Size", "${length}m × ${width}m")
                        DataRow("Total Dots", "${soilDataViewModel.totalDotsCount}")
                        DataRow("Data Collected", "${soilDataViewModel.getStoredDataCount()} / ${soilDataViewModel.totalDotsCount}")
                        DataRow("Completion", "${soilDataViewModel.getCompletionPercentage()}%")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Session name input
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Session Name") },
                        placeholder = { Text("e.g., Field A - January 2026") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isSavingSession,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (sessionName.isNotBlank()) {
                                isSavingSession = true
                                coroutineScope.launch {
                                    try {
                                        val mapTypeStr = if (mapType == MapType.SATELLITE) "SATELLITE" else "NORMAL"
                                        soilDataViewModel.saveCurrentSession(
                                            sessionName = sessionName,
                                            landArea = landArea?.toDoubleOrNull() ?: 0.0,
                                            length = length?.toDoubleOrNull() ?: 0.0,
                                            width = width?.toDoubleOrNull() ?: 0.0,
                                            crop = crop ?: "Unknown",
                                            polygonCenter = polygonCenter,
                                            rotation = rotation,
                                            mapType = mapTypeStr,
                                            cameraZoom = cameraPositionState.position.zoom
                                        )
                                        Log.d("SaveSession", "✓ Session saved: $sessionName")
                                        sessionSaveSuccess = true
                                        kotlinx.coroutines.delay(1500)
                                        showSaveSessionDialog = false
                                        sessionName = ""
                                        sessionSaveSuccess = false
                                    } catch (e: Exception) {
                                        Log.e("SaveSession", "Error saving session: ${e.message}")
                                    } finally {
                                        isSavingSession = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = sessionName.isNotBlank() && !isSavingSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        if (isSavingSession) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSavingSession) "Saving..." else "Save Session")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showSaveSessionDialog = false
                            sessionName = ""
                        },
                        enabled = !isSavingSession,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
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
                val markerColor = if (soilDataViewModel.hasSoilData(dot)) {
                    BitmapDescriptorFactory.HUE_GREEN // Green for saved data
                } else {
                    BitmapDescriptorFactory.HUE_BLUE // Blue for unsaved
                }

                Marker(
                    state = MarkerState(position = dot),
                    icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                    onClick = {
                        selectedDot = dot
                        currentSoilData = null // Reset to load from ViewModel
                        showDialog = true
                        true
                    }
                )
            }
        }


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

        // My Location button
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
            modifier = Modifier.padding(25.dp).align(Alignment.TopEnd),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
        }

        // Save Data button (top right, below My Location)
        FloatingActionButton(
            onClick = {
                Log.d("GetSoilData", "Save Data clicked")
                showSaveSessionDialog = true
            },
            modifier = Modifier
                .padding(top = 95.dp, end = 25.dp)
                .align(Alignment.TopEnd),
            containerColor = Color(0xFFFF9800)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save Data",
                tint = Color.White
            )
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

        // Get Crop Recommendation button - always visible, enabled only when all dots are complete
        val isButtonEnabled = soilDataViewModel.allDotsComplete
        val savedDotsCount = soilDataViewModel.getStoredDataCount()
        val totalDots = soilDataViewModel.totalDotsCount

        Button(
            onClick = {
                Log.d("GetSoilData", "Get Crop Recommendation clicked - All ${soilDataViewModel.totalDotsCount} dots have data")
                navController.navigate("mapping_info")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.60f)
                .height(48.dp),
            shape = RoundedCornerShape(50.dp),
            enabled = isButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3),
                disabledContainerColor = Color.Gray
            )
        ) {
            Icon(
                imageVector = Icons.Default.Agriculture,
                contentDescription = "Get Recommendation",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Get Crop Recommendation",
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Helper composable to display a labeled data row
 */
@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}