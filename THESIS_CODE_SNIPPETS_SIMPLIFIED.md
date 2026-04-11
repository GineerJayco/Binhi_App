# Thesis Code Snippets: Binhi Smart Agriculture Application (Simplified)

This document contains simplified code snippets from the Binhi application, showing how the system connects sensors, displays information, and recommends crops.

---

## Figure 17. Microcontroller Code - Soil Sensor Data Collection

### Code: ESP32 Soil Sensor Reading

```cpp
#include <HardwareSerial.h>
#include <Adafruit_ILI9341.h>
#include <BluetoothSerial.h>

// Setup hardware connections
Adafruit_ILI9341 tft = Adafruit_ILI9341(15, 2, 4);  // TFT Display
HardwareSerial RS485Serial(1);                      // RS485 Sensor
BluetoothSerial SerialBT;                           // Bluetooth

// Soil data structure
struct SoilData {
    float nitrogen;
    float phosphorus;
    float potassium;
    float pH;
    float temperature;
    float moisture;
};

SoilData soilData;

void setup() {
    Serial.begin(115200);
    RS485Serial.begin(4800, SERIAL_8N1, 16, 17);  // Start RS485
    tft.begin();
    SerialBT.begin("ESP32_SOIL_SENSOR");
    tft.fillScreen(ILI9341_BLACK);
}

void readRS485Sensor() {
    // Send query to soil sensor
    byte query[] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x07, 0x04, 0x08};
    RS485Serial.write(query, sizeof(query));
    delay(100);
    
    // Read sensor response
    if (RS485Serial.available() >= 19) {
        byte data[19];
        RS485Serial.readBytes(data, 19);
        parseData(data);
    }
}

void loop() {
    readRS485Sensor();
    delay(5000);  // Read every 5 seconds
}
```

### Explanation

This code shows how the microcontroller (ESP32) collects soil data from sensors. The system has three main parts working together: (1) an RS485 connection that reads soil sensor data like nitrogen, phosphorus, potassium, pH, temperature, and moisture, (2) a TFT display screen that shows the data to the user, and (3) Bluetooth that sends the data to a mobile phone. The microcontroller sends a request to the soil sensor every 5 seconds and waits for the response with the measurements. This creates a continuous loop where the system keeps checking the soil conditions and displaying the latest information in real-time.

---

## Figure 18. Crop Quantity Estimation GUI

### Code: VisualizeCQ.kt - Showing Crop Distribution on Map

```kotlin
@Composable
fun VisualizeCQ(
    crop: String?,
    cropQuantity: String?
) {
    val dumaguete = LatLng(9.3093, 123.308)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(dumaguete, 15f)
    }
    
    var rotation by remember { mutableFloatStateOf(0f) }
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    
    // Calculate land area needed for crops
    val areaPerPlant = CropData.crops[crop]?.areaPerPlant ?: 0.0
    val quantity = cropQuantity?.toDoubleOrNull() ?: 0.0
    val totalArea = quantity * areaPerPlant
    val sideLength = sqrt(totalArea)
    
    // Create rectangle on map to show land area
    val polygonPoints = remember(rotation, sideLength) {
        generateRectangle(dumaguete, sideLength, rotation)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Show map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType)
        ) {
            // Draw land area rectangle
            Polygon(
                points = polygonPoints,
                fillColor = Color.Red.copy(alpha = 0.5f),
                strokeColor = Color.Red
            )
            
            // Place crop markers
            cropPositions.forEachIndexed { index, pos ->
                Marker(
                    state = rememberMarkerState(position = pos),
                    title = "${crop} ${index + 1}"
                )
            }
        }
        
        // Rotate button
        IconButton(onClick = { rotation += 5f }) {
            Icon(Icons.AutoMirrored.Filled.RotateRight, tint = Color.White)
        }
    }
}
```

### Explanation

This screen helps farmers see how many crops they can grow in a given area. When the farmer enters how many crops they want to plant, the app calculates how much land is needed and draws a red rectangle on the map showing that area. Then it places individual crop markers (like little pictures) inside the rectangle to show exactly where each crop should go. The farmer can rotate the rectangle to match their actual land shape. This makes it easy to understand if the crops will fit in their field before they start planting.

---

## Figure 19. Land Area Estimation GUI

### Code: VisualizeLA.kt - Showing Land with Crop Positions

```kotlin
@Composable
fun VisualizeLA(
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?
) {
    var polygonCenter by remember { mutableStateOf(LatLng(9.3093, 123.308)) }
    var rotation by remember { mutableFloatStateOf(0f) }
    
    // Get dimensions
    val lengthMeters = length?.toDoubleOrNull() ?: 0.0
    val widthMeters = width?.toDoubleOrNull() ?: 0.0
    
    // Calculate how many crops fit in this land
    val areaPerPlant = CropData.crops[crop]?.areaPerPlant ?: 0.0
    val totalArea = landArea?.toDoubleOrNull() ?: 0.0
    val cropCount = (totalArea / areaPerPlant).toInt()
    
    // Create rectangle polygon
    val polygonPoints = createPolygon(
        polygonCenter, lengthMeters, widthMeters, rotation
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(Modifier.fillMaxSize()) {
            // Show land boundary
            Polygon(
                points = polygonPoints,
                fillColor = Color.Red.copy(alpha = 0.5f),
                strokeWidth = 5f
            )
            
            // Show each crop location
            for (i in 0 until cropCount) {
                Marker(
                    position = cropPositions[i],
                    title = "${crop} #${i + 1}"
                )
            }
        }
        
        // Drag to move land
        LaunchedEffect(Unit) {
            detectDragGestures { change, dragAmount ->
                polygonCenter = updatePosition(polygonCenter, dragAmount)
            }
        }
    }
}
```

### Explanation

This screen shows farmers their actual land on a map with all crops marked. The farmer inputs their land size (length and width in meters), and the app draws the exact shape of their land on the map. Then it calculates how many crops they can plant based on the land size and shows each crop as a marker on the map. The farmer can drag the land around the map to position it correctly where their actual field is located. This helps them plan their planting and know exactly where each crop should go in their field.

---

## Figure 20. Crop Recommendation GUI

### Code: CropRecommendation.kt - ML Model Prediction

```kotlin
@Composable
fun CropRecommendationScreen(
    avgNitrogen: Float?,
    avgPhosphorus: Float?,
    avgPotassium: Float?,
    avgPhLevel: Float?,
    avgMoisture: Float?,
    avgTemperature: Float?
) {
    var predictions by remember { mutableStateOf<List<CropPrediction>>(emptyList()) }
    val context = LocalContext.current
    
    // Load and run ML model
    LaunchedEffect(Unit) {
        predictions = runOnnxInference(
            context,
            avgNitrogen ?: 0f,
            avgPhosphorus ?: 0f,
            avgPotassium ?: 0f,
            avgPhLevel ?: 0f,
            avgTemperature ?: 0f,
            avgMoisture ?: 0f
        )
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Show best crop recommendation
        item {
            val topCrop = predictions.firstOrNull()
            if (topCrop != null) {
                Card(modifier = Modifier.padding(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(topCrop.icon, fontSize = 48.sp)
                        Text(topCrop.cropName, fontWeight = FontWeight.Bold)
                        Text("${topCrop.percentage}% Match", color = topCrop.color)
                        Text(topCrop.reasoning)
                    }
                }
            }
        }
        
        // Show all crop options
        items(predictions) { prediction ->
            CropCard(prediction)
        }
    }
}

fun runOnnxInference(
    context: Context,
    n: Float, p: Float, k: Float, 
    ph: Float, temp: Float, moist: Float
): List<CropPrediction> {
    val modelRunner = OnnxModelRunner.getInstance(context)
    val input = arrayOf(floatArrayOf(n, p, k, ph, temp, moist))
    val results = modelRunner.runInference(input)
    
    return CropConstants.CROP_NAMES.mapIndexed { idx, name ->
        CropPrediction(name, results[idx], results[idx] * 100)
    }.sortedByDescending { it.confidence }
}
```

### Explanation

This screen uses artificial intelligence to recommend the best crops for the farmer's soil. The system takes soil measurements (nitrogen, phosphorus, potassium, pH, temperature, and moisture) and feeds them into a machine learning model. The model analyzes the soil conditions and returns scores for each crop showing how well it will grow. The best recommendation appears at the top with a big card showing the crop, its name, and a percentage saying how good it is for that soil. Below that, all other crop options are listed with progress bars showing how suitable each one is. This helps farmers make smart decisions about what to plant based on their soil conditions.

---

## Summary

The four code snippets show how Binhi connects everything together: (1) **Figure 17** shows how the soil sensor reads data, (2) **Figure 18** displays crops on a map based on quantity, (3) **Figure 19** shows crops on a map based on land size, and (4) **Figure 20** uses AI to recommend the best crops. All these parts work together to help farmers grow better crops by giving them information about their soil and land.


