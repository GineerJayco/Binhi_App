package com.example.binhi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VisualizationDetails(
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?,
    onClose: () -> Unit
) {
    val areaPerPlant = when (crop) {
        "Banana" -> 3.24
        "Cassava" -> 1.0
        "Sweet Potato" -> 0.23
        "Mango" -> 400.0
        "Corn" -> 0.38
        else -> 0.0
    }

    val landAreaValue = landArea?.toDoubleOrNull() ?: 0.0
    val estimatedQuantity = if (areaPerPlant > 0) (landAreaValue / areaPerPlant).toInt() else 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Visualization Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray, RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    DetailItem(label = "Crop:", value = crop ?: "N/A")
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFE8F5E9), // A light green
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Est. Crop Quantity:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32) // Darker green
                            )
                            Text(
                                text = "~$estimatedQuantity plants",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF2E7D32) // Darker green
                            )
                        }
                    }
                }
                item {
                    DetailItem(label = "Total Land Area:", value = "${landArea ?: "N/A"} sqm")
                }
                item {
                    DetailItem(label = "Plot Length:", value = "${length ?: "N/A"} m")
                }
                item {
                    DetailItem(label = "Plot Width:", value = "${width ?: "N/A"} m")
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        Divider(modifier = Modifier.padding(top = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))
    }
}
