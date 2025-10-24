package com.example.binhi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class Crop(val name: String, val areaPerPlant: Double, var quantity: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputCropQuantityScreen(navController: NavController) {
    val crops = remember {
        mutableStateListOf(
            Crop("Banana", 3.24, 0),
            Crop("Sweet Potato", 0.23, 0),
            Crop("Cassava", 1.00, 0),
            Crop("Mango", 400.00, 0),
            Crop("Corn", 0.38, 0)
        )
    }

    val totalArea = crops.sumOf { it.quantity * it.areaPerPlant }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input Crop Quantity") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Required Area:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "%.2f sqm".format(totalArea),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalArea > 0) Color(0xFF007AFF) else Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("visualize_analyze/${totalArea}") },
                    enabled = totalArea > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    )
                ) {
                    Text(text = "Confirm", fontSize = 16.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            Text("Set quantity for each crop:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(crops) { crop ->
                    CropQuantityRow(crop = crop, onQuantityChange = { newQuantity ->
                        val index = crops.indexOf(crop)
                        if (index != -1) {
                            crops[index] = crop.copy(quantity = newQuantity)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun CropQuantityRow(crop: Crop, onQuantityChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(crop.name, fontWeight = FontWeight.Bold)
                Text("~${crop.areaPerPlant} sqm/plant", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (crop.quantity > 0) onQuantityChange(crop.quantity - 1) },
                    modifier = Modifier.background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                }
                Text(
                    text = crop.quantity.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onQuantityChange(crop.quantity + 1) },
                    modifier = Modifier.background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                }
            }
        }
    }
}