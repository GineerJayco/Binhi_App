package com.example.binhi

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.binhi.ui.theme.BinhiTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.roundToInt

// Define states for our draggable bottom sheet
private enum class SheetState { Collapsed, Expanded }

@Composable
fun VisualizeAndAnalyzeScreen(navController: NavController, landArea: String?) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    var connectionStatus by remember { mutableStateOf("") }
    var discoveredDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }


    val requestBluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startBluetoothDiscovery(context, bluetoothAdapter, onStatusChange = {
                connectionStatus = it
                if (it == "Searching for devices...") {
                    isSearching = true
                } else if (it == "Discovery Finished") {
                    isSearching = false
                }
            }, onDevicesDiscovered = { devices ->
                discoveredDevices = devices
            })
        } else {
            Toast.makeText(context, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Map State Setup ---
    val defaultLocation = LatLng(14.5995, 120.9842) // Default: Manila, PH
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    var mapType by remember { mutableStateOf(MapType.SATELLITE) }
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)) }
    // --- End Map State Setup ---


    // --- Draggable Bottom Sheet Logic ---
    var sheetState by remember { mutableStateOf(SheetState.Collapsed) }
    val collapsedHeight = 180.dp
    val expandedHeight = 400.dp
    val sheetHeight by animateDpAsState(
        targetValue = if (sheetState == SheetState.Expanded) expandedHeight else collapsedHeight,
        label = "sheetHeight"
    )
    // --- End Draggable Logic ---

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties.copy(mapType = mapType),
            uiSettings = uiSettings
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(150.dp)
                .background(Color.Red.copy(alpha = 0.3f))
                .border(2.dp, Color.Red.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                append("${landArea ?: "0.0"} sqm")
                            }
                            withStyle(style = SpanStyle(color = Color.White)) {
                                append(" Plot")
                            }
                        },
                        fontSize = 16.sp
                    )
                    Text(
                        "Drag the red square to position it.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            FloatingActionButton(
                onClick = { mapType = MapType.SATELLITE },
                modifier = Modifier.size(48.dp),
                containerColor = if (mapType == MapType.SATELLITE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (mapType == MapType.SATELLITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Satellite View")
            }
            Spacer(modifier = Modifier.height(8.dp))
            FloatingActionButton(
                onClick = { mapType = MapType.NORMAL },
                modifier = Modifier.size(48.dp),
                containerColor = if (mapType == MapType.NORMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (mapType == MapType.NORMAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Map, contentDescription = "Normal View")
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y < -10) {
                                sheetState = SheetState.Expanded
                            } else if (dragAmount.y > 10) {
                                sheetState = SheetState.Collapsed
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Connect to ESP32 Sensor",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose a method to collect soil data.",
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (!isConnected) {
                    Button(
                        onClick = {
                            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            } else {
                                arrayOf(
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_ADMIN,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            }
                            if (requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                                startBluetoothDiscovery(context, bluetoothAdapter, onStatusChange = {
                                    connectionStatus = it
                                    if (it == "Searching for devices...") {
                                        isSearching = true
                                    } else if (it == "Discovery Finished") {
                                        isSearching = false
                                    }
                                }, onDevicesDiscovered = { devices ->
                                    discoveredDevices = devices
                                })
                            } else {
                                requestBluetoothPermissionLauncher.launch(requiredPermissions)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth Icon", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect via Bluetooth", fontSize = 16.sp)
                    }
                } else {
                    Button(
                        onClick = { navController.navigate("receive_data") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Receive Data", fontSize = 16.sp)
                    }
                }

                if (connectionStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(connectionStatus, color = if (connectionStatus.startsWith("Connected")) Color.Green else if (connectionStatus.contains("Failed")) Color.Red else Color.Black)
                }

                if ((isSearching || discoveredDevices.isNotEmpty()) && !isConnected) {
                    LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                        items(discoveredDevices) { device ->
                            DeviceListItem(device = device, onDeviceClick = {
                                if (device.bondState == BluetoothDevice.BOND_BONDED) {
                                    connectToDevice(device) { status ->
                                        connectionStatus = status
                                        if (status.startsWith("Connected")) {
                                            isConnected = true
                                        }
                                    }
                                } else {
                                    pairDevice(context, device) { connectionStatus = it }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(device: BluetoothDevice, onDeviceClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeviceClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = device.name ?: "Unknown Device", modifier = Modifier.weight(1f))
        Text(text = if (device.bondState == BluetoothDevice.BOND_BONDED) "Paired" else "Available")
    }
}

@SuppressLint("MissingPermission")
private fun startBluetoothDiscovery(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    onStatusChange: (String) -> Unit,
    onDevicesDiscovered: (List<BluetoothDevice>) -> Unit
) {
    if (bluetoothAdapter == null) {
        onStatusChange("Bluetooth not supported")
        return
    }
    if (!bluetoothAdapter.isEnabled) {
        onStatusChange("Please enable Bluetooth")
        return
    }

    val receiver = object : BroadcastReceiver() {
        private val devices = mutableListOf<BluetoothDevice>()

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null && device.name != null && !devices.contains(device)) {
                        devices.add(device)
                        onDevicesDiscovered(devices.toList())
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    onStatusChange("Discovery Finished")
                }
            }
        }
    }

    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    context.registerReceiver(receiver, filter)

    if (bluetoothAdapter.isDiscovering) {
        bluetoothAdapter.cancelDiscovery()
    }
    bluetoothAdapter.startDiscovery()
    onStatusChange("Searching for devices...")
}

@SuppressLint("MissingPermission")
private fun pairDevice(
    context: Context,
    device: BluetoothDevice,
    onStatusChange: (String) -> Unit
) {
    onStatusChange("Pairing with ${device.name}...")
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                if (state == BluetoothDevice.BOND_BONDED) {
                    onStatusChange("Paired with ${device.name}")
                    context.unregisterReceiver(this)
                } else if (state == BluetoothDevice.BOND_NONE) {
                    onStatusChange("Pairing Failed")
                    context.unregisterReceiver(this)
                }
            }
        }
    }
    context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
    device.createBond()
}

@SuppressLint("MissingPermission")
private fun connectToDevice(
    device: BluetoothDevice,
    onStatusChange: (String) -> Unit
) {
    onStatusChange("Connecting to ${device.name}...")
    // In a real app, you would establish a BluetoothSocket connection here.
    // For this example, we'll just simulate a successful connection.
    onStatusChange("Connected to ${device.name}")
}

@Preview(showBackground = true)
@Composable
fun VisualizeAndAnalyzeScreenPreview() {
    BinhiTheme {
        VisualizeAndAnalyzeScreen(navController = rememberNavController(), landArea = "36.0")
    }
}
