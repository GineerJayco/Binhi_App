# Binhi App: Code Snippets Documentation

## Table of Contents
1. [Figure 17: Microcontroller Code](#figure-17-microcontroller-code)
2. [Figure 18: GUI for Crop Quantity Estimation](#figure-18-gui-for-crop-quantity-estimation)
3. [Figure 19: GUI for Land Area Estimation](#figure-19-gui-for-land-area-estimation)
4. [Figure 20: GUI for Crop Recommendation](#figure-20-gui-for-crop-recommendation)

---

## Figure 17: Microcontroller Code

### Code Snippet: RS485 Sensor Communication and Data Parsing

```cpp
// RS485 SENSOR READING AND DATA PARSING
void readRS485Sensor() {
  // Clear any remaining data in the buffer
  while (RS485Serial.available()) {
    RS485Serial.read();
  }

  // Set RS485 module to transmit mode
  digitalWrite(RS485_DE_PIN, HIGH);
  digitalWrite(RS485_RE_PIN, HIGH);
  delay(10);

  // Send query command to sensor
  RS485Serial.write(RS485_QUERY, RS485_QUERY_SIZE);
  RS485Serial.flush();

  // Switch RS485 module to receive mode
  digitalWrite(RS485_DE_PIN, LOW);
  digitalWrite(RS485_RE_PIN, LOW);
  delay(10);

  // Wait for sensor response
  unsigned long timeout = millis();
  while (RS485Serial.available() < RS485_RESPONSE_SIZE && (millis() - timeout) < 1000) {
    delay(10);
  }

  // Parse the received data
  if (RS485Serial.available() >= RS485_RESPONSE_SIZE) {
    byte receivedData[RS485_RESPONSE_SIZE];
    RS485Serial.readBytes(receivedData, RS485_RESPONSE_SIZE);
    parseRS485Data(receivedData);
  }
}

void parseRS485Data(byte* data) {
  // Extract 16-bit values (MSB first)
  unsigned int rawMoisture = (data[3] << 8) | data[4];
  unsigned int rawTemperature = (data[5] << 8) | data[6];
  unsigned int rawConductivity = (data[7] << 8) | data[8];
  unsigned int rawPH = (data[9] << 8) | data[10];
  unsigned int rawNitrogen = (data[11] << 8) | data[12];
  unsigned int rawPhosphorus = (data[13] << 8) | data[14];
  unsigned int rawPotassium = (data[15] << 8) | data[16];

  // Convert raw values to physical units
  soilData.moisture = (float)rawMoisture / 10.0;
  soilData.temperature = (float)rawTemperature / 10.0;
  soilData.conductivity = (float)rawConductivity;
  soilData.pH = (float)rawPH / 10.0;
  soilData.nitrogen = (float)rawNitrogen;
  soilData.phosphorus = (float)rawPhosphorus;
  soilData.potassium = (float)rawPotassium;

  // Redraw display with new data
  drawSoilDataDisplay();
}
```

### Explanation

Figure 17 demonstrates the critical microcontroller functionality for the Binhi soil sensor hardware integration. The `readRS485Sensor()` function implements a sophisticated RS485 communication protocol by first clearing the serial buffer to prevent data corruption, then switching the RS485 module to transmit mode by setting control pins HIGH to send a pre-defined query command to the soil sensor. The code implements a timeout mechanism to wait for the sensor response with a maximum of 1000 milliseconds, ensuring the system doesn't hang indefinitely if the sensor is unresponsive. The `parseRS485Data()` function processes the raw binary response by extracting 16-bit sensor values using bitwise operations (shifting and OR operations) to reconstruct the multi-byte data from the RS485 protocol. The conversion factors applied during parsing (dividing by 10 for moisture, temperature, and pH) normalize the raw sensor values into standard measurement units, enabling accurate representation of soil nutrients (NPK in mg/kg), pH levels, temperature in Celsius, and moisture percentages. This data is then immediately rendered on the TFT display to provide real-time feedback to field operators.

---

## Figure 18: GUI for Crop Quantity Estimation

### Code Snippet: Input Crop Quantity Screen with Validation

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputCropQuantityScreen(navController: NavController) {
    var cropQuantity by remember { mutableStateOf("") }
    var selectedCrop by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val crops = listOf("Banana", "Cassava", "Sweet Potato", "Coconut", "Corn")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Crop Quantity", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Crop Selection Dropdown
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
                            label = { Text("Crop", color = Color.Black) },
                            placeholder = { Text("Choose a crop...", color = Color.Black) },
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

                    // Crop Quantity Input
                    OutlinedTextField(
                        value = cropQuantity,
                        onValueChange = { cropQuantity = it },
                        label = { Text("Crop Quantity", color = Color.Black) },
                        placeholder = { Text("e.g., 50", color = Color.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compute and Visualize Button
            val isButtonEnabled = selectedCrop.isNotEmpty() && cropQuantity.isNotEmpty()
            Button(
                onClick = { navController.navigate("visualize_cq/$selectedCrop/$cropQuantity") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = "Compute Land Area and Visualize Crop Placement",
                    color = if (isButtonEnabled) Color.White else Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
```

### Explanation

Figure 18 illustrates the crop quantity estimation input interface, which is a foundational screen that captures two critical pieces of information from the farmer: the desired crop type and the target quantity of plants. The screen utilizes Jetpack Compose's Material Design 3 components, specifically `ExposedDropdownMenuBox` for intuitive crop selection from a predefined list including Banana, Cassava, Sweet Potato, Coconut, and Corn. The `InputCropQuantityScreen` function implements comprehensive input validation by maintaining state variables for `cropQuantity` and `selectedCrop`, restricting the quantity input to numeric values via `KeyboardType.Number`. The button's enabled state is dynamically controlled by checking both fields are non-empty (`isButtonEnabled`), preventing navigation to the visualization screen until valid data is provided. When enabled, the button triggers navigation with parameters containing the selected crop and quantity, which are then used by the visualization screen to calculate optimal crop placement and estimated land area requirements based on typical spacing for each crop type.

---

## Figure 19: GUI for Land Area Estimation

### Code Snippet: Land Area Input with Measurement Conversion

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputLandAreaScreen(navController: NavController) {
    var landArea by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var selectedCrop by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val crops = listOf("Banana", "Cassava", "Sweet Potato", "Coconut", "Corn")
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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

                        // Length Input with Increment/Decrement
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

                        // Width Input with Increment/Decrement
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

// Measurement Conversion Utility
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
```

### Explanation

Figure 19 showcases the land area estimation GUI which provides a sophisticated interface for farmers to define their agricultural plot dimensions in multiple ways. The `InputLandAreaScreen` function implements a state-based workflow where farmers first enter the total land area in square meters, and then the application automatically calculates initial length and width dimensions by computing the square root for a balanced rectangular approximation. The key innovation is the bi-directional synchronization between area, length, and width fields: when a farmer adjusts the length using increment/decrement buttons, the width is automatically recalculated to maintain the original area (`width = area / length`), and vice versa for width adjustments. This prevents mathematical inconsistencies and improves user experience. The embedded `MeasurementConverter` object provides extensive unit conversion capabilities, supporting multiple length units (meters, feet, kilometers, miles, centimeters, inches) and area units (square meters, square feet, hectares, acres, square kilometers, square miles), addressing the diverse measurement systems used globally by farmers. The "Conversion Tool" accessible via the Info icon dialog allows farmers to input measurements in their preferred units and automatically converts them to square meters for internal system processing.

---

## Figure 20: GUI for Crop Recommendation

### Code Snippet: ONNX-based Crop Recommendation with Soil Analysis

```kotlin
// Crop Recommendation Constants and Data Classes
enum class CropRecommendationStep {
    START,
    LOADING,
    RESULTS
}

data class CropPrediction(
    val cropName: String,
    val confidence: Float,
    val percentage: Int,
    val color: Color,
    val icon: String,
    val reasoning: String
)

object CropConstants {
    // Order MUST match LabelEncoder.classes_ from ML model
    val CROP_NAMES = listOf(
        "Banana",           // Index 0
        "Cassava",          // Index 1
        "Corn",             // Index 2
        "Coconut",          // Index 3
        "Sweet Potato"      // Index 4
    )

    val CROP_COLORS = mapOf(
        "Banana" to Color(0xFFFFD700),
        "Cassava" to Color(0xFFD2B48C),
        "Sweet Potato" to Color(0xFFFF8C69),
        "Corn" to Color(0xFFFFD700),
        "Coconut" to Color(0xFF8B4513)
    )

    fun getReasoningForConfidence(cropName: String, confidence: Float): String {
        return when {
            confidence >= 0.8 -> "Excellent match - highly recommended"
            confidence >= 0.6 -> "Good match - well-suited for conditions"
            confidence >= 0.4 -> "Moderate match - may require adjustments"
            confidence >= 0.2 -> "Fair match - consider other options"
            else -> "Low compatibility - not recommended"
        }
    }
}

// ONNX Model Inference Function
fun runOnnxInference(
    context: Context,
    soilDataList: List<SoilData>
): List<CropPrediction> {
    return try {
        if (soilDataList.isEmpty()) {
            Log.w("CropRecommendation", "No soil data available")
            return getDefaultRecommendations()
        }

        // Calculate averages from all soil data samples
        val avgNitrogen = soilDataList.map { it.nitrogen }.average().toFloat()
        val avgPhosphorus = soilDataList.map { it.phosphorus }.average().toFloat()
        val avgPotassium = soilDataList.map { it.potassium }.average().toFloat()
        val avgPhLevel = soilDataList.map { it.phLevel }.average().toFloat()
        val avgTemperature = soilDataList.map { it.temperature }.average().toFloat()
        val avgMoisture = soilDataList.map { it.moisture }.average().toFloat()

        Log.d(
            "CropRecommendation",
            "Average Soil Data - N: $avgNitrogen, P: $avgPhosphorus, K: $avgPotassium, " +
                    "pH: $avgPhLevel, Temp: $avgTemperature, Moisture: $avgMoisture"
        )

        // Prepare input data in shape [1, 6] - batch size 1, 6 features
        // Order: Nitrogen, Phosphorus, Potassium, pH Level, Temperature, Moisture
        val rawInputData = arrayOf(
            floatArrayOf(
                avgNitrogen,      // Nitrogen: raw mg/kg value
                avgPhosphorus,    // Phosphorus: raw mg/kg value
                avgPotassium,     // Potassium: raw mg/kg value
                avgPhLevel,       // pH Level: raw value (3.0-9.0)
                avgTemperature,   // Temperature: raw °C value
                avgMoisture       // Moisture: raw % value
            )
        )

        Log.d("CropRecommendation", "Input shape [1, 6]: ${rawInputData[0].contentToString()}")

        // Get ONNX model runner instance and run inference
        val modelRunner = OnnxModelRunner.getInstance(context)
        val outputData = modelRunner.runInference(rawInputData)

        Log.d("CropRecommendation", "Model output shape: ${outputData.size}x${if(outputData.isNotEmpty()) outputData[0].size else 0}")

        // Process model outputs and create crop predictions
        val predictions = mutableListOf<CropPrediction>()
        if (outputData.isNotEmpty() && outputData[0].size >= CropConstants.CROP_NAMES.size) {
            val confidences = outputData[0]

            // Convert raw confidences to normalized percentages
            val maxConfidence = confidences.maxOrNull() ?: 1f
            val normalizedConfidences = if (maxConfidence > 0) {
                confidences.map { it / maxConfidence }
            } else {
                confidences.toList()
            }

            CropConstants.CROP_NAMES.forEachIndexed { index, cropName ->
                if (index < normalizedConfidences.size) {
                    val confidence = normalizedConfidences[index]
                    val percentage = (confidence * 100).roundToInt()

                    predictions.add(
                        CropPrediction(
                            cropName = cropName,
                            confidence = confidence,
                            percentage = percentage,
                            color = CropConstants.CROP_COLORS[cropName] ?: Color.Gray,
                            icon = CropConstants.CROP_ICONS[cropName] ?: "🌾",
                            reasoning = CropConstants.getReasoningForConfidence(cropName, confidence)
                        )
                    )
                }
            }

            // Sort by confidence (highest first)
            predictions.sortByDescending { it.confidence }
        }

        predictions
    } catch (e: Exception) {
        Log.e("CropRecommendation", "Error running ONNX inference: ${e.message}", e)
        getDefaultRecommendations()
    }
}

// Crop Recommendation Screen Composable
@Composable
fun CropRecommendation(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    var currentStep by remember { mutableStateOf(CropRecommendationStep.START) }
    var cropPredictions by remember { mutableStateOf<List<CropPrediction>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDarkMode = isDarkModeState.value
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = { Text("Crop Recommendation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )

            when (currentStep) {
                CropRecommendationStep.START -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Analyze Soil Data for Crop Recommendation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                currentStep = CropRecommendationStep.LOADING
                                coroutineScope.launch {
                                    val soilDataList = soilDataViewModel.getAllSoilData()
                                    cropPredictions = runOnnxInference(context, soilDataList)
                                    currentStep = CropRecommendationStep.RESULTS
                                }
                            }
                        ) {
                            Text("Generate Recommendations")
                        }
                    }
                }
                CropRecommendationStep.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                CropRecommendationStep.RESULTS -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(cropPredictions) { prediction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White),
                                border = BorderStroke(1.dp, prediction.color)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = prediction.icon,
                                        fontSize = 40.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prediction.cropName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = textColor
                                        )
                                        Text(
                                            text = prediction.reasoning,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${prediction.percentage}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = prediction.color
                                        )
                                        LinearProgressIndicator(
                                            progress = { prediction.confidence },
                                            color = prediction.color,
                                            trackColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0),
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Explanation

Figure 20 presents the crop recommendation system, which represents the pinnacle of the Binhi app's machine learning integration by combining average soil parameters from all collected sampling points with an ONNX-optimized deep learning model to generate scientifically-backed crop recommendations. The `runOnnxInference()` function aggregates soil data from multiple locations by calculating averages for nitrogen, phosphorus, potassium, pH level, temperature, and moisture, creating a comprehensive soil profile that accurately represents the entire farm. The function normalizes this multi-parameter soil data into a [1, 6] shaped tensor matching the model's expected input format and executes it through the `OnnxModelRunner` for efficient on-device inference. The model outputs raw confidence scores for each crop that are normalized relative to the highest confidence value, converting them into interpretable percentages for end-user consumption. The resulting `CropPrediction` objects encapsulate the recommendation data including the crop name, numerical confidence score, percentage representation, color coding for visual differentiation, emoji icon for quick visual recognition, and contextual reasoning messages that explain the suitability level. The `CropRecommendation` composable implements a three-step workflow: START (user initiates analysis), LOADING (showing progress indicator while inference runs), and RESULTS (displaying sorted predictions in a scrollable list with visual progress indicators, colored borders matching crop types, and detailed reasoning for each recommendation). The implementation accounts for dark mode preferences through dynamic color selection, providing an accessible interface for farmers to make informed crop selection decisions based on their specific soil conditions.

---

## Summary

The Binhi app implements a comprehensive agricultural technology stack combining hardware sensor integration (Figure 17), user input interfaces for crop and land planning (Figures 18-19), and advanced machine learning-driven recommendations (Figure 20). The system seamlessly integrates microcontroller firmware for real-time soil sensor communication, a responsive mobile UI built with Jetpack Compose for data entry and visualization, and ONNX-optimized neural network inference for intelligent crop recommendations, creating a complete solution for optimizing agricultural productivity based on soil analysis and farm planning.

