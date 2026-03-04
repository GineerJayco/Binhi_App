package com.example.binhi

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.example.binhi.data.SoilData
import com.example.binhi.viewmodel.SoilDataViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Utility function to convert decimal degrees to DMS format
 */
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

/**
 * Format timestamp to readable date/time
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingInfo(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LatLng?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    // Get all stored locations and sort them - compute directly to ensure updates
    val sortedLocations = soilDataViewModel.getAllStoredLocations()
        .toList()
        .sortedWith(compareBy({ it.latitude }, { it.longitude }))

    // Check if data is available for Analyze button
    val hasData = sortedLocations.isNotEmpty()

    // Log when locations change
    LaunchedEffect(sortedLocations.size) {
        Log.d("MappingInfo", "Loaded ${sortedLocations.size} locations with soil data")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        "Soil Sample Mapping Data",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Summary Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Data Collection Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Summary Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat(
                            label = "Total Samples",
                            value = "${sortedLocations.size}",
                            color = Color(0xFF2196F3)
                        )
                        SummaryStat(
                            label = "Completion",
                            value = "${soilDataViewModel.getCompletionPercentage()}%",
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = if (sortedLocations.isEmpty()) {
                            "No soil data collected yet. Go back to GetSoilData to collect samples."
                        } else {
                            "Click on any sample card to view or delete detailed information."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    // Action Buttons Row - Inside Summary Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Analyze Button
                        Button(
                            onClick = {
                                navController.navigate("crop_recommendation")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            enabled = hasData,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            Text(
                                "Analyze",
                                color = if (hasData) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Delete All Button
                        Button(
                            onClick = {
                                showDeleteAllConfirmDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722)
                            )
                        ) {
                            Text(
                                "Delete All",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Data List
            if (sortedLocations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "No Data",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Samples Collected",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start collecting soil samples in GetSoilData",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sortedLocations) { location ->
                        val soilData = soilDataViewModel.getSoilData(location)
                        if (soilData != null) {
                            SoilDataCard(
                                location = location,
                                soilData = soilData,
                                onDelete = {
                                    locationToDelete = location
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllConfirmDialog) {
        Dialog(
            onDismissRequest = {
                showDeleteAllConfirmDialog = false
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Delete All Data?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will permanently remove all ${sortedLocations.size} soil sample records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showDeleteAllConfirmDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                soilDataViewModel.clearAllData()
                                Log.d("MappingInfo", "Deleted all soil data")
                                showDeleteAllConfirmDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Delete All", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && locationToDelete != null) {
        Dialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                locationToDelete = null
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(48.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Delete Sample?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will permanently remove this soil sample data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                locationToDelete = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (locationToDelete != null) {
                                    soilDataViewModel.deleteSoilData(locationToDelete!!)
                                    Log.d("MappingInfo", "Deleted sample at $locationToDelete")
                                }
                                showDeleteConfirmDialog = false
                                locationToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card to display soil data for a specific location
 */
@Composable
private fun SoilDataCard(
    location: LatLng,
    soilData: SoilData,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location Header with DMS coordinates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF2196F3).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sample Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = decimalToDMS(location.latitude, true),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = decimalToDMS(location.longitude, false),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Decimal: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Divider()

            // Soil Parameters in a grid
            Text(
                text = "Soil Parameters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Nitrogen, Phosphorus, Potassium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterBox(
                        label = "Nitrogen",
                        value = "${soilData.nitrogen}",
                        unit = "mg/kg",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    ParameterBox(
                        label = "Phosphorus",
                        value = "${soilData.phosphorus}",
                        unit = "mg/kg",
                        color = Color(0xFFFFC107),
                        modifier = Modifier.weight(1f)
                    )
                    ParameterBox(
                        label = "Potassium",
                        value = "${soilData.potassium}",
                        unit = "mg/kg",
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: pH, Temperature, Moisture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterBox(
                        label = "pH Level",
                        value = String.format("%.2f", soilData.phLevel),
                        unit = "pH",
                        color = Color(0xFF00BCD4),
                        modifier = Modifier.weight(1f)
                    )
                    ParameterBox(
                        label = "Temperature",
                        value = String.format("%.1f", soilData.temperature),
                        unit = "°C",
                        color = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )
                    ParameterBox(
                        label = "Moisture",
                        value = "${soilData.moisture}",
                        unit = "%",
                        color = Color(0xFF03A9F4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider()

            // Timestamp and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Collection Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formatTimestamp(soilData.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
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
    }
}

/**
 * Parameter box to display a single soil parameter
 */
@Composable
private fun ParameterBox(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * Summary statistic box
 */
@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}




