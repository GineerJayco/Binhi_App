package com.example.binhi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class to hold average soil parameters
 */
data class AverageSoilParameters(
    val avgNitrogen: Double,
    val avgPhosphorus: Double,
    val avgPotassium: Double,
    val avgPhLevel: Double,
    val avgMoisture: Double,
    val avgTemperature: Double
)

/**
 * Expandable Average Soil Parameters Section
 * Default state is collapsed (hidden)
 */
@Composable
fun AverageSoilParametersSection(
    averageParameters: AverageSoilParameters?
) {
    var isExpanded by remember { mutableStateOf(false) }

    if (averageParameters != null) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with expand/collapse icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Average Soil Parameters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expandable content
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.05f),
                            shape = RoundedCornerShape(0.dp, 0.dp, 8.dp, 8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Average N, P, K
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AverageParameterBox(
                            label = "Avg N",
                            value = "${averageParameters.avgNitrogen.toInt()}",
                            unit = "mg/kg",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        AverageParameterBox(
                            label = "Avg P",
                            value = "${averageParameters.avgPhosphorus.toInt()}",
                            unit = "mg/kg",
                            color = Color(0xFFFFC107),
                            modifier = Modifier.weight(1f)
                        )
                        AverageParameterBox(
                            label = "Avg K",
                            value = "${averageParameters.avgPotassium.toInt()}",
                            unit = "mg/kg",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Average pH, Temperature, Moisture
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AverageParameterBox(
                            label = "Avg pH",
                            value = String.format("%.2f", averageParameters.avgPhLevel),
                            unit = "pH",
                            color = Color(0xFF00BCD4),
                            modifier = Modifier.weight(1f)
                        )
                        AverageParameterBox(
                            label = "Avg Temp",
                            value = String.format("%.1f", averageParameters.avgTemperature),
                            unit = "°C",
                            color = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f)
                        )
                        AverageParameterBox(
                            label = "Avg Moist",
                            value = "${averageParameters.avgMoisture.toInt()}",
                            unit = "%",
                            color = Color(0xFF03A9F4),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

/**
 * Average parameter box to display average soil parameter
 */
@Composable
private fun AverageParameterBox(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, shape = RoundedCornerShape(8.dp))
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

