package com.example.binhi.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build

/**
 * Extension function to check Bluetooth availability
 */
fun Context.isBluetoothAvailable(): Boolean {
    val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    } else {
        @Suppress("DEPRECATION")
        BluetoothAdapter.getDefaultAdapter()
    }
    return adapter != null
}

/**
 * Extension function to check if Bluetooth is enabled
 */
fun Context.isBluetoothEnabled(): Boolean {
    val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    } else {
        @Suppress("DEPRECATION")
        BluetoothAdapter.getDefaultAdapter()
    }
    return adapter?.isEnabled == true
}

