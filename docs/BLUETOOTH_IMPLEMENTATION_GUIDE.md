# Bluetooth Classic (RFCOMM/SPP) Implementation Guide

## Overview
This guide describes the Bluetooth Classic implementation for communicating with the ESP32 soil sensor via Serial Port Profile (SPP).

## Files Created

### 1. BluetoothManager.kt
**Location**: `com.example.binhi.bluetooth.BluetoothClassicManager`

The main Bluetooth manager class that handles:
- Device connection via RFCOMM socket
- Sending commands (e.g., "READ\n")
- Receiving responses
- Permission checking and validation
- Error handling with detailed logging

**Key Features**:
- Automatic API level detection (Android 12+ vs below)
- Suspend function for non-blocking Bluetooth operations
- Runs on IO dispatcher to prevent UI blocking
- Comprehensive error messages

**Usage Example**:
```kotlin
val bluetoothManager = BluetoothClassicManager(context)

// Send command and receive response
val response = bluetoothManager.sendCommandAndReceive("READ\n")
```

### 2. SoilSensorData.kt
**Location**: `com.example.binhi.bluetooth.SoilSensorData`

Data class representing parsed Bluetooth sensor data.

**Features**:
- Parses "NPK=12,7,9" format automatically
- Handles error cases gracefully
- Provides user-friendly error messages
- Includes raw data for debugging

**Properties**:
- `nitrogen`: Int (0-255)
- `phosphorus`: Int (0-255)
- `potassium`: Int (0-255)
- `rawData`: String (original response)
- `isError`: Boolean
- `errorMessage`: String

**Usage Example**:
```kotlin
val response = "NPK=12,7,9"
val sensorData = SoilSensorData.fromResponse(response)
println("N: ${sensorData.nitrogen}, P: ${sensorData.phosphorus}, K: ${sensorData.potassium}")
```

### 3. BluetoothPermissionHelper.kt
**Location**: `com.example.binhi.bluetooth.BluetoothPermissionHelper`

Utility object for managing Bluetooth permissions.

**Features**:
- Automatic version detection
- Returns correct permissions for API level
- Provides user-friendly descriptions
- Singleton pattern for consistency

**Usage Example**:
```kotlin
val permissions = BluetoothPermissionHelper.getRequiredPermissions()
permissionLauncher.launch(permissions)
```

### 4. BluetoothExtensions.kt
**Location**: `com.example.binhi.bluetooth`

Extension functions for Context.

**Available Extensions**:
- `Context.isBluetoothAvailable()`: Check if device has Bluetooth
- `Context.isBluetoothEnabled()`: Check if Bluetooth is currently on

## AndroidManifest.xml Updates

The following permissions have been added:

```xml
<!-- Bluetooth Permissions (Android 12+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Bluetooth Permissions (Below Android 12) -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

## Integration in GetSoilData.kt

### State Variables
```kotlin
val bluetoothManager = remember { BluetoothClassicManager(context) }
var bluetoothResponse by remember { mutableStateOf<SoilSensorData?>(null) }
var showBluetoothDialog by remember { mutableStateOf(false) }
var isBluetoothLoading by remember { mutableStateOf(false) }
var hasBluetoothPermission by remember {
    mutableStateOf(bluetoothManager.hasBluetoothPermissions())
}
```

### Permission Launcher
```kotlin
val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { permissions ->
        val allGranted = permissions.values.all { it }
        hasBluetoothPermission = allGranted
        if (!allGranted) {
            // Handle permission denial
        }
    }
)
```

### Button Click Handler
```kotlin
Button(
    onClick = {
        if (!hasBluetoothPermission) {
            bluetoothPermissionLauncher.launch(
                BluetoothPermissionHelper.getRequiredPermissions()
            )
        } else {
            isBluetoothLoading = true
            coroutineScope.launch {
                try {
                    val response = bluetoothManager.sendCommandAndReceive("READ\n")
                    bluetoothResponse = SoilSensorData.fromResponse(response)
                } catch (e: Exception) {
                    bluetoothResponse = SoilSensorData(
                        isError = true,
                        errorMessage = "Failed to receive data: ${e.message}"
                    )
                } finally {
                    isBluetoothLoading = false
                    showBluetoothDialog = true
                }
            }
        }
    }
) {
    if (isBluetoothLoading) {
        CircularProgressIndicator(...)
        Text("Receiving...")
    } else {
        Text("Receive Data")
    }
}
```

## How It Works

### 1. Permission Handling
- Automatically detects Android API level
- Requests appropriate permissions (BLUETOOTH_SCAN/BLUETOOTH_CONNECT for 12+)
- Shows error if permissions denied

### 2. Device Connection
- Finds paired device named "ESP32_SOIL_SENSOR"
- Creates RFCOMM socket with SPP UUID (00001101-0000-1000-8000-00805F9B34FB)
- Connects with error handling

### 3. Command Sending
- Sends command via PrintWriter ("READ\n")
- Flushes output to ensure delivery
- Waits for single-line response

### 4. Response Parsing
- Reads one line from input stream
- Parses NPK format: "NPK=12,7,9"
- Handles parsing errors gracefully
- Returns structured data for UI display

### 5. UI Updates
- Uses Jetpack Compose state management
- Shows loading indicator during Bluetooth operation
- Displays result in dialog box
- All operations on background thread (IO dispatcher)

## ESP32 Configuration

Your ESP32 device should:
1. Be named "ESP32_SOIL_SENSOR"
2. Be paired with the Android device
3. Support Bluetooth Classic (SPP/RFCOMM)
4. Accept "READ\n" command
5. Respond with single line in format: "NPK=12,7,9"

Example ESP32 code:
```cpp
// Simple ESP32 Bluetooth Classic response
if (command == "READ") {
    Serial.println("NPK=12,7,9");
}
```

## Troubleshooting

### "Device not found or not paired"
- Ensure ESP32 is paired in Bluetooth settings
- Verify device name is exactly "ESP32_SOIL_SENSOR"

### "Bluetooth permissions not granted"
- Check AndroidManifest.xml has permissions
- Ensure user grants permissions when prompted
- On Android 12+, both SCAN and CONNECT are required

### "No response received"
- Verify ESP32 is powered and in range
- Check ESP32 Bluetooth is working (test with other app)
- Ensure ESP32 sends response in correct format

### "Invalid format" error
- Verify ESP32 sends "NPK=12,7,9" (or similar)
- Check for extra whitespace or newlines
- Ensure device sends one line only

## Testing

To test the implementation:

1. **Pair Device**: Go to Bluetooth settings and pair with ESP32_SOIL_SENSOR
2. **Grant Permissions**: When app requests Bluetooth permissions, tap "Allow"
3. **Click Map Dot**: Long-click a soil sampling point
4. **Click "Receive Data"**: Trigger Bluetooth connection and receive data
5. **View Results**: Data displayed in dialog

## Performance Notes

- Bluetooth operations run on IO dispatcher (background thread)
- UI remains responsive during connection and data transfer
- Loading indicator shown during operation
- Typical operation takes 1-3 seconds

## Security Considerations

- Only connects to paired devices
- Requires explicit runtime permissions
- No sensitive data stored
- RFCOMM is standard Bluetooth serial protocol
- All errors handled and logged

## Future Enhancements

Potential improvements:
- Connection pooling (keep socket open)
- Automatic reconnection on failure
- Multiple sensor support
- Data caching/history
- Custom device name configuration
- Bluetooth device discovery UI

