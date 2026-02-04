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
 * Handles connection, data sending/receiving via serial port profile (SPP)
 */
class BluetoothClassicManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothClassic"
        // Standard SPP (Serial Port Profile) UUID for Bluetooth Classic
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEVICE_NAME = "ESP32_SOIL_SENSOR"
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
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

    /**
     * Check if device has Bluetooth capability
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Check if required permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: Check BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 12: Only need BLUETOOTH
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get required permissions based on Android version
     */
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

    /**
     * Connect to ESP32 Bluetooth device, send command, and receive response
     * Runs on IO dispatcher to prevent blocking UI thread
     *
     * @param command The command to send (e.g., "READ\n")
     * @return Response text from ESP32 or error message
     */
    suspend fun sendCommandAndReceive(command: String): String = withContext(Dispatchers.IO) {
        try {
            // Verify permissions
            if (!hasBluetoothPermissions()) {
                return@withContext "Error: Bluetooth permissions not granted"
            }

            // Verify Bluetooth is enabled
            if (!isBluetoothEnabled()) {
                return@withContext "Error: Bluetooth is not enabled"
            }

            // Find the paired device
            val device = bluetoothAdapter?.bondedDevices?.find { it.name == DEVICE_NAME }
                ?: return@withContext "Error: Device '$DEVICE_NAME' not found or not paired"

            Log.d(TAG, "Found device: ${device.name} (${device.address})")

            var socket: BluetoothSocket? = null
            try {
                // Create RFCOMM socket (SPP)
                socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } else {
                    @Suppress("DEPRECATION")
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                }

                Log.d(TAG, "Connecting to device...")
                socket.connect() // This will block until connected or fails
                Log.d(TAG, "Connected successfully")

                // Get input/output streams
                val inputStream = socket.inputStream
                val outputStream = socket.outputStream

                // Send command
                Log.d(TAG, "Sending command: ${command.trim()}")
                val writer = PrintWriter(outputStream, true)
                writer.println(command)
                writer.flush()

                // Receive response
                Log.d(TAG, "Waiting for response...")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readLine()
                    ?: return@withContext "Error: No response received from device"

                Log.d(TAG, "Received response: $response")
                return@withContext response

            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception (missing permissions?): ${e.message}")
                return@withContext "Error: Permission denied - ${e.message}"
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}")
                return@withContext "Error: ${e.message ?: "Unknown error"}"
            } finally {
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing socket: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            return@withContext "Error: ${e.message ?: "Unexpected error"}"
        }
    }

    /**
     * Get list of paired Bluetooth devices for debugging/selection
     */
    fun getPairedDevices(): List<String> {
        return try {
            if (!hasBluetoothPermissions()) {
                return emptyList()
            }
            bluetoothAdapter?.bondedDevices?.map { "${it.name} (${it.address})" } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            emptyList()
        }
    }
}

