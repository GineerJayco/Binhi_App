/**
 * COMPLETE BLUETOOTH INTEGRATION EXAMPLE
 * This file demonstrates the complete implementation
 * Copy-paste patterns for your own projects
 */

// ============================================================================
// FILE 1: BluetoothManager.kt
// ============================================================================

package com.example.binhi.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID

/**
 * Bluetooth Classic (SPP/RFCOMM) Manager for ESP32 communication
 */
class BluetoothClassicManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothClassic"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEVICE_NAME = "ESP32_SOIL_SENSOR"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            manager.adapter
        } else {
            @Suppress("DEPRECATION")
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    suspend fun sendCommandAndReceive(command: String): String = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                return@withContext "Error: Bluetooth permissions not granted"
            }

            val device = bluetoothAdapter?.bondedDevices?.find { it.name == DEVICE_NAME }
                ?: return@withContext "Error: Device '$DEVICE_NAME' not found"

            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()

                val outputStream = socket.outputStream
                val inputStream = socket.inputStream

                val writer = PrintWriter(outputStream, true)
                writer.println(command)
                writer.flush()

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readLine() ?: return@withContext "Error: No response"

                return@withContext response

            } catch (e: Exception) {
                return@withContext "Error: ${e.message ?: "Unknown error"}"
            } finally {
                socket?.close()
            }

        } catch (e: Exception) {
            return@withContext "Error: ${e.message ?: "Unexpected error"}"
        }
    }
}

// ============================================================================
// FILE 2: SoilSensorData.kt
// ============================================================================

package com.example.binhi.bluetooth

data class SoilSensorData(
    val nitrogen: Int = 0,
    val phosphorus: Int = 0,
    val potassium: Int = 0,
    val rawData: String = "",
    val isError: Boolean = false,
    val errorMessage: String = ""
) {
    companion object {
        fun fromResponse(response: String): SoilSensorData {
            return try {
                if (response.startsWith("Error:", ignoreCase = true)) {
                    return SoilSensorData(
                        rawData = response,
                        isError = true,
                        errorMessage = response
                    )
                }

                val regex = """NPK\s*=\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)""".toRegex()
                val matchResult = regex.find(response)

                if (matchResult != null) {
                    val (n, p, k) = matchResult.destructured
                    SoilSensorData(
                        nitrogen = n.toInt(),
                        phosphorus = p.toInt(),
                        potassium = k.toInt(),
                        rawData = response
                    )
                } else {
                    SoilSensorData(
                        rawData = response,
                        isError = true,
                        errorMessage = "Invalid format: $response"
                    )
                }
            } catch (e: Exception) {
                SoilSensorData(
                    rawData = response,
                    isError = true,
                    errorMessage = "Parse error: ${e.message}"
                )
            }
        }
    }
}

// ============================================================================
// FILE 3: Jetpack Compose Integration (GetSoilData.kt)
// ============================================================================

// Add to imports:
// import com.example.binhi.bluetooth.BluetoothClassicManager
// import com.example.binhi.bluetooth.BluetoothPermissionHelper
// import com.example.binhi.bluetooth.SoilSensorData

@Composable
fun GetSoilData(
    navController: NavController,
    landArea: String?,
    length: String?,
    width: String?,
    crop: String?,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Bluetooth state
    val bluetoothManager = remember { BluetoothClassicManager(context) }
    var bluetoothResponse by remember { mutableStateOf<SoilSensorData?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var isBluetoothLoading by remember { mutableStateOf(false) }
    var hasBluetoothPermission by remember {
        mutableStateOf(bluetoothManager.hasBluetoothPermissions())
    }

    // Permission launcher
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasBluetoothPermission = permissions.values.all { it }
        }
    )

    // Dialog when dot is clicked
    if (showDialog && selectedDot != null) {
        Dialog(onDismissRequest = { showDialog = false; selectedDot = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Coordinates", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Latitude: ${decimalToDMS(selectedDot!!.latitude, true)}")
                    Text("Longitude: ${decimalToDMS(selectedDot!!.longitude, false)}")
                    Spacer(modifier = Modifier.height(16.dp))

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
                                            errorMessage = "Failed: ${e.message}"
                                        )
                                    } finally {
                                        isBluetoothLoading = false
                                        showBluetoothDialog = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBluetoothLoading
                    ) {
                        if (isBluetoothLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Receiving...")
                        } else {
                            Text("Receive Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showDialog = false; selectedDot = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Response dialog
    if (showBluetoothDialog && bluetoothResponse != null) {
        Dialog(onDismissRequest = { showBluetoothDialog = false; bluetoothResponse = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (bluetoothResponse!!.isError) "Error" else "Soil Data",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (bluetoothResponse!!.isError) Color.Red else Color.Green
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (bluetoothResponse!!.isError) {
                        Text(bluetoothResponse!!.errorMessage, color = Color.Red)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "Nitrogen" to bluetoothResponse!!.nitrogen,
                                "Phosphorus" to bluetoothResponse!!.phosphorus,
                                "Potassium" to bluetoothResponse!!.potassium
                            ).forEach { (label, value) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, style = MaterialTheme.typography.bodySmall)
                                    Text("$value", style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showBluetoothDialog = false; bluetoothResponse = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// ============================================================================
// AndroidManifest.xml - Add these permissions
// ============================================================================

/*
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
*/

// ============================================================================
// USAGE SUMMARY
// ============================================================================

/*
1. Copy BluetoothManager.kt to bluetooth package
2. Copy SoilSensorData.kt to bluetooth package
3. Add Bluetooth imports to GetSoilData.kt
4. Add Bluetooth state variables
5. Add permission launcher
6. Add button handler with Bluetooth logic
7. Add response dialog
8. Update AndroidManifest.xml with permissions
9. Test with paired ESP32_SOIL_SENSOR

Expected data flow:
User clicks "Receive Data"
  → Check permissions
  → Request if needed
  → Connect to ESP32_SOIL_SENSOR
  → Send "READ\n"
  → Receive "NPK=12,7,9"
  → Parse and display
  → Show dialog with results
*/

