package com.example.binhi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.DecimalFormat
import kotlin.math.sqrt

// Measurement conversion utility
object MeasurementConverter {
    enum class LengthUnit(val displayName: String, val toMeters: Double) {
        METERS("Meters (m)", 1.0),
        FEET("Feet (ft)", 0.3048),
        KILOMETERS("Kilometers (km)", 1000.0),
        MILES("Miles (mi)", 1609.34),
        CENTIMETERS("Centimeters (cm)", 0.01),
        INCHES("Inches (in)", 0.0254)
    }

    enum class AreaUnit(val displayName: String, val toSquareMeters: Double) {
        SQUARE_METERS("Square Meters (sqm)", 1.0),
        SQUARE_FEET("Square Feet (sqft)", 0.092903),
        HECTARES("Hectares (ha)", 10000.0),
        ACRES("Acres", 4046.86),
        SQUARE_KILOMETERS("Square Kilometers (sqkm)", 1000000.0),
        SQUARE_MILES("Square Miles (sqmi)", 2589988.0),
        SQUARE_CENTIMETERS("Square Centimeters (sqcm)", 0.0001),
        SQUARE_INCHES("Square Inches (sqin)", 0.00645)
    }

    fun convertLength(value: Double, from: LengthUnit, to: LengthUnit): Double {
        val valueInMeters = value * from.toMeters
        return valueInMeters / to.toMeters
    }

    fun convertArea(value: Double, from: AreaUnit, to: AreaUnit): Double {
        val valueInSquareMeters = value * from.toSquareMeters
        return valueInSquareMeters / to.toSquareMeters
    }
}

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
    var showConversionTool by remember { mutableStateOf(false) }
    val decimalFormat = DecimalFormat("0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Land Area", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { showConversionTool = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Conversion Tool", tint = Color.Black)
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
                    Text("Define Your Plot", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = landArea,
                        onValueChange = { if (!isAreaSet) landArea = it },
                        label = { Text("Land Area", color = Color.Black) },
                        trailingIcon = { Text("sqm", style = MaterialTheme.typography.bodySmall, color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isAreaSet,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
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
                            label = { Text("Length", color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
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
                            label = { Text("Width", color = Color.Black) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
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
                            Text("Set Area", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Image(
                            painter = painterResource(id = R.drawable.binhi_logo3),
                            contentDescription = "Binhi Logo",
                            modifier = Modifier
                                .height(350.dp)
                                .align(Alignment.CenterHorizontally)
                        )
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
                    Text("Get Soil Data", color = Color.Black)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.binhi_logo3),
                    contentDescription = "Binhi Logo",
                    modifier = Modifier
                        .height(80.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Proceed to interactive soil sampling.", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text("OR", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Black)
                    Divider(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Crop", style = MaterialTheme.typography.titleMedium, color = Color.Black)

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
                        label = { Text("Crop", color = Color.Black) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
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
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.binhi_logo3),
                    contentDescription = "Binhi Logo",
                    modifier = Modifier
                        .height(100.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Conversion Tool Dialog
    if (showConversionTool) {
        ConversionToolDialog(
            onDismiss = { showConversionTool = false },
            onConversionComplete = { convertedValue ->
                landArea = convertedValue
                isAreaSet = true
                val side = sqrt(convertedValue.toDoubleOrNull() ?: 0.0)
                val df = DecimalFormat("0.00")
                length = df.format(side)
                width = df.format(side)
                showConversionTool = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionToolCard(
    onAreaCalculated: (String) -> Unit
) {
    var conversionMode by remember { mutableStateOf(0) } // 0 = Area, 1 = Length/Width
    var inputValue by remember { mutableStateOf("") }
    var lengthValue by remember { mutableStateOf("") }
    var widthValue by remember { mutableStateOf("") }
    var selectedFromUnit by remember { mutableStateOf(MeasurementConverter.AreaUnit.SQUARE_FEET) }
    var selectedToUnit by remember { mutableStateOf(MeasurementConverter.AreaUnit.SQUARE_METERS) }
    var selectedLengthUnit by remember { mutableStateOf(MeasurementConverter.LengthUnit.METERS) }
    var selectedWidthUnit by remember { mutableStateOf(MeasurementConverter.LengthUnit.METERS) }
    var expandedFromArea by remember { mutableStateOf(false) }
    var expandedToArea by remember { mutableStateOf(false) }
    var expandedLengthUnit by remember { mutableStateOf(false) }
    var expandedWidthUnit by remember { mutableStateOf(false) }
    val decimalFormat = DecimalFormat("0.0000")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Measurement Converter", style = MaterialTheme.typography.titleMedium, color = Color.Black)

            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { conversionMode = 0 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (conversionMode == 0) Color(0xFF4CAF50) else Color.LightGray
                    )
                ) {
                    Text("Area", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { conversionMode = 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (conversionMode == 1) Color(0xFF4CAF50) else Color.LightGray
                    )
                ) {
                    Text("Length", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (conversionMode == 0) {
                // Area Conversion Mode
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Enter Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                )

                Text("From Unit:", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                ExposedDropdownMenuBox(
                    expanded = expandedFromArea,
                    onExpandedChange = { expandedFromArea = !expandedFromArea },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedFromUnit.displayName,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFromArea) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFromArea,
                        onDismissRequest = { expandedFromArea = false }
                    ) {
                        MeasurementConverter.AreaUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedFromUnit = unit
                                    expandedFromArea = false
                                }
                            )
                        }
                    }
                }

                Text("To Unit:", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                ExposedDropdownMenuBox(
                    expanded = expandedToArea,
                    onExpandedChange = { expandedToArea = !expandedToArea },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedToUnit.displayName,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToArea) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedToArea,
                        onDismissRequest = { expandedToArea = false }
                    ) {
                        MeasurementConverter.AreaUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedToUnit = unit
                                    expandedToArea = false
                                }
                            )
                        }
                    }
                }

                // Show conversion result
                if (inputValue.isNotEmpty()) {
                    val input = inputValue.toDoubleOrNull()
                    if (input != null && input > 0) {
                        val result = MeasurementConverter.convertArea(input, selectedFromUnit, selectedToUnit)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Result:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${decimalFormat.format(input)} ${selectedFromUnit.displayName} = ${decimalFormat.format(result)} ${selectedToUnit.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            } else {
                // Length & Width Mode
                OutlinedTextField(
                    value = lengthValue,
                    onValueChange = { lengthValue = it },
                    label = { Text("Length") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    trailingIcon = {
                        ExposedDropdownMenuBox(
                            expanded = expandedLengthUnit,
                            onExpandedChange = { expandedLengthUnit = !expandedLengthUnit }
                        ) {
                            Text(selectedLengthUnit.displayName.split(" ")[0], modifier = Modifier.padding(end = 8.dp))
                            ExposedDropdownMenu(
                                expanded = expandedLengthUnit,
                                onDismissRequest = { expandedLengthUnit = false }
                            ) {
                                listOf(MeasurementConverter.LengthUnit.METERS, MeasurementConverter.LengthUnit.CENTIMETERS).forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.displayName) },
                                        onClick = {
                                            selectedLengthUnit = unit
                                            expandedLengthUnit = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = widthValue,
                    onValueChange = { widthValue = it },
                    label = { Text("Width") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    trailingIcon = {
                        ExposedDropdownMenuBox(
                            expanded = expandedWidthUnit,
                            onExpandedChange = { expandedWidthUnit = !expandedWidthUnit }
                        ) {
                            Text(selectedWidthUnit.displayName.split(" ")[0], modifier = Modifier.padding(end = 8.dp))
                            ExposedDropdownMenu(
                                expanded = expandedWidthUnit,
                                onDismissRequest = { expandedWidthUnit = false }
                            ) {
                                listOf(MeasurementConverter.LengthUnit.METERS, MeasurementConverter.LengthUnit.CENTIMETERS).forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.displayName) },
                                        onClick = {
                                            selectedWidthUnit = unit
                                            expandedWidthUnit = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                // Show calculated area result
                if (lengthValue.isNotEmpty() && widthValue.isNotEmpty()) {
                    val length = lengthValue.toDoubleOrNull()
                    val width = widthValue.toDoubleOrNull()
                    if (length != null && width != null && length > 0 && width > 0) {
                        // Convert to meters first if needed
                        val lengthInMeters = if (selectedLengthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) {
                            length / 100
                        } else {
                            length
                        }
                        val widthInMeters = if (selectedWidthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) {
                            width / 100
                        } else {
                            width
                        }
                        val areaInSqm = lengthInMeters * widthInMeters

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Calculated Area:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${decimalFormat.format(lengthInMeters)}m × ${decimalFormat.format(widthInMeters)}m = ${decimalFormat.format(areaInSqm)} sqm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionToolDialog(
    onDismiss: () -> Unit,
    onConversionComplete: (String) -> Unit
) {
    var conversionMode by remember { mutableStateOf(0) } // 0 = Area, 1 = Length/Width
    var inputValue by remember { mutableStateOf("") }
    var lengthValue by remember { mutableStateOf("") }
    var widthValue by remember { mutableStateOf("") }
    var selectedFromUnit by remember { mutableStateOf(MeasurementConverter.AreaUnit.SQUARE_FEET) }
    var selectedToUnit by remember { mutableStateOf(MeasurementConverter.AreaUnit.SQUARE_METERS) }
    var selectedLengthUnit by remember { mutableStateOf(MeasurementConverter.LengthUnit.METERS) }
    var selectedWidthUnit by remember { mutableStateOf(MeasurementConverter.LengthUnit.METERS) }
    var expandedFromArea by remember { mutableStateOf(false) }
    var expandedToArea by remember { mutableStateOf(false) }
    var expandedLengthUnit by remember { mutableStateOf(false) }
    var expandedWidthUnit by remember { mutableStateOf(false) }
    val decimalFormat = DecimalFormat("0.0000")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Measurement Converter") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { conversionMode = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (conversionMode == 0) Color(0xFF4CAF50) else Color.LightGray
                        )
                    ) {
                        Text("Area", color = Color.Black)
                    }
                    Button(
                        onClick = { conversionMode = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (conversionMode == 1) Color(0xFF4CAF50) else Color.LightGray
                        )
                    ) {
                        Text("Length", color = Color.Black, maxLines = 1, softWrap = false)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (conversionMode == 0) {
                    // Area Conversion Mode
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("Enter Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("From Unit:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = expandedFromArea,
                        onExpandedChange = { expandedFromArea = !expandedFromArea },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            value = selectedFromUnit.displayName,
                            onValueChange = {},
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFromArea) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedFromArea,
                            onDismissRequest = { expandedFromArea = false }
                        ) {
                            MeasurementConverter.AreaUnit.values().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.displayName) },
                                    onClick = {
                                        selectedFromUnit = unit
                                        expandedFromArea = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("To Unit:", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = expandedToArea,
                        onExpandedChange = { expandedToArea = !expandedToArea },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            value = selectedToUnit.displayName,
                            onValueChange = {},
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToArea) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedToArea,
                            onDismissRequest = { expandedToArea = false }
                        ) {
                            MeasurementConverter.AreaUnit.values().forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.displayName) },
                                    onClick = {
                                        selectedToUnit = unit
                                        expandedToArea = false
                                    }
                                )
                            }
                        }
                    }

                    // Show conversion result
                    if (inputValue.isNotEmpty()) {
                        val input = inputValue.toDoubleOrNull()
                        if (input != null && input > 0) {
                            val result = MeasurementConverter.convertArea(input, selectedFromUnit, selectedToUnit)
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Result:", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${decimalFormat.format(input)} ${selectedFromUnit.displayName} = ${decimalFormat.format(result)} ${selectedToUnit.displayName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Length & Width Mode
                    OutlinedTextField(
                        value = lengthValue,
                        onValueChange = { lengthValue = it },
                        label = { Text("Length") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        trailingIcon = {
                            ExposedDropdownMenuBox(
                                expanded = expandedLengthUnit,
                                onExpandedChange = { expandedLengthUnit = !expandedLengthUnit }
                            ) {
                                Text(selectedLengthUnit.displayName.split(" ")[0], modifier = Modifier.padding(end = 8.dp))
                                ExposedDropdownMenu(
                                    expanded = expandedLengthUnit,
                                    onDismissRequest = { expandedLengthUnit = false }
                                ) {
                                    listOf(MeasurementConverter.LengthUnit.METERS, MeasurementConverter.LengthUnit.CENTIMETERS).forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.displayName) },
                                            onClick = {
                                                selectedLengthUnit = unit
                                                expandedLengthUnit = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = widthValue,
                        onValueChange = { widthValue = it },
                        label = { Text("Width") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        trailingIcon = {
                            ExposedDropdownMenuBox(
                                expanded = expandedWidthUnit,
                                onExpandedChange = { expandedWidthUnit = !expandedWidthUnit }
                            ) {
                                Text(selectedWidthUnit.displayName.split(" ")[0], modifier = Modifier.padding(end = 8.dp))
                                ExposedDropdownMenu(
                                    expanded = expandedWidthUnit,
                                    onDismissRequest = { expandedWidthUnit = false }
                                ) {
                                    listOf(MeasurementConverter.LengthUnit.METERS, MeasurementConverter.LengthUnit.CENTIMETERS).forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.displayName) },
                                            onClick = {
                                                selectedWidthUnit = unit
                                                expandedWidthUnit = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // Show calculated area result
                    if (lengthValue.isNotEmpty() && widthValue.isNotEmpty()) {
                        val length = lengthValue.toDoubleOrNull()
                        val width = widthValue.toDoubleOrNull()
                        if (length != null && width != null && length > 0 && width > 0) {
                            val lengthInMeters = if (selectedLengthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) length / 100 else length
                            val widthInMeters = if (selectedWidthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) width / 100 else width
                            val areaInSqm = lengthInMeters * widthInMeters

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Calculated Area:", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${decimalFormat.format(lengthInMeters)}m × ${decimalFormat.format(widthInMeters)}m = ${decimalFormat.format(areaInSqm)} sqm",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (conversionMode == 0) {
                        val input = inputValue.toDoubleOrNull()
                        if (input != null && input > 0) {
                            val result = if (selectedToUnit == MeasurementConverter.AreaUnit.SQUARE_METERS) {
                                MeasurementConverter.convertArea(input, selectedFromUnit, selectedToUnit)
                            } else {
                                MeasurementConverter.convertArea(input, selectedFromUnit, MeasurementConverter.AreaUnit.SQUARE_METERS)
                            }
                            onConversionComplete(decimalFormat.format(result))
                        }
                    } else {
                        val length = lengthValue.toDoubleOrNull()
                        val width = widthValue.toDoubleOrNull()
                        if (length != null && width != null && length > 0 && width > 0) {
                            val lengthInMeters = if (selectedLengthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) length / 100 else length
                            val widthInMeters = if (selectedWidthUnit == MeasurementConverter.LengthUnit.CENTIMETERS) width / 100 else width
                            val areaInSqm = lengthInMeters * widthInMeters
                            onConversionComplete(decimalFormat.format(areaInSqm))
                        }
                    }
                }
            ) {
                Text("Use Value (sqm)")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
