# Soil Data Storage - Code Examples & Integration Guide

## Quick Integration Examples

### 1. Using the ViewModel in Your Composable

```kotlin
import com.example.binhi.viewmodel.SoilDataViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MyScreen() {
    // Get ViewModel instance (same instance across recompositions)
    val soilDataViewModel: SoilDataViewModel = viewModel()
    
    // Your composable code here
}
```

### 2. Saving Soil Data

```kotlin
// When user clicks "Save Data" button
val soilData = bluetoothResponse!!.toSoilData()

if (soilData != null) {
    val success = soilDataViewModel.saveSoilData(
        location = selectedDot!!,
        data = soilData
    )
    
    if (success) {
        // Show success message
        showSaveSuccessMessage = true
    } else {
        // Data validation failed - show error
        showError = true
        errorMessage = "Invalid soil data values"
    }
}
```

### 3. Retrieving Soil Data

```kotlin
// Check if location has data
if (soilDataViewModel.hasSoilData(selectedDot)) {
    // Load the data
    val soilData = soilDataViewModel.getSoilData(selectedDot)
    
    // Display it
    if (soilData != null) {
        Text("Nitrogen: ${soilData.nitrogen}")
        Text("Phosphorus: ${soilData.phosphorus}")
        Text("Potassium: ${soilData.potassium}")
        Text("pH: ${soilData.phLevel}")
        Text("Temp: ${soilData.temperature}°C")
        Text("Moisture: ${soilData.moisture}%")
    }
}
```

### 4. Auto-Loading Data on Selection

```kotlin
// In GetSoilData composable
LaunchedEffect(selectedDot) {
    if (selectedDot != null && soilDataViewModel.hasSoilData(selectedDot!!) && currentSoilData == null) {
        // Automatically load stored data
        currentSoilData = soilDataViewModel.getSoilData(selectedDot!!)
    }
}
```

### 5. Color-Coding Markers

```kotlin
// In GoogleMap Marker rendering
dots.forEach { dot ->
    val markerColor = if (soilDataViewModel.hasSoilData(dot)) {
        BitmapDescriptorFactory.HUE_GREEN  // Green = has data
    } else {
        BitmapDescriptorFactory.HUE_BLUE   // Blue = no data
    }
    
    Marker(
        state = MarkerState(position = dot),
        icon = BitmapDescriptorFactory.defaultMarker(markerColor),
        onClick = {
            selectedDot = dot
            currentSoilData = null  // Reset to load from ViewModel
            showDialog = true
            true
        }
    )
}
```

## Parsing Examples

### Example 1: Standard Format

```kotlin
val response = "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
val sensorData = SoilSensorData.fromResponse(response)

// Results in:
// nitrogen = 12
// phosphorus = 7
// potassium = 9
// phLevel = 6.5f
// temperature = 29.4f
// moisture = 62
// isError = false
```

### Example 2: With Whitespace

```kotlin
val response = "NPK = 10 , 20 , 30 ; PH = 7.0 ; TEMP = 25.5 ; MOIST = 50"
val sensorData = SoilSensorData.fromResponse(response)

// Results in:
// nitrogen = 10
// phosphorus = 20
// potassium = 30
// phLevel = 7.0f
// temperature = 25.5f
// moisture = 50
// isError = false (flexible parsing handles spaces)
```

### Example 3: Missing Fields (Fallback)

```kotlin
val response = "NPK=15,5,8"  // Missing PH, TEMP, MOIST
val sensorData = SoilSensorData.fromResponse(response)

// Results in:
// nitrogen = 15
// phosphorus = 5
// potassium = 8
// phLevel = 0f          (default)
// temperature = 0f      (default)
// moisture = 0          (default)
// isError = false       (still valid, just incomplete)
```

### Example 4: Error Handling

```kotlin
val response = "NPK=abc,def,ghi"  // Invalid format
val sensorData = SoilSensorData.fromResponse(response)

// Results in:
// nitrogen = 0
// phosphorus = 0
// potassium = 0
// phLevel = 0f
// temperature = 0f
// moisture = 0
// isError = true
// errorMessage = "Invalid format: NPK=abc,def,ghi"
```

## Data Model Examples

### Creating SoilData

```kotlin
// Manual creation for testing
val soilData = SoilData(
    nitrogen = 12,
    phosphorus = 7,
    potassium = 9,
    phLevel = 6.5f,
    temperature = 29.4f,
    moisture = 62
    // timestamp auto-generated as current time
)

// Validation
println(soilData.isValid())  // true if all ranges OK
println(soilData.toString()) // Formatted string
```

### Converting from Sensor Data

```kotlin
// Bluetooth reception
val bluetoothResponse = SoilSensorData.fromResponse(
    "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
)

// Convert to persistent storage
val soilData = bluetoothResponse.toSoilData()

// This creates:
// SoilData(
//     nitrogen = 12,
//     phosphorus = 7,
//     potassium = 9,
//     phLevel = 6.5f,
//     temperature = 29.4f,
//     moisture = 62,
//     timestamp = System.currentTimeMillis()
// )
```

## Dialog Implementation Examples

### Sample Location Dialog

```kotlin
if (showDialog && selectedDot != null) {
    Dialog(
        onDismissRequest = {
            showDialog = false
            selectedDot = null
            currentSoilData = null
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Coordinates
                Text(
                    text = "Sample Location",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Latitude: ${decimalToDMS(selectedDot!!.latitude, true)}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Show existing data if available
                if (currentSoilData != null) {
                    Text(
                        text = "Stored Soil Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green
                    )
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DataRow("Nitrogen", "${currentSoilData!!.nitrogen}")
                        DataRow("Phosphorus", "${currentSoilData!!.phosphorus}")
                        DataRow("pH Level", 
                            String.format("%.2f", currentSoilData!!.phLevel)
                        )
                        // ... more fields
                    }
                } else {
                    Text("No Data Stored", color = Color.Gray)
                }
                
                // Buttons
                Button(onClick = { /* Receive Data */ }) {
                    Text("Receive Data")
                }
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        }
    }
}
```

### Data Row Helper

```kotlin
@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Usage
DataRow("Nitrogen", "12")
DataRow("Temperature", "29.4°C")
DataRow("Moisture", "62%")
```

## Advanced Examples

### 1. Getting All Stored Locations

```kotlin
// Get set of all locations with saved data
val allLocations = soilDataViewModel.getAllStoredLocations()

// Display count
val dataCount = soilDataViewModel.getStoredDataCount()
println("Data stored for $dataCount locations")

// Filter markers
dots.filter { dot ->
    soilDataViewModel.hasSoilData(dot)
}.forEach { dotWithData ->
    // Do something with saved data locations
}
```

### 2. Exporting Data

```kotlin
// Export all soil data to CSV
val csvHeader = "Latitude,Longitude,Nitrogen,Phosphorus,Potassium,pH,Temperature,Moisture\n"

val csvData = soilDataViewModel.getAllStoredLocations().map { location ->
    val data = soilDataViewModel.getSoilData(location)!!
    "${location.latitude},${location.longitude}," +
    "${data.nitrogen},${data.phosphorus},${data.potassium}," +
    "${data.phLevel},${data.temperature},${data.moisture}"
}.joinToString("\n")

val csvFile = csvHeader + csvData
// Save csvFile to file...
```

### 3. Filtering by Parameter

```kotlin
// Find all locations with nitrogen > 15
val highNitrogenLocations = soilDataViewModel.getAllStoredLocations()
    .filter { location ->
        val data = soilDataViewModel.getSoilData(location)
        data != null && data.nitrogen > 15
    }

// Find locations with pH < 7 (acidic)
val acidicLocations = soilDataViewModel.getAllStoredLocations()
    .filter { location ->
        val data = soilDataViewModel.getSoilData(location)
        data != null && data.phLevel < 7.0f
    }
```

### 4. Statistics

```kotlin
// Calculate average nitrogen across all locations
val allData = soilDataViewModel.getAllStoredLocations()
    .mapNotNull { soilDataViewModel.getSoilData(it) }

val avgNitrogen = allData.map { it.nitrogen }.average()
val avgTemperature = allData.map { it.temperature }.average()
val avgMoisture = allData.map { it.moisture }.average()

println("Avg Nitrogen: $avgNitrogen")
println("Avg Temperature: $avgTemperature°C")
println("Avg Moisture: $avgMoisture%")
```

## Error Handling Examples

### Graceful Bluetooth Error

```kotlin
try {
    val response = bluetoothManager.sendCommandAndReceive("READ\n")
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
```

### Save with Validation

```kotlin
val soilData = bluetoothResponse!!.toSoilData()

if (soilData != null) {
    if (soilDataViewModel.saveSoilData(selectedDot!!, soilData)) {
        // Success
        currentSoilData = soilData
        showSaveSuccessMessage = true
        
        coroutineScope.launch {
            kotlinx.coroutines.delay(2000)
            showSaveSuccessMessage = false
            showBluetoothDialog = false
            bluetoothResponse = null
            showDialog = false
            selectedDot = null
        }
    } else {
        // Validation failed
        showError = true
        errorMessage = "Data validation failed. Check value ranges."
    }
} else {
    // Conversion failed
    showError = true
    errorMessage = "Could not convert Bluetooth data"
}
```

## Testing Examples

### Unit Test - Data Validation

```kotlin
@Test
fun testSoilDataValidation() {
    // Valid data
    val validData = SoilData(
        nitrogen = 10,
        phosphorus = 5,
        potassium = 8,
        phLevel = 6.5f,
        temperature = 25.0f,
        moisture = 60
    )
    assertTrue(validData.isValid())
    
    // Invalid pH
    val invalidPH = validData.copy(phLevel = 15.0f)
    assertFalse(invalidPH.isValid())
    
    // Invalid moisture
    val invalidMoisture = validData.copy(moisture = 150)
    assertFalse(invalidMoisture.isValid())
}
```

### Unit Test - Bluetooth Parsing

```kotlin
@Test
fun testBluetoothParsing() {
    // Test standard format
    val response = "NPK=12,7,9;PH=6.5;TEMP=29.4;MOIST=62"
    val data = SoilSensorData.fromResponse(response)
    
    assertEquals(12, data.nitrogen)
    assertEquals(7, data.phosphorus)
    assertEquals(9, data.potassium)
    assertEquals(6.5f, data.phLevel)
    assertEquals(29.4f, data.temperature)
    assertEquals(62, data.moisture)
    assertFalse(data.isError)
}
```

### Unit Test - ViewModel Operations

```kotlin
@Test
fun testViewModelStorage() {
    val viewModel = SoilDataViewModel()
    val location = LatLng(9.3, 123.3)
    val soilData = SoilData(12, 7, 9, 6.5f, 29.4f, 62)
    
    // Save data
    assertTrue(viewModel.saveSoilData(location, soilData))
    
    // Check existence
    assertTrue(viewModel.hasSoilData(location))
    
    // Retrieve data
    val retrieved = viewModel.getSoilData(location)
    assertEquals(retrieved, soilData)
    
    // Get all locations
    assertEquals(1, viewModel.getAllStoredLocations().size)
    
    // Delete data
    viewModel.deleteSoilData(location)
    assertFalse(viewModel.hasSoilData(location))
}
```

---

These examples should give you a solid foundation for integrating and using the soil data storage system in your app!

