package com.example.binhi

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.binhi.data.SavedSession
import com.example.binhi.viewmodel.SoilDataViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.cos

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
            }
        )
    }
}

@Composable
fun SessionListView(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel,
    onSessionSelected: (SavedSession) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SavedSession?>(null) }
    var showSessionDetails by remember { mutableStateOf(false) }
    var selectedSessionForDetails by remember { mutableStateOf<SavedSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(onClick = { navController.navigateUp() }) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Text(
            text = "Saved Sessions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

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
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save a session from GetSoilData to see it here",
                    fontSize = 12.sp,
                    color = Color.LightGray
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
                        }
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
            title = { Text("Delete Session?") },
            text = {
                Text("Are you sure you want to delete '${sessionToDelete!!.sessionName}'? This action cannot be undone.")
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
                    Text("Cancel")
                }
            }
        )
    }

    // Session details dialog
    if (showSessionDetails && selectedSessionForDetails != null) {
        SessionDetailsDialog(
            session = selectedSessionForDetails!!,
            onDismiss = {
                showSessionDetails = false
                selectedSessionForDetails = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSessionMapView(
    session: SavedSession,
    onBackClick: () -> Unit
) {
    val polygonCenter = LatLng(session.polygonCenter.first, session.polygonCenter.second)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(polygonCenter, session.cameraZoom)
    }

    var selectedDot by remember { mutableStateOf<LatLng?>(null) }
    var showDotDialog by remember { mutableStateOf(false) }

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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Session Information",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SessionInfoItem("Crop", session.crop)
                    SessionInfoItem("Area", "${session.landArea.toInt()} m²")
                }
                Column {
                    SessionInfoItem("Total Dots", "${session.totalDots}")
                    SessionInfoItem("Data Points", "${session.soilDataPoints.size}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SessionInfoItem("Rotation", "${String.format("%.1f", session.rotation)}°")
                    SessionInfoItem("Map Type", session.mapType)
                }
                Column {
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
                    .height(4.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color.Gray
            )
        }
    }

    if (showDotDialog && selectedDot != null) {
        val dotPair = SavedSession.latLngToPair(selectedDot!!)
        val soilData = session.soilDataPoints[dotPair]

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
                        text = "Sample Location Data",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Latitude: ${String.format("%.6f", selectedDot!!.latitude)}°",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Longitude: ${String.format("%.6f", selectedDot!!.longitude)}°",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

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
                            DataRowItem("Nitrogen", "${soilData.nitrogen}")
                            DataRowItem("Phosphorus", "${soilData.phosphorus}")
                            DataRowItem("Potassium", "${soilData.potassium}")
                            DataRowItem("pH Level", String.format("%.2f", soilData.phLevel))
                            DataRowItem("Temperature", String.format("%.1f", soilData.temperature) + "°C")
                            DataRowItem("Moisture", "${soilData.moisture}%")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            showDotDialog = false
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
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() }
            .background(Color.White, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                        color = Color.Black
                    )
                    Text(
                        text = session.getFormattedDate(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

            Divider(color = Color.LightGray)

            Text(
                text = session.getCompletionInfo(),
                fontSize = 12.sp,
                color = Color.DarkGray
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
                trackColor = Color.LightGray
            )
        }
    }
}

@Composable
fun SessionDetailsDialog(
    session: SavedSession,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(session.sessionName)
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
                        fontSize = 14.sp
                    )
                }
                item {
                    Text("Date: ${session.getFormattedDate()}", fontSize = 12.sp)
                }
                item {
                    Text("Crop: ${session.crop}", fontSize = 12.sp)
                }
                item {
                    Text("Land Area: ${session.landArea} m²", fontSize = 12.sp)
                }
                item {
                    Text("Field Size: ${session.length}m × ${session.width}m", fontSize = 12.sp)
                }
                item {
                    Text("Total Dots: ${session.totalDots}", fontSize = 12.sp)
                }
                item {
                    Text("Data Points: ${session.soilDataPoints.size}", fontSize = 12.sp)
                }
                item {
                    Text("Completion: ${(session.soilDataPoints.size * 100) / maxOf(session.totalDots, 1)}%", fontSize = 12.sp)
                }
                item {
                    Text("Map Type: ${session.mapType}", fontSize = 12.sp)
                }
                item {
                    Text("Rotation: ${String.format("%.1f", session.rotation)}°", fontSize = 12.sp)
                }
                item {
                    Text("Zoom: ${String.format("%.1f", session.cameraZoom)}", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DataRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

