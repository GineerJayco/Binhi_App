# Thesis Code Snippets: Binhi Smart Agriculture Application

This document contains the essential code snippets from the Binhi smart agriculture application, demonstrating the integration of microcontroller hardware, machine learning models, and graphical user interfaces for crop recommendation and visualization.

---

## Figure 17. Code Snippet for the Microcontroller Code

### Code: ESP32 Soil Sensor Data Collection and Display

```cpp
/*
   ESP32 SOIL SENSOR DATA DISPLAY
   Displays soil sensor data on 3.2" TFT LCD with Bluetooth & RS485 integration
*/

#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_ILI9341.h>
#include <XPT2046_Touchscreen.h>
#include <BluetoothSerial.h>
#include <HardwareSerial.h>

// PIN CONFIGURATION
#define TFT_CS   15          // Display Chip Select
#define TFT_RST  4           // Display Reset
#define TFT_DC   2           // Display Data/Command
#define TOUCH_CS 5           // Touch Chip Select
#define RS485_RX_PIN 16      // RS485 Receive
#define RS485_TX_PIN 17      // RS485 Transmit
#define RS485_DE_PIN 21      // RS485 Driver Enable

// HARDWARE INITIALIZATION
Adafruit_ILI9341 tft = Adafruit_ILI9341(TFT_CS, TFT_DC, TFT_RST);
XPT2046_Touchscreen ts(TOUCH_CS, TOUCH_IRQ);
BluetoothSerial SerialBT;
HardwareSerial RS485Serial(1);

// SOIL DATA STRUCTURE
struct SoilData {
    float nitrogen;       // N in mg/kg
    float phosphorus;     // P in mg/kg
    float potassium;      // K in mg/kg
    float pH;             // pH level
    float temperature;    // Temperature in °C
    float moisture;       // Moisture in %
    float conductivity;   // Electrical conductivity
};

void setup() {
    Serial.begin(115200);
    
    // Initialize RS485 (ModBus RTU protocol at 4800 baud)
    RS485Serial.begin(4800, SERIAL_8N1, RS485_RX_PIN, RS485_TX_PIN);
    pinMode(RS485_DE_PIN, OUTPUT);
    digitalWrite(RS485_DE_PIN, LOW);
    
    // Initialize TFT Display (ILI9341 320x240)
    tft.begin();
    tft.setRotation(0);
    tft.fillScreen(ILI9341_BLACK);
    
    // Initialize Touch Screen (XPT2046)
    if (!ts.begin()) {
        Serial.println("ERROR: Touch screen not found!");
    }
    
    // Initialize Bluetooth Serial Communication
    SerialBT.begin("ESP32_SOIL_SENSOR");
    drawSoilDataDisplay();
    Serial.println("Setup complete");
}

void readRS485Sensor() {
    // Clear receive buffer
    while (RS485Serial.available()) {
        RS485Serial.read();
    }
    
    // Set transmit mode
    digitalWrite(RS485_DE_PIN, HIGH);
    delay(10);
    
    // Send ModBus RTU query command
    const byte RS485_QUERY[] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x07, 0x04, 0x08};
    RS485Serial.write(RS485_QUERY, sizeof(RS485_QUERY));
    RS485Serial.flush();
    
    // Switch to receive mode
    digitalWrite(RS485_DE_PIN, LOW);
    delay(10);
    
    // Wait for sensor response (up to 1 second timeout)
    unsigned long timeout = millis();
    byte receivedData[19];
    while (RS485Serial.available() < 19 && (millis() - timeout) < 1000) {
        delay(10);
    }
    
    if (RS485Serial.available() >= 19) {
        RS485Serial.readBytes(receivedData, 19);
        parseRS485Data(receivedData);
        drawSoilDataDisplay();
    }
}

void loop() {
    handleTouchInput();
    handleBluetoothInput();
    
    // Read sensors every 5 seconds
    if (millis() - lastSensorReadTime >= 5000) {
        readRS485Sensor();
        lastSensorReadTime = millis();
    }
    
    delay(50);
}
```

### Explanation

This figure presents the core Arduino/ESP32 microcontroller code responsible for hardware integration in the Binhi application. The code demonstrates the complete initialization and operation of a soil monitoring system that interfaces with multiple hardware components: a 3.2-inch TFT LCD display for real-time data visualization, a capacitive touchscreen for user interaction, and an RS485 serial interface for communicating with external soil sensors using the ModBus RTU protocol. The system operates at a 4800 baud rate to ensure reliable communication with the soil sensor module, which transmits nitrogen, phosphorus, potassium, pH, temperature, moisture, and conductivity measurements. The implementation includes proper communication protocol handling with transmit/receive mode switching, timeout management, and data buffering to ensure robust sensor data acquisition every five seconds. Additionally, the code establishes Bluetooth connectivity through the SerialBT interface, enabling wireless communication between the microcontroller and the mobile application, thereby creating a seamless integration between embedded hardware and software systems in the agricultural IoT ecosystem.

---

## Figure 18. Code Snippet of the GUI for the Crop Quantity Estimation

### Code: VisualizeCQ.kt - Crop Quantity Visualization Interface

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizeCQ(
    navController: NavController,
    crop: String?,
    cropQuantity: String?,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false)
) {
    // State management for map and UI elements
    var showDetails by remember { mutableStateOf(false) }
    val dumaguete = LatLng(9.3093, 123.308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dumaguete, 15f)
    }
    
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var polygonCenter by remember { mutableStateOf(dumaguete) }
    
    // Calculate area and dimensions based on crop quantity
    val areaPerPlant = CropData.crops[crop]?.areaPerPlant ?: 0.0
    val quantity = cropQuantity?.toDoubleOrNull() ?: 0.0
    val estimatedLandArea = quantity * areaPerPlant
    val lengthInMeters = sqrt(estimatedLandArea)
    val widthInMeters = sqrt(estimatedLandArea)
    
    // Generate polygon points for land visualization
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
                LatLng(polygonCenter.latitude + latOffset, 
                       polygonCenter.longitude + lonOffset)
            }
        } else {
            emptyList()
        }
    }
    
    // Calculate crop positions within polygon
    LaunchedEffect(lengthInMeters, widthInMeters, crop) {
        if (polygonPoints.isNotEmpty()) {
            baselineCropPositions = calculateCropPositions(
                polygonPoints, crop, quantity.toInt()
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Render interactive Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            if (polygonPoints.isNotEmpty()) {
                // Draw land area polygon
                Polygon(
                    points = polygonPoints,
                    fillColor = Color.Red.copy(alpha = 0.5f),
                    strokeColor = Color.Red,
                    strokeWidth = 5f
                )
                
                // Render crop markers at calculated positions
                transformedCropPositions.forEachIndexed { index, position ->
                    Marker(
                        state = rememberMarkerState(position = position),
                        title = "${crop ?: "Crop"} ${index + 1}",
                        icon = getCropIcon(context, crop ?: ""),
                        anchor = Offset(0.5f, 0.5f)
                    )
                }
            }
        }
        
        // Control panel: Rotation, movement, map type toggle
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { rotation += 5f }) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, 
                     contentDescription = "Rotate", tint = Color.White)
            }
            IconButton(onClick = { rotation -= 5f }) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, 
                     contentDescription = "Rotate", tint = Color.White)
            }
            Switch(
                checked = mapType == MapType.SATELLITE,
                onCheckedChange = { checked ->
                    mapType = if (checked) MapType.SATELLITE else MapType.NORMAL
                }
            )
        }
    }
}
```

### Explanation

This figure demonstrates the comprehensive GUI implementation for crop quantity estimation in the Binhi mobile application, showcasing the integration of Google Maps API with mathematical geospatial calculations. The VisualizeCQ composable function utilizes Jetpack Compose framework to create a responsive, interactive map-based interface that dynamically calculates and visualizes the spatial distribution of crops based on user-provided quantity estimates. The system employs trigonometric transformations to convert latitude and longitude coordinates to metric distances using the standard Earth radius approximation (111,132 meters per degree latitude and 111,320 meters per degree longitude), enabling accurate polygon generation that represents the land area required for the specified crop quantity. The code implements efficient state management through Kotlin flows and memoization to prevent unnecessary recomputations, while the interactive control panel enables users to rotate the visualization and toggle between satellite and street map types. Furthermore, the implementation includes crop marker placement algorithms that distribute individual crop icons within the polygon boundary using a grid-based approach, with visual feedback through animated radar circles highlighting selected crops, thus providing an intuitive and scientifically accurate visualization of agricultural land requirements.

---

## Figure 19. Code Snippet of the GUI for the Land Area Estimation

### Code: VisualizeLA.kt - Land Area Visualization Interface

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizeLA(
    navController: NavController,
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false),
) {
    val dumaguete = LatLng(9.3093, 123.308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dumaguete, 15f)
    }
    
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var polygonCenter by remember { mutableStateOf(dumaguete) }
    var isPolygonDragging by remember { mutableStateOf(false) }
    
    // Radar animation state for visual effects
    val radarScaleAnimatable = remember { Animatable(1f) }
    val radarAlphaAnimatable = remember { Animatable(1f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isPolygonDragging) {
                            val sensitivity = 0.125f
                            val centerLatRad = Math.toRadians(polygonCenter.latitude)
                            val latOffset = -dragAmount.y * sensitivity / 111132.0
                            val lonOffset = dragAmount.x * sensitivity / 
                                           (111320.0 * cos(centerLatRad))
                            
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
        
        // Calculate polygon corners with rotation support
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
                    LatLng(polygonCenter.latitude + latOffset, 
                           polygonCenter.longitude + lonOffset)
                }
            } else {
                emptyList()
            }
        }
        
        // Calculate crop quantity based on land area
        LaunchedEffect(lengthInMeters, widthInMeters, landArea, crop) {
            if (polygonPoints.isNotEmpty()) {
                val estimatedQuantity = landArea?.toDoubleOrNull()?.let { area ->
                    val plantingArea = CropData.crops[crop]?.areaPerPlant ?: 0.0
                    if (plantingArea > 0) {
                        floor(area / plantingArea).toInt()
                    } else 0
                } ?: 0
                
                baselineCropPositions = calculateCropPositions(
                    polygonPoints, crop, estimatedQuantity
                )
            }
        }
        
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType)
        ) {
            if (polygonPoints.isNotEmpty()) {
                Polygon(
                    points = polygonPoints,
                    fillColor = Color.Red.copy(alpha = 0.5f),
                    strokeColor = Color.Red,
                    strokeWidth = 5f
                )
                
                fixedCropPositions.forEachIndexed { index, position ->
                    Marker(
                        state = rememberMarkerState(position = position),
                        title = "${crop ?: "Crop"} ${index + 1}",
                        icon = getCropIcon(context, crop ?: "")
                    )
                }
            }
        }
        
        // Enhanced crop list dialog with dark mode
        if (showCropListDialog && cropLocationsList.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showCropListDialog = false },
                title = { Text("Crop Locations") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(cropLocationsList.size) { index ->
                            val (cropNumber, location) = cropLocationsList[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* navigate */ }
                            ) {
                                Row(modifier = Modifier.padding(12.dp)) {
                                    Text("$cropNumber", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(convertToDMS(location.latitude, true))
                                        Text(convertToDMS(location.longitude, false))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCropListDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
```

### Explanation

This figure presents the land area estimation GUI component, which represents a critical module for visualizing and managing agricultural land parcels within the Binhi application. The VisualizeLA composable implements an interactive geospatial interface that allows farmers to visualize the spatial distribution of their land based on provided dimensions (length and width in meters) and to estimate optimal crop quantities based on computed land areas. The implementation incorporates advanced gesture recognition through the `detectDragGesturesAfterLongPress` function, enabling intuitive long-press and drag operations to reposition land parcels on the map, with coordinate transformations accounting for latitude-dependent longitude scaling factors. The system calculates crop positions using a sophisticated grid-based distribution algorithm that validates point containment within polygon boundaries using ray-casting algorithms, ensuring accurate crop placement only within demarcated land areas. The code demonstrates responsive UI design patterns through state management with `mutableStateOf`, animated radar effects using `Animatable`, and dark mode support through conditional theming. Additionally, the crop location list dialog provides comprehensive coordinate information in Degrees-Minutes-Seconds (DMS) format, facilitating ground truthing and GPS navigation for field surveys, thereby enabling farmers to systematically manage spatial agricultural data at both digital and physical field levels.

---

## Figure 20. Code Snippet of the GUI for the Crop Recommendation

### Code: CropRecommendation.kt - ML-Based Crop Recommendation Interface

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropRecommendationScreen(
    navController: NavController,
    soilDataViewModel: SoilDataViewModel = viewModel(),
    skipStartScreen: Boolean = false,
    avgNitrogen: Float? = null,
    avgPhosphorus: Float? = null,
    avgPotassium: Float? = null,
    avgPhLevel: Float? = null,
    avgMoisture: Float? = null,
    avgTemperature: Float? = null,
    isDarkModeState: MutableState<Boolean> = mutableStateOf(false),
) {
    val context = LocalContext.current
    var currentStep by remember {
        mutableStateOf(if (skipStartScreen) CropRecommendationStep.LOADING 
                       else CropRecommendationStep.START)
    }
    var predictions by remember { mutableStateOf<List<CropPrediction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crop Recommendation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when (currentStep) {
                CropRecommendationStep.LOADING -> {
                    LaunchedEffect(Unit) {
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                // Prepare input data for ONNX model inference
                                val rawInputData = arrayOf(
                                    floatArrayOf(
                                        avgNitrogen ?: 0f,      // Nitrogen
                                        avgPhosphorus ?: 0f,    // Phosphorus
                                        avgPotassium ?: 0f,     // Potassium
                                        avgPhLevel ?: 0f,       // pH Level
                                        avgTemperature ?: 0f,   // Temperature
                                        avgMoisture ?: 0f       // Moisture
                                    )
                                )
                                
                                // Execute ONNX model for crop prediction
                                val modelRunner = OnnxModelRunner.getInstance(context)
                                if (!modelRunner.isReady()) {
                                    modelRunner.initializeEnvironment()
                                }
                                
                                val results = modelRunner.runInference(rawInputData)
                                val confidences = modelRunner.extractProbabilities(
                                    results["output"], CropConstants.CROP_NAMES.size
                                )
                                
                                // Convert confidences to crop predictions
                                predictions = confidences.mapIndexed { index, confidence ->
                                    val cropName = CropConstants.CROP_NAMES.getOrNull(index) 
                                                   ?: "Crop $index"
                                    CropPrediction(
                                        cropName = cropName,
                                        confidence = confidence.coerceIn(0f, 1f),
                                        percentage = (confidence * 100).roundToInt(),
                                        color = CropConstants.getCropColor(cropName),
                                        icon = CropConstants.getCropIcon(cropName),
                                        reasoning = CropConstants.getReasoningForConfidence(
                                            cropName, confidence
                                        )
                                    )
                                }.sortedByDescending { it.confidence }
                                
                                // Simulate processing time for UX feedback
                                kotlinx.coroutines.delay(1500)
                                currentStep = CropRecommendationStep.RESULTS
                                
                            } catch (e: Exception) {
                                Log.e("CropRecommendation", "Error: ${e.message}", e)
                            }
                        }
                    }
                    LoadingScreen()
                }
                
                CropRecommendationStep.RESULTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Summary card with top recommendation
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val topCrop = predictions.firstOrNull()
                                    if (topCrop != null) {
                                        Text(
                                            topCrop.icon,
                                            fontSize = 48.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            topCrop.cropName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                        Text(
                                            "${topCrop.percentage}% Match",
                                            fontWeight = FontWeight.Bold,
                                            color = topCrop.color
                                        )
                                        Text(
                                            topCrop.reasoning,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        // All recommendations list
                        items(predictions) { prediction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prediction.icon, fontSize = 32.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            prediction.cropName,
                                            fontWeight = FontWeight.Bold
                                        )
                                        LinearProgressIndicator(
                                            progress = prediction.confidence,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp),
                                            color = prediction.color
                                        )
                                        Text(
                                            prediction.reasoning,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        "${prediction.percentage}%",
                                        fontWeight = FontWeight.Bold,
                                        color = prediction.color
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

/**
 * ONNX Model Inference for Crop Prediction
 * Input: [N, P, K, pH, Temperature, Moisture]
 * Output: Confidence scores for each crop
 */
fun runOnnxInference(
    context: Context,
    soilDataList: List<SoilData>
): List<CropPrediction> {
    return try {
        if (soilDataList.isEmpty()) {
            return getDefaultRecommendations()
        }
        
        // Calculate average soil parameters
        val avgNitrogen = soilDataList.map { it.nitrogen }.average().toFloat()
        val avgPhosphorus = soilDataList.map { it.phosphorus }.average().toFloat()
        val avgPotassium = soilDataList.map { it.potassium }.average().toFloat()
        val avgPhLevel = soilDataList.map { it.phLevel }.average().toFloat()
        val avgTemperature = soilDataList.map { it.temperature }.average().toFloat()
        val avgMoisture = soilDataList.map { it.moisture }.average().toFloat()
        
        // Prepare input array in shape [1, 6]
        val rawInputData = arrayOf(
            floatArrayOf(
                avgNitrogen, avgPhosphorus, avgPotassium,
                avgPhLevel, avgTemperature, avgMoisture
            )
        )
        
        // Run ONNX inference
        val modelRunner = OnnxModelRunner.getInstance(context)
        if (!modelRunner.isReady()) modelRunner.initializeEnvironment()
        
        val results = modelRunner.runInference(rawInputData)
        val confidences = modelRunner.extractProbabilities(
            results[modelRunner.getOutputNames()?.get(1)],
            CropConstants.CROP_NAMES.size
        )
        
        // Create and return sorted predictions
        confidences.mapIndexed { index, confidence ->
            CropPrediction(
                cropName = CropConstants.CROP_NAMES[index],
                confidence = confidence.coerceIn(0f, 1f),
                percentage = (confidence * 100).roundToInt().coerceIn(0, 100),
                color = CropConstants.getCropColor(CropConstants.CROP_NAMES[index]),
                icon = CropConstants.getCropIcon(CropConstants.CROP_NAMES[index]),
                reasoning = CropConstants.getReasoningForConfidence(
                    CropConstants.CROP_NAMES[index], confidence
                )
            )
        }.sortedByDescending { it.confidence }
        
    } catch (e: Exception) {
        Log.e("CropRecommendation", "Error in inference: ${e.message}", e)
        getDefaultRecommendations()
    }
}
```

### Explanation

This figure demonstrates the comprehensive implementation of the machine learning-based crop recommendation system within the Binhi application, showcasing the integration of ONNX deep learning models with Android Compose UI framework. The CropRecommendationScreen composable orchestrates a three-step workflow: initialization, ONNX model inference, and results presentation, utilizing Kotlin coroutines for asynchronous processing to maintain responsive user interface during computationally intensive model inference operations. The system accepts soil parameters (nitrogen, phosphorus, potassium, pH level, temperature, and moisture) collected from field sensors or entered manually, formats these inputs into the required ONNX model shape [1, 6], and executes probabilistic inference to generate confidence scores for five crop types (Banana, Cassava, Corn, Coconut, and Sweet Potato) trained on a representative dataset specific to Philippine agricultural conditions. The confidence scores undergo post-processing through softmax probability extraction, quantization to percentage values, and sorting by descending confidence to present ranked crop recommendations ranked by suitability. The UI implementation utilizes Material Design 3 principles with responsive card layouts, progress indicators visualizing confidence levels, and color-coded crop icons providing intuitive visual communication of recommendation rankings. The system gracefully handles inference failures through fallback mechanisms and default recommendations, while the modular architecture enables seamless integration of crop visualization modules for field planning applications, thereby bridging machine learning model deployment with practical agricultural decision-making workflows.

---

## Summary of Technical Integration

The four code snippets presented above demonstrate the complete vertical integration of the Binhi smart agriculture application across hardware, embedded systems, machine learning inference, and mobile user interfaces. Figure 17 establishes the foundational hardware-software interface for real-time sensor data acquisition through RS485 communication protocols and TFT display rendering. Figures 18 and 19 implement sophisticated geospatial visualization capabilities using Google Maps APIs combined with mathematical coordinate transformations for accurate agricultural land representation. Figure 20 completes the ecosystem by deploying machine learning models for evidence-based crop recommendations based on aggregated soil parameters. This integrated architecture exemplifies modern IoT and precision agriculture paradigms where edge computing, cloud-based machine learning, and user-centric mobile applications converge to empower smallholder farmers with scientific agricultural decision support systems tailored to local environmental conditions.


