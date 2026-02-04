package com.example.binhi.bluetooth

import android.Manifest
import android.os.Build

/**
 * Utility object for managing Bluetooth permissions
 */
object BluetoothPermissionHelper {
    /**
     * Get list of permissions needed based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) and above
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Below Android 12
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    /**
     * Get a user-friendly description of required permissions
     */
    fun getPermissionDescription(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "This app needs Bluetooth permission to connect to your soil sensor device."
        } else {
            "This app needs Bluetooth permissions to communicate with your soil sensor device."
        }
    }
}

