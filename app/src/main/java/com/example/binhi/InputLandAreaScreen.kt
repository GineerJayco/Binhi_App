package com.example.binhi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.DecimalFormat
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputLandAreaScreen(navController: NavController) {
    var landArea by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var selectedCrop by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val crops = listOf("Banana", "Cassava", "Sweet Potato", "Mango", "Corn")
    var isAreaSet by remember { mutableStateOf(false) }
    val decimalFormat = DecimalFormat("0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Land Area") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Define Your Plot", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = landArea,
                        onValueChange = { if (!isAreaSet) landArea = it },
                        label = { Text("Land Area") },
                        trailingIcon = { Text("sqm", style = MaterialTheme.typography.bodySmall) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isAreaSet,
                        singleLine = true
                    )

                    if (isAreaSet) {
                        TextButton(onClick = {
                            isAreaSet = false
                            landArea = ""
                            length = ""
                            width = ""
                        }) {
                            Text("Change Area")
                        }

                        // Length
                        OutlinedTextField(
                            value = length,
                            onValueChange = {
                                length = it
                                val l = it.toDoubleOrNull()
                                val area = landArea.toDoubleOrNull()
                                if (l != null && l > 0 && area != null) {
                                    width = decimalFormat.format(area / l)
                                }
                            },
                            label = { Text("Length") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = {
                                        val l = length.toDoubleOrNull() ?: 0.0
                                        val newLength = l + 1
                                        length = decimalFormat.format(newLength)
                                        val area = landArea.toDoubleOrNull()
                                        if (area != null && newLength > 0) {
                                            width = decimalFormat.format(area / newLength)
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ArrowDropUp, contentDescription = "Increase Length")
                                    }
                                    IconButton(onClick = {
                                        val l = length.toDoubleOrNull() ?: 0.0
                                        if (l > 1) {
                                            val newLength = l - 1
                                            length = decimalFormat.format(newLength)
                                            val area = landArea.toDoubleOrNull()
                                            if (area != null && newLength > 0) {
                                                width = decimalFormat.format(area / newLength)
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrease Length")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Width
                        OutlinedTextField(
                            value = width,
                            onValueChange = {
                                width = it
                                val w = it.toDoubleOrNull()
                                val area = landArea.toDoubleOrNull()
                                if (w != null && w > 0 && area != null) {
                                    length = decimalFormat.format(area / w)
                                }
                            },
                            label = { Text("Width") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(onClick = {
                                        val w = width.toDoubleOrNull() ?: 0.0
                                        val newWidth = w + 1
                                        width = decimalFormat.format(newWidth)
                                        val area = landArea.toDoubleOrNull()
                                        if (area != null && newWidth > 0) {
                                            length = decimalFormat.format(area / newWidth)
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ArrowDropUp, contentDescription = "Increase Width")
                                    }
                                    IconButton(onClick = {
                                        val w = width.toDoubleOrNull() ?: 0.0
                                        if (w > 1) {
                                            val newWidth = w - 1
                                            width = decimalFormat.format(newWidth)
                                            val area = landArea.toDoubleOrNull()
                                            if (area != null && newWidth > 0) {
                                                length = decimalFormat.format(area / newWidth)
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Decrease Width")
                                    }
                                }
                            },
                            singleLine = true
                        )

                    } else {
                        Button(
                            onClick = {
                                val area = landArea.toDoubleOrNull()
                                if (area != null && area > 0) {
                                    isAreaSet = true
                                    val side = sqrt(area)
                                    length = decimalFormat.format(side)
                                    width = decimalFormat.format(side)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Set Area")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isAreaSet) {
                 Button(
                    onClick = { navController.navigate("get_soil_data/$landArea/$length/$width/$selectedCrop") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Get Soil Data")
                }
                
                Text(text = "Proceed to interactive soil sampling.", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text("OR", modifier = Modifier.padding(horizontal = 8.dp))
                    Divider(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Crop", style = MaterialTheme.typography.titleMedium)
                
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedCrop,
                        onValueChange = {},
                        placeholder = { Text("Choose a crop...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        crops.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedCrop = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isVisualizeEnabled = selectedCrop.isNotEmpty()
                Button(
                    onClick = { navController.navigate("visualize_la/$landArea/$length/$width/$selectedCrop") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = isVisualizeEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = "Visualize",
                        color = if (isVisualizeEnabled) Color.White else Color.DarkGray
                    )
                }
            }
        }
    }
}
