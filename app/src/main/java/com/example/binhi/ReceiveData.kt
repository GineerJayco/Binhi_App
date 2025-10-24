package com.example.binhi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveDataScreen(navController: NavController) {
    var dataState by remember { mutableStateOf("receiving") } // "receiving", "received"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Data") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (dataState) {
                "receiving" -> {
                    Text("Receiving data...")
                    // Simulate data reception
                    LaunchedEffect(Unit) {
                        delay(3000) // Simulate a 3-second delay for receiving data
                        dataState = "received"
                    }
                }
                "received" -> {
                    Text("Data received. Do you want to accept it?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            // Handle accept - maybe navigate back or show a confirmation
                            navController.popBackStack()
                        }) {
                            Text("Accept")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = {
                            // Handle reject - maybe navigate back
                            navController.popBackStack()
                        }) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}