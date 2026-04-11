package com.example.binhi

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.binhi.bluetooth.BluetoothClassicManager
import com.example.binhi.bluetooth.BluetoothPermissionHelper
import com.example.binhi.bluetooth.SoilSensorData
import com.example.binhi.data.SavedSession
import com.example.binhi.data.SoilData
import com.example.binhi.viewmodel.SoilDataViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.cos
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun SavedDataScreen(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    var showSessionList by remember { mutableStateOf(true) }
    var selectedSession by remember { mutableStateOf<SavedSession?>(null) }

    if (showSessionList) {
        SessionListView(
            navController = navController,
            soilDataViewModel = soilDataViewModel,
            onSessionSelected = { session ->
                selectedSession = session
                showSessionList = false
            }
        )
    } else if (selectedSession != null) {
        SavedSessionMapView(
            session = selectedSession!!,
            onBackClick = {
                showSessionList = true
                selectedSession = null
            },
            navController = navController,
            soilDataViewModel = soilDataViewModel,
            isDarkModeState = isDarkModeState
        )
    }
}

@Composable
fun SessionListView(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel,
    onSessionSelected: (SavedSession) -> Unit,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    val isDarkMode = isDarkModeState.value
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SavedSession?>(null) }
    var showSessionDetails by remember { mutableStateOf(false) }
    var selectedSessionForDetails by remember { mutableStateOf<SavedSession?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<SavedSession?>(null) }
    var newSessionName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Back button aligned with title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }

            Text(
                text = "Saved Sessions",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        val savedSessions = soilDataViewModel.getAllSavedSessions()
        Log.d("SavedDataScreen", "Displaying ${savedSessions.size} saved sessions")

        if (savedSessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No saved data yet",
                    fontSize = 16.sp,
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save a session from GetSoilData to see it here",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.DarkGray else Color.LightGray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedSessions) { session ->
                    SessionCard(
                        session = session,
                        onDetailsClick = {
                            onSessionSelected(session)
                        },
                        onDeleteClick = {
                            sessionToDelete = session
                            showDeleteConfirm = true
                        },
                        onInfoClick = {
                            selectedSessionForDetails = session
                            showSessionDetails = true
                        },
                        onRenameClick = {
                            sessionToRename = session
                            newSessionName = session.sessionName
                            showRenameDialog = true
                        },
                        isDarkModeState = isDarkModeState
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                sessionToDelete = null
            },
            title = { Text("Delete Session?", color = textColor) },
            text = {
                Text("Are you sure you want to delete '${sessionToDelete!!.sessionName}'? This action cannot be undone.", color = textColor)
            },
            confirmButton = {
                Button(
                    onClick = {
                        soilDataViewModel.deleteSavedSession(sessionToDelete!!.id)
                        showDeleteConfirm = false
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    sessionToDelete = null
                }) {
                    Text("Cancel", color = textColor)
                }
            },
            containerColor = bgColor
        )
    }

    // Session details dialog
    if (showSessionDetails && selectedSessionForDetails != null) {
        SessionDetailsDialog(
            session = selectedSessionForDetails!!,
            onDismiss = {
                showSessionDetails = false
                selectedSessionForDetails = null
            },
            isDarkModeState = isDarkModeState
        )
    }

    // Rename dialog
    if (showRenameDialog && sessionToRename != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                sessionToRename = null
                newSessionName = ""
            },
            title = { Text("Rename Session", color = textColor) },
            text = {
                Column {
                    Text("Enter new name for '${sessionToRename!!.sessionName}':", color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newSessionName,
                        onValueChange = { newSessionName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color.White,
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = if (isDarkMode) Color.White else Color.Black,
                            focusedIndicatorColor = Color.Blue,
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSessionName.isNotBlank() && sessionToRename != null) {
                            val updatedSession = sessionToRename!!.copy(sessionName = newSessionName)
                            soilDataViewModel.updateSessionName(sessionToRename!!.id, newSessionName)
                            showRenameDialog = false
                            sessionToRename = null
                            newSessionName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    sessionToRename = null
                    newSessionName = ""
                }) {
                    Text("Cancel", color = textColor)
                }
            },
            containerColor = bgColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSessionMapView(
    session: SavedSession,
    onBackClick: () -> Unit,
    navController: NavController,
    soilDataViewModel: SoilDataViewModel,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    val isDarkMode = isDarkModeState.value
    val bottomSheetBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.Black
    val bottomSheetTextColor = if (isDarkMode) Color.White else Color.White
    val polygonCenter = LatLng(session.polygonCenter.first, session.polygonCenter.second)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(polygonCenter, session.cameraZoom)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bluetoothManager = remember { BluetoothClassicManager(context) }

    var selectedDot by remember { mutableStateOf<LatLng?>(null) }
    var showDotDialog by remember { mutableStateOf(false) }
    var isSessionInfoExpanded by remember { mutableStateOf(false) }

    // Bluetooth state for updating saved data
    var bluetoothResponse by remember { mutableStateOf<SoilSensorData?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var isBluetoothLoading by remember { mutableStateOf(false) }
    var hasBluetoothPermission by remember {
        mutableStateOf(bluetoothManager.hasBluetoothPermissions())
    }
    var showUpdateSuccessMessage by remember { mutableStateOf(false) }

    // Bluetooth permission launcher
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

    val dots = remember(session) {
        session.soilDataPoints.keys.map { pair ->
            SavedSession.pairToLatLng(pair)
        }
    }

    val lengthInMeters = session.length
    val widthInMeters = session.width
    val rotation = session.rotation

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
                val rotatedX = x * kotlin.math.cos(angleRad) - y * kotlin.math.sin(angleRad)
                val rotatedY = x * kotlin.math.sin(angleRad) + y * kotlin.math.cos(angleRad)
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

    var mapType by remember { mutableStateOf(
        if (session.mapType == "SATELLITE") MapType.SATELLITE else MapType.NORMAL
    ) }

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
                    containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
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
                            DataRowItem("Nitrogen", "${bluetoothResponse!!.nitrogen}", isDarkMode)
                            DataRowItem("Phosphorus", "${bluetoothResponse!!.phosphorus}", isDarkMode)
                            DataRowItem("Potassium", "${bluetoothResponse!!.potassium}", isDarkMode)
                            DataRowItem("pH Level", String.format("%.2f", bluetoothResponse!!.phLevel), isDarkMode)
                            DataRowItem("Temperature", String.format("%.1f", bluetoothResponse!!.temperature) + "°C", isDarkMode)
                            DataRowItem("Moisture", "${bluetoothResponse!!.moisture}%", isDarkMode)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Raw: ${bluetoothResponse!!.rawData}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Update Data button - save the received data to the session
                    if (!bluetoothResponse!!.isError && selectedDot != null) {
                        Button(
                            onClick = {
                                val dotPair = SavedSession.latLngToPair(selectedDot!!)
                                val soilData = bluetoothResponse!!.toSoilData()
                                if (soilData != null) {
                                    // Update the session data by creating a new map with updated values
                                    val updatedDataPoints = session.soilDataPoints.toMutableMap()
                                    updatedDataPoints[dotPair] = soilData
                                    val updatedSession = session.copy(soilDataPoints = updatedDataPoints)

                                    // Save to database via ViewModel
                                    soilDataViewModel.updateSavedSession(updatedSession)
                                    Log.d("SavedData", "✓ Soil data updated for dot: $selectedDot")

                                    showUpdateSuccessMessage = true
                                    // Auto-close success message after 2 seconds
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        showUpdateSuccessMessage = false
                                        showBluetoothDialog = false
                                        bluetoothResponse = null
                                        showDotDialog = false
                                        selectedDot = null
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
                                contentDescription = "Update",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Data")
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

    // Update success message
    if (showUpdateSuccessMessage) {
        Dialog(
            onDismissRequest = { showUpdateSuccessMessage = false }
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
                        text = "Soil Data Updated!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Data saved to session",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = true
            )
        ) {
            if (polygonPoints.isNotEmpty()) {
                Polygon(
                    points = polygonPoints,
                    fillColor = Color.Red.copy(alpha = 0.5f),
                    strokeColor = Color.Red,
                    strokeWidth = 5f
                )
            }

            dots.forEach { dot ->
                Marker(
                    state = MarkerState(position = dot),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    onClick = {
                        selectedDot = dot
                        showDotDialog = true
                        true
                    }
                )
            }
        }

        TopAppBar(
            title = {
                Text(
                    text = session.sessionName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Floating action button at top right corner (only icon)
        FloatingActionButton(
            onClick = { isSessionInfoExpanded = !isSessionInfoExpanded },
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = if (isSessionInfoExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isSessionInfoExpanded) "Hide Session Info" else "Show Session Info",
                modifier = Modifier.size(24.dp)
            )
        }


        // Get Crop Recommendation button at bottom center (behind the Session Info panel)
        Button(
            onClick = {
                // Load session data into ViewModel before navigating to MappingInfo
                soilDataViewModel.loadSession(session)
                // Navigate to MappingInfo with session data now available
                navController.navigate("mapping_info")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .height(44.dp)
                .widthIn(min = 200.dp, max = 300.dp)
        ) {
            Text("Get Crop Recommendation", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Session Information Panel - appears only when FAB is clicked
        if (isSessionInfoExpanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.85f)
                    .background(bottomSheetBgColor.copy(alpha = 0.95f), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header with title only
                Text(
                    text = "Session Information",
                    color = bottomSheetTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SessionInfoItem("Crop", session.crop)
                        SessionInfoItem("Area", "${session.landArea.toInt()} m²")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SessionInfoItem("Dots", "${session.totalDots}")
                        SessionInfoItem("Data Pts", "${session.soilDataPoints.size}")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SessionInfoItem("Rotation", "${String.format("%.1f", session.rotation)}°")
                        SessionInfoItem("Map", session.mapType)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SessionInfoItem("Zoom", "${String.format("%.1f", session.cameraZoom)}")
                        SessionInfoItem("Date", session.getFormattedDate())
                    }
                }

                LinearProgressIndicator(
                    progress = if (session.totalDots > 0) {
                        session.soilDataPoints.size.toFloat() / session.totalDots.toFloat()
                    } else {
                        0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.Gray
                )

                // Bottom left expand/collapse icon
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Collapse",
                    tint = bottomSheetTextColor,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { isSessionInfoExpanded = false }
                        .align(Alignment.Start)
                        .padding(start = 2.dp, bottom = 2.dp)
                )
            }
        }
    }

    if (showDotDialog && selectedDot != null) {
        val dotPair = SavedSession.latLngToPair(selectedDot!!)
        val soilData = session.soilDataPoints[dotPair]
        val dialogBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        val dialogTextColor = if (isDarkMode) Color.White else Color.Black

        Dialog(
            onDismissRequest = {
                showDotDialog = false
                selectedDot = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = dialogBgColor
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
                        text = "Sample Location Data",
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latitude: ${String.format("%.6f", selectedDot!!.latitude)}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogTextColor
                    )
                    Text(
                        text = "Longitude: ${String.format("%.6f", selectedDot!!.longitude)}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogTextColor
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = if (isDarkMode) Color.DarkGray else Color.LightGray)

                    if (soilData != null) {
                        Text(
                            text = "Stored Soil Data",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Green
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DataRowItem("Nitrogen", "${soilData.nitrogen}", isDarkMode)
                            DataRowItem("Phosphorus", "${soilData.phosphorus}", isDarkMode)
                            DataRowItem("Potassium", "${soilData.potassium}", isDarkMode)
                            DataRowItem("pH Level", String.format("%.2f", soilData.phLevel), isDarkMode)
                            DataRowItem("Temperature", String.format("%.1f", soilData.temperature) + "°C", isDarkMode)
                            DataRowItem("Moisture", "${soilData.moisture}%", isDarkMode)
                        }
                    } else {
                        Text(
                            text = "No Data Stored",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Receive Data button - allows updating stored data from Bluetooth sensor
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
                        enabled = !isBluetoothLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
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
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Receive Data",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Receive Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showDotDialog = false
                            selectedDot = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = Color.Blue)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SessionCard(
    session: SavedSession,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onInfoClick: () -> Unit,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false),
    onRenameClick: () -> Unit = {}
) {
    val isDarkMode = isDarkModeState.value
    val cardBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray
    val dividerColor = if (isDarkMode) Color.DarkGray else Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() }
            .background(cardBgColor, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.sessionName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = session.getFormattedDate(),
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRenameClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = Color.Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Details",
                            tint = Color.Blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Divider(color = dividerColor)

            Text(
                text = session.getCompletionInfo(),
                fontSize = 12.sp,
                color = secondaryTextColor
            )

            LinearProgressIndicator(
                progress = if (session.totalDots > 0) {
                    session.soilDataPoints.size.toFloat() / session.totalDots.toFloat()
                } else {
                    0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4CAF50),
                trackColor = if (isDarkMode) Color.DarkGray else Color.LightGray
            )
        }
    }
}

@Composable
fun SessionDetailsDialog(
    session: SavedSession,
    onDismiss: () -> Unit,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    val isDarkMode = isDarkModeState.value
    val bgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(session.sessionName, color = textColor)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Session Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
                item {
                    Text("Date: ${session.getFormattedDate()}", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Crop: ${session.crop}", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Land Area: ${session.landArea} m²", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Field Size: ${session.length}m × ${session.width}m", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Total Dots: ${session.totalDots}", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Data Points: ${session.soilDataPoints.size}", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Completion: ${(session.soilDataPoints.size * 100) / maxOf(session.totalDots, 1)}%", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Map Type: ${session.mapType}", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Rotation: ${String.format("%.1f", session.rotation)}°", fontSize = 12.sp, color = secondaryTextColor)
                }
                item {
                    Text("Zoom: ${String.format("%.1f", session.cameraZoom)}", fontSize = 12.sp, color = secondaryTextColor)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Blue)
            }
        },
        containerColor = bgColor
    )
}

@Composable
fun DataRowItem(label: String, value: String, isDarkMode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDarkMode) Color.LightGray else Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkMode) Color.White else Color.Black
        )
    }
}

